import argparse
import io
import socket
import json
from ipaddress import IPv4Address, IPv6Address
from random import randint

dns_types = {1: 'A', 15: 'MX', 16: 'TXT', 28: 'AAAA'}
dns_types_by_names = {'A': 1, 'MX': 15, 'TXT': 16, 'AAAA': 28}


class Decoder:
    def __init__(self, data, start, end):
        self.data = data
        self.start = start
        self.end = end

    def __len__(self):
        return self.end - self.start

    def read_chunk(self, count, start_pos=None):
        if start_pos is None:
            if self.start + count > self.end:
                raise ValueError('End of packet chunk')
            self.start += count
            return self.data[(self.start - count):self.start]
        else:
            return self.data[start_pos:(start_pos + count)]

    def read_int(self, count):
        chunk = self.read_chunk(count)
        return int.from_bytes(chunk, byteorder='big', signed=False)

    def read_data_to_decoder(self, count):
        if self.start + count > self.end:
            raise ValueError('End of packet chunk')
        self.start += count
        return Decoder(self.data, self.start - count, self.start)

    def read_pos_name(self, pos):
        size = self.data[pos]
        if size == 0:
            return ''
        elif (size >> 6) == 3:
            pos = ((size & 63) << 8) | self.data[pos + 1]
            return self.read_pos_name(pos)
        else:
            label = self.read_chunk(size, pos + 1).decode('ascii')
            return f"{label}.{self.read_pos_name(pos + 1 + size)}"

    def read_name(self):
        size = self.read_int(1)
        if size == 0:
            return ''
        elif (size >> 6) == 3:
            pos = ((size & 63) << 8) | self.read_int(1)
            return self.read_pos_name(pos)
        else:
            label = self.read_chunk(size).decode('ascii')
            return f"{label}.{self.read_name()}"


class Encoder:
    def __init__(self, writer):
        self.writer = writer

    def write_chuck(self, value):
        self.writer.write(value)

    def write_int(self, value, count):
        value_bytes = value.to_bytes(count, byteorder='big', signed=False)
        self.write_chuck(value_bytes)

    def write_name(self, value):
        labels = value.split('.')
        for label in labels:
            label_bytes = label.encode('ascii')
            self.write_int(len(label_bytes), 1)
            self.write_chuck(label_bytes)
        self.write_int(0, 1)


class Question:
    def __init__(self, rtype=1, name=""):
        self.type_id = rtype
        type_name = dns_types[self.type_id]
        if type_name is not None:
            self.type = type_name
        self.name = name

    def encode(self, encoder):
        encoder.write_name(self.name)
        encoder.write_int(self.type_id, 2)
        encoder.write_int(1, 2)  # qclass = IN

    def decode(self, decoder):
        self.name = decoder.read_name()
        self.type_id = decoder.read_int(2)
        type_name = dns_types[self.type_id]
        if type_name is not None:
            self.type = type_name
        decoder.read_int(2)


def read_question(decoder):
    question = Question()
    question.decode(decoder)
    return question


def read_record(decoder):
    name = decoder.read_name()
    rtype = decoder.read_int(2)
    decoder.read_int(2)
    ttl = decoder.read_int(4)
    size = decoder.read_int(2)
    result = {'name': name, 'ttl': ttl, 'type_id': rtype}
    record_decoder = decoder.read_data_to_decoder(size)
    record_type = dns_types[rtype]
    if record_type == 'A':
        result['ipv4_address'] = IPv4Address(record_decoder.read_int(4))
    elif record_type == 'MX':
        result['preference'] = record_decoder.read_int(2)
        result['exchange'] = record_decoder.read_name()
    elif record_type == 'TXT':
        result['text'] = record_decoder.read_name()
    elif record_type == 'AAAA':
        result['ipv6_address'] = IPv6Address(record_decoder.read_int(16))
    return result


class Packet:
    def __init__(self, decoder):  # decoding packet
        self.id = decoder.read_int(2)

        octet = decoder.read_int(1)
        self.qr = bool(octet >> 7)
        self.opcode = (octet >> 3) & 15
        self.authority_answer = bool((octet >> 2) & 1)
        self.truncation = bool((octet >> 1) & 1)
        self.recurtion_desired = bool(octet & 1)

        octet = decoder.read_int(1)
        self.recursion_available = bool(octet >> 7)
        self.rcode = octet & 15

        self.qdcount = decoder.read_int(2)
        self.ancount = decoder.read_int(2)
        self.nscount = decoder.read_int(2)
        self.arcount = decoder.read_int(2)

        self.questions = [read_question(decoder) for _ in range(self.qdcount)]
        self.answers = [read_record(decoder) for _ in range(self.ancount)]
        self.name_servers = [read_record(decoder) for _ in range(self.nscount)]
        self.additional_answers = [read_record(decoder) for _ in range(self.arcount)]


def preparing_request(encoder, question):
    # header
    id = randint(0, 65536)
    encoder.write_int(id, 2)  # random id
    encoder.write_int(1, 1)   # qr = 0; opcode = 1; aa = 0; tc = 0; rd = 1;
    encoder.write_int(0, 1)   # ra = 0; z = 0; rcode = 0;
    encoder.write_int(1, 2)   # qdcount = 1
    encoder.write_int(0, 2)   # ancount = 1
    encoder.write_int(0, 2)   # nscount = 1
    encoder.write_int(0, 2)   # arcount = 1
    # question
    question.encode(encoder)


def serializer(obj):
    if isinstance(obj, IPv4Address) or isinstance(obj, IPv6Address):
        return str(obj)
    return obj.__dict__


parser = argparse.ArgumentParser()
parser.add_argument('-ip', default='8.8.8.8', type=str)
parser.add_argument('-p', default=53, type=int)
parser.add_argument('-t', default='A')
parser.add_argument('-d')
args = parser.parse_args()
addr = (args.ip, args.p)

client_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
writer = io.BytesIO()
encoder = Encoder(writer)

try:
    type_code = dns_types_by_names[args.t]
    question = Question(type_code, args.d)
except ValueError:
    print(f'Unknown type: {args.t}')
    exit(1)

preparing_request(encoder, question)
writer.seek(0)
encoded_packet = writer.read()
client_sock.sendto(encoded_packet, addr)
encoded_packet, address = client_sock.recvfrom(10000)
decoder = Decoder(encoded_packet, 0, len(encoded_packet))
packet = Packet(decoder)
print(json.dumps(packet, indent=4, default=serializer))

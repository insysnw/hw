from __future__ import print_function

import argparse
import fnmatch
import functools
import logging
import socket
import string
import struct
import sys
import types

from contextlib import closing

try:
    from StringIO import StringIO
except ImportError:
    from io import StringIO

__version__ = '1'

PY3 = sys.version_info[0] == 3

logging.basicConfig(format='[%(levelname)s] %(message)s')
logger = logging.getLogger()
logger.setLevel(logging.WARNING)

ASN1_INTEGER = 0x02
ASN1_OCTET_STRING = 0x04
ASN1_NULL = 0x05
ASN1_OBJECT_IDENTIFIER = 0x06
ASN1_SEQUENCE = 0x30
ASN1_IPADDRESS = 0x40
ASN1_NO_SUCH_OBJECT = 0x80
ASN1_NO_SUCH_INSTANCE = 0x81
ASN1_END_OF_MIB_VIEW = 0x82
ASN1_GET_REQUEST_PDU = 0xA0
ASN1_GET_NEXT_REQUEST_PDU = 0xA1
ASN1_GET_RESPONSE_PDU = 0xA2
ASN1_ERROR_STATUS_NO_ERROR = 0x00
ASN1_ERROR_STATUS_NO_SUCH_NAME = 0x02


def encode_to_7bit(value):
    if value > 0x7f:
        res = []
        res.insert(0, value & 0x7f)
        while value > 0x7f:
            value >>= 7
            res.insert(0, (value & 0x7f) | 0x80)
        return res
    return [value]


def oid_to_bytes_list(oid):
    if oid.startswith('iso'):
        oid = oid.replace('iso', '1')
    oid_values = [int(x) for x in oid.split('.') if x]
    first_val = 40 * oid_values[0] + oid_values[1]
    result_values = [first_val]
    for node_num in oid_values[2:]:
        result_values += encode_to_7bit(node_num)
    return result_values


def oid_to_bytes(oid):
    return ''.join([chr(x) for x in oid_to_bytes_list(oid)])


def bytes_to_oid(data):
    values = [ord(x) for x in data]
    first_val = values.pop(0)
    res = []
    res += divmod(first_val, 40)
    while values:
        val = values.pop(0)
        if val > 0x7f:
            huges = []
            huges.append(val)
            while True:
                next_val = values.pop(0)
                huges.append(next_val)
                if next_val < 0x80:
                    break
            huge = 0
            for i, huge_byte in enumerate(huges):
                huge += (huge_byte & 0x7f) << (7 * (len(huges) - i - 1))
            res.append(huge)
        else:
            res.append(val)
    return '.'.join(str(x) for x in res)


def int_to_ip(value):
    return socket.inet_ntoa(struct.pack("!I", value))


def twos_complement(value, bits):
    mask = 2 ** (bits - 1)
    return -(value & mask) + (value & ~mask)


def _read_byte(stream):
    read_byte = stream.read(1)
    if not read_byte:
        raise Exception('No more bytes!')
    return ord(read_byte)


def _read_int_len(stream, length, signed=False):
    result = 0
    sign = None
    for _ in range(length):
        value = _read_byte(stream)
        if sign is None:
            sign = value & 0x80
        result = (result << 8) + value
    if signed and sign:
        result = twos_complement(result, 8 * length)
    return result


def _write_int(value, strip_leading_zeros=True):
    if abs(value) > 0xffffffffffffffff:
        raise Exception('Invalid int value')
    if value < 0:
        if abs(value) <= 0x7f:
            result = struct.pack('>b', value)
        elif abs(value) <= 0x7fff:
            result = struct.pack('>h', value)
        elif abs(value) <= 0x7fffffff:
            result = struct.pack('>i', value)
        elif abs(value) <= 0x7fffffffffffffff:
            result = struct.pack('>q', value)
    else:
        result = struct.pack('>Q', value)
    result = result.lstrip(b'\x00') if strip_leading_zeros else result
    return result or b'\x00'


def _write_asn1_length(length):
    if length > 0x7f:
        if length <= 0xff:
            packed_length = 0x81
        elif length <= 0xffff:
            packed_length = 0x82
        elif length <= 0xffffff:
            packed_length = 0x83
        elif length <= 0xffffffff:
            packed_length = 0x84
        else:
            raise Exception('Length is too big!')
        return struct.pack('B', packed_length) + _write_int(length)
    return struct.pack('B', length)


def _parse_asn1_length(stream):
    length = _read_byte(stream)
    if length > 0x7f:
        data_length = length - 0x80
        if not 0 < data_length <= 4:
            raise Exception('Invalid data')
        length = _read_int_len(stream, data_length)
    return length


def _parse_asn1_octet_string(stream):
    length = _parse_asn1_length(stream)
    value = stream.read(length)
    if any([c not in string.printable for c in value]):
        return ' '.join(['%02X' % ord(x) for x in value])
    return value


def _parse_snmp_asn1(stream):
    result = []
    wait_oid_value = False
    pdu_index = 0
    while True:
        read_byte = stream.read(1)
        if not read_byte:
            if pdu_index < 7:
                raise Exception('Not all SNMP protocol data units are read!')
            return result
        tag = ord(read_byte)
        if (
                pdu_index in [1, 4, 5, 6] and tag != ASN1_INTEGER or
                pdu_index == 2 and tag != ASN1_OCTET_STRING or
                pdu_index == 3 and tag not in [
                    ASN1_GET_REQUEST_PDU,
                    ASN1_GET_NEXT_REQUEST_PDU,
                ]
        ):
            raise Exception('Invalid tag for PDU unit')
        if tag == ASN1_SEQUENCE:
            length = _parse_asn1_length(stream)
            logger.debug('SEQUENCE: %s', 'length = {}'.format(length))
        elif tag == ASN1_INTEGER:
            length = _read_byte(stream)
            value = _read_int_len(stream, length, True)
            logger.debug('INTEGER: %s', value)
            if wait_oid_value or pdu_index in [1, 4, 5, 6]:
                result.append(('INTEGER', value))
                wait_oid_value = False
        elif tag == ASN1_OCTET_STRING:
            value = _parse_asn1_octet_string(stream)
            logger.debug('OCTET_STRING: %s', value)
            if wait_oid_value or pdu_index == 2:
                result.append(('STRING', value))
                wait_oid_value = False
        elif tag == ASN1_OBJECT_IDENTIFIER:
            length = _read_byte(stream)
            value = stream.read(length)
            logger.debug('OBJECT_IDENTIFIER: %s', bytes_to_oid(value))
            result.append(('OID', bytes_to_oid(value)))
            wait_oid_value = True
        elif tag == ASN1_GET_REQUEST_PDU:
            length = _parse_asn1_length(stream)
            logger.debug('GET_REQUEST_PDU: %s', 'length = {}'.format(length))
            if pdu_index == 3:
                result.append(('ASN1_GET_REQUEST_PDU', tag))
        elif tag == ASN1_GET_NEXT_REQUEST_PDU:
            length = _parse_asn1_length(stream)
            logger.debug('GET_NEXT_REQUEST_PDU: %s', 'length = {}'.format(length))
            if pdu_index == 3:
                result.append(('GET_NEXT_REQUEST_PDU', tag))
        elif tag == ASN1_IPADDRESS:
            length = _read_byte(stream)
            value = _read_int_len(stream, length)
            logger.debug('ASN1_IPADDRESS: %s (%s)', value, int_to_ip(value))
            if wait_oid_value:
                result.append(('IPADDRESS', int_to_ip(value)))
                wait_oid_value = False
        elif tag == ASN1_NULL:
            value = _read_byte(stream)
            logger.debug('ASN1_NULL: %s', value)
        elif tag == ASN1_NO_SUCH_OBJECT:
            value = _read_byte(stream)
            logger.debug('ASN1_NO_SUCH_OBJECT: %s', value)
            result.append('No Such Object')
        elif tag == ASN1_NO_SUCH_INSTANCE:
            value = _read_byte(stream)
            logger.debug('ASN1_NO_SUCH_INSTANCE: %s', value)
            result.append('No Such Instance with OID')
        elif tag == ASN1_END_OF_MIB_VIEW:
            value = _read_byte(stream)
            logger.debug('ASN1_END_OF_MIB_VIEW: %s', value)
            return (('', ''), ('', ''))
        pdu_index += 1
    return result


def get_next_oid(oid):
    oid_vals = oid.rsplit('.', 2)
    if len(oid_vals) < 2:
        oid_vals[-1] = str(int(oid_vals[-1]) + 1)
    else:
        oid_vals[-2] = str(int(oid_vals[-2]) + 1)
        oid_vals[-1] = '1'
    oid_next = '.'.join(oid_vals)
    return oid_next


def write_tlv(tag, length, value):
    return struct.pack('B', tag) + _write_asn1_length(length) + value


def write_tv(tag, value):
    return write_tlv(tag, len(value), value)


def integer(value, enum=None):
    return write_tv(ASN1_INTEGER, _write_int(value, False)), enum


def octet_string(value):
    return write_tv(ASN1_OCTET_STRING, value.encode() if PY3 else value)


def null():
    return write_tv(ASN1_NULL, b'')


def object_identifier(value):
    value = oid_to_bytes(value)
    return write_tv(ASN1_OBJECT_IDENTIFIER, value.encode() if PY3 else value)


def ip_address(value):
    return write_tv(ASN1_IPADDRESS, socket.inet_aton(value))


def replace_wildcards(value):
    return value.replace('?', '9').replace('*', str(0xffffffff))


def oid_cmp(oid1, oid2):
    oid1 = replace_wildcards(oid1)
    oid2 = replace_wildcards(oid2)
    oid1 = [int(x) for x in oid1.replace('iso', '1').strip('.').split('.')]
    oid2 = [int(x) for x in oid2.replace('iso', '1').strip('.').split('.')]
    if oid1 < oid2:
        return -1
    elif oid1 > oid2:
        return 1
    return 0


def get_next(oids, oid):
    for val in sorted(oids, key=functools.cmp_to_key(oid_cmp)):
        if not oid:
            return val
        elif oid_cmp(oid, val) < 0:
            return val
    return ''


def parse_config(filename):
    oids = {}
    with open(filename, 'rb') as conf_file:
        data = conf_file.read()
        out_locals = {}
        exec(data, globals(), out_locals)
        oids = out_locals['DATA']
        for value in oids.values():
            if isinstance(value, types.FunctionType):
                if value.__code__.co_argcount != 1:
                    raise Exception('"{}" must have one argument'.format(value.__name__))
        return oids


def find_oid_and_value_with_wildcard(oids, oid):
    wildcard_keys = [x for x in oids.keys() if '*' in x or '?' in x]
    out = []
    for wck in wildcard_keys:
        if fnmatch.filter([oid], wck):
            value = oids[wck](oid)
            out.append((wck, value,))
    return out


def handle_get_request(oids, oid):
    error_status = ASN1_ERROR_STATUS_NO_ERROR
    error_index = 0
    oid_value = null()
    found = oid in oids
    if found:
        oid_value = oids[oid]
        if not oid_value:
            oid_value = struct.pack('BB', ASN1_NO_SUCH_OBJECT, 0)
    else:
        results = find_oid_and_value_with_wildcard(oids, oid)
        if results:
            _, oid_value = results[0]
            if oid_value:
                found = True
    if not found:
        error_status = ASN1_ERROR_STATUS_NO_SUCH_NAME
        error_index = 1
        oid_value = struct.pack('BB', ASN1_NO_SUCH_INSTANCE, 0)
    return error_status, error_index, oid_value


def handle_get_next_request(oids, oid):
    error_status = ASN1_ERROR_STATUS_NO_ERROR
    error_index = 0
    oid_value = null()
    new_oid = None
    if oid in oids:
        new_oid = get_next(oids, oid)
        if not new_oid:
            oid_value = struct.pack('BB', ASN1_END_OF_MIB_VIEW, 0)  #null()
        else:
            oid_value = oids.get(new_oid)
    else:
        results = find_oid_and_value_with_wildcard(oids, oid)
        if results:
            oid_key, oid_value = results[0]
            new_oid = get_next(oids, oid_key)
        else:
            new_oid = get_next(oids, oid)
        oid_value = oids.get(new_oid)
    if not oid_value:
        oid_value = null()
    if new_oid:
        oid = new_oid
    else:
        oid = get_next_oid(oid.rstrip('.0')) + '.0'
    final_oid = replace_wildcards(oid)
    return error_status, error_index, final_oid, oid_value


def craft_response(version, community, request_id, error_status, error_index, oid_items):
    response = write_tv(
        ASN1_SEQUENCE,
        write_tv(ASN1_INTEGER, _write_int(version)) +
        write_tv(ASN1_OCTET_STRING, community.encode() if PY3 else str(community)) +
        write_tv(
            ASN1_GET_RESPONSE_PDU,
            write_tv(ASN1_INTEGER, _write_int(request_id)) +
            write_tlv(ASN1_INTEGER, 1, _write_int(error_status)) +
            write_tlv(ASN1_INTEGER, 1, _write_int(error_index)) +
            write_tv(
                ASN1_SEQUENCE,
                b''.join(
                    write_tv(
                        ASN1_SEQUENCE,
                        write_tv(
                            ASN1_OBJECT_IDENTIFIER,
                            oid_key.encode() if PY3 else oid_key
                        ) +
                        oid_value
                    ) for (oid_key, oid_value) in oid_items
                )
            )
        )
    )
    return response


def snmp_server(host, port, oids):
    with closing(socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)) as sock:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind((host, port))
        print('SNMP server listening on {}:{}'.format(host, port))

        while True:
            request_data, address = sock.recvfrom(4096)
            request_stream = StringIO(request_data.decode('latin'))
            request_result = _parse_snmp_asn1(request_stream)

            version = request_result[0][1]
            community = request_result[1][1]
            pdu_type = request_result[2][1]
            request_id = request_result[3][1]

            error_status = ASN1_ERROR_STATUS_NO_ERROR
            error_index = 0
            oid_items = []
            oid_value = null()

            if pdu_type == ASN1_GET_REQUEST_PDU:
                requested_oids = request_result[6:]
                for _, oid in requested_oids:
                    _, _, oid_value = handle_get_request(oids, oid)
                    if isinstance(oid_value, types.FunctionType):
                        oid_value = oid_value(oid)
                    if isinstance(oid_value, tuple):
                        oid_value = oid_value[0]
                    oid_items.append((oid_to_bytes(oid), oid_value))

            elif pdu_type == ASN1_GET_NEXT_REQUEST_PDU:
                oid = request_result[6][1]
                error_status, error_index, oid, oid_value = handle_get_next_request(oids, oid)
                if isinstance(oid_value, types.FunctionType):
                    oid_value = oid_value(oid)
                if isinstance(oid_value, tuple):
                    oid_value = oid_value[0]
                oid_items.append((oid_to_bytes(oid), oid_value))

            response = craft_response(
                version, community, request_id, error_status, error_index, oid_items)
            try:
                sock.sendto(response, address)
            except socket.error:
                logger.error('Failed to send')
            logger.debug('')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '-p', '--port', dest='port', type=int, default=161, required=False)
    parser.add_argument(
        '-c', '--config', default="config.py", type=str, required=False)
    parser.add_argument(
        '-v', '--version', default=1, action='version',
        version='SNMP server v{}'.format(__version__))

    args = parser.parse_args()

    oids = {
        '*': lambda oid: octet_string(oid),
    }
    config_filename = args.config
    if config_filename:
        try:
            oids = parse_config(config_filename)
        except Exception as ex:
            logger.error(ex)
            sys.exit(-1)

    host = '0.0.0.0'
    port = args.port
    try:
        snmp_server(host, port, oids)
    except KeyboardInterrupt:
        logger.debug('Ctrl+C')


if __name__ == '__main__':
    main()
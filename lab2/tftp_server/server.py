import argparse
import struct
import sys
import os
from threading import Thread, Event
from socket import socket, AF_INET, SOCK_DGRAM, gethostbyname, gethostname

RRQ_MODE = 1  # read request
WRQ_MODE = 2  # write request
DATA_MODE = 3  # data
ACK_MODE = 4  # answer
ERROR_MODE = 5  # error

MAX_BYTES = 512

# error codes
FILE_NOT_FOUND_ERROR = 1
ACCESS_VIOLATION_ERROR = 2
DISK_FULL_ERROR = 3
ILLEGAL_TFTP_OP_ERROR = 4
UNKNOWN_TID_ERROR = 5
FILE_ALREADY_EXISTS_ERROR = 6
NO_SUCH_USER_ERROR = 7


class FileContext:

    def __init__(self, filename, block_num, timer, is_done=None):
        self.filename = filename
        self.block_num = block_num
        self.timer = timer
        self.is_done = is_done


class Server:

    def __init__(self, s, to):
        self.sock = s
        self.timeout = to

        self.reads = {}
        self.writes = {}

    def handle_RRQ(self, address, filename, block_num):
        if not os.path.exists(filename):
            self.handle_ERROR(address, FILE_NOT_FOUND_ERROR, 'File not found')
            pass
        with open(filename, 'rb') as file:
            file.seek((block_num - 1) * MAX_BYTES, os.SEEK_SET)
            buf = file.read(MAX_BYTES)

        is_done = len(buf) < MAX_BYTES
        packet = struct.pack('>hh%ds' % len(buf), DATA_MODE, block_num, buf)
        timer = Event()
        self.reads[address] = FileContext(filename, block_num, timer, is_done)

        print(f'Sent DATA {block_num}')
        self.sock.sendto(packet, address)
        while not timer.wait(self.timeout):
            print('Retry Data', block_num)
            self.sock.sendto(packet, address)

    def handle_WRQ(self, address, filename, block_num, buf=None):
        if os.path.exists(filename):
            self.handle_ERROR(address, FILE_ALREADY_EXISTS_ERROR, 'File has already exist')
            pass
        is_done = False
        if block_num > 0:
            with open(filename, 'wb') as file:
                file.seek((block_num - 1) * MAX_BYTES, os.SEEK_SET)
                file.write(buf)

            is_done = len(buf) < MAX_BYTES

        packet = struct.pack('>hh', ACK_MODE, block_num)
        timer = Event()
        self.writes[address] = FileContext(filename, block_num, timer)

        print(f'Sent ACK {block_num}')
        self.sock.sendto(packet, address)
        if not is_done:
            while not timer.wait(self.timeout):
                print(f'Retry ACK {block_num}')
                self.sock.sendto(packet, address)

    def handle_ACK(self, address, block_num):
        if address not in self.reads.keys():
            self.handle_ERROR(address, UNKNOWN_TID_ERROR, 'Unknown transfer ID')
            sys.exit()

        fc = self.reads[address]
        if fc.block_num == block_num:
            fc.timer.set()
            print(f'Received ACK {block_num}')
            if not fc.is_done:
                self.handle_RRQ(address, fc.filename, fc.block_num + 1)

    def handle_DATA(self, address, block_num, buf):
        if address not in self.writes.keys():
            self.handle_ERROR(address, UNKNOWN_TID_ERROR, 'Unknown transfer ID')
            sys.exit()

        fc = self.writes[address]
        if fc.block_num == block_num - 1:
            fc.timer.set()
            print(f'Received DATA {block_num}')
            self.handle_WRQ(address, fc.filename, fc.block_num + 1, buf)

    def handle_ERROR(self, address, error_code, message):
        packet = struct.pack('>hh%dsb' % len((message + '\0').encode()), ERROR_MODE, error_code,
                             (message + '\0').encode(), 0)
        print(f'Sent ERROR {error_code}: {message}')
        self.sock.sendto(packet, address)

    def parse_packet(self, data, address):
        mode, rest = struct.unpack('>h', data[:2])[0], data[2:]
        if mode == RRQ_MODE:
            filename, mode = rest.decode().split('\0')[:2]
            if not os.path.exists(filename):
                self.handle_ERROR(address, FILE_NOT_FOUND_ERROR, "File doesn't exist")
                sys.exit()
            if address in self.reads.keys():
                self.reads[address].timer.set()
            print('Received RRQ')
            block_num = 1
            self.handle_RRQ(address, filename, block_num)

        elif mode == WRQ_MODE:
            filename, mode = rest.decode().split('\0')[:2]

            if address in self.writes.keys():
                self.writes[address].timer.set()
            print('Received WRQ')
            block_num = 0
            self.handle_WRQ(address, filename, block_num)

        elif mode == ACK_MODE:
            block_num = struct.unpack('>h', rest)[0]
            self.handle_ACK(address, block_num)

        elif mode == DATA_MODE:
            block_num, data = struct.unpack('>h', rest[:2])[0], rest[2:]
            self.handle_DATA(address, block_num, data)

        else:
            self.handle_ERROR(address, 0, 'Invalid packet type')

    def run_server(self):
        print('Server is running')
        while True:
            data, address = self.sock.recvfrom(2 * MAX_BYTES)

            if data:
                t = Thread(target=self.parse_packet, args=(data, address))
                t.start()


parser = argparse.ArgumentParser()
parser.add_argument('-ip', type=str)
parser.add_argument('-p', type=int)
parser.add_argument('-t', type=int)

arguments = parser.parse_args(sys.argv[1:])

host = gethostbyname(gethostname()) if arguments.ip is None else arguments.ip
port = 69 if arguments.p is None else int(arguments.p)
timeout = 1 if arguments.t is None else int(arguments.t)

sock = socket(AF_INET, SOCK_DGRAM)
sock.bind((host, port))
timeout /= 1000
print(f'Server is ready! <host> = {host}; <port> = {port}; <timeout> = {timeout}')

server = Server(sock, timeout)
server.run_server()

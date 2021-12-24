
import datetime
import socket

from math import ceil
from re import sub

TFTP_MODES = ['unknown', 'netascii', 'octet', 'mail']
                
TFTP_OPCODES = ['Unknown', 'Read', 'Write', 'Data', 'ACK', 'Error', 'Option_ACK']  # Unknown, RRQ, WRQ, DATA, ACKNOWLEDGMENT, ERROR, OACK

SERVER_ERROR_MSG = {
    0 : 'Not defined, see error message (if any)',
    1 : 'File not found',
    2 : 'Access violation',
    3 : 'Disk full or allocation exceeded',
    4 : 'Illegal TFTP operation',
    5 : 'Unknown transfer ID',
    6 : 'File already exists',
    7 : 'No such user' }

'''
                2 bytes    string    1 byte    string   1 byte         1 byte
               ---------------------------------------------------------------
      RRQ/WRQ  | 01/02 |  Filename  |  0  |    Mode    |  0  | OPTIONS |  0  |
               ---------------------------------------------------------------

               2 bytes    2 bytes     n bytes
              ---------------------------------
      DATA    |  03  |   Block #  |   Data    |
              ---------------------------------
              
              2 bytes   2 bytes
             ---------------------
      ACK   |  04   |   Block #  |
             ---------------------

             2 bytes  2 bytes       string    1 byte
             ---------------------------------------
      ERROR |  05  |  ErrorCode |   ErrMsg   |  0  |
             ---------------------------------------

                2 bytes          1 byte
               ------------------------
   Option_ACK  |  06  | OPTIONS |  0  |
               ------------------------

'''

DEFAULT_OPTIONS = {'timeout':3, 'tsize':1024, 'blksize':516}

class TFTP_client_class():

    def __init__(self, port):
        self.SERVER_ADDRESS = ('127.0.0.1', port)
        self.SOCKET = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.FLAG_OACK = True
        self.MAX_RETRY_COUNT = 5
        self.OPTIONS = DEFAULT_OPTIONS.copy()
        self.main(port)  

    def length_bytes(self, x):  
        return ceil((1 << (x - 1).bit_length()) / 8)

    def send_start_message(self, filename, req = 'RRQ', mode = 'netascii'):
        message  = (1).to_bytes(2, 'big')
        if req == 'WRQ': message = (2).to_bytes(2, 'big')
        message += filename.encode('ascii', 'replace')
        message += (0).to_bytes(1, 'big')
        message += mode.encode('ascii', 'replace')
        message += (0).to_bytes(1, 'big')

        if self.FLAG_OACK:
            option_list = list(self.OPTIONS.keys())
            for i in range(0, 3):
                message += option_list[i].encode('ascii')
                message += (0).to_bytes(1, 'big')
                if   i == 0: message += (self.OPTIONS['timeout']).to_bytes(1, 'big')
                elif i == 1:
                    tsize = self.OPTIONS['tsize']
                    message += (tsize).to_bytes(self.length_bytes(tsize), 'big')
                elif i == 2: message += (self.OPTIONS['blksize']).to_bytes(2, 'big')   
                message += (0).to_bytes(1, 'big')
        
        self.SOCKET.sendto(message, self.SERVER_ADDRESS)
        print(message)
        return 0

    def recv_OACK(self):
        (data, self.SERVER_ADDRESS) = self.SOCKET.recvfrom(30)
        opcode = int.from_bytes(data[:2], byteorder = 'big')
        if opcode != 6:
            return -1
        iter = 2
        print(data)
        while iter < len(data):
            option_index = data.find((0).to_bytes(1, 'big'), iter)
            option_name = data[iter:option_index].decode('ascii')
            iter = option_index + 1
            for i in range(iter, len(data)):
                if data[i:(i + 1)] == (0).to_bytes(1, 'big'):
                    x = i
                    if (i != len(data) - 1) and data[(i + 1):(i + 2)].decode('ascii') in ['t', 'b']: break
            option_value = int.from_bytes(data[iter:x], byteorder = 'big')
            iter = x + 1
            self.OPTIONS[option_name] = option_value
        return 0

    def tftp_read_RRQ(self, filename, mode = 'netascii'):
        self.SOCKET.settimeout(self.OPTIONS['timeout'] + 3)

        if self.FLAG_OACK:
            message = (4).to_bytes(2, 'big') + (0).to_bytes(2, 'big')
            self.SOCKET.sendto(message, self.SERVER_ADDRESS)

        retry_count = 0
        symbol = 'w'
        if mode == 'octet': symbol += 'b'
        with open(filename, symbol) as file:
            while True:
                try:
                    (data, self.SERVER_ADDRESS) = self.SOCKET.recvfrom(512)
                    print(data)
                    opcode = int.from_bytes(data[:2], byteorder = 'big')
                    if   opcode == 5:
                        return data[4:-1].decode('ascii')
                    elif opcode == 3:
                        number_block = int.from_bytes(data[2:4], byteorder = 'big')
                        data_write = data[4:]
                        if   mode == 'octet': file.write(data_write)
                        elif mode == 'netascii':
                            file.write(data_write.decode('ascii', 'replace').replace('\r\n', '\n'))
                    message = (4).to_bytes(2, 'big') + (number_block).to_bytes(2, 'big')
                    self.SOCKET.sendto(message, self.SERVER_ADDRESS)
                    print(message)
                    if 512 > len(data): break
                except socket.timeout:
                    retry_count += 1
                    if retry_count > self.MAX_RETRY_COUNT:
                        return '\n\tError! Connection loss'
                except FileNotFoundError:
                    return f'\n\tError! File <{filename}> does not exist!'
        return 0

    def tftp_write_WRQ(self, filename, mode = 'netascii'):
        self.SOCKET.settimeout(self.OPTIONS['timeout'] + 3)

        if not self.FLAG_OACK:
            (get_message, self.SERVER_ADDRESS) = self.SOCKET.recvfrom(64)
            print(get_message)
            if int.from_bytes(get_message[:2], byteorder = 'big') == 5:
                return get_message[4:-1].decode('ascii')
        
        symbol = 'r'
        if mode == 'octet': symbol += 'b'
        try:
            file = open('test.txt', symbol)
            data_bytes = file.read()
            file.close()
        except FileNotFoundError:
            return f'\n\tError! File <{filename}> does not exist!'
        if mode == 'netascii':
            data_bytes = data_bytes.replace('\n', '\r\n').encode('ascii', 'replace')
        number_block = 1
        start = 0
        retry_count = 0
        while True:
            try:
                buffer  = data_bytes[start:(start + 512)]
                message = (3).to_bytes(2, 'big') + (number_block).to_bytes(2, 'big') + buffer             
                self.SOCKET.sendto(message, self.SERVER_ADDRESS)
                print(message)
                get_message = self.SOCKET.recv(64)            
                print(get_message)
                if int.from_bytes(get_message[:2], byteorder = 'big') == 5:
                   return get_message[4:-1].decode('ascii')
                if int.from_bytes(get_message[2:4], byteorder = 'big') != number_block:
                   continue
                start += 512
                number_block += 1
                if len(message) < 512:
                    break
            except socket.timeout:
                retry_count += 1
                if retry_count > self.MAX_RETRY_COUNT:
                    return '\n\tError! Connection loss'
        return 0

    def parser_string(self, input_string):
        inp_str = sub(r'\s+|\t', ' ', input_string.strip().lower())
        input_array = inp_str.split(' ')
        mode = 'netascii'
        if len(input_array) < 3:
            return '\n\t Error! Unvalid input...'       
        if   input_array[0] in ['-w',  '--write', 'write']: req = 'WRQ'
        elif input_array[0] in ['-r',  '--read',   'read']: req = 'RRQ'
        else:
            print('\n\t Error! Invalid input!')
            return -1
        filename = input_array[1]
        if input_array[2] == 'octet': mode = 'octet'
        for i in range(3, len(input_array), 2):
            flag  = input_array[i]
            value = input_array[i + 1]
            try:
                if   flag in ['-t',  '--timeout']: self.OPTIONS['timeout'] = int(value)
                elif flag in ['-bs', '--blksize']: self.OPTIONS['blksize'] = int(value)
                elif flag in ['-ts', '--tsize']:   self.OPTIONS['tsize']   = int(value)
            except ValueError:
                print('\n\t Error! Invalid input!')
                return -1
        return (filename, req, mode)
   
    def main(self, port):
        while True:
            self.SERVER_ADDRESS = ('127.0.0.1', port)
            input_string = input('\n\t Please, inter the flags: > ')
            result_parser = self.parser_string(input_string)
            if result_parser == -1: continue
            else: filename, req, mode = result_parser
            self.send_start_message(filename, req, mode) 
            if self.FLAG_OACK:
                if self.recv_OACK() == -1:
                    print('\n\t Error! Unvalid Options or Filename')
                    continue
            if req == 'RRQ': res = self.tftp_read_RRQ(filename,  mode)
            else:            res = self.tftp_write_WRQ(filename, mode)
            if   res == -1: print('\n\t Error!!!')
            elif res != 0:  print(res)
            if self.FLAG_OACK: self.FLAG_OACK = False
            cont = input('\n\t Exit? (N or ANY OTHER): > ')
            if not cont.lower().startswith('n'): break
        print('')        
        self.SOCKET.close()
        return 0

if __name__ == '__main__':
    TFTP_client_class(69)
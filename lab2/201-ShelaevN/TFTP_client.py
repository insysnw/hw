
import datetime
import socket

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

PORT = 69

class TFTP_client_class():

    DEFAULT_OPTIONS = { 'timeout':3, 'tsize':1024, 'blksize':516 }

    def __init__(self, PORT):
        self.PORT = PORT
        self.SERVER_ADDRESS = ('127.0.0.1', PORT)
        self.SOCKET = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

        self.FLAG_OACK = True
        self.MAX_RETRY_COUNT = 5
        self.OPTIONS = self.DEFAULT_OPTIONS.copy()
        self.main()

    def send_start_message(self, filename, req = 'RRQ', mode = 'netascii'):
        message  = (1).to_bytes(2, 'big')
        if req == 'WRQ':
            message = (2).to_bytes(2, 'big')
            try:
                open(filename, 'rb').close()
            except FileNotFoundError:
                return f'\n   Error! File <{filename}> does not exist!'
        message += filename.encode('ascii', 'replace')
        message += (0).to_bytes(1, 'big')
        message += mode.encode('ascii', 'replace')
        message += (0).to_bytes(1, 'big')

        if self.FLAG_OACK:
            option_list = list(self.OPTIONS.keys())
            for i in range(0, 3):
                message += option_list[i].encode('ascii')
                message += (0).to_bytes(1, 'big')
                if   i == 0:
                    message += (self.OPTIONS['timeout']).to_bytes(1, 'big')
                elif i == 1:
                    tsize = self.OPTIONS['tsize']
                    message += str(tsize).encode('ascii')
                elif i == 2:
                    message += (self.OPTIONS['blksize']).to_bytes(2, 'big')   
                message += (0).to_bytes(1, 'big')
        
        self.SOCKET.sendto(message, self.SERVER_ADDRESS)
        return 0

    def recv_OACK(self):
        (data, self.SERVER_ADDRESS) = self.SOCKET.recvfrom(512)
        opcode = int.from_bytes(data[:2], byteorder = 'big')
        if opcode == 5:
            return ('\n   Server: ' + data[4:-1].decode('ascii'))
        iter = 2
        dataLength = len(data)
        while iter < dataLength:
            optionIndex = data.find((0).to_bytes(1, 'big'), iter)
            optionName = data[iter:optionIndex].decode('ascii')
            iter = optionIndex + 1
            for i in range(iter, dataLength):
                if data[i:(i + 1)] == (0).to_bytes(1, 'big'):
                    optionIndex = i
                    if (i != dataLength - 1) and data[(i + 1):(i + 2)].decode('ascii') in ['t', 'b']:
                        break         
            if optionName == 'tsize':
                optionValue = int(data[iter:optionIndex].decode('ascii'))
            else:
                optionValue = int.from_bytes(data[iter:optionIndex], byteorder = 'big')
            iter = optionIndex + 1
            self.OPTIONS[optionName] = optionValue
        return 0

    def tftp_read_RRQ(self, filename, mode = 'netascii'):
        self.SOCKET.settimeout(self.OPTIONS['timeout'] + 1)

        numberBlock = 0
        if self.FLAG_OACK:
            message = (4).to_bytes(2, 'big') + (numberBlock).to_bytes(2, 'big')
            self.SOCKET.sendto(message, self.SERVER_ADDRESS)

        retryCount = 0
        symbol = 'w'
        if mode == 'octet':
            symbol += 'b'
        with open(filename, symbol) as file:
            while True:
                try:
                    (data, self.SERVER_ADDRESS) = self.SOCKET.recvfrom(self.OPTIONS['blksize'] - 4)
                    opcode = int.from_bytes(data[:2], byteorder = 'big')
                    if opcode == 5:
                        return ('\n   Server: ' + data[4:-1].decode('ascii'))
                    numberBlock = int.from_bytes(data[2:4], byteorder = 'big')
                    if mode == 'octet':
                        file.write(data[4:])
                    else:
                        file.write(data[4:].decode('ascii', 'replace').replace('\r\n', '\n'))
                    message = (4).to_bytes(2, 'big') + (numberBlock).to_bytes(2, 'big')
                    self.SOCKET.sendto(message, self.SERVER_ADDRESS)
                    if len(data) < (self.OPTIONS['blksize'] - 4):
                        break
                except socket.timeout:
                    retryCount += 1
                    if retryCount > self.MAX_RETRY_COUNT:
                        return '\n   Error! Connection loss...' 
        return 0

    def tftp_write_WRQ(self, filename, mode = 'netascii'):
        self.SOCKET.settimeout(self.OPTIONS['timeout'] + 1)

        if not self.FLAG_OACK:
            (message, self.SERVER_ADDRESS) = self.SOCKET.recvfrom(self.OPTIONS['blksize'] - 4)
            if int.from_bytes(message[:2], byteorder = 'big') == 5:
                return ('\n   Server: ' + message[4:-1].decode('ascii'))
        
        symbol = 'r'
        if mode == 'octet':
            symbol += 'b'
        try:
            file = open(filename, symbol)
            dataBytes = file.read()
            file.close()
        except FileNotFoundError:
            return f'\n   Error! File <{filename}> does not exist!'
        if mode == 'netascii':
            dataBytes = dataBytes.replace('\n', '\r\n').encode('ascii', 'replace')

        numberBlock = 1
        startByte = 0
        retryCount = 0
        while True:
            try:
                buffer = dataBytes[startByte:(startByte + self.OPTIONS['blksize'] - 4)] 
                message = (3).to_bytes(2, 'big') + (numberBlock).to_bytes(2, 'big') + buffer
                self.SOCKET.sendto(message, self.SERVER_ADDRESS)
                messageAnswer = self.SOCKET.recv(self.OPTIONS['blksize'] - 4)            
                if int.from_bytes(messageAnswer[:2], byteorder = 'big') == 5:
                   return ('\n   Server: ' + messageAnswer[4:-1].decode('ascii'))
                if int.from_bytes(messageAnswer[2:4], byteorder = 'big') != numberBlock:
                   continue
                startByte += (self.OPTIONS['blksize'] - 4)
                numberBlock += 1
                if len(message) < (self.OPTIONS['blksize'] - 4):
                    break
            except socket.timeout:
                retryCount += 1
                if retryCount > self.MAX_RETRY_COUNT:
                    return '\n   Error! Connection loss'
        return 0

    def parser_string(self, input_string):
        input_array = sub(r'\s+|\t', ' ', input_string.strip().lower()).split(' ')
        mode = 'netascii'
        if len(input_array) < 3:
            return '\n   Error! Unvalid input...'
        
        if   input_array[0] in ['-w', '--write', 'write']:
            req = 'WRQ'
        elif input_array[0] in ['-r', '--read', 'read']:
            req = 'RRQ'
        else:
            return '\n   Error! Invalid input!'

        filename = input_array[1]
        if input_array[2] == 'octet':
            mode = 'octet'

        for i in range(3, len(input_array), 2):
            flag  = input_array[i]
            value = input_array[i + 1]
            try:
                if   flag in ['-t',  '--timeout']:
                    self.OPTIONS['timeout'] = abs(int(value))
                elif flag in ['-bs', '--blksize']:
                    self.OPTIONS['blksize'] = abs(int(value))
                elif flag in ['-ts', '--tsize']:
                    self.OPTIONS['tsize']   = abs(int(value))
            except ValueError:
                return '\n   Error! Invalid input with Options!'
        return (filename, req, mode)
   
    def main(self):
        while True:
            self.SERVER_ADDRESS = ('127.0.0.1', self.PORT)

            result_parser = self.parser_string(input('\n\t Please, inter the flags: > '))
            if len(result_parser) != 3:
                print(result_parser)
                continue
            else:
                filename, req, mode = result_parser

            resultMessage = self.send_start_message(filename, req, mode)
            if resultMessage != 0:
                print(resultMessage)
                continue

            if self.FLAG_OACK:
                result_OACK = self.recv_OACK()
                if result_OACK != 0:
                    print(result_OACK)
                    continue

            if req == 'RRQ':
                result = self.tftp_read_RRQ(filename,  mode)
            else:
                result = self.tftp_write_WRQ(filename, mode)
            if result != 0:
                print(result)
            self.FLAG_OACK = False
            continueFlag = input('\n   Exit? (N or ANY OTHER): > ')
            if not continueFlag.lower().startswith('n'):
                break
        print('')        
        self.SOCKET.close()
        return 0

if __name__ == '__main__':
    TFTP_client_class(PORT)

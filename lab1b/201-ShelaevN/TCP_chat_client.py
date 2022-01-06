
import socket
import random
import threading

from re import sub
from time import sleep
from datetime import timezone, datetime

PORT = 7575
ENCODING = 'utf-8'

'''
TCP_Protocol:
    Time(5 + 6 + 6 = 17) | Length_name(6) | Flag_file(1) | Name() | Message()
'''

class TCP_chat_client():

    def __init__(self, PORT, ENCODING):
        self.TIME_HOUR_DIFF = int(datetime.now(timezone.utc).astimezone().utcoffset().total_seconds() / 3600)

        self.FILE_PATH_FROM = '../../TCP_client_from'
        self.FILE_PATH_TO   = '../../TCP_client_to'
        self.FILE_SPLIT = '?/:'

        self.SOCKET = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.IP_ADDR = f'127.0.0.{random.randint(5, 254)}' 
        self.SOCKET.connect((self.IP_ADDR, PORT))
        self.BREAK_FLAG = False

        self.NAME = ''
        self.NAME_ENCODE = b''
        self.NAME_CHECK = False

        self.ENCODING = ENCODING
        self.main()

    def UTC2LocalTime(self, hour):
        hourLocalTime = hour + self.TIME_HOUR_DIFF
        if hourLocalTime > 23:
            hourLocalTime -= 24
        if hourLocalTime < 0:
            hourLocalTime += 24 
        return hourLocalTime

    def sendFile(self, file_name):
        filePath = self.FILE_PATH_FROM + '/' + file_name
        try:
            file = open(filePath, 'rb')
            data = file.read()
            file.close()
        except FileNotFoundError:
            print(f'\n   ERROR! Requested file "{filePath}" not found!')
            return None
        return ((file_name + self.FILE_SPLIT).encode(self.ENCODING) + data)

    def makeFile(self, message):
        message_split = message.split(self.FILE_SPLIT, 1)
        with open(self.FILE_PATH_TO + '/' + message_split[0], 'w') as file:
            file.writelines(message_split[1].split('\r'))
        return message_split[0]

    def printResult(self, time, name, message, flag_file):
        messageNew = message
        if flag_file:
            messageNew = '(file) ' + self.makeFile(message)
        if name == 'TCP-server':
            namePrint = ''
        elif name == self.NAME:
            namePrint = ' <You>:'
        else:
            namePrint = f' <{name}>:'
        print('\n\t\t\t[%2d:%2d:%2d]%s %s\n\t > ' %(time[0], time[1], time[2], namePrint, messageNew))
        return

    def message2Bit(self, message, lenght):
        return format(message, 'b').zfill(lenght)

    def makeMessage(self, flagFile, messageEncode = b''):
        time = datetime.now(timezone.utc).time()
        message_bit  = self.message2Bit(time.hour, 5) + self.message2Bit(time.minute, 6) + self.message2Bit(time.second, 6)
        message_bit += (self.message2Bit(len(self.NAME_ENCODE), 6) + format(flagFile, '1b'))
        message_send = int(message_bit, 2).to_bytes(3, byteorder = 'big') + self.NAME_ENCODE + messageEncode
        return message_send

    def inputName(self, message = ''):
        self.NAME = ''
        while self.NAME == '':
            self.NAME = input('\n\t Please, inter your ' + message + 'name: > ')
            self.NAME = sub(r'\s|\t|,|\.|\"', '_', self.NAME)
            if self.NAME == '':
                print('ERROR! Invalid name!')
            self.NAME_ENCODE = self.NAME.encode(self.ENCODING)
            if len(self.NAME_ENCODE) > 63:
                self.NAME == ''
                print('ERROR! The name is too long!')
        print(f'\n   Your name is <{self.NAME}>\n')
        try:
            self.SOCKET.sendall(self.makeMessage(False))
        except ConnectionError as e:
            print('\n   Error! Connection loss...')
            self.BREAK_FLAG = True
        return

    def thread_send_message(self):
        print('\n   Your name has been verified, you can type messages...\n')
        while not self.BREAK_FLAG:
            flagFile = False
            message = input('\t > ')
            if message == '':
                print('\n   ERROR! Empty message!')
                continue
            if message.lower() in ['-q', '--quit', 'exit']:
                self.BREAK_FLAG = True
            flagCheck = message.split(' ', 1)
            messageEncode = message.encode(self.ENCODING)
            if flagCheck[0].lower() in ['-f', '--file']:
                flagFile = True
                messageEncode = self.sendFile(flagCheck[1])
                if messageEncode is None:
                    continue 
            try:
                self.SOCKET.sendall(self.makeMessage(flagFile, messageEncode))
            except ConnectionError as e:
                self.BREAK_FLAG = True
                break
        return

    def stream_parser(self, data):
        dataTime = bin(int.from_bytes(data[:3], byteorder = 'big') >> 7)[2:]
        time = (self.UTC2LocalTime(int(dataTime[:5], 2)), int(dataTime[5:11], 2), int(dataTime[11:], 2))
        length_name = ((int.from_bytes(data[2:3], byteorder = 'big') % 128) >> 1) + 3
        flag_file = int.from_bytes(data[2:3], byteorder = 'big') % 2
        name = data[3:length_name].decode(self.ENCODING)
        message = data[length_name:].decode(self.ENCODING)
        if not self.NAME_CHECK and name == 'TCP-server' and message == 'ERROR-Name':
            print('\n   ERROR! This name is already taken, please choose another name')
            self.inputName('new ')
            return 
        else:
            self.NAME_CHECK = True
            self.printResult(time, name, message, flag_file)
        return

    def thread_get_message(self):
        while not self.BREAK_FLAG:
            data_full = b''
            while True:
                try:
                    data = self.SOCKET.recv(255)
                    data_full += data
                    if len(data) < 255:
                        break
                except ConnectionError as e:
                    print(f'\n   Warning! Lost connection...!')
                    self.BREAK_FLAG = True
                    self.NAME_CHECK = True
                    break
            if len(data_full) > 0:
                self.stream_parser(data_full)
        return

    def main(self):

        threadGet = threading.Thread(target = self.thread_get_message)
        threadGet.start()

        self.inputName()
        while not self.NAME_CHECK:
            sleep(1)

        self.thread_send_message()
        
        threadGet.join()

        self.SOCKET.close()
        return 0

if __name__ == '__main__':
   TCP_chat_client(PORT, ENCODING)

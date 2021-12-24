
import socket
import random
import threading

from re import sub
from datetime import timezone, datetime

'''
Protocol:
    Time(5 + 6 + 6 = 17) | Length_name(6) | Flag_file(1) | Name() | Message()
'''

class TCP_chat_client():

    def __init__(self):
        self.TIME_DIFF = int(datetime.now(timezone.utc).astimezone().utcoffset().total_seconds() / 3600)

        self.FILE_PATH = '../TCP_client'
        self.FILE_SPLIT = '?/:'

        self.SOCKET = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.IP_ADDR = f'127.0.0.{random.randint(5, 255)}' 
        self.SOCKET.connect((self.IP_ADDR, 7575))
        self.BREAK_FLAG = False
        self.main()

    def UTC2LocalTime(self, hour):
        hourLocalTime = hour + self.TIME_DIFF
        if hourLocalTime > 23:
            hourLocalTime -= 24
        if hourLocalTime < 0:
            hourLocalTime += 24 
        return hourLocalTime

    def bitToInt(self, data):
        result = 0
        for i in range(0, len(data)):
            result = result * 2 + int(data[i])
        return result

    def byte_decode(self, data_bin):
        res_str = ''
        for i in range(0, len(data_bin), 8):
            res_str += int(data_bin[i:(i + 8)], 2).to_bytes(1, byteorder = 'big').decode('utf-8') 
        return res_str

    def bit_zfill(self, mess, length):
        return format(mess, 'b').zfill(length)

    def gen_message(self, flag_file, name_encode, message_encode = b''):
        time = datetime.now(timezone.utc).time()
        message_bit  = self.bit_zfill(time.hour, 5) + self.bit_zfill(time.minute, 6) + self.bit_zfill(time.second, 6)
        message_bit += (self.bit_zfill(len(name_encode), 6) + format(flag_file, '1b'))
        message_send = b''
        for i in range(0, 24, 8):
            message_send += int(message_bit[i:(i + 8)], 2).to_bytes(1, byteorder = 'big')
        message_send += (name_encode + message_encode)
        return message_send

    def input_name(self, message = ''):
        name = ''
        while name == '':
            name = input('\n\t Please, inter your ' + message +  'name: > ')
            name = sub(r'\s|\t|,|\.|\"', '_', name)
            if name == '':
                print('ERROR! Invalid name!')
            name_encode = name.encode('utf-8')
            if len(name_encode) > 63:
                name == ''
                print('ERROR! The name is too long!')
        print(f'\n Your name is <{name}>\n')
        return name_encode

    def get_answer(self, name_encode):
        data = self.SOCKET.recv(24)
        data_hex = data.hex()
        data_bin = (bin(int(data_hex[0], 16))[2:]).zfill(4)
        for i in range(1, len(data_hex)):
            data_bin += (bin(int(data_hex[i], 16))[2:]).zfill(4)
        length_name = self.bitToInt(data_bin[17:23]) * 8
        name_end = 24 + length_name
        name = self.byte_decode(data_bin[24:name_end])    
        message = self.byte_decode(data_bin[name_end:])
        if name == 'TCP-server' and message == 'ERROR-Name':
            print('\n\t ERROR! This name is already taken, please choose another name')
            return self.input_name('new ')
        return name_encode

    def send_file(self, file_name):
        file_path = self.FILE_PATH + '/' + file_name
        file_name_encode = (file_name + self.FILE_SPLIT).encode('utf-8')
        try:
            with open(file_path, 'rb') as file:
                data = file.read()
        except FileNotFoundError:
            print(f'\n\t ERROR! Requested file "{file_path}" not found!')
            return None
        return (file_name_encode + data)

    def check_name(self):
        name_encode = self.input_name()
        message_send = self.gen_message(False, name_encode)
        try:
            self.SOCKET.sendall(message_send)
            name_encode = self.get_answer(name_encode)
        except ConnectionError as e:
            return -1
        return name_encode

    def thread_send_message(self, sock, name_encode):
        while not self.BREAK_FLAG:
            flag_file = False
            message = input('\t > ')
            if message == '':
                print(' ERROR! Empty message!')
                continue
            flag_check = message.split(' ', 1)
            if message.lower() in ['-q',  'quit', 'exit']:
                self.BREAK_FLAG = True
            message_encode = message.encode('utf-8')
            if flag_check[0].lower() in ['-f', '--file']:
                flag_file = True
                message_encode = self.send_file(flag_check[1])
                if message_encode is None:
                    continue 
            message_send = self.gen_message(flag_file, name_encode, message_encode)
            # print(message_send)
            try: sock.sendall(message_send)
            except ConnectionError as e: break
        return

    def print_result(self, time, name, message, flag_file):
        if flag_file:
            message = '(file) ' + message.split(self.FILE_SPLIT, 1)[0]
        namePrint = name
        if name == 'TCP-server': namePrint = ''
        print('\n\t\t\t[%2d:%2d:%2d] <%s>: %s' %(time[0], time[1], time[2], namePrint, message))
        print('\t > ')
        return

    def stream_parser(self, data):
        data_hex = data.hex()
        data_bin = (bin(int(data_hex[0], 16))[2:]).zfill(4)
        for i in range(1, len(data_hex)):
            data_bin += (bin(int(data_hex[i], 16))[2:]).zfill(4)
        time = (self.UTC2LocalTime(self.bitToInt(data_bin[:5])), self.bitToInt(data_bin[5:11]), self.bitToInt(data_bin[11:17]))
        length_name = self.bitToInt(data_bin[17:23]) * 8
        flag_file = self.bitToInt(data_bin[23:24])
        name_end = 24 + length_name
        name = self.byte_decode(data_bin[24:name_end])    
        message = self.byte_decode(data_bin[name_end:])
        self.print_result(time, name, message, flag_file)
        return

    def thread_recv_message(self, sock):
        while not self.BREAK_FLAG:
            data_full = b''
            while True:
                try:
                    data = sock.recv(255)
                    if data: data_full += data
                    else: break
                except socket.timeout: break
            if len(data_full) > 0: self.stream_parser(data_full)
        return

    def main(self):
        name_encode = self.check_name()
        if name_encode == -1:
            print('\n\t Error! Connection loss...')
            return
        print('\tYour name has been verified, you can type messages...\n')

        thread_send = threading.Thread(target = self.thread_send_message, args = (self.SOCKET, name_encode))
        thread_recv = threading.Thread(target = self.thread_recv_message, args = (self.SOCKET,))
        thread_send.start()
        thread_recv.start()
        thread_send.join()
        thread_recv.join()

        self.SOCKET.close()
        return 0

if __name__ == '__main__':
   TCP_chat_client()

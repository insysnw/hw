
import socket
import threading

from datetime import timezone, datetime

'''
Protocol:
    Time(5 + 6 + 6 = 17) | Length_name(6) | Flag_file(1) | Name() | Message()
'''

class TCP_chat_sync_server():

    def __init__(self):
        self.FILE_SPLIT = '?/:'

        self.SOCKET = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.SOCKET.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.SOCKET.bind(('', 7575))
        self.SOCKET.listen(5)

        self.CLIENTS = {'TCP-server':'-'}
        self.main()

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

    def send_answer(self, connect, message):
        time = datetime.now(timezone.utc).time()
        name_encode = 'TCP-server'.encode('utf-8')
        message_encode = message.encode('utf-8')
        message_bit  = self.bit_zfill(time.hour, 5) + self.bit_zfill(time.minute, 6) + self.bit_zfill(time.second, 6)
        message_bit += (self.bit_zfill(len(name_encode), 6) + format(False, '1b'))
        message_send = b''
        for i in range(0, 24, 8):
            message_send += int(message_bit[i:(i + 8)], 2).to_bytes(1, byteorder = 'big')
        message_send += (name_encode + message_encode)
        try:
            connect.sendall(message_send)
        except ConnectionError as e:
            pass
        return

    def makeNewData(self, data, message):
        name_encode = 'TCP-server'.encode('utf-8')
        message_encode = message.encode('utf-8')
        message_bit = data[:17] + self.bit_zfill(len(name_encode), 6) + format(False, '1b')
        dataNew = b''
        for i in range(0, 24, 8):
            dataNew += int(message_bit[i:(i + 8)], 2).to_bytes(1, byteorder = 'big')
        dataNew += (name_encode + message_encode)
        return dataNew

    def stream_parser(self, data, connect):
        data_hex = data.hex()
        data_bin = (bin(int(data_hex[0], 16))[2:]).zfill(4)
        for i in range(1, len(data_hex)):
            data_bin += (bin(int(data_hex[i], 16))[2:]).zfill(4)
        length_name = self.bitToInt(data_bin[17:23]) * 8 
        name = self.byte_decode(data_bin[24:(24 + length_name)])    
        # print(connect.getsockname())
        if name in self.CLIENTS.keys() and self.CLIENTS[name] != connect:
            self.send_answer(connect, 'ERROR-Name')
            return
        dataNew = data
        if name not in self.CLIENTS.keys():
            self.CLIENTS.setdefault(name, connect)
            dataNew = self.makeNewData(data_bin, f'\t <{name}> joined the chat!')
        if message.lower() in ['-q', 'quit', 'exit']:
            self.CLIENTS.pop(name)
            dataNew = self.makeNewData(data_bin, f'\t <{name}> exited from the chat!')
        # print(dataNew)
        for client in self.CLIENTS:
            if client != 'TCP-server':
                self.CLIENTS[client].sendall(dataNew)
        return

    def thread_transmitter(self, connect):
        data_full = b''
        while True:
            try:
                data = connect.recv(255)
                if data:
                    connect.settimeout(2)
                    data_full += data
            except socket.timeout: break
        if len(data_full) > 0: self.stream_parser(data_full, connect)
        return

    def main(self):
        thread_transm = []
        print('\n\t TCP server ready!\n')
        while True:
            try:
                # input() 
                conn, addr = self.SOCKET.accept()
                transmitter = threading.Thread(target = self.thread_transmitter, args = (conn,))
                transmitter.start()
                thread_transm.append(transmitter)
            except KeyboardInterrupt:
                print('\n\t TCP server stopped!\n')
                break  
        for thread in thread_transm:
            thread.join()
        self.SOCKET.close()
        return 0
    
if __name__ == '__main__':
    TCP_chat_sync_server()

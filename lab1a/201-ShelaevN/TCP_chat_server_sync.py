
import socket
import threading

from datetime import timezone, datetime

PORT = 7575
ENCODING = 'utf-8'

'''
TCP_Protocol:
    Time(5 + 6 + 6 = 17) | Length_name(6) | Flag_file(1) | Name() | Message()
'''

class TCP_chat_sync_server():

    def __init__(self, PORT, ENCODING):

        self.SOCKET = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.SOCKET.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.SOCKET.bind(('', PORT))
        self.SOCKET.listen(5)

        self.NAME = 'TCP-server'
        self.CLIENTS = {self.NAME:'-'}

        self.ENCODING = ENCODING
        self.main()

    def message2Bit(self, message, lenght):
        return format(message, 'b').zfill(lenght)

    def makeNewTime(self):
        time = datetime.now(timezone.utc).time()
        newData = self.message2Bit(time.hour, 5) + self.message2Bit(time.minute, 6) + self.message2Bit(time.second, 6)
        return newData

    def makeNewDataWithoutTime(self, dataTime, name, message):
        messageBit = dataTime + self.message2Bit(len(name), 6) + format(False, '1b')
        dataNew = int(messageBit, 2).to_bytes(3, byteorder = 'big') + name.encode(self.ENCODING) + message.encode(self.ENCODING)
        return dataNew

    def clientErrorDelete(self, name, dataTime, clientError):
        dataNew = self.makeNewDataWithoutTime(dataTime, name, '-q')
        self.stream_parser(dataNew, self.CLIENTS[name], clientError)
        return

    def sendMessage(self, connect, name, message):
        try:
            connect.sendall(message)
        except ConnectionError as e:
            print(f'\n   Warning! Lost connection with <{name}>!')
            return 1
        return 0

    def stream_parser(self, data, connect, nameError = []):
        length_name = ((int.from_bytes(data[2:3], byteorder = 'big') % 128) >> 1) + 3
        name = data[3:length_name].decode(self.ENCODING)
        message = data[length_name:].decode(self.ENCODING)
        if name in self.CLIENTS.keys() and self.CLIENTS[name] != connect:
            dataNew = self.makeNewDataWithoutTime(self.makeNewTime(), self.NAME, 'ERROR-Name')
            self.sendMessage(connect, name, dataNew)
            return True
        dataTime = bin(int.from_bytes(data[:3], byteorder = 'big') >> 7)[2:]
        dataNew = data
        if name not in self.CLIENTS.keys():
            self.CLIENTS.setdefault(name, connect)
            dataNew = self.makeNewDataWithoutTime(dataTime, self.NAME, f'<{name}> joined the chat!')
        flagWork = True
        if message.lower() in ['-q', 'quit', 'exit']:
            flagWork = False
            if name in nameError:
                dataNew = self.makeNewDataWithoutTime(dataTime, self.NAME, f'Lost connection with <{name}>!')
            else:
                dataNew = self.makeNewDataWithoutTime(dataTime, self.NAME, f'<{name}> exited from the chat!')
                self.sendMessage(self.CLIENTS[name], name, dataNew)
            self.CLIENTS.pop(name)
        clientError = []
        for client in self.CLIENTS.keys():
            if client != 'TCP-server' and client not in nameError:
                if self.sendMessage(self.CLIENTS[client], client, dataNew):
                    clientError.append(client)
        for name in clientError:
            self.clientErrorDelete(name, dataTime, clientError)
        return flagWork

    def thread_transmitter(self, connect):
        flagWork = True
        while flagWork:
            data_full = b''
            while True:
                try:
                    data = connect.recv(255)
                    data_full += data
                    if len(data) < 255:
                        break
                except ConnectionError as e:
                    flagWork = False
                    print(f'\n   Warning! Lost connection...!')
                    break
            if len(data_full) > 0:
                flagWork = self.stream_parser(data_full, connect)
        return

    def main(self):
        thread_transm = []
        print('\n\t TCP server (sync) ready!\n')
        while True:
            try: 
                connect, addr = self.SOCKET.accept()
                transmitter = threading.Thread(target = self.thread_transmitter, args = (connect,))
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
    TCP_chat_sync_server(PORT, ENCODING)

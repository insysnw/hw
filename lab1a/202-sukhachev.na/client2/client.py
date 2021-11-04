import threading
import socket
from datetime import datetime
import signal
import os
import re

HEADER_LENGTH = 10
SEPARATOR = "<SEPARATOR>"
SEND_FILE = 2**72
byteorder = "big"
UID = "84ecf7f7-674a-48c2-b09a-b7c6eea66d75".encode("utf-8")
IP = "127.0.0.1"
PORT = 10000

CONNECT = "CONNECT"
DISCONNECT = "DISCONNECT"
CONNECTION = 2**64


def main():
    nickname = input("Напишите имя для чата: ")
    clientsocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    clientsocket.connect((IP, PORT))

    def catch_interrupt(signal, frame):
        clientsocket.shutdown(socket.SHUT_WR)
        clientsocket.close()
        os._exit(0)

    signal.signal(signal.SIGINT, catch_interrupt)
    nickname_code = nickname.encode('utf-8')
    nickname_header = len(nickname_code).to_bytes(HEADER_LENGTH,byteorder)
    clientsocket.send(nickname_header + nickname_code)
    receive_thread = threading.Thread(target=getMessage, args=(clientsocket,))
    receive_thread.start()
    try:
        while True:
            message = input()
            if re.match("^send .*\.\w*\s*$", message):
                try:
                    files = re.findall("\s+\w.*\.\w*\s*", message)
                    fileName = files[0].strip()
                    filesize = os.path.getsize(fileName)
                    fileHeader = f"{files[0]}{SEPARATOR}{filesize}".encode()
                    clientsocket.send((SEND_FILE+len(fileHeader)).to_bytes(HEADER_LENGTH,byteorder))
                    clientsocket.send(fileHeader)
                    f = open(fileName, "rb")
                    bytes_read = f.read(filesize)
                    clientsocket.sendall(bytes_read)
                    f.close()
                    clientsocket.send(UID)
                except:
                    print(f'Невозможно найти файл {fileName}')

            else:
                if message:
                    message_code = message.encode('utf-8')
                    message_header = len(message_code).to_bytes(HEADER_LENGTH,byteorder)
                    clientsocket.send(
                        message_header + message_code )
    except:
        os._exit(0)


def getMessage(clientsocket):
    while True:
        nickname_header = clientsocket.recv(HEADER_LENGTH)
        nickname_length = int.from_bytes(nickname_header, byteorder)
        current_time = datetime.now().strftime("%H:%M")
        if len(nickname_header) == 0:
            print("Закрываю соединение")
            clientsocket.shutdown(socket.SHUT_WR)
            clientsocket.close()
            os._exit(0)
        if nickname_length > SEND_FILE:
            name = clientsocket.recv(nickname_length- SEND_FILE).decode()
            file_header = clientsocket.recv(HEADER_LENGTH)
            file_header_len = int.from_bytes(file_header,byteorder)
            file_header = clientsocket.recv(file_header_len)
            filename, filesize = file_header.decode('utf-8').split(SEPARATOR)
            filename = os.path.basename(filename)
            filesize = int(filesize)
            f = open(filename, "wb")
            total_bytes = bytes()
            while not UID in total_bytes:
                bytes_read = clientsocket.recv(filesize)
                total_bytes += bytes_read
            f.write(total_bytes[:(len(total_bytes)-len(UID))])
            f.close()
            print(f'<{current_time}> {name} отправляет файл {filename}')
        else:
            if nickname_length > CONNECTION:
                warning = clientsocket.recv(nickname_length - CONNECTION).decode('utf-8')
                print(f'{warning}')
                continue
            nickname = clientsocket.recv(nickname_length).decode('utf-8')
            message_header = clientsocket.recv(HEADER_LENGTH)
            message_length = int.from_bytes(message_header, byteorder)
            message = clientsocket.recv(message_length).decode('utf-8')
            print(f'<{current_time}> [{nickname}]: {message}')

if __name__ == '__main__':
    main()



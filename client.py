import datetime
import socket
import sys
import threading
import time
from threading import Thread

# host and port choosing
if len(sys.argv) == 3:
    host = sys.argv[1]
    port = int(sys.argv[2])
else:
    host = '127.0.0.1'
    port = 9090
address = (host, port)

# connect to server
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.connect(address)
print("Welcome to TCP chat!")

# choosing nickname
input_name: str = input("Please, enter your nickname: ")
nickname: bytes = input_name.encode('utf-8')
nickname_len: bytes = len(nickname).to_bytes(2, byteorder='big')


def get_package(sock):
    header: bytes = sock.recv(2)
    length: int = int.from_bytes(header, byteorder='big', signed=False)
    return sock.recv(length).decode('utf-8')


# send to server
def write():
    while True:
        message: str = input()
        enc_message: bytes = message.encode('utf-8')
        message_len: bytes = len(enc_message).to_bytes(2, byteorder='big')
        server.send(nickname_len + nickname)
        server.send(message_len + enc_message)
        if message == ":q":
            print("Disconnected")
            close()


def receive():
    while True:
        try:
            message_time: int = int.from_bytes(server.recv(4), byteorder='big', signed=False)
            message_time -= time.timezone
            loc_time: datetime = datetime.datetime.fromtimestamp(message_time)
            nick: str = get_package(server)
            message: str = get_package(server)
            print('{} | {}: {}'.format(loc_time.strftime('%H:%M:%S'), nick, message))

        except Exception as e:
            close()


def close():
    server.close()
    sys.exit()


# start thread for listening and sending
receive_thread: Thread = threading.Thread(target=receive)
receive_thread.start()

write_thread: Thread = threading.Thread(target=write)
write_thread.start()

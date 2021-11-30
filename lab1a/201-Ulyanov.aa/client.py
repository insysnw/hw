import socket
from threading import Thread
from datetime import datetime
from pytz import utc
from hashlib import sha256
from time import sleep

PORT = 5050
HOST = socket.gethostbyname(socket.gethostname())
ADDRESS = (HOST, PORT)

SIZE = 1048576
FORMAT = 'utf-8'
FILE_MESSAGE = '!send'
EXIT_MESSAGE = '!exit'

client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client.connect(ADDRESS)
connected = True


def send():
    global connected
    while connected:
        try:
            msg = input()
            msg_time = str(datetime.utcnow())
            message = (msg_time + '|' + msg).encode(FORMAT)

            if msg.strip() == EXIT_MESSAGE:
                exit_msg = (msg_time + '|' + msg).encode(FORMAT)
                client.send(exit_msg)
                connected = False
            elif msg.startswith(FILE_MESSAGE):
                try:
                    send_msg = (msg_time + '|' + msg).encode(FORMAT)
                    client.send(send_msg)
                    file_name = msg.split(' ')[1]
                    with open(file_name, 'rb') as file:
                        data = file.read()
                        sha = sha256(data).hexdigest()
                        text = (sha + '|').encode(FORMAT) + data
                        client.send(text)
                except Exception:
                    print('Some problem with file')
            else:
                client.send(message)
        except EOFError:
            break


def listen():
    global connected
    while connected:
        try:
            msg_str_time, message = client.recv(SIZE).decode(FORMAT).split('|', maxsplit=1)

            msg_time = datetime.strptime(msg_str_time, '%Y-%m-%d %H:%M:%S.%f').replace(tzinfo=utc).astimezone()
            time_to_msg = f"{msg_time.hour:02}:{msg_time.minute:02}:{msg_time.second:02}"

            if message.split(' ')[0].startswith(FILE_MESSAGE):
                filename = message.split(' ')[1]
                sha, data = client.recv(SIZE).split('|'.encode(FORMAT), maxsplit=1)

                msg_str_time, message = client.recv(SIZE).decode(FORMAT).split('|', maxsplit=1)

                msg_time = datetime.strptime(msg_str_time, '%Y-%m-%d %H:%M:%S.%f').replace(tzinfo=utc).astimezone()
                time_to_msg = f"{msg_time.hour:02}:{msg_time.minute:02}:{msg_time.second:02}"

                if sha.decode(FORMAT) != sha256(data).hexdigest():
                    print("Couldn't read the file")
                else:
                    with open(filename, 'wb') as file:
                        file.write(data)
                    print(f"<{time_to_msg}> {message}")
            else:
                print(f"<{time_to_msg}> {message}")
        except Exception:
            continue


send_thread = Thread(target=send)
send_thread.start()

listen_thread = Thread(target=listen)
listen_thread.start()

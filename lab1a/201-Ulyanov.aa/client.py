import sys
import socket
from threading import Thread
from datetime import datetime
from pytz import utc
from hashlib import sha256

PORT = 5050
HOST = socket.gethostbyname(socket.gethostname())
ADDRESS = (HOST, PORT)

HEADER_LENGTH = 20
SIZE = 1048576
FORMAT = 'utf-8'
FILE_MESSAGE = '!send'
EXIT_MESSAGE = '!exit'

client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client.connect(ADDRESS)
connected = True


def get_message(s):
    header = s.recv(HEADER_LENGTH)
    if not len(header):
        print("The connection to the server has been dropped.")
        client.close()
        sys.exit()
    size = int.from_bytes(header, byteorder='big', signed=False)
    data = s.recv(size)
    if not len(data):
        print("The connection to the server has been dropped.")
        client.close()
        sys.exit()
    return data


def send():
    global connected
    while connected:
        try:
            msg = input()
            message = msg.encode(FORMAT)
            message_header = len(message).to_bytes(HEADER_LENGTH, byteorder='big')

            if msg.strip() == EXIT_MESSAGE:
                exit_msg = EXIT_MESSAGE.encode(FORMAT)
                exit_msg_header = len(exit_msg).to_bytes(HEADER_LENGTH, byteorder='big')
                client.send(exit_msg_header + exit_msg)
                connected = False

            elif msg.startswith(FILE_MESSAGE):
                try:
                    client.send(message_header + message)
                    file_name = msg.split(' ')[1]
                    with open(file_name, 'rb') as read_file:
                        read_file_data = read_file.read()
                        read_file_data_header = len(read_file_data).to_bytes(HEADER_LENGTH, byteorder='big')

                        sha = sha256(read_file_data).hexdigest().encode(FORMAT)
                        sha_header = len(sha).to_bytes(HEADER_LENGTH, byteorder='big')

                        client.send(sha_header + sha + read_file_data_header + read_file_data)
                except Exception:
                    print('Some problem with file')
            else:
                client.send(message_header + message)
        except EOFError:
            break


def listen():
    global connected
    while connected:
        try:
            time = get_message(client).decode(FORMAT)
            nick = get_message(client).decode(FORMAT)
            message = get_message(client).decode(FORMAT)

            msg_time = datetime.strptime(time, '%Y-%m-%d %H:%M:%S.%f').replace(tzinfo=utc).astimezone()
            time_to_msg = f"{msg_time.hour:02}:{msg_time.minute:02}:{msg_time.second:02}"

            if message.strip().split(' ')[0].startswith(FILE_MESSAGE):
                sha = get_message(client).decode(FORMAT)
                data = get_message(client)
                filename = message.strip().split(' ')[1]

                if sha != sha256(data).hexdigest():
                    print("Couldn't read the file")
                else:
                    with open(filename, 'wb') as file:
                        file.write(data)
            else:
                print(f"<{time_to_msg}> [{nick}]: {message}")
        except Exception:
            continue


send_thread = Thread(target=send)
send_thread.start()

listen_thread = Thread(target=listen)
listen_thread.start()

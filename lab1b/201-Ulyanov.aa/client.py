import socket
import sys
from time import sleep
from errno import EAGAIN, EWOULDBLOCK
from threading import Thread
from datetime import datetime
from pytz import utc
from hashlib import sha256

HEADER_LENGTH = 20
FORMAT = 'utf-8'
FILE_MESSAGE = '!send'
EXIT_MESSAGE = '!exit'

PORT = 5050
HOST = socket.gethostbyname(socket.gethostname())
ADDRESS = (HOST, PORT)

client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client.connect(ADDRESS)
connected = True
client.setblocking(False)
next_file_msg = 0

nickname = input("Choose your nickname: ").encode(FORMAT)
nickname_header = len(nickname).to_bytes(HEADER_LENGTH, byteorder='big')
client.send(nickname_header + nickname)


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


def send_message():
    global connected
    while connected:
        message = input()
        if message == EXIT_MESSAGE:
            connected = False

        if message:
            enc_message = message.encode(FORMAT)
            enc_message_header = len(enc_message).to_bytes(HEADER_LENGTH, byteorder='big')
            client.send(enc_message_header + enc_message)
            if message.startswith(FILE_MESSAGE):
                sleep(0.5)
                read_filename = message.split(' ')[1]
                with open(read_filename, 'rb') as read_file:
                    read_file_data = read_file.read()
                    read_file_data_header = len(read_file_data).to_bytes(HEADER_LENGTH, byteorder='big')
                    sha = sha256(read_file_data).hexdigest().encode(FORMAT)
                    sha_header = len(sha).to_bytes(HEADER_LENGTH, byteorder='big')
                    client.send(sha_header + sha + read_file_data_header + read_file_data)


sending_thread = Thread(target=send_message, daemon=True)
sending_thread.start()

while connected:
    try:
        time_utc = get_message(client).decode(FORMAT)
        msg_time = datetime.strptime(time_utc, '%Y-%m-%d %H:%M:%S.%f').replace(tzinfo=utc).astimezone()
        time_to_msg = f"{msg_time.hour:02}:{msg_time.minute:02}:{msg_time.second:02}"

        nick = get_message(client).decode(FORMAT)

        if next_file_msg == 0:
            msg = get_message(client).decode(FORMAT)

            if msg == EXIT_MESSAGE:
                continue
            elif msg.startswith(FILE_MESSAGE):
                next_file_msg = 2
                filename = msg.split(' ')[1]
            else:
                print(f"<{time_to_msg}> [{nick}]: {msg}")

        elif next_file_msg == 1:
            file_data = get_message(client)
            if file_sha != sha256(file_data).hexdigest():
                print(f"Couldn't read the file. \nSHA = {file_sha}\nData = {file_data}")
            else:
                with open(filename, 'wb') as file:
                    file.write(file_data)
                print(f"<{time_to_msg}> {nick} send {filename}")
            next_file_msg = 0

        elif next_file_msg == 2:
            file_sha = get_message(client).decode(FORMAT)
            next_file_msg = 1

    except IOError as e:
        if e.errno != EAGAIN and e.errno != EWOULDBLOCK:
            print(f"Error: {e}")
            client.close()
            sys.exit()

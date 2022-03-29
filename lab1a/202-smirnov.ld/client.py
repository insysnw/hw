import socket
import struct
import threading
from datetime import datetime
import sys
sys.path.append('..')
from utils import get_actual_name


def safe_recv(size: int):
    msg = bytes()
    while size != 0:
        try:
            msg_tmp = socket.recv(size)
        except (Exception,):
            print("Connection closed")
            disconnect_event.set()
            break
        size -= len(msg_tmp)
        msg += msg_tmp
    return msg


def client_recv():
    (time_size,) = struct.unpack(">I", safe_recv(4))
    (name_size,) = struct.unpack(">I", safe_recv(4))
    (msg_size,) = struct.unpack(">I", safe_recv(4))
    time = safe_recv(time_size).decode()
    name = safe_recv(name_size).decode()
    msg = safe_recv(msg_size - 1).decode()
    flag = safe_recv(1)[0]
    if flag:
        (path_size,) = struct.unpack(">I", safe_recv(4))
        (file_size,) = struct.unpack(">I", safe_recv(4))
        file_path = safe_recv(path_size).decode()
        file = safe_recv(file_size)

        actual_path = get_actual_name(file_path)
        f = open(actual_path, "w+b")
        f.write(file)
        f.close()
    else:
        actual_path = None
    return time, name, msg, actual_path


def print_msg(time, name_msg, msg, file_path):
    time = str(datetime.fromisoformat(time).astimezone().strftime('%H:%M'))
    if file_path is not None:
        s = f' ({file_path} attached)'
    else:
        s = ""
    print(f'\n<{time}>[{name_msg}] {msg}{s}')


def client_send(msg: bytes, path=None):
    flag = 1 * (path is not None)
    msg = bytearray(msg)
    msg.append(flag)
    msg = bytes(msg)
    msg_size = len(msg)

    msg_packed_size = struct.pack(">I", msg_size)

    socket.send(msg_packed_size)
    socket.send(msg)
    if path is not None:
        f = open(path, "rb")
        file = f.read()
        f.close()

        path = path.encode()
        path_packed_size = struct.pack(">I", len(file_path))
        file_packed_size = struct.pack(">I", len(file))

        socket.send(path_packed_size)
        socket.send(file_packed_size)
        socket.send(path)
        socket.send(file)


def receive_and_print():
    while True:
        msg_time, msg_name, msg_text, msg_file_path = client_recv()
        print_msg(msg_time, msg_name, msg_text, msg_file_path)


max_name_len = 32

while True:
    address = str(input(f'Enter server address: '))
    port = int(input(f'Enter server port: '))
    name = input(f'Enter your name({max_name_len} symbols max):').encode()
    if len(name) > max_name_len:
        print("Name is too long")
    else:
        socket = socket.socket()
        socket.connect((address, port))
        client_send(name)
        break

disconnect_event = threading.Event()
listenThread = threading.Thread(target=receive_and_print)
listenThread.start()

while True:
    msg = input(f'Enter message: ')
    if msg.lower() == "quit":
        print("Bye!")
        disconnect_event.set()
        client_send('msg'.encode(), '')
        break
    file_path = input(f'Path to file: ')

    if len(file_path) == 0:
        file_path = None
    if disconnect_event.is_set():
        break
    client_send(msg.encode(), file_path)

listenThread.join()
socket.close()

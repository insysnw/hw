import struct
import threading
from datetime import datetime, timezone
import sys
sys.path.append('..')
from utils import get_actual_name


def create_msg(time: str, name: str, msg: bytes, flag: int, path=None, file=None):
    time_encode = time.encode()
    time_size = len(time_encode)
    time_size_packed = struct.pack(">I", time_size)

    name_encode = name.encode()
    name_size = len(name_encode)
    name_size_packed = struct.pack(">I", name_size)

    msg_size = len(msg) + 1
    msg_size_packed = struct.pack(">I", msg_size)

    flag_encode = bytearray()
    flag_encode.append(flag)
    flag_encode = bytes(flag_encode)

    file_size_packed = bytes()
    file_path = bytes()
    file_path_size_packed = bytes()

    if path is not None:
        file_path = path.encode()
        file_path_size_packed = struct.pack(">I", len(file_path))
        file_size_packed = struct.pack(">I", len(file))
    else:
        file = bytes()

    c = [
        *time_size_packed,
        *name_size_packed,
        *msg_size_packed,
        *time_encode,
        *name_encode,
        *msg,
        *flag_encode,
        *file_path_size_packed,
        *file_size_packed,
        *file_path,
        *file]

    msg_all = bytes(c)
    return msg_all


class ClientThread(threading.Thread):
    def __init__(self, socket, address, threads):
        threading.Thread.__init__(self)
        self.socket = socket
        self.address = address
        self.threads = threads
        self.name = ""

    def registration(self):
        (time, name, msg, flags, path, file) = self.server_recv()
        self.name = msg.decode()

    def safe_recv(self, size: int):
        msg = bytes()
        try:
            while size != 0:
                msg_tmp = self.socket.recv(size)
                size -= len(msg_tmp)
                msg += msg_tmp
        except(Exception,):
            for thread in self.threads:
                if thread.address == self.address:
                    self.threads.remove(thread)
                    self.socket.close()
        return msg

    def server_recv(self):
        (size,) = struct.unpack(">I", self.safe_recv(4))
        msg = self.safe_recv(size - 1)
        flag = self.safe_recv(1)[0]
        actual_path = None
        file = None
        if flag:
            path_size_packed = self.safe_recv(4)
            (path_size_up,) = struct.unpack(">I", path_size_packed)

            file_size_packed = self.safe_recv(4)
            (file_size_up,) = struct.unpack(">I", file_size_packed)

            path = self.safe_recv(path_size_up).decode()
            file = self.safe_recv(file_size_up)
            actual_path = get_actual_name(path)

            f = open(actual_path, "w+b")
            f.write(file)
            f.close()
        time = str(datetime.now(tz=timezone.utc))
        name = self.name
        return time, name, msg, flag, actual_path, file

    def send_to_everyone(self, msg_all: bytes):
        for thread in self.threads:
            if thread.address != self.address:
                try:
                    thread.socket.send(msg_all)
                except OSError:
                    self.threads.remove(thread)

    def run(self):
        self.registration()
        for thread in self.threads:
            if thread.name == self.name and thread.address != self.address:
                error_msg = create_msg(str(datetime.now(tz=timezone.utc)),
                                         "Server", f'{self.name} is already taken'.encode(), 0)
                self.socket.send(error_msg)
                self.socket.close()
                break
        else:
            connect_msg = create_msg(str(datetime.now(tz=timezone.utc)),
                                     "Server", f'{self.name} connected'.encode(), 0)
            self.send_to_everyone(connect_msg)
            while True:
                try:
                    recv_msg = self.server_recv()
                    crt_msg = create_msg(*recv_msg)
                    self.send_to_everyone(crt_msg)
                except (Exception,):
                    for thread in self.threads:
                        if thread.address == self.address:
                            self.threads.remove(thread)
                            self.socket.close()

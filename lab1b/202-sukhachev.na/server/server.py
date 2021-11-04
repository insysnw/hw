import socket
import threading
from datetime import datetime
import os
import select
import time

HEADER_LENGTH = 10
IP = "127.0.0.1"
PORT = 10000
clients = {}
SEND_FILE = 2 ** 72
SEPARATOR = "<SEPARATOR>"
byteorder = "big"
CONNECT = "CONNECT"
DISCONNECT = "DISCONNECT"
CONNECTION = 2 ** 64
UID = "84ecf7f7-674a-48c2-b09a-b7c6eea66d75".encode("utf-8")


def main():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind((IP, PORT))
    server.listen(5)
    server.setblocking(False)
    print("Сервер запущен!!!")
    inputs = [server]
    try:
        while True:
            reads, send, excepts = select.select(inputs, [], inputs)
            for conn in reads:
                if conn == server:
                    new_conn, client_addr = conn.accept()
                    new_conn.setblocking(False)
                    inputs.append(new_conn)
                else:
                    current_time = datetime.now().strftime("%H:%M")
                    if conn not in clients.keys():
                        while True:
                            header = conn.recv(HEADER_LENGTH)
                            header_len = int.from_bytes(header, byteorder)
                            name = conn.recv(header_len).decode("utf-8")
                            if name in clients.values():
                                notice = f"Извините, клиент с именем {name} уже существует," \
                                         f"выберите другое имя".encode('utf-8')
                                notice_header = int.to_bytes(CONNECTION + len(notice), HEADER_LENGTH, byteorder)
                                message = notice_header + notice
                                conn.send(message)
                                conn.close()
                            else:
                                clients[conn] = name
                                print(f"В {current_time} подключился новый клиент {name}")
                                notificationForClient(CONNECT, conn)
                            break
                    else:
                        while True:
                            try:
                                header = conn.recv(HEADER_LENGTH)
                                header_len = int.from_bytes(header, byteorder)
                                name = clients[conn].encode("utf-8")
                                if header_len < SEND_FILE:
                                    header_name = int.to_bytes(len(name), HEADER_LENGTH, byteorder)
                                    message = conn.recv(header_len)
                                    print(
                                        f'В {current_time}  получено сообщение от {clients[conn]}: {message.decode("utf-8")}')
                                    final_message = header_name + name + header + message
                                    sendToAll(final_message, conn)
                                else:
                                    file_header = conn.recv(header_len - SEND_FILE)
                                    filename, filesize = file_header.decode('utf-8').split(
                                        SEPARATOR)
                                    filesize = int(filesize)
                                    total_bytes = bytes()
                                    time.sleep(1)
                                    try:
                                        while not UID in total_bytes:
                                            bytes_read = conn.recv(filesize)
                                            total_bytes += bytes_read
                                    finally:
                                        file_header_len = int.to_bytes(len(file_header),HEADER_LENGTH,byteorder)
                                        name_header = int.to_bytes(SEND_FILE+len(name),HEADER_LENGTH,byteorder)
                                        fileMessage = name_header + name + file_header_len + file_header + total_bytes
                                        print(
                                            f"В {current_time} клиент {clients[conn]} отправил файл {filename}")
                                        sendToAll(fileMessage, conn)
                            except:
                                inputs.remove(conn)
                                print(f"{clients[conn]} отключается")
                                notificationForClient(DISCONNECT, conn)
                                del clients[conn]
                                conn.close()
                                break

                            break

            for conn in excepts:
                print(f"{clients[conn]} отключается")
                notificationForClient(DISCONNECT, conn)
                inputs.remove(conn)
                if conn in outputs:
                    outputs.remove(conn)
                conn.close()
                del messages[conn]
                del clients[conn]

    except KeyboardInterrupt:
        for cl in clients:
            cl.shutdown(socket.SHUT_WR)
            cl.close()
        server.shutdown(socket.SHUT_WR)
        server.close()
        os._exit(0)


def sendToAll(msg, clientsocket):
    for client in clients:
        if client != clientsocket:
            client.send(msg)


def notificationForClient(type, clientsocket):
    code_n = 2 ** 64
    notice: bytes
    name = clients[clientsocket]
    if type == CONNECT:
        notice = f"{name} входит в чат ".encode('utf-8')
    if type == DISCONNECT:
        notice = f"{name} выходит из чата ".encode('utf-8')
    notice_header = (code_n + len(notice)).to_bytes(HEADER_LENGTH, byteorder)
    message = notice_header + notice
    sendToAll(message, clientsocket)

if __name__ == '__main__':
    main()

import socket
import threading
from datetime import datetime
import os

HEADER_LENGTH = 10
IP = "127.0.0.1"
PORT = 10000
clients = {}
SEND_FILE = 2 ** 72
SEPARATOR = "<SEPARATOR>"
byteorder = "big"
CONNECT = "CONNECT"
DISCONNECT = "DISCONNECT"
CONNECTION = 2**64
UID = "84ecf7f7-674a-48c2-b09a-b7c6eea66d75".encode("utf-8")


def main():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind((IP, PORT))
    server.listen(5)
    print("Сервер запущен!!!")
    try:
        while True:
            (clientsocket, address) = server.accept()
            client = getMessage(clientsocket)
            if client:
                if client in clients.values():
                    notice = f"Извините, клиент с именем {client['data'].decode('UTF-8')} уже существует," \
                             f"выберите другое имя".encode('utf-8')
                    notice_header = int.to_bytes(CONNECTION+len(notice),HEADER_LENGTH,byteorder)
                    message =  notice_header + notice
                    clientsocket.send(message)
                    clientsocket.close()
                else:
                    clients[clientsocket] = client
                    time = datetime.now().strftime("%H:%M")
                    name = client['data'].decode('UTF-8')
                    print(
                        f"В {time} подключился новый клиент {name}")
                    notificationForClient(CONNECT, clientsocket)
                    handler_thread = threading.Thread(target=handle_client,
                                                      args=(clientsocket,))
                    handler_thread.start()
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
    name = clients[clientsocket]['data'].decode('utf-8')
    if type == CONNECT:
        notice = f"{name} входит в чат ".encode('utf-8')
    if type == DISCONNECT:
        notice = f"{name} выходит из чата ".encode('utf-8')
    notice_header = (code_n + len(notice)).to_bytes(HEADER_LENGTH, byteorder)
    message = notice_header + notice
    sendToAll(message, clientsocket)


def getMessage(clientsocket):
    try:
        message_header = clientsocket.recv(HEADER_LENGTH)
        messsage_length = int.from_bytes(message_header, byteorder)
        if not len(message_header):
            return False
        if messsage_length > SEND_FILE:
            file_header = clientsocket.recv(messsage_length - SEND_FILE)
            filename, filesize = file_header.decode('utf-8').split(
                SEPARATOR)
            filesize = int(filesize)
            total_bytes = bytes()
            while not UID in total_bytes:
                bytes_read = clientsocket.recv(filesize)
                total_bytes += bytes_read
            return {
                'sendFile': True,
                'header': file_header,
                'data': total_bytes
            }
        else:
            return {
                'sendFile': False,
                'header': message_header,
                'data': clientsocket.recv(messsage_length)}
    except ValueError:
        print("Тип header должен быть int")
        return False
    except:
        return False


def handle_client(clientsocket):
    while True:
        message = getMessage(clientsocket)
        name = clients[clientsocket]['data'].decode('utf-8')
        current_time = datetime.now().strftime("%H:%M")
        if message is False:
            clientsocket.shutdown(socket.SHUT_WR)
            clientsocket.close()
            print(f"{name} отключается")
            notificationForClient(DISCONNECT, clientsocket)
            del clients[clientsocket]
            return None
        if message['sendFile']:
            message_header_len = int.to_bytes(len(message["header"]),HEADER_LENGTH,byteorder)
            name_header = (SEND_FILE+int.from_bytes((clients[clientsocket]['header']),byteorder)).to_bytes(HEADER_LENGTH,byteorder)
            fileMessage = name_header + clients[clientsocket][
                'data'] + message_header_len + \
                          message["header"] + message["data"]
            sendToAll(fileMessage, clientsocket)
            filename = message["header"].decode('utf-8').split(SEPARATOR)[0]
            print(
                f"В {current_time} клиент {name} отправил файл {filename}")
        else:
            print(
                f'В {current_time}  получено сообщение от {name}: {message["data"].decode("utf-8")}')
            final_message = clients[clientsocket]['header'] + clients[clientsocket]['data'] + message['header'] + \
                            message['data']
            sendToAll(final_message, clientsocket)


if __name__ == '__main__':
    main()

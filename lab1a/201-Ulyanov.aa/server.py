import socket
from threading import Thread, active_count
from datetime import datetime
from time import sleep

PORT = 5050
HOST = socket.gethostbyname(socket.gethostname())
ADDRESS = (HOST, PORT)

SIZE = 1048576
FORMAT = 'utf-8'
FILE_MESSAGE = '!send'
EXIT_MESSAGE = '!exit'

server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.bind(ADDRESS)

clients_nicknames = {}
all_nicknames = set()


def send_to_all_clients(msg, sending_client):
    if sending_client == 'SERVER':
        print(msg)
    for client, (nickname, _) in clients_nicknames.items():
        if nickname != sending_client:
            client.send(msg)


def handle(client):
    connected = True
    while connected:
        try:
            msg = client.recv(SIZE)
            msg_str_time, message = msg.decode(FORMAT).split('|', maxsplit=1)
            msg_time = datetime.strptime(msg_str_time, '%Y-%m-%d %H:%M:%S.%f')

            nickname, last_active_time = clients_nicknames[client]

            if last_active_time > msg_time:
                continue

            if message.strip() == EXIT_MESSAGE:
                del clients_nicknames[client]
                all_nicknames.remove(nickname)
                connected = False

                left_msg = (msg_str_time + '|' + f"[SERVER]: {nickname} left").encode(FORMAT)
                send_to_all_clients(left_msg, 'SERVER')
                client.close()

            elif message.strip().startswith(FILE_MESSAGE):
                send_to_all_clients(msg, nickname)
                sleep(0.001)

                text = client.recv(SIZE)
                send_to_all_clients(text, nickname)
                sleep(0.001)

                send_msg = (msg_str_time + '|' + f"{nickname} send file {message.split(' ')[1]}").encode(FORMAT)
                send_to_all_clients(send_msg, nickname)

            else:
                msg_text = (msg_str_time + '|' + f"[{nickname}]: {message}").encode(FORMAT)
                send_to_all_clients(msg_text, nickname)

        except ConnectionError:
            nickname, _ = clients_nicknames[client]

            del clients_nicknames[client]
            all_nicknames.remove(nickname)
            client.close()
            connected = False
            left_msg = (str(datetime.utcnow()) + '|' + f"[SERVER]: {nickname} left").encode(FORMAT)
            send_to_all_clients(left_msg, 'SERVER')


def start():
    server.listen()
    print("[SERVER IS LISTENING]")
    while True:
        client, address = server.accept()

        what_nick = (str(datetime.utcnow()) + '|' + "[SERVER]: What's your nickname?").encode(FORMAT)
        client.send(what_nick)

        _, nickname = client.recv(SIZE).decode(FORMAT).split('|', maxsplit=1)

        while nickname in all_nicknames:
            what_nick = (str(datetime.utcnow()) + '|' +
                         "[SERVER]: Nickname is busy. Choose another one.").encode(FORMAT)
            client.send(what_nick)

            _, nickname = client.recv(SIZE).decode(FORMAT).split('|', maxsplit=1)

        time_utc = datetime.utcnow()
        clients_nicknames[client] = (nickname, time_utc)
        all_nicknames.add(nickname)

        joined_msg = (str(datetime.utcnow()) + '|' + f"[SERVER]: {nickname} joined").encode(FORMAT)
        send_to_all_clients(joined_msg, 'SERVER')

        thread = Thread(target=handle, args=(client, ))
        thread.start()
        print(f"Active connection {active_count() - 1}")


start()

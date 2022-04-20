import socket
from threading import Thread, active_count
from datetime import datetime

PORT = 5050
HOST = socket.gethostbyname(socket.gethostname())
ADDRESS = (HOST, PORT)

HEADER_LENGTH = 20
SIZE = 1048576
FORMAT = 'utf-8'
FILE_MESSAGE = '!send'
EXIT_MESSAGE = '!exit'

server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.bind(ADDRESS)

clients_nicknames = {}
all_nicknames = set()


def preparing_a_message(text_message, nick):
    time_utc = str(datetime.utcnow())
    time_header = len(time_utc).to_bytes(HEADER_LENGTH, byteorder='big')
    nick_header = len(nick).to_bytes(HEADER_LENGTH, byteorder='big')
    message_header = len(text_message).to_bytes(HEADER_LENGTH, byteorder='big')
    return time_header + time_utc.encode(FORMAT) + nick_header + nick.encode(FORMAT) + \
           message_header + text_message.encode(FORMAT)


def get_message(client):
    try:
        header = client.recv(HEADER_LENGTH)
    except ConnectionResetError or OSError:
        return False

    if not len(header):
        return False

    size = int.from_bytes(header, byteorder='big', signed=False)
    data = client.recv(size)
    if data is None:
        return False

    return {'header': header, 'data': data}


def send_to_all_clients(msg, sending_client):
    for client, (nickname, _) in clients_nicknames.items():
        if nickname != sending_client:
            client.send(msg)


def handle(client):
    connected = True
    while connected:
        try:
            msg = get_message(client)
            if not msg:
                continue
            message = msg['data'].decode(FORMAT)

            msg_time = datetime.utcnow()

            nickname, last_active_time = clients_nicknames[client]

            if last_active_time > msg_time:
                continue

            if message.strip() == EXIT_MESSAGE:
                del clients_nicknames[client]
                all_nicknames.remove(nickname)
                connected = False

                left_msg = preparing_a_message(f"{nickname} left", 'SERVER')
                send_to_all_clients(left_msg, 'SERVER')
                client.close()

            elif message.strip().startswith(FILE_MESSAGE):
                send_msg = preparing_a_message(message, nickname)
                send_to_all_clients(send_msg, nickname)

                sha = get_message(client)
                if not sha:
                    continue
                send_msg = sha['header'] + sha['data']
                send_to_all_clients(send_msg, nickname)

                file_data = get_message(client)
                if not file_data:
                    continue
                send_msg = file_data['header'] + file_data['data']
                send_to_all_clients(send_msg, nickname)

                send_msg = preparing_a_message(f"{nickname} send file {message.split(' ')[1]}", 'SERVER')
                send_to_all_clients(send_msg, nickname)

            else:
                msg_text = preparing_a_message(message, nickname)
                send_to_all_clients(msg_text, nickname)

        except ConnectionError:
            nickname, _ = clients_nicknames[client]

            del clients_nicknames[client]
            all_nicknames.remove(nickname)
            client.close()
            connected = False
            left_msg = preparing_a_message(f"{nickname} left", 'SERVER')
            send_to_all_clients(left_msg, 'SERVER')


def start():
    server.listen()
    print("[SERVER IS LISTENING]")
    while True:
        client, address = server.accept()

        what_nick = "What's your nickname?"
        msg = preparing_a_message(what_nick, 'SERVER')
        client.send(msg)

        nickname_msg = get_message(client)
        if not nickname_msg:
            break
        else:
            nickname = nickname_msg['data'].decode(FORMAT)
        what_nick = "Nickname is busy. Choose another one."

        while nickname in all_nicknames:
            msg = preparing_a_message(what_nick, 'SERVER')
            client.send(msg)

            nickname_msg = get_message(client)
            if not nickname_msg:
                break
            else:
                nickname = nickname_msg['data'].decode(FORMAT)
        else:
            time_utc = datetime.utcnow()
            clients_nicknames[client] = (nickname, time_utc)
            all_nicknames.add(nickname)

            joined_msg = preparing_a_message(f"{nickname} joined", 'SERVER')
            send_to_all_clients(joined_msg, 'SERVER')

            thread = Thread(target=handle, args=(client,))
            thread.start()
            print(f"Active connection {active_count() - 1}")


start()

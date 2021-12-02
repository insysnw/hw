import socket
import select
from datetime import datetime

HEADER_LENGTH = 20
FORMAT = 'utf-8'
SIZE = 1048576

PORT = 5050
HOST = socket.gethostbyname(socket.gethostname())
ADDRESS = (HOST, PORT)

server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind(ADDRESS)
server_nick = {'header': len('SERVER').to_bytes(HEADER_LENGTH, byteorder='big'), 'data': 'SERVER'.encode(FORMAT)}

# listen to new connections
server.listen(100)
print("Server is listening...")

inputs = [server]

users = {}


def send_to_all_clients(s, nickname, message, header):
    time = str(datetime.utcnow())
    time_header = len(time).to_bytes(HEADER_LENGTH, byteorder='big')
    print(f"<{time}> {nickname['data']}: {message}")
    for client in inputs:
        if client != s and client != server:
            client.send(time_header + time.encode(FORMAT) + nickname['header'] + nickname['data'] + header + message)


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


while True:
    read_sockets, _, exception_sockets = select.select(inputs, [], inputs)

    for conn in read_sockets:
        if conn == server:
            # если это серверный сокет, то пришёл новый клиент
            new_conn, client_address = conn.accept()
            user = get_message(new_conn)

            if not user:
                continue

            if user in users.values():
                msg = "Username is already exists.".encode(FORMAT)
                time_utc = str(datetime.utcnow())
                time_utc_header = len(time_utc).to_bytes(HEADER_LENGTH, byteorder='big')
                new_conn.send(time_utc_header + time_utc.encode(FORMAT) +
                              server_nick['header'] + server_nick['data'] +
                              len(msg).to_bytes(HEADER_LENGTH, byteorder='big') + msg)
                new_conn.close()
                continue

            new_conn.setblocking(False)
            inputs.append(new_conn)
            users[new_conn] = user

            msg = f"{user['data'].decode(FORMAT)} joined.".encode(FORMAT)
            msg_header = len(msg).to_bytes(HEADER_LENGTH, byteorder='big')

            send_to_all_clients(new_conn, server_nick, msg, msg_header)
        else:
            # если не серверный сокет, то пришло сообщение
            getting_msg = get_message(conn)
            user = users[conn]

            if not getting_msg:
                msg = f"{users[conn]['data'].decode(FORMAT)} left.".encode(FORMAT)
                msg_header = len(msg).to_bytes(HEADER_LENGTH, byteorder='big')
                send_to_all_clients(conn, server_nick, msg, msg_header)
                inputs.remove(conn)
                del users[conn]
                conn.close()
                continue

            send_to_all_clients(conn, user, getting_msg['data'], getting_msg['header'])

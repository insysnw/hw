import socket
import threading
import time
import datetime

CLOSE_MESSAGE = ":q"

# connection
host = '127.0.0.1'
port = 9090

# start
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind((host, port))
server.listen(10)

clients = []

print("Server started!")


# send to all clients
def broadcast(sock, nick, t, msg_header, msg):
    for tmp in clients:
        tmp.send(t + nick['header'] + nick['data'] + msg_header + msg)


# receive messages from clients
def read_part(client):
    while True:
        try:
            header = client.recv(2)
        except Exception as e:
            print("Connection reset: {}".format(str(e)))
            return dict()
        length = int.from_bytes(header, byteorder='big', signed=False)
        try:
            msg = client.recv(length)
        except Exception as e:
            print("Connection reset: {}".format(str(e)))
            return dict()
        return {'header': header, 'data': msg}


def handle(client, address):
    if client not in clients:
        clients.append(client)
        print("{} | Connected {}".format(time.strftime('%H:%M', time.localtime()), client.getpeername()))

    while True:
        enc_time = int(datetime.datetime.utcnow().timestamp()).to_bytes(4, byteorder='big')
        nickname = read_part(client)
        message = read_part(client)
        if len(message) == 0 or message['data'].decode('utf-8') == CLOSE_MESSAGE:
            left_message = 'disconnected'
            print('Connection from {} {} was closed'.format(nickname['data'].decode('utf-8'), client.getpeername()))
            clients.remove(client)
            for tmp in clients:
                tmp.send(enc_time + nickname['header'] + nickname['data'] + len(left_message).to_bytes(2, byteorder='big') + left_message.encode('utf-8'))
            break
        else:
            print('{} | {}: {}'.format(
                datetime.datetime.utcnow().strftime('%H:%M:%S'),
                nickname['data'].decode('utf-8'),
                message['data'].decode('utf-8')))
            broadcast(client, nickname, enc_time, message['header'], message['data'])


while True:
    # thread for clients
    client_socket, address_socket = server.accept()
    thread = threading.Thread(target=handle, args=(client_socket, address_socket)).start()

import socket
import argparse
import select
import sys
import time

TYPE_INIT = 1
TYPE_DUPLICATENAME = 2
TYPE_WRONGNAMELEN = 3
TYPE_OKNAME = 4
TYPE_MESSAGE = 5
TYPE_FILE = 6
TYPE_ENDSESSION = 7

MESSAGE_SIZE = 2048
FORMAT = 'utf-8'

parser = argparse.ArgumentParser()
parser.add_argument('--ip', default='0.0.0.0', type=str)
parser.add_argument('--port', default=2000, type=int)

server_address = parser.parse_args()
print('Server started at', server_address.ip, ':', server_address.port)

server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.setblocking(True)
server_socket.bind((server_address.ip, server_address.port))
server_socket.listen()

answer_time = 0
answer_time_len = 0
answer_name = bytearray('Server'.encode(FORMAT))
answer_name_len = len(answer_name)


def decode_message(message):

    if len(message) == 0:
        return

    count = 0
    message_type = message[0]
    count = count + 1
    message_time = bytearray()
    message_sender_name = bytearray()
    message_data = bytearray()
    message_data_len = bytearray()

    if count + 1 > MESSAGE_SIZE:
        message = request_message(connection, MESSAGE_SIZE)
        message_time_len = message[0]
    else:
        message_time_len = message[1]
        count = count + 1

    if count + 1 > MESSAGE_SIZE:
        message = request_message(connection, MESSAGE_SIZE)
        message_sender_name_len = message[0]
        count = 1
    else:
        message_sender_name_len = message[2]
        count = count + 1

    if message_time_len + count > MESSAGE_SIZE:
        for o in range(count, MESSAGE_SIZE):
            message_time.append(message[o])

        message_time_len_mod = message_time_len - (MESSAGE_SIZE - count)

        while message_time_len_mod > MESSAGE_SIZE:
            message = request_message(connection, MESSAGE_SIZE)
            for o in range(0, MESSAGE_SIZE):
                message_time.append(message[o])
            message_time_len_mod = message_time_len_mod - MESSAGE_SIZE

        message = connection.recv(MESSAGE_SIZE)
        for o in range(0, message_time_len_mod):
            message_time.append(message[o])
        count = message_time_len_mod

    else:
        for i in range(count, message_time_len + count):
            message_time.append(message[i])
            count = count + 1

    if message_sender_name_len + count > MESSAGE_SIZE:
        for o in range(count, MESSAGE_SIZE):
            message_sender_name.append(message[o])

        message_sender_name_len_mod = message_sender_name_len - (MESSAGE_SIZE - count)

        while message_sender_name_len_mod > MESSAGE_SIZE:
            message = request_message(connection, MESSAGE_SIZE)
            for o in range(0, MESSAGE_SIZE):
                message_sender_name.append(message[o])

            message_sender_name_len_mod = message_sender_name_len_mod - MESSAGE_SIZE

        message = request_message(connection, MESSAGE_SIZE)
        for o in range(0, message_sender_name_len_mod):
            message_sender_name.append(message[o])
        count = message_sender_name_len_mod
    else:
        for j in range(count, message_sender_name_len + count):
            message_sender_name.append(message[j])
            count = count + 1

    message_sender_name = message_sender_name.decode(FORMAT)

    if count + 1 > MESSAGE_SIZE:
        message = request_message(connection, MESSAGE_SIZE)
        message_data_len_len = message[0]
        count = 1
    else:
        message_data_len_len = message[count]
        count = count + 1

    if message_data_len_len + count > MESSAGE_SIZE:
        for o in range(count, MESSAGE_SIZE):
            message_data_len.append(message[o])

        message_data_len_len_mod = message_data_len_len - (MESSAGE_SIZE - count)

        while message_data_len_len_mod > MESSAGE_SIZE:
            message = request_message(connection, MESSAGE_SIZE)
            for o in range(0, MESSAGE_SIZE):
                message_data_len.append(message[o])
            message_data_len_len_mod = message_data_len_len_mod - MESSAGE_SIZE
        message = request_message(connection, MESSAGE_SIZE)
        for o in range(0, message_data_len_len_mod):
            message_data_len.append(message[o])
        count = message_data_len_len_mod
    else:
        for o in range(count, count + message_data_len_len):
            message_data_len.append(message[o])
            count = count + 1

    message_data_len = int.from_bytes(message_data_len, sys.byteorder)

    if message_data_len + count > MESSAGE_SIZE:
        for o in range(count, MESSAGE_SIZE):
            message_data.append(message[o])

        message_data_len_mod = message_data_len - (MESSAGE_SIZE - count)

        while message_data_len_mod > MESSAGE_SIZE:
            message = request_message(connection, MESSAGE_SIZE)
            for o in range(0, MESSAGE_SIZE):
                message_data.append(message[o])
            message_data_len_mod = message_data_len_mod - MESSAGE_SIZE

        message = request_message(connection, message_data_len_mod)
        for o in range(0, message_data_len_mod):
            message_data.append(message[o])

    else:
        for o in range(count, count + message_data_len):
            message_data.append(message[o])

    return message_type, message_time_len, message_sender_name_len, message_time, message_sender_name, \
        message_data_len_len, message_data_len, message_data


def send_message(message):
    try:
        _, ready_clients, _ = select.select([], clients.keys(), [])
    except OSError:
        return
    for client in ready_clients:
        if client != server_socket:
            client.send(message)


def make_answer(message_type, message_time_len, message_sender_name_len, message_time, message_sender_name,
                message_data_len, message_data):
    message_data_len = bytearray([message_data_len])
    message_data_len_len = bytes([len(message_data_len)])
    return bytes([message_type]) + bytes([message_time_len]) + bytes([message_sender_name_len]) \
        + message_time.to_bytes(36, 'big') + message_sender_name + \
        message_data_len_len + message_data_len + message_data


def shutdown_connection(connection):

    try:
        connection.shutdown(socket.SHUT_RDWR)
        connection.close()
    except WindowsError:
        print('[', time.asctime(), '] ', 'User broke connection')

    if connection in sockets:
        sockets.remove(connection)
    if connection in clients:
        print('[', time.asctime(), '] ', clients[connection], 'was disconnected')
        answer_data = bytearray(('User ' + clients[connection] + ' was disconnected').encode(FORMAT))
        answer_data_len = len(answer_data)
        answer_for_all = make_answer(TYPE_MESSAGE, answer_time_len, answer_name_len, answer_time,
                                     answer_name, answer_data_len, answer_data)
        del clients[connection]
        send_message(answer_for_all)


def request_message(connection, message_size):
    try:
        message = connection.recv(message_size)
        return bytearray(message)
    except ConnectionError and ConnectionResetError:
        shutdown_connection(connection)
        return ''


def handle_connection(connection):
    global answer_time, answer_time_len

    answer_time = time.time_ns()
    answer_time_len = sys.getsizeof(answer_time)

    message = request_message(connection, MESSAGE_SIZE)

    if len(message) == 0 or message[0] == TYPE_ENDSESSION:
        shutdown_connection(connection)
        return

    message_type, message_time_len, message_sender_name_len, message_time, message_sender_name, message_data_len_len, \
        message_data_len, message_data = decode_message(message)

    if connection in clients:
        if clients[connection] != message_sender_name:
            shutdown_connection(connection)
            return

    if message_type == TYPE_INIT:
        is_already_used = False
        for client in clients.values():
            if client == message_sender_name:
                is_already_used = True
                break

        if is_already_used:
            answer_data = bytearray('Your name is already in use, try another.'.encode(FORMAT))
            answer_data_len = len(answer_data)
            answer_message = make_answer(TYPE_DUPLICATENAME, answer_time_len, answer_name_len, answer_time,
                                         answer_name, answer_data_len, answer_data)
            sockets.append(connection)
            connection.send(answer_message)

        else:
            if 0 < message_sender_name_len < 20:
                print('[', time.ctime(), '] ', 'New client ', message_sender_name, ' was connected')

                clients[connection] = message_sender_name

                if connection not in sockets:
                    sockets.append(connection)

                answer_data = bytearray('You successfully registered in the chat!'.encode(FORMAT))
                answer_data_len = len(answer_data)
                answer_for_client = make_answer(TYPE_OKNAME, answer_time_len, answer_name_len, answer_time,
                                                answer_name, answer_data_len, answer_data)
                connection.send(answer_for_client)

                time.sleep(1)

                answer_data = bytearray(('User ' + message_sender_name + ' has arrived').encode(FORMAT))
                answer_data_len = len(answer_data)
                answer_for_all = make_answer(TYPE_MESSAGE, answer_time_len, answer_name_len, answer_time,
                                             answer_name, answer_data_len, answer_data)
                send_message(answer_for_all)

            else:
                answer_data = bytearray('Your name is wrong in length, try another'.encode(FORMAT))
                answer_data_len = len(answer_data)
                answer_message = make_answer(TYPE_WRONGNAMELEN, answer_time_len, answer_name_len, answer_time,
                                             answer_name, answer_data_len, answer_data)
                connection.send(answer_message)

    elif message_type == TYPE_MESSAGE or TYPE_FILE:

        if message_type == TYPE_MESSAGE:
            reply = bytes([TYPE_MESSAGE])
            print('[', time.ctime(), '] ', 'New message from ', message_sender_name + '.')
        else:
            reply = bytes([TYPE_FILE])
            print('[', time.ctime(), '] ', 'New file from ', message_sender_name + '.')

        if len(message_data) <= 256:
            message_data_len = bytearray([len(message_data)])
        else:
            message_data_len = int.to_bytes(len(message_data), int((len(message_data) / 256) + 1), sys.byteorder)

        reply = reply + bytes([message_time_len]) + bytes([message_sender_name_len]) \
            + message_time + bytearray(message_sender_name.encode(FORMAT)) + bytes([message_data_len_len]) \
            + message_data_len + message_data
        send_message(reply)


clients = {}
sockets = [server_socket]

try:
    while True:
        ready, _, _ = select.select(sockets, [], [])
        for sock in ready:
            if sock == server_socket:
                connection, address = server_socket.accept()
                handle_connection(connection)
            else:
                handle_connection(sock)
except KeyboardInterrupt:
    answer_time = time.time_ns()
    answer_time_len = sys.getsizeof(answer_time)
    answer_data = bytearray('Server is closing'.encode(FORMAT))
    answer_data_len = len(answer_data)
    msg = make_answer(TYPE_ENDSESSION, answer_time_len, answer_name_len, answer_time, answer_name, answer_data_len,
                      answer_data)
    send_message(msg)
    for client in clients.keys():
        client.close()
    clients.clear()
    sockets.clear()
    server_socket.shutdown(socket.SHUT_RDWR)
    server_socket.close()
finally:
    answer_time = time.time_ns()
    answer_time_len = sys.getsizeof(answer_time)
    answer_data = bytearray('Server is closing'.encode(FORMAT))
    answer_data_len = len(answer_data)
    msg = make_answer(TYPE_ENDSESSION, answer_time_len, answer_name_len, answer_time, answer_name, answer_data_len,
                      answer_data)
    send_message(msg)
    for client in clients.keys():
        client.close()
    clients.clear()
    sockets.clear()
    server_socket.shutdown(socket.SHUT_RDWR)
    server_socket.close()

import os
import socket
import argparse
import threading
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
FORMAT = "utf-8"

working = True
check = False
file_count = 1


def make_message(message_type, message_time_len, message_sender_name_len, message_time, message_sender_name,
                 message_data, message_data_name='', message_data_name_len=0):
    message_data = bytearray(message_data.encode(FORMAT))

    if message_data_name_len != 0:
        message_data = bytes([message_data_name_len]) + bytearray(message_data_name.encode(FORMAT)) \
                   + message_data

    if len(message_data) <= 256:
        message_data_len = bytearray([len(message_data)])
    else:
        message_data_len = int.to_bytes(len(message_data), int((len(message_data) / 256) + 1), sys.byteorder)
    message_data_len_len = bytes([len(message_data_len)])
    return bytes([message_type]) + bytes([message_time_len]) + bytes([message_sender_name_len]) + \
        message_time.to_bytes(36, 'big') + message_sender_name + \
        message_data_len_len + message_data_len + message_data


def request_message(connection, message_size):
    try:
        message = connection.recv(message_size)
        return bytearray(message)
    except ConnectionError and ConnectionResetError:
        return ''


def decode_message(connection):

    message = request_message(connection, MESSAGE_SIZE)

    if len(message) == 0:
        return TYPE_ENDSESSION, '', ''

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

    message_time = time.localtime(int.from_bytes(message_time, 'big') / 1000000000)
    return message_type, '[' + time.asctime(message_time) + ']' + '[' + message_sender_name + '] ', message_data


def handle_incoming():
    global working, msg, check, client_name, file_count
    while True:
        message_type, message, message_data = decode_message(client_socket)

        if len(message) == 0 or message_type == TYPE_ENDSESSION:
            print("Connection with server was lost, type anything to close chat")
            working = False
            break

        elif message_type == TYPE_OKNAME:
            check = True
            print(message + message_data.decode(FORMAT))

        elif message_type == TYPE_FILE:

            file_name_len = message_data[0]
            file_name = bytearray()

            for o in range(1, file_name_len + 1):
                file_name.append(message_data[o])

            new_file_path = os.path.join(os.getcwd(), client_name + str(file_count))

            if not os.path.isfile(new_file_path):
                with open(new_file_path, 'w+') as file:
                    file_count = file_count + 1
                    file.write(message_data[file_name_len + 1:].decode(FORMAT))
                    file.close()
                    print(message + ' You got new file named ' + file_name.decode(FORMAT))

            else:
                while os.path.isfile(new_file_path):
                    file_count = file_count + 1
                    new_file_path = os.path.join(os.getcwd(), client_name + str(file_count))
                with open(new_file_path, 'w+') as file:
                    file.write(message_data[file_name_len + 1:].decode(FORMAT))
                    file.close()
                    print(message + ' You got new file named ' + file_name.decode(FORMAT))

        else:
            print(message + message_data.decode(FORMAT))


parser = argparse.ArgumentParser()
parser.add_argument("-ip", default='127.0.0.1', type=str)
parser.add_argument("-port", default=2000, type=int)
server_address = parser.parse_args()

client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client_socket.connect((server_address.ip, server_address.port))
client_socket.setblocking(True)

print('Type your nickname: ')
client_name = str(input())
msg_name = bytearray(client_name.encode(FORMAT))
msg_time = time.time_ns()
msg_time_len = sys.getsizeof(msg_time)
msg_name_len = len(msg_name)
msg = make_message(TYPE_INIT, msg_time_len, msg_name_len, msg_time, msg_name, '')
client_socket.send(msg)

reading_thread = threading.Thread(target=handle_incoming)
reading_thread.start()

while True:

    msg_data = str(input())
    msg_time = time.time_ns()
    msg_time_len = sys.getsizeof(msg_time)

    if check:

        if not working:
            break

        if msg_data == 'quit':
            print('Ending connection with server')
            msg = make_message(TYPE_ENDSESSION, msg_time_len, msg_name_len, msg_time, msg_name, msg_data)
            client_socket.send(msg)
            break

        elif msg_data == 'send file':

            if not working:
                break

            is_file = False
            while not is_file:
                print('Type name of the file')
                file_name = str(input())
                file_path = os.path.join(os.getcwd(), file_name)
                if os.path.isfile(file_path):
                    is_file = True
                    with open(file_path) as file:
                        msg_data = file.read()
                        file_name_len = len(bytearray(file_name.encode(FORMAT)))
                        msg = make_message(TYPE_FILE, msg_time_len, msg_name_len, msg_time, msg_name, msg_data,
                                           message_data_name_len=file_name_len, message_data_name=file_name)
                        client_socket.send(msg)
        else:

            if not working:
                break

            msg = make_message(TYPE_MESSAGE, msg_time_len, msg_name_len, msg_time, msg_name, msg_data)
            client_socket.send(msg)

    else:

        if not working:
            break

        client_name = msg_data
        msg_name = bytearray(client_name.encode(FORMAT))
        msg_name_len = len(msg_name)
        msg = make_message(TYPE_INIT, msg_time_len, msg_name_len, msg_time, msg_name, '')
        client_socket.send(msg)

reading_thread.join()
client_socket.shutdown(socket.SHUT_RDWR)
client_socket.close()
print('Chat was closed')

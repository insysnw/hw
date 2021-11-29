import socket
import select
import errno
import sys
from datetime import datetime


class Style:
    BLACK = '\033[30m'
    RED = '\033[31m'
    GREEN = '\033[32m'
    YELLOW = '\033[33m'
    BLUE = '\033[34m'
    MAGENTA = '\033[35m'
    CYAN = '\033[36m'
    WHITE = '\033[37m'
    UNDERLINE = '\033[4m'
    RESET = '\033[0m'


HEADER_LENGTH = 10

IP = "127.0.0.1"
PORT = 1234
FORMAT = 'utf-8'
my_username = input("Username: ")

client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client_socket.connect((IP, PORT))
client_socket.setblocking(False)

username = my_username.encode(FORMAT)
username_header = f"{len(username):<{HEADER_LENGTH}}".encode(FORMAT)
client_socket.send(username_header + username)

while True:
    message = input()
    if message:
        message = message.encode(FORMAT)
        message_header = f"{len(message):<{HEADER_LENGTH}}".encode(FORMAT)
        client_socket.send(message_header + message)

    try:
        # Now we want to loop over received messages (there might be more than one) and print them
        while True:
            username_header = client_socket.recv(HEADER_LENGTH)
            if not len(username_header):
                print('Connection closed by the server')
                sys.exit()

            username_length = int(username_header.decode(FORMAT).strip())
            username = client_socket.recv(username_length).decode(FORMAT)

            message_header = client_socket.recv(HEADER_LENGTH)
            message_length = int(message_header.decode(FORMAT).strip())
            message = client_socket.recv(message_length).decode(FORMAT)

            # Print message
            now = datetime.now()
            current_time = now.strftime("%H:%M")
            print(f'<{Style.RESET + current_time}> {Style.RED + username}: {Style.RESET + message}')
            # print(f'{username} > {message}')

    except IOError as e:
        if e.errno != errno.EAGAIN and e.errno != errno.EWOULDBLOCK:
            print('Reading error: {}'.format(str(e)))
            sys.exit()
        continue

    except Exception as e:
        # Any other exception - something happened, exit
        print('Reading error: '.format(str(e)))
        sys.exit()

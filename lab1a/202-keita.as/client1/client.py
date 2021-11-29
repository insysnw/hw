import os
import re
import socket
import threading
from datetime import datetime

# Choosing Nickname
nickname = input("Choose your nickname: ")

# Connecting To Server
client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client.connect(('127.0.0.1', 15200))

HEADER_LENGTH = 10
SEND_FILE = "SEND_FILE"
FORMAT = 'ascii'
SIZE = 1024


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


# Listening to Server and Sending Nickname
def receive():
    while True:
        try:
            message = client.recv(SIZE).decode('ascii')
            if message == 'NICK':
                nick = nickname.encode(FORMAT)
                nick_header = f"{len(nick):<{HEADER_LENGTH}}".encode(FORMAT)
                client.send(nick_header + nick)
            elif message.strip() == SEND_FILE:
                filename = client.recv(SIZE).decode('ascii')
                file = open(filename, "w")
                data = client.recv(SIZE).decode('ascii')
                file.write(data)
                file.close()
            else:
                now = datetime.now()
                current_time = now.strftime("%H:%M")
                nickname2 = client.recv(SIZE).decode('ascii')
                print(f'<{Style.RESET + current_time}> {Style.RED + nickname2}: {Style.MAGENTA + message}')

        except Exception as e:
            # Close Connection When Error
            print("An error occurred!", str(e))
            client.close()
            break


# Sending Messages To Server
def write():
    while True:
        msg = input()
        if re.match("^send .*\.\w*\s*$", msg):
            client.send(f"{SEND_FILE}".encode())
            try:
                files = re.findall("\s+\w.*\.\w*\s*", msg)
                fileName = files[0].strip()
                filesize = os.path.getsize(fileName)
                if filesize > SIZE:
                    print("file exceed")
                    client.close()
                    break
                client.send(fileName.encode(FORMAT))
                file = open(fileName, "r")
                data = file.read()
                client.send(data.encode(FORMAT))
                file.close()
                client.send(f'{nickname} sent file {fileName}'.encode(FORMAT))
            except:
                print(f'can not find file {fileName}')
        else:
            if msg:
                message = msg.encode(FORMAT)
                client.send(message)


# Starting Threads For Listening And Writing
receive_thread = threading.Thread(target=receive)
receive_thread.start()

write_thread = threading.Thread(target=write)
write_thread.start()

import socket
import threading
import re
import os
from datetime import datetime

# Choosing Nickname
nickname = input("Choose your nickname: ")

# Connecting To Server
client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client.connect(('127.0.0.1', 55555))

SEND_FILE = "SEND_FILE"
FORMAT = 'ascii'
SIZE = 1024


# Listening to Server and Sending Nickname
def receive():
    while True:
        try:
            # Receive Message From Server
            # If 'NICK' Send Nickname
            message = client.recv(SIZE).decode('ascii')
            if message == 'NICK':
                client.send(nickname.encode(FORMAT))
            else:
                now = datetime.now()
                current_time = now.strftime("%H:%M")
                if message.strip() == SEND_FILE:
                    filename = client.recv(SIZE
                                           ).decode('ascii')
                    file = open(filename, "w")
                    data = client.recv(SIZE
                                       ).decode('ascii')
                    file.write(data)
                    file.close()
                else:
                    print("<{}> {}".format(current_time, message))

        except:
            # Close Connection When Error
            print("An error occurred!")
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
                message = '{}: {}'.format(nickname, msg)
                client.send(message.encode(FORMAT))


# Starting Threads For Listening And Writing
receive_thread = threading.Thread(target=receive)
receive_thread.start()

write_thread = threading.Thread(target=write)
write_thread.start()

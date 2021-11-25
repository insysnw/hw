import threading
import socket

# Connection Data
host = '127.0.0.1'  # localhost
port = 15200

# Starting Server
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.bind((host, port))
server.listen()

# Lists For Clients and Their Nicknames
clients = []
nicknames = []
clients_ = {}

HEADER_LENGTH = 10
FORMAT = 'ascii'
SIZE = 1024


# Sending Messages To All Connected Clients
def broadcast(message, _client):
    for client in clients:
        if client != _client:
            client.send(message)


# Handling Messages From Clients
def handle(client):

    # Request And Store Nickname
    client.send('NICK'.encode(FORMAT))

    nickname_header = client.recv(HEADER_LENGTH)
    nickname_length = int(nickname_header.decode(FORMAT).strip())
    nickname = client.recv(nickname_length).decode(FORMAT)

    nicknames.append(nickname)
    clients.append(client)
    clients_[client] = nickname

    client.send('Connected to server!'.encode(FORMAT))
    # Print And Broadcast Nickname
    print("Nickname is {}".format(nickname))
    broadcast("{} joined!".format(nickname).encode(FORMAT), client)

    while True:
        try:
            # Broadcasting Messages
            message = client.recv(SIZE)
            broadcast(message, client)
            nickname2 = clients_[client].encode(FORMAT)
            broadcast(nickname2, client)
        except:
            # Removing And Closing Clients
            index = clients.index(client)
            clients.remove(client)
            client.close()
            nickname = nicknames[index]
            broadcast('{} left!'.format(nickname).encode(FORMAT), client)
            nicknames.remove(nickname)
            break


# Receiving / Listening Function
def receive():
    while True:
        # Accept Connection
        client, address = server.accept()
        print("Connected with {}".format(str(address)))
        thread = threading.Thread(target=handle, args=(client,))
        thread.start()


print("server is listening...")
receive()

import socket
from ClientThread import ClientThread

socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
socket.bind(('', 9090))
socket.listen(10)
print('Server is listening')

threads = []
while True:
    connection, address = socket.accept()
    print(f'{address} connected')
    threads.append(ClientThread(connection, address, threads))
    threads[-1].start()

socket.close()

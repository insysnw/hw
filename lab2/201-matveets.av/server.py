import socket

# Типы DHCP-сообщения
DHCPDISCOVER = 1
DHCPOFFER = 2
DHCPREQUEST = 3
DHCPDECLINE = 4
DHCPACK = 5
DHCPNAK = 6
DHCPRELEASE = 7

subnet_mask = '255.255.255.0'
default_address = '192.168.0.1'

host_address = '0.0.0.0'
server_port = 67

addresses = []

print('DHCP server running on', host_address, ':', server_port)
server_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
server_socket.bind((host_address, server_port))


class DHCPPacket:
    def __init__(self):
        self.opcode = bytearray([2])  # Тип сообщения
        self.htype = bytearray([1])  # Тип адреса устройства
        self.hlen = bytearray([6])  # Длина адреса устройства
        self.hops = bytearray([0])  # Число ретрансляционных участков
        self.xid = bytearray(4)  # Идентификатор процедуры информационного обмена
        self.secs = bytearray(2)  # Время начала процедуры
        self.flags = bytearray(2)  # Флаги
        self.ciaddr = bytearray(4)  # IP-адрес клиента
        self.yiaddr = bytearray(4)  # «Ваш» (клиента) IP-адрес
        self.siaddr = bytearray([0, 0, 0, 0])  # IP-адрес следующего сервера
        self.giaddr = bytearray([0, 0, 0, 0])  # IP-адрес агента/ретранслятора
        self.chaddr = bytearray(16)  # Адрес устройства клиента
        self.sname = bytearray(64)  # Дополнительное имя устройства, со встроенным сервером
        self.file = bytearray(128)  # Имя файла загрузки
        self.options = bytearray(312)  # Дополнительные параметры
        self.options[:4] = [99, 130, 83, 99]  # “магическая” метка, описываемая в стандарте RFC-1497
        self.options[4:7] = [53, 1, 2]  # Тип DHCP сообщения, код 53, длина 1, по умолчанию DHCPOFFER
        # Адрес сервера
        self.options[7:13] = bytearray([54, 4]) + bytearray([int(byte) for byte in host_address.split('.')])
        # Время аренды IP адреса, код 51, длина 4, время в секундах
        self.options[13:19] = [51, 4, 255, 255, 255, 255]
        # Маска подсети, код 1, длина 4
        self.options[19:25] = bytearray([1, 4]) + bytearray([int(byte) for byte in subnet_mask.split('.')])
        # Маршрутизатор, код 3, длина = 4 * number_of_routers
        self.options[25:31] = bytearray([3, 4]) + bytearray([int(byte) for byte in default_address.split('.')])
        # Имя домена, код 6, длина = 4 * число серверов
        self.options[31:37] = bytearray([6, 4]) + bytearray([int(byte) for byte in default_address.split('.')])
        self.options[37:38] = [255]  # Конец, код 255, длина 1

    def build(self):
        return self.opcode + self.htype + self.hlen + self.hops + self.xid + self.secs + self.flags + self.ciaddr + self.yiaddr \
               + self.siaddr + self.giaddr + self.chaddr + self.sname + self.file + self.options


def handle_discover(msg, addr):
    print('DHCPDISCOVER accepted')
    send_response(msg, addr)
    print('send DHCPOFFER to: ', addr)


def handle_request(msg, addr):
    print('DHCPREQUEST accepted')
    send_response(msg, addr)
    print('send DHCPACK to: ', addr)


def send_response(msg, addr):
    response = build_packet(msg)
    server_socket.sendto(response, addr)


def handle_decline():
    print('DHCPDECLINE accepted')


def handle_release(msg):
    print('DHCPRELEASE accepted')
    ciaddr = msg[12:16]
    addresses.remove(ciaddr)
    print([byte for byte in ciaddr], 'removed')


def build_packet(msg):
    packet = DHCPPacket()

    packet.xid = msg[4:8]
    packet.flags = msg[10:12]
    packet.ciaddr = msg[12:16]
    packet.giaddr = msg[24:28]
    packet.chaddr = msg[28:44]

    # Если клиент хочет получить какой-то адрес, который не занят, то мы выдаем его,
    # если он занят, либо опция 50 не выставлена, то даем новый
    msg_yiaddr = msg[245:249]
    byte_host_address = bytearray([int(byte) for byte in host_address.split('.')])
    if msg[243] == 50 and msg_yiaddr not in addresses and msg_yiaddr != byte_host_address:
        packet.yiaddr = msg_yiaddr
    else:
        # получаем новый IP адрес
        yiaddr = byte_host_address
        while yiaddr[3] < 255:
            yiaddr[3] = yiaddr[3] + 1
            if yiaddr not in addresses:
                packet.yiaddr = yiaddr
                break

    msg_type = msg[242]
    if msg_type == DHCPDISCOVER:
        packet.options[6] = DHCPOFFER
        print('offered address: ', [byte for byte in packet.yiaddr])
    elif msg_type == DHCPREQUEST:
        packet.options[6] = DHCPACK
        addresses.append(packet.yiaddr)
        print('assigned address: ', [byte for byte in packet.yiaddr])

    return packet.build()


while True:
    message, address = server_socket.recvfrom(1024)
    print('\nmessage received from: ', address)

    message_type = message[242]
    print('message type: ', message_type)

    if message_type == DHCPDISCOVER:
        handle_discover(message, address)
    elif message_type == DHCPREQUEST:
        handle_request(message, address)
    elif message_type == DHCPDECLINE:
        handle_decline()
    elif message_type == DHCPRELEASE:
        handle_release(message)

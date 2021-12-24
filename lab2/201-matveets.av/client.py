import os
import socket
from datetime import datetime
from math import modf
from struct import pack
from struct import unpack

UNIX_START_TIME_SEC = 2208988800  # 1970-01-01

packet_format = '!4B11I'
host_address = 'pool.ntp.org'
port = 123
timeout = 10

print('Server:', host_address)


def unix_to_ntp(dt):
    ntp_second_fractions, ntp_seconds = modf(dt.timestamp() + UNIX_START_TIME_SEC)
    ntp_second_fractions = int(ntp_second_fractions * 2 ** 32)
    ntp_seconds = int(ntp_seconds)
    return ntp_second_fractions, ntp_seconds


def ntp_to_unix(seconds, second_fractions):
    ntp_time = seconds + second_fractions / 2 ** 32
    return datetime.fromtimestamp(ntp_time - UNIX_START_TIME_SEC)


client_second_fractions, client_seconds = unix_to_ntp(datetime.now())

# Клиент заполняет поля (остальные нули):
# 1. version number - 1-4
# 2. mode - 3 (режим клиента)
# 3. transmit - время отправки пакета
# LI: 11 = 3 (unknown); VN: 100 = v4; MODE: 011 = 3 (client)
packet = pack(packet_format, int('11100011', 2), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, client_seconds, client_second_fractions)

# Посылаем запрос на сервер
client_transmit_ts = datetime.now()
server_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
server_socket.sendto(packet, (host_address, port))
server_socket.settimeout(timeout)
try:
    # Парсим ответ
    response = server_socket.recvfrom(1024)[0]
    client_receive_ts = datetime.now()
    if response:
        response_packet = unpack(packet_format, response)
        first_oct = format(response_packet[0], 'b').zfill(8)

        leap = int(first_oct[0:2], 2)  # Индикатор коррекции – целое число, показывающее предупреждение о секунде координации.
        version_number = int(first_oct[2:5], 2)  # Номер версии
        mode = int(first_oct[5:8], 2)  # Режим

        stratum = response_packet[1]  # Часовой слой
        poll_interval = response_packet[2]  # Интервал опроса
        precision = response_packet[3]  # Точность
        root_delay = response_packet[4]  # Задержка сервера
        root_dispersion = response_packet[5]  # Разброс показаний сервера
        reference_identifier = response_packet[6]  # Идентификатор источника
        reference_ts = ntp_to_unix(response_packet[7], response_packet[8])  # Последние показания часов на сервере
        originate_ts = ntp_to_unix(response_packet[9], response_packet[10])  # Время клиента, когда запрос отправляется серверу
        receive_ts = ntp_to_unix(response_packet[11], response_packet[12])  # Время сервера, когда пришло сообщение от клиента
        transmit_ts = ntp_to_unix(response_packet[13], response_packet[14])  # Время отправки пакета с сервера
        rtt = (client_receive_ts - client_transmit_ts).total_seconds() - (transmit_ts - receive_ts).total_seconds()  # Время приема-передачи
        # Смещение времени в миллисекундах между клиентом и сервером
        offset = ((transmit_ts - client_receive_ts).total_seconds() + (receive_ts - client_transmit_ts).total_seconds()) / 2

        print('Server time: ', transmit_ts)
        print('Client time: ', client_transmit_ts)
        print("Round trip time: {:.0f} ms".format(rtt * 1000))
        print("Offset: {:.0f} ms".format(offset * 1000))
except:
    print('Timed out, please try to connect later')
    os._exit(0)

import datetime
from socket import socket, gethostbyname, gethostname, AF_INET, SOCK_DGRAM
from time import time, gmtime, ctime
from select import select
from _thread import start_new_thread
from ntp_packet import NTPPacket

SYSTEM_EPOCH = datetime.date(*gmtime(0)[0:3])
NTP_EPOCH = datetime.date(1900, 1, 1)
NTP_DELTA = (SYSTEM_EPOCH - NTP_EPOCH).total_seconds()
print(f"SE = {SYSTEM_EPOCH}; NE = {NTP_EPOCH}; DELTA = {NTP_DELTA}")
HOST = gethostbyname(gethostname())
PORT = 123
ADDRESS = (HOST, PORT)


def system_to_ntp_time(timestamp):
    return timestamp + NTP_DELTA


def ntp_to_system_time(timestamp):
    return timestamp - NTP_DELTA


def make_resp(data, address, recv_ts):
    recv_packet = NTPPacket()
    recv_packet.unpack(data)

    resp_packet = NTPPacket(version=3, mode=4)
    resp_packet.stratum = 0x2
    resp_packet.poll = 3
    resp_packet.precision = -25
    resp_packet.root_delay = 0x0bfa
    resp_packet.root_dispersion = 0x06a7
    resp_packet.ref_id = 0xa29fc801
    resp_packet.reference_time = recv_ts
    resp_packet.origin_time = recv_packet.transmit_time
    resp_packet.receive_time = recv_ts
    resp_packet.transmit_time = system_to_ntp_time(time())

    server.sendto(resp_packet.pack(), address)
    # print(f"Send to {address[0]}:{address[1]}\n"
    #       f"[DATA]: ts_time = {ctime(ntp_to_system_time(resp_packet.transmit_time))}; "
    #       f"rec_time = {ctime(ntp_to_system_time(resp_packet.receive_time))}; "
    #       f"origin_time = {ctime(ntp_to_system_time(resp_packet.origin_time))};")


server = socket(AF_INET, SOCK_DGRAM)
server.bind(ADDRESS)

print(f"Listening on {HOST}:{PORT}")

while True:
    read_list, _, _ = select([server], [], [], 1)
    if len(read_list) != 0:
        print(f"Receive {len(read_list)} packets")
        for temp_socket in read_list:
            try:
                data, addr = temp_socket.recvfrom(48)
                recv_timestamp = system_to_ntp_time(time())
                start_new_thread(make_resp, (data, addr, recv_timestamp))
            except Exception as e:
                print(e.args)

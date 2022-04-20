import socket
import struct
from random import randint
from uuid import getnode as get_mac
from time import sleep

UDP_PORT_OUT = 67
UDP_PORT_IN = 68
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
sock.bind(('0.0.0.0', UDP_PORT_IN))



def get_mac_address():
    mac = ':'.join(("%012X" % get_mac())[i:i + 2] for i in range(0, 12, 2))  # create string with : separated mac values
    byte_mac = b''
    for element in mac.split(":"):
        byte_mac += struct.pack('!B', int(element, 16))  # create byte package with mac address
    return byte_mac


def generate_transaction_id():
    id = b''
    for i in range(4):
        id += struct.pack('!B', randint(0, 255))
    return id


class Client:

    def __init__(self):
        self.operation = "Unknown"
        self.requests = {'SubnetMask': False, 'DomainName': False, 'Router': False, 'DNS': False, 'StaticRoute': False}
        self.options = {}
        self.data = 0
        self.end_byte = bytes([0xff])
        self.transaction_id = generate_transaction_id()
        self.magic_cookie = bytes([0x63, 0x82, 0x53, 0x63])
        self.subnetMask = b''
        self.offerIP = 0
        self.nextServerIP = 0
        self.router = 0
        self.DNS = 0
        self.leaseTime = 0
        self.DHCPServerIdentifier = 0

    def set_data(self, data):

        self.data = data
        self.options_analyzer()
        self.options_handler()
        if self.operation == "DHCPOFFER":
            self.unpack()
            pack = self.build_request_pack()
            print(f"[ЗАПРОС] REQUEST в ответ на {self.operation} ")
            sock.sendto(pack, ("192.168.0.255", UDP_PORT_OUT))
        elif self.operation == "DHCPACK":
            print(f"[ОТВЕТ] DHCP ACK, IP: {self.offerIP} на: {self.leaseTime}")
            exit(0)

    def options_analyzer(self):
        pointer = 240
        if self.magic_cookie == bytes([0x63, 0x82, 0x53, 0x63]):
            while self.data[pointer] != struct.unpack('!B', self.end_byte)[0]:  # returns tuple
                self.options[f"{self.data[pointer]}"] = list(
                    self.data[i] for i in range(pointer + 2, pointer + 2 + self.data[pointer + 1]))
                pointer = pointer + 1 + self.data[pointer + 1] + 1
        print(f"RECIVED: {self.options}")

    def unpack(self):
        if self.data[4:8] == bytes([0x39, 0x03, 0xF3, 0x26]):
            self.offerIP = '.'.join(map(lambda x: str(x), self.data[16:20]))
            self.nextServerIP = '.'.join(map(lambda x: str(x), self.data[20:24]))

    def build_discover_pack(self):
        mac_address = get_mac_address()
        package = b''
        message_type = bytes([0x01])  # Message type: Boot Request (1)
        hardware_type = bytes([0x01])  # Hardware type: Ethernet
        hardware_add_len = bytes([0x06])  # Hardware address length: 6
        hops = bytes([0x00])  # Hops: 0
        transaction_id = self.transaction_id  # Transaction ID
        seconds_elapsed = bytes([0x00, 0x00])  # Seconds elapsed: 0
        flags = bytes([0x80, 0x00])  # Bootp flags: 0x8000 (Broadcast) + reserved flags
        client_ip = bytes([0x00, 0x00, 0x00, 0x00])  # Client IP address: 0.0.0.0
        your_ip = bytes([0x00, 0x00, 0x00, 0x00])  # Your (client) IP address: 0.0.0.0
        next_server_ip = bytes([0x00, 0x00, 0x00, 0x00])  # Next server IP address: 0.0.0.0
        relay_agent_ip = bytes([0x00, 0x00, 0x00, 0x00])  # Relay agent IP address: 0.0.0.0
        mac_address = get_mac_address() # MAC address of client
        client_hard_address_1 = bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                                       0x00])  # Client hardware address: 00000000000000000000
        client_hard_address_2 = bytes(192)  # Server host name not given and Boot file name not given
        magic_cookie = bytes([0x63, 0x82, 0x53, 0x63])  # Magic cookie: DHCP
        dhcp_option_1 = bytes([0x35, 0x01, 0x01])  # Option: (t=53,l=1) DHCP Message Type = DHCP Discover
        dhcp_option_2 = bytes([0x3d, 0x06]) + mac_address  # Option (t=61 , l=6) Client identifier
        dhcp_option_3 = bytes([0x37, 0x03, 0x03, 0x01, 0x06])  # Option: (t=55,l=3) Parameter Request List
        end_option = bytes([0xff])  # End Option
        package += message_type + hardware_type + hardware_add_len + hops + transaction_id + seconds_elapsed + flags + client_ip + your_ip + next_server_ip + relay_agent_ip + mac_address + client_hard_address_1 + client_hard_address_2 + magic_cookie + dhcp_option_1 + dhcp_option_2 + dhcp_option_3 + end_option
        return package

    def build_request_pack(self):
        mac_address = get_mac_address()
        package = b''
        message_type = bytes([0x01])  # Message type: Boot Request (1)
        hardware_type = bytes([0x01])  # Hardware type: Ethernet
        hardware_add_len = bytes([0x06])  # Hardware address length: 6
        hops = bytes([0x00])  # Hops: 0
        transaction_id = self.transaction_id # Transaction ID
        seconds_elapsed = bytes([0x00, 0x00])  # Seconds elapsed: 0
        flags = bytes([0x80, 0x00]) # Bootp flags: 0x8000 (Broadcast) + reserved flags
        client_ip = bytes([0x00, 0x00, 0x00, 0x00]) # Client IP address: 0.0.0.0
        your_ip = socket.inet_pton(socket.AF_INET, self.offerIP)  # Your (client) IP address
        next_server_ip = bytes([0x00, 0x00, 0x00, 0x00])  # Next server IP address: 0.0.0.0
        relay_agent_ip = bytes([0x00, 0x00, 0x00, 0x00])  # Relay agent IP address: 0.0.0.0
        mac_address = get_mac_address() # may be 0 here
        client_hard_address_1 = bytes([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                                       0x00])  # Client hardware address padding: 00000000000000000000
        client_hard_address_2 = bytes(192)  # Server host name not given and Boot file name not given

        magic_cookie = bytes([0x63, 0x82, 0x53, 0x63])  # Magic cookie: DHCP
        dhcp_option_1 = bytes([0x35, 0x01, 0x03])  # Option: (t=53,l=1) DHCP Message Type = DHCP Request
        dhcp_option_2 = bytes([0x3d, 0x06]) + mac_address   # Option (t=61 , l=6) Client identifier
        dhcp_option_3 = bytes([0x32, 0x04]) + socket.inet_pton(socket.AF_INET,
                                                  self.offerIP) # Option: (t=50,l=1) Requested IP Address
        dhcp_option_4 = bytes([0x36, 0x04]) + socket.inet_pton(socket.AF_INET,
                                                  self.DHCPServerIdentifier) # Option: (t=54,l=1) DHCP server identifer
        dhcp_option_5 = bytes([0x0c, 0x0f]) + bytes(socket.gethostname(), 'utf-8') # Option: (t=12,l=15) Host Name
        dhcp_option_6 = bytes([0x37, 0x03, 0x03, 0x01, 0x06]) # Option: (t=55,l=3) Parameter Request List
        end_option = bytes([0xff])  # End Option
        package += message_type + hardware_type + hardware_add_len + hops + transaction_id + seconds_elapsed + flags + client_ip + your_ip + next_server_ip + relay_agent_ip + mac_address + client_hard_address_1 + client_hard_address_2 + magic_cookie + dhcp_option_1 + dhcp_option_2 + dhcp_option_3 + dhcp_option_4 + dhcp_option_5 + dhcp_option_6 + end_option
        return package

    def options_handler(self):
        for key, value in self.options.items():
            if int(key) == 1:
                self.subnetMask = '.'.join(map(lambda x: str(x), value))
            if int(key) == 3:
                self.router = '.'.join(map(lambda x: str(x), value))
            if int(key) == 6:
                self.DNS = '.'.join(map(lambda x: str(x), value))
            if int(key) == 51:
                self.leaseTime = 0
                for i, elem in enumerate(value):
                    self.leaseTime += 255 ** (3 - i) * elem
            if int(key) == 53:
                if value[0] == 1:
                    self.operation = "DHCPDISCOVER"
                if value[0] == 2:
                    self.operation = "DHCPOFFER"
                if value[0] == 3:
                    self.operation = "DHCPREQUEST"
                if value[0] == 4:
                    self.operation = "DHCPDECLINE"
                if value[0] == 5:
                    self.operation = "DHCPACK"
            if int(key) == 54:
                self.DHCPServerIdentifier = '.'.join(
                    map(lambda x: str(x), value))


if __name__ == '__main__':

    client = Client()
    sock.sendto(client.build_discover_pack(), ('192.168.0.255', UDP_PORT_OUT))
    print("[ЗАПРОС] DHCP DISCOVER ")
    while True:
        recv_data, addr = sock.recvfrom(1024)
        client.set_data(recv_data)
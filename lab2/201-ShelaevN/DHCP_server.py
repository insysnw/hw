
import socket
import threading

from time import time

'''
    This class models a DHCP packet. From RFC 2131:
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |     op (1)    |   htype (1)   |   hlen (1)    |   hops (1)    |
    +---------------+---------------+---------------+---------------+
    |                            xid (4)                            |
    +-------------------------------+-------------------------------+
    |           secs (2)            |           flags (2)           |
    +-------------------------------+-------------------------------+
    |                          ciaddr  (4)                          |
    +---------------------------------------------------------------+
    |                          yiaddr  (4)                          |
    +---------------------------------------------------------------+
    |                          siaddr  (4)                          |
    +---------------------------------------------------------------+
    |                          giaddr  (4)                          |
    +---------------------------------------------------------------+
    |                                                               |
    |                          chaddr  (16)                         |
    |                                                               |
    |                                                               |
    +---------------------------------------------------------------+
    |                                                               |
    |                          sname   (64)                         |
    +---------------------------------------------------------------+
    |                                                               |
    |                          file    (128)                        |
    +---------------------------------------------------------------+
    |                                                               |
    |                          options (variable)                   |
    +---------------------------------------------------------------+
'''

PacketStructDict = { 'op':'Unknown', 'htype':'Unknown', 'hlen':0, 'hops':0, 'xid':0, 'secs':0, 'flags':0, 'ciaddr':'0.0.0.0', \
    'siaddr':'0.0.0.0', 'giaddr':'0.0.0.0', 'chaddr':'Unknown', 'sname':'-', 'file':'-', 'magic_cookie':'99.130.83.99', 'parser':0, \
    'SubnetMask':'255.255.255.0', 'DHCPServerIP':'127.0.0.11', 'Router':'192.168.0.1', 'DNS':'192.168.0.1', 'NTPS':'192.168.0.1', \
    'HostName':'Test', 'IPLeaseTime':60, 'RequestedIpAddress':'0.0.0.0'}

class DHCP_server():

    def __init__(self, PORT_IN, PORT_OUT):
        self.DynamicIPDict = dict()
        self.DynamicIPTime = dict()
        self.DynamicIPArray = [f'192.168.5.{x}' for x in range(35, 255)]
        self.IPADDRESS = self.DynamicIPArray.pop(0)

        self.PORT_IN  = PORT_IN
        self.PORT_OUT = PORT_OUT

        self.SOCKET = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.SOCKET.bind(('127.0.0.11', self.PORT_IN))
        self.SOCKET.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        self.main()
    
    def parserIPAddress(self, address):
        ipAddress = [int.from_bytes(data[x], byteorder = 'big') for x in range(0, len(address))]
        return '.'.join(ipAddress)            

    def findOptions(self, data, res):

        while res['parser'] < len(data):

            parser = res['parser']
            check_byte = int.from_bytes(data[parser:(parser + 1)], byteorder = 'big')
            ln = int.from_bytes(data[(parser + 1):(parser + 2)], byteorder = 'big')

            # Host Name
            if check_byte == 12:
                res['HostName'] = data[(parser + 2):(parser + ln + 3)].decode('utf-8')
              
            # Запрошенный IP-адрес
            elif check_byte == 50:
                res['RequestedIpAddress'] = socket.inet_ntoa(data[(parser + 2):(parser + 6)])

            # IP Lease Time (время аренды IP-адреса)
            elif check_byte == 51:
                res['IPLeaseTime'] = int.from_bytes(data[(parser + 2):(parser + 6)], byteorder = 'big') 

            # Тип запроса
            elif check_byte == 53: 
                op_check = int.from_bytes(data[(parser + 2):(parser + 3)], byteorder = 'big')
                if   op_check == 1:  res['op'] = 'DHCPDISCOVER'
                elif op_check == 2:  res['op'] = 'DHCPOFFER'
                elif op_check == 3:  res['op'] = 'DHCPREQUEST'
                elif op_check == 4:  res['op'] = 'DHCPDECLINE'
                elif op_check == 5:  res['op'] = 'DHCPACK'
                elif op_check == 6:  res['op'] = 'DHCPNAK'
                elif op_check == 7:  res['op'] = 'DHCPRELEASE'
                elif op_check == 8:  res['op'] = 'DHCPINFORM'
                elif op_check == 13: res['op'] = 'LEASEQUERY'

            # IP DHCP-сервера
            elif check_byte == 54:       
                res['DHCPServerIP'] = socket.inet_ntoa(data[(parser + 2):(parser + 6)])

            # MAC-адрес клиента
            elif check_byte == 61:
                if int.from_bytes(data[(parser + 2):(parser + 3)], byteorder = 'big') == 1:
                    res['htype']  = 'Ethernet'      
                    res['chaddr'] = data[(parser + 3):(parser + ln + 3)].hex()
            
            res['parser'] += ln + 2

        return res

    def parserPacket(self, res, data):
        if len(data) < 240:
            print('\n\tError!')
            return res
        op = int.from_bytes(data[:1], byteorder = 'big')
        if   op == 1: res['op'] = 'DHCPDISCOVER/DHCPREQUEST'
        elif op == 2: res['op'] = 'DHCPOFFER/DHCPACK'
        if int.from_bytes(data[1:2], byteorder = 'big') == 1: res['htype'] = 'MAC'
        res['hlen']   = int.from_bytes(data[2:3],   byteorder = 'big')
        res['hops']   = int.from_bytes(data[3:4],   byteorder = 'big')
        res['xid']    = int.from_bytes(data[4:8],   byteorder = 'big')
        res['secs']   = int.from_bytes(data[8:10],  byteorder = 'big')
        res['flags']  = int.from_bytes(data[10:11], byteorder = 'big') >> 7
        res['ciaddr'] = socket.inet_ntoa(data[12:16])
        res['yiaddr'] = socket.inet_ntoa(data[16:20])
        res['siaddr'] = socket.inet_ntoa(data[20:24])
        res['giaddr'] = socket.inet_ntoa(data[24:28])
        res['chaddr'] = data[28:44].hex()
        res['sname']  = data[44:108].decode('utf-8')
        res['file']   = data[108:236].decode('utf-8')
        res['magic_cookie'] = socket.inet_ntoa(data[236:240])
        if res['magic_cookie'] == '99.130.83.99':    
            res = self.findOptions(data[240:len(data)], res)
        return res

    def array2bytes(self, array):
        bytes = b''
        for elem in array:
            bytes += (elem).to_bytes(1, 'big')
        return bytes

    def MAC2bytes(self, MAC):
        bytes = b''
        for i in range(0, len(MAC), 2):
            bytes += int(MAC[i:(i + 2)], 16).to_bytes(1, 'big')
        return bytes

    def createDHCPPacket(self, rev_packet, mode = 'OFFER'):
        packet = self.array2bytes([2, 1, 6, 0])
        packet += (rev_packet['xid']).to_bytes(4, 'big') + (0).to_bytes(8, 'big')
        packet += socket.inet_aton(self.IPADDRESS)
        packet += socket.inet_aton(rev_packet['DHCPServerIP'])
        packet += socket.inet_aton('0.0.0.0')
        packet += self.MAC2bytes(rev_packet['chaddr'])
        packet += (0).to_bytes(192, 'big')
        packet += socket.inet_aton(rev_packet['magic_cookie'])
        packet += self.array2bytes([1, 4]) + socket.inet_aton(rev_packet['SubnetMask'])
        packet += self.array2bytes([3, 4]) + socket.inet_aton(rev_packet['Router'])
        packet += self.array2bytes([6, 4]) + socket.inet_aton(rev_packet['DNS'])
        packet += self.array2bytes([50, 4]) + socket.inet_aton(self.IPADDRESS)
        packet += self.array2bytes([51, 4]) + (rev_packet['IPLeaseTime']).to_bytes(4, 'big')
        if   mode == 'ACK':   packet += self.array2bytes([53, 1, 5])
        elif mode == 'OFFER': packet += self.array2bytes([53, 1, 2])
        elif mode == 'NAK':
            print('\n\t Error! DHCPNAK...(Unvalid IP address)')
            packet += self.array2bytes([53, 1, 6])
        packet += self.array2bytes([54, 4]) + socket.inet_aton(rev_packet['DHCPServerIP'])
        packet += (255).to_bytes(1, 'big') + (0).to_bytes(1, 'big')
        return packet

    def MAC2IPAddress(self, packet):
        lock = threading.Lock()
        lock.acquire()
        try:
            self.DynamicIPDict.setdefault(self.IPADDRESS, packet['chaddr'])
            self.DynamicIPTime.setdefault(self.IPADDRESS, (packet['IPLeaseTime'], time()))
            print('\n Give IP-address: %s to %s in %d' %(self.IPADDRESS, packet['chaddr'], packet['IPLeaseTime']))
            if len(self.DynamicIPArray) == 0: 
                current_time = time()
                for k in self.DynamicIPTime.keys():
                    if self.DynamicIPTime[k][0] + self.DynamicIPTime[k][1] < current_time:
                        self.DynamicIPTime.pop(k)
                        self.DynamicIPDict.pop(k)
                        self.DynamicIPArray.append(k)
            if len(self.DynamicIPArray) > 0:
                self.IPADDRESS = self.DynamicIPArray.pop(0)
            else: self.IPADDRESS = '1.1.1.1'
        finally:
            lock.release()
        return

    def DHCPServer(self, data, address):
        packet = self.parserPacket(PacketStructDict.copy(), data)
        if packet['op'] == 'DHCPDISCOVER':
            packetOffer = self.createDHCPPacket(packet, 'OFFER')
            self.SOCKET.sendto(packetOffer, ('255.255.255.255', self.PORT_OUT))
        elif packet['op'] == 'DHCPREQUEST' or packet['op'] == 'DHCPINFORM':
            mode = 'ACK'
            if self.IPADDRESS == '1.1.1.1': mode = 'NAK'
            packetACK = self.createDHCPPacket(packet, mode)
            self.SOCKET.sendto(packetACK, ('255.255.255.255', self.PORT_OUT))
            if packet['op'] == 'DHCPREQUEST': self.MAC2IPAddress(packet)
        elif packet['op'] == 'DHCPDECLINE' or packet['op'] == 'DHCPRELEASE':
            lock = threading.Lock()
            lock.acquire()
            try:
                self.DynamicIPTime.pop(self.IPADDRESS)
                self.DynamicIPDict.pop(self.IPADDRESS)
                if packet['op'] == 'DHCPRELEASE': self.DynamicIPArray.append(self.IPADDRESS)
                else: print('\n\t Error! DHCPDECLINE...(This IP-address is already used)')
            finally:
                lock.release()       
        return 0

    def main(self):
        thread_array = []
        print('\n\t DHCP Start!!!')
        while True:
            data, addr = self.SOCKET.recvfrom(512)
            threadServer = threading.Thread(target = self.DHCPServer, args = (data, addr))
            thread_array.append(threadServer)
            threadServer.start()
        for thread in thread_array:
            thread.join()
        self.SOCKET.close()
        print('\n\t DHCP Close!!!')
        return 0

if __name__ == '__main__':
    DHCP_server(67, 68)


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

PORT_IN  = 67
PORT_OUT = 68

class DHCP_server():

    DHCPServerIP = '127.0.0.11'
    IP_COOKIE = '99.130.83.99'
    ERROR_IP_ADDRESS = '1.1.1.1'

    PacketStructDict = { 'op':'Unknown', 'htype':'Unknown', 'hlen':0, 'hops':0, 'xid':0, 'secs':0, 'flags':0, 'ciaddr':'0.0.0.0', 'yiaddr':'0.0.0.0',
        'siaddr':'0.0.0.0', 'giaddr':'0.0.0.0', 'chaddr':'Unknown', 'sname':'', 'file':'', 'magic_cookie':IP_COOKIE, 'SubnetMask':'255.255.255.0',
        'DHCPServerIP':DHCPServerIP, 'Router':'192.168.0.1', 'DNS':'192.168.0.1', 'NTPS':'192.168.0.1', 'HostName':'Test', 'IPLeaseTime':60, 'parser':0 }

    def __init__(self, PORT_IN, PORT_OUT):
        self.DynamicIPDict = dict()
        self.DynamicIPTime = dict()
        self.DynamicIPArray = [f'192.168.5.{x}' for x in range(25, 255)]
        self.IP_ADDRESS = self.DynamicIPArray.pop(0)

        self.PORT_OUT = PORT_OUT

        self.SOCKET = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.SOCKET.bind((self.DHCPServerIP, PORT_IN))
        self.SOCKET.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        self.main()            

    def findOptions(self, data, structOptions):
        while structOptions['parser'] < len(data):

            parser = structOptions['parser']
            checkByte = int.from_bytes(data[parser:(parser + 1)], byteorder = 'big')
            ln = int.from_bytes(data[(parser + 1):(parser + 2)], byteorder = 'big')

            # Host Name
            if checkByte == 12:
                structOptions['HostName'] = data[(parser + 2):(parser + ln + 3)].decode('ascii', 'replace')
              
            # Запрошенный IP-адрес
            elif checkByte == 50:
                structOptions['yiaddr'] = socket.inet_ntoa(data[(parser + 2):(parser + 6)])

            # IP Lease Time (время аренды IP-адреса)
            elif checkByte == 51:
                structOptions['IPLeaseTime'] = int.from_bytes(data[(parser + 2):(parser + 6)], byteorder = 'big') 

            # Тип запроса
            elif checkByte == 53: 
                opCheck = int.from_bytes(data[(parser + 2):(parser + 3)], byteorder = 'big')
                if   opCheck == 1:  structOptions['op'] = 'DHCPDISCOVER'
                elif opCheck == 2:  structOptions['op'] = 'DHCPOFFER'
                elif opCheck == 3:  structOptions['op'] = 'DHCPREQUEST'
                elif opCheck == 4:  structOptions['op'] = 'DHCPDECLINE'
                elif opCheck == 5:  structOptions['op'] = 'DHCPACK'
                elif opCheck == 6:  structOptions['op'] = 'DHCPNAK'
                elif opCheck == 7:  structOptions['op'] = 'DHCPRELEASE'
                elif opCheck == 8:  structOptions['op'] = 'DHCPINFORM'
                elif opCheck == 13: structOptions['op'] = 'LEASEQUERY'

            # IP DHCP-сервера
            elif checkByte == 54:
                DHCPServerIP = socket.inet_ntoa(data[(parser + 2):(parser + 6)])
                if DHCPServerIP != structOptions['DHCPServerIP']:
                    structOptions['op'] = 'DHCPNAK'
                    break

            # MAC-адрес клиента
            elif checkByte == 61:
                if int.from_bytes(data[(parser + 2):(parser + 3)], byteorder = 'big') == 1:
                    structOptions['htype']  = 'Ethernet'      
                    structOptions['chaddr'] = data[(parser + 3):(parser + ln + 3)].hex()
            
            structOptions['parser'] += (ln + 2)
        return structOptions

    def MAC_Check(self, MAC_IN):
        lock = threading.Lock()
        lock.acquire()
        try:
            for IP, MAC in self.DynamicIPDict.items():
                if MAC == MAC_IN:
                    self.DynamicIPArray.insert(0, self.IP_ADDRESS)
                    self.IP_ADDRESS = IP
                    break
        finally:
            lock.release()
        return

    def parserPacket(self, structOptions, data):
        if len(data) < 240:
            print('\n\tError! Small data...')
            return structOptions
        op = int.from_bytes(data[:1], byteorder = 'big')
        if   op == 1: structOptions['op'] = 'DHCPDISCOVER/DHCPREQUEST'
        elif op == 2: structOptions['op'] = 'DHCPOFFER/DHCPACK'
        if int.from_bytes(data[1:2], byteorder = 'big') == 1:
            structOptions['htype'] = 'MAC'
        structOptions['hlen']   = int.from_bytes(data[2:3],   byteorder = 'big')
        structOptions['hops']   = int.from_bytes(data[3:4],   byteorder = 'big')
        structOptions['xid']    = int.from_bytes(data[4:8],   byteorder = 'big')
        structOptions['secs']   = int.from_bytes(data[8:10],  byteorder = 'big')
        structOptions['flags']  = int.from_bytes(data[10:11], byteorder = 'big') >> 7
        structOptions['ciaddr'] = socket.inet_ntoa(data[12:16])
        structOptions['yiaddr'] = socket.inet_ntoa(data[16:20])
        structOptions['siaddr'] = socket.inet_ntoa(data[20:24])
        structOptions['giaddr'] = socket.inet_ntoa(data[24:28])
        structOptions['chaddr'] = data[28:44].hex()
        structOptions['sname']  = data[ 44:108].decode('ascii', 'replace')
        structOptions['file']   = data[108:236].decode('ascii', 'replace')
        if socket.inet_ntoa(data[236:240]) == structOptions['magic_cookie']:    
            structOptions = self.findOptions(data[240:], structOptions)

        if structOptions['op'] == 'DHCPDISCOVER' and structOptions['chaddr'] in self.DynamicIPDict.values():
                self.MAC_Check(structOptions['chaddr'])
        if structOptions['op'] == 'DHCPREQUEST'  and structOptions['yiaddr'] != self.IP_ADDRESS:
            self.IP_ADDRESS = self.ERROR_IP_ADDRESS

        return structOptions

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

    def createDHCPPacket(self, structOptions, mode = 'OFFER'):
        packet = self.array2bytes([2, 1, 6, 0])
        packet += (structOptions['xid']).to_bytes(4, byteorder = 'big') + (0).to_bytes(8, byteorder = 'big')
        packet += socket.inet_aton(self.IP_ADDRESS)
        packet += socket.inet_aton(structOptions['DHCPServerIP'])
        packet += socket.inet_aton('0.0.0.0')
        packet += self.MAC2bytes(structOptions['chaddr'])
        packet += (0).to_bytes(192, byteorder = 'big')
        packet += socket.inet_aton(structOptions['magic_cookie'])
        packet += self.array2bytes([1, 4]) + socket.inet_aton(structOptions['SubnetMask'])
        packet += self.array2bytes([3, 4]) + socket.inet_aton(structOptions['Router'])
        packet += self.array2bytes([6, 4]) + socket.inet_aton(structOptions['DNS'])
        packet += self.array2bytes([50, 4]) + socket.inet_aton(self.IP_ADDRESS)
        packet += self.array2bytes([51, 4]) + (structOptions['IPLeaseTime']).to_bytes(4, byteorder = 'big')

        if   mode == 'ACK':
            packet += self.array2bytes([53, 1, 5])
        elif mode == 'OFFER':
            packet += self.array2bytes([53, 1, 2])
        elif mode == 'NAK':
            print('\n\t Error! DHCPNAK...(Unvalid IP address)')
            packet += self.array2bytes([53, 1, 6])

        packet += self.array2bytes([54, 4]) + socket.inet_aton(structOptions['DHCPServerIP'])
        packet += (255).to_bytes(1, 'big') + (0).to_bytes(1, 'big')
        return packet

    def MAC2IPAddress(self, structOptions):
        lock = threading.Lock()
        lock.acquire()
        try:
            self.DynamicIPDict.update({ self.IP_ADDRESS:structOptions['chaddr'] })
            self.DynamicIPTime.update({ self.IP_ADDRESS:(structOptions['IPLeaseTime'], time()) })
            print('\n Give IP-address: %s to %s in %d' %(self.IP_ADDRESS, structOptions['chaddr'], structOptions['IPLeaseTime']))
            if len(self.DynamicIPArray) == 0: 
                current_time = time()
                for IP in self.DynamicIPTime.keys():
                    if (self.DynamicIPTime[IP][0] + self.DynamicIPTime[IP][1]) < current_time:
                        self.DynamicIPTime.pop(IP)
                        self.DynamicIPDict.pop(IP)
                        self.DynamicIPArray.append(IP)
            if len(self.DynamicIPArray) > 0:
                self.IP_ADDRESS = self.DynamicIPArray.pop(0)
            else:
                self.IP_ADDRESS = self.ERROR_IP_ADDRESS
        finally:
            lock.release()
        return

    def DHCPServer(self, data):
        structOptions = self.parserPacket(self.PacketStructDict.copy(), data)
        if   structOptions['op'] == 'DHCPDISCOVER':
            packetOffer = self.createDHCPPacket(structOptions, 'OFFER')
            self.SOCKET.sendto(packetOffer, ('255.255.255.255', self.PORT_OUT))
        elif structOptions['op'] == 'DHCPREQUEST' or structOptions['op'] == 'DHCPINFORM':
            mode = 'ACK'
            if self.IP_ADDRESS == self.ERROR_IP_ADDRESS:
                mode = 'NAK'
            packetACK = self.createDHCPPacket(structOptions, mode)
            self.SOCKET.sendto(packetACK, ('255.255.255.255', self.PORT_OUT))
            if structOptions['op'] == 'DHCPREQUEST':
                self.MAC2IPAddress(structOptions)
        elif structOptions['op'] == 'DHCPDECLINE' or structOptions['op'] == 'DHCPRELEASE':
            lock = threading.Lock()
            lock.acquire()
            try:
                self.DynamicIPTime.pop(self.IP_ADDRESS)
                self.DynamicIPDict.pop(self.IP_ADDRESS)
                if structOptions['op'] == 'DHCPRELEASE':
                    self.DynamicIPArray.append(self.IP_ADDRESS)
                else:
                    print('\n\t Error! DHCPDECLINE...(This IP-address is already used)')
            finally:
                lock.release()
        return 0

    def main(self):
        thread_array = []
        print('\n\t DHCP Start!!!')
        while True:
            data, addr = self.SOCKET.recvfrom(512)
            threadServer = threading.Thread(target = self.DHCPServer, args = (data,))
            thread_array.append(threadServer)
            threadServer.start()
        for thread in thread_array:
            thread.join()
        self.SOCKET.close()
        print('\n\t DHCP Close!!!')
        return 0

if __name__ == '__main__':
    DHCP_server(PORT_IN, PORT_OUT)

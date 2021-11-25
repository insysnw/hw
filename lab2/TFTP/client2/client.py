import socket
from socket import timeout
import utils
import os

HOST = "127.0.0.1"
PORT = 69
SERVER_ADDRESS = (HOST, PORT)
tmpPacket = b""
recieveAddress = (HOST, PORT)
isLastPacket = False
trying = 0
length = 0
blockNumber = 1


def main():
    global trying, recieveAddress, tmpPacket
    clientsocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    while True:
        operation = input(
            "Read from server or write to server? (r or w): ")
        filename = input(
            "Enter filename: ")
        if operation == 'r':
            opcode = utils.RRQ
            request = makeRequest(opcode, filename)
            file = open(filename, 'wb')
            clientsocket.sendto(request, SERVER_ADDRESS)
            while not isLastPacket:
                clientsocket.settimeout(5)
                try:
                    pack, addr = clientsocket.recvfrom(
                        utils.MAX_BLOCK_SIZE)
                    recieveAddress = addr
                    read = recieveFile(pack, addr, file, clientsocket)
                    if read == False:
                        os.remove(filename)
                    trying = 0
                except timeout as e:
                    if trying < 10 and blockNumber == 1:
                        clientsocket.sendto(request, SERVER_ADDRESS)
                        trying += 1
                        print("trying = " + str(trying))
                        print("resend request: ", request)
                        continue
                    if trying < 10 and blockNumber != 1:
                        clientsocket.sendto(tmpPacket, recieveAddress)
                        trying += 1
                        print("trying = " + str(trying))
                        print("resend ack", tmpPacket)
                        continue
                    elif trying == 10:
                        print(e)
                        os.remove(filename)
                        break
            reset()
        if operation == 'w':
            opcode = utils.WRQ
            request = makeRequest(opcode, filename)
            send = True
            try:
                file = open(filename, 'rb')
                if send:
                    clientsocket.sendto(request, SERVER_ADDRESS)
                    clientsocket.settimeout(5)
                    while True:
                        try:
                            pack, addr = clientsocket.recvfrom(utils.MAX_BLOCK_SIZE)
                            recieveAddress = addr
                            trying = 0
                            if not isLastPacket:
                                send = sendFile(pack, clientsocket, file, addr)
                            if isLastPacket:
                                reset()
                                blocks = utils.getBlockNumber(pack)
                                file.close()
                                print(f"Last Acknowldege packet: {pack} + {blocks}")
                                print("finished file writing ")
                                break
                        except timeout as e:
                            if trying < 10 and blockNumber == 1:
                                clientsocket.sendto(request, SERVER_ADDRESS)
                                trying += 1
                                print("trying = " + str(trying))
                                print("resend request: ", request)
                                continue
                            if trying < 10 and blockNumber != 1:
                                dataPacket = tmpPacket
                                clientsocket.sendto(dataPacket, recieveAddress)
                                trying += 1
                                continue
                            if trying == 10:
                                print(e)
                                send = False
                                reset()
                                break
            except FileNotFoundError as e:
                print(e)
                send = False
            reset()


def makeRequest(opcode, filename, mode='octet'):
    request = opcode + filename.encode('utf-8') + b'\x00' + mode.encode('utf-8') + b'\x00'
    return request


def recieveFile(packet, addr, file, s):
    global isLastPacket, blockNumber, tmpPacket
    receiveHost, recievePort = addr
    if receiveHost != HOST:
        return False
    packetLength = len(packet)
    opcode = utils.getOpCode(packet)
    if opcode == utils.ERROR:
        errorCode = utils.getErrorCode(packet)
        errorMessage = utils.getErrorMessage(packet)
        isLastPacket = True
        file.close()
        print(f"Error {errorCode}: {errorMessage}")
        return False
    if opcode == utils.DATA:
        blocks = utils.getBlockNumber(packet)
        if blocks == blockNumber:
            data = utils.getData(packet)
            file.write(data)
            acknowledgePacket = utils.getAcknowledgePacket(blocks)
            tmpPacket = acknowledgePacket
            s.sendto(acknowledgePacket, addr)
            blockNumber += 1
            print(f"Packet number {blocks} is received")
            if packetLength < utils.PACKET_SIZE + 4:
                isLastPacket = True
                file.close()
                print("recieved all")
            return True


def sendFile(packet, s, file, addr):
    global isLastPacket
    global blockNumber, length, tmpPacket
    opcode = utils.getOpCode(packet)
    if opcode == utils.ERROR:
        errorCode = utils.getErrorCode(packet)
        errorMessage = utils.getErrorMessage(packet)
        isLastPacket = True
        file.close()
        print(f"Error {errorCode}: {errorMessage}")
        return False

    if opcode == utils.ACK:
        blocks = utils.getBlockNumber(packet)
        if blocks == blockNumber - 1:
            data = file.read(utils.PACKET_SIZE)
            dataPacket = utils.getDataPacket(blockNumber, data)
            tmpPacket = dataPacket
            s.sendto(dataPacket, addr)
            length = len(data)
            blockNumber += 1
        if length < utils.PACKET_SIZE:
            isLastPacket = True


def reset():
    global blockNumber, isLastPacket
    global trying, length, tmpPacket
    tmpPacket = b""
    isLastPacket = False
    blockNumber = 1
    trying = 0
    length = 0


if __name__ == '__main__':
    main()
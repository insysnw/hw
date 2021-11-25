import socket
from socket import timeout
import utils
import os

HOST = "0.0.0.0"
PORT69 = 69
PORT = 5000
SERVER_ADDR69 = (HOST, PORT69)
SERVER_ADDR = (HOST, PORT)
isLastPacket = False
trying = 0
blockNumberRRQ = 0
blockNumberWRQ = 0
file = {
    "fileName": None,
    "fileData": None
}

def main():
    server69 = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    server = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    server69.bind(SERVER_ADDR69)
    server.bind(SERVER_ADDR)
    while True:
        handleClient(server69, server)


def handleClient(s69, s):
    global blockNumberRRQ, blockNumberWRQ
    global isLastPacket, trying
    while True:
        pack, addr = s69.recvfrom(1024)
        opcode = utils.getOpCode(pack)
        sendPacket = b""
        if opcode == utils.RRQ:
            fileName = utils.getFileName(pack)
            file["fileName"] = fileName
            print(f"Сервер будет отправлять файл по адресу: {addr}")
            try:
                fileData = open(fileName, 'rb')
                file["fileData"] = fileData
                readData = fileData.read(512)
                blockNumberRRQ += 1
                dataPacket = utils.getDataPacket(blockNumberRRQ, readData)
                sendPacket = dataPacket
                s.sendto(dataPacket, addr)
                if(len(dataPacket) < utils.PACKET_SIZE):
                    isLastPacket == True
            except FileNotFoundError:
                errorPacket = utils.getErrorPacket(utils.FILE_NOT_FOUND)
                s.sendto(errorPacket, addr)
                continue
        elif opcode == utils.WRQ:
            fileName = utils.getFileName(pack)
            if os.path.exists(fileName):
                errorPacket = utils.getErrorPacket(utils.FILE_EXIST)
                s.sendto(errorPacket, addr)
                continue
            else:
                file["fileName"] = fileName
                print(f"Сервер будет принимать файл от {addr}")
                fileData = open(fileName, 'wb')
                file["fileData"] = fileData
                acknowledgePacket = utils.getAcknowledgePacket(blockNumberWRQ)
                sendPacket = acknowledgePacket
                s.sendto(acknowledgePacket, addr)
                blockNumberWRQ += 1
        else:
            errorPacket = utils.getErrorPacket(utils.ILLEGAL_OPERATION)
            s.sendto(errorPacket, addr)
            continue
        tmpPacket = sendPacket
        s.settimeout(5)
        while True:
            try:
                pack, addr = s.recvfrom(1024)
                opcode = utils.getOpCode(pack)
                if opcode == utils.ACK:
                    if len(tmpPacket) < utils.PACKET_SIZE + 4:
                        isLastPacket = True
                    num = utils.getBlockNumber(pack)
                    if num == blockNumberRRQ:
                        print(f'Пакет номер {num} отправлен')
                        blockNumberRRQ += 1
                        data = file["fileData"].read(512)
                        dataPacket = utils.getDataPacket(
                            blockNumberRRQ, data)
                        tmpPacket = dataPacket
                        if not isLastPacket:
                            s.sendto(dataPacket, addr)
                        else:
                            print("Файл отправлен")
                            reset()
                            break

                elif opcode == utils.DATA:
                    num = utils.getBlockNumber(pack)
                    if num == blockNumberWRQ:
                        data = utils.getData(pack)
                        file["fileData"].write(data)
                        acknowledgePacket = utils.getAcknowledgePacket(num)
                        tmpPacket = acknowledgePacket
                        s.sendto(acknowledgePacket, addr)
                        blockNumberWRQ += 1
                        print(f'Дата пакет номер {num} получен')

                        if len(data) < utils.PACKET_SIZE:
                            isLastPacket = True
                            print("Файл полностью получен")
                            file["fileData"].close()
                            reset()
                            break

                elif opcode == utils.RRQ or opcode == utils.WRQ:
                    s.sendto(sendPacket, addr)

            except timeout as e:
                if trying < 10 and (blockNumberRRQ == 0 or blockNumberWRQ == 0):
                    print("Переотправляю первый пакет", sendPacket)
                    print(f"попытка №{trying}")
                    s.sendto(sendPacket, addr)
                elif trying < 10 and (blockNumberRRQ != 0 or blockNumberWRQ != 0):
                    print("Переотправляю пакет:", tmpPacket)
                    print(f"попытка №{trying}")
                    s.sendto(tmpPacket, addr)
                elif trying == 10:
                    print(e)
                    reset()
                    break


def reset():
    global blockNumberRRQ, blockNumberWRQ
    global isLastPacket
    isLastPacket = False
    blockNumberRRQ = 0
    blockNumberWRQ = 0
    file["fileName"] = None
    file["fileData"] = None


if __name__ == '__main__':
    main()

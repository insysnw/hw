import time, datetime, socket, struct
from socket import timeout


class Packet:
    FORMAT = "!B B b b 11I"

    def __init__(self, versionNumber=2, mode=3, transmit=0):
        self.leapIndicator = 0
        self.versionNumber = versionNumber
        self.mode = mode
        self.stratum = 0
        self.pool = 0
        self.precision = 0
        self.rootDelay = 0
        self.rootDispersion = 0
        self.referenceID = 0
        self.reference = 0
        self.originate = 0
        self.receive = 0
        self.transmit = transmit

    def pack(self):
        byte0 = '{0:02b}'.format(self.leapIndicator) + '{0:03b}'.format(self.versionNumber) + '{0:03b}'.format(
            self.mode)
        return struct.pack(Packet.FORMAT,
                           int(byte0, 2),
                           self.stratum,
                           self.pool,
                           self.precision,
                           int(self.rootDelay) + self.getFractionalPart(self.rootDelay, 16),
                           int(self.rootDispersion) +
                           self.getFractionalPart(self.rootDispersion, 16),
                           self.referenceID,
                           int(self.reference),
                           self.getFractionalPart(self.reference, 32),
                           int(self.originate),
                           self.getFractionalPart(self.originate, 32),
                           int(self.receive),
                           self.getFractionalPart(self.receive, 32),
                           int(self.transmit),
                           self.getFractionalPart(self.transmit, 32))

    def unpack(self, data: bytes):
        unpacked = struct.unpack(Packet.FORMAT, data)
        byte0 = '{0:08b}'.format(unpacked[0])
        rootDelayBits = '{0:032b}'.format(unpacked[4])
        rootDispersionBits = '{0:032b}'.format(unpacked[5])
        referenceIDBits = '{0:032b}'.format(unpacked[6])
        self.leapIndicator = int(byte0[0:2], 2)
        self.versionNumber = int(byte0[2:5], 2)
        self.mode = int(byte0[5:], 2)
        self.stratum = unpacked[1]
        self.pool = unpacked[2]
        self.precision = unpacked[3]
        self.rootDelay = (int(rootDelayBits[:16], 2) / 2 ** 16) + (int(rootDelayBits[16:], 2) / 2 ** 16)
        self.rootDispersion = (int(rootDispersionBits[:16], 2) / 2 ** 16) + (int(rootDispersionBits[16:], 2) / 2 ** 16)
        self.referenceID = f"{(int(referenceIDBits[0:8], 2))}.{(int(referenceIDBits[8:16], 2))}.{(int(referenceIDBits[16:24], 2))}.{(int(referenceIDBits[24:], 2))}"
        self.reference = unpacked[7] + unpacked[8] / 2 ** 32
        self.originate = unpacked[9] + unpacked[10] / 2 ** 32
        self.receive = unpacked[11] + unpacked[12] / 2 ** 32
        self.transmit = unpacked[13] + unpacked[14] / 2 ** 32
        return self

    def getDifference(self, arriveTime):
        travelTime = ((arriveTime - self.originate) - (self.transmit - self.receive)) / 2
        return self.receive - self.originate - travelTime

    def getFractionalPart(self, number, precision):
        return int((number - int(number)) * 2 ** precision)


seventyYears = 2208988800
host = "pool.ntp.org"
port = 123
trying = 0


def formatTime(time):
    return datetime.datetime.fromtimestamp(time - seventyYears).strftime("%m/%d/%Y, %H:%M:%S:%f")


while True:
    try:
        packet = Packet(versionNumber=2, mode=3, transmit=time.time() + seventyYears)
        answer = Packet()
        clientSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        clientSocket.settimeout(5)
        clientSocket.sendto(packet.pack(), (host, port))
        data = clientSocket.recv(48)
        arriveTime = time.time() + seventyYears
        answer.unpack(data)

        timeDifference = answer.getDifference(arriveTime)
        result = "Разница во времени: {}\nВремя клиента сейчас: {}\nВремя сервера сейчас: {}\n".format(
            timeDifference,
            datetime.datetime.fromtimestamp(time.time()).strftime("%m/%d/%Y, %H:%M:%S:%f"),
            datetime.datetime.fromtimestamp(time.time() + timeDifference).strftime("%m/%d/%Y, %H:%M:%S:%f"))
        print(result)
        print("Информация о полученном пакете:")
        print(f"Индикатор коррекции: {answer.leapIndicator}")
        print(f"Номер версии: {answer.versionNumber}")
        print(f"Режим: {answer.mode}")
        print(f"Часовой слой: {answer.stratum}")
        print(f"Интервал опроса: {answer.pool}")
        print(f"Точность: {answer.precision}")
        print(f"Задержка: {answer.rootDelay}")
        print(f"Дисперсия: {answer.rootDispersion}")
        print(f"Идентификатор источника: {answer.referenceID}")
        print(f"Время обновления: {formatTime(answer.reference)}")
        print(f"Начальное время: {formatTime(answer.originate)}")
        print(f"Время приёма: {formatTime(answer.receive)}")
        print(f"Время отправки: {formatTime(answer.transmit)}")
        break
    except timeout as e:
        if trying < 10:
            trying += 1
            print(f"попытка №{trying} - сервер не ответил")
        else:
            print("Сервер не отвечает")
            break

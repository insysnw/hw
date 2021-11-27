RRQ = b'\x00\x01'
WRQ = b'\x00\x02'
DATA = b'\x00\x03'
ACK = b'\x00\x04'
ERROR = b'\x00\x05'
UNKNOWN = b'\x00\x00'
FILE_NOT_FOUND = b'\x00\x01'
ACCESS_VIOLATION = b'\x00\x02'
DISK_FULL = b'\x00\x03'
ILLEGAL_OPERATION = b'\x00\x04'
UNKNOWN_TRANFER_ID = b'\x00\x05'
FILE_EXIST = b'\x00\x06'
NO_SUCH_USER = b'\x00\x07'
ERR_MESSAGE = {
    UNKNOWN: "Error not defined",
    FILE_NOT_FOUND: "File not found",
    ACCESS_VIOLATION: "Access violation",
    DISK_FULL: "Disk full or allocation exceeded",
    ILLEGAL_OPERATION: "Illegal TFTP operation",
    FILE_EXIST: "File already exists",
    NO_SUCH_USER: "No such user"
}


def getDataPacket(blockNumber, data):
    bytesNumber = blockNumber.to_bytes(2, 'big')
    bytesData = DATA + bytesNumber + data
    return bytesData


def getAcknowledgePacket(blockNumber):
    bytesNumber = blockNumber.to_bytes(2, 'big')
    bytesAcknowledge = ACK + bytesNumber
    return bytesAcknowledge


def getErrorPacket(type):
    return ERROR + type + ERR_MESSAGE[type].encode('utf-8') + b'\x00'


def getOpCode(packet):
    return packet[0:2]


def getBlockNumber(packet):
    number = int.from_bytes(packet[2:4], 'big')
    return number


def getData(packet):
    return packet[4:]


def getErrorCode(packet):
    return int.from_bytes(packet[2:4], 'big')


def getErrorMessage(packet):
    return packet[4:-1].decode('utf-8')


def getFileName(packet):
    return packet[2:-1].partition(b'\x00')[0].decode('utf-8')


PACKET_SIZE = 512
MAX_BLOCK_SIZE = 1024

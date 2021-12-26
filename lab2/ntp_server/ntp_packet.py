import struct


class NTPPacket:
    packet_format = "!B B b b 11I"

    def __init__(self, version=2, mode=3, transmit=0):
        self.li = 0  # leap indicator
        self.version = version  # version number
        self.mode = mode  # Mode 3 = client; 4 = server;
        self.stratum = 0  # The number of layers to server with reference_time time
        self.poll = 0  # Expected interval between requests (log2)
        self.precision = 0  # Precision
        self.root_delay = 0  # Interval for time to reach NTP-server
        self.root_dispersion = 0  # NTP-server dispersion
        self.ref_id = 0  # ID of server's source of time
        self.reference_time = 0  # Last time on server
        self.origin_time = 0  # Time of sending packet to server
        self.receive_time = 0  # Time of receiving packet on server
        self.transmit_time = transmit  # Time of sending answer from server

    def pack(self):
        return struct.pack(
            NTPPacket.packet_format,
            (self.li << 6) + (self.version << 3) + self.mode,
            self.stratum,
            self.poll,
            self.precision,
            int(self.root_delay) + get_fraction(self.root_delay, 16),
            int(self.root_dispersion) + get_fraction(self.root_dispersion, 16),
            self.ref_id,
            int(self.reference_time),
            get_fraction(self.reference_time, 32),
            int(self.origin_time),
            get_fraction(self.origin_time, 32),
            int(self.receive_time),
            get_fraction(self.receive_time, 32),
            int(self.transmit_time),
            get_fraction(self.transmit_time, 32)
        )

    def unpack(self, data: bytes):
        unpacked_data = struct.unpack(NTPPacket.packet_format, data)
        self.li = unpacked_data[0] >> 6
        self.version = (unpacked_data[0] >> 3) & 0b111
        self.mode = unpacked_data[0] & 0b111

        self.stratum = unpacked_data[1]
        self.poll = unpacked_data[2]
        self.precision = unpacked_data[3]

        self.root_delay = (unpacked_data[4] >> 16) + (unpacked_data[4] & 0xFFFF) / 2 ** 16
        self.root_dispersion = (unpacked_data[5] >> 16) + (unpacked_data[5] & 0xFFFF) / 2 ** 16

        self.ref_id = str((unpacked_data[6] >> 24) & 0xFF) + " " + str((unpacked_data[6] >> 16) & 0xFF) + " " + str(
            (unpacked_data[6] >> 8) & 0xFF) + " " + str(unpacked_data[6] & 0xFF)

        self.reference_time = unpacked_data[7] + unpacked_data[8] / 2**32
        self.origin_time = unpacked_data[9] + unpacked_data[10] / 2**32
        self.receive_time = unpacked_data[11] + unpacked_data[12] / 2*32
        self.transmit_time = unpacked_data[13] + unpacked_data[14] / 2**32

        return self


def get_fraction(number, precision):
    return int((number - int(number)) * 2 ** precision)

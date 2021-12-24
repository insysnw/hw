
import asyncio
import socket
import concurrent.futures

from datetime import timezone, datetime

'''
Protocol:
    Time(5 + 6 + 6 = 17) | Length_name(6) | Flag_file(1) | Name() | Message()
'''

class TCP_chat_async_server():

    def __init__(self):
        self.FILE_PATH = '../TCP_server'
        self.FILE_SPLIT = '?/:'

        self.CLIENTS = {'TCP-server':'-'}
        self.main()

    async def bitToInt(self, data):
        result = 0
        for i in range(0, len(data)):
            result = result * 2 + int(data[i])
        return result

    async def byte_decode(self, data_bin):
        res_str = ''
        for i in range(0, len(data_bin), 8):
            res_str += int(data_bin[i:(i + 8)], 2).to_bytes(1, byteorder = 'big').decode('utf-8') 
        return res_str

    async def bit_zfill(self, mess, length):
        return format(mess, 'b').zfill(length)

    async def send_answer(self, connect, message):
        loop = asyncio.get_event_loop()
        time = datetime.now(timezone.utc).time()
        name_encode = 'TCP-server'.encode('utf-8')
        message_encode = message.encode('utf-8')
        message_bit  = await self.bit_zfill(time.hour, 5) + await self.bit_zfill(time.minute, 6) + await self.bit_zfill(time.second, 6)
        message_bit += (await self.bit_zfill(len(name_encode), 6) + format(False, '1b'))
        message_send = b''
        for i in range(0, 24, 8):
            message_send += int(message_bit[i:(i + 8)], 2).to_bytes(1, byteorder = 'big')
        message_send += (name_encode + message_encode)
        try:
            connect.write(message_send)
            await connect.drain()
        except ConnectionError as e:
            pass
        return

    async def make_file(self, message):
        message_split = message.split(self.FILE_SPLIT, 1)
        file_path = self.FILE_PATH + '/' + message_split[0]
        with open(file_path, 'w') as file:
            file.writelines(message_split[1].split('\r'))
        return

    async def makeNewData(self, data, message):
        name_encode = 'TCP-server'.encode('utf-8')
        message_encode = message.encode('utf-8')
        message_bit = data[:17] + await self.bit_zfill(len(name_encode), 6) + format(False, '1b')
        dataNew = b''
        for i in range(0, 24, 8):
            dataNew += int(message_bit[i:(i + 8)], 2).to_bytes(1, byteorder = 'big')
        dataNew += (name_encode + message_encode)
        return dataNew

    async def stream_parser(self, data, connect):
        data_hex = data.hex()
        data_bin = (bin(int(data_hex[0], 16))[2:]).zfill(4)
        for i in range(1, len(data_hex)):
            data_bin += (bin(int(data_hex[i], 16))[2:]).zfill(4)
        length_name = await self.bitToInt(data_bin[17:23]) * 8
        flag_file = await self.bitToInt(data_bin[23:24])
        name_end = 24 + length_name
        name = await self.byte_decode(data_bin[24:name_end])    
        message = await self.byte_decode(data_bin[name_end:])
        # print(connect.getsockname())
        if name in self.CLIENTS.keys() and self.CLIENTS[name] != connect:
            await self.send_answer(connect, 'ERROR-Name')
            return
        dataNew = data
        if name not in self.CLIENTS.keys():
            self.CLIENTS.setdefault(name, connect)
            dataNew = await self.makeNewData(data_bin, f'\t <{name}> joined the chat!')
        if message.lower() in ['-q', 'quit', 'exit']:
            self.CLIENTS.pop(name)
            dataNew = await self.makeNewData(data_bin, f'\t <{name}> exited from the chat!')
        if flag_file: await self.make_file(message)
        # print(dataNew)
        for client in self.CLIENTS:
            if client != 'TCP-server':
                self.CLIENTS[client].write(dataNew)
                await self.CLIENTS[client].drain()
        return

    async def handle_client(self, reader, writer):
        loop = asyncio.get_event_loop()
        data_full = await reader.read(255)
        await self.stream_parser(data_full, writer)
        return

    async def run_server(self):
        print('\n\t TCP server ready!\n')
        server = await asyncio.start_server(self.handle_client, '', 7575, family = socket.AF_INET)
        async with server:
            await server.serve_forever()

    def main(self):
        asyncio.run(self.run_server())
        self.SOCKET.close()
        return 0

if __name__ == '__main__':
    TCP_chat_async_server()

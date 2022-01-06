
import asyncio
import socket
import concurrent.futures

from datetime import timezone, datetime

PORT = 7575
ENCODING = 'utf-8'

'''
TCP_Protocol:
    Time(5 + 6 + 6 = 17) | Length_name(6) | Flag_file(1) | Name() | Message()
'''

class TCP_chat_async_server():

    def __init__(self, PORT, ENCODING):

        self.NAME = 'TCP-server'
        self.CLIENTS = {self.NAME:'-'}
       
        self.PORT = PORT
        self.ENCODING = ENCODING
        self.main()

    async def message2Bit(self, message, lenght):
        return format(message, 'b').zfill(lenght)

    async def makeNewTime(self):
        time = datetime.now(timezone.utc).time()
        newData = await self.message2Bit(time.hour, 5) + await self.message2Bit(time.minute, 6) + await self.message2Bit(time.second, 6)
        return newData

    async def makeNewDataWithoutTime(self, dataTime, name, message):
        messageBit = dataTime + await self.message2Bit(len(name), 6) + format(False, '1b')
        dataNew = int(messageBit, 2).to_bytes(3, byteorder = 'big') + name.encode(self.ENCODING) + message.encode(self.ENCODING)
        return dataNew

    async def clientErrorDelete(self, name, dataTime, clientError):
        dataNew = await self.makeNewDataWithoutTime(dataTime, name, '-q')
        await self.stream_parser(dataNew, self.CLIENTS[name], clientError)
        return

    async def sendMessage(self, connect, name, message):
        try:
            connect.write(message)
            await connect.drain()
        except ConnectionError as e:
            print(f'\n\t Warning! Lost connection with <{name}>!')
            return 1
        return 0

    async def stream_parser(self, data, connect, nameError = []):
        length_name = ((int.from_bytes(data[2:3], byteorder = 'big') % 128) >> 1) + 3
        name = data[3:length_name].decode(self.ENCODING)
        message = data[length_name:].decode(self.ENCODING)
        if name in self.CLIENTS.keys() and self.CLIENTS[name] != connect:
            dataNew = await self.makeNewDataWithoutTime(await self.makeNewTime(), self.NAME, 'ERROR-Name')
            await self.sendMessage(connect, name, dataNew)
            return True
        dataTime = bin(int.from_bytes(data[:3], byteorder = 'big') >> 7)[2:]
        dataNew = data
        if name not in self.CLIENTS.keys():
            self.CLIENTS.setdefault(name, connect)
            dataNew = await self.makeNewDataWithoutTime(dataTime, self.NAME, f'<{name}> joined the chat!')
        flagWork = True
        if message.lower() in ['-q', 'quit', 'exit']:
            flagWork = False
            if name in nameError:
                dataNew = await self.makeNewDataWithoutTime(dataTime, self.NAME, f'Lost connection with <{name}>!')
            else:
                dataNew = await self.makeNewDataWithoutTime(dataTime, self.NAME, f'<{name}> exited from the chat!')
                await self.sendMessage(self.CLIENTS[name], name, dataNew)
            self.CLIENTS.pop(name)
        clientError = []
        for client in self.CLIENTS.keys():
            if client != 'TCP-server' and client not in nameError:
                if await self.sendMessage(self.CLIENTS[client], client, dataNew):
                    clientError.append(client)
        for name in clientError:
            await self.clientErrorDelete(name, dataTime, clientError)
        return flagWork

    async def handle_client(self, reader, writer):
        flagWork = True
        while flagWork:
            data_full = b''
            while True:
                try:
                    data = await reader.read(255)
                    data_full += data
                    if len(data) < 255:
                        break
                except ConnectionError as e:
                        flagWork = False
                        print(f'\n\t Warning! Lost connection...!')
                        break
            if len(data_full) > 0:
                flagWork = await self.stream_parser(data_full, writer)
        return

    async def run_server(self):
        print('\n\t TCP server (async) ready!\n')
        server = await asyncio.start_server(self.handle_client, '', self.PORT, family = socket.AF_INET)
        async with server:
            await server.serve_forever()

    def main(self):
        asyncio.run(self.run_server())
        self.SOCKET.close()
        return 0

if __name__ == '__main__':
    TCP_chat_async_server(PORT, ENCODING)

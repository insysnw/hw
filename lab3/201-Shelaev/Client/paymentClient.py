
import json
import http.client as HC
import requests

from requests.auth import HTTPBasicAuth
from hashlib import sha256

class PaymentClient():

    def __init__(self):
        self.URL = ''
        self.HEADER = {'Content-type': 'application/json'}
        self.COMMAND = ['exit', 'login', 'logout', 'register', 'wallets', 'account', 'create', 'transfer']
        self.main()

    def connection(self):
        address = input('\n \t Please, enter IP-address and PORT (IP:PORT) > ').strip().split(':')
        if len(address) != 2:
            print('\n ERROR: Incorrect input')
            return
        try:
            connection = HC.HTTPConnection(address[0], int(address[1]))
            connection.connect()
            self.URL = 'http://' + address[0] + ':' + address[1]
        except ValueError:
            print('\n ERROR: Incorrect input')
            return
        except ConnectionError:
            print('\n ERROR: Lost connection...!')
            return
        return connection

    def logout(self, login, password):
        conn = requests.post(self.URL + f'/logout', data = json.dumps({'login': login}), 
                             auth = HTTPBasicAuth(login, password), headers = self.HEADER)
        if   conn.status_code == 200:
            print('\n OK: You have successfully logged out')
        elif conn.status_code == 403:
            print('\n ERROR: Incorrect request')
        else:
            print(f'\n ERROR: {conn.status_code}')

    def login(self, connection):
        login = input('\n\t Please, enter your login    > ').strip()
        password = input('\t And now enter your password > ').strip()
        password_hash = sha256(password.encode('utf-8')).hexdigest()
        connection.request('POST', f'/login', json.dumps({'login': login, 'password': password_hash}), self.HEADER)
        response = connection.getresponse()
        if   response.status == 200:
            print('\n OK: You have successfully logged in')
            return (login, password_hash)
        elif response.status == 403:
            print('\n ERROR: Incorrect request')
        elif response.status == 404:
            print('\n ERROR: Incorrect login and/or password')
        else:
            print(f'\n ERROR: {conn.status_code}')
        return (login, 'password')        

    def register(self, connection):
        login = input('\n\t Please, enter your new login    > ').strip()
        password = input('\t And now enter your new password > ').strip()
        password_hash = sha256(password.encode('utf-8')).hexdigest()
        connection.request('POST', f'/create/client/', json.dumps({'login': login, 'password': password_hash}), self.HEADER)
        response = connection.getresponse()
        if   response.status == 201:
            wallet = json.loads(response.read().decode('utf-8'))['wallet']
            print(f'\n Your new wallet {wallet} with a value of 0')
            return (login, password_hash)
        elif response.status == 403:
            print('\n ERROR: Incorrect request')
        elif response.status == 400:
            print('\n ERROR: This login is already occupied, choose another one')
        else:
            print(f'\n ERROR: {conn.status_code}')
        return (login, 'password')

    def getWallets(self, login, password):
        conn = requests.get(self.URL + '/get/wallets/', data = json.dumps({'login': login}), 
                            auth = HTTPBasicAuth(login, password), headers = self.HEADER)
        if   conn.status_code == 200:
            wallets = conn.json()['wallets']
            print(f'\n Your wallets: {wallets}')
            return wallets
        elif conn.status_code == 403:
            print('\n ERROR: Incorrect request')
        elif conn.status_code == 404:
            print('\n ERROR: There is no user with this login, try again')
        else:
            print(f'\n ERROR: {conn.status_code}')    

    def getAccount(self, login, password):
        wallet_number = input('\n\t Please, enter Your wallet number (default: all) > ').strip()
        wallets = []
        try:
            wallet_number_int = int(wallet_number)
            wallets.append(wallet_number_int)
        except ValueError:
            wallets = self.getWallets(login, password)
        print('')
        for wallet_num in wallets:
            conn = requests.get(self.URL + f'/get/wallet/{wallet_num}', data = json.dumps({'login': login}), 
                            auth = HTTPBasicAuth(login, password), headers = self.HEADER)
            if   conn.status_code == 200:
                account = conn.json()['account']
                print(f' In the wallet {wallet_num} You have {account}')
                continue
            elif conn.status_code == 403:
                print(' ERROR: Incorrect request')
            elif conn.status_code == 404:
                print(' ERROR: There is no user with this login and/or no wallet with this number, try again')
            else:
                print(f' ERROR: {conn.status_code}')
            return

    def createWallet(self, login, password):
        conn = requests.post(self.URL + '/create/wallet/', data = json.dumps({'login': login}), 
                            auth = HTTPBasicAuth(login, password), headers = self.HEADER)
        if   conn.status_code == 201:
            wallet = conn.json()['wallet']
            print(f'\n Your new wallet: {wallet} with 0')
        elif conn.status_code == 403:
            print('\n ERROR: Incorrect request')
        elif conn.status_code == 404:
            print('\n ERROR: There is no user with this login, try again')
        else:
            print(f'\n ERROR: {conn.status_code}')

    def transfer(self, login, password):
        from_wallet = input('\n\t Please, enter Your wallet number (transfer from) > ').strip()
        account = input('\t Please, enter the amount You want to transfer > ').strip()
        try:
            from_wallet_int = int(from_wallet)
            account_double = float(account)
        except ValueError:
            print('\n ERROR: Incorrect input')
            return
        data = json.dumps({'login': login, 'wallet': from_wallet_int, 'sum': account_double})
        conn = requests.post(self.URL + f'/create/payment/', data = data, 
                            auth = HTTPBasicAuth(login, password), headers = self.HEADER)
        flag_continue = True
        if conn.status_code != 201:
            flag_continue = False
        if flag_continue:
            result = conn.json()
            print(f"\n In the wallet {result['wallet']}, now You have {result['account']}" + 
                f" and will become {result['new_account']}")
            flag_continue = input('\n\t Do you agree with the transfer? (default: N) > ').strip().lower()
            if not flag_continue.startswith('y'):
                return
            to_wallet = input('\n\t Please, enter wallet number (transfer to) > ').strip()
            try:
                to_wallet_int = int(to_wallet)
            except ValueError:
                print('\n ERROR: Incorrect input')
                return
            data = json.dumps({'from_wallet': from_wallet_int, 'to_wallet': to_wallet_int, 'sum': account_double})
            conn = requests.post(self.URL + f'/create/payment/transfer', data = data, 
                            auth = HTTPBasicAuth(login, password), headers = self.HEADER)
        if   conn.status_code == 201:
            result = conn.json()
            print(f"\n The transfer was completed! Your wallet {result['wallet']} has become {result['account']}")
        elif conn.status_code == 400:
            print('\n ERROR: The amount to transfer exceeds the value on your wallet')
        elif conn.status_code == 403:
            print('\n ERROR: Incorrect request')
        elif conn.status_code == 404:
            print('\n ERROR: There is no user with this login and/or no wallet with this number, try again')
        else:
            print(f'\n ERROR: {conn.status_code}')

    def main(self):
        conn = self.connection()
        if conn is None:
            return 0
        print('\n   Good connection. Please, log in or register ("login" or "register")')
        connected = True
        login = 'admin'
        password = 'password'
        while connected:
            command = input('\n\t Enter your command > ').strip().lower()
            try:               
                if password == 'password':
                    if command == 'exit':
                        connected = False
                    elif command == 'login':
                        login, password = self.login(conn)
                    elif command == 'register':
                        login, password = self.register(conn)
                    elif command in self.COMMAND:
                        print('\n Warning! You are not logged in and most of the commands are unavailable')
                    else:
                        print('\n ERROR! Wrong command')
                else:
                    if command == 'exit' or command == 'logout':
                        self.logout(login, password)
                        login, password = ('admin', 'password')
                        if command == 'exit':
                            connected = False
                    elif command == 'wallets':
                        self.getWallets(login, password)
                    elif command == 'account':
                        self.getAccount(login, password)
                    elif command == 'create':
                        self.createWallet(login, password)
                    elif command == 'transfer':
                        self.transfer(login, password)
                    elif command in self.COMMAND:
                        print('\n Warning! You have already logged in to the system')
                    else:
                        print('\n ERROR! Wrong command')
            except ConnectionError or requests.exceptions.ConnectionError:
                print('\n\t ERROR: Lost connection...!')
                connected = False
        conn.close()
        print('\n Bye...')
        return 0

if __name__ == '__main__':
    PaymentClient()

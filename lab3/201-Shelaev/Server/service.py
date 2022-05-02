
from hashlib import sha256

CLIENTS  = [
    {'login': 'admin', 
     'password': sha256('password'.encode('utf-8')).hexdigest(),
     'wallets': [201, 555]
     }]
ACCOUNTS = {201: 1200.0, 555: 0.0}

def create_client(login, password):
    client = list(filter(lambda wal: wal['login'] == login, CLIENTS))
    if client:
        return
    new_wallet_number = list(ACCOUNTS.keys())[-1] + 1
    new_client = {'login': login,
                  'password': password,
                  'wallets': [new_wallet_number]}
    CLIENTS.append(new_client)
    ACCOUNTS[new_wallet_number] = 0.0
    return new_wallet_number

def create_wallet(login):
    client = list(filter(lambda wal: wal['login'] == login, CLIENTS))
    if not client:
        return
    new_wallet_number = list(ACCOUNTS.keys())[-1] + 1
    client[0]['wallets'].append(new_wallet_number)
    ACCOUNTS[new_wallet_number] = 0.0
    return new_wallet_number

def create_payment(login, wallet, account):
    client = list(filter(lambda wal: wal['login'] == login, CLIENTS))
    try:
        wallet_number = int(wallet)
        account_double = round(float(account), 3)
    except ValueError:
        return
    if not (client and wallet_number in client[0]['wallets']):
        return
    new_account = ACCOUNTS[wallet_number] - account_double
    if new_account < 0:
        return -1
    return (wallet_number, account_double, new_account)

def do_transfer(from_wallet, to_wallet, account):
    try:
        from_wallet_number = int(from_wallet)
        to_wallet_number = int(to_wallet)
        account_double = round(float(account), 3)
    except ValueError:
        return
    if not to_wallet_number in ACCOUNTS.keys():
        return
    ACCOUNTS[from_wallet_number] -= account_double
    ACCOUNTS[to_wallet_number]   += account_double
    return (from_wallet_number, ACCOUNTS[from_wallet_number])

def verify_password(login, password):
    client = list(filter(lambda wal: wal['login'] == login, CLIENTS))
    if not client:
        return
    if client[0]['password'] == password:
        return client[0]['login']

def get_wallets(login):
    wallet = list(filter(lambda wal: wal['login'] == login, CLIENTS))
    if wallet:
        return wallet[0]['wallets']

def get_account(login, wallet_number):
    wallet = list(filter(lambda wal: wal['login'] == login, CLIENTS))
    if wallet and (wallet_number in wallet[0]['wallets']) and (wallet_number in ACCOUNTS.keys()):
        return ACCOUNTS[wallet_number]

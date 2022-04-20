import sys
import argparse
from threading import Thread
import json
import http.client as c


parser = argparse.ArgumentParser()
parser.add_argument('-ip', type=str)
parser.add_argument('-p', type=int)

arguments = parser.parse_args(sys.argv[1:])

conn = c.HTTPConnection(arguments.ip, arguments.p)
conn.connect()

role = input('Would you be croupier (C) or player (P)? ')
isCroupier = role == 'C'
login = input('Write your login: ')

conn.request('GET', f'/login?login={login}&isCroupier={isCroupier}')
response = conn.getresponse()
if response.status != 200:
    if response.status == 401:
        print(f'Error {response.status}. The croupier already exists or there is another user with the same login.')
    else:
        print(f'Error {response.status}')
    sys.exit()
token = response.read().decode('utf-8')


conn.request("GET", '/game/info', headers={'authorization': f'Bearer {token}'})
response = conn.getresponse()
if response.status != 200:
    if response.status == 403:
        print(f'Error {response.status}. The croupier already exists or there is another user with the same login.')
    else:
        print(f'Error {response.status}')
response.read()

connected = response.status == 200


def send():
    global connected
    while connected:
        msg = input()

        if msg == '!exit':
            conn.request('POST', '/logout', headers={'authorization': f'Bearer {token}'})
            connected = False
            conn.close()
            break

        elif msg == '!game_info':
            conn.request('GET', '/game/info', headers={'authorization': f'Bearer {token}'})
            r = conn.getresponse()
            if response.status != 200:
                print(f'Error {response.status}.')
                continue
            gameInfo = json.loads(r.read().decode('utf-8'))
            print(f'Game number = {gameInfo["numberGame"]}. '
                  f'There is{" " if gameInfo["hasCroupier"] else " not "}croupier on the table.')
            if len(gameInfo['bets']) != 0:
                print('Bets:')
                for bet_user, bet_info in gameInfo["bets"].items():
                    print(f'Player {bet_user} bets {bet_info["sum"]} on {(bet_info["type"]["type"]).lower()}', end='')
                    if bet_info["type"]["type"] == "NUM":
                        print(f'ber {bet_info["type"]["num"]}')
                    else:
                        print()
            else:
                print('There is not a single bet in the current game.')

        elif msg.startswith('!result'):
            _, gameNum = msg.split(' ')
            conn.request('GET', f'/game/info?numberGame={gameNum}', headers={'authorization': f'Bearer {token}'})
            r = conn.getresponse()
            if response.status != 200:
                print(f'Error {response.status}.')
                continue
            game_res = json.loads(r.read().decode('utf-8'))
            if game_res['gameStatus']['status'] == 'IS_ENDED':
                winNum = game_res["gameStatus"]["winNum"]
                print(f'Game is over. The number {winNum} dropped out.', end='')
                users_bet = game_res["bets"][login]["type"] if login in game_res["bets"].keys() else None
                if users_bet is None:
                    print('You have not bet in that game.')
                elif (winNum % 2 == 0 and users_bet['type'] == 'EVEN') or \
                        (winNum % 2 != 0 and users_bet['type'] == 'ODD') or \
                        (users_bet["type"] == 'NUM' and winNum == users_bet["num"]):
                    print("You have won.")
                else:
                    print("You have lost.")
            else:
                print("The game is still playing.")

        elif msg.startswith('!bet'):
            _, bet_sum, bet = msg.split(' ', maxsplit=2)
            if bet.startswith('NUM'):
                bet_type, bet_num = bet.split()
                bet_num = int(bet_num)
            else:
                bet_num = 0
                bet_type = bet
            headers = {'authorization': f'Bearer {token}'}
            conn.request('POST', f'/game/createBet?sum={float(bet_sum)}&betTypeEnum={bet_type}&num={bet_num}',
                         headers=headers)
            r = conn.getresponse()
            if response.status != 200:
                print(f'Error {response.status}.')
                continue
            gameInfo = json.loads(r.read().decode('utf-8'))
            print(f'Your bet on the {gameInfo["numberGame"]} game is accepted')

        elif msg == '!start' and isCroupier:
            headers = {'authorization': f'Bearer {token}'}
            conn.request('POST', '/game/start', headers=headers)
            r = conn.getresponse()
            if response.status != 200:
                print(f'Error {response.status}.')
                continue
            r.read()
        else:
            print('Unknown command.')


send_thread = Thread(target=send)
send_thread.start()

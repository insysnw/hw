import json

import requests

HOST = 'http://localhost:10000'
myId = None

def main():
    global myId
    help()
    while True:
        command = input()
        if command.strip(' ') == 'create account':
            createAccount()
        elif command.strip(' ') == 'get id':
            getId()
        elif command.strip(' ') == 'get value':
            getValue()
        elif command.strip(' ') == 'delete account':
            deleteAccount()
        elif command.strip(' ') == 'get client list':
            getClientList()
        elif command.strip(' ') == 'send money':
            sendMoney()
        elif command.strip(' ') == 'get help':
            help()
        elif command.strip(' ') == 'set money':
            setMoney()
        elif command.strip(' ') == 'get history':
            getHistory()
        else:
            print('Для вызова помощи введите "get instruction"')


def help():
    print('Чтобы создать аккаунт введите "create account"')
    print('Чтобы удалить аккаунт введите "delete account"')
    print('Чтобы узнать баланс введите "get value"')
    print('Чтобы перевести деньги другому пользователю введите "send money"')
    print('Чтобы узнать номер своего аккаунта введите "get id"')
    print('Чтобы узнать список пользователей платежной системы введите "get client list"')
    print('Чтобы пополнить баланс пользователю введите "set money"(! Требуется пароль администратора !)')
    print('Чтобы получить историю операций введите "get history"(! Требуется пароль администратора !)')
    print('Для повторного вызова помощи введите "get instruction"')

def createAccount():
    global myId
    if myId:
         answer = input('У вас уже есть аккаунт, удалить его?(да/нет)')
         if answer.strip(' ') == 'да':
             deleteAccount()
         else:
             print('аккаунт не будет удалён')
    name = input('Введите ваше имя: ')
    password = input('Введите пароль для аккаунта:')
    data = {
        'name': name,
        'password': password
    }
    response = requests.post(HOST + '/createAccount', data=json.dumps(data))
    if response.status_code == 200:
        myId = response.text
        print('Аккаунт успешно создан, ваш ID: ' + myId)
    else:
        print(response.text)

def deleteAccount():
    global myId
    if myId:
        password = input('Введите пароль от аккаунта:')
        data = {
            'password': password,
            'id': myId
        }
        response = requests.delete(HOST + '/deleteAccount', data=json.dumps(data))
        if response.status_code == 200:
            myId = None
            print('Аккаунт успешно удалён')
        elif response.status_code == 400:
            print(response.text)
            myId = None
        else:
            print(response.text)

def getValue():
    password = input('Введите пароль от аккаунта:')
    data = {
        'password': password,
        'id': myId
    }
    response = requests.get(HOST + '/getValue', data=json.dumps(data))
    if response.status_code == 200:
        print('Баланс вашего аккаунта: ' + response.text)
    else:
        print(response.text)

def getClientList():
    response = requests.get(HOST + '/getClientList')
    if response.status_code == 200:
        clientList = json.loads(response.text)
        for el in clientList.keys():
            client = clientList[el]
            print(client + ' - ' +el)
    else:
        print('Невозможно получить список клиентов')


def getId():
    if myId:
        print(myId)
    else:
        print('Вы ещё не создали аккаунт')

def sendMoney():
    if myId:
        password = input('Введите пароль:')
        receiver = input('Введите идентификатор получателя:')
        sum = input('Введите сумму перевода:')
        data = {
            'password': password,
            'sender': myId,
            'receiver': receiver,
            'sum': sum
        }
        response = requests.post(HOST + '/sendMoney', data=json.dumps(data))
        if response.status_code == 200:
            print('Операция успешно проведена')
        else:
            print(response.text)
    else:
        print('Создайте аккаунт')

def setMoney():
    password = input('Введите пароль администратора:')
    id = input('Введите уникальный идентификатор аккаунта:')
    sum = input('Введите сумму, на которую необходимо изменить баланс аккаунта:')
    data = {
        'password': password,
        'id': id,
        'sum': sum
    }
    response = requests.post(HOST + '/addMoney', data=json.dumps(data))
    if response.status_code == 200:
        print('Операция успешно проведена')
    else:
        print(response.text)

def getHistory():
    password = input('Введите пароль администратора:')
    response = requests.get(HOST + '/getHistory', data=json.dumps(password))
    if response.status_code == 200:
        history = json.loads(response.text)
        for el in history:
            print(el)
    else:
        print(response.text)

if __name__ == '__main__':
    main()
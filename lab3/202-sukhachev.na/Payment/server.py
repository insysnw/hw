import json
import uuid

from bottle import route, run, request, response

HOST = 'http://localhost:10000'

accounts = {
    '1': {
        'id': '1',
        'name': 'Test',
        'value': 0,
        'password': 'Test'
    }
}

accountsPublic = {
    '1': {
        'id': '1',
        'name': 'Test',
    }
}

history = []

initialValue = 1000
adminPassword = '12345'


@route('/getValue', method='GET')
def getValue():
    accountInfo = json.loads(request.body.getvalue().decode('utf-8'))
    id = accountInfo['id']
    password = accountInfo['password']
    if id in accounts.keys():
        if password == accounts[id]['password']:
            return str(accounts[id]['value'])
        else:
            response.status = 401
            return 'Неверный пароль'
    else:
        response.status = 400
        return 'Аккаунт с таким ID не существует или был удалён'


@route('/createAccount', method='POST')
def createAccount():
    accountInfo = json.loads(request.body.getvalue().decode('utf-8'))
    name = accountInfo['name']
    password = accountInfo['password']
    id = str(uuid.uuid4())
    if len(password) == 0:
        response.status = 400
        return 'Для создания аккаунта необходимо придумать пароль!'
    if len(name) == 0:
        response.status = 400
        return 'Для создания аккаунта необходимо ввести имя!'
    accounts[id] = {
        'name': name.strip(' '),
        'id': id,
        'password': password,
        'value': initialValue
    }
    accountsPublic[id] = {
        'name': name.strip(' '),
        'id': id,
    }
    history.append('Аккаунт ' + id + ' создан')
    return id


@route('/sendMoney', method='POST')
def sendMoney():
    info = json.loads(request.body.getvalue().decode('utf-8'))
    password = info['password']
    sender = info['sender']
    receiver = info['receiver']
    sum = int(info['sum'])
    if sum <= 0:
        response.status = 400
        return 'Невозможно перевести нулевую или отрицательную сумму!'
    elif sender in accounts.keys() and receiver in accounts.keys():
        if password == accounts[sender]['password']:
            if accounts[sender]['value'] >= sum:
                accounts[sender]['value'] = accounts[sender]['value'] - sum
                accounts[receiver]['value'] = accounts[receiver]['value'] + sum
                history.append('Перевод от ' + sender + ' для ' + receiver + ' на ' + str(sum))
            else:
                response.status = 400
                return 'На вашем счете недостаточно средств'
        else:
            response.status = 401
            return 'Неверный пароль'

    else:
        response.status = 400
        return 'Невозможно перевести деньги, проверьте реквизиты'


@route('/deleteAccount', method='DELETE')
def deleteAccount():
    accountInfo = json.loads(request.body.getvalue().decode('utf-8'))
    id = accountInfo['id']
    password = accountInfo['password']
    if id in accounts.keys():
        if password == accounts[id]['password']:
            del accounts[id]
            del accountsPublic[id]
            history.append('Аккаунт ' + id + ' удалён владельцем')
        else:
            response.status = 401
            return 'Неверный пароль'
    else:
        response.status = 400
        return 'Аккаунт с таким ID не существует или был удалён'


@route('/getClientList', method='GET')
def getClientList():
    return json.dumps(accountsPublic)


@route('/addMoney', method='POST')
def setMoney():
    accountInfo = json.loads(request.body.getvalue().decode('utf-8'))
    id = accountInfo['id']
    sum = int(accountInfo['sum'])
    password = accountInfo['password']
    if id in accounts.keys():
        if password == adminPassword:
            accounts[id]['value'] = accounts[id]['value'] + sum
            history.append('Аккаунту ' + id + ' изменён баланс администратором на ' + str(sum))
        else:
            response.status = 401
            return 'Неверный пароль'
    else:
        response.status = 400
        return 'Аккаунт с таким ID не существует или был удалён'


@route('/getHistory', method='GET')
def getHistory():
    password = json.loads(request.body.getvalue().decode('utf-8'))
    if password == adminPassword:
        return json.dumps(history)
    else:
        response.status = 401
        return 'Неверный пароль'


run(host='localhost', port=10000)

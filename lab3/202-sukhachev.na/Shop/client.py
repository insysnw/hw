import json

import requests

from Product import Product

HOST = 'http://localhost:10000'
busket = {}
productList = {}


def main():
    global productList
    global busket
    getHelp()
    getProductList()
    while True:
        command = input()
        if command.strip(' ') == 'get product list':
            getProductList(needPrint=True)
        elif command.strip(' ') == 'add product':
            addProduct()
        elif command.strip(' ') == 'delete product':
            deleteProduct()
        elif command.strip(' ') == 'add to busket':
            addToBusket()
        elif command.strip(' ') == 'delete from busket':
            deleteFromBusket()
        elif command.strip(' ') == 'watch busket':
            watchBusket()
        elif command.strip(' ') == 'buy':
            buy()
        elif command.strip(' ') == 'get instruction':
            help()
        else:
            print('Для вызова помощи введети "help"')


def getHelp():
    print('Для получения списка товаров введите "get product list"')
    print('Чтобы посмотреть корзину введите "watch busket"')
    print('Для добавления в корзину введите "add to busket"')
    print('Для удаления из корзины введите "delete from busket')
    print('Для покупки введите "buy"')
    print('Чтобы добавить продукт в список товаров введите "add product"(! требуется пароль администратора !)')
    print(
        'Чтобы добавить удалить продукт из списка товаров введите "delete product"(! требуется пароль администратора !)')
    print('Чтобы вызвать помощь ещё раз, введите "get instruction"')


def getProductList(needPrint=False):
    global productList
    response = requests.get(HOST + '/getProductList')
    list = json.loads(response.text)
    if response.status_code == 200:
        productList = {}
        for key in list.keys():
            el = list[key]
            product = el['product']
            productList[product['id']] = Product(id=product['id'], productName=product['productName'],
                                                 price=product['price'])
            if needPrint:
                print('id: ' + product['id'] + ", название: " + product['productName'] + ', цена: ' +
                      str(product['price']) + ', в наличии: ' + str(el['count']) + ' шт')
    else:
        print("Не могу получить список товаров, код ошибки: " + response.status_code)


def addProduct():
    data = {}
    password = input('Введите пароль администратора:')
    data['password'] = password
    productName = input('Введите название продукта:')
    data['productName'] = productName
    price = input('Введите цену продукта:')
    data['price'] = price
    count = input('Введите количество товара:')
    data['count'] = count
    response = requests.post(HOST + '/addProduct', data=json.dumps(data))
    if response.status_code == 200:
        print("Продукт успешно добавлен")
    else:
        print(response.text)


def deleteProduct():
    data = {}
    password = input('Введите пароль администратора:')
    data['password'] = password
    id = input('Введите идентификатор продукта:')
    data['id'] = id
    response = requests.delete(HOST + '/deleteProduct', data=json.dumps(data))
    if response.status_code == 200:
        print("Продукт успешно удалён")
    else:
        print(response.text)


def addToBusket():
    global productList
    global busket
    id = input('Введите идентификатор продукта:')
    count = input('Введите количество продукта:')
    if int(count) > 0:
        if id in productList.keys():
            busket[id] = {
                'count': int(count),
                'product': productList[id].toObj()
            }
            print('Продукт добавлен в корзину')
        else:
            print('Товара с таким идентификатором не существует')

    else:
        print('Невозможно добавить отрицательное количество товара')


def deleteFromBusket():
    id = input('Введите идентификатор продукта:')
    if id in busket.keys():
        del busket[id]
        print('Товар удалён из корзины')
    else:
        print('Продукта с таким идентификаторм нет в корзине')


def watchBusket():
    if not len(busket):
        print('В корзине ничего нет!')
    for key in busket.keys():
        count = busket[key]['count']
        product = productList[key]
        print('id: ' + product.id + ", название: " + product.productName + ', цена: ' +
              str(product.price) + ', в корзине: ' + str(count) + ' шт' + ', итоговая цена: ' + str(
            count * product.price))


def buy():
    global busket
    if (len(busket)) != 0:
        response = requests.post(HOST + '/checkout', data=json.dumps(busket))
        if response.status_code == 200:
            busket = {}
            print('Покупка успешно оформлена, к оплате: ' + response.text)
        else:
            print(response.text)
    else:
        print('Прежде чем оформить покупку, добавьте что-нибудь в корзину')


if __name__ == '__main__':
    main()

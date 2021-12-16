import json

from bottle import route, run, request, response

from Product import Product

adminPassword = '12345'

productList = {
    '1': {
        "product": Product(id=1, productName='bánh mì', price=1000).toObj(),
        "count": 100
    },
    '2': {
        "product": Product(id=2, productName='nước ép', price=8000).toObj(),
        "count": 50
    },
    '3': {
        "product": Product(id=3, productName='mì tôm', price=6000).toObj(),
        "count": 10
    },
    '4': {
        "product": Product(id=4, productName='cà chua', price=100).toObj(),
        "count": 40
    },
}


@route('/addProduct', method='POST')
def addProduct():
    posted = json.loads(request.body.getvalue().decode('utf-8'))
    try:
        password = request.auth[1]
        productName = posted['productName']
        try:
            price = int(posted['price'])
            count = int(posted['count'])
        except:
            response.status = 400
            return 'Невозможно добавить продукт с указанными аргументами'
        if password == adminPassword:
            if len(productName) == 0 or price < 0 or count < 0:
                response.status = 400
                return 'Невозможно добавить продукт с указанными аргументами'
            else:
                product = Product(productName=productName, price=price)
                newProduct = {
                    'product': product.toObj(),
                    'count': count
                }
                productList[product.id] = newProduct
        else:
            response.status = 401
            return 'Неверный пароль'
    except:
        response.status = 400
        return 'Некорректные данные'


@route('/deleteProduct', method='DELETE')
def deleteProduct():
    deleted = json.loads(request.body.getvalue().decode('utf-8'))
    password = request.auth[1]
    id = deleted['id']
    if password == adminPassword:
        if id in productList.keys():
            del productList[id]
        else:
            response.status = 400
            return 'Продукт с указанным id не существует'
    else:
        response.status = 401
        return 'Неверный пароль'


@route('/getProductList', method='GET')
def getProductList():
    response.status = 200
    return json.dumps(productList)


@route('/checkout', method='POST')
def buy():
    global productList
    result = json.loads(request.body.getvalue().decode('utf-8'))
    sum = 0
    productListCopy = productList
    for key in result:
        item = result[key]
        count = item['count']
        product = item['product']
        if key in productList.keys():
            if int(item['count']) <= productList[key]['count']:
                sum += count * int(product['price'])
                productListCopy[key]['count'] = productListCopy[key]['count']-count
            else:
                response.status = 400
                answer = 'Товара ' + product['productName'] + ' в наличии всего ' + str(
                    productList[key]['count']) + ', а в вашем заказе ' + str(count)
                return answer
        else:
            response.status = 400
            answer = 'Товар с id = ' + key + ' не существует'
            return answer
    productList = productListCopy
    return str(sum)


run(host='localhost', port=10000)

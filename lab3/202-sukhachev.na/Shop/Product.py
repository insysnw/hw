import json
import uuid


class Product:
    def __init__(self, productName, price,id=-1):
        self.id = str(uuid.uuid4()) if id == -1 else str(id)
        self.productName = productName
        self.price = price

    def toObj(self):
        obj = {
            'id': self.id,
            'productName': self.productName,
            'price': self.price
        }
        return obj

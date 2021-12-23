# Лабораторная работа 2. DNS клиент.
Написана на python.
## Запуск
Для запуска необходимо написать в командную строку `python client.py -ip <host> -p <port> -t <type> -d <domain>`.
+ `-h <host>` - имя хоста DNS сервера (по умолчанию `8.8.8.8`)
+ `-p <port>` - порт DNS сервера (по умолчанию 53)
+ `-t <type>` - тип DNS записи, может быть `A` (по умолчанию), `MX`, `TXT`, `AAAA`.
+ `-d <domain>` - доменное имя

## Тестирование
На запрос `python client.py -t A -d yandex.ru` получим:
```
{
    "id": 6575,
    "qr": true,
    "opcode": 0,
    "authority_answer": false,
    "truncation": false,
    "recurtion_desired": true,
    "recursion_available": true,
    "rcode": 0,
    "qdcount": 1,
    "ancount": 4,
    "nscount": 0,
    "arcount": 0,
    "questions": [
        {
            "type_id": 1,
            "type": "A",
            "name": "yandex.ru."
        }
    ],
    "answers": [
        {
            "name": "yandex.ru.",
            "ttl": 70,
            "type_id": 1,
            "ipv4_address": "77.88.55.88"
        },
        {
            "name": "yandex.ru.",
            "ttl": 70,
            "type_id": 1,
            "ipv4_address": "5.255.255.88"
        },
        {
            "name": "yandex.ru.",
            "ttl": 70,
            "type_id": 1,
            "ipv4_address": "5.255.255.80"
        },
        {
            "name": "yandex.ru.",
            "ttl": 70,
            "type_id": 1,
            "ipv4_address": "77.88.55.77"
        }
    ],
    "name_servers": [],
    "additional_answers": []
}
```

А на запрос `python client.py -t MX -d yandex.ru` получим:
```
{
    "id": 46384,
    "qr": true,
    "opcode": 0,
    "authority_answer": false,
    "truncation": false,
    "recurtion_desired": true,
    "recursion_available": true,
    "rcode": 0,
    "qdcount": 1,
    "ancount": 1,
    "nscount": 0,
    "arcount": 0,
    "questions": [
        {
            "type_id": 15,
            "type": "MX",
            "name": "yandex.ru."
        }
    ],
    "answers": [
        {
            "name": "yandex.ru.",
            "ttl": 91,
            "type_id": 15,
            "preference": 10,
            "exchange": "mx.yandex.ru."
        }
    ],
    "name_servers": [],
    "additional_answers": []
}
```
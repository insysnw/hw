# Лабораторная работа 2. TFTP сервер.

Написана на python.

##Запуск
Для запуска сервера необходимо написать в командную строку `python server.py -ip <host> -p <port> - t <timeout>`
Флаги являются опциональными. 

##Описание протокола

###Формат пакетов
Протокол поддерживает 5 типов пакетов:
+ запрос на чтение файла (RRQ - read request)

Тип пакета | Имя файла | Режим передачи | 
---------- | --------- | -------------- | 
0x01 | n bytes | n bytes |

+ запрос на запись файла (WRQ - write request)

Тип пакета | Имя файла | Режим передачи | 
---------- | --------- | -------------- | 
0x02 | n bytes | n bytes |

+ передаваемые данные (DATA)

Тип пакета | Номер блока | Данные | 
---------- | --------- | ------ | 
0x03 | 2 bytes | 512 bytes |

+ подтверждение пакета (ACK)

Тип пакета | Номер блока |
---------- | -------- |
0x04 | 2 bytes |

+ сообщение об ошибке (ERROR)

Тип пакета | Код ошибки | Сообщение | 
---------- | --------- | ------ | 
0x05 | 2 bytes | n bytes |

**Ошибки:**

Код ошибки | Объяснение |
--- | --- |
1 | Файл не найден |
2 | Нарушение прав доступа |
3 | Нехватке места на диске |
4 | Недопустимая TFTP операция |
5 | Неправильный Transfer ID |
6 | Файл уже существует |
7 | Пользователя не существует |

## Тестирование 

Запрос клиента на запись файла на сервер:
```
D:\UniProgsAndFiles\TCS\hw\lab2\tftp_server\client_1>tftp -i 192.168.56.1 PUT test.txt
Успешная передача: 3282 байт за 1 сек., 3282 байт/с
```

Ответ сервера:
```
Server is ready! <host> = 192.168.56.1; <port> = 69; <timeout> = 0.001
Server is running
Received WRQ
Sent ACK 0
Received DATA 1
Sent ACK 1
Received DATA 2
Sent ACK 2
Received DATA 3
Sent ACK 3
Received DATA 4
Sent ACK 4
Received DATA 5
Sent ACK 5
Received DATA 6
Sent ACK 6
Received DATA 7
Sent ACK 7
```

Запрос клиента на запись файла на сервер:
```
D:\UniProgsAndFiles\TCS\hw\lab2\tftp_server\client_2>tftp -i 192.168.56.1 GET test.txt
Успешная передача: 3282 байт за 1 сек., 3282 байт/с
```

Ответ сервера:
```
Server is ready! <host> = 192.168.56.1; <port> = 69; <timeout> = 0.001
Server is running
Received RRQ
Sent DATA 1
Received ACK 1
Sent DATA 2
Received ACK 2
Sent DATA 3
Received ACK 3
Sent DATA 4
Received ACK 4
Sent DATA 5
Received ACK 5
Sent DATA 6
Received ACK 6
Sent DATA 7
Received ACK 7
```

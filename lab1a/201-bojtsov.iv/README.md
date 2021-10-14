# Синхронный и асинхронный TCP чат

Написан на Rust.
Работа проверялась на ArchLinux дистрибутиве.

## Протокол

Подразумевает последовательность действий:

1. Отправка дескриптора сообщения.
2. (Только для клиентов) Ожидание ответа от сервера (ACK или ошибка) в виде дескриптора.
3. Отправка заголовка сообщения.
4. (Только для клиентов) Ожидание ответа от сервера (ACK или ошибка) в виде дескриптора.
5. Отправка сообщения.
6. (Только для клиентов в случае отправки личного сообщения) Ожидание ответа от сервера (ACK или ошибка) в виде дескриптора.

Формат дескриптора:

```text
 0               1               2               3
 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                       Conten size (Hi)                        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                       Conten size (Lo)                        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|          Header Size          |         Magic Number          |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|  Message Type  |
+-+-+-+-+-+-+-+-+-
```

Заголовок - сериализованная в формате JSON структура:
Клиент:

```Rust
struct Header {
    pub to: Option<String>,       // username if message is private
    pub filename: Option<String>, // filename if sending a file
}
```

Сервер:

```Rust
struct Header {
    pub username: String,    // sender of the message
    pub time: DateTime<Utc>, // message timestamp
    pub p: bool,             // private message
    pub f: Option<String>,   // name of the file
}
```

Чтобы войти в комнату, клиент должен отправить дескриптор с Message type = Login и заголовок с to = "username", после чего дождаться ответа от сервера.

## Build

Для сборки использовался rustc 1.55.0.

## Запуск

Синхронный сервер:

```sh
cd sync_room
cargo run -- --address=<address>
# Default value for address is 127.0.0.1:6969
```

Асинхронный сервер:

```sh
cd async_room
cargo run -- --address=<address>
# Default value for address is 127.0.0.1:6969
```

Клиент:

```sh
cd client
cargo run -- --address=<address> --username=<username> --save_directory==<save_directory>
# Default value for address is 127.0.0.1:6969
# Default value for save_directory is .
```

## Post Scriptum

Синхронная и асинхронная версии сервера практически ничем не отличаются, кроме наличия `.await` и `async` тут и там (асинхронная - копипаста синхронной с небольшими изменениями), ну и использованием `tokio::net` и `tokio::io` вместо `std::net` и `std::io` соответственно.

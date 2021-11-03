# Неблокирующий TCP чат

Написан на Rust.
Работа проверялась на Windows и macOS.

Так как работа над чатом началась до появления данного репозитория, то ее можно найти [тут](https://github.com/lunakoly/RustNetCourse).

На данный момент, в этом репозитории находится пример с блокирующим сервером и неблокирующим клиентом. Если нужно посмотреть на полностью неблокирующий сервер, то сделать это можно в ветке [full-async](https://github.com/lunakoly/RustNetCourse/tree/full-async). 

Сравнить блокирующий и неблокирующий код можно либо по `lib.rs`-файлам, либо по [коммиту перевода клиента на неблок](https://github.com/lunakoly/RustNetCourse/commit/843e851d50a3ebe0668bd17cbe847f9ca4fd5381), либо по [коммиту перевода сервера на неблок](https://github.com/lunakoly/RustNetCourse/commit/15d136973bb6aa5ae6c6802fce3d09711a008b10).

## Протокол

Полное описание протокола находится в [README](https://github.com/lunakoly/RustNetCourse/blob/main/README.md).

## Build

Для сборки использовались `cargo` 1.55.0 (32da73ab1 2021-08-23), `rustc` 1.55.0 (c8dfcfe04 2021-09-06).

```sh
cargo build
```

## Запуск

Сервер:

```sh
cargo run -p server
```

Клиент:

```sh
cargo run -p client
```

Подробности запуска можно прочесть там же, в исходной репе.

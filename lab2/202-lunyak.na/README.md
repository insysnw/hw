# Реальные UDP протоколы

Выбраны были:
- TFTP-сервер
- DHCP-клиент

Оба написаны на Rust. Оба хранятся [вот тут](https://github.com/lunakoly/RustUDP).

Подробное описание доступно там же в [README](https://github.com/lunakoly/RustUDP/blob/main/README.md).

## Build

Для сборки использовались `cargo` 1.57.0 (b2e52d7ca 2021-10-21), `rustc` 1.57.0 (f1edd0429 2021-11-29).

```sh
cargo build
```

## Запуск

TFTP-сервер:

```sh
cargo run -p tftp-server
```

По умолчанию использует порт 6969, поменять это можно вот [тут](https://github.com/lunakoly/RustUDP/blob/a002e7a1f2a274511e7c8756c459fd0a1d79154a/tftp-server/src/tftp.rs#L13) руками перед запуском.

DHCP-клиент:

```sh
cargo run -p dhcp-client
```

По умолчанию [используются](https://github.com/lunakoly/RustUDP/blob/a002e7a1f2a274511e7c8756c459fd0a1d79154a/dhcp-client/src/dhcp.rs#L150) порты 67 и 68, поэтому из-под обычного пользователя нужно `sudo target/debug/dhcp-client`.

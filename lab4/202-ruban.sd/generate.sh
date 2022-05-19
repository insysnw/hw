#!/bin/zsh

openapi-generator generate -g kotlin -i amogus-terminal-specification.yaml -o amogus-terminal --package-name "net.fennmata.amogus.terminal.client"

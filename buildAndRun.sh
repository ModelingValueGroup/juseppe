#!/usr/bin/env bash

set -ue

[[ $# -eq 2   ]] || { echo "Usage: $0 <port> <dir>"              >&2 ; exit 1 ; }
PORT="$1"; shift
 DIR="$1"; shift
[[ -n "$PORT" ]] || { echo "Non-empty port required"             >&2 ; exit 1 ; }
[[ -n "$DIR"  ]] || { echo "Non-empty dir with plugins required" >&2 ; exit 1 ; }
[[ -d "$DIR"  ]] || { echo "That is not a dir ($DIR)"            >&2 ; exit 1 ; }

echo "running maven to build juseppe..."
mvn package > mvn.log
rm juseppe-cli/release-history.json
rm juseppe-cli/update-center.json

if [[ ! -f uc.key ]]; then
    echo "generating cert..."
    openssl genrsa -out uc.key 2048
    openssl req -nodes -x509 -new \
        -key uc.key \
        -out uc.crt \
        -days 1056 \
        -subj "/C=EN/ST=Update-Center/L=Juseppe/O=Juseppe"
fi

echo
echo "This is the used certificate:"
java \
    -Djuseppe.certificate=uc.crt \
    -Djuseppe.private.key=uc.key \
    -jar juseppe-cli/target/juseppe.jar \
    cert

echo
echo "starting server now (hit cntl-C top stop)..."
java \
    -Djuseppe.certificate=uc.crt \
    -Djuseppe.private.key=uc.key \
    -Djuseppe.plugins.dir=$DIR \
    -Djuseppe.saveto.dir=$DIR \
    -jar juseppe-cli/target/juseppe.jar \
    serve \
    $PORT
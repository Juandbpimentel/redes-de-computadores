#!/bin/sh
echo -ne '\033c\033]0;Duelo de Esgrima\a'
base_path="$(dirname "$(realpath "$0")")"
"$base_path/applinux.x86_64" "$@"

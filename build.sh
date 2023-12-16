#!/usr/bin/env bash

set -euo pipefail

main() {
  docker build . \
    --target build-output \
    --output build

  docker build . \
    --output build/libs
}

main "$@"

#!/usr/bin/env bash

set -euo pipefail

main() {
  export BUILDKIT_PROGRESS=plain
  export PROGRESS_NO_TRUNC=1

  docker build . \
    --target build-output \
    --output build

  docker build .
}

main "$@"

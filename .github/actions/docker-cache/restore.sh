#!/usr/bin/env bash

set -euo pipefail
# shellcheck source=timing.sh
. "${BASH_SOURCE%/*}/timing.sh"

main() {
  local cache_tar=$1

  timing sudo service docker stop

  fast_delete /var/lib/docker

  if [[ -f "$cache_tar" ]]; then
    ls -lh "$cache_tar"
    sudo mkdir -p /var/lib/docker
    timing sudo tar -xf "$cache_tar" -C /var/lib/docker
  fi

  timing sudo service docker start
}

main "$@"

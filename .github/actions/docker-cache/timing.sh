#!/usr/bin/env bash

timing() {
  command time -f "[$*] took %E" "$@"
}

# mv is sometimes dramatically (20+seconds) faster than rm -rf
fast_delete() {
  local to_delete=$1

  if [ -d "$to_delete" ]; then
    local temp_dir; temp_dir="$(mktemp -d --dry-run)"
    timing sudo mv "$to_delete" "$temp_dir"
    in_background rm -rf "$temp_dir"
  fi
}

in_background() {
  nohup "$@" </dev/null >/dev/null 2>&1 & disown
}

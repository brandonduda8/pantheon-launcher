# VORTEX // EMBER for Termux (opt-in)

Nothing in the launcher touches Termux. These are manual, reversible steps. No root needed.

## 1. Back up your current colors
```sh
mkdir -p ~/.termux
[ -f ~/.termux/colors.properties ] && cp ~/.termux/colors.properties ~/.termux/colors.properties.pre-vortex
```

## 2. Install the Vortex palette
Copy `assets/vortex/termux-colors.properties` from this repo to your phone (e.g. `~/storage/downloads/`), then:
```sh
cp ~/storage/downloads/termux-colors.properties ~/.termux/colors.properties
termux-reload-settings
```

## 3. Optional prompt and aliases
Copy `assets/vortex/shell/vortex-prompt.sh` to `~/vortex-prompt.sh` and try it with `source ~/vortex-prompt.sh`.
Only if you like it, add `source ~/vortex-prompt.sh` to `~/.bashrc` yourself. The script defines a prompt and three read-only aliases (`vx-status`, `vx-ll`, `vx-disk`); it makes no network calls and starts no services.

## Restore
```sh
if [ -f ~/.termux/colors.properties.pre-vortex ]; then
  mv ~/.termux/colors.properties.pre-vortex ~/.termux/colors.properties
else
  rm -f ~/.termux/colors.properties   # Termux falls back to its default theme
fi
termux-reload-settings
```
Remove the `source ~/vortex-prompt.sh` line from `~/.bashrc` (if you added it) and open a new session.

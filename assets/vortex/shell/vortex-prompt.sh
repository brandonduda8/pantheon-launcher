# Genesis Vortex shell prompt + aliases (opt-in).
# Nothing here runs automatically. To try for one session:
#   source ~/vortex-prompt.sh
# To keep it, add that line to ~/.bashrc yourself. Remove the line to undo.
# No network calls, no root, no background services.

_vx_violet='\[\e[38;2;122;43;255m\]'
_vx_magenta='\[\e[38;2;209;75;255m\]'
_vx_muted='\[\e[38;2;181;169;201m\]'
_vx_reset='\[\e[0m\]'
PS1="${_vx_violet}▲${_vx_magenta}vortex${_vx_reset} ${_vx_muted}\w${_vx_reset} › "

alias vx-status='printf "VORTEX // EMBER  |  %s  |  %s\n" "$(date +%H:%M)" "$(pwd)"'
alias vx-ll='ls -lah --color=auto'
alias vx-disk='df -h "$HOME"'

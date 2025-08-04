#!/usr/bin/env bash

# logging.sh
# Pure logging library for Claude Code Hooks scripts
# Provides colored logging functions only - no validation or utilities
# Inspired by setup-claude-hooks.sh patterns

# Prevent multiple inclusions
if [[ -n "${CLAUDE_HOOKS_LOGGING_LOADED:-}" ]]; then
  return 0
fi
readonly CLAUDE_HOOKS_LOGGING_LOADED=1

# Color codes for logging (with guards to prevent readonly conflicts)
if [[ -z "${COLOR_RESET:-}" ]]; then
  readonly COLOR_RESET="\033[0m"      # Reset color
  readonly COLOR_BLUE="\033[34m"      # Blue for [INFO]
  readonly COLOR_GREEN="\033[32m"     # Green for [SUCCESS]
  readonly COLOR_RED="\033[31m"       # Red for [ERROR]
  readonly COLOR_YELLOW="\033[33m"    # Yellow for [WARNING]
fi

# Common constants (with guards)
if [[ -z "${SUCCESS:-}" ]]; then
  readonly SUCCESS=0
  readonly FAILURE=1
fi

# Core logging function with color and timestamp
log() {
  local prefix="$1"
  local message="$2"
  local color=""

  # Set color for specific log types
  case "$prefix" in
    "INFO") color="$COLOR_BLUE";;
    "SUCCESS") color="$COLOR_GREEN";;
    "ERROR") color="$COLOR_RED";;
    "WARNING") color="$COLOR_YELLOW";;
    "DEBUG") color="$COLOR_BLUE";;
  esac

  printf "${color}[%s] [%s]: %s${COLOR_RESET}\n" "$prefix" "$(date '+%Y-%m-%d %H:%M:%S')" "$message" >&2
}

# Convenience functions for different log levels
log_info() {
  log "INFO" "$1"
}

log_success() {
  log "SUCCESS" "$1"
}

log_error() {
  log "ERROR" "$1"
}

log_warning() {
  log "WARNING" "$1"
}

log_debug() {
  local message="$1"
  if [[ "${CLAUDE_HOOKS_DEBUG:-false}" == "true" ]]; then
    log "DEBUG" "$message"
  fi
}

log_step() {
  log "INFO" "$1"
}

log_detail() {
  log "INFO" "   → $1"
}

# Error logging with exit - this is logging-related
die() {
  local message="$1"
  local exit_code="${2:-$FAILURE}"
  log_error "$message"
  exit "$exit_code"
}
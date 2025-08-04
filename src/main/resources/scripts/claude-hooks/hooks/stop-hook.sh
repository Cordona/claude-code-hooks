#!/usr/bin/env bash

# stop-hook.sh
# Claude Code stop hook entry point
# Handles task completion notifications by forwarding to the stop service

set -euo pipefail

# Get script directory and source required libraries
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLAUDE_HOOKS_DIR="$(dirname "$SCRIPT_DIR")"
# shellcheck source=../lib/utils.sh
source "$CLAUDE_HOOKS_DIR/lib/utils.sh"

# Main function
main() {
  log_debug "Stop hook triggered"
  
  initialize_configuration
  validate_configuration
  
  local payload
  payload=$(read_payload_from_stdin)
  
  send_stop_notification "$payload"
  
  log_debug "Stop hook completed successfully"
}

# Initialize configuration
initialize_configuration() {
  source_config "$CLAUDE_HOOKS_DIR"
  log_debug "JSON configuration initialized"
}

# Validate required configuration
validate_configuration() {
  check_env_var "STOP_ENDPOINT" "Stop endpoint"
  log_debug "Configuration validated"
}

# Send stop notification to the service
send_stop_notification() {
  local payload="$1"
  
  log_debug "Sending stop notification to endpoint: $STOP_ENDPOINT"
  
  if ! "$CLAUDE_HOOKS_DIR/lib/call-endpoint.sh" "$STOP_ENDPOINT" "$payload"; then
    if [[ "${CLAUDE_HOOKS_SILENT_ERRORS:-true}" != "true" ]]; then
      log_error "Failed to call stop endpoint"
    fi
    exit 1
  fi
  
  log_debug "Stop notification sent successfully"
}

# Run main function
main "$@"
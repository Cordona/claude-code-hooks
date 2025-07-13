#!/usr/bin/env bash

# notification-hook.sh
# Claude Code notification hook entry point
# Handles permission request notifications by extracting project context
# and forwarding to the notification service

set -euo pipefail

# Get script directory and source required libraries
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLAUDE_HOOKS_DIR="$(dirname "$SCRIPT_DIR")"
# shellcheck source=../lib/utils.sh
source "$CLAUDE_HOOKS_DIR/lib/utils.sh"

# Main function
main() {
  log_debug "Notification hook triggered"
  
  initialize_configuration
  validate_configuration
  
  local payload cwd
  payload=$(read_payload_from_stdin)
  cwd=$(extract_working_directory "$payload")
  
  send_notification "$payload" "$cwd"
  
  log_debug "Notification hook completed successfully"
}

# Initialize configuration
initialize_configuration() {
  source_config "$CLAUDE_HOOKS_DIR"
  log_debug "JSON configuration initialized"
}

# Validate required configuration
validate_configuration() {
  check_env_var "NOTIFICATION_ENDPOINT" "Notification endpoint"
  log_debug "Configuration validated"
}

# Extract working directory using lib function
extract_working_directory() {
  local payload="$1"
  local cwd
  
  log_debug "Extracting working directory from payload"
  
  if ! cwd=$("$CLAUDE_HOOKS_DIR/lib/extract-cwd.sh" <<< "$payload"); then
    if [[ "${CLAUDE_HOOKS_SILENT_ERRORS:-true}" != "true" ]]; then
      log_error "Failed to extract working directory"
    fi
    # Continue with empty cwd if extraction fails
    cwd=""
  fi
  
  log_debug "Working directory extracted: '$cwd'"
  echo "$cwd"
}

# Send notification to the service
send_notification() {
  local payload="$1"
  local cwd="$2"
  
  log_debug "Sending notification to endpoint: $NOTIFICATION_ENDPOINT"
  
  if ! "$CLAUDE_HOOKS_DIR/lib/call-endpoint.sh" "$NOTIFICATION_ENDPOINT" "$cwd" "$payload"; then
    if [[ "${CLAUDE_HOOKS_SILENT_ERRORS:-true}" != "true" ]]; then
      log_error "Failed to call notification endpoint"
    fi
    exit 1
  fi
  
  log_debug "Notification sent successfully"
}

# Run main function
main "$@"
#!/usr/bin/env bash

# update-claude-hooks-config.sh
# Updates configuration values in existing Claude Code Hooks deployment
# Usage: update-claude-hooks-config.sh [options]

set -euo pipefail

# Get script directory and source required libraries
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/logging.sh
source "$SCRIPT_DIR/lib/logging.sh"
# shellcheck source=lib/validation.sh
source "$SCRIPT_DIR/lib/validation.sh"
# shellcheck source=lib/config-manager.sh
source "$SCRIPT_DIR/lib/config-manager.sh"
# shellcheck source=lib/json-utils.sh
source "$SCRIPT_DIR/lib/json-utils.sh"

# Configuration variables
NEW_HOSTNAME=""
NEW_PORT=""
NEW_TIMEOUT=""
NEW_ENDPOINT=""
INTERACTIVE_MODE=""
DRY_RUN=""
CONFIG_FILE=""
BACKUP_FILE=""

# Current configuration variables
CURRENT_HOSTNAME=""
CURRENT_PORT=""
CURRENT_TIMEOUT=""
CURRENT_ENDPOINT=""

# Change tracking
HAS_CHANGES=false
CHANGES=()

# Main function
main() {
  log_info "Starting Claude Code Hooks configuration update..."
  log_debug "Command line arguments: $*"
  
  log_step "Parsing command line arguments..."
  parse_arguments "$@"
  
  log_step "Locating configuration file..."
  find_config_file
  
  log_step "Loading current configuration..."
  load_current_config
  
  log_step "Determining new configuration values..."
  determine_new_values
  
  log_step "Detecting configuration changes..."
  detect_changes
  
  if [[ "$HAS_CHANGES" == false ]]; then
    log_info "Configuration is already up to date"
    exit 0
  fi
  
  show_changes_summary
  
  if [[ "$DRY_RUN" == true ]]; then
    log_info "Dry run mode - no changes applied"
    exit 0
  fi
  
  confirm_changes
  apply_changes
  verify_changes
  
  log_success "Configuration updated successfully!"
}

# Parse command line arguments
parse_arguments() {
  while [[ $# -gt 0 ]]; do
    case $1 in
      --hostname)
        NEW_HOSTNAME="$2"
        shift 2
        ;;
      --port)
        NEW_PORT="$2"
        shift 2
        ;;
      --endpoint)
        NEW_ENDPOINT="$2"
        shift 2
        ;;
      --timeout)
        NEW_TIMEOUT="$2"
        shift 2
        ;;
      --interactive|-i)
        INTERACTIVE_MODE=true
        shift
        ;;
      --dry-run|-n)
        DRY_RUN=true
        shift
        ;;
      --help|-h)
        show_help
        exit 0
        ;;
      *)
        log_error "Unknown option: $1"
        show_help
        exit 1
        ;;
    esac
  done
  
  # If no arguments provided, use interactive mode
  if [[ -z "$NEW_HOSTNAME" && -z "$NEW_PORT" && -z "$NEW_ENDPOINT" && -z "$NEW_TIMEOUT" && "$INTERACTIVE_MODE" != true ]]; then
    INTERACTIVE_MODE=true
  fi
}

# Show help message
show_help() {
  cat << EOF
Usage: $0 [OPTIONS]

Update Claude Code Hooks configuration values

OPTIONS:
    --hostname HOST       Set new hostname (e.g., localhost, myserver.local)
    --port PORT          Set new port number (e.g., 8085, 9090)
    --endpoint URL       Set full endpoint URL (overrides hostname/port)
    --timeout SECONDS    Set timeout in seconds (e.g., 10, 30)
    --interactive, -i    Interactive mode with prompts
    --dry-run, -n        Show changes without applying them
    --help, -h           Show this help message

EXAMPLES:
    $0 --hostname myserver.local --port 9090
    $0 --endpoint http://192.168.1.100:8080
    $0 --timeout 30
    $0 --interactive
    $0 --dry-run --hostname newhost

EOF
}

# Find existing configuration file
find_config_file() {
  log_detail "Searching for existing hooks-config.json..."
  local search_paths=(
    "$HOME/.claude/scripts/claude-hooks/hooks-config.json"
    "$PWD/.claude/scripts/claude-hooks/hooks-config.json"
    "$SCRIPT_DIR/hooks-config.json"
  )
  
  for path in "${search_paths[@]}"; do
    log_detail "Checking: $path"
    if [[ -f "$path" ]]; then
      CONFIG_FILE="$path"
      log_success "Found configuration file: $CONFIG_FILE"
      return
    else
      log_detail "Not found: $path"
    fi
  done
  
  log_error "No Claude Code Hooks configuration found"
  log_error "Please run deploy-claude-hooks.sh first to set up the hooks"
  exit 1
}

# Load current configuration values
load_current_config() {
  log_detail "Validating configuration file exists: $CONFIG_FILE"
  check_file_exists "$CONFIG_FILE" "Configuration file"
  
  log_detail "Checking for jq dependency..."
  check_dependency "jq" "apt-get install jq or brew install jq"
  
  log_detail "Parsing JSON configuration from: $CONFIG_FILE"
  
  # Extract current values using jq
  log_detail "Extracting baseUrl from configuration..."
  local base_url
  base_url=$(jq -r '.service.baseUrl // empty' "$CONFIG_FILE")
  log_detail "Raw baseUrl: '$base_url'"
  
  if [[ -n "$base_url" ]]; then
    log_detail "Parsing hostname and port from baseUrl..."
    # Parse hostname and port from baseUrl
    CURRENT_HOSTNAME=$(echo "$base_url" | sed -E 's|^https?://([^:]+).*|\1|')
    CURRENT_PORT=$(echo "$base_url" | sed -E 's|^https?://[^:]+:([0-9]+).*|\1|')
    CURRENT_ENDPOINT="$base_url"
    log_detail "Parsed hostname: '$CURRENT_HOSTNAME'"
    log_detail "Parsed port: '$CURRENT_PORT'"
  else
    log_warning "No baseUrl found in configuration"
  fi
  
  log_detail "Extracting timeout from configuration..."
  CURRENT_TIMEOUT=$(jq -r '.timeout.seconds // empty' "$CONFIG_FILE")
  log_detail "Current timeout: '$CURRENT_TIMEOUT' seconds"
  
  log_debug "Current hostname: '$CURRENT_HOSTNAME'"
  log_debug "Current port: '$CURRENT_PORT'" 
  log_debug "Current timeout: '$CURRENT_TIMEOUT'"
}

# Determine new values from arguments or interactive prompts
determine_new_values() {
  if [[ "$INTERACTIVE_MODE" == true ]]; then
    prompt_for_values
  else
    # Use command line arguments
    if [[ -n "$NEW_ENDPOINT" ]]; then
      # Parse hostname and port from full endpoint
      NEW_HOSTNAME=$(echo "$NEW_ENDPOINT" | sed -E 's|^https?://([^:]+).*|\1|')
      NEW_PORT=$(echo "$NEW_ENDPOINT" | sed -E 's|^https?://[^:]+:([0-9]+).*|\1|')
    fi
  fi
  
  # Use current values as defaults for unspecified options
  NEW_HOSTNAME="${NEW_HOSTNAME:-$CURRENT_HOSTNAME}"
  NEW_PORT="${NEW_PORT:-$CURRENT_PORT}"
  NEW_TIMEOUT="${NEW_TIMEOUT:-$CURRENT_TIMEOUT}"
  
  # Validate new values
  validate_new_values
}

# Interactive prompts for new values
prompt_for_values() {
  echo
  log_info "Interactive configuration update mode"
  log_detail "Current hostname: $CURRENT_HOSTNAME"
  log_detail "Current port: $CURRENT_PORT"
  log_detail "Current timeout: $CURRENT_TIMEOUT seconds"
  echo
  
  # Prompt for hostname
  read -rp "Enter hostname [$CURRENT_HOSTNAME]: " input_hostname
  NEW_HOSTNAME="${input_hostname:-$CURRENT_HOSTNAME}"
  log_detail "New hostname: '$NEW_HOSTNAME'"
  
  # Prompt for port
  read -rp "Enter port [$CURRENT_PORT]: " input_port
  NEW_PORT="${input_port:-$CURRENT_PORT}"
  log_detail "New port: '$NEW_PORT'"
  
  # Prompt for timeout
  read -rp "Enter timeout in seconds [$CURRENT_TIMEOUT]: " input_timeout
  NEW_TIMEOUT="${input_timeout:-$CURRENT_TIMEOUT}"
  log_detail "New timeout: '$NEW_TIMEOUT' seconds"
}

# Validate new configuration values
validate_new_values() {
  log_detail "Validating new configuration values..."
  
  if [[ -n "$NEW_PORT" ]]; then
    log_detail "Validating port: '$NEW_PORT'"
    check_numeric "$NEW_PORT" "Port"
    log_detail "Port validation: passed"
  fi
  
  if [[ -n "$NEW_TIMEOUT" ]]; then
    log_detail "Validating timeout: '$NEW_TIMEOUT'"
    check_numeric "$NEW_TIMEOUT" "Timeout"
    log_detail "Timeout validation: passed"
  fi
  
  if [[ -n "$NEW_HOSTNAME" && -n "$NEW_PORT" ]]; then
    NEW_ENDPOINT="http://${NEW_HOSTNAME}:${NEW_PORT}"
    log_detail "Constructed endpoint: '$NEW_ENDPOINT'"
  fi
}

# Detect what has changed
detect_changes() {
  CHANGES=()
  HAS_CHANGES=false
  
  if [[ "$NEW_HOSTNAME" != "$CURRENT_HOSTNAME" ]]; then
    CHANGES+=("hostname:$CURRENT_HOSTNAME:$NEW_HOSTNAME")
    HAS_CHANGES=true
  fi
  
  if [[ "$NEW_PORT" != "$CURRENT_PORT" ]]; then
    CHANGES+=("port:$CURRENT_PORT:$NEW_PORT")
    HAS_CHANGES=true
  fi
  
  if [[ "$NEW_TIMEOUT" != "$CURRENT_TIMEOUT" ]]; then
    CHANGES+=("timeout:$CURRENT_TIMEOUT:$NEW_TIMEOUT")
    HAS_CHANGES=true
  fi
}

# Show changes summary with visual formatting
show_changes_summary() {
  echo
  echo "Configuration Changes Summary:"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  
  local hostname_changed=false
  local port_changed=false
  local timeout_changed=false
  
  for change in "${CHANGES[@]}"; do
    IFS=':' read -r field old_val new_val <<< "$change"
    case $field in
      hostname)
        printf "  %-12s %-18s →  %s\n" "Hostname:" "$old_val" "$new_val"
        hostname_changed=true
        ;;
      port)
        printf "  %-12s %-18s →  %s\n" "Port:" "$old_val" "$new_val"
        port_changed=true
        ;;
      timeout)
        printf "  %-12s %-18s →  %s seconds\n" "Timeout:" "$old_val seconds" "$new_val"
        timeout_changed=true
        ;;
    esac
  done
  
  # Only show endpoint summary if both hostname AND port changed (avoid redundancy)
  if [[ "$hostname_changed" == true && "$port_changed" == true ]]; then
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    local old_endpoint="http://${CURRENT_HOSTNAME}:${CURRENT_PORT}"
    local new_endpoint="http://${NEW_HOSTNAME}:${NEW_PORT}"
    printf "  %-12s %s\n" "Endpoint:" "$old_endpoint → $new_endpoint"
  fi
  
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo
}

# Confirm changes with user
confirm_changes() {
  echo "These changes will update the hooks configuration."
  echo "Hook scripts will automatically use the new endpoints."
  echo
  
  read -rp "Continue with configuration update? [y/N]: " confirm
  
  case "$confirm" in
    y|Y|yes|YES)
      log_info "Proceeding with configuration update..."
      ;;
    *)
      log_info "Configuration update cancelled by user"
      exit 0
      ;;
  esac
}

# Apply changes to configuration file
apply_changes() {
  log_step "Creating backup of current configuration..."
  # Create backup
  BACKUP_FILE="${CONFIG_FILE}.backup.$(date +%Y%m%d_%H%M%S)"
  log_detail "Backup file: $BACKUP_FILE"
  cp "$CONFIG_FILE" "$BACKUP_FILE"
  log_success "Backup created: $BACKUP_FILE"
  
  log_step "Updating configuration file..."
  # Update configuration using jq
  local temp_file="${CONFIG_FILE}.tmp"
  log_detail "Using temporary file: $temp_file"
  log_detail "Updating baseUrl: $NEW_ENDPOINT"
  log_detail "Updating timeout: $NEW_TIMEOUT seconds"
  
  jq --arg baseUrl "$NEW_ENDPOINT" \
     --arg timeout "$NEW_TIMEOUT" \
     '.service.baseUrl = $baseUrl | .timeout.seconds = ($timeout | tonumber)' \
     "$CONFIG_FILE" > "$temp_file"
  
  log_detail "Moving temporary file to final location..."
  mv "$temp_file" "$CONFIG_FILE"
  
  log_success "Configuration file updated: $CONFIG_FILE"
}

# Verify changes were applied correctly
verify_changes() {
  log_step "Verifying configuration changes..."
  
  # Validate JSON syntax
  log_detail "Validating JSON syntax..."
  if ! jq empty "$CONFIG_FILE" &> /dev/null; then
    log_error "Generated configuration contains invalid JSON"
    log_info "Restoring from backup: $BACKUP_FILE"
    cp "$BACKUP_FILE" "$CONFIG_FILE"
    exit 1
  fi
  log_detail "JSON validation: passed"
  
  # Verify baseUrl was updated correctly
  log_detail "Verifying updated baseUrl..."
  local actual_base_url
  actual_base_url=$(jq -r '.service.baseUrl' "$CONFIG_FILE")
  log_detail "Expected baseUrl: '$NEW_ENDPOINT'"
  log_detail "Actual baseUrl: '$actual_base_url'"
  
  if [[ "$actual_base_url" != "$NEW_ENDPOINT" ]]; then
    log_error "baseUrl verification failed: expected '$NEW_ENDPOINT', got '$actual_base_url'"
    log_info "Restoring from backup: $BACKUP_FILE"
    cp "$BACKUP_FILE" "$CONFIG_FILE"
    exit 1
  fi
  
  # Verify timeout was updated correctly
  log_detail "Verifying updated timeout..."
  local actual_timeout
  actual_timeout=$(jq -r '.timeout.seconds' "$CONFIG_FILE")
  log_detail "Expected timeout: '$NEW_TIMEOUT'"
  log_detail "Actual timeout: '$actual_timeout'"
  
  if [[ "$actual_timeout" != "$NEW_TIMEOUT" ]]; then
    log_error "timeout verification failed: expected '$NEW_TIMEOUT', got '$actual_timeout'"
    log_info "Restoring from backup: $BACKUP_FILE" 
    cp "$BACKUP_FILE" "$CONFIG_FILE"
    exit 1
  fi
  
  log_success "Configuration verification passed"
}

# Run main function
main "$@"
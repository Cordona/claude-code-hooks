#!/usr/bin/env bash

# utils.sh
# Utility library for Claude Code Hooks scripts
# Provides helper functions for JSON parsing, directory navigation, and configuration

# Get lib directory for imports (avoid conflicting with main SCRIPT_DIR)
LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./logging.sh
source "$LIB_DIR/logging.sh"
# shellcheck source=./validation.sh
source "$LIB_DIR/validation.sh"

# Safe JSON parsing with error handling and defaults
safe_jq() {
  local filter="$1"
  local input="$2"
  local default="${3:-}"
  
  local result
  if result=$(echo "$input" | jq -r "$filter" 2>/dev/null); then
    if [[ "$result" == "null" || "$result" == "" ]]; then
      echo "$default"
    else
      echo "$result"
    fi
  else
    if [[ -n "$default" ]]; then
      echo "$default"
    else
      die "Failed to parse JSON with filter: $filter"
    fi
  fi
}

# Get script directory (for relative imports)
get_script_dir() {
  cd "$(dirname "${BASH_SOURCE[1]}")" && pwd
}

# Get claude hooks root directory by traversing up from current script
get_claude_hooks_dir() {
  local script_dir
  script_dir=$(get_script_dir)
  
  # Navigate up to find claude-hooks directory
  local current_dir="$script_dir"
  while [[ "$current_dir" != "/" ]]; do
    if [[ "$(basename "$current_dir")" == "claude-hooks" ]]; then
      echo "$current_dir"
      return
    fi
    current_dir=$(dirname "$current_dir")
  done
  
  die "Claude hooks directory not found from: $script_dir"
}

# Source JSON configuration with validation
source_config() {
  local claude_hooks_dir="${1:-$(get_claude_hooks_dir)}"
  local config_file="$claude_hooks_dir/hooks-config.json"
  
  check_file_exists "$config_file" "Configuration file"
  check_dependency "jq" "apt-get install jq or brew install jq"
  
  # Load JSON config into environment variables
  local base_url base_path notification_path stop_path
  base_url=$(jq -r '.service.baseUrl' "$config_file")
  base_path=$(jq -r '.api.basePath' "$config_file")
  notification_path=$(jq -r '.api.endpoints.notification' "$config_file")
  stop_path=$(jq -r '.api.endpoints.stop' "$config_file")
  
  # Construct full URLs from parts
  export NOTIFICATION_ENDPOINT="${base_url}${base_path}${notification_path}"
  export STOP_ENDPOINT="${base_url}${base_path}${stop_path}"
  export CLAUDE_HOOKS_TIMEOUT=$(jq -r '.timeout.seconds' "$config_file")
  export CLAUDE_HOOKS_SILENT_ERRORS=$(jq -r '.errorHandling.silentErrors' "$config_file")
  export CLAUDE_HOOKS_DEBUG=$(jq -r '.debug.enabled' "$config_file")
  
  log_debug "JSON configuration loaded from: $config_file"
}

# Read and validate payload from stdin
read_payload_from_stdin() {
  local payload
  if ! payload=$(cat -); then
    die "Failed to read payload from stdin"
  fi
  
  if [[ -z "$payload" ]]; then
    die "Empty payload received from stdin"
  fi
  
  # Validate it's valid JSON
  check_json_valid "$payload" "Input payload"
  
  log_debug "Payload received and validated from stdin"
  echo "$payload"
}

# Extract field from JSON payload with validation
extract_json_field() {
  local payload="$1"
  local field_name="$2"
  local description="${3:-$field_name}"
  local required="${4:-true}"
  
  local value
  value=$(safe_jq ".${field_name} // empty" "$payload")
  
  if [[ "$required" == "true" && -z "$value" ]]; then
    die "$description not found in payload"
  fi
  
  log_debug "$description extracted: '$value'"
  echo "$value"
}

# Retry function with exponential backoff
retry_with_backoff() {
  local max_attempts="$1"
  local delay="$2"
  local description="$3"
  shift 3
  
  local attempt=1
  while [[ $attempt -le $max_attempts ]]; do
    log_debug "Attempt $attempt/$max_attempts: $description"
    
    if "$@"; then
      log_debug "$description succeeded on attempt $attempt"
      return 0
    fi
    
    if [[ $attempt -eq $max_attempts ]]; then
      die "$description failed after $max_attempts attempts"
    fi
    
    log_debug "$description failed, retrying in ${delay}s..."
    sleep "$delay"
    delay=$((delay * 2))  # Exponential backoff
    ((attempt++))
  done
}
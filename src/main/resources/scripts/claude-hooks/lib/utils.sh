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
  
  # Load and export API key for authentication
  export CLAUDE_HOOKS_API_KEY=$(extract_api_key_from_config "$config_file" "API key" "true")
  log_debug "API key loaded: $(mask_api_key "$CLAUDE_HOOKS_API_KEY")"
  
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

# Mask API key for secure display (show first 10 chars + "...")
mask_api_key() {
  local api_key="$1"
  
  if [[ -z "$api_key" || "$api_key" == "null" ]]; then
    echo "(not set)"
    return
  fi
  
  if [[ ${#api_key} -le 10 ]]; then
    echo "$api_key"
  else
    echo "${api_key:0:10}..."
  fi
}

# Validate API key format (must start with chk_ and have minimum length)
validate_api_key_format() {
  local api_key="$1"
  local description="${2:-API key}"
  
  if [[ -z "$api_key" ]]; then
    die "$description cannot be empty"
  fi
  
  if [[ ! "$api_key" =~ ^chk_ ]]; then
    die "$description must start with 'chk_' prefix"
  fi
  
  if [[ ${#api_key} -lt 15 ]]; then
    die "$description is too short (minimum 15 characters required)"
  fi
  
  # Validate allowed characters (alphanumeric, underscore, hyphen, period)
  if [[ ! "$api_key" =~ ^[a-zA-Z0-9_.-]+$ ]]; then
    die "$description contains invalid characters (only letters, numbers, underscore, hyphen, and period allowed)"
  fi
  
  log_debug "$description format validation: passed"
}

# Extract API key from JSON configuration with validation
extract_api_key_from_config() {
  local config_file="$1"
  local description="${2:-API key}"
  local required="${3:-true}"
  
  check_file_exists "$config_file" "Configuration file"
  check_dependency "jq" "apt-get install jq or brew install jq"
  
  local api_key
  api_key=$(jq -r '.authentication.apiKey // empty' "$config_file")
  
  if [[ "$required" == "true" && ( -z "$api_key" || "$api_key" == "null" ) ]]; then
    die "$description not found in configuration file: $config_file"
  fi
  
  if [[ -n "$api_key" && "$api_key" != "null" ]]; then
    validate_api_key_format "$api_key" "$description"
    log_debug "$description extracted and validated from config"
  fi
  
  echo "$api_key"
}
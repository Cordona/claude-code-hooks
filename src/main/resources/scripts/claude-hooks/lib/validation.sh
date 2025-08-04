#!/usr/bin/env bash

# validation.sh
# Validation library for Claude Code Hooks scripts
# Provides dependency checks, file validation, and configuration validation

# Get lib directory for imports (avoid conflicting with main SCRIPT_DIR)
LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./logging.sh
source "$LIB_DIR/logging.sh"

# Validate required dependencies
check_dependency() {
  local cmd="$1"
  local install_hint="${2:-}"
  
  if ! command -v "$cmd" &> /dev/null; then
    local error_msg="Required dependency '$cmd' not found"
    if [[ -n "$install_hint" ]]; then
      error_msg="$error_msg. Install with: $install_hint"
    fi
    die "$error_msg"
  fi
}

# Validate file exists
check_file_exists() {
  local file_path="$1"
  local description="${2:-file}"
  
  if [[ ! -f "$file_path" ]]; then
    die "$description not found: $file_path"
  fi
}

# Validate directory exists
check_dir_exists() {
  local dir_path="$1"
  local description="${2:-directory}"
  
  if [[ ! -d "$dir_path" ]]; then
    die "$description not found: $dir_path"
  fi
}

# Validate required environment variable
check_env_var() {
  local var_name="$1"
  local description="${2:-$var_name}"
  
  if [[ -z "${!var_name:-}" ]]; then
    die "$description not configured (missing $var_name)"
  fi
}

# Validate URL format (basic check)
check_url_format() {
  local url="$1"
  local description="${2:-URL}"
  
  if [[ ! "$url" =~ ^https?:// ]]; then
    die "$description must start with http:// or https://"
  fi
}

# Validate JSON string
check_json_valid() {
  local json_string="$1"
  local description="${2:-JSON}"
  
  if ! echo "$json_string" | jq empty 2>/dev/null; then
    die "$description contains invalid JSON"
  fi
}

# Validate numeric value
check_numeric() {
  local value="$1"
  local description="${2:-value}"
  
  if ! [[ "$value" =~ ^[0-9]+$ ]]; then
    die "$description must be a positive integer"
  fi
}
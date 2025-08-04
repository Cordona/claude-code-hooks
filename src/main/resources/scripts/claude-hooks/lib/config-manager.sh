#!/usr/bin/env bash

# config-manager.sh
# Shared configuration management functions for Claude Code Hooks scripts
# Provides common functions for finding, reading, updating, and validating configuration files

# Get lib directory for imports (avoid conflicting with main SCRIPT_DIR)
LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./logging.sh
source "$LIB_DIR/logging.sh"
# shellcheck source=./validation.sh
source "$LIB_DIR/validation.sh"

# Find configuration file in standard locations
find_hooks_config_file() {
  local config_file_var="$1"  # Variable name to store result
  local config_file=""
  
  log_detail "Searching for existing hooks-config.json..."
  local search_paths=(
    "$HOME/.claude/scripts/claude-hooks/hooks-config.json"
    "$PWD/.claude/scripts/claude-hooks/hooks-config.json"
    "$(dirname "$SCRIPT_DIR")/hooks-config.json"
    "$SCRIPT_DIR/hooks-config.json"
  )
  
  for path in "${search_paths[@]}"; do
    log_detail "Checking: $path"
    if [[ -f "$path" ]]; then
      config_file="$path"
      log_success "Found configuration file: $config_file"
      break
    else
      log_detail "Not found: $path"
    fi
  done
  
  if [[ -z "$config_file" ]]; then
    log_error "No Claude Code Hooks configuration found"
    log_error "Please run deploy-claude-hooks.sh first to set up the hooks"
    return 1
  fi
  
  # Use indirect assignment to set the variable
  printf -v "$config_file_var" '%s' "$config_file"
  return 0
}

# Load configuration values from hooks-config.json
load_hooks_config() {
  local config_file="$1"
  local -n config_vars="$2"  # Associative array reference
  
  log_detail "Validating configuration file exists: $config_file"
  check_file_exists "$config_file" "Configuration file"
  
  log_detail "Checking for jq dependency..."
  check_dependency "jq" "apt-get install jq or brew install jq"
  
  log_detail "Parsing JSON configuration from: $config_file"
  
  # Extract values using jq and store in associative array
  log_detail "Extracting baseUrl from configuration..."
  config_vars["base_url"]=$(jq -r '.service.baseUrl // empty' "$config_file")
  log_detail "Raw baseUrl: '${config_vars["base_url"]}'"
  
  if [[ -n "${config_vars["base_url"]}" ]]; then
    log_detail "Parsing hostname and port from baseUrl..."
    config_vars["hostname"]=$(echo "${config_vars["base_url"]}" | sed -E 's|^https?://([^:]+).*|\1|')
    config_vars["port"]=$(echo "${config_vars["base_url"]}" | sed -E 's|^https?://[^:]+:([0-9]+).*|\1|')
    log_detail "Parsed hostname: '${config_vars["hostname"]}'"
    log_detail "Parsed port: '${config_vars["port"]}'"
  else
    log_warning "No baseUrl found in configuration"
  fi
  
  log_detail "Extracting timeout from configuration..."
  config_vars["timeout"]=$(jq -r '.timeout.seconds // empty' "$config_file")
  log_detail "Current timeout: '${config_vars["timeout"]}' seconds"
  
  log_detail "Extracting additional configuration values..."
  config_vars["base_path"]=$(jq -r '.api.basePath // empty' "$config_file")
  config_vars["silent_errors"]=$(jq -r '.errorHandling.silentErrors // empty' "$config_file")
  config_vars["debug_enabled"]=$(jq -r '.debug.enabled // empty' "$config_file")
  
  log_success "Configuration loaded successfully"
}

# Create backup of configuration file
create_config_backup() {
  local config_file="$1"
  local backup_file_var="$2"  # Variable name to store backup file path
  
  log_step "Creating backup of current configuration..."
  local backup_file="${config_file}.backup.$(date +%Y%m%d_%H%M%S)"
  log_detail "Backup file: $backup_file"
  
  if ! cp "$config_file" "$backup_file"; then
    log_error "Failed to create backup file: $backup_file"
    return 1
  fi
  
  log_success "Backup created: $backup_file"
  
  # Use indirect assignment to set the variable
  printf -v "$backup_file_var" '%s' "$backup_file"
  return 0
}

# Update configuration file with new values
update_hooks_config() {
  local config_file="$1"
  local -n new_values="$2"  # Associative array reference
  
  log_step "Updating configuration file..."
  local temp_file="${config_file}.tmp"
  log_detail "Using temporary file: $temp_file"
  
  # Build jq update expression
  local jq_args=()
  local jq_filter='. '
  
  if [[ -n "${new_values["base_url"]:-}" ]]; then
    log_detail "Updating baseUrl: ${new_values["base_url"]}"
    jq_args+=(--arg baseUrl "${new_values["base_url"]}")
    jq_filter+='| .service.baseUrl = $baseUrl '
  fi
  
  if [[ -n "${new_values["timeout"]:-}" ]]; then
    log_detail "Updating timeout: ${new_values["timeout"]} seconds"
    jq_args+=(--arg timeout "${new_values["timeout"]}")
    jq_filter+='| .timeout.seconds = ($timeout | tonumber) '
  fi
  
  if [[ -n "${new_values["base_path"]:-}" ]]; then
    log_detail "Updating basePath: ${new_values["base_path"]}"
    jq_args+=(--arg basePath "${new_values["base_path"]}")
    jq_filter+='| .api.basePath = $basePath '
  fi
  
  if [[ -n "${new_values["silent_errors"]:-}" ]]; then
    log_detail "Updating silentErrors: ${new_values["silent_errors"]}"
    jq_args+=(--arg silentErrors "${new_values["silent_errors"]}")
    jq_filter+='| .errorHandling.silentErrors = ($silentErrors | test("true")) '
  fi
  
  if [[ -n "${new_values["debug_enabled"]:-}" ]]; then
    log_detail "Updating debug: ${new_values["debug_enabled"]}"
    jq_args+=(--arg debugEnabled "${new_values["debug_enabled"]}")
    jq_filter+='| .debug.enabled = ($debugEnabled | test("true")) '
  fi
  
  # Apply updates using jq
  if ! jq "${jq_args[@]}" "$jq_filter" "$config_file" > "$temp_file"; then
    log_error "Failed to update configuration using jq"
    rm -f "$temp_file"
    return 1
  fi
  
  log_detail "Moving temporary file to final location..."
  if ! mv "$temp_file" "$config_file"; then
    log_error "Failed to move temporary file to final location"
    return 1
  fi
  
  log_success "Configuration file updated: $config_file"
  return 0
}

# Verify configuration file after updates
verify_hooks_config() {
  local config_file="$1"
  local -n expected_values="$2"  # Associative array reference
  
  log_step "Verifying configuration changes..."
  
  # Validate JSON syntax
  log_detail "Validating JSON syntax..."
  if ! jq empty "$config_file" &> /dev/null; then
    log_error "Generated configuration contains invalid JSON"
    return 1
  fi
  log_detail "JSON validation: passed"
  
  # Verify values were updated correctly
  log_detail "Verifying updated values..."
  
  if [[ -n "${expected_values["base_url"]:-}" ]]; then
    local actual_base_url
    actual_base_url=$(jq -r '.service.baseUrl' "$config_file")
    log_detail "Expected baseUrl: '${expected_values["base_url"]}'"
    log_detail "Actual baseUrl: '$actual_base_url'"
    if [[ "$actual_base_url" != "${expected_values["base_url"]}" ]]; then
      log_error "Failed to update base URL in configuration"
      return 1
    fi
    log_detail "BaseUrl verification: passed"
  fi
  
  if [[ -n "${expected_values["timeout"]:-}" ]]; then
    local actual_timeout
    actual_timeout=$(jq -r '.timeout.seconds' "$config_file")
    log_detail "Expected timeout: '${expected_values["timeout"]}'"
    log_detail "Actual timeout: '$actual_timeout'"
    if [[ "$actual_timeout" != "${expected_values["timeout"]}" ]]; then
      log_error "Failed to update timeout in configuration"
      return 1
    fi
    log_detail "Timeout verification: passed"
  fi
  
  log_success "Configuration verification completed successfully"
  return 0
}

# Generate new configuration file with provided values
generate_hooks_config_file() {
  local config_file="$1"
  local -n config_values="$2"  # Associative array reference
  
  log_step "Generating new hooks configuration..."
  log_detail "Target file: $config_file"
  
  # Ensure directory exists
  local config_dir
  config_dir=$(dirname "$config_file")
  if [[ ! -d "$config_dir" ]]; then
    log_detail "Creating configuration directory: $config_dir"
    mkdir -p "$config_dir"
  fi
  
  # Generate configuration with default values
  local base_url="${config_values["base_url"]:-http://localhost:8085}"
  local base_path="${config_values["base_path"]:-/api/v1/claude-code/hooks}"
  local timeout="${config_values["timeout"]:-10}"
  local silent_errors="${config_values["silent_errors"]:-true}"
  local debug_enabled="${config_values["debug_enabled"]:-false}"
  
  log_detail "Using baseUrl: $base_url"
  log_detail "Using basePath: $base_path"
  log_detail "Using timeout: $timeout seconds"
  
  # Create configuration file
  cat > "$config_file" << EOF
{
  "service": {
    "baseUrl": "$base_url"
  },
  "api": {
    "basePath": "$base_path",
    "endpoints": {
      "notification": "/notification/event",
      "stop": "/stop/event"
    }
  },
  "timeout": {
    "seconds": $timeout
  },
  "errorHandling": {
    "silentErrors": $silent_errors
  },
  "debug": {
    "enabled": $debug_enabled
  }
}
EOF
  
  # Validate generated JSON
  if ! jq empty "$config_file" &> /dev/null; then
    log_error "Generated configuration contains invalid JSON"
    return 1
  fi
  
  log_success "Generated hooks configuration: $config_file"
  return 0
}

# Parse endpoint URL into components
parse_endpoint_url() {
  local url="$1"
  local -n url_parts="$2"  # Associative array reference
  
  log_detail "Parsing endpoint URL: $url"
  
  # Extract protocol
  url_parts["protocol"]=$(echo "$url" | sed -E 's|^(https?)://.*|\1|')
  
  # Extract hostname
  url_parts["hostname"]=$(echo "$url" | sed -E 's|^https?://([^:]+).*|\1|')
  
  # Extract port
  url_parts["port"]=$(echo "$url" | sed -E 's|^https?://[^:]+:([0-9]+).*|\1|')
  
  # Extract path (if any)
  url_parts["path"]=$(echo "$url" | sed -E 's|^https?://[^/]+(/.*)|\1|' | sed 's|^[^/]*$||')
  
  log_detail "Parsed protocol: '${url_parts["protocol"]}'"
  log_detail "Parsed hostname: '${url_parts["hostname"]}'"
  log_detail "Parsed port: '${url_parts["port"]}'"
  log_detail "Parsed path: '${url_parts["path"]}'"
}

# Build endpoint URL from components
build_endpoint_url() {
  local hostname="$1"
  local port="$2"
  local protocol="${3:-http}"
  
  echo "${protocol}://${hostname}:${port}"
}
#!/usr/bin/env bash

# json-utils.sh
# Shared JSON manipulation utilities for Claude Code Hooks scripts
# Provides common functions for JSON parsing, validation, and manipulation

# Get lib directory for imports (avoid conflicting with main SCRIPT_DIR)
LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./logging.sh
source "$LIB_DIR/logging.sh"
# shellcheck source=./validation.sh
source "$LIB_DIR/validation.sh"

# Safe JSON query with error handling and defaults
safe_jq_query() {
  local filter="$1"
  local input="$2"
  local default="${3:-}"
  local description="${4:-JSON query}"
  
  log_detail "Executing jq query: $filter"
  
  local result
  if result=$(echo "$input" | jq -r "$filter" 2>/dev/null); then
    if [[ "$result" == "null" || "$result" == "" ]]; then
      log_detail "$description returned null/empty, using default: '$default'"
      echo "$default"
    else
      log_detail "$description result: '$result'"
      echo "$result"
    fi
  else
    if [[ -n "$default" ]]; then
      log_warning "$description failed, using default: '$default'"
      echo "$default"
    else
      log_error "Failed to parse JSON with filter: $filter"
      return 1
    fi
  fi
}

# Safe JSON file query with error handling
safe_jq_file() {
  local filter="$1"
  local file_path="$2"
  local default="${3:-}"
  local description="${4:-JSON file query}"
  
  check_file_exists "$file_path" "JSON file"
  
  log_detail "Executing jq query on file: $file_path"
  log_detail "Query filter: $filter"
  
  local result
  if result=$(jq -r "$filter" "$file_path" 2>/dev/null); then
    if [[ "$result" == "null" || "$result" == "" ]]; then
      log_detail "$description returned null/empty, using default: '$default'"
      echo "$default"
    else
      log_detail "$description result: '$result'"
      echo "$result"
    fi
  else
    if [[ -n "$default" ]]; then
      log_warning "$description failed, using default: '$default'"
      echo "$default"
    else
      log_error "Failed to parse JSON file '$file_path' with filter: $filter"
      return 1
    fi
  fi
}

# Update JSON file with multiple key-value pairs
update_json_file() {
  local file_path="$1"
  local -n updates="$2"  # Associative array reference
  local temp_file="${file_path}.tmp"
  
  log_detail "Updating JSON file: $file_path"
  log_detail "Using temporary file: $temp_file"
  
  check_file_exists "$file_path" "JSON file"
  
  # Build jq update expression
  local jq_args=()
  local jq_filter='. '
  
  for key in "${!updates[@]}"; do
    local value="${updates[$key]}"
    log_detail "Adding update: $key = $value"
    
    case "$key" in
      service.baseUrl)
        jq_args+=(--arg baseUrl "$value")
        jq_filter+='| .service.baseUrl = $baseUrl '
        ;;
      timeout.seconds)
        jq_args+=(--arg timeout "$value")
        jq_filter+='| .timeout.seconds = ($timeout | tonumber) '
        ;;
      api.basePath)
        jq_args+=(--arg basePath "$value")
        jq_filter+='| .api.basePath = $basePath '
        ;;
      errorHandling.silentErrors)
        jq_args+=(--arg silentErrors "$value")
        jq_filter+='| .errorHandling.silentErrors = ($silentErrors | test("true")) '
        ;;
      debug.enabled)
        jq_args+=(--arg debugEnabled "$value")
        jq_filter+='| .debug.enabled = ($debugEnabled | test("true")) '
        ;;
      *)
        log_warning "Unknown configuration key: $key, skipping..."
        ;;
    esac
  done
  
  log_detail "Generated jq filter: $jq_filter"
  
  # Apply updates using jq
  if ! jq "${jq_args[@]}" "$jq_filter" "$file_path" > "$temp_file"; then
    log_error "Failed to update JSON file using jq"
    rm -f "$temp_file"
    return 1
  fi
  
  # Validate updated JSON
  if ! jq empty "$temp_file" &> /dev/null; then
    log_error "Generated JSON contains syntax errors"
    rm -f "$temp_file"
    return 1
  fi
  
  # Move temporary file to final location
  if ! mv "$temp_file" "$file_path"; then
    log_error "Failed to move temporary file to final location"
    return 1
  fi
  
  log_success "JSON file updated successfully: $file_path"
  return 0
}

# Add or update a field in JSON structure
add_json_field() {
  local json_input="$1"
  local field_path="$2"
  local field_value="$3"
  local field_type="${4:-string}"  # string, number, boolean, array, object
  
  log_detail "Adding JSON field: $field_path = $field_value (type: $field_type)"
  
  local jq_filter
  case "$field_type" in
    string)
      jq_filter=".$field_path = \"$field_value\""
      ;;
    number)
      jq_filter=".$field_path = $field_value"
      ;;
    boolean)
      jq_filter=".$field_path = $field_value"
      ;;
    *)
      log_error "Unsupported field type: $field_type"
      return 1
      ;;
  esac
  
  local result
  if result=$(echo "$json_input" | jq "$jq_filter" 2>/dev/null); then
    echo "$result"
  else
    log_error "Failed to add field '$field_path' to JSON"
    return 1
  fi
}

# Remove a field from JSON structure
remove_json_field() {
  local json_input="$1"
  local field_path="$2"
  
  log_detail "Removing JSON field: $field_path"
  
  local result
  if result=$(echo "$json_input" | jq "del(.$field_path)" 2>/dev/null); then
    echo "$result"
  else
    log_error "Failed to remove field '$field_path' from JSON"
    return 1
  fi
}

# Merge two JSON objects
merge_json_objects() {
  local base_json="$1"
  local overlay_json="$2"
  
  log_detail "Merging JSON objects"
  log_detail "Base JSON length: $(echo "$base_json" | wc -c) characters"
  log_detail "Overlay JSON length: $(echo "$overlay_json" | wc -c) characters"
  
  local result
  if result=$(jq -s '.[0] * .[1]' <<< "$base_json"$'\n'"$overlay_json" 2>/dev/null); then
    log_detail "JSON merge successful"
    echo "$result"
  else
    log_error "Failed to merge JSON objects"
    return 1
  fi
}

# Validate JSON structure against expected schema
validate_json_schema() {
  local json_input="$1"
  local schema_type="$2"  # hooks-config, claude-settings
  
  log_detail "Validating JSON schema: $schema_type"
  
  # Basic JSON syntax validation
  if ! echo "$json_input" | jq empty 2>/dev/null; then
    log_error "Invalid JSON syntax"
    return 1
  fi
  
  case "$schema_type" in
    hooks-config)
      validate_hooks_config_schema "$json_input"
      ;;
    claude-settings)
      validate_claude_settings_schema "$json_input"
      ;;
    *)
      log_warning "Unknown schema type: $schema_type, performing basic validation only"
      ;;
  esac
}

# Validate hooks-config.json schema
validate_hooks_config_schema() {
  local json_input="$1"
  
  log_detail "Validating hooks-config.json schema"
  
  # Check for required top-level fields
  local required_fields=("service" "api" "timeout" "errorHandling" "debug")
  for field in "${required_fields[@]}"; do
    if ! echo "$json_input" | jq -e "has(\"$field\")" >/dev/null 2>&1; then
      log_error "Missing required field: $field"
      return 1
    fi
    log_detail "Required field present: $field"
  done
  
  # Check service.baseUrl
  local base_url
  base_url=$(echo "$json_input" | jq -r '.service.baseUrl // empty')
  if [[ -z "$base_url" ]]; then
    log_error "Missing service.baseUrl"
    return 1
  fi
  log_detail "service.baseUrl present: $base_url"
  
  # Check api.basePath and endpoints
  local base_path
  base_path=$(echo "$json_input" | jq -r '.api.basePath // empty')
  if [[ -z "$base_path" ]]; then
    log_error "Missing api.basePath"
    return 1
  fi
  log_detail "api.basePath present: $base_path"
  
  # Check for notification and stop endpoints
  local notification_endpoint stop_endpoint
  notification_endpoint=$(echo "$json_input" | jq -r '.api.endpoints.notification // empty')
  stop_endpoint=$(echo "$json_input" | jq -r '.api.endpoints.stop // empty')
  
  if [[ -z "$notification_endpoint" ]]; then
    log_error "Missing api.endpoints.notification"
    return 1
  fi
  log_detail "api.endpoints.notification present: $notification_endpoint"
  
  if [[ -z "$stop_endpoint" ]]; then
    log_error "Missing api.endpoints.stop"
    return 1
  fi
  log_detail "api.endpoints.stop present: $stop_endpoint"
  
  log_success "hooks-config.json schema validation passed"
  return 0
}

# Validate Claude settings.json schema (basic)
validate_claude_settings_schema() {
  local json_input="$1"
  
  log_detail "Validating Claude settings.json schema"
  
  # Check if hooks object exists
  if ! echo "$json_input" | jq -e 'has("hooks")' >/dev/null 2>&1; then
    log_warning "No hooks section found in Claude settings"
  else
    log_detail "hooks section present"
  fi
  
  log_success "Claude settings.json schema validation passed"
  return 0
}

# Pretty print JSON with proper formatting
format_json() {
  local json_input="$1"
  local indent="${2:-2}"
  
  log_detail "Formatting JSON with $indent-space indentation"
  
  local result
  if result=$(echo "$json_input" | jq --indent "$indent" . 2>/dev/null); then
    echo "$result"
  else
    log_error "Failed to format JSON"
    return 1
  fi
}

# Extract specific values from hooks configuration
extract_hooks_config_values() {
  local config_file="$1"
  local -n extracted_values="$2"  # Associative array reference
  
  log_detail "Extracting values from hooks configuration: $config_file"
  
  extracted_values["base_url"]=$(safe_jq_file '.service.baseUrl' "$config_file" "" "base URL")
  extracted_values["base_path"]=$(safe_jq_file '.api.basePath' "$config_file" "" "base path")
  extracted_values["notification_endpoint"]=$(safe_jq_file '.api.endpoints.notification' "$config_file" "" "notification endpoint")
  extracted_values["stop_endpoint"]=$(safe_jq_file '.api.endpoints.stop' "$config_file" "" "stop endpoint")
  extracted_values["timeout"]=$(safe_jq_file '.timeout.seconds' "$config_file" "10" "timeout")
  extracted_values["silent_errors"]=$(safe_jq_file '.errorHandling.silentErrors' "$config_file" "true" "silent errors")
  extracted_values["debug_enabled"]=$(safe_jq_file '.debug.enabled' "$config_file" "false" "debug enabled")
  
  # Parse hostname and port from base_url if available
  if [[ -n "${extracted_values["base_url"]}" ]]; then
    extracted_values["hostname"]=$(echo "${extracted_values["base_url"]}" | sed -E 's|^https?://([^:]+).*|\1|')
    extracted_values["port"]=$(echo "${extracted_values["base_url"]}" | sed -E 's|^https?://[^:]+:([0-9]+).*|\1|')
    log_detail "Parsed hostname: '${extracted_values["hostname"]}'"
    log_detail "Parsed port: '${extracted_values["port"]}'"
  fi
  
  log_success "Configuration values extracted successfully"
}
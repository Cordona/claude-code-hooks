#!/usr/bin/env bash

# extract-cwd.sh
# Extracts the current working directory from Claude Code session transcript
# Usage: echo "$payload" | extract-cwd.sh
# Output: Working directory path or empty string if not found

set -euo pipefail

# Get script directory and source required libraries
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./utils.sh
source "$SCRIPT_DIR/utils.sh"

# Main function
main() {
  log_debug "Starting cwd extraction"
  
  validate_dependencies
  local payload
  payload=$(read_payload_from_stdin)
  
  local transcript_path cwd
  transcript_path=$(extract_transcript_path "$payload")
  
  validate_transcript_file "$transcript_path"
  
  cwd=$(extract_cwd_from_transcript "$transcript_path")
  
  log_debug "Extracted cwd: '$cwd'"
  echo "$cwd"
}

# Validate required dependencies
validate_dependencies() {
  check_dependency "jq" "apt-get install jq or brew install jq"
  check_dependency "grep"
}

# Extract transcript_path from payload
extract_transcript_path() {
  local payload="$1"
  extract_json_field "$payload" "transcript_path" "Transcript path"
}

# Validate transcript file exists
validate_transcript_file() {
  local transcript_path="$1"
  check_file_exists "$transcript_path" "Transcript file"
}

# Extract cwd from transcript file
extract_cwd_from_transcript() {
  local transcript_path="$1"
  local cwd
  
  log_debug "Extracting cwd from transcript: $transcript_path"
  
  # Look for "cwd":"..." pattern and extract the path
  if ! cwd=$(grep -o '"cwd":"[^"]*"' "$transcript_path" | head -n 1 | cut -d':' -f2 | tr -d '"' 2>/dev/null || true); then
    log_warning "Failed to extract cwd from transcript file"
    cwd=""
  fi
  
  if [[ -z "$cwd" ]]; then
    log_warning "No cwd found in transcript file"
  fi
  
  echo "$cwd"
}

# Run main function
main "$@"
#!/usr/bin/env bash

# call-endpoint.sh
# Makes HTTP calls to Claude Code Hooks service with proper headers
# Usage: call-endpoint.sh <endpoint_url> <cwd> <payload>
# Arguments:
#   endpoint_url: Full URL to the service endpoint
#   cwd: Current working directory to include in headers
#   payload: JSON payload to send in request body

set -euo pipefail

# Get script directory and source required libraries
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./validation.sh
source "$SCRIPT_DIR/validation.sh"

# Main function
main() {
  validate_dependencies
  validate_arguments "$@"
  
  local endpoint_url="$1"
  local cwd="$2"
  local payload="$3"
  
  log_debug "Calling endpoint: $endpoint_url"
  log_debug "Working directory: $cwd"
  
  make_http_request "$endpoint_url" "$cwd" "$payload"
  
  log_debug "HTTP request completed successfully"
}

# Validate required dependencies
validate_dependencies() {
  check_dependency "curl" "apt-get install curl or brew install curl"
}

# Validate command line arguments
validate_arguments() {
  if [[ $# -ne 3 ]]; then
    die "Usage: call-endpoint.sh <endpoint_url> <cwd> <payload>"
  fi
  
  local endpoint_url="$1"
  local cwd="$2"
  local payload="$3"
  
  if [[ -z "$endpoint_url" ]]; then
    die "endpoint_url cannot be empty"
  fi
  
  if [[ -z "$payload" ]]; then
    die "payload cannot be empty"
  fi
  
  log_debug "Arguments validated successfully"
}

# Build HTTP headers array
build_headers() {
  local cwd="$1"
  
  headers=(
    "-H" "Content-Type: application/json"
  )
  
  # Add cwd header if provided
  if [[ -n "$cwd" ]]; then
    headers+=("-H" "X-Context-Work-Directory: $cwd")
    log_debug "Added X-Context-Work-Directory header: $cwd"
  fi
}

# Make the HTTP request with proper error handling
make_http_request() {
  local endpoint_url="$1"
  local cwd="$2"
  local payload="$3"
  
  # Set timeout from environment or default to 10 seconds
  local timeout="${CLAUDE_HOOKS_TIMEOUT:-10}"
  
  # Build headers array
  local headers
  build_headers "$cwd"
  
  log_debug "Making HTTP POST request with timeout: ${timeout}s"
  
  # Make the HTTP request
  # Use --fail to ensure non-zero exit code on HTTP errors
  # Use --silent to suppress progress output
  # Use --show-error to show errors even in silent mode
  if ! curl \
    --fail \
    --silent \
    --show-error \
    --max-time "$timeout" \
    -X POST \
    "${headers[@]}" \
    -d "$payload" \
    "$endpoint_url"; then
    
    die "Failed to call endpoint: $endpoint_url"
  fi
}

# Run main function
main "$@"
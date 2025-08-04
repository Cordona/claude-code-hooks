#!/usr/bin/env bash

# call-endpoint.sh
# Makes HTTP calls to Claude Code Hooks service
# Usage: call-endpoint.sh <endpoint_url> <payload>
# Arguments:
#   endpoint_url: Full URL to the service endpoint
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
  local payload="$2"
  
  log_debug "Calling endpoint: $endpoint_url"
  
  make_http_request "$endpoint_url" "$payload"
  
  log_debug "HTTP request completed successfully"
}

# Validate required dependencies
validate_dependencies() {
  check_dependency "curl" "apt-get install curl or brew install curl"
  
  # Check for required API key environment variable
  if [[ -z "${CLAUDE_HOOKS_API_KEY:-}" ]]; then
    die "CLAUDE_HOOKS_API_KEY environment variable not set. Please ensure hooks configuration is loaded."
  fi
  
  log_debug "API key validation: passed"
}

# Validate command line arguments
validate_arguments() {
  if [[ $# -ne 2 ]]; then
    die "Usage: call-endpoint.sh <endpoint_url> <payload>"
  fi
  
  local endpoint_url="$1"
  local payload="$2"
  
  if [[ -z "$endpoint_url" ]]; then
    die "endpoint_url cannot be empty"
  fi
  
  if [[ -z "$payload" ]]; then
    die "payload cannot be empty"
  fi
  
  log_debug "Arguments validated successfully"
}

# Make the HTTP request with proper error handling
make_http_request() {
  local endpoint_url="$1"
  local payload="$2"
  
  # Set timeout from environment or default to 10 seconds
  local timeout="${CLAUDE_HOOKS_TIMEOUT:-10}"
  
  log_debug "Making HTTP POST request with timeout: ${timeout}s"
  log_debug "Using API key: $(mask_api_key "$CLAUDE_HOOKS_API_KEY")"
  
  # Make the HTTP request with API key authentication
  # Use --fail to ensure non-zero exit code on HTTP errors
  # Use --silent to suppress progress output
  # Use --show-error to show errors even in silent mode
  if ! curl \
    --fail \
    --silent \
    --show-error \
    --max-time "$timeout" \
    -X POST \
    -H "Content-Type: application/json" \
    -H "X-API-Key: ${CLAUDE_HOOKS_API_KEY}" \
    -d "$payload" \
    "$endpoint_url"; then
    
    die "Failed to call endpoint: $endpoint_url"
  fi
}

# Run main function
main "$@"
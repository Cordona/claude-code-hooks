#!/usr/bin/env bash

# deploy-claude-hooks.sh
# Deployment script for Claude Code Hooks modular scripts
# Copies scripts from resources and configures Claude settings.json

set -euo pipefail

# Get script directory and source logging
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/logging.sh
source "$SCRIPT_DIR/lib/logging.sh"
# shellcheck source=lib/config-manager.sh
source "$SCRIPT_DIR/lib/config-manager.sh"
# shellcheck source=lib/json-utils.sh
source "$SCRIPT_DIR/lib/json-utils.sh"
# shellcheck source=lib/utils.sh
source "$SCRIPT_DIR/lib/utils.sh"

# Common variables (SUCCESS and FAILURE come from logging.sh)
YES="yes"
NO="no"
MAX_RETRIES=3

# Debug logging (disabled by default)
# export CLAUDE_HOOKS_DEBUG=true

# Configuration variables
SERVICE_ENDPOINT=""
SERVICE_API_KEY=""
DEPLOYMENT_TARGET=""
HOOK_CHOICE=""
CLAUDE_CONFIG_DIR=""
CLAUDE_CONFIG_FILE=""
SCRIPTS_SOURCE_DIR="$SCRIPT_DIR"
SCRIPTS_TARGET_DIR=""

# Main function
main() {
  print_info_section
  detect_claude_config_path
  check_dependencies
  prompt_for_service_endpoint
  prompt_for_api_key
  prompt_for_deployment_target
  prompt_for_hook_selection
  backup_existing_config
  deploy_scripts_to_target
  generate_hooks_config
  install_selected_hooks
  verify_installation
  display_completion_message
}

# Display welcome information
print_info_section() {
  log_info "Welcome to the Claude Code Hooks Deployment Script!"
  echo
  log_info "This script will:"
  log_info "  - Deploy modular hook scripts to your chosen location"
  log_info "  - Configure Claude settings.json with script paths"
  log_info "  - Generate hooks-config.json with your service endpoint and API key"
  log_info "  - Preserve existing hooks while adding new functionality"
  echo
}

# Detect Claude configuration path based on OS (copied from setup-claude-hooks.sh)
detect_claude_config_path() {
  local os_type
  os_type=$(uname -s)
  
  case "$os_type" in
    Darwin)
      # macOS
      CLAUDE_CONFIG_DIR="$HOME/.claude"
      log_info "Detected macOS - Using config path: $CLAUDE_CONFIG_DIR"
      ;;
    Linux)
      # Linux
      if [[ -n "$XDG_CONFIG_HOME" ]]; then
        CLAUDE_CONFIG_DIR="$XDG_CONFIG_HOME/claude"
      else
        CLAUDE_CONFIG_DIR="$HOME/.config/claude"
      fi
      log_info "Detected Linux - Using config path: $CLAUDE_CONFIG_DIR"
      ;;
    CYGWIN*|MINGW32*|MSYS*|MINGW*)
      # Windows (Git Bash, MSYS2, etc.)
      if [[ -n "$APPDATA" ]]; then
        CLAUDE_CONFIG_DIR="$APPDATA/claude"
      else
        CLAUDE_CONFIG_DIR="$HOME/.claude"
      fi
      log_info "Detected Windows - Using config path: $CLAUDE_CONFIG_DIR"
      ;;
    *)
      # Unknown OS - fallback to HOME/.claude
      CLAUDE_CONFIG_DIR="$HOME/.claude"
      log_warning "Unknown OS ($os_type) - Using fallback config path: $CLAUDE_CONFIG_DIR"
      ;;
  esac
  
  CLAUDE_CONFIG_FILE="$CLAUDE_CONFIG_DIR/settings.json"
  
  # Check if config directory exists, if not, look for alternative locations
  if [[ ! -d "$CLAUDE_CONFIG_DIR" ]]; then
    log_info "Primary config directory not found, searching for existing Claude configuration..."
    
    # Try common alternative locations
    local alternative_paths=(
      "$HOME/.claude"
      "$HOME/.config/claude"
      "$HOME/Library/Application Support/claude"
      "$APPDATA/claude"
    )
    
    for alt_path in "${alternative_paths[@]}"; do
      if [[ -d "$alt_path" && -f "$alt_path/settings.json" ]]; then
        CLAUDE_CONFIG_DIR="$alt_path"
        CLAUDE_CONFIG_FILE="$alt_path/settings.json"
        log_success "Found existing Claude configuration at: $CLAUDE_CONFIG_DIR"
        return
      fi
    done
    
    log_info "No existing Claude configuration found. Will create new one at: $CLAUDE_CONFIG_DIR"
  else
    log_success "Claude configuration directory found: $CLAUDE_CONFIG_DIR"
  fi
}

# Check if required dependencies are installed (copied from setup-claude-hooks.sh)
check_dependencies() {
  log_info "Checking required dependencies..."
  
  local missing_deps=()
  local install_instructions=()
  
  # Check for jq
  if ! command -v jq &> /dev/null; then
    missing_deps+=("jq")
    install_instructions+=("  - macOS: brew install jq")
    install_instructions+=("  - Ubuntu/Debian: sudo apt-get install jq")
    install_instructions+=("  - CentOS/RHEL: sudo yum install jq")
    install_instructions+=("  - Windows: Download from https://stedolan.github.io/jq/download/")
  fi

  # Check for curl
  if ! command -v curl &> /dev/null; then
    missing_deps+=("curl")
    install_instructions+=("  - macOS: brew install curl")
    install_instructions+=("  - Ubuntu/Debian: sudo apt-get install curl")
    install_instructions+=("  - CentOS/RHEL: sudo yum install curl")
    install_instructions+=("  - Windows: Usually pre-installed, or download from https://curl.se/windows/")
  fi

  # If dependencies are missing, show them and prompt user
  if [[ ${#missing_deps[@]} -gt 0 ]]; then
    log_error "Missing required dependencies: ${missing_deps[*]}"
    echo
    echo -e "${COLOR_RED}Please install the missing dependencies with one of the following commands:${COLOR_RESET}"
    for instruction in "${install_instructions[@]}"; do
      echo -e "${COLOR_RED}$instruction${COLOR_RESET}"
    done
    echo
    
    # Prompt user to continue after installing
    retries=0
    while true; do
      read -rp "Have you installed the missing dependencies? Continue? [y/n]: " continue_choice
      continue_choice=$(normalize_input "$continue_choice")
      
      if [[ "$continue_choice" == "$YES" ]]; then
        log_info "Rechecking dependencies..."
        check_dependencies  # Recursive call to recheck
        return
      elif [[ "$continue_choice" == "$NO" ]]; then
        log_info "Deployment cancelled by user. Please install dependencies and run the script again."
        exit "$SUCCESS"
      else
        log_error "Invalid input. Please enter 'y' for yes or 'n' for no."
        ((retries++))
        if [[ $retries -ge $MAX_RETRIES ]]; then
          log_error "Maximum retries reached. Exiting..."
          exit "$FAILURE"
        fi
      fi
    done
  fi

  log_success "All dependencies are available (jq, curl, bash)."
}

# Prompt for service endpoint configuration
prompt_for_service_endpoint() {
  retries=0
  while true; do
    echo "Where is your Claude Code Hooks service deployed?"
    echo "1) Local deployment"
    echo "2) Remote deployment (custom URL)"
    read -rp "Please enter your choice [1/2]: " deployment_type
    
    case "$deployment_type" in
      1|local)
        prompt_for_local_endpoint
        break
        ;;
      2|remote)
        read -rp "Enter the remote service URL (e.g., https://hooks.example.com:8080): " SERVICE_ENDPOINT
        if [[ -n "$SERVICE_ENDPOINT" ]]; then
          log_info "Using remote deployment: $SERVICE_ENDPOINT"
          break
        else
          log_error "Service endpoint cannot be empty."
        fi
        ;;
      *)
        log_error "Invalid choice. Please select '1' for local or '2' for remote."
        ((retries++))
        if [[ $retries -ge $MAX_RETRIES ]]; then
          log_error "Maximum retries reached. Exiting..."
          exit "$FAILURE"
        fi
        ;;
    esac
  done
}

# Prompt for local endpoint configuration
prompt_for_local_endpoint() {
  local hostname port
  
  echo
  log_info "Configuring local deployment endpoint..."
  
  # Prompt for hostname with default
  read -rp "Enter hostname [localhost]: " hostname
  hostname="${hostname:-localhost}"
  
  # Prompt for port with default
  read -rp "Enter port [8085]: " port
  port="${port:-8085}"
  
  # Validate port is numeric
  if ! [[ "$port" =~ ^[0-9]+$ ]]; then
    log_error "Port must be a number. Using default port 8085."
    port="8085"
  fi
  
  SERVICE_ENDPOINT="http://${hostname}:${port}"
  log_info "Using local deployment: $SERVICE_ENDPOINT"
}

# Prompt for deployment target
prompt_for_deployment_target() {
  retries=0
  while true; do
    echo "Where would you like to deploy the scripts?"
    echo "1) Global deployment (~/.claude/scripts) - Available to all projects"
    echo "2) Project deployment (./PROJECT/.claude/scripts) - This project only"
    read -rp "Please enter your choice [1/2]: " target_choice
    
    case "$target_choice" in
      1|global)
        DEPLOYMENT_TARGET="global"
        log_info "Using global deployment"
        break
        ;;
      2|project)
        DEPLOYMENT_TARGET="project"
        log_info "Using project-local deployment"
        break
        ;;
      *)
        log_error "Invalid choice. Please select '1' for global or '2' for project."
        ((retries++))
        if [[ $retries -ge $MAX_RETRIES ]]; then
          log_error "Maximum retries reached. Exiting..."
          exit "$FAILURE"
        fi
        ;;
    esac
  done
}

# Prompt for hook selection
prompt_for_hook_selection() {
  retries=0
  while true; do
    echo "Which hooks would you like to install?"
    echo "1) Notification hooks only - Shows project paths when Claude needs permission"
    echo "2) Stop hooks only - Shows project paths when Claude completes tasks"
    echo "3) Both notification and stop hooks - Complete project path tracking"
    read -rp "Please enter your choice [1/2/3]: " hook_choice_input
    
    case "$hook_choice_input" in
      1|notification)
        HOOK_CHOICE="notification"
        log_info "Installing notification hooks only"
        break
        ;;
      2|stop)
        HOOK_CHOICE="stop"
        log_info "Installing stop hooks only"
        break
        ;;
      3|both)
        HOOK_CHOICE="both"
        log_info "Installing both notification and stop hooks"
        break
        ;;
      *)
        log_error "Invalid choice. Please select '1', '2', or '3'."
        ((retries++))
        if [[ $retries -ge $MAX_RETRIES ]]; then
          log_error "Maximum retries reached. Exiting..."
          exit "$FAILURE"
        fi
        ;;
    esac
  done
}

# Prompt for API key configuration
prompt_for_api_key() {
  local retries=0
  local user_api_key=""
  
  echo
  log_info "API Key Configuration"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "The Claude Code Hooks service requires an API key for authentication."
  echo "You can generate an API key by:"
  echo "  1. Starting your Claude Code Hooks service"
  echo "  2. Visiting the service endpoint in your browser"
  echo "  3. Using the API key generation feature"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo
  
  while [[ $retries -lt $MAX_RETRIES ]]; do
    read -rp "Enter your API key (starts with 'chk_'): " user_api_key
    
    if [[ -z "$user_api_key" ]]; then
      log_error "API key cannot be empty"
      ((retries++))
      continue
    fi
    
    # Validate API key format using our utility function
    if validate_api_key_format "$user_api_key" "API key" 2>/dev/null; then
      log_info "API key format validation: passed"
      log_info "API key to be configured: $(mask_api_key "$user_api_key")"
      echo
      read -rp "Is this API key correct? [y/N]: " confirm
      
      case "$confirm" in
        y|Y|yes|YES)
          SERVICE_API_KEY="$user_api_key"
          log_success "API key configured successfully"
          return 0
          ;;
        *)
          log_info "Please re-enter your API key"
          ((retries++))
          continue
          ;;
      esac
    else
      log_error "Invalid API key format. API key must:"
      log_error "  - Start with 'chk_' prefix"
      log_error "  - Be at least 15 characters long"
      log_error "  - Contain only valid characters"
      ((retries++))
    fi
  done
  
  log_error "Maximum retries reached for API key input. Exiting..."
  exit "$FAILURE"
}

# Backup existing configuration
backup_existing_config() {
  if [[ -f "$CLAUDE_CONFIG_FILE" ]]; then
    local backup_file
    backup_file="$CLAUDE_CONFIG_FILE.backup.$(date +%Y%m%d_%H%M%S)"
    cp "$CLAUDE_CONFIG_FILE" "$backup_file"
    log_success "Backup created: $backup_file"
  else
    log_info "No existing configuration found. Will create new settings.json file."
  fi
}

# Deploy scripts to target location
deploy_scripts_to_target() {
  # Determine target directory based on deployment choice
  if [[ "$DEPLOYMENT_TARGET" == "global" ]]; then
    SCRIPTS_TARGET_DIR="$CLAUDE_CONFIG_DIR/scripts/claude-hooks"
  else
    # Project-local deployment
    SCRIPTS_TARGET_DIR="$PWD/.claude/scripts/claude-hooks"
  fi
  
  log_info "Deploying scripts to: $SCRIPTS_TARGET_DIR"
  
  # Create target directory structure
  mkdir -p "$SCRIPTS_TARGET_DIR/lib"
  mkdir -p "$SCRIPTS_TARGET_DIR/hooks"
  
  # Copy all scripts from source to target
  log_info "Copying script files..."
  
  # Copy lib scripts
  cp "$SCRIPTS_SOURCE_DIR/lib/"*.sh "$SCRIPTS_TARGET_DIR/lib/"
  
  # Copy hook scripts  
  cp "$SCRIPTS_SOURCE_DIR/hooks/"*.sh "$SCRIPTS_TARGET_DIR/hooks/"
  
  # Set executable permissions
  chmod +x "$SCRIPTS_TARGET_DIR/lib/"*.sh
  chmod +x "$SCRIPTS_TARGET_DIR/hooks/"*.sh
  
  log_success "Scripts deployed successfully to: $SCRIPTS_TARGET_DIR"
}

# Generate hooks-config.json with service endpoint
generate_hooks_config() {
  log_step "Generating hooks configuration..."
  
  local config_file="$SCRIPTS_TARGET_DIR/hooks-config.json"
  log_detail "Target configuration file: $config_file"
  
  # Prepare configuration values
  local base_url="$SERVICE_ENDPOINT"
  local api_key="$SERVICE_API_KEY"
  local base_path="/api/v1/claude-code/hooks"
  local timeout="10"
  local silent_errors="true"
  local debug_enabled="false"
  
  log_detail "Using service endpoint: $base_url"
  log_detail "Using API key: $(mask_api_key "$api_key")"
  log_detail "Using API base path: $base_path"
  
  # Generate configuration JSON directly for Bash 3.2 compatibility
  cat > "$config_file" << EOF
{
  "service": {
    "baseUrl": "$base_url"
  },
  "authentication": {
    "apiKey": "$api_key"
  },
  "api": {
    "basePath": "$base_path",
    "endpoints": {
      "notification": "/notification/event",
      "stop": "/stop/event",
      "events": "/events/stream"
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

  # Validate generated configuration
  if ! jq empty "$config_file" &> /dev/null; then
    log_error "Generated configuration contains invalid JSON"
    exit 1
  fi
  
  log_success "Generated hooks configuration: $config_file"
}

# Install selected hooks (adapted from setup-claude-hooks.sh)
install_selected_hooks() {
  log_info "Installing selected hooks (preserving existing hooks)..."
  
  # Ensure Claude config directory exists
  if [[ ! -d "$CLAUDE_CONFIG_DIR" ]]; then
    mkdir -p "$CLAUDE_CONFIG_DIR"
    log_info "Created Claude configuration directory: $CLAUDE_CONFIG_DIR"
  fi
  
  # Create base configuration structure
  if [[ ! -f "$CLAUDE_CONFIG_FILE" ]]; then
    echo '{"model": "sonnet", "hooks": {}}' | jq . > "$CLAUDE_CONFIG_FILE"
    log_info "Creating new configuration"
  else
    log_info "Loading existing configuration"
  fi
  
  # Validate existing JSON
  if ! jq empty "$CLAUDE_CONFIG_FILE" &> /dev/null; then
    log_error "Invalid JSON in existing configuration file"
    exit "$FAILURE"
  fi
  
  # Ensure hooks object exists
  jq '.hooks //= {}' "$CLAUDE_CONFIG_FILE" > "${CLAUDE_CONFIG_FILE}.tmp" && mv "${CLAUDE_CONFIG_FILE}.tmp" "$CLAUDE_CONFIG_FILE"
  
  # Track if any changes were made
  local changes_made=false
  
  # Install notification hooks (preserve existing)
  if [[ "$HOOK_CHOICE" == "notification" || "$HOOK_CHOICE" == "both" ]]; then
    log_info "Processing notification hooks..."
    set +e  # Temporarily disable exit on error
    install_hook_type "notification" "$CLAUDE_CONFIG_FILE"
    local exit_code=$?
    set -e  # Re-enable exit on error
    log_debug "install_hook_type notification returned: $exit_code"
    if [[ $exit_code -eq 0 ]]; then
      changes_made=true
      log_info "Notification hooks installed successfully"
    elif [[ $exit_code -eq 1 ]]; then
      log_info "Notification hooks already exist, continuing..."
    elif [[ $exit_code -eq 2 ]]; then
      log_error "Failed to install notification hooks"
      exit "$FAILURE"
    fi
    log_debug "Finished processing notification hooks"
  fi
  
  # Install stop hooks (preserve existing)
  if [[ "$HOOK_CHOICE" == "stop" || "$HOOK_CHOICE" == "both" ]]; then
    log_info "Processing stop hooks..."
    set +e  # Temporarily disable exit on error
    install_hook_type "stop" "$CLAUDE_CONFIG_FILE"
    local exit_code=$?
    set -e  # Re-enable exit on error
    if [[ $exit_code -eq 0 ]]; then
      changes_made=true
      log_info "Stop hooks installed successfully"
    elif [[ $exit_code -eq 1 ]]; then
      log_info "Stop hooks already exist, continuing..."
    elif [[ $exit_code -eq 2 ]]; then
      log_error "Failed to install stop hooks"
      exit "$FAILURE"
    fi
  fi
  
  if [[ "$changes_made" == true ]]; then
    log_success "Configuration updated successfully: $CLAUDE_CONFIG_FILE"
  else
    log_info "No changes made to configuration (all hooks already exist): $CLAUDE_CONFIG_FILE"
  fi
}

# Install a specific hook type (adapted from setup-claude-hooks.sh)
install_hook_type() {
  local hook_type="$1"
  local config_file="$2"
  
  log_debug "install_hook_type called with: $hook_type, $config_file"
  
  # Capitalize first letter for JSON field name  
  local json_field
  json_field="$(echo "${hook_type:0:1}" | tr '[:lower:]' '[:upper:]')${hook_type:1}"
  
  log_debug "JSON field: $json_field"
  
  # Create the hook command using our deployed script paths
  local hook_command
  hook_command=$(build_absolute_script_path "$hook_type")
  
  # Check if our hook already exists by looking for our script path pattern
  local hook_already_exists
  local script_pattern="scripts/claude-hooks/hooks/${hook_type}-hook.sh"
  hook_already_exists=$(jq --arg script_pattern "$script_pattern" --arg field "$json_field" '
    [.hooks[$field][]?.hooks[]? | select(.type == "command" and (.command | contains($script_pattern)))] | length' "$config_file")
  
  if [[ "$hook_already_exists" != "0" ]]; then
    log_warning "Claude Code ${hook_type} hook already exists. Skipping installation."
    log_debug "Returning 1 from install_hook_type for $hook_type"
    return 1  # No changes made
  fi
  
  log_info "Installing ${hook_type} hook"
  
  # Check if hooks for this type already exist
  local existing_hooks
  existing_hooks=$(jq --arg field "$json_field" '.hooks[$field] // []' "$config_file")
  
  if [[ "$existing_hooks" != "[]" ]]; then
    log_info "Existing ${hook_type} hooks found, adding our hook to existing ones"
    
    # Look for existing wildcard matcher
    local wildcard_matcher_exists
    wildcard_matcher_exists=$(jq --arg field "$json_field" '[.hooks[$field][] | select(.matcher == "*")] | length' "$config_file")
    
    if [[ "$wildcard_matcher_exists" != "0" ]]; then
      # Add to existing wildcard matcher
      jq --arg cmd "$hook_command" --arg field "$json_field" '
        .hooks[$field] = (.hooks[$field] | map(
          if .matcher == "*" then
            .hooks += [{"type": "command", "command": $cmd}]
          else
            .
          end
        ))' "$config_file" > "${config_file}.tmp" && mv "${config_file}.tmp" "$config_file"
    else
      # Check if there are hooks without matcher field (like Stop hooks)
      local no_matcher_exists
      no_matcher_exists=$(jq --arg field "$json_field" '[.hooks[$field][] | select(has("matcher") | not)] | length' "$config_file")
      
      if [[ "$no_matcher_exists" != "0" ]]; then
        # Add to first entry without matcher
        jq --arg cmd "$hook_command" --arg field "$json_field" '
          .hooks[$field][0].hooks += [{"type": "command", "command": $cmd}]' "$config_file" > "${config_file}.tmp" && mv "${config_file}.tmp" "$config_file"
      else
        # No suitable existing entry, create new wildcard matcher entry
        jq --arg cmd "$hook_command" --arg field "$json_field" '
          .hooks[$field] += [{"matcher": "*", "hooks": [{"type": "command", "command": $cmd}]}]' "$config_file" > "${config_file}.tmp" && mv "${config_file}.tmp" "$config_file"
      fi
    fi
  else
    log_info "No existing ${hook_type} hooks found, creating new ones"
    
    # Create new hook structure
    # For Stop hooks, we match the existing pattern (no matcher field)
    if [[ "$hook_type" == "stop" ]]; then
      jq --arg cmd "$hook_command" --arg field "$json_field" '
        .hooks[$field] = [{"hooks": [{"type": "command", "command": $cmd}]}]' "$config_file" > "${config_file}.tmp" && mv "${config_file}.tmp" "$config_file"
    else
      # For Notification hooks, use matcher field
      jq --arg cmd "$hook_command" --arg field "$json_field" '
        .hooks[$field] = [{"matcher": "*", "hooks": [{"type": "command", "command": $cmd}]}]' "$config_file" > "${config_file}.tmp" && mv "${config_file}.tmp" "$config_file"
    fi
  fi
  
  # Validate the updated JSON
  if ! jq empty "$config_file" &> /dev/null; then
    log_error "Invalid JSON generated during ${hook_type} hook installation"
    return 2  # Error
  fi
  
  log_success "${json_field} hooks installed (existing hooks preserved)"
  return 0  # Changes made
}

# Verify installation
verify_installation() {
  log_info "Verifying installation..."
  
  if [[ ! -d "$SCRIPTS_TARGET_DIR" ]]; then
    log_error "Scripts directory not found after deployment"
    exit "$FAILURE"
  fi
  
  if [[ ! -f "$SCRIPTS_TARGET_DIR/hooks-config.json" ]]; then
    log_error "Configuration file not found after deployment"
    exit "$FAILURE"
  fi
  
  # Validate JSON structure
  if ! jq empty "$CLAUDE_CONFIG_FILE" &> /dev/null; then
    log_error "Generated settings.json contains invalid JSON"
    exit "$FAILURE"
  fi
  
  # Check if hooks are present in settings.json
  local has_notification_hooks
  local has_stop_hooks
  
  has_notification_hooks=$(jq -r '.hooks.Notification // empty' "$CLAUDE_CONFIG_FILE")
  has_stop_hooks=$(jq -r '.hooks.Stop // empty' "$CLAUDE_CONFIG_FILE")
  
  if [[ "$HOOK_CHOICE" == "notification" || "$HOOK_CHOICE" == "both" ]]; then
    if [[ -n "$has_notification_hooks" ]]; then
      log_success "Notification hooks verified"
    else
      log_error "Notification hooks not found in configuration"
      exit "$FAILURE"
    fi
  fi
  
  if [[ "$HOOK_CHOICE" == "stop" || "$HOOK_CHOICE" == "both" ]]; then
    if [[ -n "$has_stop_hooks" ]]; then
      log_success "Stop hooks verified"
    else
      log_error "Stop hooks not found in configuration"
      exit "$FAILURE"
    fi
  fi
  
  log_success "Installation verification completed"
}

# Display completion message
display_completion_message() {
  echo
  log_success "Claude Code Hooks deployment completed successfully!"
  echo
  log_info "Deployment Summary:"
  log_info "  - Service endpoint: $SERVICE_ENDPOINT"
  log_info "  - API key configured: $(mask_api_key "$SERVICE_API_KEY")"
  log_info "  - Deployment target: $DEPLOYMENT_TARGET ($SCRIPTS_TARGET_DIR)"
  log_info "  - Hooks installed: $HOOK_CHOICE"
  log_info "  - Configuration file: $CLAUDE_CONFIG_FILE"
  echo
  log_info "Your Claude Code hooks will now display actual project paths in notifications!"
  echo
  log_info "🔒 Security Reminder:"
  log_info "  - Your API key is stored locally in hooks-config.json"
  log_info "  - Keep your API key secure and do not share it"
  log_info "  - You can update your API key using: ./update-claude-hooks-config.sh --api-key NEW_KEY"
  echo
}

# Normalize input for yes/no responses (copied from setup-claude-hooks.sh)
normalize_input() {
  local input="$1"
  case "$input" in
    y|yes) echo "$YES";;
    n|no) echo "$NO";;
    *) echo "$input";;
  esac
}

# Build absolute script path for OS-specific hook commands
build_absolute_script_path() {
  local hook_type="$1"
  
  # Construct the full path to the hook script
  local script_path="$SCRIPTS_TARGET_DIR/hooks/${hook_type}-hook.sh"
  
  # For Windows, we might need to convert paths, but for now return as-is
  # since we're using absolute paths that should work across platforms
  echo "$script_path"
}

# Run main function
main "$@"
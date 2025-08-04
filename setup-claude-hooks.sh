#!/usr/bin/env bash

# Claude Code Hooks Setup Script
# This script configures Claude Code hooks to display actual project paths in notifications

# Common variables
YES="yes"
NO="no"
SUCCESS=0
FAILURE=1
MAX_RETRIES=3

# Color codes for logging
COLOR_RESET="\033[0m"      # Reset color
COLOR_BLUE="\033[34m"      # Blue for [INFO]
COLOR_GREEN="\033[32m"     # Green for [SUCCESS]
COLOR_RED="\033[31m"       # Red for [ERROR]
COLOR_YELLOW="\033[33m"    # Yellow for [WARNING]

# Configuration variables
SERVICE_ENDPOINT=""
HOOK_CHOICE=""
CLAUDE_CONFIG_DIR=""
CLAUDE_CONFIG_FILE=""

# Main function
main() {
  print_info_section
  detect_claude_config_path
  check_dependencies
  ensure_claude_config_dir
  prompt_for_service_endpoint
  prompt_for_hook_selection
  backup_existing_config
  install_selected_hooks
  verify_installation
  display_completion_message
}

# Detect Claude configuration path based on OS
detect_claude_config_path() {
  local os_type
  os_type=$(uname -s)
  
  case "$os_type" in
    Darwin)
      # macOS
      CLAUDE_CONFIG_DIR="$HOME/.claude"
      log "INFO" "Detected macOS - Using config path: $CLAUDE_CONFIG_DIR"
      ;;
    Linux)
      # Linux
      if [[ -n "$XDG_CONFIG_HOME" ]]; then
        CLAUDE_CONFIG_DIR="$XDG_CONFIG_HOME/claude"
      else
        CLAUDE_CONFIG_DIR="$HOME/.config/claude"
      fi
      log "INFO" "Detected Linux - Using config path: $CLAUDE_CONFIG_DIR"
      ;;
    CYGWIN*|MINGW32*|MSYS*|MINGW*)
      # Windows (Git Bash, MSYS2, etc.)
      if [[ -n "$APPDATA" ]]; then
        CLAUDE_CONFIG_DIR="$APPDATA/claude"
      else
        CLAUDE_CONFIG_DIR="$HOME/.claude"
      fi
      log "INFO" "Detected Windows - Using config path: $CLAUDE_CONFIG_DIR"
      ;;
    *)
      # Unknown OS - fallback to HOME/.claude
      CLAUDE_CONFIG_DIR="$HOME/.claude"
      log "WARNING" "Unknown OS ($os_type) - Using fallback config path: $CLAUDE_CONFIG_DIR"
      ;;
  esac
  
  CLAUDE_CONFIG_FILE="$CLAUDE_CONFIG_DIR/settings.json"
  
  # Check if config directory exists, if not, look for alternative locations
  if [[ ! -d "$CLAUDE_CONFIG_DIR" ]]; then
    log "INFO" "Primary config directory not found, searching for existing Claude configuration..."
    
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
        log "SUCCESS" "Found existing Claude configuration at: $CLAUDE_CONFIG_DIR"
        return
      fi
    done
    
    log "INFO" "No existing Claude configuration found. Will create new one at: $CLAUDE_CONFIG_DIR"
  else
    log "SUCCESS" "Claude configuration directory found: $CLAUDE_CONFIG_DIR"
  fi
}

# Display the welcome information
print_info_section() {
  echo -e "${COLOR_BLUE}INFO: Welcome to the Claude Code Hooks Setup Script!${COLOR_RESET}"
  echo
  echo -e "${COLOR_BLUE}This script configures Claude Code hooks to display actual project paths in notifications by:${COLOR_RESET}"
  echo -e "${COLOR_BLUE}  - Extracting real project paths from Claude session files${COLOR_RESET}"
  echo -e "${COLOR_BLUE}  - Adding hooks to your ~/.claude/settings.json configuration${COLOR_RESET}"
  echo -e "${COLOR_BLUE}  - Sending project context to your Claude Code Hooks service${COLOR_RESET}"
  echo -e "${COLOR_BLUE}  - Creating backups of your existing configuration${COLOR_RESET}"
  echo
  echo -e "${COLOR_BLUE}Before starting, this script will:${COLOR_RESET}"
  echo -e "${COLOR_BLUE}  - Check for required dependencies (jq, curl, bash)${COLOR_RESET}"
  echo -e "${COLOR_BLUE}  - Allow you to configure your service endpoint${COLOR_RESET}"
  echo -e "${COLOR_BLUE}  - Let you choose which hooks to install${COLOR_RESET}"
  echo
  echo -e "${COLOR_BLUE}You can respond to prompts with:${COLOR_RESET}"
  echo -e "${COLOR_BLUE}  - 'y' or 'yes' for confirmation${COLOR_RESET}"
  echo -e "${COLOR_BLUE}  - 'n' or 'no' to decline${COLOR_RESET}"
  echo
}

# Check if required dependencies are installed
check_dependencies() {
  log "INFO" "Checking required dependencies..."
  
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
    log "ERROR" "Missing required dependencies: ${missing_deps[*]}"
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
        log "INFO" "Rechecking dependencies..."
        check_dependencies  # Recursive call to recheck
        return
      elif [[ "$continue_choice" == "$NO" ]]; then
        log "INFO" "Setup cancelled by user. Please install dependencies and run the script again."
        exit "$SUCCESS"
      else
        log "ERROR" "Invalid input. Please enter 'y' for yes or 'n' for no."
        ((retries++))
        if [[ $retries -ge $MAX_RETRIES ]]; then
          log "ERROR" "Maximum retries reached. Exiting..."
          exit "$FAILURE"
        fi
      fi
    done
  fi

  log "SUCCESS" "All dependencies are available (jq, curl, bash)."
}

# Ensure Claude configuration directory exists
ensure_claude_config_dir() {
  if [[ ! -d "$CLAUDE_CONFIG_DIR" ]]; then
    log "INFO" "Creating Claude configuration directory: $CLAUDE_CONFIG_DIR"
    mkdir -p "$CLAUDE_CONFIG_DIR"
  fi
}

# Prompt for service endpoint configuration
prompt_for_service_endpoint() {
  retries=0
  while true; do
    echo "Where is your Claude Code Hooks service deployed?"
    echo "1) Local deployment (http://localhost:8085)"
    echo "2) Remote deployment (custom URL)"
    read -rp "Please enter your choice [1/2]: " deployment_type
    
    case "$deployment_type" in
      1|local)
        SERVICE_ENDPOINT="http://localhost:8085"
        log "INFO" "Using local deployment: $SERVICE_ENDPOINT"
        break
        ;;
      2|remote)
        read -rp "Enter the remote service URL (e.g., https://hooks.example.com:8080): " SERVICE_ENDPOINT
        if [[ -n "$SERVICE_ENDPOINT" ]]; then
          log "INFO" "Using remote deployment: $SERVICE_ENDPOINT"
          break
        else
          log "ERROR" "Service endpoint cannot be empty."
        fi
        ;;
      *)
        log "ERROR" "Invalid choice. Please select '1' for local or '2' for remote."
        ((retries++))
        if [[ $retries -ge $MAX_RETRIES ]]; then
          log "ERROR" "Maximum retries reached. Exiting..."
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
        log "INFO" "Installing notification hooks only"
        break
        ;;
      2|stop)
        HOOK_CHOICE="stop"
        log "INFO" "Installing stop hooks only"
        break
        ;;
      3|both)
        HOOK_CHOICE="both"
        log "INFO" "Installing both notification and stop hooks"
        break
        ;;
      *)
        log "ERROR" "Invalid choice. Please select '1', '2', or '3'."
        ((retries++))
        if [[ $retries -ge $MAX_RETRIES ]]; then
          log "ERROR" "Maximum retries reached. Exiting..."
          exit "$FAILURE"
        fi
        ;;
    esac
  done
}

# Backup existing configuration
backup_existing_config() {
  if [[ -f "$CLAUDE_CONFIG_FILE" ]]; then
    local backup_file
    backup_file="$CLAUDE_CONFIG_FILE.backup.$(date +%Y%m%d_%H%M%S)"
    cp "$CLAUDE_CONFIG_FILE" "$backup_file"
    log "SUCCESS" "Backup created: $backup_file"
  else
    log "INFO" "No existing configuration found. Creating new settings.json file."
  fi
}

# Install selected hooks
install_selected_hooks() {
  log "INFO" "Installing selected hooks (preserving existing hooks)..."
  
  # Create base configuration structure
  if [[ ! -f "$CLAUDE_CONFIG_FILE" ]]; then
    echo '{"model": "sonnet", "hooks": {}}' | jq . > "$CLAUDE_CONFIG_FILE"
    log "INFO" "Creating new configuration"
  else
    log "INFO" "Loading existing configuration"
  fi
  
  # Validate existing JSON
  if ! jq empty "$CLAUDE_CONFIG_FILE" &> /dev/null; then
    log "ERROR" "Invalid JSON in existing configuration file"
    exit "$FAILURE"
  fi
  
  # Ensure hooks object exists
  jq '.hooks //= {}' "$CLAUDE_CONFIG_FILE" > "${CLAUDE_CONFIG_FILE}.tmp" && mv "${CLAUDE_CONFIG_FILE}.tmp" "$CLAUDE_CONFIG_FILE"
  
  # Track if any changes were made
  local changes_made=false
  
  # Install notification hooks (preserve existing)
  if [[ "$HOOK_CHOICE" == "notification" || "$HOOK_CHOICE" == "both" ]]; then
    install_hook_type "notification" "$CLAUDE_CONFIG_FILE"
    local exit_code=$?
    if [[ $exit_code -eq 0 ]]; then
      changes_made=true
    elif [[ $exit_code -eq 2 ]]; then
      exit "$FAILURE"
    fi
  fi
  
  # Install stop hooks (preserve existing)
  if [[ "$HOOK_CHOICE" == "stop" || "$HOOK_CHOICE" == "both" ]]; then
    install_hook_type "stop" "$CLAUDE_CONFIG_FILE"
    local exit_code=$?
    if [[ $exit_code -eq 0 ]]; then
      changes_made=true
    elif [[ $exit_code -eq 2 ]]; then
      exit "$FAILURE"
    fi
  fi
  
  if [[ "$changes_made" == true ]]; then
    log "SUCCESS" "Configuration updated successfully: $CLAUDE_CONFIG_FILE"
  else
    log "INFO" "No changes made to configuration (all hooks already exist): $CLAUDE_CONFIG_FILE"
  fi
}

# Install a specific hook type (notification or stop)
install_hook_type() {
  local hook_type="$1"
  local config_file="$2"
  
  # Capitalize first letter for JSON field name  
  local json_field
  json_field="$(echo "${hook_type:0:1}" | tr '[:lower:]' '[:upper:]')${hook_type:1}"
  
  # Create the hook command  
  local hook_command="bash -c 'payload=\$(cat -); session_id=\$(echo \"\$payload\" | jq -r \".session_id\"); transcript_path=\$(echo \"\$payload\" | jq -r \".transcript_path\"); cwd=\$(grep -o \"\\\"cwd\\\":\\\"[^\\\"]*\\\"\" \"\$transcript_path\" | head -n 1 | cut -d\":\" -f2 | tr -d \"\\\"\"); curl -s -X POST ${SERVICE_ENDPOINT}/api/v1/claude-code/hooks/${hook_type}/event -H \"Content-Type: application/json\" -H \"X-Context-Work-Directory: \$cwd\" -d \"\$payload\"'"
  
  # Check if our hook already exists by looking for the specific API endpoint pattern
  local hook_already_exists
  local endpoint_path="/api/v1/claude-code/hooks/${hook_type}/event"
  hook_already_exists=$(jq --arg endpoint_path "$endpoint_path" --arg field "$json_field" '
    [.hooks[$field][]?.hooks[]? | select(.type == "command" and (.command | contains($endpoint_path)))] | length' "$config_file")
  
  if [[ "$hook_already_exists" != "0" ]]; then
    log "WARNING" "Claude Code ${hook_type} hook already exists. Skipping installation."
    return 1  # No changes made
  fi
  
  log "INFO" "Installing ${hook_type} hook"
  
  # Check if hooks for this type already exist
  local existing_hooks
  existing_hooks=$(jq --arg field "$json_field" '.hooks[$field] // []' "$config_file")
  
  if [[ "$existing_hooks" != "[]" ]]; then
    log "INFO" "Existing ${hook_type} hooks found, adding our hook to existing ones"
    
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
      # Check if there are hooks without matcher field (like Stop hooks in user's example)
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
    log "INFO" "No existing ${hook_type} hooks found, creating new ones"
    
    # Create new hook structure
    # For Stop hooks, we match the user's example (no matcher field)
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
    log "ERROR" "Invalid JSON generated during ${hook_type} hook installation"
    return 2  # Error
  fi
  
  log "SUCCESS" "${json_field} hooks installed (existing hooks preserved)"
  return 0  # Changes made
}

# Verify installation
verify_installation() {
  log "INFO" "Verifying installation..."
  
  if [[ ! -f "$CLAUDE_CONFIG_FILE" ]]; then
    log "ERROR" "Configuration file not found after installation"
    exit "$FAILURE"
  fi
  
  # Validate JSON structure
  if ! jq empty "$CLAUDE_CONFIG_FILE" &> /dev/null; then
    log "ERROR" "Generated configuration file contains invalid JSON"
    exit "$FAILURE"
  fi
  
  # Check if hooks are present
  local has_notification_hooks
  local has_stop_hooks
  
  has_notification_hooks=$(jq -r '.hooks.Notification // empty' "$CLAUDE_CONFIG_FILE")
  has_stop_hooks=$(jq -r '.hooks.Stop // empty' "$CLAUDE_CONFIG_FILE")
  
  if [[ "$HOOK_CHOICE" == "notification" || "$HOOK_CHOICE" == "both" ]]; then
    if [[ -n "$has_notification_hooks" ]]; then
      log "SUCCESS" "Notification hooks verified"
    else
      log "ERROR" "Notification hooks not found in configuration"
      exit "$FAILURE"
    fi
  fi
  
  if [[ "$HOOK_CHOICE" == "stop" || "$HOOK_CHOICE" == "both" ]]; then
    if [[ -n "$has_stop_hooks" ]]; then
      log "SUCCESS" "Stop hooks verified"
    else
      log "ERROR" "Stop hooks not found in configuration"
      exit "$FAILURE"
    fi
  fi
  
  log "SUCCESS" "Installation verification completed"
}

# Display completion message
display_completion_message() {
  echo
  echo -e "${COLOR_GREEN}[SUCCESS] Claude Code Hooks setup completed successfully!${COLOR_RESET}"
  echo
  echo -e "${COLOR_BLUE}Configuration Summary:${COLOR_RESET}"
  echo -e "${COLOR_BLUE}  - Service endpoint: $SERVICE_ENDPOINT${COLOR_RESET}"
  echo -e "${COLOR_BLUE}  - Hooks installed: $HOOK_CHOICE${COLOR_RESET}"
  echo -e "${COLOR_BLUE}  - Configuration file: $CLAUDE_CONFIG_FILE${COLOR_RESET}"
  echo
  echo -e "${COLOR_BLUE}Setup complete! Your Claude Code hooks will now display actual project paths.${COLOR_RESET}"
  echo
}

# Normalize input for yes/no responses
normalize_input() {
  local input="$1"
  case "$input" in
    y|yes) echo "$YES";;
    n|no) echo "$NO";;
    *) echo "$input";;
  esac
}

# Logging helper with color and improved format
log() {
  local prefix="$1"
  local message="$2"
  local color=""

  # Set color for specific log types
  case "$prefix" in
    "INFO") color="$COLOR_BLUE";;
    "SUCCESS") color="$COLOR_GREEN";;
    "ERROR") color="$COLOR_RED";;
    "WARNING") color="$COLOR_YELLOW";;
  esac

  printf "${color}[%s] [%s]: %s${COLOR_RESET}\n" "$prefix" "$(date '+%Y-%m-%d %H:%M:%S')" "$message"
}

# Run main function
main "$@"
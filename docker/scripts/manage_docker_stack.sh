#!/usr/bin/env bash

# Common variables
DOCKER_COMPOSE_UP="up"
DOCKER_COMPOSE_DOWN="down"
YES="yes"
NO="no"
SUCCESS=0
FAILURE=1
CONTINUE_LEVEL=2
MAX_RETRIES=3

# Color codes for logging
COLOR_RESET="\033[0m"      # Reset color
COLOR_BLUE="\033[34m"      # Blue for [INFO]
COLOR_GREEN="\033[32m"     # Green for [SUCCESS]
COLOR_RED="\033[31m"       # Red for [ERROR]
COLOR_YELLOW="\033[33m"    # Yellow for [WARNING]

# Main function
main() {
  print_info_section
  check_docker_installed

  # Prompt for project name (new)
  prompt_for_project_name

  prompt_for_env_dependency

  if [[ "$use_env_file" == "$YES" ]]; then
    ENV_FILE=$(prompt_for_file_path "environment file")
    DOCKER_COMPOSE_FILE=$(prompt_for_file_path "Docker Compose file")
  else
    DOCKER_COMPOSE_FILE=$(prompt_for_file_path "Docker Compose file")
  fi

  prompt_user_choice

  # Ask if the user wants to include the app service (new)
  if [[ "$action" == "$DOCKER_COMPOSE_UP" ]]; then
    prompt_for_app_inclusion
    start_docker_compose
  elif [[ "$action" == "$DOCKER_COMPOSE_DOWN" ]]; then
    stop_docker_compose
    prompt_cleanup_volumes
  fi
}

# Prompt for project name (new function)
prompt_for_project_name() {
  while true; do
    read -rp "Enter the project name: " PROJECT_NAME
    if [[ -z "$PROJECT_NAME" ]]; then
      log "ERROR" "Project name cannot be empty. Please provide a valid project name."
    else
      log "INFO" "Using project name: $PROJECT_NAME"
      break
    fi
  done
}

# Prompt for app inclusion (new function)
prompt_for_app_inclusion() {
  retries=0
  while true; do
    read -rp "Do you want to include the application service in Docker? (If no, only infrastructure services will be started) yes/no: " include_app
    include_app=$(normalize_input "$include_app")

    if [[ "$include_app" == "$YES" || "$include_app" == "$NO" ]]; then
      break
    else
      log "ERROR" "Invalid input. Please enter one of the following: 'yes', 'no', 'y', 'n'."
      ((retries++))
      if [[ $retries -ge $MAX_RETRIES ]]; then
        log "ERROR" "Maximum retries reached. Defaulting to infrastructure only..."
        include_app="$NO"
        break
      fi
    fi
  done

  if [[ "$include_app" == "$YES" ]]; then
    log "INFO" "Application service will be included in orchestration"
  else
    log "INFO" "Only infrastructure services will be started (application will run locally)"
  fi
}

# Display the welcome information
print_info_section() {
  echo -e "${COLOR_BLUE}INFO: Welcome to the Docker Compose Manager!${COLOR_RESET}"
  echo
  echo -e "${COLOR_BLUE}This script helps you manage your Docker Compose stack by:${COLOR_RESET}"
  echo -e "${COLOR_BLUE}  - Starting or stopping your Docker Compose stack.${COLOR_RESET}"
  echo -e "${COLOR_BLUE}  - Using an optional environment file to configure services.${COLOR_RESET}"
  echo -e "${COLOR_BLUE}  - Letting you choose whether to run the application in Docker or locally.${COLOR_RESET}"
  echo -e "${COLOR_BLUE}  - Prompting to clean up Docker volumes after stopping the stack.${COLOR_RESET}"
  echo
  echo -e "${COLOR_BLUE}You can respond to prompts with the following:${COLOR_RESET}"
  echo
  echo -e "${COLOR_BLUE}  - 'y' or 'yes' for confirmation${COLOR_RESET}"
  echo -e "${COLOR_BLUE}  - 'n' or 'no' to decline${COLOR_RESET}"
  echo
}

# Check if Docker and Docker Compose are installed
check_docker_installed() {
  if ! command -v docker &> /dev/null; then
    log "ERROR" "Docker is not installed or not available in the PATH."
    exit "$FAILURE"
  fi

  if ! docker compose version &> /dev/null; then
    log "ERROR" "Docker Compose is not available as a Docker subcommand."
    exit "$FAILURE"
  fi
}

# Prompt the user to confirm if they use an environment variable file
prompt_for_env_dependency() {
  retries=0
  while true; do
    read -rp "Does your Docker Compose depend on an environment variable file? yes/no: " use_env_file
    use_env_file=$(normalize_input "$use_env_file")

    if [[ "$use_env_file" == "$YES" || "$use_env_file" == "$NO" ]]; then
      break
    else
      log "ERROR" "Invalid input. Please enter one of the following: 'yes', 'no', 'y', 'n'."
      ((retries++))
      if [[ $retries -ge $MAX_RETRIES ]]; then
        log "ERROR" "Maximum retries reached. Exiting..."
        exit "$FAILURE"
      fi
    fi
  done
}

# Prompt the user for a file path
prompt_for_file_path() {
  local file_type="$1"
  local file_path

  while true; do
    read -rp "Please enter the path to the ${file_type}: " file_path
    if [[ -f "$file_path" ]]; then
      echo "$file_path"
      return
    else
      log "ERROR" "The specified ${file_type} does not exist: $file_path"
    fi
  done
}

# Prompt the user for the action they want to take (up or down)
prompt_user_choice() {
  while true; do
    echo "What would you like to do with the Docker Compose stack?"
    echo "($DOCKER_COMPOSE_UP) Start the stack (docker compose up)"
    echo "($DOCKER_COMPOSE_DOWN) Stop the stack (docker compose down)"
    read -rp "Please enter your choice [$DOCKER_COMPOSE_UP/$DOCKER_COMPOSE_DOWN]: " action

    if [[ "$action" == "$DOCKER_COMPOSE_UP" || "$action" == "$DOCKER_COMPOSE_DOWN" ]]; then
      break
    else
      log "ERROR" "Invalid choice. Please select either '$DOCKER_COMPOSE_UP' or '$DOCKER_COMPOSE_DOWN'."
    fi
  done
}

# Start the Docker Compose stack (modified to handle app profile)
start_docker_compose() {
  local profile_flag=""

  # Add the app profile if the user wants to include the application
  if [[ "$include_app" == "$YES" ]]; then
    profile_flag="--profile app"
    log "INFO" "Starting with application profile enabled"
  else
    log "INFO" "Starting infrastructure services only"
  fi

  if [[ "$use_env_file" == "$YES" ]]; then
    if docker compose --env-file "$ENV_FILE" -f "$DOCKER_COMPOSE_FILE" $profile_flag -p "$PROJECT_NAME" up -d; then
      log "SUCCESS" "Docker Compose stack started."

      if [[ "$include_app" == "$NO" ]]; then
        display_local_run_instructions
      fi
    else
      log "ERROR" "Failed to start Docker Compose stack."
      exit "$FAILURE"
    fi
  else
    if docker compose -f "$DOCKER_COMPOSE_FILE" $profile_flag -p "$PROJECT_NAME" up -d; then
      log "SUCCESS" "Docker Compose stack started."

      if [[ "$include_app" == "$NO" ]]; then
        display_local_run_instructions
      fi
    else
      log "ERROR" "Failed to start Docker Compose stack."
      exit "$FAILURE"
    fi
  fi
}

# Display instructions for running the application locally (new function)
display_local_run_instructions() {
  echo -e "${COLOR_YELLOW}[NOTE] [$(date '+%Y-%m-%d %H:%M:%S')]: To run the application locally:${COLOR_RESET}"
  echo -e "${COLOR_YELLOW}1. Make sure your application is configured to connect to:${COLOR_RESET}"
  echo -e "${COLOR_YELLOW}   - MongoDB: localhost:27018${COLOR_RESET}"
  echo -e "${COLOR_YELLOW}   - PostgreSQL: localhost:5433${COLOR_RESET}"
  echo -e "${COLOR_YELLOW}   - Keycloak: localhost:8080 (HTTP)${COLOR_RESET}"
  echo -e "${COLOR_YELLOW}2. Use the local.env environment file:${COLOR_RESET}"
  echo -e "${COLOR_YELLOW}   --env-file .env/local.env${COLOR_RESET}"
  echo -e "${COLOR_YELLOW}3. Run your application with:${COLOR_RESET}"
  echo -e "${COLOR_YELLOW}   ./gradlew bootRun${COLOR_RESET}"
  echo -e "${COLOR_YELLOW}   (or run from IDE with local profile)${COLOR_RESET}"
  echo
}

# Stop the Docker Compose stack
stop_docker_compose() {
  if [[ "$use_env_file" == "$YES" ]]; then
    if docker compose --env-file "$ENV_FILE" -f "$DOCKER_COMPOSE_FILE" -p "$PROJECT_NAME" down; then
      log "SUCCESS" "Docker Compose stack stopped."
    else
      log "ERROR" "Failed to stop Docker Compose stack."
      exit "$FAILURE"
    fi
  else
    if docker compose -f "$DOCKER_COMPOSE_FILE" -p "$PROJECT_NAME" down; then
      log "SUCCESS" "Docker Compose stack stopped."
    else
      log "ERROR" "Failed to stop Docker Compose stack."
      exit "$FAILURE"
    fi
  fi
}

# Prompt the user to clean up Docker volumes after stopping the stack
prompt_cleanup_volumes() {
  retries=0
  while true; do
    read -rp "Do you want to clean up the Docker volumes? yes/no: " cleanup_choice
    cleanup_choice=$(normalize_input "$cleanup_choice")

    if [[ "$cleanup_choice" == "$YES" ]]; then
      cleanup_volumes
      break
    elif [[ "$cleanup_choice" == "$NO" ]]; then
      log "INFO" "Skipping volume cleanup."
      break
    else
      log "ERROR" "Invalid input. Please enter one of the following: 'yes', 'no', 'y', 'n'."
      ((retries++))
      if [[ $retries -ge $MAX_RETRIES ]]; then
        log "ERROR" "Maximum retries reached. Skipping volume cleanup..."
        break
      fi
    fi
  done
}

# Clean up Docker volumes
cleanup_volumes() {
  volumes=()
  while IFS= read -r volume; do
    volumes+=("$volume")
  done < <(docker volume ls -q --filter "name=$PROJECT_NAME")

  if [[ ${#volumes[@]} -eq $SUCCESS ]]; then
    log "INFO" "No volumes found for project $PROJECT_NAME."
    return
  fi

  echo "Found the following Docker volumes associated with $PROJECT_NAME:"
  for i in "${!volumes[@]}"; do
    echo "$((i+1))) ${volumes[i]}"
  done

  while true; do
    read -rp "Do you want to remove all volumes (all) or specific volumes by index (e.g., 1 3 5)? [all/some]: " remove_choice

    if [[ "$remove_choice" == "all" ]]; then
      if docker volume rm "${volumes[@]}"; then
        local count
        count="${#volumes[@]}"
        local volume_list
        volume_list=$(IFS=, ; echo "${volumes[*]}")
        log "SUCCESS" "All $count Docker volumes associated with $PROJECT_NAME were cleaned up: $volume_list"
      else
        log "ERROR" "Failed to clean up Docker volumes."
      fi
      break
    elif [[ "$remove_choice" == "some" ]]; then
      read -rp "Please enter the indexes of the volumes to remove (separated by spaces): " volume_indexes
      selected_volumes=()
      for index in $volume_indexes; do
        if [[ $index -gt $SUCCESS && $index -le ${#volumes[@]} ]]; then
          selected_volumes+=("${volumes[$((index-1))]}")
        else
          log "ERROR" "Invalid index: $index. Please try again."
          continue $CONTINUE_LEVEL
        fi
      done

      if docker volume rm "${selected_volumes[@]}"; then
        local count
        count="${#selected_volumes[@]}"
        local volume_list
        volume_list=$(IFS=, ; echo "${selected_volumes[*]}")
        log "SUCCESS" "$count Selected Docker volumes were cleaned up: $volume_list"
      else
        log "ERROR" "Failed to clean up selected Docker volumes."
      fi
      break
    else
      log "ERROR" "Invalid choice. Please enter 'all' or 'some'."
    fi
  done
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
  if [[ "$prefix" == "INFO" ]]; then
    color="$COLOR_BLUE"
  elif [[ "$prefix" == "SUCCESS" ]]; then
    color="$COLOR_GREEN"
  elif [[ "$prefix" == "ERROR" ]]; then
    color="$COLOR_RED"
  elif [[ "$prefix" == "WARNING" ]]; then
    color="$COLOR_YELLOW"
  fi

  printf "${color}[%s] [%s]: %s${COLOR_RESET}\n" "$prefix" "$(date '+%Y-%m-%d %H:%M:%S')" "$message"
}

main "$@"
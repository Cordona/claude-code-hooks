#!/usr/bin/env bash

# Keycloak HTTP Setup Script for Claude Code Hooks
# Automatically configures a fresh Keycloak instance with HTTP (no SSL complexity)

# Common variables
SUCCESS=0
FAILURE=1
MAX_RETRIES=3
HEALTH_CHECK_RETRIES=10
HEALTH_CHECK_DELAY=5

# Color codes for logging
COLOR_RESET="\033[0m"      # Reset color
COLOR_BLUE="\033[34m"      # Blue for [INFO]
COLOR_GREEN="\033[32m"     # Green for [SUCCESS]
COLOR_RED="\033[31m"       # Red for [ERROR]
COLOR_YELLOW="\033[33m"    # Yellow for [WARNING]
COLOR_CYAN="\033[36m"      # Cyan for [STEP]

# Keycloak configuration (HTTP - Simple!)
KEYCLOAK_BASE_URL="http://localhost:8080"
REALM_NAME="claude-code-hooks"
CLIENT_ID="claude-code-hooks-frontend"
ADMIN_REALM="master"

# OAuth credentials (embedded)
GOOGLE_CLIENT_ID="463730219120-ad9vmk2njucp4u9pscito6pqggrkt9kh.apps.googleusercontent.com"
GOOGLE_CLIENT_SECRET="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"
GITHUB_CLIENT_ID="Ov23liZrYCeFasBqfiKq" 
GITHUB_CLIENT_SECRET="92cd3d14163e336fdb22f8565d13f56e76b8a1c8"

# Environment variables (will be read from Docker environment)
KEYCLOAK_ADMIN_USER=""
KEYCLOAK_ADMIN_PASSWORD=""
ACCESS_TOKEN=""

# Main function
main() {
    print_welcome_banner
    check_prerequisites
    load_environment_variables
    disable_ssl_requirements
    authenticate_admin
    setup_realm
    setup_client
    setup_identity_providers
    configure_account_linking
    validate_setup
    print_completion_summary
}

# Display welcome banner
print_welcome_banner() {
    echo
    echo -e "${COLOR_CYAN}╔══════════════════════════════════════════════════════════════╗${COLOR_RESET}"
    echo -e "${COLOR_CYAN}║              Keycloak HTTP Setup for Claude Code Hooks              ║${COLOR_RESET}"
    echo -e "${COLOR_CYAN}╚══════════════════════════════════════════════════════════════╝${COLOR_RESET}"
    echo
    echo -e "${COLOR_BLUE}This script will configure a fresh Keycloak instance with:${COLOR_RESET}"
    echo -e "${COLOR_BLUE}  • HTTP configuration (no SSL complexity)${COLOR_RESET}"
    echo -e "${COLOR_BLUE}  • Keycloak 26.0 HTTPS requirement bypass (kcadm.sh)${COLOR_RESET}"
    echo -e "${COLOR_BLUE}  • Claude Code Hooks realm and client${COLOR_RESET}"
    echo -e "${COLOR_BLUE}  • Google and GitHub identity providers (with real credentials)${COLOR_RESET}"
    echo -e "${COLOR_BLUE}  • Cross-email account linking${COLOR_RESET}"
    echo -e "${COLOR_BLUE}  • Simple JWT configuration (no DevJwtConfig needed!)${COLOR_RESET}"
    echo
    echo -e "${COLOR_GREEN}🎯 Benefits of HTTP setup:${COLOR_RESET}"
    echo -e "${COLOR_GREEN}  • No SSL certificate issues${COLOR_RESET}"
    echo -e "${COLOR_GREEN}  • No DevJwtConfig needed${COLOR_RESET}"
    echo -e "${COLOR_GREEN}  • No Frontend URL drama${COLOR_RESET}"
    echo -e "${COLOR_GREEN}  • Simple OAuth configuration${COLOR_RESET}"
    echo
}

# Check all prerequisites
check_prerequisites() {
    log "STEP" "1/8 - Checking prerequisites..."
    
    # Check if jq is installed
    if ! command -v jq &> /dev/null; then
        log "ERROR" "jq is required but not installed. Please install jq: brew install jq"
        exit $FAILURE
    fi
    
    # Check if curl is available
    if ! command -v curl &> /dev/null; then
        log "ERROR" "curl is required but not installed."
        exit $FAILURE
    fi
    
    # Check if Keycloak is running (HTTP)
    check_keycloak_health
    
    log "SUCCESS" "All prerequisites satisfied"
}

# Check Keycloak health with retries (HTTP)
check_keycloak_health() {
    log "INFO" "Checking Keycloak health status (HTTP)..."
    
    local retries=0
    while [ $retries -lt $HEALTH_CHECK_RETRIES ]; do
        # Keycloak HTTP health endpoint
        if curl -s -f "http://localhost:9001/health/ready" > /dev/null 2>&1; then
            log "SUCCESS" "Keycloak is healthy and ready (HTTP)"
            return $SUCCESS
        fi
        
        retries=$((retries + 1))
        log "WARNING" "Keycloak not ready (attempt $retries/$HEALTH_CHECK_RETRIES). Retrying in ${HEALTH_CHECK_DELAY}s..."
        sleep $HEALTH_CHECK_DELAY
    done
    
    log "ERROR" "Keycloak is not running or not healthy. Please start Keycloak with HTTP setup:"
    log "INFO" "Run: docker compose -f docker-compose-http.yml --env-file .env/http.env up"
    exit $FAILURE
}

# Load environment variables from Docker
load_environment_variables() {
    log "STEP" "2/8 - Loading environment configuration..."
    
    # Try to get admin credentials from running container
    if docker ps | grep -q "keycloak-auth-server-http"; then
        KEYCLOAK_ADMIN_USER=$(docker exec keycloak-auth-server-http printenv KEYCLOAK_ADMIN 2>/dev/null || echo "admin")
        KEYCLOAK_ADMIN_PASSWORD=$(docker exec keycloak-auth-server-http printenv KEYCLOAK_ADMIN_PASSWORD 2>/dev/null || echo "keycloak_admin_password")
    else
        # Fallback to default values
        KEYCLOAK_ADMIN_USER="admin"
        KEYCLOAK_ADMIN_PASSWORD="keycloak_admin_password"
    fi
    
    log "INFO" "Using admin user: $KEYCLOAK_ADMIN_USER"
    log "INFO" "Keycloak HTTP URL: $KEYCLOAK_BASE_URL"
    log "SUCCESS" "Environment configuration loaded"
}

# Disable SSL requirements using kcadm.sh (Keycloak 26.0 requirement)
disable_ssl_requirements() {
    log "STEP" "3/8 - Disabling SSL requirements using Keycloak admin CLI..."
    
    # Wait for Keycloak to be fully ready before running kcadm commands
    log "INFO" "Waiting for Keycloak to be fully ready for admin operations..."
    sleep 10
    
    # Execute kcadm.sh commands inside the Docker container
    log "INFO" "Configuring Keycloak admin credentials..."
    
    # Configure kcadm.sh credentials
    local kcadm_config_result
    kcadm_config_result=$(docker exec keycloak-auth-server-http /opt/keycloak/bin/kcadm.sh config credentials \
        --server http://localhost:8080 \
        --realm master \
        --user "$KEYCLOAK_ADMIN_USER" \
        --password "$KEYCLOAK_ADMIN_PASSWORD" 2>&1)
    
    if [ $? -ne 0 ]; then
        log "ERROR" "Failed to configure kcadm.sh credentials:"
        log "ERROR" "$kcadm_config_result"
        exit $FAILURE
    fi
    
    log "SUCCESS" "kcadm.sh credentials configured"
    
    # Disable SSL requirements for master realm
    log "INFO" "Disabling SSL requirements for master realm..."
    
    local ssl_disable_result
    ssl_disable_result=$(docker exec keycloak-auth-server-http /opt/keycloak/bin/kcadm.sh update realms/master \
        -s sslRequired=NONE 2>&1)
    
    if [ $? -ne 0 ]; then
        log "WARNING" "Failed to disable SSL for master realm (may already be disabled):"
        log "INFO" "$ssl_disable_result"
    else
        log "SUCCESS" "SSL requirements disabled for master realm"
    fi
    
    # Also ensure our target realm will have SSL disabled
    log "INFO" "Pre-configuring SSL settings for target realm..."
    
    # This will be applied when we create the realm, but we set it here for safety
    log "SUCCESS" "SSL configuration prepared for HTTP-only operation"
}

# Authenticate with Keycloak admin API (HTTP)
authenticate_admin() {
    log "STEP" "4/8 - Authenticating with Keycloak admin API (HTTP)..."
    
    local response
    response=$(curl -s -X POST "$KEYCLOAK_BASE_URL/realms/$ADMIN_REALM/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "username=$KEYCLOAK_ADMIN_USER" \
        -d "password=$KEYCLOAK_ADMIN_PASSWORD" \
        -d "grant_type=password" \
        -d "client_id=admin-cli")
    
    if [ $? -ne 0 ] || [ -z "$response" ]; then
        log "ERROR" "Failed to connect to Keycloak authentication endpoint"
        exit $FAILURE
    fi
    
    ACCESS_TOKEN=$(echo "$response" | jq -r '.access_token')
    
    if [ "$ACCESS_TOKEN" = "null" ] || [ -z "$ACCESS_TOKEN" ]; then
        log "ERROR" "Failed to authenticate with Keycloak. Check admin credentials."
        log "ERROR" "Response: $response"
        exit $FAILURE
    fi
    
    log "SUCCESS" "Successfully authenticated with Keycloak admin API"
}

# Create and configure the realm
setup_realm() {
    log "STEP" "5/8 - Setting up Claude Code Hooks realm..."
    
    # Check if realm already exists
    local existing_realm
    existing_realm=$(curl -s -H "Authorization: Bearer $ACCESS_TOKEN" \
        "$KEYCLOAK_BASE_URL/admin/realms/$REALM_NAME" 2>/dev/null)
    
    if [ $? -eq 0 ] && echo "$existing_realm" | jq -e '.realm' > /dev/null 2>&1; then
        log "WARNING" "Realm '$REALM_NAME' already exists. Skipping creation."
        return $SUCCESS
    fi
    
    # Create realm
    local realm_config='{
        "realm": "'$REALM_NAME'",
        "displayName": "Claude Code Hooks",
        "enabled": true,
        "registrationAllowed": false,
        "loginWithEmailAllowed": true,
        "duplicateEmailsAllowed": false,
        "rememberMe": true,
        "verifyEmail": false,
        "loginTheme": "keycloak",
        "accountTheme": "keycloak",
        "adminTheme": "keycloak",
        "emailTheme": "keycloak",
        "accessTokenLifespan": 3600,
        "refreshTokenMaxReuse": 0,
        "accessCodeLifespan": 60,
        "accessCodeLifespanUserAction": 300,
        "accessCodeLifespanLogin": 1800,
        "sslRequired": "none",
        "attributes": {
            "frontendUrl": "http://localhost:8080"
        }
    }'
    
    local response
    response=$(curl -s -w "%{http_code}" -X POST "$KEYCLOAK_BASE_URL/admin/realms" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$realm_config")
    
    local http_code="${response: -3}"
    if [ "$http_code" = "201" ]; then
        log "SUCCESS" "Realm '$REALM_NAME' created successfully"
    else
        log "ERROR" "Failed to create realm. HTTP code: $http_code"
        log "ERROR" "Response: ${response%???}"
        exit $FAILURE
    fi
}

# Create and configure the application client
setup_client() {
    log "STEP" "6/8 - Setting up application client..."
    
    # Check if client already exists
    local existing_clients
    existing_clients=$(curl -s -H "Authorization: Bearer $ACCESS_TOKEN" \
        "$KEYCLOAK_BASE_URL/admin/realms/$REALM_NAME/clients?clientId=$CLIENT_ID")
    
    if [ $? -eq 0 ] && [ "$(echo "$existing_clients" | jq '. | length')" -gt 0 ]; then
        log "WARNING" "Client '$CLIENT_ID' already exists. Skipping creation."
        return $SUCCESS
    fi
    
    # Create client configuration
    local client_config='{
        "clientId": "'$CLIENT_ID'",
        "name": "Claude Code Hooks Frontend Application",
        "description": "Public client for Claude Code Hooks React frontend",
        "enabled": true,
        "clientAuthenticatorType": "client-secret",
        "redirectUris": [
            "http://localhost:3000/auth/callback",
            "http://localhost:3000/auth/callback/*",
            "http://localhost:3000/*"
        ],
        "webOrigins": [
            "http://localhost:3000",
            "http://localhost:8080"
        ],
        "protocol": "openid-connect",
        "publicClient": true,
        "bearerOnly": false,
        "standardFlowEnabled": true,
        "implicitFlowEnabled": false,
        "directAccessGrantsEnabled": false,
        "serviceAccountsEnabled": false,
        "fullScopeAllowed": true,
        "frontchannelLogout": true,
        "attributes": {
            "access.token.lifespan": "3600",
            "pkce.code.challenge.method": "S256"
        }
    }'
    
    local response
    response=$(curl -s -w "%{http_code}" -X POST "$KEYCLOAK_BASE_URL/admin/realms/$REALM_NAME/clients" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$client_config")
    
    local http_code="${response: -3}"
    if [ "$http_code" = "201" ]; then
        log "SUCCESS" "Client '$CLIENT_ID' created successfully"
    else
        log "ERROR" "Failed to create client. HTTP code: $http_code"
        log "ERROR" "Response: ${response%???}"
        exit $FAILURE
    fi
}

# Setup Google and GitHub identity providers
setup_identity_providers() {
    log "STEP" "7/8 - Setting up identity providers with real credentials..."
    
    setup_google_provider
    setup_github_provider
}

# Configure Google identity provider
setup_google_provider() {
    log "INFO" "Setting up Google identity provider with real credentials..."
    
    local provider_config='{
        "alias": "google",
        "providerId": "google",
        "enabled": true,
        "trustEmail": true,
        "storeToken": false,
        "addReadTokenRoleOnCreate": false,
        "authenticateByDefault": false,
        "linkOnly": false,
        "firstBrokerLoginFlowAlias": "first broker login",
        "config": {
            "useJwksUrl": "true",
            "syncMode": "IMPORT",
            "clientId": "'$GOOGLE_CLIENT_ID'",
            "clientSecret": "'$GOOGLE_CLIENT_SECRET'",
            "defaultScope": "openid profile email"
        }
    }'
    
    local response
    response=$(curl -s -w "%{http_code}" -X POST \
        "$KEYCLOAK_BASE_URL/admin/realms/$REALM_NAME/identity-provider/instances" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$provider_config")
    
    local http_code="${response: -3}"
    if [ "$http_code" = "201" ]; then
        log "SUCCESS" "Google identity provider created with Client ID: ${GOOGLE_CLIENT_ID:0:20}..."
    elif [ "$http_code" = "409" ]; then
        log "WARNING" "Google identity provider already exists - updating with new credentials"
        update_google_provider
    else
        log "ERROR" "Failed to create Google identity provider. HTTP code: $http_code"
        log "ERROR" "Response: ${response%???}"
    fi
}

# Update existing Google identity provider
update_google_provider() {
    local provider_config='{
        "alias": "google",
        "providerId": "google",
        "enabled": true,
        "trustEmail": true,
        "storeToken": false,
        "addReadTokenRoleOnCreate": false,
        "authenticateByDefault": false,
        "linkOnly": false,
        "firstBrokerLoginFlowAlias": "first broker login",
        "config": {
            "useJwksUrl": "true",
            "syncMode": "IMPORT",
            "clientId": "'$GOOGLE_CLIENT_ID'",
            "clientSecret": "'$GOOGLE_CLIENT_SECRET'",
            "defaultScope": "openid profile email"
        }
    }'
    
    local response
    response=$(curl -s -w "%{http_code}" -X PUT \
        "$KEYCLOAK_BASE_URL/admin/realms/$REALM_NAME/identity-provider/instances/google" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$provider_config")
    
    local http_code="${response: -3}"
    if [ "$http_code" = "204" ]; then
        log "SUCCESS" "Google identity provider updated with Client ID: ${GOOGLE_CLIENT_ID:0:20}..."
    else
        log "ERROR" "Failed to update Google identity provider. HTTP code: $http_code"
    fi
}

# Configure GitHub identity provider
setup_github_provider() {
    log "INFO" "Setting up GitHub identity provider with real credentials..."
    
    local provider_config='{
        "alias": "github",
        "providerId": "github",
        "enabled": true,
        "trustEmail": true,
        "storeToken": false,
        "addReadTokenRoleOnCreate": false,
        "authenticateByDefault": false,
        "linkOnly": false,
        "firstBrokerLoginFlowAlias": "first broker login",
        "config": {
            "syncMode": "IMPORT",
            "clientId": "'$GITHUB_CLIENT_ID'",
            "clientSecret": "'$GITHUB_CLIENT_SECRET'",
            "defaultScope": "user:email"
        }
    }'
    
    local response
    response=$(curl -s -w "%{http_code}" -X POST \
        "$KEYCLOAK_BASE_URL/admin/realms/$REALM_NAME/identity-provider/instances" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$provider_config")
    
    local http_code="${response: -3}"
    if [ "$http_code" = "201" ]; then
        log "SUCCESS" "GitHub identity provider created with Client ID: ${GITHUB_CLIENT_ID:0:20}..."
    elif [ "$http_code" = "409" ]; then
        log "WARNING" "GitHub identity provider already exists - updating with new credentials"
        update_github_provider
    else
        log "ERROR" "Failed to create GitHub identity provider. HTTP code: $http_code"
        log "ERROR" "Response: ${response%???}"
    fi
}

# Update existing GitHub identity provider
update_github_provider() {
    local provider_config='{
        "alias": "github",
        "providerId": "github",
        "enabled": true,
        "trustEmail": true,
        "storeToken": false,
        "addReadTokenRoleOnCreate": false,
        "authenticateByDefault": false,
        "linkOnly": false,
        "firstBrokerLoginFlowAlias": "first broker login",
        "config": {
            "syncMode": "IMPORT",
            "clientId": "'$GITHUB_CLIENT_ID'",
            "clientSecret": "'$GITHUB_CLIENT_SECRET'",
            "defaultScope": "user:email"
        }
    }'
    
    local response
    response=$(curl -s -w "%{http_code}" -X PUT \
        "$KEYCLOAK_BASE_URL/admin/realms/$REALM_NAME/identity-provider/instances/github" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$provider_config")
    
    local http_code="${response: -3}"
    if [ "$http_code" = "204" ]; then
        log "SUCCESS" "GitHub identity provider updated with Client ID: ${GITHUB_CLIENT_ID:0:20}..."
    else
        log "ERROR" "Failed to update GitHub identity provider. HTTP code: $http_code"
    fi
}

# Configure account linking policies
configure_account_linking() {
    log "STEP" "8/8 - Configuring cross-email account linking and Frontend URL..."
    
    # Update realm settings for account linking and Frontend URL
    local realm_update='{
        "duplicateEmailsAllowed": false,
        "loginWithEmailAllowed": true,
        "editUsernameAllowed": false,
        "attributes": {
            "frontendUrl": "http://localhost:8080"
        }
    }'
    
    local response
    response=$(curl -s -w "%{http_code}" -X PUT "$KEYCLOAK_BASE_URL/admin/realms/$REALM_NAME" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$realm_update")
    
    local http_code="${response: -3}"
    if [ "$http_code" = "204" ]; then
        log "SUCCESS" "Account linking policies configured"
    else
        log "WARNING" "Account linking configuration may have issues. HTTP code: $http_code"
    fi
}

# Validate the complete setup
validate_setup() {
    log "STEP" "Validating HTTP setup..."
    
    # Check realm
    local realm_check
    realm_check=$(curl -s -H "Authorization: Bearer $ACCESS_TOKEN" \
        "$KEYCLOAK_BASE_URL/admin/realms/$REALM_NAME")
    
    if echo "$realm_check" | jq -e '.realm' > /dev/null 2>&1; then
        log "SUCCESS" "✓ Realm '$REALM_NAME' is accessible"
    else
        log "ERROR" "✗ Realm validation failed"
        return $FAILURE
    fi
    
    # Check client
    local client_check
    client_check=$(curl -s -H "Authorization: Bearer $ACCESS_TOKEN" \
        "$KEYCLOAK_BASE_URL/admin/realms/$REALM_NAME/clients?clientId=$CLIENT_ID")
    
    if [ "$(echo "$client_check" | jq '. | length')" -gt 0 ]; then
        log "SUCCESS" "✓ Client '$CLIENT_ID' is configured"
    else
        log "ERROR" "✗ Client validation failed"
        return $FAILURE
    fi
    
    # Check identity providers
    local providers_check
    providers_check=$(curl -s -H "Authorization: Bearer $ACCESS_TOKEN" \
        "$KEYCLOAK_BASE_URL/admin/realms/$REALM_NAME/identity-provider/instances")
    
    local provider_count
    provider_count=$(echo "$providers_check" | jq '. | length')
    
    if [ "$provider_count" -ge 2 ]; then
        log "SUCCESS" "✓ Identity providers configured ($provider_count providers)"
    else
        log "WARNING" "⚠ Only $provider_count identity providers found"
    fi
    
    # Validate OpenID configuration
    validate_openid_configuration
    
    log "SUCCESS" "HTTP setup validation completed! 🎉"
}

# Validate OpenID configuration endpoint
validate_openid_configuration() {
    log "INFO" "🔍 Validating OpenID configuration (HTTP)..."
    
    local openid_endpoint="$KEYCLOAK_BASE_URL/realms/$REALM_NAME/.well-known/openid-configuration"
    
    # Check if OpenID configuration endpoint is accessible
    local openid_config
    openid_config=$(curl -s "$openid_endpoint" 2>/dev/null)
    
    if [ $? -ne 0 ] || [ -z "$openid_config" ]; then
        log "ERROR" "✗ Failed to fetch OpenID configuration from: $openid_endpoint"
        return $FAILURE
    fi
    
    # Validate that the response is valid JSON
    if ! echo "$openid_config" | jq empty 2>/dev/null; then
        log "ERROR" "✗ OpenID configuration response is not valid JSON"
        return $FAILURE
    fi
    
    # Extract and validate the issuer URL
    local issuer
    issuer=$(echo "$openid_config" | jq -r '.issuer' 2>/dev/null)
    
    local expected_issuer="$KEYCLOAK_BASE_URL/realms/$REALM_NAME"
    
    if [ "$issuer" = "null" ] || [ -z "$issuer" ]; then
        log "ERROR" "✗ No issuer found in OpenID configuration"
        return $FAILURE
    elif [ "$issuer" = "$expected_issuer" ]; then
        log "SUCCESS" "✓ OpenID configuration accessible at: $openid_endpoint"
        log "SUCCESS" "✓ Issuer URL matches expected: $issuer"
        log "INFO" "💡 JWT validation will work correctly (no DevJwtConfig needed!)"
    else
        log "ERROR" "✗ Issuer URL mismatch!"
        log "ERROR" "  Expected: $expected_issuer"
        log "ERROR" "  Got:      $issuer"
        return $FAILURE
    fi
    
    # Validate other important endpoints
    local auth_endpoint
    auth_endpoint=$(echo "$openid_config" | jq -r '.authorization_endpoint' 2>/dev/null)
    
    local token_endpoint
    token_endpoint=$(echo "$openid_config" | jq -r '.token_endpoint' 2>/dev/null)
    
    local jwks_uri
    jwks_uri=$(echo "$openid_config" | jq -r '.jwks_uri' 2>/dev/null)
    
    if [ "$auth_endpoint" != "null" ] && [ "$token_endpoint" != "null" ] && [ "$jwks_uri" != "null" ]; then
        log "SUCCESS" "✓ OAuth2 endpoints configured correctly"
        log "INFO" "  Authorization: $auth_endpoint"
        log "INFO" "  Token: $token_endpoint"
        log "INFO" "  JWKS: $jwks_uri"
    else
        log "WARNING" "⚠ Some OAuth2 endpoints may not be properly configured"
    fi
}

# Print completion summary and next steps
print_completion_summary() {
    echo
    echo -e "${COLOR_GREEN}╔══════════════════════════════════════════════════════════════╗${COLOR_RESET}"
    echo -e "${COLOR_GREEN}║                🎉 HTTP Setup Complete! 🎉                   ║${COLOR_RESET}"
    echo -e "${COLOR_GREEN}╚══════════════════════════════════════════════════════════════╝${COLOR_RESET}"
    echo
    echo -e "${COLOR_BLUE}✅ Keycloak HTTP Configuration Summary:${COLOR_RESET}"
    echo -e "${COLOR_BLUE}   • SSL Requirements: DISABLED (kcadm.sh bypass)${COLOR_RESET}"
    echo -e "${COLOR_BLUE}   • Realm: $REALM_NAME${COLOR_RESET}"
    echo -e "${COLOR_BLUE}   • Client: $CLIENT_ID${COLOR_RESET}"
    echo -e "${COLOR_BLUE}   • Identity Providers: Google (${GOOGLE_CLIENT_ID:0:20}...), GitHub (${GITHUB_CLIENT_ID:0:20}...)${COLOR_RESET}"
    echo -e "${COLOR_BLUE}   • Base URL: $KEYCLOAK_BASE_URL (HTTP - no SSL!)${COLOR_RESET}"
    echo -e "${COLOR_BLUE}   • JWT Issuer: $KEYCLOAK_BASE_URL/realms/$REALM_NAME${COLOR_RESET}"
    echo
    echo -e "${COLOR_GREEN}🎯 HTTP Benefits Achieved:${COLOR_RESET}"
    echo -e "${COLOR_GREEN}   • Keycloak 26.0 HTTPS requirement bypassed${COLOR_RESET}"
    echo -e "${COLOR_GREEN}   • No SSL certificates needed${COLOR_RESET}"
    echo -e "${COLOR_GREEN}   • No DevJwtConfig needed${COLOR_RESET}"
    echo -e "${COLOR_GREEN}   • No Frontend URL complexity${COLOR_RESET}"
    echo -e "${COLOR_GREEN}   • Simple OAuth configuration${COLOR_RESET}"
    echo
    echo -e "${COLOR_YELLOW}🔍 Quick Test:${COLOR_RESET}"
    echo -e "${COLOR_YELLOW}   • Keycloak Admin: $KEYCLOAK_BASE_URL/admin/${COLOR_RESET}"
    echo -e "${COLOR_YELLOW}   • Account Console: $KEYCLOAK_BASE_URL/realms/$REALM_NAME/account/${COLOR_RESET}"
    echo -e "${COLOR_YELLOW}   • OpenID Config: $KEYCLOAK_BASE_URL/realms/$REALM_NAME/.well-known/openid-configuration${COLOR_RESET}"
    echo
    echo -e "${COLOR_CYAN}📋 Next Steps:${COLOR_RESET}"
    echo -e "${COLOR_CYAN}1. Update OAuth Provider Redirect URIs:${COLOR_RESET}"
    echo -e "${COLOR_CYAN}   • Google: $KEYCLOAK_BASE_URL/realms/$REALM_NAME/broker/google/endpoint${COLOR_RESET}"
    echo -e "${COLOR_CYAN}   • GitHub: $KEYCLOAK_BASE_URL/realms/$REALM_NAME/broker/github/endpoint${COLOR_RESET}"
    echo
    echo -e "${COLOR_CYAN}2. Start your application with HTTP configuration:${COLOR_RESET}"
    echo -e "${COLOR_CYAN}   • Use KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/claude-code-hooks${COLOR_RESET}"
    echo -e "${COLOR_CYAN}   • Remove or disable DevJwtConfig (not needed!)${COLOR_RESET}"
    echo
    log "SUCCESS" "Keycloak HTTP setup completed successfully! 🚀"
}

# Logging helper with color and improved format
log() {
    local prefix="$1"
    local message="$2"
    local color=""
    
    # Set color for specific log types
    case "$prefix" in
        "INFO") color="$COLOR_BLUE" ;;
        "SUCCESS") color="$COLOR_GREEN" ;;
        "ERROR") color="$COLOR_RED" ;;
        "WARNING") color="$COLOR_YELLOW" ;;
        "STEP") color="$COLOR_CYAN" ;;
    esac
    
    printf "${color}[%s] [%s]: %s${COLOR_RESET}\\n" "$prefix" "$(date '+%Y-%m-%d %H:%M:%S')" "$message"
}

# Execute main function
main "$@"
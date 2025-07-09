/**
 * Server-Sent Events Manager
 * Handles SSE connections with robust error handling and automatic reconnection
 */

import { logger } from '../utils/logger.js';
import { CONFIG } from '../config/constants.js';

/**
 * SSE Manager Class
 * Manages Server-Sent Events connection with automatic reconnection
 */
export class SSEManager {
    constructor({ notification, stats, ui }) {
        this.notificationManager = notification;
        this.statsManager = stats;
        this.uiManager = ui;
        
        // Connection state
        this.eventSource = null;
        this.isConnecting = false;
        this.isConnected = false;
        this.reconnectAttempts = 0;
        this.reconnectTimeout = null;
        
        // Bind methods
        this.connect = this.connect.bind(this);
        this.disconnect = this.disconnect.bind(this);
        this.handleOpen = this.handleOpen.bind(this);
        this.handleMessage = this.handleMessage.bind(this);
        this.handleError = this.handleError.bind(this);
        this.scheduleReconnect = this.scheduleReconnect.bind(this);
    }

    /**
     * Initialize SSE manager
     * @throws {Error} If initialization fails
     */
    async init() {
        try {
            logger.info('Initializing SSE Manager');
            await this.connect();
        } catch (error) {
            logger.error('Failed to initialize SSE Manager:', error);
            throw new Error(`SSE Manager initialization failed: ${error.message}`);
        }
    }

    /**
     * Connect to SSE stream
     * @returns {Promise<void>} Connection promise
     * @throws {Error} If connection fails after all retry attempts
     */
    async connect() {
        if (this.isConnecting) {
            logger.debug('Connection attempt already in progress');
            return Promise.resolve();
        }

        if (this.isConnected) {
            logger.debug('Already connected to SSE stream');
            return Promise.resolve();
        }

        this.isConnecting = true;
        
        try {
            // Clear any existing reconnect timeout
            if (this.reconnectTimeout) {
                clearTimeout(this.reconnectTimeout);
                this.reconnectTimeout = null;
            }

            // Close existing connection
            if (this.eventSource) {
                this.eventSource.close();
                this.eventSource = null;
            }

            // Create new EventSource connection
            await this.createConnection();
            
        } catch (error) {
            logger.error('Failed to connect to SSE stream:', error);
            this.isConnecting = false;
            
            // Update managers about connection failure
            this.uiManager?.updateConnectionStatus(false);
            this.statsManager?.updateConnectionCount(0);
            this.statsManager?.incrementErrorCount();
            
            // Schedule reconnection for automatic retry
            this.scheduleReconnect();
            
            // Re-throw for caller awareness
            throw error;
        }
    }

    /**
     * Create new SSE connection
     * @returns {Promise<void>} Connection creation promise
     * @throws {Error} If connection creation fails
     */
    async createConnection() {
        return new Promise((resolve, reject) => {
            let timeout;
            
            try {
                logger.info('Creating new SSE connection...');
                
                // Validate endpoint URL
                if (!CONFIG.API.ENDPOINTS.SSE_STREAM) {
                    throw new Error('SSE stream endpoint not configured');
                }
                
                // Create EventSource
                this.eventSource = new EventSource(CONFIG.API.ENDPOINTS.SSE_STREAM);
                
                // Set up event listeners
                this.eventSource.addEventListener('open', this.handleOpen);
                this.eventSource.addEventListener('message', this.handleMessage);
                this.eventSource.addEventListener('error', (event) => {
                    clearTimeout(timeout);
                    this.handleError(event);
                    reject(new Error('SSE connection failed'));
                });
                
                // Handle specific event types
                this.eventSource.addEventListener(CONFIG.EVENTS.SSE.CONNECTED, this.handleConnected.bind(this));
                this.eventSource.addEventListener(CONFIG.EVENTS.SSE.CLAUDE_HOOK, this.handleClaudeHook.bind(this));
                
                // Connection timeout
                timeout = setTimeout(() => {
                    if (!this.isConnected) {
                        logger.error('SSE connection timeout');
                        this.cleanup();
                        reject(new Error(`Connection timeout after ${CONFIG.SSE.CONNECTION_TIMEOUT}ms`));
                    }
                }, CONFIG.SSE.CONNECTION_TIMEOUT);
                
                // Resolve when connection is established
                this.eventSource.addEventListener('open', () => {
                    clearTimeout(timeout);
                    resolve();
                }, { once: true });
                
            } catch (error) {
                if (timeout) clearTimeout(timeout);
                logger.error('Failed to create SSE connection:', error);
                reject(error);
            }
        });
    }

    /**
     * Handle connection open
     */
    handleOpen() {
        logger.info('✅ SSE connection established');
        this.isConnecting = false;
        this.isConnected = true;
        this.reconnectAttempts = 0;
        
        // Update UI
        this.uiManager?.updateConnectionStatus(true);
        
        // Update stats
        this.statsManager?.updateConnectionCount(1);
        
        // Dispatch custom event
        this.dispatchEvent(CONFIG.EVENTS.CUSTOM.CONNECTION_STATUS_CHANGED, {
            connected: true,
            timestamp: new Date().toISOString()
        });
    }

    /**
     * Handle generic message
     */
    handleMessage(event) {
        logger.debug('Received SSE message:', event.data);
    }

    /**
     * Handle connection event
     */
    handleConnected(event) {
        logger.debug('SSE connection confirmed:', event.data);
        try {
            const data = JSON.parse(event.data);
            logger.info('Connection data:', data);
        } catch (error) {
            logger.debug('Connection event data is not JSON');
        }
    }

    /**
     * Handle Claude hook event
     * @param {MessageEvent} event - SSE message event
     */
    async handleClaudeHook(event) {
        try {
            const hookEvent = JSON.parse(event.data);
            logger.info('Claude hook event received:', hookEvent);
            
            // Process the hook event asynchronously
            await this.processHookEvent(hookEvent);
            
        } catch (error) {
            logger.error('Error processing Claude hook event:', error);
            
            // Update error stats
            this.statsManager?.incrementErrorCount();
            
            // Show error in UI
            this.uiManager?.showError(`Failed to process hook event: ${error.message}`);
        }
    }

    /**
     * Process hook event
     * @param {Object} hookEvent - Hook event data
     * @throws {Error} If event processing fails
     */
    async processHookEvent(hookEvent) {
        try {
            // Validate event data
            if (!this.validateHookEvent(hookEvent)) {
                throw new Error(`Invalid hook event data: ${JSON.stringify(hookEvent)}`);
            }

            // Handle notification asynchronously
            if (this.notificationManager) {
                await this.notificationManager.handleEvent(hookEvent);
            }

            // Update stats
            if (this.statsManager) {
                this.statsManager.incrementEventCount();
            }

            // Update UI
            if (this.uiManager) {
                this.uiManager.addNotification(hookEvent);
            }

            // Dispatch custom event
            this.dispatchEvent(CONFIG.EVENTS.CUSTOM.NOTIFICATION_RECEIVED, hookEvent);
            
        } catch (error) {
            logger.error('Failed to process hook event:', error);
            throw error;
        }
    }

    /**
     * Validate hook event data
     * @param {Object} hookEvent - Hook event data
     * @returns {boolean} True if valid
     */
    validateHookEvent(hookEvent) {
        if (!hookEvent || typeof hookEvent !== 'object') {
            return false;
        }

        const requiredFields = ['id', 'message', 'timestamp'];
        return requiredFields.every(field => field in hookEvent);
    }

    /**
     * Handle connection error
     */
    handleError(event) {
        logger.error('SSE connection error:', event);
        
        this.isConnecting = false;
        this.isConnected = false;
        
        // Update UI
        this.uiManager?.updateConnectionStatus(false);
        
        // Update stats
        this.statsManager?.updateConnectionCount(0);
        
        // Close failed connection
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
        }
        
        // Schedule reconnection
        this.scheduleReconnect();
        
        // Dispatch custom event
        this.dispatchEvent(CONFIG.EVENTS.CUSTOM.CONNECTION_STATUS_CHANGED, {
            connected: false,
            timestamp: new Date().toISOString(),
            error: event
        });
    }

    /**
     * Schedule reconnection
     */
    scheduleReconnect() {
        if (this.reconnectTimeout) {
            clearTimeout(this.reconnectTimeout);
        }

        if (this.isConnecting) {
            return;
        }

        if (this.reconnectAttempts >= CONFIG.SSE.MAX_RECONNECT_ATTEMPTS) {
            logger.error('Max reconnect attempts reached, giving up');
            return;
        }

        const delay = Math.min(
            CONFIG.SSE.INITIAL_RECONNECT_DELAY * Math.pow(CONFIG.SSE.RECONNECT_BACKOFF_MULTIPLIER, this.reconnectAttempts),
            CONFIG.SSE.RECONNECT_INTERVAL
        );

        this.reconnectAttempts++;
        
        logger.info(`⏳ Scheduling reconnection attempt ${this.reconnectAttempts} in ${delay}ms`);
        
        this.reconnectTimeout = setTimeout(() => {
            logger.info('🔄 Attempting to reconnect...');
            this.connect();
        }, delay);
    }

    /**
     * Disconnect from SSE stream
     * @returns {Promise<void>} Disconnection promise
     */
    async disconnect() {
        try {
            logger.info('Disconnecting from SSE stream');
            
            // Clear reconnect timeout
            if (this.reconnectTimeout) {
                clearTimeout(this.reconnectTimeout);
                this.reconnectTimeout = null;
            }

            // Close connection
            if (this.eventSource) {
                this.eventSource.close();
                this.eventSource = null;
            }

            // Reset state
            this.isConnecting = false;
            this.isConnected = false;
            this.reconnectAttempts = 0;
            
            // Update UI
            this.uiManager?.updateConnectionStatus(false);
            
            // Update stats
            this.statsManager?.updateConnectionCount(0);
            
            // Dispatch disconnection event
            this.dispatchEvent(CONFIG.EVENTS.CUSTOM.CONNECTION_STATUS_CHANGED, {
                connected: false,
                timestamp: new Date().toISOString(),
                reason: 'manual_disconnect'
            });
            
        } catch (error) {
            logger.error('Error during SSE disconnect:', error);
            throw error;
        }
    }

    /**
     * Ensure connection is active
     */
    ensureConnection() {
        if (!this.isConnected && !this.isConnecting) {
            logger.info('Ensuring SSE connection is active');
            this.connect();
        }
    }

    /**
     * Get connection status
     * @returns {Object} Connection status
     */
    getConnectionStatus() {
        return {
            connected: this.isConnected,
            connecting: this.isConnecting,
            reconnectAttempts: this.reconnectAttempts,
            readyState: this.eventSource?.readyState,
            url: this.eventSource?.url
        };
    }

    /**
     * Dispatch custom event
     * @param {string} eventName - Event name
     * @param {Object} detail - Event detail
     */
    dispatchEvent(eventName, detail) {
        const event = new CustomEvent(eventName, { detail });
        document.dispatchEvent(event);
    }

    /**
     * Cleanup resources
     */
    cleanup() {
        logger.info('Cleaning up SSE Manager');
        this.disconnect();
    }
}
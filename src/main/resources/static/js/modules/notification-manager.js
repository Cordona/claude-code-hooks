/**
 * Notification Manager
 * Handles browser notifications, audio feedback, and notification history
 */

import { logger } from '../utils/logger.js';
import { CONFIG, CAPABILITIES } from '../config/constants.js';

/**
 * Notification Manager Class
 * Manages browser notifications and notification history
 */
export class NotificationManager {
    constructor({ audio }) {
        this.audioManager = audio;
        
        // Notification state
        this.notifications = [];
        this.permission = Notification.permission;
        this.isRequestingPermission = false;
        
        // Bind methods
        this.handleEvent = this.handleEvent.bind(this);
        this.requestPermission = this.requestPermission.bind(this);
        this.showNotification = this.showNotification.bind(this);
        this.test = this.test.bind(this);
        this.clearAll = this.clearAll.bind(this);
    }

    /**
     * Initialize notification manager
     * @throws {Error} If initialization fails
     */
    async init() {
        try {
            logger.info('Initializing Notification Manager');
            
            // Check browser support
            if (!CAPABILITIES.NOTIFICATIONS) {
                logger.warn('Browser notifications not supported');
                return;
            }

            // Load stored notifications
            await this.loadNotifications();
            
            // Check permission status
            this.checkPermission();
            
            // Set up permission monitoring
            this.setupPermissionMonitoring();
            
        } catch (error) {
            logger.error('Failed to initialize Notification Manager:', error);
            throw new Error(`Notification Manager initialization failed: ${error.message}`);
        }
    }

    /**
     * Load notifications from storage
     * @returns {Promise<void>} Loading promise
     */
    async loadNotifications() {
        try {
            const stored = localStorage.getItem(CONFIG.STORAGE.NOTIFICATIONS);
            if (stored) {
                this.notifications = JSON.parse(stored);
                logger.debug(`Loaded ${this.notifications.length} notifications from storage`);
            }
        } catch (error) {
            logger.error('Failed to load notifications from storage:', error);
            this.notifications = [];
            throw new Error(`Failed to load notifications: ${error.message}`);
        }
    }

    /**
     * Save notifications to storage
     */
    saveNotifications() {
        try {
            // Keep only recent notifications
            if (this.notifications.length > CONFIG.NOTIFICATIONS.MAX_HISTORY) {
                this.notifications = this.notifications.slice(-CONFIG.NOTIFICATIONS.MAX_HISTORY);
            }
            
            localStorage.setItem(CONFIG.STORAGE.NOTIFICATIONS, JSON.stringify(this.notifications));
        } catch (error) {
            logger.error('Failed to save notifications to storage:', error);
        }
    }

    /**
     * Check notification permission
     */
    checkPermission() {
        this.permission = Notification.permission;
        logger.debug(`Notification permission: ${this.permission}`);
        
        // Update UI indicator
        this.updatePermissionIndicator();
    }

    /**
     * Set up permission monitoring
     */
    setupPermissionMonitoring() {
        // Check permission periodically
        setInterval(() => {
            const currentPermission = Notification.permission;
            if (currentPermission !== this.permission) {
                this.permission = currentPermission;
                this.updatePermissionIndicator();
                logger.info(`Notification permission changed to: ${this.permission}`);
            }
        }, CONFIG.NOTIFICATIONS.PERMISSION_CHECK_INTERVAL);
    }

    /**
     * Update permission indicator in UI
     */
    updatePermissionIndicator() {
        const indicator = document.getElementById('notificationStatus');
        if (indicator) {
            const isEnabled = this.permission === 'granted';
            indicator.classList.toggle('status-indicator--enabled', isEnabled);
            indicator.setAttribute('aria-label', 
                isEnabled ? 'Browser notifications enabled' : 'Browser notifications disabled'
            );
        }
    }

    /**
     * Request notification permission
     * @returns {Promise<string>} Permission result
     */
    async requestPermission() {
        if (!CAPABILITIES.NOTIFICATIONS) {
            throw new Error('Browser notifications not supported');
        }

        if (this.isRequestingPermission) {
            logger.debug('Permission request already in progress');
            return this.permission;
        }

        if (this.permission === 'granted') {
            return this.permission;
        }

        this.isRequestingPermission = true;

        try {
            logger.info('Requesting notification permission');
            const result = await Notification.requestPermission();
            
            this.permission = result;
            this.updatePermissionIndicator();
            
            if (result === 'granted') {
                logger.info('✅ Notification permission granted');
                
                // Show welcome notification
                this.showNotification({
                    title: '🎉 Notifications Enabled!',
                    message: 'You\'ll now receive Claude Code notifications in your system notification center.',
                    type: 'success'
                });
                
            } else {
                logger.warn('❌ Notification permission denied');
            }
            
            return result;
            
        } catch (error) {
            logger.error('Error requesting notification permission:', error);
            throw error;
        } finally {
            this.isRequestingPermission = false;
        }
    }

    /**
     * Handle incoming hook event
     * @param {Object} hookEvent - Hook event data
     * @returns {Promise<void>} Event handling promise
     */
    async handleEvent(hookEvent) {
        try {
            logger.debug('Handling notification event:', hookEvent);
            
            // Validate event data
            if (!hookEvent || typeof hookEvent !== 'object') {
                throw new Error('Invalid event data provided');
            }
            
            if (!hookEvent.message) {
                throw new Error('Event message is required');
            }
            
            // Add to history
            this.addToHistory(hookEvent);
            
            // Show browser notification if permitted
            if (this.permission === 'granted') {
                await this.showNotification({
                    title: '🤖 Claude Code Hooks',
                    message: hookEvent.message,
                    data: hookEvent
                });
            }
            
            // Play audio feedback
            if (this.audioManager) {
                await this.audioManager.playNotificationSound();
            }
            
        } catch (error) {
            logger.error('Failed to handle event:', error);
            throw error;
        }
    }

    /**
     * Add notification to history
     * @param {Object} hookEvent - Hook event data
     */
    addToHistory(hookEvent) {
        const notification = {
            id: hookEvent.id,
            message: hookEvent.message,
            timestamp: hookEvent.timestamp,
            projectContext: hookEvent.project_context || hookEvent.projectContext,
            addedAt: new Date().toISOString()
        };
        
        this.notifications.unshift(notification);
        this.saveNotifications();
        
        logger.debug('Added notification to history:', notification);
    }

    /**
     * Show browser notification
     * @param {Object} options - Notification options
     * @returns {Promise<Notification|null>} Notification instance or null
     */
    async showNotification(options) {
        if (!CAPABILITIES.NOTIFICATIONS || this.permission !== 'granted') {
            logger.debug('Cannot show notification - no permission');
            return null;
        }

        try {
            // Validate required options
            if (!options.title || !options.message) {
                throw new Error('Notification title and message are required');
            }
            
            const notification = new Notification(options.title, {
                body: options.message,
                icon: this.getNotificationIcon(options.type),
                tag: options.data?.id || 'claude-hook',
                requireInteraction: false,
                silent: false,
                timestamp: Date.now(),
                ...options
            });

            // Handle notification events
            notification.addEventListener('show', () => {
                logger.debug('✅ Notification shown');
            });

            notification.addEventListener('click', () => {
                logger.debug('Notification clicked');
                window.focus();
                notification.close();
            });

            notification.addEventListener('close', () => {
                logger.debug('Notification closed');
            });

            notification.addEventListener('error', (error) => {
                logger.error('Notification error:', error);
            });

            // Auto-close after duration
            setTimeout(() => {
                notification.close();
            }, CONFIG.NOTIFICATIONS.DEFAULT_DURATION);

            return notification;

        } catch (error) {
            logger.error('Error creating notification:', error);
            throw error;
        }
    }

    /**
     * Get notification icon based on type
     * @param {string} type - Notification type
     * @returns {string} Icon data URL
     */
    getNotificationIcon(type) {
        const icons = {
            success: 'PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTEyIDJMMTMuMDkgOC4yNkwyMCA5TDEzLjA5IDE1Ljc0TDEyIDIyTDEwLjkxIDE1Ljc0TDQgOUwxMC45MSA4LjI2TDEyIDJaIiBmaWxsPSIjMjhhNzQ1Ii8+Cjwvc3ZnPgo=',
            warning: 'PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTEyIDJMMTMuMDkgOC4yNkwyMCA5TDEzLjA5IDE1Ljc0TDEyIDIyTDEwLjkxIDE1Ljc0TDQgOUwxMC45MSA4LjI2TDEyIDJaIiBmaWxsPSIjZmZjMTA3Ii8+Cjwvc3ZnPgo=',
            error: 'PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTEyIDJMMTMuMDkgOC4yNkwyMCA5TDEzLjA5IDE1Ljc0TDEyIDIyTDEwLjkxIDE1Ljc0TDQgOUwxMC45MSA4LjI2TDEyIDJaIiBmaWxsPSIjZWY0NDQ0Ii8+Cjwvc3ZnPgo=',
            default: 'PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTEyIDJMMTMuMDkgOC4yNkwyMCA5TDEzLjA5IDE1Ljc0TDEyIDIyTDEwLjkxIDE1Ljc0TDQgOUwxMC45MSA4LjI2TDEyIDJaIiBmaWxsPSIjNjY3ZWVhIi8+Cjwvc3ZnPgo='
        };

        return `data:image/svg+xml;base64,${icons[type] || icons.default}`;
    }

    /**
     * Test notification functionality
     * @returns {Promise<void>} Test promise
     */
    async test() {
        try {
            logger.info('Testing notification functionality');
            
            // Request permission if needed
            if (this.permission !== 'granted') {
                await this.requestPermission();
            }
            
            // Show test notification
            await this.showNotification({
                title: '🧪 Test Notification',
                message: 'If you see this, browser notifications are working correctly!',
                type: 'default'
            });
            
            // Play test sound
            if (this.audioManager) {
                await this.audioManager.playNotificationSound();
            }
            
            logger.info('✅ Test notification sent successfully');
            
        } catch (error) {
            logger.error('Test notification failed:', error);
            throw new Error(`Notification test failed: ${error.message}`);
        }
    }

    /**
     * Clear all notifications
     */
    clearAll() {
        logger.info('Clearing all notifications');
        
        // Clear history
        this.notifications = [];
        this.saveNotifications();
        
        // Update UI
        this.updateNotificationsList();
        
        logger.info('✅ All notifications cleared');
    }

    /**
     * Remove specific notification
     * @param {string} id - Notification ID
     */
    remove(id) {
        const index = this.notifications.findIndex(n => n.id === id);
        if (index !== -1) {
            this.notifications.splice(index, 1);
            this.saveNotifications();
            this.updateNotificationsList();
            logger.debug(`Removed notification: ${id}`);
        }
    }

    /**
     * Update notifications list in UI
     */
    updateNotificationsList() {
        const listElement = document.getElementById('notificationsList');
        const titleElement = document.getElementById('notificationsTitle');
        
        if (!listElement || !titleElement) return;
        
        // Update title
        titleElement.textContent = `Recent Notifications (${this.notifications.length} total)`;
        
        // Update list
        if (this.notifications.length === 0) {
            listElement.innerHTML = `
                <div class="notifications__empty" role="status">
                    <div class="notifications__empty-icon" aria-hidden="true">·</div>
                    <p>No notifications yet</p>
                    <p class="notifications__empty-subtitle">Test the connection or wait for Claude to request permissions</p>
                </div>
            `;
        } else {
            const notificationsHTML = this.notifications.map((notification, index) => {
                const timestamp = new Date(notification.timestamp);
                const timeStr = this.formatTime(timestamp);
                const dateStr = this.formatDate(timestamp);
                
                return `
                    <div class="notifications__row" role="row">
                        <div class="notifications__cell notifications__cell--time" role="cell">${timeStr}</div>
                        <div class="notifications__cell notifications__cell--message" role="cell">${notification.message}</div>
                        <div class="notifications__cell notifications__cell--date" role="cell">${dateStr}</div>
                        <div class="notifications__cell notifications__cell--actions" role="cell">
                            <button 
                                class="notifications__delete-btn" 
                                onclick="notificationManager.remove('${notification.id}')"
                                aria-label="Delete notification"
                                title="Delete notification">
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                    <line x1="18" y1="6" x2="6" y2="18"/>
                                    <line x1="6" y1="6" x2="18" y2="18"/>
                                </svg>
                            </button>
                        </div>
                    </div>
                `;
            }).join('');
            
            listElement.innerHTML = notificationsHTML;
        }
    }

    /**
     * Format time for display
     * @param {Date} date - Date object
     * @returns {string} Formatted time
     */
    formatTime(date) {
        return date.toLocaleTimeString('en-GB', { 
            hour: '2-digit', 
            minute: '2-digit' 
        });
    }

    /**
     * Format date for display
     * @param {Date} date - Date object
     * @returns {string} Formatted date
     */
    formatDate(date) {
        return date.toLocaleDateString('en-GB', { 
            day: '2-digit', 
            month: '2-digit', 
            year: 'numeric' 
        });
    }

    /**
     * Get notification count
     * @returns {number} Number of notifications
     */
    getCount() {
        return this.notifications.length;
    }

    /**
     * Get notification history
     * @returns {Array} Notification history
     */
    getHistory() {
        return [...this.notifications];
    }

    /**
     * Get permission status
     * @returns {string} Permission status
     */
    getPermissionStatus() {
        return this.permission;
    }

    /**
     * Cleanup resources
     */
    cleanup() {
        logger.info('Cleaning up Notification Manager');
        // Save current state
        this.saveNotifications();
    }
}
/**
 * UI Manager
 * Handles user interface interactions and updates
 */

import { logger } from '../utils/logger.js';
import { $, createElement, addClass, removeClass, toggleClass } from '../utils/dom.js';
import { CONFIG } from '../config/constants.js';

/**
 * UI Manager Class
 * Manages all UI interactions and state
 */
export class UIManager {
    constructor({ theme, notification }) {
        this.themeManager = theme;
        this.notificationManager = notification;
        
        // UI state
        this.isHelpOpen = false;
        this.isMenuOpen = false;
        
        // Bind methods
        this.toggleHelp = this.toggleHelp.bind(this);
        this.toggleMenu = this.toggleMenu.bind(this);
        this.closeAll = this.closeAll.bind(this);
        this.handleClickOutside = this.handleClickOutside.bind(this);
        this.updateConnectionStatus = this.updateConnectionStatus.bind(this);
        this.addNotification = this.addNotification.bind(this);
    }

    /**
     * Initialize UI manager
     */
    async init() {
        logger.info('Initializing UI Manager');
        
        // Set up event listeners
        this.setupEventListeners();
        
        // Initialize UI state
        this.initializeUIState();
        
        // Set up tooltips
        this.setupTooltips();
    }

    /**
     * Set up event listeners
     */
    setupEventListeners() {
        // Click outside to close menus
        document.addEventListener('click', this.handleClickOutside);
        
        // Help button
        const helpBtn = $('#helpBtn');
        if (helpBtn) {
            helpBtn.addEventListener('click', this.toggleHelp);
        }
        
        // Menu button
        const menuBtn = $('#menuBtn');
        if (menuBtn) {
            menuBtn.addEventListener('click', this.toggleMenu);
        }
        
        // Delete all notifications button
        const deleteAllBtn = $('#deleteAllBtn');
        if (deleteAllBtn) {
            deleteAllBtn.addEventListener('click', () => {
                this.confirmDeleteAll();
            });
        }
        
        // ESC key to close menus
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                this.closeAll();
            }
        });
        
        // Theme selector buttons
        this.setupThemeButtons();
        
        // Menu action buttons
        this.setupMenuActions();
    }

    /**
     * Initialize UI state
     */
    initializeUIState() {
        // Update connection status
        this.updateConnectionStatus(false);
        
        // Update notification list
        if (this.notificationManager) {
            this.notificationManager.updateNotificationsList();
        }
    }

    /**
     * Set up tooltips
     */
    setupTooltips() {
        const tooltipElements = document.querySelectorAll('[title]');
        tooltipElements.forEach(element => {
            this.setupTooltip(element);
        });
    }

    /**
     * Set up individual tooltip
     * @param {Element} element - Element with tooltip
     */
    setupTooltip(element) {
        let timeout;
        
        element.addEventListener('mouseenter', () => {
            timeout = setTimeout(() => {
                this.showTooltip(element);
            }, CONFIG.UI.TOOLTIP_DELAY);
        });
        
        element.addEventListener('mouseleave', () => {
            clearTimeout(timeout);
            this.hideTooltip(element);
        });
    }

    /**
     * Show tooltip
     * @param {Element} element - Element with tooltip
     */
    showTooltip(element) {
        const title = element.getAttribute('title');
        if (!title) return;
        
        // Create tooltip element
        const tooltip = createElement('div', {
            className: 'tooltip tooltip--visible',
            textContent: title
        });
        
        // Position tooltip
        const rect = element.getBoundingClientRect();
        tooltip.style.left = rect.left + 'px';
        tooltip.style.top = (rect.bottom + 8) + 'px';
        
        // Add to DOM
        document.body.appendChild(tooltip);
        
        // Store reference
        element._tooltip = tooltip;
    }

    /**
     * Hide tooltip
     * @param {Element} element - Element with tooltip
     */
    hideTooltip(element) {
        if (element._tooltip) {
            element._tooltip.remove();
            delete element._tooltip;
        }
    }

    /**
     * Toggle help tooltip
     * @param {Event} event - Click event
     */
    toggleHelp(event) {
        event.stopPropagation();
        
        const helpTooltip = $('#helpTooltip');
        const helpBtn = $('#helpBtn');
        
        if (!helpTooltip || !helpBtn) return;
        
        this.isHelpOpen = !this.isHelpOpen;
        
        if (this.isHelpOpen) {
            // Close menu if open
            this.closeMenu();
            
            // Position tooltip
            const rect = helpBtn.getBoundingClientRect();
            helpTooltip.style.left = (rect.left - 200) + 'px';
            helpTooltip.style.top = (rect.bottom + 10) + 'px';
            
            // Show tooltip
            helpTooltip.setAttribute('aria-hidden', 'false');
            helpBtn.setAttribute('aria-expanded', 'true');
            
        } else {
            this.closeHelp();
        }
    }

    /**
     * Toggle menu
     * @param {Event} event - Click event
     */
    toggleMenu(event) {
        event.stopPropagation();
        
        const dropdownMenu = $('#dropdownMenu');
        const menuBtn = $('#menuBtn');
        
        if (!dropdownMenu || !menuBtn) return;
        
        this.isMenuOpen = !this.isMenuOpen;
        
        if (this.isMenuOpen) {
            // Close help if open
            this.closeHelp();
            
            // Position menu
            const rect = menuBtn.getBoundingClientRect();
            dropdownMenu.style.left = (rect.left - 180) + 'px';
            dropdownMenu.style.top = (rect.bottom + 10) + 'px';
            
            // Show menu
            dropdownMenu.setAttribute('aria-hidden', 'false');
            menuBtn.setAttribute('aria-expanded', 'true');
            addClass(menuBtn, 'header__btn--active');
            
        } else {
            this.closeMenu();
        }
    }

    /**
     * Close help tooltip
     */
    closeHelp() {
        const helpTooltip = $('#helpTooltip');
        const helpBtn = $('#helpBtn');
        
        if (helpTooltip && helpBtn) {
            helpTooltip.setAttribute('aria-hidden', 'true');
            helpBtn.setAttribute('aria-expanded', 'false');
        }
        
        this.isHelpOpen = false;
    }

    /**
     * Close menu
     */
    closeMenu() {
        const dropdownMenu = $('#dropdownMenu');
        const menuBtn = $('#menuBtn');
        
        if (dropdownMenu && menuBtn) {
            dropdownMenu.setAttribute('aria-hidden', 'true');
            menuBtn.setAttribute('aria-expanded', 'false');
            removeClass(menuBtn, 'header__btn--active');
        }
        
        this.isMenuOpen = false;
    }

    /**
     * Close all menus and tooltips
     */
    closeAll() {
        this.closeHelp();
        this.closeMenu();
    }

    /**
     * Handle click outside
     * @param {Event} event - Click event
     */
    handleClickOutside(event) {
        const helpTooltip = $('#helpTooltip');
        const helpBtn = $('#helpBtn');
        const dropdownMenu = $('#dropdownMenu');
        const menuBtn = $('#menuBtn');
        
        // Check if click is outside help
        if (this.isHelpOpen && helpTooltip && helpBtn) {
            if (!helpTooltip.contains(event.target) && !helpBtn.contains(event.target)) {
                this.closeHelp();
            }
        }
        
        // Check if click is outside menu
        if (this.isMenuOpen && dropdownMenu && menuBtn) {
            if (!dropdownMenu.contains(event.target) && !menuBtn.contains(event.target)) {
                this.closeMenu();
            }
        }
    }

    /**
     * Update connection status
     * @param {boolean} connected - Connection status
     */
    updateConnectionStatus(connected) {
        const indicator = $('#connectionStatus');
        if (indicator) {
            toggleClass(indicator, 'status-indicator--connected', connected);
            indicator.setAttribute('aria-label', 
                connected ? 'Service connected' : 'Service disconnected'
            );
        }
    }

    /**
     * Add notification to UI
     * @param {Object} notification - Notification data
     */
    addNotification(notification) {
        // Update notifications list
        if (this.notificationManager) {
            this.notificationManager.updateNotificationsList();
        }
        
        // Add visual feedback
        this.showNotificationFeedback(notification);
    }

    /**
     * Show notification feedback
     * @param {Object} notification - Notification data
     */
    showNotificationFeedback(notification) {
        // Create temporary feedback element
        const feedback = createElement('div', {
            className: 'notification-feedback',
            innerHTML: `
                <div class="notification-feedback__content">
                    <span class="notification-feedback__icon">📢</span>
                    <span class="notification-feedback__message">${notification.message}</span>
                </div>
            `
        });
        
        // Add to DOM
        document.body.appendChild(feedback);
        
        // Animate in
        setTimeout(() => {
            addClass(feedback, 'notification-feedback--visible');
        }, 10);
        
        // Remove after delay
        setTimeout(() => {
            removeClass(feedback, 'notification-feedback--visible');
            setTimeout(() => {
                feedback.remove();
            }, 300);
        }, 3000);
    }

    /**
     * Confirm delete all notifications
     */
    confirmDeleteAll() {
        if (confirm('Delete all notifications?')) {
            if (this.notificationManager) {
                this.notificationManager.clearAll();
            }
        }
    }

    /**
     * Show loading state
     * @param {boolean} loading - Loading state
     */
    showLoading(loading) {
        const container = $('.container');
        if (container) {
            toggleClass(container, 'container--loading', loading);
        }
    }

    /**
     * Show error message
     * @param {string} message - Error message
     * @param {number} duration - Display duration in milliseconds
     * @returns {HTMLElement} Created error element
     */
    showError(message, duration = 5000) {
        try {
            // Validate input
            if (!message || typeof message !== 'string') {
                throw new Error('Error message must be a non-empty string');
            }
            
            // Sanitize message to prevent XSS
            const sanitizedMessage = message.replace(/</g, '&lt;').replace(/>/g, '&gt;');
            
            const error = createElement('div', {
                className: 'error-message',
                innerHTML: `
                    <div class="error-message__content">
                        <span class="error-message__icon">⚠️</span>
                        <span class="error-message__text">${sanitizedMessage}</span>
                        <button class="error-message__close" onclick="this.parentElement.parentElement.remove()" aria-label="Close error message">×</button>
                    </div>
                `
            });
            
            document.body.appendChild(error);
            
            // Auto-remove after duration
            const timeoutId = setTimeout(() => {
                if (error.parentNode) {
                    error.remove();
                }
            }, duration);
            
            // Store timeout ID for potential cleanup
            error.dataset.timeoutId = timeoutId;
            
            return error;
            
        } catch (err) {
            logger.error('Failed to show error message:', err);
            
            // Fallback to alert if UI creation fails
            alert(`Error: ${message}`);
            return null;
        }
    }

    /**
     * Show success message
     * @param {string} message - Success message
     * @param {number} duration - Display duration in milliseconds
     * @returns {HTMLElement} Created success element
     */
    showSuccess(message, duration = 3000) {
        try {
            // Validate input
            if (!message || typeof message !== 'string') {
                throw new Error('Success message must be a non-empty string');
            }
            
            // Sanitize message to prevent XSS
            const sanitizedMessage = message.replace(/</g, '&lt;').replace(/>/g, '&gt;');
            
            const success = createElement('div', {
                className: 'success-message',
                innerHTML: `
                    <div class="success-message__content">
                        <span class="success-message__icon">✅</span>
                        <span class="success-message__text">${sanitizedMessage}</span>
                    </div>
                `
            });
            
            document.body.appendChild(success);
            
            // Add animation class
            setTimeout(() => {
                addClass(success, 'success-message--visible');
            }, 10);
            
            // Auto-remove after duration
            const timeoutId = setTimeout(() => {
                removeClass(success, 'success-message--visible');
                setTimeout(() => {
                    if (success.parentNode) {
                        success.remove();
                    }
                }, 300); // Wait for animation
            }, duration);
            
            // Store timeout ID for potential cleanup
            success.dataset.timeoutId = timeoutId;
            
            return success;
            
        } catch (err) {
            logger.error('Failed to show success message:', err);
            return null;
        }
    }

    /**
     * Get UI state
     * @returns {Object} UI state
     */
    getState() {
        return {
            helpOpen: this.isHelpOpen,
            menuOpen: this.isMenuOpen
        };
    }

    /**
     * Set up theme selector buttons
     */
    setupThemeButtons() {
        const themeButtons = document.querySelectorAll('.theme-selector__btn');
        logger.debug(`Found ${themeButtons.length} theme buttons`);
        
        themeButtons.forEach(button => {
            button.addEventListener('click', (e) => {
                const theme = button.getAttribute('data-theme');
                logger.debug(`Theme button clicked: ${theme}`);
                
                if (theme && this.themeManager) {
                    this.themeManager.setTheme(theme);
                } else {
                    logger.error('Theme manager not available or invalid theme:', { theme, hasThemeManager: !!this.themeManager });
                }
            });
        });
    }

    /**
     * Set up menu action buttons
     */
    setupMenuActions() {
        const actionButtons = document.querySelectorAll('[data-action]');
        logger.debug(`Found ${actionButtons.length} action buttons`);
        
        actionButtons.forEach(button => {
            button.addEventListener('click', async (e) => {
                const action = button.getAttribute('data-action');
                logger.debug(`Action button clicked: ${action}`);
                await this.handleMenuAction(action);
            });
        });
    }

    /**
     * Handle menu actions
     * @param {string} action - Action to perform
     */
    async handleMenuAction(action) {
        try {
            switch (action) {
                case 'test-notification':
                    if (this.notificationManager) {
                        await this.notificationManager.test();
                    }
                    break;
                    
                case 'clear-notifications':
                    if (this.notificationManager) {
                        this.confirmDeleteAll();
                    }
                    break;
                    
                default:
                    logger.warn(`Unknown menu action: ${action}`);
            }
        } catch (error) {
            logger.error(`Failed to handle menu action '${action}':`, error);
            this.showError(`Failed to ${action.replace('-', ' ')}: ${error.message}`);
        }
    }

    /**
     * Cleanup resources
     */
    cleanup() {
        logger.info('Cleaning up UI Manager');
        
        // Remove event listeners
        document.removeEventListener('click', this.handleClickOutside);
        
        // Close all menus
        this.closeAll();
    }
}
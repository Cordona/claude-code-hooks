/**
 * Keyboard Manager
 * Handles keyboard shortcuts and accessibility
 */

import { logger } from '../utils/logger.js';
import { CONFIG } from '../config/constants.js';

/**
 * Keyboard Manager Class
 * Manages keyboard shortcuts and accessibility features
 */
export class KeyboardManager {
    constructor({ notification, ui }) {
        this.notificationManager = notification;
        this.uiManager = ui;
        
        this.shortcuts = new Map();
        this.isEnabled = true;
        
        // Bind methods
        this.handleKeyDown = this.handleKeyDown.bind(this);
        this.registerShortcuts = this.registerShortcuts.bind(this);
    }

    /**
     * Initialize keyboard manager
     */
    async init() {
        logger.info('Initializing Keyboard Manager');
        
        // Set up keyboard event listeners
        this.setupEventListeners();
        
        // Register default shortcuts
        this.registerDefaultShortcuts();
        
        // Set up focus management
        this.setupFocusManagement();
    }

    /**
     * Set up event listeners
     */
    setupEventListeners() {
        document.addEventListener('keydown', this.handleKeyDown);
    }

    /**
     * Register default shortcuts
     */
    registerDefaultShortcuts() {
        this.registerShortcuts({
            'cmd+t': () => this.notificationManager?.test(),
            'ctrl+t': () => this.notificationManager?.test(),
            'cmd+c': () => this.notificationManager?.clearAll(),
            'ctrl+c': () => this.notificationManager?.clearAll(),
            'cmd+h': () => this.uiManager?.toggleHelp(),
            'ctrl+h': () => this.uiManager?.toggleHelp(),
            'cmd+m': () => this.uiManager?.toggleMenu(),
            'ctrl+m': () => this.uiManager?.toggleMenu(),
            'escape': () => this.uiManager?.closeAll(),
            'f1': () => this.uiManager?.toggleHelp()
        });
    }

    /**
     * Register shortcuts
     * @param {Object} shortcuts - Shortcut mappings
     */
    registerShortcuts(shortcuts) {
        Object.entries(shortcuts).forEach(([key, handler]) => {
            this.shortcuts.set(key, handler);
        });
        
        logger.debug(`Registered ${Object.keys(shortcuts).length} keyboard shortcuts`);
    }

    /**
     * Handle key down event
     * @param {KeyboardEvent} event - Keyboard event
     */
    handleKeyDown(event) {
        if (!this.isEnabled) return;
        
        // Don't handle shortcuts when typing in inputs
        if (this.isInputFocused()) return;
        
        const shortcut = this.getShortcutKey(event);
        const handler = this.shortcuts.get(shortcut);
        
        if (handler) {
            event.preventDefault();
            event.stopPropagation();
            
            try {
                handler();
                logger.debug(`Executed keyboard shortcut: ${shortcut}`);
            } catch (error) {
                logger.error(`Error executing shortcut '${shortcut}':`, error);
            }
        }
    }

    /**
     * Get shortcut key string from event
     * @param {KeyboardEvent} event - Keyboard event
     * @returns {string} Shortcut key string
     */
    getShortcutKey(event) {
        const parts = [];
        
        if (event.ctrlKey) parts.push('ctrl');
        if (event.metaKey) parts.push('cmd');
        if (event.altKey) parts.push('alt');
        if (event.shiftKey) parts.push('shift');
        
        parts.push(event.key.toLowerCase());
        
        return parts.join('+');
    }

    /**
     * Check if input is focused
     * @returns {boolean} True if input is focused
     */
    isInputFocused() {
        const activeElement = document.activeElement;
        if (!activeElement) return false;
        
        const inputTypes = ['input', 'textarea', 'select'];
        const isInput = inputTypes.includes(activeElement.tagName.toLowerCase());
        const isContentEditable = activeElement.contentEditable === 'true';
        
        return isInput || isContentEditable;
    }

    /**
     * Set up focus management
     */
    setupFocusManagement() {
        // Focus trap for modals and menus
        this.setupFocusTrap();
        
        // Skip links for accessibility
        this.setupSkipLinks();
        
        // Focus indicators
        this.setupFocusIndicators();
    }

    /**
     * Set up focus trap
     */
    setupFocusTrap() {
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Tab') {
                // Handle Tab navigation in modals
                this.handleTabNavigation(event);
            }
        });
    }

    /**
     * Handle tab navigation
     * @param {KeyboardEvent} event - Keyboard event
     */
    handleTabNavigation(event) {
        const modal = document.querySelector('[aria-hidden="false"]');
        if (!modal) return;
        
        const focusableElements = modal.querySelectorAll(
            'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
        );
        
        if (focusableElements.length === 0) return;
        
        const firstElement = focusableElements[0];
        const lastElement = focusableElements[focusableElements.length - 1];
        
        if (event.shiftKey) {
            // Shift+Tab
            if (document.activeElement === firstElement) {
                event.preventDefault();
                lastElement.focus();
            }
        } else {
            // Tab
            if (document.activeElement === lastElement) {
                event.preventDefault();
                firstElement.focus();
            }
        }
    }

    /**
     * Set up skip links
     */
    setupSkipLinks() {
        const skipLink = document.createElement('a');
        skipLink.href = '#main';
        skipLink.textContent = 'Skip to main content';
        skipLink.className = 'skip-link';
        skipLink.style.cssText = `
            position: absolute;
            top: -40px;
            left: 6px;
            background: var(--color-primary);
            color: white;
            padding: 8px;
            text-decoration: none;
            border-radius: 4px;
            z-index: 1000;
            transition: top 0.3s;
        `;
        
        skipLink.addEventListener('focus', () => {
            skipLink.style.top = '6px';
        });
        
        skipLink.addEventListener('blur', () => {
            skipLink.style.top = '-40px';
        });
        
        document.body.insertBefore(skipLink, document.body.firstChild);
    }

    /**
     * Set up focus indicators
     */
    setupFocusIndicators() {
        // Enhanced focus indicators for keyboard navigation
        const style = document.createElement('style');
        style.textContent = `
            .keyboard-focus {
                outline: 2px solid var(--color-primary) !important;
                outline-offset: 2px !important;
            }
            
            .focus-visible {
                outline: 2px solid var(--color-primary) !important;
                outline-offset: 2px !important;
            }
        `;
        document.head.appendChild(style);
        
        // Track keyboard usage
        let usingKeyboard = false;
        
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Tab') {
                usingKeyboard = true;
                document.body.classList.add('using-keyboard');
            }
        });
        
        document.addEventListener('mousedown', () => {
            usingKeyboard = false;
            document.body.classList.remove('using-keyboard');
        });
    }

    /**
     * Enable keyboard shortcuts
     */
    enable() {
        this.isEnabled = true;
        logger.info('Keyboard shortcuts enabled');
    }

    /**
     * Disable keyboard shortcuts
     */
    disable() {
        this.isEnabled = false;
        logger.info('Keyboard shortcuts disabled');
    }

    /**
     * Toggle keyboard shortcuts
     * @returns {boolean} New enabled state
     */
    toggle() {
        this.isEnabled = !this.isEnabled;
        logger.info(`Keyboard shortcuts ${this.isEnabled ? 'enabled' : 'disabled'}`);
        return this.isEnabled;
    }

    /**
     * Get registered shortcuts
     * @returns {Array} Array of shortcuts
     */
    getShortcuts() {
        return Array.from(this.shortcuts.keys());
    }

    /**
     * Get shortcut description
     * @param {string} shortcut - Shortcut key
     * @returns {string} Shortcut description
     */
    getShortcutDescription(shortcut) {
        const descriptions = {
            'cmd+t': 'Test Notification',
            'ctrl+t': 'Test Notification',
            'cmd+c': 'Clear History',
            'ctrl+c': 'Clear History',
            'cmd+h': 'Toggle Help',
            'ctrl+h': 'Toggle Help',
            'cmd+m': 'Toggle Menu',
            'ctrl+m': 'Toggle Menu',
            'escape': 'Close All',
            'f1': 'Help'
        };
        
        return descriptions[shortcut] || 'Unknown';
    }

    /**
     * Format shortcut for display
     * @param {string} shortcut - Shortcut key
     * @returns {string} Formatted shortcut
     */
    formatShortcut(shortcut) {
        return shortcut
            .replace('cmd', '⌘')
            .replace('ctrl', 'Ctrl')
            .replace('alt', 'Alt')
            .replace('shift', 'Shift')
            .replace('+', ' + ')
            .toUpperCase();
    }

    /**
     * Get keyboard manager status
     * @returns {Object} Status object
     */
    getStatus() {
        return {
            enabled: this.isEnabled,
            shortcutCount: this.shortcuts.size,
            inputFocused: this.isInputFocused()
        };
    }

    /**
     * Cleanup resources
     */
    cleanup() {
        logger.info('Cleaning up Keyboard Manager');
        
        // Remove event listeners
        document.removeEventListener('keydown', this.handleKeyDown);
        
        // Clear shortcuts
        this.shortcuts.clear();
    }
}
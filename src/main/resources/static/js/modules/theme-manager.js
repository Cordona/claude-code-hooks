/**
 * Theme Manager
 * Handles theme switching and persistence
 */

import { logger } from '../utils/logger.js';
import { CONFIG } from '../config/constants.js';

/**
 * Theme Manager Class
 * Manages application themes and user preferences
 */
export class ThemeManager {
    constructor() {
        this.currentTheme = CONFIG.THEMES.SYSTEM;
        this.mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
        
        // Bind methods
        this.setTheme = this.setTheme.bind(this);
        this.handleSystemThemeChange = this.handleSystemThemeChange.bind(this);
    }

    /**
     * Initialize theme manager
     */
    async init() {
        logger.info('Initializing Theme Manager');
        
        // Load saved theme
        this.loadTheme();
        
        // Set up system theme monitoring
        this.setupSystemThemeMonitoring();
        
        // Apply initial theme
        this.applyTheme(this.currentTheme);
        
        // Update UI
        this.updateThemeButtons();
    }

    /**
     * Load theme from storage
     */
    loadTheme() {
        try {
            const savedTheme = localStorage.getItem(CONFIG.STORAGE.THEME);
            if (savedTheme && Object.values(CONFIG.THEMES).includes(savedTheme)) {
                this.currentTheme = savedTheme;
                logger.debug(`Loaded theme from storage: ${savedTheme}`);
            }
        } catch (error) {
            logger.error('Failed to load theme from storage:', error);
        }
    }

    /**
     * Save theme to storage
     */
    saveTheme() {
        try {
            localStorage.setItem(CONFIG.STORAGE.THEME, this.currentTheme);
        } catch (error) {
            logger.error('Failed to save theme to storage:', error);
        }
    }

    /**
     * Set theme
     * @param {string} theme - Theme name
     */
    setTheme(theme) {
        if (!Object.values(CONFIG.THEMES).includes(theme)) {
            logger.warn(`Invalid theme: ${theme}`);
            return;
        }

        logger.info(`Setting theme to: ${theme}`);
        
        this.currentTheme = theme;
        this.applyTheme(theme);
        this.updateThemeButtons();
        this.saveTheme();
        
        // Dispatch theme change event
        const event = new CustomEvent(CONFIG.EVENTS.CUSTOM.THEME_CHANGED, {
            detail: { theme }
        });
        document.dispatchEvent(event);
    }

    /**
     * Apply theme to document
     * @param {string} theme - Theme name
     */
    applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        
        // Update meta theme color
        this.updateMetaThemeColor(theme);
        
        logger.debug(`Applied theme: ${theme}`);
    }

    /**
     * Update meta theme color
     * @param {string} theme - Theme name
     */
    updateMetaThemeColor(theme) {
        const metaThemeColor = document.querySelector('meta[name="theme-color"]');
        if (metaThemeColor) {
            const colors = {
                [CONFIG.THEMES.LIGHT]: '#667eea',
                [CONFIG.THEMES.DARK]: '#8b9cf7',
                [CONFIG.THEMES.SYSTEM]: this.getSystemTheme() === 'dark' ? '#8b9cf7' : '#667eea'
            };
            
            metaThemeColor.setAttribute('content', colors[theme] || colors[CONFIG.THEMES.LIGHT]);
        }
    }

    /**
     * Update theme buttons
     */
    updateThemeButtons() {
        const buttons = document.querySelectorAll('.theme-selector__btn');
        buttons.forEach(button => {
            const buttonTheme = button.getAttribute('data-theme');
            const isActive = buttonTheme === this.currentTheme;
            
            button.classList.toggle('theme-selector__btn--active', isActive);
            button.setAttribute('aria-pressed', isActive.toString());
        });
    }

    /**
     * Set up system theme monitoring
     */
    setupSystemThemeMonitoring() {
        this.mediaQuery.addEventListener('change', this.handleSystemThemeChange);
    }

    /**
     * Handle system theme change
     */
    handleSystemThemeChange() {
        if (this.currentTheme === CONFIG.THEMES.SYSTEM) {
            // Re-apply system theme to pick up changes
            this.applyTheme(CONFIG.THEMES.SYSTEM);
            this.updateMetaThemeColor(CONFIG.THEMES.SYSTEM);
            
            logger.debug('System theme changed, updated application theme');
        }
    }

    /**
     * Get system theme preference
     * @returns {string} System theme ('light' or 'dark')
     */
    getSystemTheme() {
        return this.mediaQuery.matches ? 'dark' : 'light';
    }

    /**
     * Get current theme
     * @returns {string} Current theme
     */
    getCurrentTheme() {
        return this.currentTheme;
    }

    /**
     * Get effective theme (resolves system theme)
     * @returns {string} Effective theme
     */
    getEffectiveTheme() {
        if (this.currentTheme === CONFIG.THEMES.SYSTEM) {
            return this.getSystemTheme();
        }
        return this.currentTheme;
    }

    /**
     * Cleanup resources
     */
    cleanup() {
        logger.info('Cleaning up Theme Manager');
        
        // Remove event listeners
        this.mediaQuery.removeEventListener('change', this.handleSystemThemeChange);
        
        // Save current state
        this.saveTheme();
    }
}
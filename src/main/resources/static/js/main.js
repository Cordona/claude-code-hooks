/**
 * Claude Code Hooks Dashboard - Main Application Module
 * 
 * This module coordinates all application functionality including:
 * - Server-Sent Events management
 * - Notification handling
 * - Theme management
 * - UI interactions
 * - Audio feedback
 * 
 * @author Claude Code Hooks Team
 * @version 1.0.0
 */

import { SSEManager } from './modules/sse-manager.js';
import { NotificationManager } from './modules/notification-manager.js';
import { ThemeManager } from './modules/theme-manager.js';
import { AudioManager } from './modules/audio-manager.js';
import { UIManager } from './modules/ui-manager.js';
import { StatsManager } from './modules/stats-manager.js';
import { KeyboardManager } from './modules/keyboard-manager.js';
import { logger } from './utils/logger.js';
import { CONFIG } from './config/constants.js';

/**
 * Main Application Class
 * Orchestrates all application modules and manages their lifecycle
 */
class ClaudeCodeHooksApp {
    constructor() {
        this.modules = new Map();
        this.isInitialized = false;
        this.startTime = Date.now();
        
        // Bind methods to maintain context
        this.handleUnload = this.handleUnload.bind(this);
        this.handleVisibilityChange = this.handleVisibilityChange.bind(this);
        this.handleKeyboardShortcut = this.handleKeyboardShortcut.bind(this);
    }

    /**
     * Initialize the application
     * Sets up all modules in the correct order
     * @throws {Error} If application initialization fails
     */
    async init() {
        if (this.isInitialized) {
            logger.warn('Application already initialized');
            return;
        }

        try {
            logger.info('🚀 Initializing Claude Code Hooks Dashboard');
            
            // Initialize core modules
            await this.initializeModules();
            
            // Set up event listeners
            this.setupEventListeners();
            
            // Set up keyboard shortcuts
            this.setupKeyboardShortcuts();
            
            // Verify system health
            await this.verifySystemHealth();
            
            // Mark as initialized
            this.isInitialized = true;
            
            logger.info('✅ Application initialized successfully');
            
        } catch (error) {
            logger.error('❌ Failed to initialize application:', error);
            await this.handleInitializationError(error);
            throw error;
        }
    }

    /**
     * Initialize all application modules
     * Modules are initialized in dependency order
     * @throws {Error} If any module fails to initialize
     */
    async initializeModules() {
        const moduleConfigs = [
            { name: 'theme', class: ThemeManager, deps: [] },
            { name: 'audio', class: AudioManager, deps: [] },
            { name: 'notification', class: NotificationManager, deps: ['audio'] },
            { name: 'stats', class: StatsManager, deps: [] },
            { name: 'ui', class: UIManager, deps: ['theme', 'notification'] },
            { name: 'keyboard', class: KeyboardManager, deps: ['notification', 'ui'] },
            { name: 'sse', class: SSEManager, deps: ['notification', 'stats', 'ui'] }
        ];

        let initializedModules = [];
        
        try {
            for (const config of moduleConfigs) {
                const startTime = performance.now();
                
                await this.initializeModule(config);
                initializedModules.push(config.name);
                
                const duration = Math.round(performance.now() - startTime);
                logger.debug(`✅ Module '${config.name}' initialized (${duration}ms)`);
            }
            
            logger.info(`All ${moduleConfigs.length} modules initialized successfully`);
            
        } catch (error) {
            logger.error(`Module initialization failed. Initialized: [${initializedModules.join(', ')}]`);
            
            // Cleanup any partially initialized modules
            await this.cleanupModules(initializedModules);
            
            throw new Error(`Module initialization failed: ${error.message}`);
        }
    }

    /**
     * Initialize a single module
     * @param {Object} config - Module configuration
     * @throws {Error} If module initialization fails
     */
    async initializeModule(config) {
        const { name, class: ModuleClass, deps } = config;
        
        try {
            // Validate config
            if (!name || !ModuleClass) {
                throw new Error('Module config must have name and class properties');
            }
            
            // Get dependencies
            const dependencies = deps.reduce((acc, depName) => {
                const dep = this.modules.get(depName);
                if (!dep) {
                    throw new Error(`Dependency '${depName}' not found for module '${name}'`);
                }
                acc[depName] = dep;
                return acc;
            }, {});

            // Create module instance
            const module = new ModuleClass(dependencies);
            
            // Validate module interface
            if (typeof module.init !== 'function') {
                throw new Error(`Module '${name}' must implement init() method`);
            }
            
            // Initialize with timeout
            await Promise.race([
                module.init(),
                new Promise((_, reject) => 
                    setTimeout(() => reject(new Error('Module initialization timeout')), 
                    CONFIG.MODULE_INIT_TIMEOUT || 10000)
                )
            ]);
            
            this.modules.set(name, module);
            
            // Make module globally accessible for onclick handlers
            window[name + 'Manager'] = module;
            
            // Expose specific methods for inline event handlers
            this.exposeGlobalMethods(name, module);
            
            logger.debug(`Module '${name}' exposed globally as '${name}Manager'`);
            
        } catch (error) {
            throw new Error(`Failed to initialize module '${name}': ${error.message}`);
        }
    }

    /**
     * Set up global event listeners
     */
    setupEventListeners() {
        // Page lifecycle events
        window.addEventListener('beforeunload', this.handleUnload);
        document.addEventListener('visibilitychange', this.handleVisibilityChange);
        
        // Error handling
        window.addEventListener('error', this.handleGlobalError.bind(this));
        window.addEventListener('unhandledrejection', this.handleUnhandledRejection.bind(this));
        
        // Initialize audio on first user interaction
        this.setupAudioInitialization();
    }

    /**
     * Set up keyboard shortcuts
     */
    setupKeyboardShortcuts() {
        const keyboardManager = this.modules.get('keyboard');
        if (keyboardManager) {
            keyboardManager.registerShortcuts({
                'cmd+t': () => this.modules.get('notification')?.test(),
                'cmd+c': () => this.modules.get('notification')?.clearAll(),
                'cmd+h': () => this.modules.get('ui')?.toggleHelp(),
                'cmd+m': () => this.modules.get('ui')?.toggleMenu(),
                'escape': () => this.modules.get('ui')?.closeAll()
            });
        }
    }

    /**
     * Set up audio initialization on first user interaction
     */
    setupAudioInitialization() {
        const initAudio = () => {
            const audioManager = this.modules.get('audio');
            if (audioManager) {
                audioManager.initialize();
            }
            document.removeEventListener('click', initAudio);
            document.removeEventListener('keydown', initAudio);
        };

        document.addEventListener('click', initAudio, { once: true });
        document.addEventListener('keydown', initAudio, { once: true });
    }

    /**
     * Verify system health after initialization
     * @throws {Error} If critical systems are not healthy
     */
    async verifySystemHealth() {
        const criticalModules = ['theme', 'ui', 'notification'];
        
        for (const moduleName of criticalModules) {
            const module = this.modules.get(moduleName);
            if (!module) {
                throw new Error(`Critical module '${moduleName}' is not initialized`);
            }
            
            // Check module health if method exists
            if (typeof module.getStatus === 'function') {
                try {
                    const status = module.getStatus();
                    if (status && status.error) {
                        throw new Error(`Module '${moduleName}' health check failed: ${status.error}`);
                    }
                } catch (error) {
                    throw new Error(`Module '${moduleName}' health check failed: ${error.message}`);
                }
            }
        }
        
        logger.info('System health verification passed');
    }

    /**
     * Cleanup specific modules
     * @param {Array<string>} moduleNames - Names of modules to cleanup
     */
    async cleanupModules(moduleNames) {
        const cleanupPromises = moduleNames.map(async (name) => {
            const module = this.modules.get(name);
            if (module && typeof module.cleanup === 'function') {
                try {
                    if (module.cleanup.constructor.name === 'AsyncFunction') {
                        await module.cleanup();
                    } else {
                        module.cleanup();
                    }
                    logger.debug(`Cleaned up module '${name}'`);
                } catch (error) {
                    logger.error(`Error cleaning up module '${name}':`, error);
                }
            }
            this.modules.delete(name);
        });
        
        await Promise.allSettled(cleanupPromises);
    }

    /**
     * Handle page unload
     * Clean up resources and close connections
     */
    async handleUnload() {
        try {
            logger.info('🧹 Cleaning up application resources');
            
            // Get all module names in reverse order for cleanup
            const moduleNames = Array.from(this.modules.keys()).reverse();
            await this.cleanupModules(moduleNames);
            
            this.isInitialized = false;
            
        } catch (error) {
            logger.error('Error during application cleanup:', error);
        }
    }

    /**
     * Handle visibility change
     * Manage connections when page becomes hidden/visible
     */
    handleVisibilityChange() {
        const sseManager = this.modules.get('sse');
        if (!sseManager) return;

        if (document.hidden) {
            logger.debug('📱 Page hidden, maintaining connection');
            // Keep connection alive but reduce activity
        } else {
            logger.debug('📱 Page visible, checking connection');
            // Ensure connection is active
            sseManager.ensureConnection();
        }
    }

    /**
     * Handle keyboard shortcuts
     * @param {KeyboardEvent} event - Keyboard event
     */
    handleKeyboardShortcut(event) {
        const keyboardManager = this.modules.get('keyboard');
        if (keyboardManager) {
            keyboardManager.handleKeyDown(event);
        }
    }

    /**
     * Handle global JavaScript errors
     * @param {ErrorEvent} event - Error event
     */
    handleGlobalError(event) {
        logger.error('Global error:', {
            message: event.message,
            filename: event.filename,
            lineno: event.lineno,
            colno: event.colno,
            error: event.error
        });
    }

    /**
     * Handle unhandled promise rejections
     * @param {PromiseRejectionEvent} event - Promise rejection event
     */
    handleUnhandledRejection(event) {
        logger.error('Unhandled promise rejection:', event.reason);
    }

    /**
     * Handle initialization errors
     * @param {Error} error - Initialization error
     */
    async handleInitializationError(error) {
        try {
            logger.error('Application initialization failed:', error);
            
            // Create error UI
            const errorContainer = document.createElement('div');
            errorContainer.style.cssText = `
                position: fixed;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background: rgba(0, 0, 0, 0.9);
                display: flex;
                align-items: center;
                justify-content: center;
                z-index: 10000;
                font-family: system-ui, -apple-system, sans-serif;
            `;
            
            errorContainer.innerHTML = `
                <div style="
                    background: white;
                    border-radius: 12px;
                    padding: 2rem;
                    max-width: 500px;
                    margin: 1rem;
                    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
                    text-align: center;
                ">
                    <div style="
                        width: 64px;
                        height: 64px;
                        background: #ef4444;
                        border-radius: 50%;
                        margin: 0 auto 1.5rem;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        font-size: 2rem;
                    ">
                        ❌
                    </div>
                    <h2 style="margin: 0 0 1rem 0; color: #1f2937; font-size: 1.5rem;">
                        Application Failed to Start
                    </h2>
                    <p style="margin: 0 0 1.5rem 0; color: #6b7280; line-height: 1.5;">
                        Claude Code Hooks Dashboard encountered an error during initialization. 
                        This may be due to network connectivity or browser compatibility issues.
                    </p>
                    <details style="margin: 0 0 1.5rem 0; text-align: left;">
                        <summary style="cursor: pointer; color: #6b7280; margin-bottom: 0.5rem;">Error Details</summary>
                        <pre style="
                            background: #f3f4f6;
                            padding: 1rem;
                            border-radius: 6px;
                            font-size: 0.8rem;
                            color: #374151;
                            overflow: auto;
                            white-space: pre-wrap;
                        ">${error.message}</pre>
                    </details>
                    <div style="display: flex; gap: 1rem; justify-content: center;">
                        <button onclick="location.reload()" style="
                            background: #ef4444;
                            color: white;
                            border: none;
                            padding: 0.75rem 1.5rem;
                            border-radius: 6px;
                            cursor: pointer;
                            font-weight: 500;
                            font-size: 0.9rem;
                        ">
                            Reload Page
                        </button>
                        <button onclick="console.error('Application Error:', ${JSON.stringify(error.stack || error.message)})" style="
                            background: #6b7280;
                            color: white;
                            border: none;
                            padding: 0.75rem 1.5rem;
                            border-radius: 6px;
                            cursor: pointer;
                            font-weight: 500;
                            font-size: 0.9rem;
                        ">
                            Log to Console
                        </button>
                    </div>
                </div>
            `;
            
            document.body.appendChild(errorContainer);
            
        } catch (uiError) {
            // Fallback if UI creation fails
            logger.error('Failed to show error UI:', uiError);
            alert(`Application initialization failed: ${error.message}\n\nPlease refresh the page.`);
        }
    }

    /**
     * Get application statistics
     * @returns {Object} Application statistics
     */
    getStats() {
        const statsManager = this.modules.get('stats');
        return {
            uptime: Date.now() - this.startTime,
            modules: Array.from(this.modules.keys()),
            isInitialized: this.isInitialized,
            ...statsManager?.getStats()
        };
    }

    /**
     * Get module by name
     * @param {string} name - Module name
     * @returns {Object|null} Module instance or null if not found
     */
    getModule(name) {
        return this.modules.get(name) || null;
    }

    /**
     * Expose specific module methods globally for inline event handlers
     * @param {string} moduleName - Name of the module
     * @param {Object} module - Module instance
     */
    exposeGlobalMethods(moduleName, module) {
        // The HTML uses module manager references like themeManager.setTheme()
        // So we ensure the manager objects have the correct methods accessible
        
        // No additional global methods needed since window[name + 'Manager'] = module
        // already exposes themeManager.setTheme(), notificationManager.test(), etc.
        
        // But we can add some convenience global functions if needed
        if (moduleName === 'notification') {
            // Add global test functions for backward compatibility
            window.testNotification = async () => {
                try {
                    await module.test();
                } catch (error) {
                    console.error('Test notification failed:', error);
                }
            };
            
            window.clearNotifications = () => {
                try {
                    module.clearAll();
                } catch (error) {
                    console.error('Clear notifications failed:', error);
                }
            };
        }
        
        // Audio test function
        if (moduleName === 'audio') {
            window.playTestSound = async () => {
                try {
                    await module.initialize(); // Ensure audio is initialized
                    await module.playNotificationSound();
                } catch (error) {
                    console.error('Test sound failed:', error);
                }
            };
        }
    }

    /**
     * Get application status for debugging
     * @returns {Object} Application status
     */
    getStatus() {
        return {
            initialized: this.isInitialized,
            startTime: this.startTime,
            uptime: Date.now() - this.startTime,
            moduleCount: this.modules.size,
            modules: Array.from(this.modules.keys())
        };
    }
}

// Initialize application when DOM is ready
document.addEventListener('DOMContentLoaded', async () => {
    try {
        // Create global app instance
        window.app = new ClaudeCodeHooksApp();
        
        // Initialize application
        await window.app.init();
        
        // Debug: Log what's available globally
        logger.debug('Global objects available:', {
            themeManager: !!window.themeManager,
            notificationManager: !!window.notificationManager,
            uiManager: !!window.uiManager,
            audioManager: !!window.audioManager
        });
        
        logger.info('🎉 Claude Code Hooks Dashboard is ready!');
        
    } catch (error) {
        logger.error('Failed to start application:', error);
    }
});

// Export for testing
export default ClaudeCodeHooksApp;
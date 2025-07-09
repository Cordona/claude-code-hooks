/**
 * Stats Manager
 * Handles application statistics and metrics
 */

import { logger } from '../utils/logger.js';
import { CONFIG } from '../config/constants.js';

/**
 * Stats Manager Class
 * Manages application statistics and performance metrics
 */
export class StatsManager {
    constructor() {
        this.stats = {
            uptime: 0,
            connectionCount: 0,
            eventCount: 0,
            errorCount: 0,
            startTime: Date.now(),
            lastUpdate: Date.now()
        };
        
        this.updateInterval = null;
        
        // Bind methods
        this.updateStats = this.updateStats.bind(this);
        this.updateConnectionCount = this.updateConnectionCount.bind(this);
        this.incrementEventCount = this.incrementEventCount.bind(this);
        this.incrementErrorCount = this.incrementErrorCount.bind(this);
    }

    /**
     * Initialize stats manager
     */
    async init() {
        logger.info('Initializing Stats Manager');
        
        // Load stored stats
        this.loadStats();
        
        // Start update interval
        this.startUpdateInterval();
        
        // Initial UI update
        this.updateUI();
    }

    /**
     * Load stats from storage
     */
    loadStats() {
        try {
            const stored = localStorage.getItem(CONFIG.STORAGE.STATS);
            if (stored) {
                const storedStats = JSON.parse(stored);
                // Only load persistent stats
                this.stats.eventCount = storedStats.eventCount || 0;
                this.stats.errorCount = storedStats.errorCount || 0;
                logger.debug('Loaded stats from storage');
            }
        } catch (error) {
            logger.error('Failed to load stats from storage:', error);
        }
    }

    /**
     * Save stats to storage
     */
    saveStats() {
        try {
            const persistentStats = {
                eventCount: this.stats.eventCount,
                errorCount: this.stats.errorCount,
                lastSaved: Date.now()
            };
            localStorage.setItem(CONFIG.STORAGE.STATS, JSON.stringify(persistentStats));
        } catch (error) {
            logger.error('Failed to save stats to storage:', error);
        }
    }

    /**
     * Start update interval
     */
    startUpdateInterval() {
        this.updateInterval = setInterval(() => {
            this.updateStats();
            this.updateUI();
        }, 1000);
    }

    /**
     * Stop update interval
     */
    stopUpdateInterval() {
        if (this.updateInterval) {
            clearInterval(this.updateInterval);
            this.updateInterval = null;
        }
    }

    /**
     * Update stats
     */
    updateStats() {
        const now = Date.now();
        this.stats.uptime = now - this.stats.startTime;
        this.stats.lastUpdate = now;
        
        // Dispatch stats update event
        const event = new CustomEvent(CONFIG.EVENTS.CUSTOM.STATS_UPDATED, {
            detail: this.getStats()
        });
        document.dispatchEvent(event);
    }

    /**
     * Update connection count
     * @param {number} count - Connection count
     */
    updateConnectionCount(count) {
        this.stats.connectionCount = count;
        this.updateUI();
    }

    /**
     * Increment event count
     */
    incrementEventCount() {
        this.stats.eventCount++;
        this.saveStats();
    }

    /**
     * Increment error count
     */
    incrementErrorCount() {
        this.stats.errorCount++;
        this.saveStats();
    }

    /**
     * Update UI elements
     */
    updateUI() {
        // Update active streams count
        const activeStreamsElement = document.getElementById('activeStreamsCount');
        if (activeStreamsElement) {
            activeStreamsElement.textContent = this.stats.connectionCount;
        }
        
        // Update uptime display
        const uptimeElement = document.getElementById('uptimeDisplay');
        if (uptimeElement) {
            uptimeElement.textContent = this.formatUptime(this.stats.uptime);
        }
        
        // Update notifications title
        const notificationsTitle = document.getElementById('notificationsTitle');
        if (notificationsTitle) {
            notificationsTitle.textContent = `Recent Notifications (${this.stats.eventCount} total)`;
        }
    }

    /**
     * Format uptime for display
     * @param {number} ms - Uptime in milliseconds
     * @returns {string} Formatted uptime
     */
    formatUptime(ms) {
        const seconds = Math.floor(ms / 1000);
        const hours = Math.floor(seconds / 3600);
        const minutes = Math.floor((seconds % 3600) / 60);
        const secs = seconds % 60;
        
        return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }

    /**
     * Get current stats
     * @returns {Object} Current statistics
     */
    getStats() {
        return {
            ...this.stats,
            uptimeFormatted: this.formatUptime(this.stats.uptime)
        };
    }

    /**
     * Reset stats
     */
    resetStats() {
        logger.info('Resetting statistics');
        
        this.stats = {
            uptime: 0,
            connectionCount: 0,
            eventCount: 0,
            errorCount: 0,
            startTime: Date.now(),
            lastUpdate: Date.now()
        };
        
        this.saveStats();
        this.updateUI();
    }

    /**
     * Get performance metrics
     * @returns {Object} Performance metrics
     */
    getPerformanceMetrics() {
        const performance = window.performance;
        const navigation = performance.getEntriesByType('navigation')[0];
        
        return {
            domContentLoaded: navigation?.domContentLoadedEventEnd - navigation?.domContentLoadedEventStart,
            loadComplete: navigation?.loadEventEnd - navigation?.loadEventStart,
            firstPaint: performance.getEntriesByType('paint').find(entry => entry.name === 'first-paint')?.startTime,
            firstContentfulPaint: performance.getEntriesByType('paint').find(entry => entry.name === 'first-contentful-paint')?.startTime,
            memoryUsage: performance.memory ? {
                used: performance.memory.usedJSHeapSize,
                total: performance.memory.totalJSHeapSize,
                limit: performance.memory.jsHeapSizeLimit
            } : null
        };
    }

    /**
     * Export stats data
     * @returns {Object} Exportable stats data
     */
    exportStats() {
        return {
            ...this.getStats(),
            performance: this.getPerformanceMetrics(),
            exportedAt: new Date().toISOString()
        };
    }

    /**
     * Cleanup resources
     */
    cleanup() {
        logger.info('Cleaning up Stats Manager');
        
        // Stop update interval
        this.stopUpdateInterval();
        
        // Save final stats
        this.saveStats();
    }
}
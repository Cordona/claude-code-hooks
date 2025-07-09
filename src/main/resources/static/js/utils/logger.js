/**
 * Logger Utility
 * Provides structured logging with different levels and formatting
 */

const LOG_LEVELS = {
    ERROR: 0,
    WARN: 1,
    INFO: 2,
    DEBUG: 3
};

class Logger {
    constructor(level = LOG_LEVELS.INFO) {
        this.level = level;
        this.prefix = '[Claude Code Hooks]';
    }

    /**
     * Set log level
     * @param {number} level - Log level
     */
    setLevel(level) {
        this.level = level;
    }

    /**
     * Log error message
     * @param {string} message - Error message
     * @param {...any} args - Additional arguments
     */
    error(message, ...args) {
        if (this.level >= LOG_LEVELS.ERROR) {
            console.error(`${this.prefix} ❌ ${message}`, ...args);
        }
    }

    /**
     * Log warning message
     * @param {string} message - Warning message
     * @param {...any} args - Additional arguments
     */
    warn(message, ...args) {
        if (this.level >= LOG_LEVELS.WARN) {
            console.warn(`${this.prefix} ⚠️  ${message}`, ...args);
        }
    }

    /**
     * Log info message
     * @param {string} message - Info message
     * @param {...any} args - Additional arguments
     */
    info(message, ...args) {
        if (this.level >= LOG_LEVELS.INFO) {
            console.info(`${this.prefix} ℹ️  ${message}`, ...args);
        }
    }

    /**
     * Log debug message
     * @param {string} message - Debug message
     * @param {...any} args - Additional arguments
     */
    debug(message, ...args) {
        if (this.level >= LOG_LEVELS.DEBUG) {
            console.debug(`${this.prefix} 🔍 ${message}`, ...args);
        }
    }

    /**
     * Log with custom level
     * @param {string} level - Log level name
     * @param {string} message - Log message
     * @param {...any} args - Additional arguments
     */
    log(level, message, ...args) {
        const method = this[level.toLowerCase()];
        if (method) {
            method.call(this, message, ...args);
        }
    }
}

// Create default logger instance
const logger = new Logger(
    process?.env?.NODE_ENV === 'development' ? LOG_LEVELS.DEBUG : LOG_LEVELS.INFO
);

export { logger, Logger, LOG_LEVELS };
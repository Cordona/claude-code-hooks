/**
 * Application Configuration Constants
 * Centralized configuration for the Claude Code Hooks Dashboard
 */

export const CONFIG = {
    // API Configuration
    API: {
        BASE_URL: '',
        ENDPOINTS: {
            SSE_STREAM: '/api/v1/claude-code/hooks/events/stream',
            NOTIFICATION: '/api/v1/claude-code/hooks/notification/event',
            STOP: '/api/v1/claude-code/hooks/stop/event',
            HEALTH: '/actuator/health'
        }
    },

    // SSE Configuration
    SSE: {
        RECONNECT_INTERVAL: 5000,
        MAX_RECONNECT_ATTEMPTS: 10,
        RECONNECT_BACKOFF_MULTIPLIER: 1.5,
        INITIAL_RECONNECT_DELAY: 1000,
        CONNECTION_TIMEOUT: 30000
    },

    // Notification Configuration
    NOTIFICATIONS: {
        MAX_HISTORY: 100,
        DEFAULT_DURATION: 5000,
        PERMISSION_CHECK_INTERVAL: 1000,
        RETRY_PERMISSION_DELAY: 2000
    },

    // Audio Configuration
    AUDIO: {
        FREQUENCY: 2093, // C7 note frequency
        DURATION: 1200, // milliseconds
        VOLUME: 0.3,
        FADE_OUT_DURATION: 0.8,
        CONTEXT_RESUME_TIMEOUT: 5000
    },

    // Theme Configuration
    THEMES: {
        LIGHT: 'light',
        DARK: 'dark',
        SYSTEM: 'system'
    },

    // UI Configuration
    UI: {
        ANIMATION_DURATION: 300,
        TOOLTIP_DELAY: 500,
        MENU_CLOSE_DELAY: 150,
        SCROLL_THRESHOLD: 50,
        DEBOUNCE_DELAY: 250
    },

    // Keyboard Shortcuts
    SHORTCUTS: {
        'cmd+t': 'Test Notification',
        'cmd+c': 'Clear History',
        'cmd+h': 'Toggle Help',
        'cmd+m': 'Toggle Menu',
        'escape': 'Close All'
    },

    // Storage Keys
    STORAGE: {
        THEME: 'claude-hooks-theme',
        NOTIFICATIONS: 'claude-hooks-notifications',
        SETTINGS: 'claude-hooks-settings',
        STATS: 'claude-hooks-stats'
    },

    // Event Names
    EVENTS: {
        SSE: {
            CONNECTED: 'connected',
            CLAUDE_HOOK: 'claude-hook',
            ERROR: 'error',
            CLOSE: 'close'
        },
        CUSTOM: {
            THEME_CHANGED: 'theme-changed',
            NOTIFICATION_RECEIVED: 'notification-received',
            CONNECTION_STATUS_CHANGED: 'connection-status-changed',
            STATS_UPDATED: 'stats-updated'
        }
    },

    // Error Messages
    ERRORS: {
        SSE_CONNECTION_FAILED: 'Failed to establish SSE connection',
        NOTIFICATION_PERMISSION_DENIED: 'Browser notifications are blocked',
        AUDIO_CONTEXT_FAILED: 'Failed to initialize audio context',
        THEME_LOAD_FAILED: 'Failed to load theme preferences',
        STORAGE_QUOTA_EXCEEDED: 'Storage quota exceeded'
    },

    // Success Messages
    SUCCESS: {
        SSE_CONNECTED: 'Successfully connected to event stream',
        NOTIFICATION_PERMISSION_GRANTED: 'Browser notifications enabled',
        AUDIO_INITIALIZED: 'Audio system initialized',
        THEME_APPLIED: 'Theme applied successfully'
    },

    // Animation Timings
    ANIMATIONS: {
        FADE_IN: 'fadeIn 0.3s ease-out',
        FADE_OUT: 'fadeOut 0.3s ease-in',
        SLIDE_IN: 'slideIn 0.3s ease-out',
        SLIDE_OUT: 'slideOut 0.3s ease-in',
        SCALE_IN: 'scaleIn 0.2s ease-out',
        SCALE_OUT: 'scaleOut 0.2s ease-in'
    },

    // Validation Rules
    VALIDATION: {
        MESSAGE_MAX_LENGTH: 500,
        TITLE_MAX_LENGTH: 100,
        URL_PATTERN: /^https?:\/\/.+/,
        EMAIL_PATTERN: /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    },

    // Performance Thresholds
    PERFORMANCE: {
        RENDER_BUDGET: 16, // 60 FPS
        LARGE_LIST_THRESHOLD: 50,
        VIRTUAL_SCROLL_THRESHOLD: 100,
        DEBOUNCE_SEARCH: 300
    },

    // Module Configuration
    MODULE_INIT_TIMEOUT: 10000, // 10 seconds
    MODULE_HEALTH_CHECK_TIMEOUT: 5000 // 5 seconds
};

// Feature flags for progressive enhancement
export const FEATURES = {
    VIRTUAL_SCROLLING: true,
    OFFLINE_SUPPORT: true,
    PUSH_NOTIFICATIONS: true,
    ACCESSIBILITY_ENHANCEMENTS: true,
    PERFORMANCE_MONITORING: true,
    ERROR_REPORTING: true
};

// Browser capability detection
export const CAPABILITIES = {
    NOTIFICATIONS: 'Notification' in window,
    SSE: 'EventSource' in window,
    AUDIO: 'AudioContext' in window || 'webkitAudioContext' in window,
    LOCAL_STORAGE: 'localStorage' in window,
    INTERSECTION_OBSERVER: 'IntersectionObserver' in window,
    RESIZE_OBSERVER: 'ResizeObserver' in window,
    WEB_WORKERS: 'Worker' in window,
    SERVICE_WORKERS: 'serviceWorker' in navigator
};

// Device detection
export const DEVICE = {
    IS_MOBILE: /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent),
    IS_TABLET: /iPad|Android/i.test(navigator.userAgent),
    IS_DESKTOP: !/Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent),
    IS_TOUCH: 'ontouchstart' in window || navigator.maxTouchPoints > 0,
    PREFERS_REDUCED_MOTION: window.matchMedia('(prefers-reduced-motion: reduce)').matches
};

// Environment detection
export const ENV = {
    IS_DEVELOPMENT: window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1',
    IS_PRODUCTION: window.location.protocol === 'https:' && !window.location.hostname.includes('localhost'),
    IS_HTTPS: window.location.protocol === 'https:'
};
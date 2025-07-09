/**
 * Audio Manager
 * Handles audio feedback and sound effects
 */

import { logger } from '../utils/logger.js';
import { CONFIG, CAPABILITIES } from '../config/constants.js';

/**
 * Audio Manager Class
 * Manages audio context and notification sounds
 */
export class AudioManager {
    constructor() {
        this.audioContext = null;
        this.isInitialized = false;
        this.isEnabled = true;
        
        // Bind methods
        this.initialize = this.initialize.bind(this);
        this.resumeContext = this.resumeContext.bind(this);
        this.playNotificationSound = this.playNotificationSound.bind(this);
    }

    /**
     * Initialize audio manager
     */
    async init() {
        logger.info('Initializing Audio Manager');
        
        // Check browser support
        if (!CAPABILITIES.AUDIO) {
            logger.warn('Web Audio API not supported');
            return;
        }

        // Audio will be initialized on first user interaction
        logger.debug('Audio manager ready - waiting for user interaction');
    }

    /**
     * Initialize audio context
     * Must be called after user interaction
     */
    async initialize() {
        if (this.isInitialized || !CAPABILITIES.AUDIO) {
            return;
        }

        try {
            // Create audio context
            const AudioContext = window.AudioContext || window.webkitAudioContext;
            this.audioContext = new AudioContext();
            
            this.isInitialized = true;
            logger.info('✅ Audio context initialized');
            
            // Resume context if suspended
            await this.resumeContext();
            
        } catch (error) {
            logger.error('Failed to initialize audio context:', error);
            this.isInitialized = false;
        }
    }

    /**
     * Resume audio context
     */
    async resumeContext() {
        if (!this.audioContext) return;

        try {
            if (this.audioContext.state === 'suspended') {
                await this.audioContext.resume();
                logger.debug('Audio context resumed');
            }
        } catch (error) {
            logger.error('Failed to resume audio context:', error);
        }
    }

    /**
     * Play notification sound
     */
    async playNotificationSound() {
        if (!this.isEnabled || !this.audioContext || !this.isInitialized) {
            logger.debug('Audio not available or disabled');
            return;
        }

        try {
            // Ensure context is running
            await this.resumeContext();
            
            if (this.audioContext.state !== 'running') {
                logger.debug('Audio context not running');
                return;
            }

            // Create oscillator for glass sound
            const oscillator = this.audioContext.createOscillator();
            const gainNode = this.audioContext.createGain();
            const filterNode = this.audioContext.createBiquadFilter();
            
            // Configure oscillator
            oscillator.frequency.setValueAtTime(CONFIG.AUDIO.FREQUENCY, this.audioContext.currentTime);
            oscillator.type = 'sine';
            
            // Configure filter for glass-like sound
            filterNode.type = 'bandpass';
            filterNode.frequency.setValueAtTime(CONFIG.AUDIO.FREQUENCY, this.audioContext.currentTime);
            filterNode.Q.setValueAtTime(10, this.audioContext.currentTime);
            
            // Configure gain (volume and fade out)
            gainNode.gain.setValueAtTime(CONFIG.AUDIO.VOLUME, this.audioContext.currentTime);
            gainNode.gain.exponentialRampToValueAtTime(0.001, this.audioContext.currentTime + CONFIG.AUDIO.FADE_OUT_DURATION);
            
            // Connect audio nodes
            oscillator.connect(filterNode);
            filterNode.connect(gainNode);
            gainNode.connect(this.audioContext.destination);
            
            // Play sound
            oscillator.start(this.audioContext.currentTime);
            oscillator.stop(this.audioContext.currentTime + CONFIG.AUDIO.DURATION / 1000);
            
            logger.debug('🔊 Notification sound played');
            
        } catch (error) {
            logger.error('Failed to play notification sound:', error);
        }
    }

    /**
     * Enable audio
     */
    enable() {
        this.isEnabled = true;
        logger.info('Audio enabled');
    }

    /**
     * Disable audio
     */
    disable() {
        this.isEnabled = false;
        logger.info('Audio disabled');
    }

    /**
     * Toggle audio
     * @returns {boolean} New enabled state
     */
    toggle() {
        this.isEnabled = !this.isEnabled;
        logger.info(`Audio ${this.isEnabled ? 'enabled' : 'disabled'}`);
        return this.isEnabled;
    }

    /**
     * Check if audio is enabled
     * @returns {boolean} True if enabled
     */
    isAudioEnabled() {
        return this.isEnabled;
    }

    /**
     * Get audio context state
     * @returns {string} Audio context state
     */
    getState() {
        return this.audioContext?.state || 'closed';
    }

    /**
     * Get audio manager status
     * @returns {Object} Status object
     */
    getStatus() {
        return {
            initialized: this.isInitialized,
            enabled: this.isEnabled,
            supported: CAPABILITIES.AUDIO,
            contextState: this.getState()
        };
    }

    /**
     * Cleanup resources
     */
    cleanup() {
        logger.info('Cleaning up Audio Manager');
        
        if (this.audioContext) {
            this.audioContext.close();
            this.audioContext = null;
        }
        
        this.isInitialized = false;
    }
}
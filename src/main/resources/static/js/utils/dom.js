/**
 * DOM Utility Functions
 * Provides helper functions for DOM manipulation and queries
 */

import { logger } from './logger.js';

/**
 * Query selector with error handling
 * @param {string} selector - CSS selector
 * @param {Element} context - Context element (default: document)
 * @returns {Element|null} Element or null if not found
 */
export function $(selector, context = document) {
    try {
        return context.querySelector(selector);
    } catch (error) {
        logger.error(`Invalid selector: ${selector}`, error);
        return null;
    }
}

/**
 * Query selector all with error handling
 * @param {string} selector - CSS selector
 * @param {Element} context - Context element (default: document)
 * @returns {NodeList} NodeList of elements
 */
export function $$(selector, context = document) {
    try {
        return context.querySelectorAll(selector);
    } catch (error) {
        logger.error(`Invalid selector: ${selector}`, error);
        return [];
    }
}

/**
 * Create element with attributes and children
 * @param {string} tag - HTML tag name
 * @param {Object} attributes - Element attributes
 * @param {...(string|Element)} children - Child elements or text
 * @returns {Element} Created element
 */
export function createElement(tag, attributes = {}, ...children) {
    const element = document.createElement(tag);
    
    // Set attributes
    Object.entries(attributes).forEach(([key, value]) => {
        if (key === 'className') {
            element.className = value;
        } else if (key === 'innerHTML') {
            element.innerHTML = value;
        } else if (key === 'textContent') {
            element.textContent = value;
        } else if (key.startsWith('on') && typeof value === 'function') {
            element.addEventListener(key.substring(2).toLowerCase(), value);
        } else {
            element.setAttribute(key, value);
        }
    });
    
    // Append children
    children.forEach(child => {
        if (typeof child === 'string') {
            element.appendChild(document.createTextNode(child));
        } else if (child instanceof Element) {
            element.appendChild(child);
        }
    });
    
    return element;
}

/**
 * Add class to element
 * @param {Element} element - Target element
 * @param {string} className - Class name to add
 */
export function addClass(element, className) {
    if (element && className) {
        element.classList.add(className);
    }
}

/**
 * Remove class from element
 * @param {Element} element - Target element
 * @param {string} className - Class name to remove
 */
export function removeClass(element, className) {
    if (element && className) {
        element.classList.remove(className);
    }
}

/**
 * Toggle class on element
 * @param {Element} element - Target element
 * @param {string} className - Class name to toggle
 * @returns {boolean} True if class was added, false if removed
 */
export function toggleClass(element, className) {
    if (element && className) {
        return element.classList.toggle(className);
    }
    return false;
}

/**
 * Check if element has class
 * @param {Element} element - Target element
 * @param {string} className - Class name to check
 * @returns {boolean} True if element has class
 */
export function hasClass(element, className) {
    return element && className && element.classList.contains(className);
}

/**
 * Set multiple attributes on element
 * @param {Element} element - Target element
 * @param {Object} attributes - Attributes to set
 */
export function setAttributes(element, attributes) {
    if (element && attributes) {
        Object.entries(attributes).forEach(([key, value]) => {
            element.setAttribute(key, value);
        });
    }
}

/**
 * Get element position relative to viewport
 * @param {Element} element - Target element
 * @returns {Object} Position object with x, y, width, height
 */
export function getElementPosition(element) {
    if (!element) return { x: 0, y: 0, width: 0, height: 0 };
    
    const rect = element.getBoundingClientRect();
    return {
        x: rect.left,
        y: rect.top,
        width: rect.width,
        height: rect.height
    };
}

/**
 * Check if element is visible in viewport
 * @param {Element} element - Target element
 * @returns {boolean} True if element is visible
 */
export function isElementVisible(element) {
    if (!element) return false;
    
    const rect = element.getBoundingClientRect();
    const windowHeight = window.innerHeight || document.documentElement.clientHeight;
    const windowWidth = window.innerWidth || document.documentElement.clientWidth;
    
    return rect.top < windowHeight && 
           rect.bottom > 0 && 
           rect.left < windowWidth && 
           rect.right > 0;
}

/**
 * Scroll element into view smoothly
 * @param {Element} element - Target element
 * @param {Object} options - Scroll options
 */
export function scrollIntoView(element, options = {}) {
    if (!element) return;
    
    const defaultOptions = {
        behavior: 'smooth',
        block: 'nearest',
        inline: 'nearest'
    };
    
    element.scrollIntoView({ ...defaultOptions, ...options });
}

/**
 * Get element's computed style property
 * @param {Element} element - Target element
 * @param {string} property - CSS property name
 * @returns {string} Computed style value
 */
export function getComputedStyle(element, property) {
    if (!element || !property) return '';
    
    return window.getComputedStyle(element).getPropertyValue(property);
}

/**
 * Set CSS custom property
 * @param {Element} element - Target element
 * @param {string} property - CSS custom property name
 * @param {string} value - Property value
 */
export function setCSSProperty(element, property, value) {
    if (!element || !property) return;
    
    element.style.setProperty(property, value);
}

/**
 * Get CSS custom property value
 * @param {Element} element - Target element
 * @param {string} property - CSS custom property name
 * @returns {string} Property value
 */
export function getCSSProperty(element, property) {
    if (!element || !property) return '';
    
    return getComputedStyle(element, property).trim();
}

/**
 * Animate element with CSS transitions
 * @param {Element} element - Target element
 * @param {Object} styles - CSS styles to animate to
 * @param {number} duration - Animation duration in ms
 * @returns {Promise} Promise that resolves when animation completes
 */
export function animateElement(element, styles, duration = 300) {
    return new Promise((resolve) => {
        if (!element) {
            resolve();
            return;
        }
        
        // Set transition
        element.style.transition = `all ${duration}ms ease-out`;
        
        // Apply styles
        Object.entries(styles).forEach(([property, value]) => {
            element.style[property] = value;
        });
        
        // Remove transition after animation
        setTimeout(() => {
            element.style.transition = '';
            resolve();
        }, duration);
    });
}

/**
 * Debounce function calls
 * @param {Function} func - Function to debounce
 * @param {number} wait - Wait time in ms
 * @returns {Function} Debounced function
 */
export function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

/**
 * Throttle function calls
 * @param {Function} func - Function to throttle
 * @param {number} limit - Time limit in ms
 * @returns {Function} Throttled function
 */
export function throttle(func, limit) {
    let inThrottle;
    return function(...args) {
        if (!inThrottle) {
            func.apply(this, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
}

/**
 * Wait for DOM to be ready
 * @returns {Promise} Promise that resolves when DOM is ready
 */
export function domReady() {
    return new Promise((resolve) => {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', resolve);
        } else {
            resolve();
        }
    });
}

/**
 * Create document fragment from HTML string
 * @param {string} html - HTML string
 * @returns {DocumentFragment} Document fragment
 */
export function createFragment(html) {
    const template = document.createElement('template');
    template.innerHTML = html;
    return template.content;
}

/**
 * Safe HTML insertion that prevents XSS
 * @param {Element} element - Target element
 * @param {string} html - HTML string to insert
 * @param {string} position - Insert position (beforebegin, afterbegin, beforeend, afterend)
 */
export function safeInsertHTML(element, html, position = 'beforeend') {
    if (!element || !html) return;
    
    // Create a temporary element to sanitize HTML
    const temp = document.createElement('div');
    temp.innerHTML = html;
    
    // Insert sanitized content
    element.insertAdjacentElement(position, temp.firstElementChild);
}
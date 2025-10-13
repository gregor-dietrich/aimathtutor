/**
 * Graspable Math Integration
 * Initializes and manages the Graspable Math canvas
 */

console.log('[GM] Script loaded');

// Global initialization function
window.initializeGraspableMath = function() {
    console.log('[GM] Starting initialization...');

    // Check if canvas element exists in DOM
    var canvasElement = document.getElementById('graspable-canvas');
    if (!canvasElement) {
        console.error('[GM] Canvas element not found in DOM');
        return;
    }

    // If canvas already exists, clear it and reinitialize
    if (window.graspableCanvas) {
        console.log('[GM] Canvas already exists, clearing for reinitialization...');
        try {
            // Clear the old canvas
            window.graspableCanvas = null;
            canvasElement.innerHTML = '';
        } catch (e) {
            console.error('[GM] Error clearing old canvas:', e);
        }
    }

    // Load Graspable Math library directly (not the inject script)
    if (!window.gmath) {
        console.log('[GM] Loading Graspable Math library (gmath)...');

        // Load the actual library script
        var script = document.createElement('script');
        script.src = 'https://graspablemath.com/shared/libs/gmath-dist/gmath-3.5.13.min.js';
        script.onload = function() {
            console.log('[GM] Library loaded, initializing canvas...');
            setTimeout(initializeCanvas, 500);
        };
        script.onerror = function() {
            console.error('[GM] Failed to load library');
            initializeWithIframe();
        };
        document.head.appendChild(script);
    } else {
        console.log('[GM] Library already available');
        setTimeout(initializeCanvas, 100);
    }
};

function initializeCanvas() {
    console.log('[GM] Initializing canvas...');

    // The library exposes 'gmath' globally
    if (!window.gmath) {
        console.error('[GM] gmath not found!');
        return;
    }

    console.log('[GM] gmath API found:', window.gmath);

    var canvasElement = document.getElementById('graspable-canvas');
    if (!canvasElement) {
        console.error('[GM] Canvas element not found!');
        return;
    }

    try {
        // Check if dark theme is active and set it BEFORE creating canvas
        var isDarkMode = document.documentElement.hasAttribute('theme') && 
                         document.documentElement.getAttribute('theme').includes('dark');
        
        if (isDarkMode && window.gmath.setDarkTheme) {
            console.log('[GM] Setting dark theme');
            window.gmath.setDarkTheme(true);
        }

        // Create canvas using gmath.Canvas with minimal toolbar
        var canvas = new window.gmath.Canvas(canvasElement, {
            use_fade_effects: true,
            use_property_effect: true,
            use_toolbar: false,  // We're using our own buttons in the Vaadin view
            vertical_scroll: true,
            horizontal_scroll: false
        });

        window.graspableCanvas = canvas;
        console.log('[GM] Canvas created successfully');
        console.log('[GM] Canvas initialization complete! Ready for problems.');
    } catch (error) {
        console.error('[GM] Error creating canvas:', error);
        console.error('[GM] Error details:', error.stack);
    }
}

function initializeWithIframe() {
    console.log('[GM] Using iframe fallback...');

    var canvasElement = document.getElementById('graspable-canvas');
    if (!canvasElement) {
        console.error('[GM] Canvas element not found!');
        return;
    }

    // Clear any existing content
    canvasElement.innerHTML = '';

    // Create iframe with Graspable Math widget
    var iframe = document.createElement('iframe');
    iframe.style.width = '100%';
    iframe.style.height = '100%';
    iframe.style.border = 'none';
    iframe.src = 'https://graspablemath.com/canvas?load=_5fa7c8bcb9ba7';

    canvasElement.appendChild(iframe);

    console.log('[GM] Iframe created');

    // Store reference
    window.graspableCanvas = { type: 'iframe', iframe: iframe };
}

function handleGraspableEvent(event) {
    var eventData = {
        type: event.type || 'unknown',
        expressionBefore: event.before || '',
        expressionAfter: event.after || ''
    };

    console.log('[GM] Event:', eventData);

    if (window.graspableViewConnector) {
        window.graspableViewConnector.onMathAction(
            eventData.type,
            eventData.expressionBefore,
            eventData.expressionAfter
        );
    }
}

// Utility functions for manipulating the canvas
window.graspableMathUtils = {
    clearCanvas: function() {
        console.log('[GM] Clearing canvas...');
        if (window.graspableCanvas) {
            try {
                // Remove all elements from the canvas
                var elements = window.graspableCanvas.model.elements();
                console.log('[GM] Found', elements.length, 'elements to remove');
                
                elements.forEach(function(element) {
                    window.graspableCanvas.model.removeElement(element);
                });
                
                console.log('[GM] Canvas cleared');
            } catch (error) {
                console.error('[GM] Error clearing canvas:', error);
            }
        }
    },
    
    loadProblem: function(equation, x, y) {
        console.log('[GM] Loading problem:', equation, 'at', x, y);
        if (!window.graspableCanvas) {
            console.error('[GM] Canvas not initialized');
            return;
        }
        
        try {
            // Use createElement to add a new derivation
            var derivation = window.graspableCanvas.model.createElement('derivation', {
                eq: equation,
                pos: { x: x || 100, y: y || 50 },
                font_size: 40
            });
            
            // Set up event listener for this derivation
            if (derivation && derivation.events) {
                derivation.events.on('change', function(event) {
                    var lastEq = derivation.getLastModel().to_ascii();
                    console.log('[GM] Equation changed to:', lastEq);
                    handleGraspableEvent({
                        type: 'change',
                        before: event.before_ascii || '',
                        after: lastEq
                    });
                });
            }
            
            console.log('[GM] Problem loaded:', equation);
        } catch (error) {
            console.error('[GM] Error loading problem:', error);
            console.error('[GM] Stack:', error.stack);
        }
    }
};

console.log('[GM] Script ready');

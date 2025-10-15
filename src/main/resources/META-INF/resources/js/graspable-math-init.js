/**
 * Graspable Math Integration
 * Initializes and manages the Graspable Math canvas
 */

console.log("[GM] Script loaded");

// Global initialization function
window.initializeGraspableMath = function () {
    console.log("[GM] Starting initialization...");

    // Check if canvas element exists in DOM
    var canvasElement = document.getElementById("graspable-canvas");
    if (!canvasElement) {
        console.error("[GM] Canvas element not found in DOM");
        return;
    }

    // If canvas already exists, clear it and reinitialize
    if (window.graspableCanvas) {
        console.log(
            "[GM] Canvas already exists, clearing for reinitialization..."
        );
        try {
            // Clear the old canvas
            window.graspableCanvas = null;
            canvasElement.innerHTML = "";
        } catch (e) {
            console.error("[GM] Error clearing old canvas:", e);
        }
    }

    // Load Graspable Math library directly (not the inject script)
    if (!window.gmath) {
        console.log("[GM] Loading Graspable Math library (gmath)...");

        // Load the actual library script
        var script = document.createElement("script");
        script.src =
            "https://graspablemath.com/shared/libs/gmath-dist/gmath-3.5.13.min.js";
        script.onload = function () {
            console.log("[GM] Library loaded, initializing canvas...");
            setTimeout(initializeCanvas, 500);
        };
        script.onerror = function () {
            console.error("[GM] Failed to load library");
            initializeWithIframe();
        };
        document.head.appendChild(script);
    } else {
        console.log("[GM] Library already available");
        setTimeout(initializeCanvas, 100);
    }
};

function initializeCanvas() {
    console.log("[GM] Initializing canvas...");

    // The library exposes 'gmath' globally
    if (!window.gmath) {
        console.error("[GM] gmath not found!");
        return;
    }

    console.log("[GM] gmath API found:", window.gmath);

    var canvasElement = document.getElementById("graspable-canvas");
    if (!canvasElement) {
        console.error("[GM] Canvas element not found!");
        return;
    }

    try {
        // Check if dark theme is active and set it BEFORE creating canvas
        var isDarkMode =
            document.documentElement.hasAttribute("theme") &&
            document.documentElement.getAttribute("theme").includes("dark");

        if (isDarkMode && window.gmath.setDarkTheme) {
            console.log("[GM] Setting dark theme");
            window.gmath.setDarkTheme(true);
        }

        // Create canvas using gmath.Canvas with minimal toolbar
        var canvas = new window.gmath.Canvas(canvasElement, {
            use_fade_effects: true,
            use_property_effect: true,
            use_toolbar: true,
            vertical_scroll: true,
            horizontal_scroll: false,
        });

        window.graspableCanvas = canvas;
        console.log("[GM] Canvas created successfully");
        console.log("[GM] Canvas initialization complete! Ready for problems.");
    } catch (error) {
        console.error("[GM] Error creating canvas:", error);
        console.error("[GM] Error details:", error.stack);
    }
}

function initializeWithIframe() {
    console.log("[GM] Using iframe fallback...");

    var canvasElement = document.getElementById("graspable-canvas");
    if (!canvasElement) {
        console.error("[GM] Canvas element not found!");
        return;
    }

    // Clear any existing content
    canvasElement.innerHTML = "";

    // Create iframe with Graspable Math widget
    var iframe = document.createElement("iframe");
    iframe.style.width = "100%";
    iframe.style.height = "100%";
    iframe.style.border = "none";
    iframe.src = "https://graspablemath.com/canvas?load=_5fa7c8bcb9ba7";

    canvasElement.appendChild(iframe);

    console.log("[GM] Iframe created");

    // Store reference
    window.graspableCanvas = { type: "iframe", iframe: iframe };
}

function handleGraspableEvent(event) {
    var eventData = {
        type: event.type || "unknown",
        expressionBefore: event.before || "",
        expressionAfter: event.after || "",
    };

    console.log("[GM] Event:", eventData);

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
    clearCanvas: function () {
        console.log("[GM] Clearing canvas...");
        if (window.graspableCanvas) {
            try {
                // Remove all elements from the canvas
                var elements = window.graspableCanvas.model.elements();
                console.log(
                    "[GM] Found",
                    elements.length,
                    "elements to remove"
                );

                elements.forEach(function (element) {
                    window.graspableCanvas.model.removeElement(element);
                });

                console.log("[GM] Canvas cleared");
            } catch (error) {
                console.error("[GM] Error clearing canvas:", error);
            }
        }
    },

    loadProblem: function (equation, x, y) {
        console.log("[GM] Loading problem:", equation, "at", x, y);
        if (!window.graspableCanvas) {
            console.error("[GM] Canvas not initialized");
            return;
        }

        try {
            // Split equation by semicolon to handle multiple equations (e.g., systems of equations)
            var equations = equation.split(';').map(function(eq) { return eq.trim(); }).filter(function(eq) { return eq.length > 0; });
            console.log("[GM] Split into", equations.length, "equation(s):", equations);
            
            var startY = y || 50;
            var spacing = 80; // Vertical spacing between equations
            
            // Create a derivation for each equation
            equations.forEach(function(eq, index) {
                var currentY = startY + (index * spacing);
                console.log("[GM] Creating derivation for equation", index + 1, ":", eq, "at y =", currentY);
                
                var derivation = window.graspableCanvas.model.createElement(
                    "derivation",
                    {
                        eq: eq,
                        pos: { x: x || 100, y: currentY },
                        font_size: 40,
                    }
                );

                // Set up event listener for this derivation
                if (derivation && derivation.events) {
                var lastKnownEq = derivation.getLastModel().to_ascii();
                
                derivation.events.on("change", function (event) {
                    try {
                        var currentEq = derivation.getLastModel().to_ascii();
                        console.log("[GM] Change event:", event);
                        console.log("[GM] Equation changed to:", currentEq);
                        console.log("[GM] Derivation rows count:", derivation.rows ? derivation.rows.length : "no rows");
                        
                        // Try to get more detailed action information
                        var actionType = "math_step"; // Changed default to be more specific
                        var actionDetails = null;
                        var beforeEq = lastKnownEq;
                        
                        // Try multiple ways to access row information
                        if (derivation.rows && derivation.rows.length > 0) {
                            console.log("[GM] Total rows:", derivation.rows.length);
                            
                            // Get the last row (most recent action)
                            var lastRow = derivation.rows[derivation.rows.length - 1];
                            console.log("[GM] Last row:", lastRow);
                            
                            // Check if this row has an action
                            if (lastRow && lastRow.action) {
                                console.log("[GM] Action found:", lastRow.action);
                                console.log("[GM] Action name:", lastRow.action.name);
                                console.log("[GM] Action type:", lastRow.action.type);
                                
                                actionType = lastRow.action.name || lastRow.action.type || "math_step";
                                
                                // Try to get action details/description
                                if (lastRow.action.description) {
                                    actionDetails = lastRow.action.description;
                                } else if (lastRow.action.label) {
                                    actionDetails = lastRow.action.label;
                                }
                            }
                            
                            // Get the before equation from the previous row if available
                            if (derivation.rows.length > 1) {
                                var prevRow = derivation.rows[derivation.rows.length - 2];
                                if (prevRow && prevRow.model) {
                                    beforeEq = prevRow.model.to_ascii();
                                }
                            }
                        }
                        
                        console.log("[GM] Action type:", actionType);
                        console.log("[GM] Before:", beforeEq, "-> After:", currentEq);
                        
                        handleGraspableEvent({
                            type: actionType,
                            before: beforeEq,
                            after: currentEq,
                            details: actionDetails,
                        });
                        
                        // Update last known equation
                        lastKnownEq = currentEq;
                    } catch (error) {
                        console.error("[GM] Error in change handler:", error);
                        console.error("[GM] Stack trace:", error.stack);
                        // Fallback to simple change event
                        handleGraspableEvent({
                            type: "math_step",
                            before: lastKnownEq,
                            after: derivation.getLastModel().to_ascii(),
                        });
                        lastKnownEq = derivation.getLastModel().to_ascii();
                    }
                });
                
                // Also listen to mistake events
                derivation.events.on("mistake", function (model) {
                    console.log("[GM] Mistake event - invalid action attempted");
                    // Optionally notify the backend about mistakes
                    // handleGraspableEvent({
                    //     type: "mistake",
                    //     before: model.to_ascii(),
                    //     after: model.to_ascii(),
                    // });
                });
                
                // Listen to undo events
                derivation.events.on("undo", function () {
                    console.log("[GM] Undo event");
                    handleGraspableEvent({
                        type: "undo",
                        before: "",
                        after: derivation.getLastModel().to_ascii(),
                    });
                });
                
                // Listen to redo events
                derivation.events.on("redo", function () {
                    console.log("[GM] Redo event");
                    handleGraspableEvent({
                        type: "redo",
                        before: "",
                        after: derivation.getLastModel().to_ascii(),
                    });
                });
            }
            });

            console.log("[GM] Problem loaded successfully with", equations.length, "equation(s)");
        } catch (error) {
            console.error("[GM] Error loading problem:", error);
            console.error("[GM] Stack:", error.stack);
        }
    },
};

console.log("[GM] Script ready");

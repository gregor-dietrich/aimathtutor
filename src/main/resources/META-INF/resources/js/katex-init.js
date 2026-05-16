/**
 * KaTeX Integration
 * Loads KaTeX from a CDN and exposes a safe render helper for AI tutor chat
 * messages. Mirrors the CDN-loading pattern used by graspable-math-init.js.
 */

(function () {
    if (window.aiMathRender) {
        return; // Already initialized
    }

    var KATEX_VERSION = "0.16.46";
    var BASE = "https://cdn.jsdelivr.net/npm/katex@" + KATEX_VERSION + "/dist/";

    var ready = false;
    var pending = [];

    var DELIMITERS = [
        { left: "$$", right: "$$", display: true },
        { left: "$", right: "$", display: false },
        { left: "\\(", right: "\\)", display: false },
        { left: "\\[", right: "\\]", display: true },
    ];

    function typeset(el) {
        try {
            window.renderMathInElement(el, {
                delimiters: DELIMITERS,
                throwOnError: false,
                trust: false,
                strict: "ignore",
            });
        } catch (e) {
            console.error("[KaTeX] render failed:", e);
        }
    }

    function flushPending() {
        ready = true;
        var queued = pending;
        pending = [];
        queued.forEach(function (el) {
            typeset(el);
        });
    }

    function loadCss() {
        if (document.querySelector('link[data-katex="1"]')) {
            return;
        }
        var link = document.createElement("link");
        link.rel = "stylesheet";
        link.href = BASE + "katex.min.css";
        link.setAttribute("integrity", "sha384-hW6ZmmePRD2f/9cuxGE6C9faGprtIBOme5OLUiEjtRKMTN67tY23ur9eAi21H8De");
        link.setAttribute("crossorigin", "anonymous");
        link.setAttribute("data-katex", "1");
        document.head.appendChild(link);
    }

    function loadScript(src, onload, integrity) {
        var s = document.createElement("script");
        s.src = src;
        s.defer = true;
        s.setAttribute("integrity", integrity);
        s.setAttribute("crossorigin", "anonymous");
        s.onload = onload;
        s.onerror = function () {
            console.error("[KaTeX] Failed to load:", src);
        };
        document.head.appendChild(s);
    }

    loadCss();
    loadScript(
        BASE + "katex.min.js",
        function () {
            loadScript(
                BASE + "contrib/auto-render.min.js",
                function () {
                    flushPending();
                },
                "sha384-bjyGPfbij8/NDKJhSGZNP/khQVgtHUE5exjm4Ydllo42FwIgYsdLO2lXGmRBf5Mz"
            );
        },
        "sha384-FVvsvR4UzyIP8Y5hVvHjOfjVh+LWV78ll63SYx1t+nuuMPGMAihB8dJ2YsYyg1Wb"
    );

    /**
     * Render AI message text into el. Sets textContent first (XSS-safe and a
     * graceful fallback if KaTeX never loads), then typesets matched math
     * delimiters once KaTeX is ready. Unmatched lone "$" (e.g. currency) is
     * left untouched by KaTeX auto-render.
     */
    window.aiMathRender = function (el, text) {
        if (!el) {
            return;
        }
        el.textContent = text == null ? "" : String(text);
        if (ready) {
            typeset(el);
        } else {
            pending.push(el);
        }
    };
})();

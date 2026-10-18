/**
 * Appearance bootstrap, inlined into <head> (AppConfig's @Inline) so it runs before first paint.
 * Sets the effective colour scheme, html[data-color-scheme] (light|dark), from the user's colour mode,
 * html[data-color-mode] (light|dark|system, rendered by ThemeService), and the OS preference.
 */
(function () {
    var root = document.documentElement;
    var media = window.matchMedia("(prefers-color-scheme: dark)");

    function apply() {
        var mode = root.dataset.colorMode;
        root.dataset.colorScheme = mode === "light" || mode === "dark" ? mode : media.matches ? "dark" : "light";
    }

    window.appAppearance = {
        setColorMode: function (mode) {
            root.dataset.colorMode = mode;
            apply();
        },
    };
    media.addEventListener("change", apply);
    apply();
}());

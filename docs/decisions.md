# Decisions

## ADR-1: Priority ordering for design trade-offs

**Status:** Proposed

When options conflict on these axes, security comes first, then reliability,
then performance. This is a tie-breaker between otherwise acceptable options,
not an admission criterion for what gets built.

## ADR-2: Theme-neutral styling through `--vaadin-*` and an app bridge

**Status:** Proposed

Java names no base-theme token or variant; only `AppConfig` and `ThemeService`
name a theme. Inline styles use Vaadin's `--vaadin-*` properties.
Accent and status colours use seven `--app-*` tokens, which the bridge at the
top of `styles.css` defines from whichever base theme is loaded. Checkstyle
`RegexpSinglelineJava` `BaseThemeCoupling` enforces this. Rejected: a
`--lumo-*` alias stylesheet for Aura (Java would keep speaking Lumo), keeping
every style inline (stylesheets could not restyle shared composites), and
moving all inline styling to classes (mostly layout, unverifiable without view
tests).

**Premise:** Vaadin ships no theme-neutral accent or status-colour property
(checked in 25.3.2, component-base `style-props.js`).
**Guard:** watcher: at every Vaadin minor upgrade, check `style-props.js` for a
`--vaadin-*` accent or status property; if one exists, map the `--app-*` tokens
onto it.

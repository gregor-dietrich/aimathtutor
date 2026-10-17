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
(checked in 25.3.2, component-base `style-props.js`). The bridge follows a dark
theme set below `<html>` only while Lumo and Aura write the bridged colours as
`light-dark()` on `:root`, and while each bridged colour's value in Lumo's
explicit-dark block equals the dark branch of its `light-dark()` (checked in
Lumo 25.3.3, which the 25.3.2 platform resolves: `color.css` against
`color-scheme.css`).
**Guard:** watcher: at every Vaadin minor upgrade, check `style-props.js` for a
`--vaadin-*` accent or status property; if one exists, map the `--app-*` tokens
onto it. Also check that every bridged Lumo and Aura colour is still a
`light-dark()` on `:root` and that their explicit-dark values in Lumo still
match the dark branches; if either no longer holds, measure each `--app-*`
token against the token it bridges in Light, Dark and System mode before
upgrading. The `vaadin.version` property in `pom.xml` points here.

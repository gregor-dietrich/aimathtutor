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

## ADR-3: Who shares a login throttle bucket

**Status:** Accepted

The client bucket and the username+client bucket key an IPv6 client by its /64
prefix and an IPv4 client by its address. A successful sign-in removes that
username's bucket for that client and subtracts its failures from the client
bucket. It forgives one more failure there when its account is new to that
client's sign-in window (an hour from the window's first sign-in) and another
account has already signed in within it. Nothing is banked while the count is
zero.

Reason: one signal tells a busy shared address, such as a school behind one IPv4
NAT, apart from someone guessing passwords from it: many different accounts
signing in. Crediting each further account lets the shared bucket heal as the
school's users sign in. A holder of one account gains nothing by signing in
between guesses at other usernames, and a holder of N accounts gains at most
N−1 guesses an hour. Known costs:

- An insider who times a failure at the end of each lock still holds the
  address in hour-long locks, as before this change, because no one can sign in
  during a lock (#270).
- A lock still has no remedy short of a restart (#269).
- A subscriber delegated a /56 or /48 holds 256 to 65,536 client buckets, so
  spraying from such a delegation is bounded per /64, not per subscriber.
- Other IPv6 forms that embed an IPv4 address are keyed by their /64 like any
  IPv6 address. All clients behind one NAT64/SIIT prefix, with IPv4-compatible
  addresses, or behind one Teredo server therefore share a bucket.

Rejected:

- Subtracting only the signing-in username's failures, as first proposed here.
  Failures no sign-in of their own forgives keep the bucket alive: unknown or
  mistyped usernames, banned or deactivated accounts, abandoned attempts, and
  own failures whose bucket expired. Once the client lock reaches an hour,
  every own bucket has expired, so the first failure after each lock re-locks
  the whole address for another hour.
- Clearing the client bucket on every success: an account holder resets the
  bound on spraying other usernames between guesses.
- Clearing it only after a success for a username that failed from that client:
  one failure on one's own account restores the reset.
- Never clearing it on success: a school behind one IPv4 NAT locks out after
  five typos across all its students.
- Crediting every account's first sign-in in the window, the first included:
  one account holder gains a guess an hour by signing in.
- Resetting the client bucket for each new account instead of forgiving one:
  an insider gets up to four free guesses per classmate's sign-in instead of
  one. The faster recovery after an hour lock does not help against an insider
  who times failures at lock ends, who re-locks before any sign-in either way.
- Not counting, or forgiving, failures for usernames that do not exist: the
  difference in lockout behaviour reveals which usernames exist, which the dummy
  password check exists to hide.
- Forgiving the failures of the browser session that signs in: an account
  holder sprays from a session and then signs in from it.
- Counting distinct failed usernames per client instead of failures: each
  mistyped username is a distinct name, so a school still accumulates, and the
  per-client set holds attacker-chosen strings.
- Draining the client count over time: the drain rate is at once a lone
  attacker's sustained guessing rate and too slow for a school's
  start-of-lesson burst; no rate serves both.
- A fixed window on the client bucket like the account bucket's: a window large
  enough for a school's burst lets a lone address guess dozens of times an hour
  instead of about eleven in the first hour and one an hour after.
- An operator list of shared addresses exempt from the client bucket: it needs
  the school's public address, which can change, and exempts an insider's
  guessing entirely.
- Device cookies that let a browser bypass the client lock for its own
  username: they add cookie handling at sign-in, a signing key and per-cookie
  buckets, and help no browser that clears its cookies, such as shared lab
  machines (#270 keeps them as the candidate if insider lockouts become a
  problem).
- Keying the username+client bucket by the full IPv6 address: a /64 could
  rotate addresses to guess one account up to the account limit, and behind an
  IPv4 NAT a neighbour can lock one account for everyone anyway.
- Keying IPv6 by /56 or /48: mobile carriers give each device its own /64, so a
  wider prefix puts unrelated customers in one bucket.

**Premise:** at an address many users share, accounts signing in for the first
time within the hour outnumber the failures that no sign-in of their own
forgives. Mobile carriers give each device its own IPv6 /64.
**Guard:** watcher: a report of a sign-in lockout shared by a whole school or
network, or a client-bucket WARN line naming such an address, re-opens this
entry together with #269.

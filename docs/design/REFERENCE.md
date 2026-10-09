# RepLock UI reference (round 2)

The target design is a six-screen, dark-green reference supplied by the product owner.
The original image was not stored in the repository because the upload did not reach
the workspace. Re-attach it and save it as `docs/design/reference-ui.png` to keep the
pixel reference. Until then, this file is the written spec.

## Palette
- Background: near-black `#050505`; cards: glass `#0D1613` with a green hairline border (alpha 0.16).
- Primary: electric green `#00FF88`. Locked state: red `#FF3B30`. Pro: gold `#FFC857`.
- Secondary chart series: blue `#4FA3FF` (unlocks / other apps); violet `#B388FF` for accents.

## Navigation
Bottom bar: Home, Stats, centre raised green lock button (opens Blocked Apps), Rewards, Settings.

## Screens
1. **Onboarding welcome**: "EARN YOUR" (white) / "SCREEN TIME" (green), "Less distractions. / More progress.",
   glowing hourglass hero with floating app badges, three steps (Pick your apps, Do your reps,
   Earn screen time), "Get Started" pill, footer "Your focus = Your power".
2. **Home**: REPLOCK wordmark with "FOCUS · EARN · GROW", avatar opens Settings, "Blocking Active"
   card with switch, large unlock ring (x/3 unlocks), reps-today card with "+N vs yesterday",
   goal ring (reps / target %) with minutes earned, blocked-app icon row with "+", "Manage Blocklist" card.
3. **Stats**: Week / Month / All Time segmented control, totals with delta vs previous period,
   bar chart of the last 7 days, reps and unlocks line chart, quote card.
4. **Blocked Apps**: category chips (All / Social / Entertainment / Other, from the app category),
   search, toggle per app, "Add App" row that focuses search.
5. **Settings**: profile card with Upgrade, expandable rows (rep target, unlock window up to 3 hours,
   exercise, notifications, privacy, help).
6. **Rewards**: level and XP ring, title, Pro card, milestones with progress. XP = reps x 10 + unlocks x 50,
   level = XP / 300 + 1. Everything comes from real history.

## Known gaps
- Visual match was not checked on a render. Compare against the reference on a device.
- The reference's exact pixel layout and icon set are approximations (Material icons).

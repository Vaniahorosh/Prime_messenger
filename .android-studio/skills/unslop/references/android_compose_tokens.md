# Android & Jetpack Compose Tokens (Unslop Guide)

When unslopping Android apps (Jetpack Compose or XML Views), translate the 9-pass design redline into Android idioms:

## 1. Spacing (4dp / 8dp grid)
- **Scale:** `4.dp`, `8.dp`, `12.dp`, `16.dp`, `20.dp`, `24.dp`, `32.dp`, `40.dp`, `48.dp`
- **Compose:** Use `Modifier.padding(...)`, `Spacer(Modifier.height(16.dp))`, `Arrangement.spacedBy(8.dp)`.
- Replace odd or magic numbers (e.g. `13.dp`, `7.dp`) with standard scale steps.
- Row / item heights:
  - Compact row: `36.dp` to `40.dp`
  - Default list item: `48.dp` to `56.dp`
  - Minimum touch target: `48.dp` (`Modifier.minimumInteractiveComponentSize()`)

## 2. Color & Material 3 Surfaces
- Build from Material 3 semantic roles instead of hardcoded hex / random gradient brushes:
  - Surface hierarchy: `MaterialTheme.colorScheme.surface`, `surfaceContainerLow`, `surfaceContainer`, `surfaceContainerHigh`
  - Text roles:
    - Primary text: `MaterialTheme.colorScheme.onSurface`
    - Secondary / metadata text: `MaterialTheme.colorScheme.onSurfaceVariant`
    - Muted / hint: `MaterialTheme.colorScheme.outline`
  - Borders / Dividers: `MaterialTheme.colorScheme.outlineVariant` (subtle hairline, 1.dp or 0.5.dp)
  - Single Accent: `MaterialTheme.colorScheme.primary` or custom product accent used sparingly (e.g., active tab indicator, primary FAB or action button).
- **Remove slop:**
  - Strip out `Brush.horizontalGradient(...)` or `Brush.radialGradient(...)` from cards and app backgrounds (e.g. purple-to-blue or pink-to-violet).
  - Strip glowing box shadows / blur washes behind cards. Use subtle elevation (`CardDefaults.cardElevation(defaultElevation = 1.dp)`) or flat surface steps with hairlines (`BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)`).

## 3. Icons
- Replace random emojis in UI labels/buttons (e.g., `Text("Settings ⚙️")`, `Button { Text("🚀 Launch") }`) with proper Vector Drawables or Compose Material Icons:
  - `Icon(imageVector = Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(20.dp))`
  - Tint icons with `LocalContentColor.current` or `MaterialTheme.colorScheme.onSurfaceVariant`.
  - Maintain a uniform icon box size (`Modifier.size(24.dp)`) across list items for clean column alignment.

## 4. Typography
- Stick to standard M3 Type Scale:
  - Headline: `MaterialTheme.typography.titleLarge` or `headlineSmall`
  - Body: `MaterialTheme.typography.bodyMedium`
  - Captions/Metadata: `MaterialTheme.typography.labelSmall`
- Restrict font weights to `FontWeight.Normal` (400) and `FontWeight.Medium` (500). Avoid `FontWeight.ExtraBold` or `W800` unless purposefully branded.
- For financial or metric data: enable tabular figures (`FontFeatureSettings("tnum")`).

## 5. Copy
- Concise, clear, no hype words ("seamless", "unlock magic", "supercharge").
- Drop exclamation marks and trailing periods in buttons and titles.
- Show clear timeframes on stats (e.g., `+12% vs last month` instead of `+12%!!`).

## 6. Layout & Geometry
- Consistent corner radii:
  - Small elements (chips, badges): `RoundedCornerShape(8.dp)`
  - Cards, dialogs: `RoundedCornerShape(16.dp)`
  - Large sheets: `RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)`
  - Avoid turning every card or input into a full pill (`CircleShape`).

## 7. Motion
- Restrained, purposeful animations:
  - `animateContentSize(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))`
  - `AnimatedVisibility(enter = fadeIn(tween(150)) + expandVertically(), exit = fadeOut(tween(150)))`
  - Avoid continuous spinning, glowing pulses, or jarring spring bounces.

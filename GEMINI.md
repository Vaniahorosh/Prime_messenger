# Prime Messenger Agent Guidelines & Skills

## Project Overview
- **Platform:** Android (Min SDK 26, Target SDK 37)
- **Languages:** Kotlin, Java
- **UI Stack:** Jetpack Compose & Material 3 (primary), Android ViewBinding / Custom Views
- **Theme & Design System:** Material 3 Expressive (M3E), dark/light mode surface tokens, custom shapes and smooth blur menus.

---

## Skills

### `unslop`
Located at `.agents/skills/unslop/` (and `.android-studio/skills/unslop/`).

Turns AI-generated or vibecoded UI into clean, production-ready, deliberate interfaces.

**When to activate:**
- Whenever the user asks to "unslop", "polish", "clean up", or "redesign" a screen/component.
- When an Android screen shows typical AI tells: random hue gradients, glowing shadows, emoji labels/buttons, magic spacing values (e.g. `13.dp`, `7.dp`), shouty copy, or unbalanced padding.
- When the user runs `/unslop` or uses the "Unslop UI" prompt template.

**Workflow:**
1. **Audit:** Scan Compose or XML code for signs of slop ([signs.md](.agents/skills/unslop/references/signs.md)).
2. **Direction:** Define 5 lines: Product, Character, World, Moments, Signature.
3. **9 Passes in Strict Order:**
   - **Spacing:** Strict 4dp/8dp scale (`4.dp`, `8.dp`, `12.dp`, `16.dp`, `24.dp`), 48dp minimum touch target.
   - **Color:** Material 3 neutral ramp (`surface`, `surfaceContainer`), exactly one accent, no neon/hue gradients.
   - **Icons:** Material Icons / Vector Drawables with consistent size (`20.dp` / `24.dp`), remove emoji decorations.
   - **Copy:** Concise, calm, no hype words ("magic", "unlock", "seamless"), no exclamation marks.
   - **Typography:** Material 3 type hierarchy, weights 400 (`Normal`) and 500 (`Medium`), tabular figures for data.
   - **Layout & Geometry:** Consistent `RoundedCornerShape` radii, avoid turning all elements into full pills.
   - **Details:** Subtle hairline borders (`BorderStroke(1.dp, colorScheme.outlineVariant)`) and M3 tonal elevations.
   - **Visuals:** Real app previews or clean charts instead of fake glowing orbs.
   - **Motion:** Restrained Material transitions (150-200ms `FastOutSlowInEasing`).
4. **Android Compose Tokens:** Consult [.agents/skills/unslop/references/android_compose_tokens.md](.agents/skills/unslop/references/android_compose_tokens.md).

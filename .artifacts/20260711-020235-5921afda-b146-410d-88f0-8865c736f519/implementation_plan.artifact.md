# Animated Labels and Shimmering Gradient in Settings

The user wants two enhancements in `SettingsActivity`:
1. **Animated Button Labels**: White text labels for "Photo", "Settings", and "Logout" that appear from under the buttons when expanding and fade/move up when collapsing.
2. **Shimmering Gradient**: A white gradient at the bottom of the profile photo that shimmers/glows and potentially adapts to the environment.

## Proposed Changes

### [Component: UI - Settings]

#### [activity_settings.xml](file:///C:/Users/going/StudioProjects/Prime_messenger/app/src/main/res/layout/activity_settings.xml)

- **Shimmering Gradient**:
    - Add a `View` (`photoShimmer`) inside the `CollapsingToolbarLayout`, positioned at the bottom of `ivProfilePhoto`.
    - Set its background to a new drawable `bg_photo_shimmer.xml`.
- **Button Labels**:
    - Remove text and top-gravity icons from `btnChangePhoto`, `btnExtraSettings`, and `btnLogout`.
    - Change button icons to be centered.
    - Wrap each button and its label in a `ConstraintLayout` to allow overlapping/translation.
    - Add `tvLabelPhoto`, `tvLabelSettings`, `tvLabelLogout` with white text color.
    - Set their initial state (below buttons).

#### [NEW] [bg_photo_shimmer.xml](file:///C:/Users/going/StudioProjects/Prime_messenger/app/src/main/res/drawable/bg_photo_shimmer.xml)

- A linear gradient from transparent to semi-transparent white.

#### [SettingsActivity.kt](file:///C:/Users/going/StudioProjects/Prime_messenger/app/src/main/java/com/messenger/prime/SettingsActivity.kt)

- **Label Animations**:
    - Update `updateButtonsAppearance(percentage: Float)` to animate `alpha` and `translationY` of the new labels.
    - Labels will move "up" and disappear behind buttons as the app bar collapses.
- **Shimmer Animation**:
    - In `onCreate`, start a repeating `ValueAnimator` to change the `alpha` or `translationX` of the `photoShimmer` view to create a shimmering effect.
- **Adaptive Color**: (Optional/Simplified) Adjust shimmer intensity or tint based on the color morphing logic.

## Verification Plan

### Manual Verification
- Deploy the app and navigate to Settings.
- **Labels**: Scroll the list up and down. Verify white labels slide down from under buttons when expanding and slide up/fade when collapsing.
- **Shimmer**: Verify the bottom of the profile photo has a gentle, animated white glow/shimmer.

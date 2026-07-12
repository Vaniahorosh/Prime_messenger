# Cleanup Legacy Photo Viewer and Fix Fullscreen Bugs

I will remove the old activity-based photo viewer and fix the rendering/gesture issues in the new inline expansion mode.

## Proposed Changes

### [Component: UI - Cleanup]

#### [DELETE] [PhotoViewerActivity.kt](file:///C:/Users/going/StudioProjects/Prime_messenger/app/src/main/java/com/messenger/prime/PhotoViewerActivity.kt)
#### [DELETE] [activity_photo_viewer.xml](file:///C:/Users/going/StudioProjects/Prime_messenger/app/src/main/res/layout/activity_photo_viewer.xml)

#### [AndroidManifest.xml](file:///C:/Users/going/StudioProjects/Prime_messenger/app/src/main/AndroidManifest.xml)
- Remove `PhotoViewerActivity` declaration.

### [Component: UI - Settings Fixes]

#### [SettingsActivity.kt](file:///C:/Users/going/StudioProjects/Prime_messenger/app/src/main/java/com/messenger/prime/SettingsActivity.kt)
- **Remove Legacy Logic**:
    - Remove `handlePullThresholdReached` and related callbacks in `OverscrollNestedScrollView`.
    - Delete any unused "pull" related variables (`pullThresholdPx`, `photoDragStartRawY`, etc., if they are only for the old gesture).
- **Fix Rendering (White Gap)**:
    - In `openFullScreenMode()`, set `binding.root.setBackgroundColor(Color.BLACK)` to eliminate the light-gray background visible in the screenshot.
    - Restore the original background in `closeFullScreenMode()`.
- **Improve Gestures**:
    - Update the `GestureDetector` in fullscreen mode to handle both **Upward** and **Downward** flings to close the photo.
    - Ensure smooth transitions for the background color.

## Verification Plan

### Manual Verification
- **Gesture**:
    - Fling down on the photo -> Expands to fullscreen.
    - Fling down again while in fullscreen -> Returns to normal.
    - Fling up while in fullscreen -> Returns to normal.
- **Visuals**:
    - Verify that the entire background becomes solid black in fullscreen mode (no white/gray areas).
    - Verify that all settings content is hidden.
- **Cleanup**:
    - Ensure the project builds and runs without `PhotoViewerActivity`.

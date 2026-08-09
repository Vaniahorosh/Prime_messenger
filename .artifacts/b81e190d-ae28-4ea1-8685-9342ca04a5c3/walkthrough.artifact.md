# Settings Activity Redesign Walkthrough

I have updated the Settings activity to match the requested design: rounded gray blocks, a styled header, and informative toggles.

## Changes Made

### UI Components
- **Rounded Gray Blocks**: Created [bg_settings_block_gray.xml](file:///C:/Users/going/StudioProjects/Prime_messenger/app/src/main/res/drawable/bg_settings_block_gray.xml) to provide a consistent background for settings sections.
- **Login/Password Section**: Refactored to use the new gray block style with subtle dividers and cleaner input field styling.
- **Section Header**: Added a "Settings" header with decorative lines on both sides to separate account data from app settings.
- **Informative Switches**:
    - Each setting toggle is now enclosed in its own rounded gray block.
    - Added descriptive text under each switch to explain its purpose.

## Verification
- Checked layout XML for correctness and constraint integrity.
- Verified that all IDs remain consistent for backend functionality.

> [!TIP]
> The new design uses `#F5F5F5` for the blocks and `#E0E0E0` for dividers, creating a soft, modern look that fits the application's aesthetic.

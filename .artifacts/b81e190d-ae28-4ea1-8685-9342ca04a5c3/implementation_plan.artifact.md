# Redesign Settings Activity Components

The goal is to update the `activity_settings.xml` layout to include rounded gray blocks for input fields and settings switches, add a styled header for the settings section, and improve the information display for switches.

## Proposed Changes

### [Resources]

#### [NEW] [bg_settings_block_gray.xml](file:///C:/Users/going/StudioProjects/Prime_messenger/app/src/main/res/drawable/bg_settings_block_gray.xml)
Create a new drawable for the rounded gray blocks.

### [Layout]

#### [MODIFY] [activity_settings.xml](file:///C:/Users/going/StudioProjects/Prime_messenger/app/src/main/res/layout/activity_settings.xml)
- Update the background of the account data and settings blocks to use the new gray drawable.
- Add a new "Settings" header with lines on the sides between the account block and settings switches.
- Refactor the switches to be inside their own blocks with descriptive gray text.

## Verification Plan

### Manual Verification
- Deploy the app and open the Settings activity.
- Verify the login/password fields are in a rounded gray block.
- Verify the "Settings" header with lines is present.
- Verify the toggles are in rounded blocks with descriptive text.

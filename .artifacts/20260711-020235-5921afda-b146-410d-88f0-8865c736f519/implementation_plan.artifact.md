# Redesign Account Edit Dialog Buttons

The user wants to update the button layout in `dialog_edit_account.xml`:
1. Change the "Назад" (Back) button from text to an arrow icon.
2. Expand the "Сохранить" (Save) button in length (width) to take up more space.

## Proposed Changes

### [Component: UI - Settings Dialog]

#### [dialog_edit_account.xml](file:///C:/Users/going/StudioProjects/Prime_messenger/app/src/main/res/layout/dialog_edit_account.xml)

- Update `btnBack`:
    - Set `android:text=""`.
    - Add `app:icon="@drawable/ic_arrow_back"`.
    - Adjust its width to be fixed (e.g., `56dp`) or wrap content to look like a square-ish icon button.
    - Keep `app:cornerRadius="15dp"`.
- Update `btnSave`:
    - Set `android:layout_width="0dp"`.
    - Set `android:layout_weight` or use ConstraintLayout constraints to make it fill the remaining space.
    - Adjust margins for proper spacing between the icon button and the save button.

## Verification Plan

### Manual Verification
- Deploy the app and navigate to Settings.
- Open the edit dialog.
- Verify the "Back" button now shows an arrow icon.
- Verify the "Save" button is wider and fills the available space next to the arrow.
- Confirm both buttons maintain their functionality and rounded corners.

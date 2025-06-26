package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.Theme;

public class MuteOptionsDialog {
    
    public interface OnMuteOptionSelectedListener {
        void onDisableSound();
        void onMuteFor();
        void onCustomize();
        void onMuteForever();
    }
    
    public static ActionBarPopupWindow show(Context context, View anchorView, Theme.ResourcesProvider resourcesProvider, OnMuteOptionSelectedListener listener) {
        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(8), 
                             AndroidUtilities.dp(8), AndroidUtilities.dp(8));
        
        // Background
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(AndroidUtilities.dp(8));
        background.setColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground, resourcesProvider));
        background.setStroke(AndroidUtilities.dp(1), 
            Theme.getColor(Theme.key_actionBarDefaultSubmenuSeparator, resourcesProvider));
        rootLayout.setBackground(background);
        
        // Disable Sound
        LinearLayout disableSoundItem = createMenuItem(context, resourcesProvider,
            R.drawable.msg_mute, "Disable Sound", false);
        disableSoundItem.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDisableSound();
            }
        });
        rootLayout.addView(disableSoundItem);
        
        // Mute for...
        LinearLayout muteForItem = createMenuItem(context, resourcesProvider,
            R.drawable.msg_mute, "Mute for...", false);
        muteForItem.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMuteFor();
            }
        });
        rootLayout.addView(muteForItem);
        
        // Customize
        LinearLayout customizeItem = createMenuItem(context, resourcesProvider,
            R.drawable.msg_customize, "Customize", false);
        customizeItem.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCustomize();
            }
        });
        rootLayout.addView(customizeItem);
        
        // Separator
        View separator = new View(context);
        separator.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuSeparator, resourcesProvider));
        LinearLayout.LayoutParams separatorParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(1));
        separatorParams.topMargin = AndroidUtilities.dp(4);
        separatorParams.bottomMargin = AndroidUtilities.dp(4);
        separator.setLayoutParams(separatorParams);
        rootLayout.addView(separator);
        
        // Mute Forever (red text)
        LinearLayout muteForeverItem = createMenuItem(context, resourcesProvider,
            R.drawable.msg_mute, "Mute Forever", true);
        muteForeverItem.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMuteForever();
            }
        });
        rootLayout.addView(muteForeverItem);
        
        ActionBarPopupWindow popupWindow = new ActionBarPopupWindow(rootLayout, 
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        popupWindow.setAnimationEnabled(true);
        popupWindow.setAnimationStyle(R.style.PopupContextAnimation);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setClippingEnabled(true);
        popupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
        // Remove this line as SOFT_INPUT_STATE_UNSPECIFIED doesn't exist
        // popupWindow.setSoftInputMode(ActionBarPopupWindow.SOFT_INPUT_STATE_UNSPECIFIED);
        
        // Calculate position
        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        
        int x = location[0] + anchorView.getWidth() / 2 - AndroidUtilities.dp(120);
        int y = location[1] + anchorView.getHeight() + AndroidUtilities.dp(4);
        
        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y);
        
        return popupWindow;
    }
    
    private static LinearLayout createMenuItem(Context context, Theme.ResourcesProvider resourcesProvider, 
                                              int iconRes, String text, boolean isDestructive) {
        LinearLayout item = new LinearLayout(context);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), 
                       AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
            AndroidUtilities.dp(200), AndroidUtilities.dp(44));
        item.setLayoutParams(itemParams);
        
        // Ripple effect background
        GradientDrawable rippleBackground = new GradientDrawable();
        rippleBackground.setCornerRadius(AndroidUtilities.dp(4));
        rippleBackground.setColor(0x00000000); // Transparent
        item.setBackground(rippleBackground);
        
        // Icon
        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        int iconColor = isDestructive ? 
            Theme.getColor(Theme.key_text_RedRegular, resourcesProvider) :
            Theme.getColor(Theme.key_actionBarDefaultSubmenuItem, resourcesProvider);
        icon.setColorFilter(iconColor);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
            AndroidUtilities.dp(20), AndroidUtilities.dp(20));
        iconParams.rightMargin = AndroidUtilities.dp(12);
        icon.setLayoutParams(iconParams);
        item.addView(icon);
        
        // Text
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextSize(16);
        int textColor = isDestructive ? 
            Theme.getColor(Theme.key_text_RedRegular, resourcesProvider) :
            Theme.getColor(Theme.key_actionBarDefaultSubmenuItem, resourcesProvider);
        textView.setTextColor(textColor);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textView.setLayoutParams(textParams);
        item.addView(textView);
        
        return item;
    }
}
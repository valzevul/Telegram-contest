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
import org.telegram.ui.ActionBar.Theme;

public class ProfileActionsComponent extends LinearLayout {
    private Theme.ResourcesProvider resourcesProvider;
    private LinearLayout[] actionButtons;
    private OnActionClickListener listener;
    
    public interface OnActionClickListener {
        void onActionClick(int actionType);
    }
    
    public static class ActionButton {
        public static final int TYPE_MESSAGE = 0;
        public static final int TYPE_CALL = 1;
        public static final int TYPE_VIDEO = 2;
        public static final int TYPE_MUTE = 3;
        public static final int TYPE_UNMUTE = 4;
        public static final int TYPE_SHARE = 5;
        public static final int TYPE_VOICE_CHAT = 6;
        public static final int TYPE_LEAVE = 7;
        public static final int TYPE_JOIN = 8;
        public static final int TYPE_REPORT = 9;
        public static final int TYPE_BLOCK = 10;
        public static final int TYPE_GIFT = 11;
        public static final int TYPE_LIVE_STREAM = 12;
        public static final int TYPE_STORY = 13;
        
        public int type;
        public String text;
        public int iconRes;
        public boolean isPrimary;
        
        public ActionButton(int type, String text, int iconRes, boolean isPrimary) {
            this.type = type;
            this.text = text;
            this.iconRes = iconRes;
            this.isPrimary = isPrimary;
        }
    }

    public ProfileActionsComponent(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = AndroidUtilities.dp(16);
        params.bottomMargin = AndroidUtilities.dp(16);
        params.leftMargin = AndroidUtilities.dp(16);
        params.rightMargin = AndroidUtilities.dp(16);
        setLayoutParams(params);
        
        actionButtons = new LinearLayout[4];
    }
    
    public void setActions(ActionButton[] actions) {
        removeAllViews();
        
        for (int i = 0; i < Math.min(actions.length, 4); i++) {
            LinearLayout button = createActionButton(actions[i], i);
            actionButtons[i] = button;
            addView(button);
        }
    }
    
    private LinearLayout createActionButton(ActionButton action, int index) {
        LinearLayout button = new LinearLayout(getContext());
        button.setOrientation(LinearLayout.VERTICAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), 
                         AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            0, AndroidUtilities.dp(72), 1f); // Slightly taller to match wireframes
        
        if (index > 0) {
            buttonParams.leftMargin = AndroidUtilities.dp(12); // More spacing between buttons
        }
        
        button.setLayoutParams(buttonParams);
        
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(AndroidUtilities.dp(20)); // More rounded to match wireframes
        
        if (action.isPrimary) {
            background.setColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, resourcesProvider));
        } else {
            background.setColor(Theme.getColor(Theme.key_windowBackgroundGray, resourcesProvider));
        }
        
        button.setBackground(background);
        
        ImageView icon = new ImageView(getContext());
        icon.setLayoutParams(new LinearLayout.LayoutParams(
            AndroidUtilities.dp(28), AndroidUtilities.dp(28))); // Slightly larger icons
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        
        if (action.iconRes != 0) {
            icon.setImageResource(action.iconRes);
        } else {
            icon.setImageResource(getDefaultIconForAction(action.type));
        }
        
        if (action.isPrimary) {
            icon.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
        } else {
            icon.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider));
        }
        
        TextView text = new TextView(getContext());
        text.setText(action.text);
        text.setTextSize(12);
        text.setGravity(Gravity.CENTER);
        text.setMaxLines(1);
        
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        textParams.topMargin = AndroidUtilities.dp(4);
        text.setLayoutParams(textParams);
        
        if (action.isPrimary) {
            text.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
        } else {
            text.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider));
        }
        
        button.addView(icon);
        button.addView(text);
        
        button.setOnClickListener(v -> {
            if (listener != null) {
                listener.onActionClick(action.type);
            }
        });
        
        return button;
    }
    
    private int getDefaultIconForAction(int actionType) {
        switch (actionType) {
            case ActionButton.TYPE_MESSAGE:
                return R.drawable.msg_send;
            case ActionButton.TYPE_CALL:
                return R.drawable.msg_callback;
            case ActionButton.TYPE_VIDEO:
                return R.drawable.msg_videocall;
            case ActionButton.TYPE_MUTE:
                return R.drawable.msg_mute;
            case ActionButton.TYPE_UNMUTE:
                return R.drawable.msg_unmute;
            case ActionButton.TYPE_SHARE:
                return R.drawable.msg_share;
            case ActionButton.TYPE_VOICE_CHAT:
                return R.drawable.msg_voiceclose;
            case ActionButton.TYPE_LEAVE:
                return R.drawable.msg_leave;
            case ActionButton.TYPE_JOIN:
                return R.drawable.msg_add;
            case ActionButton.TYPE_REPORT:
                return R.drawable.msg_report;
            case ActionButton.TYPE_BLOCK:
                return R.drawable.msg_block;
            case ActionButton.TYPE_GIFT:
                return R.drawable.msg_gift_premium;
            default:
                return R.drawable.msg_send;
        }
    }
    
    public void setOnActionClickListener(OnActionClickListener listener) {
        this.listener = listener;
    }
    
    public void updateTheme() {
        for (int i = 0; i < actionButtons.length; i++) {
            if (actionButtons[i] != null) {
                updateButtonTheme(actionButtons[i], i);
            }
        }
    }
    
    private void updateButtonTheme(LinearLayout button, int index) {
        GradientDrawable background = (GradientDrawable) button.getBackground();
        if (background != null) {
            background.setColor(Theme.getColor(Theme.key_windowBackgroundGray, resourcesProvider));
        }
        
        ImageView icon = (ImageView) button.getChildAt(0);
        if (icon != null) {
            icon.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider));
        }
        
        TextView text = (TextView) button.getChildAt(1);
        if (text != null) {
            text.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider));
        }
    }
    
    public static ActionButton[] getDefaultUserActions() {
        return new ActionButton[]{
            new ActionButton(ActionButton.TYPE_MESSAGE, "Message", 0, true),
            new ActionButton(ActionButton.TYPE_MUTE, "Mute", 0, false),
            new ActionButton(ActionButton.TYPE_CALL, "Call", 0, false),
            new ActionButton(ActionButton.TYPE_VIDEO, "Video", 0, false)
        };
    }
    
    public static ActionButton[] getUserActionsWithGift() {
        return new ActionButton[]{
            new ActionButton(ActionButton.TYPE_MESSAGE, "Message", 0, true),
            new ActionButton(ActionButton.TYPE_CALL, "Call", 0, false),
            new ActionButton(ActionButton.TYPE_VIDEO, "Video", 0, false),
            new ActionButton(ActionButton.TYPE_GIFT, "Gift", 0, false)
        };
    }
    
    public static ActionButton[] getDefaultBotActions() {
        return new ActionButton[]{
            new ActionButton(ActionButton.TYPE_MESSAGE, "Message", 0, true),
            new ActionButton(ActionButton.TYPE_UNMUTE, "Unmute", 0, false),
            new ActionButton(ActionButton.TYPE_SHARE, "Share", 0, false),
            new ActionButton(ActionButton.TYPE_BLOCK, "Stop", 0, false)
        };
    }
    
    public static ActionButton[] getDefaultGroupActions() {
        return new ActionButton[]{
            new ActionButton(ActionButton.TYPE_MESSAGE, "Message", 0, true),
            new ActionButton(ActionButton.TYPE_UNMUTE, "Unmute", 0, false),
            new ActionButton(ActionButton.TYPE_VOICE_CHAT, "Voice Chat", 0, false),
            new ActionButton(ActionButton.TYPE_LEAVE, "Leave", 0, false)
        };
    }
    
    public static ActionButton[] getDefaultChannelActions() {
        return new ActionButton[]{
            new ActionButton(ActionButton.TYPE_JOIN, "Join", 0, true),
            new ActionButton(ActionButton.TYPE_UNMUTE, "Unmute", 0, false),
            new ActionButton(ActionButton.TYPE_SHARE, "Share", 0, false),
            new ActionButton(ActionButton.TYPE_REPORT, "Report", 0, false)
        };
    }
    
    public static ActionButton[] getDefaultBusinessActions() {
        return new ActionButton[]{
            new ActionButton(ActionButton.TYPE_MESSAGE, "Message", 0, true),
            new ActionButton(ActionButton.TYPE_CALL, "Call", 0, false),
            new ActionButton(ActionButton.TYPE_VIDEO, "Video", 0, false),
            new ActionButton(ActionButton.TYPE_MUTE, "Mute", 0, false)
        };
    }
    
    public static ActionButton[] getUserActionsWithMute() {
        return new ActionButton[]{
            new ActionButton(ActionButton.TYPE_MESSAGE, "Message", 0, true),
            new ActionButton(ActionButton.TYPE_CALL, "Call", 0, false),
            new ActionButton(ActionButton.TYPE_VIDEO, "Video", 0, false),
            new ActionButton(ActionButton.TYPE_MUTE, "Mute", 0, false)
        };
    }
}
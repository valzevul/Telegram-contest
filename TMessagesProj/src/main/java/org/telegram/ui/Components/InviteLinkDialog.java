package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;

public class InviteLinkDialog extends BottomSheet {
    private TLRPC.User restrictedUser;
    private OnInviteLinkSentListener listener;
    
    public interface OnInviteLinkSentListener {
        void onInviteLinkSent();
    }
    
    public InviteLinkDialog(Context context, TLRPC.User user, Theme.ResourcesProvider resourcesProvider) {
        super(context, false, resourcesProvider);
        this.restrictedUser = user;
        
        setApplyTopPadding(false);
        setApplyBottomPadding(false);
        
        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(32), 
                             AndroidUtilities.dp(24), AndroidUtilities.dp(24));
        
        createCloseButton(rootLayout, context);
        createIconAndTitle(rootLayout, context);
        createDescription(rootLayout, context);
        createSendButton(rootLayout, context);
        
        setCustomView(rootLayout);
    }
    
    private void createCloseButton(LinearLayout rootLayout, Context context) {
        FrameLayout closeButtonContainer = new FrameLayout(context);
        LinearLayout.LayoutParams closeContainerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            AndroidUtilities.dp(32));
        closeButtonContainer.setLayoutParams(closeContainerParams);
        
        ImageView closeButton = new ImageView(context);
        closeButton.setImageResource(R.drawable.ic_close_white);
        closeButton.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
            AndroidUtilities.dp(24), AndroidUtilities.dp(24));
        closeParams.gravity = Gravity.END;
        closeButton.setLayoutParams(closeParams);
        closeButton.setOnClickListener(v -> dismiss());
        closeButtonContainer.addView(closeButton);
        
        rootLayout.addView(closeButtonContainer);
    }
    
    private void createIconAndTitle(LinearLayout rootLayout, Context context) {
        // Icon container with blue circular background
        FrameLayout iconContainer = new FrameLayout(context);
        LinearLayout.LayoutParams iconContainerParams = new LinearLayout.LayoutParams(
            AndroidUtilities.dp(80), AndroidUtilities.dp(80));
        iconContainerParams.gravity = Gravity.CENTER_HORIZONTAL;
        iconContainerParams.topMargin = AndroidUtilities.dp(16);
        iconContainer.setLayoutParams(iconContainerParams);
        
        GradientDrawable iconBackground = new GradientDrawable();
        iconBackground.setShape(GradientDrawable.OVAL);
        iconBackground.setColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, resourcesProvider));
        iconContainer.setBackground(iconBackground);
        
        ImageView linkIcon = new ImageView(context);
        linkIcon.setImageResource(R.drawable.msg_link);
        linkIcon.setColorFilter(0xFFFFFFFF); // White icon
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
            AndroidUtilities.dp(32), AndroidUtilities.dp(32));
        iconParams.gravity = Gravity.CENTER;
        linkIcon.setLayoutParams(iconParams);
        iconContainer.addView(linkIcon);
        
        rootLayout.addView(iconContainer);
        
        // Title
        TextView titleView = new TextView(context);
        titleView.setText("Invite via Link");
        titleView.setTextSize(20);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.gravity = Gravity.CENTER_HORIZONTAL;
        titleParams.topMargin = AndroidUtilities.dp(24);
        titleView.setLayoutParams(titleParams);
        rootLayout.addView(titleView);
    }
    
    private void createDescription(LinearLayout rootLayout, Context context) {
        TextView descriptionView = new TextView(context);
        
        String userName = restrictedUser != null ? 
            org.telegram.messenger.ContactsController.formatName(restrictedUser.first_name, restrictedUser.last_name) :
            "This user";
        
        String descriptionText = userName + " restricts calling them. You can send them an invite link instead.";
        descriptionView.setText(descriptionText);
        descriptionView.setTextSize(16);
        descriptionView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
        descriptionView.setGravity(Gravity.CENTER);
        descriptionView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        descParams.topMargin = AndroidUtilities.dp(12);
        descParams.leftMargin = AndroidUtilities.dp(16);
        descParams.rightMargin = AndroidUtilities.dp(16);
        descriptionView.setLayoutParams(descParams);
        rootLayout.addView(descriptionView);
    }
    
    private void createSendButton(LinearLayout rootLayout, Context context) {
        TextView sendButton = new TextView(context);
        sendButton.setText("Send Invite Link");
        sendButton.setTextSize(16);
        sendButton.setTextColor(0xFFFFFFFF); // White text
        sendButton.setGravity(Gravity.CENTER);
        sendButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            AndroidUtilities.dp(48));
        buttonParams.topMargin = AndroidUtilities.dp(32);
        sendButton.setLayoutParams(buttonParams);
        
        GradientDrawable buttonBackground = new GradientDrawable();
        buttonBackground.setCornerRadius(AndroidUtilities.dp(8));
        buttonBackground.setColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, resourcesProvider));
        sendButton.setBackground(buttonBackground);
        
        sendButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onInviteLinkSent();
            }
            dismiss();
        });
        
        rootLayout.addView(sendButton);
    }
    
    public void setOnInviteLinkSentListener(OnInviteLinkSentListener listener) {
        this.listener = listener;
    }
    
    public static void show(Context context, TLRPC.User restrictedUser, Theme.ResourcesProvider resourcesProvider, OnInviteLinkSentListener listener) {
        InviteLinkDialog dialog = new InviteLinkDialog(context, restrictedUser, resourcesProvider);
        dialog.setOnInviteLinkSentListener(listener);
        dialog.show();
    }
}
package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;

public class ProfileInfoComponent extends LinearLayout {
    private Theme.ResourcesProvider resourcesProvider;
    private LinearLayout usernameLayout;
    private OnQrClickListener qrClickListener;
    
    public interface OnQrClickListener {
        void onQrClick();
    }
    private TextView usernameView;
    private LinearLayout bioLayout;
    private TextView bioView;
    private LinearLayout businessHoursLayout;
    private TextView businessHoursView;
    private LinearLayout locationLayout;
    private TextView locationView;
    private LinearLayout verificationLayout;
    private TextView verificationView;
    private LinearLayout subscribersLayout;
    private TextView subscribersView;
    
    public ProfileInfoComponent(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        
        setOrientation(LinearLayout.VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = AndroidUtilities.dp(24); // Reduced spacing from action buttons
        setLayoutParams(params);
        
        createViews();
    }
    
    private void createViews() {
        // Name and status are now in ProfileHeaderComponent, so start with username
        createUsernameLayout();
        createSubscribersLayout();
        createBioLayout();
        createBusinessHoursLayout();
        createLocationLayout();
        createVerificationLayout();
    }
    
    private void createUsernameLayout() {
        usernameLayout = new LinearLayout(getContext());
        usernameLayout.setOrientation(LinearLayout.HORIZONTAL);
        usernameLayout.setGravity(Gravity.CENTER);
        usernameLayout.setVisibility(View.GONE);
        
        LinearLayout.LayoutParams usernameLayoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        usernameLayoutParams.topMargin = AndroidUtilities.dp(8); // Tighter spacing
        usernameLayout.setLayoutParams(usernameLayoutParams);
        
        ImageView atIcon = new ImageView(getContext());
        atIcon.setImageResource(R.drawable.msg_mention);
        atIcon.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
            AndroidUtilities.dp(12), AndroidUtilities.dp(12)); // Smaller icon
        iconParams.gravity = Gravity.CENTER_VERTICAL;
        iconParams.rightMargin = AndroidUtilities.dp(3);
        atIcon.setLayoutParams(iconParams);
        usernameLayout.addView(atIcon);
        
        usernameView = new TextView(getContext());
        usernameView.setTextSize(15);
        usernameView.setTextColor(0xFF666666); // Exact gray color as requested
        usernameLayout.addView(usernameView);
        
        ImageView copyIcon = new ImageView(getContext());
        copyIcon.setImageResource(R.drawable.msg_copy);
        copyIcon.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider));
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(
            AndroidUtilities.dp(12), AndroidUtilities.dp(12)); // Smaller icon
        copyParams.gravity = Gravity.CENTER_VERTICAL;
        copyParams.leftMargin = AndroidUtilities.dp(5);
        copyIcon.setLayoutParams(copyParams);
        usernameLayout.addView(copyIcon);
        
        ImageView qrIcon = new ImageView(getContext());
        qrIcon.setImageResource(R.drawable.msg_qrcode);
        qrIcon.setColorFilter(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, resourcesProvider));
        LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(
            AndroidUtilities.dp(16), AndroidUtilities.dp(16)); // Smaller QR icon
        qrParams.gravity = Gravity.CENTER_VERTICAL;
        qrParams.leftMargin = AndroidUtilities.dp(8);
        qrIcon.setLayoutParams(qrParams);
        qrIcon.setOnClickListener(v -> {
            if (qrClickListener != null) {
                qrClickListener.onQrClick();
            }
        });
        usernameLayout.addView(qrIcon);
        
        addView(usernameLayout);
    }
    
    private void createSubscribersLayout() {
        subscribersLayout = new LinearLayout(getContext());
        subscribersLayout.setOrientation(LinearLayout.HORIZONTAL);
        subscribersLayout.setGravity(Gravity.CENTER);
        subscribersLayout.setVisibility(View.GONE);
        
        LinearLayout.LayoutParams subscribersLayoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        subscribersLayoutParams.topMargin = AndroidUtilities.dp(12);
        subscribersLayout.setLayoutParams(subscribersLayoutParams);
        
        subscribersView = new TextView(getContext());
        subscribersView.setTextSize(14);
        subscribersView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider));
        subscribersLayout.addView(subscribersView);
        
        addView(subscribersLayout);
    }
    
    private void createBioLayout() {
        bioLayout = new LinearLayout(getContext());
        bioLayout.setOrientation(LinearLayout.VERTICAL);
        bioLayout.setVisibility(View.GONE);
        
        LinearLayout.LayoutParams bioLayoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        bioLayoutParams.topMargin = AndroidUtilities.dp(20); // More consistent top padding
        bioLayoutParams.leftMargin = AndroidUtilities.dp(16);
        bioLayoutParams.rightMargin = AndroidUtilities.dp(16);
        bioLayout.setLayoutParams(bioLayoutParams);
        
        TextView bioLabel = new TextView(getContext());
        bioLabel.setText("Bio");
        bioLabel.setTextSize(12);
        bioLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider));
        bioLabel.setAlpha(0.75f); // Further reduced contrast
        bioLabel.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        bioLayout.addView(bioLabel);
        
        bioView = new TextView(getContext());
        bioView.setTextSize(17);
        bioView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        bioView.setLineSpacing(AndroidUtilities.dp(4), 1.0f); // Better line height
        
        LinearLayout.LayoutParams bioParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        bioParams.topMargin = AndroidUtilities.dp(8);
        bioView.setLayoutParams(bioParams);
        bioLayout.addView(bioView);
        
        addView(bioLayout);
    }
    
    private void createBusinessHoursLayout() {
        businessHoursLayout = new LinearLayout(getContext());
        businessHoursLayout.setOrientation(LinearLayout.VERTICAL);
        businessHoursLayout.setVisibility(View.GONE);
        
        LinearLayout.LayoutParams businessLayoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        businessLayoutParams.topMargin = AndroidUtilities.dp(16);
        businessLayoutParams.leftMargin = AndroidUtilities.dp(16);
        businessLayoutParams.rightMargin = AndroidUtilities.dp(16);
        businessHoursLayout.setLayoutParams(businessLayoutParams);
        
        TextView businessLabel = new TextView(getContext());
        businessLabel.setText("Business hours");
        businessLabel.setTextSize(13);
        businessLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider));
        businessLabel.setAlpha(0.75f); // Further reduced contrast
        businessLabel.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        businessHoursLayout.addView(businessLabel);
        
        businessHoursView = new TextView(getContext());
        businessHoursView.setTextSize(15);
        businessHoursView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        
        LinearLayout.LayoutParams businessHoursParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        businessHoursParams.topMargin = AndroidUtilities.dp(4);
        businessHoursView.setLayoutParams(businessHoursParams);
        businessHoursLayout.addView(businessHoursView);
        
        addView(businessHoursLayout);
    }
    
    private void createLocationLayout() {
        locationLayout = new LinearLayout(getContext());
        locationLayout.setOrientation(LinearLayout.VERTICAL);
        locationLayout.setVisibility(View.GONE);
        
        LinearLayout.LayoutParams locationLayoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        locationLayoutParams.topMargin = AndroidUtilities.dp(16);
        locationLayoutParams.leftMargin = AndroidUtilities.dp(16);
        locationLayoutParams.rightMargin = AndroidUtilities.dp(16);
        locationLayout.setLayoutParams(locationLayoutParams);
        
        TextView locationLabel = new TextView(getContext());
        locationLabel.setText("Location");
        locationLabel.setTextSize(13);
        locationLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider));
        locationLabel.setAlpha(0.75f); // Further reduced contrast
        locationLabel.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        locationLayout.addView(locationLabel);
        
        LinearLayout locationRow = new LinearLayout(getContext());
        locationRow.setOrientation(LinearLayout.HORIZONTAL);
        locationRow.setGravity(Gravity.CENTER_VERTICAL);
        
        LinearLayout.LayoutParams locationRowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        locationRowParams.topMargin = AndroidUtilities.dp(4);
        locationRow.setLayoutParams(locationRowParams);
        
        locationView = new TextView(getContext());
        locationView.setTextSize(15);
        locationView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        
        LinearLayout.LayoutParams locationTextParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        locationView.setLayoutParams(locationTextParams);
        locationRow.addView(locationView);
        
        ImageView mapIcon = new ImageView(getContext());
        mapIcon.setImageResource(R.drawable.msg_map);
        mapIcon.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider));
        LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(
            AndroidUtilities.dp(32), AndroidUtilities.dp(32));
        mapParams.leftMargin = AndroidUtilities.dp(8);
        mapIcon.setLayoutParams(mapParams);
        locationRow.addView(mapIcon);
        
        locationLayout.addView(locationRow);
        addView(locationLayout);
    }
    
    private void createVerificationLayout() {
        verificationLayout = new LinearLayout(getContext());
        verificationLayout.setOrientation(LinearLayout.HORIZONTAL);
        verificationLayout.setGravity(Gravity.CENTER);
        verificationLayout.setVisibility(View.GONE);
        
        LinearLayout.LayoutParams verificationLayoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        verificationLayoutParams.topMargin = AndroidUtilities.dp(16);
        verificationLayoutParams.leftMargin = AndroidUtilities.dp(16);
        verificationLayoutParams.rightMargin = AndroidUtilities.dp(16);
        verificationLayout.setLayoutParams(verificationLayoutParams);
        
        GradientDrawable verificationBg = new GradientDrawable();
        verificationBg.setCornerRadius(AndroidUtilities.dp(8));
        verificationBg.setColor(Theme.getColor(Theme.key_avatar_backgroundGreen, resourcesProvider));
        verificationLayout.setBackground(verificationBg);
        verificationLayout.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), 
                                    AndroidUtilities.dp(12), AndroidUtilities.dp(8));
        
        ImageView verificationIcon = new ImageView(getContext());
        verificationIcon.setImageResource(R.drawable.verified_area);
        verificationIcon.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
        LinearLayout.LayoutParams verificationIconParams = new LinearLayout.LayoutParams(
            AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        verificationIconParams.gravity = Gravity.CENTER_VERTICAL;
        verificationIconParams.rightMargin = AndroidUtilities.dp(6);
        verificationIcon.setLayoutParams(verificationIconParams);
        verificationLayout.addView(verificationIcon);
        
        verificationView = new TextView(getContext());
        verificationView.setTextSize(13);
        verificationView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
        verificationView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        verificationLayout.addView(verificationView);
        
        addView(verificationLayout);
    }
    
    // Name and status are now handled by ProfileHeaderComponent
    
    public void setOnQrClickListener(OnQrClickListener listener) {
        this.qrClickListener = listener;
    }
    
    public void setUsername(String username) {
        if (!TextUtils.isEmpty(username)) {
            // Remove any existing @ prefix before adding our own
            String cleanUsername = username.startsWith("@") ? username.substring(1) : username;
            usernameView.setText(cleanUsername);
            usernameLayout.setVisibility(View.VISIBLE);
        } else {
            usernameLayout.setVisibility(View.GONE);
        }
    }
    
    public void setSubscribers(String subscribersText) {
        if (!TextUtils.isEmpty(subscribersText)) {
            subscribersView.setText(subscribersText);
            subscribersLayout.setVisibility(View.VISIBLE);
        } else {
            subscribersLayout.setVisibility(View.GONE);
        }
    }
    
    public void setBio(String bio) {
        if (!TextUtils.isEmpty(bio)) {
            bioView.setText(bio);
            bioLayout.setVisibility(View.VISIBLE);
        } else {
            bioLayout.setVisibility(View.GONE);
        }
    }
    
    public void setBusinessHours(String hours, boolean isOpen) {
        if (!TextUtils.isEmpty(hours)) {
            // Handle colored text for business hours: "Closed • opens in 15 hours"
            if (hours.contains("•")) {
                // Split status and additional info
                String[] parts = hours.split("•", 2);
                if (parts.length == 2) {
                    String status = parts[0].trim(); // "Closed" or "Open"
                    String info = parts[1].trim();   // "opens in 15 hours"
                    
                    // Create spannable string with different colors
                    SpannableString spannableText = new SpannableString(status + " • " + info);
                    
                    // Color the status part (Closed/Open)
                    int statusColor = isOpen ? 
                        Theme.getColor(Theme.key_avatar_backgroundGreen, resourcesProvider) :
                        Theme.getColor(Theme.key_text_RedRegular, resourcesProvider);
                    spannableText.setSpan(new ForegroundColorSpan(statusColor), 0, status.length(), 
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    
                    // Color the bullet and additional info in gray
                    int grayColor = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider);
                    spannableText.setSpan(new ForegroundColorSpan(grayColor), status.length(), 
                        spannableText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    
                    businessHoursView.setText(spannableText);
                } else {
                    // Fallback to simple coloring
                    businessHoursView.setText(hours);
                    if (isOpen) {
                        businessHoursView.setTextColor(Theme.getColor(Theme.key_avatar_backgroundGreen, resourcesProvider));
                    } else {
                        businessHoursView.setTextColor(Theme.getColor(Theme.key_text_RedRegular, resourcesProvider));
                    }
                }
            } else {
                // Simple text without separator - just color based on open/closed status
                businessHoursView.setText(hours);
                if (isOpen) {
                    businessHoursView.setTextColor(Theme.getColor(Theme.key_avatar_backgroundGreen, resourcesProvider));
                } else {
                    businessHoursView.setTextColor(Theme.getColor(Theme.key_text_RedRegular, resourcesProvider));
                }
            }
            businessHoursLayout.setVisibility(View.VISIBLE);
        } else {
            businessHoursLayout.setVisibility(View.GONE);
        }
    }
    
    public void setLocation(String location) {
        if (!TextUtils.isEmpty(location)) {
            locationView.setText(location);
            locationLayout.setVisibility(View.VISIBLE);
        } else {
            locationLayout.setVisibility(View.GONE);
        }
    }
    
    public void setVerification(String verificationText) {
        if (!TextUtils.isEmpty(verificationText)) {
            verificationView.setText(verificationText);
            verificationLayout.setVisibility(View.VISIBLE);
        } else {
            verificationLayout.setVisibility(View.GONE);
        }
    }
    
    public void updateTheme() {
        // Name and status theming is now handled by ProfileHeaderComponent
        usernameView.setTextColor(0xFF666666); // Keep muted gray color
        subscribersView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider));
        bioView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        locationView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        verificationView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
        
        GradientDrawable verificationBg = (GradientDrawable) verificationLayout.getBackground();
        if (verificationBg != null) {
            verificationBg.setColor(Theme.getColor(Theme.key_avatar_backgroundGreen, resourcesProvider));
        }
    }
}
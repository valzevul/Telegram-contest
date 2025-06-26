package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;

public class ProfileBioUsernameComponent extends LinearLayout {
    private Theme.ResourcesProvider resourcesProvider;
    private LinearLayout bioLayout;
    private TextView bioTitleView;
    private TextView bioContentView;
    private LinearLayout usernameLayout;
    private LinearLayout usernameContainer;
    private TextView usernameTitleView;
    private TextView usernameContentView;
    private ImageView qrCodeView;
    private OnQrClickListener qrClickListener;
    
    public interface OnQrClickListener {
        void onQrClick();
    }
    
    public ProfileBioUsernameComponent(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        
        setOrientation(LinearLayout.VERTICAL);
        
        // Set grey background for section separation
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray, resourcesProvider));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        setLayoutParams(params);
        
        // Create white background container for content
        LinearLayout contentContainer = new LinearLayout(getContext());
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
        
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        containerParams.topMargin = AndroidUtilities.dp(8);
        containerParams.bottomMargin = AndroidUtilities.dp(8);
        contentContainer.setLayoutParams(containerParams);
        
        createBioSection();
        createUsernameSection();
        
        // Add bio and username to content container
        contentContainer.addView(bioLayout);
        contentContainer.addView(usernameContainer);
        
        addView(contentContainer);
    }
    
    private void addTopSeparator() {
        View separator = new View(getContext());
        separator.setBackgroundColor(Theme.getColor(Theme.key_divider, resourcesProvider));
        LinearLayout.LayoutParams separatorParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            AndroidUtilities.dp(1));
        separatorParams.topMargin = AndroidUtilities.dp(8);
        separatorParams.bottomMargin = AndroidUtilities.dp(16);
        separator.setLayoutParams(separatorParams);
        addView(separator);
    }
    
    private void addBottomSeparator() {
        View separator = new View(getContext());
        separator.setBackgroundColor(Theme.getColor(Theme.key_divider, resourcesProvider));
        LinearLayout.LayoutParams separatorParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            AndroidUtilities.dp(1));
        separatorParams.topMargin = AndroidUtilities.dp(8);
        separatorParams.bottomMargin = AndroidUtilities.dp(8);
        separator.setLayoutParams(separatorParams);
        addView(separator);
    }
    
    private void createBioSection() {
        bioLayout = new LinearLayout(getContext());
        bioLayout.setOrientation(LinearLayout.VERTICAL);
        bioLayout.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), 0);
        bioLayout.setVisibility(View.GONE); // Hidden by default
        
        LinearLayout.LayoutParams bioLayoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        bioLayoutParams.bottomMargin = AndroidUtilities.dp(16);
        bioLayout.setLayoutParams(bioLayoutParams);
        
        // Bio title
        bioTitleView = new TextView(getContext());
        bioTitleView.setText("Bio");
        bioTitleView.setTextSize(14);
        bioTitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider));
        bioTitleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        
        LinearLayout.LayoutParams bioTitleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        bioTitleParams.bottomMargin = AndroidUtilities.dp(4);
        bioTitleView.setLayoutParams(bioTitleParams);
        bioLayout.addView(bioTitleView);
        
        // Bio content
        bioContentView = new TextView(getContext());
        bioContentView.setTextSize(16);
        bioContentView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        bioContentView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        bioLayout.addView(bioContentView);
    }
    
    private void createUsernameSection() {
        // Container for username and QR code
        usernameContainer = new LinearLayout(getContext());
        usernameContainer.setOrientation(LinearLayout.HORIZONTAL);
        usernameContainer.setGravity(Gravity.CENTER_VERTICAL);
        usernameContainer.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        usernameContainer.setVisibility(View.GONE); // Hidden by default
        
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        usernameContainer.setLayoutParams(containerParams);
        
        // Username section (left side)
        usernameLayout = new LinearLayout(getContext());
        usernameLayout.setOrientation(LinearLayout.VERTICAL);
        
        LinearLayout.LayoutParams usernameLayoutParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        usernameLayout.setLayoutParams(usernameLayoutParams);
        
        // Username title
        usernameTitleView = new TextView(getContext());
        usernameTitleView.setText("Username");
        usernameTitleView.setTextSize(14);
        usernameTitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider));
        usernameTitleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        
        LinearLayout.LayoutParams usernameTitleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        usernameTitleParams.bottomMargin = AndroidUtilities.dp(4);
        usernameTitleView.setLayoutParams(usernameTitleParams);
        usernameLayout.addView(usernameTitleView);
        
        // Username content
        usernameContentView = new TextView(getContext());
        usernameContentView.setTextSize(15);
        usernameContentView.setTextColor(0xFF666666); // Exact gray color as requested
        usernameContentView.setAlpha(0.9f); // Slightly reduced opacity
        usernameLayout.addView(usernameContentView);
        
        usernameContainer.addView(usernameLayout);
        
        // QR code (right side)
        qrCodeView = new ImageView(getContext());
        qrCodeView.setImageResource(R.drawable.msg_qrcode);
        qrCodeView.setColorFilter(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, resourcesProvider));
        qrCodeView.setScaleType(ImageView.ScaleType.CENTER);
        qrCodeView.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), 
                             AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        
        // No background for QR button - transparent like the rest
        qrCodeView.setBackground(null);
        
        LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(
            AndroidUtilities.dp(40), AndroidUtilities.dp(40));
        qrParams.leftMargin = AndroidUtilities.dp(16);
        qrCodeView.setLayoutParams(qrParams);
        
        qrCodeView.setOnClickListener(v -> {
            if (qrClickListener != null) {
                qrClickListener.onQrClick();
            }
        });
        
        usernameContainer.addView(qrCodeView);
    }
    
    public void setBio(String bio) {
        if (!TextUtils.isEmpty(bio)) {
            bioContentView.setText(bio);
            bioLayout.setVisibility(View.VISIBLE);
        } else {
            bioLayout.setVisibility(View.GONE);
        }
    }
    
    public void setUsername(String username) {
        if (!TextUtils.isEmpty(username)) {
            usernameContentView.setText("@" + username);
            usernameContainer.setVisibility(View.VISIBLE);
        } else {
            usernameContainer.setVisibility(View.GONE);
        }
    }
    
    public void setUser(TLRPC.User user, TLRPC.UserFull userInfo) {
        // Set bio from userInfo
        if (userInfo != null && !TextUtils.isEmpty(userInfo.about)) {
            setBio(userInfo.about);
        }
        
        // Set username from user
        if (user != null && !TextUtils.isEmpty(user.username)) {
            setUsername(user.username);
        }
    }
    
    public void setOnQrClickListener(OnQrClickListener listener) {
        this.qrClickListener = listener;
    }
    
    public void updateTheme() {
        bioTitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider));
        bioContentView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        usernameTitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider));
        usernameContentView.setTextColor(0xFF666666); // Keep muted gray color
        usernameContentView.setAlpha(0.9f); // Maintain consistent opacity
        qrCodeView.setColorFilter(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, resourcesProvider));
    }
}
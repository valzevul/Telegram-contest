package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;

public class ProfileMessagePreviewComponent extends LinearLayout {
    private Theme.ResourcesProvider resourcesProvider;
    private BackupImageView channelAvatarView;
    private TextView channelNameView;
    private TextView channelTypeView;
    private TextView messageTimeView;
    private TextView messageTextView;
    private BackupImageView messageThumbnailView;
    private OnMessageClickListener listener;
    
    public interface OnMessageClickListener {
        void onMessageClick();
    }
    
    public ProfileMessagePreviewComponent(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        
        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), 
                   AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = AndroidUtilities.dp(8);
        params.leftMargin = AndroidUtilities.dp(12);
        params.rightMargin = AndroidUtilities.dp(12);
        setLayoutParams(params);
        
        // White background without rounded corners for section look
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
        
        // Add subtle bottom border for visual separation
        setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), 
                   AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        
        createViews();
    }
    
    private void createViews() {
        // Channel avatar
        channelAvatarView = new BackupImageView(getContext());
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(
            AndroidUtilities.dp(40), AndroidUtilities.dp(40));
        avatarParams.rightMargin = AndroidUtilities.dp(12);
        channelAvatarView.setLayoutParams(avatarParams);
        channelAvatarView.getImageReceiver().setRoundRadius(AndroidUtilities.dp(20));
        addView(channelAvatarView);
        
        // Text container
        LinearLayout textContainer = new LinearLayout(getContext());
        textContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textContainer.setLayoutParams(textParams);
        
        // Header row (channel name + type + time)
        LinearLayout headerRow = new LinearLayout(getContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        headerRow.setLayoutParams(headerParams);
        
        // Channel name
        channelNameView = new TextView(getContext());
        channelNameView.setTextSize(15);
        channelNameView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        channelNameView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        channelNameView.setMaxLines(1);
        channelNameView.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        nameParams.rightMargin = AndroidUtilities.dp(8);
        channelNameView.setLayoutParams(nameParams);
        headerRow.addView(channelNameView);
        
        // Channel type badge
        channelTypeView = new TextView(getContext());
        channelTypeView.setTextSize(13);
        channelTypeView.setTextColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, resourcesProvider));
        channelTypeView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        channelTypeView.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(2),
                                   AndroidUtilities.dp(6), AndroidUtilities.dp(2));
        GradientDrawable typeBg = new GradientDrawable();
        typeBg.setCornerRadius(AndroidUtilities.dp(8));
        typeBg.setColor(0x1A0099FF); // Light blue background
        channelTypeView.setBackground(typeBg);
        LinearLayout.LayoutParams typeParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        typeParams.rightMargin = AndroidUtilities.dp(8);
        channelTypeView.setLayoutParams(typeParams);
        headerRow.addView(channelTypeView);
        
        // Spacer
        View spacer = new View(getContext());
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
            0, AndroidUtilities.dp(1), 1f);
        spacer.setLayoutParams(spacerParams);
        headerRow.addView(spacer);
        
        // Time - hidden for channel preview in profile
        messageTimeView = new TextView(getContext());
        messageTimeView.setTextSize(13);
        messageTimeView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider));
        messageTimeView.setVisibility(View.GONE); // Hide timestamps in profile
        headerRow.addView(messageTimeView);
        
        textContainer.addView(headerRow);
        
        // Message text
        messageTextView = new TextView(getContext());
        messageTextView.setTextSize(14);
        messageTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
        messageTextView.setMaxLines(2);
        messageTextView.setEllipsize(TextUtils.TruncateAt.END);
        messageTextView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        messageParams.topMargin = AndroidUtilities.dp(4);
        messageTextView.setLayoutParams(messageParams);
        textContainer.addView(messageTextView);
        
        addView(textContainer);
        
        // Message thumbnail (for photos/videos)
        messageThumbnailView = new BackupImageView(getContext());
        LinearLayout.LayoutParams thumbnailParams = new LinearLayout.LayoutParams(
            AndroidUtilities.dp(40), AndroidUtilities.dp(40));
        thumbnailParams.leftMargin = AndroidUtilities.dp(8);
        messageThumbnailView.setLayoutParams(thumbnailParams);
        messageThumbnailView.getImageReceiver().setRoundRadius(AndroidUtilities.dp(8));
        messageThumbnailView.setVisibility(View.GONE); // Hidden by default
        addView(messageThumbnailView);
        
        setOnClickListener(v -> {
            if (listener != null) {
                listener.onMessageClick();
            }
        });
    }
    
    public void setChannelInfo(String channelName, String channelType, String avatarUrl) {
        try {
            if (channelNameView != null && !TextUtils.isEmpty(channelName)) {
                channelNameView.setText(channelName);
            }
            
            if (channelTypeView != null) {
                if (!TextUtils.isEmpty(channelType)) {
                    channelTypeView.setText(channelType);
                    channelTypeView.setVisibility(View.VISIBLE);
                } else {
                    channelTypeView.setVisibility(View.GONE);
                }
            }
            
            if (channelAvatarView != null && !TextUtils.isEmpty(avatarUrl)) {
                // In real implementation, load image from URL
                // channelAvatarView.setImage(ImageLocation.getForPath(avatarUrl), "40_40", null);
            }
        } catch (Exception e) {
            // Ignore errors to prevent hanging
        }
    }
    
    public void setMessageInfo(String messageText, String time) {
        try {
            if (messageTextView != null && !TextUtils.isEmpty(messageText)) {
                messageTextView.setText(messageText);
            }
            
            if (messageTimeView != null && !TextUtils.isEmpty(time)) {
                messageTimeView.setText(time);
            }
        } catch (Exception e) {
            // Ignore errors to prevent hanging
        }
    }
    
    public void setChannelName(String channelName) {
        if (channelNameView != null && !TextUtils.isEmpty(channelName)) {
            channelNameView.setText(channelName);
        }
    }
    
    public void setMessageText(String messageText) {
        if (messageTextView != null && !TextUtils.isEmpty(messageText)) {
            messageTextView.setText(messageText);
        }
    }
    
    public void setMessageThumbnail(org.telegram.messenger.MessageObject messageObject) {
        if (messageThumbnailView == null) return;
        
        if (messageObject != null && messageObject.isPhoto()) {
            // Load photo thumbnail with higher quality to avoid blur
            TLRPC.PhotoSize photoSize = org.telegram.messenger.FileLoader.getClosestPhotoSizeWithSize(
                messageObject.photoThumbs, AndroidUtilities.dp(80)); // Higher quality
            if (photoSize != null) {
                messageThumbnailView.setImage(
                    ImageLocation.getForPhoto(photoSize, messageObject.messageOwner.media.photo), 
                    "40_40", null, null, 0, messageObject);
                messageThumbnailView.setVisibility(View.VISIBLE);
            } else {
                messageThumbnailView.setVisibility(View.GONE);
            }
        } else if (messageObject != null && (messageObject.isVideo() || messageObject.isGif())) {
            // Load video/GIF thumbnail
            TLRPC.PhotoSize thumb = null;
            if (messageObject.getDocument() != null && messageObject.getDocument().thumbs != null) {
                thumb = org.telegram.messenger.FileLoader.getClosestPhotoSizeWithSize(
                    messageObject.getDocument().thumbs, AndroidUtilities.dp(40));
            }
            
            if (thumb != null) {
                messageThumbnailView.setImage(
                    ImageLocation.getForDocument(thumb, messageObject.getDocument()), 
                    "40_40", null, null, 0, messageObject);
                messageThumbnailView.setVisibility(View.VISIBLE);
            } else {
                // Try message photo thumbs as fallback
                TLRPC.PhotoSize photoThumb = org.telegram.messenger.FileLoader.getClosestPhotoSizeWithSize(
                    messageObject.photoThumbs, AndroidUtilities.dp(40));
                if (photoThumb != null) {
                    messageThumbnailView.setImage(
                        ImageLocation.getForPhoto(photoThumb, messageObject.messageOwner.media.photo), 
                        "40_40", null, null, 0, messageObject);
                    messageThumbnailView.setVisibility(View.VISIBLE);
                } else {
                    messageThumbnailView.setVisibility(View.GONE);
                }
            }
        } else {
            // Hide thumbnail for non-media messages
            messageThumbnailView.setVisibility(View.GONE);
        }
    }
    
    public void setChannelType(String type) {
        if (channelTypeView != null && !android.text.TextUtils.isEmpty(type)) {
            channelTypeView.setText(type);
            channelTypeView.setVisibility(View.VISIBLE);
        } else if (channelTypeView != null) {
            channelTypeView.setVisibility(View.GONE);
        }
    }
    
    public void setMessageTime(String time) {
        if (messageTimeView != null && !android.text.TextUtils.isEmpty(time)) {
            messageTimeView.setText(time);
            messageTimeView.setVisibility(View.VISIBLE);
        } else if (messageTimeView != null) {
            messageTimeView.setVisibility(View.GONE);
        }
    }
    
    public void setChannelAvatar(TLRPC.Chat channel, int account) {
        if (channelAvatarView != null && channel != null) {
            AvatarDrawable avatarDrawable = new AvatarDrawable(channel);
            
            if (channel.photo != null) {
                ImageLocation imageLocation = ImageLocation.getForUserOrChat(channel, ImageLocation.TYPE_SMALL);
                channelAvatarView.setImage(imageLocation, "40_40", avatarDrawable, channel);
            } else {
                channelAvatarView.setImageDrawable(avatarDrawable);
            }
        }
    }
    
    public void setOnMessageClickListener(OnMessageClickListener listener) {
        this.listener = listener;
    }
    
    public void updateTheme() {
        GradientDrawable background = (GradientDrawable) getBackground();
        if (background != null) {
            background.setColor(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
            background.setStroke(AndroidUtilities.dp(1), 
                Theme.getColor(Theme.key_windowBackgroundGray, resourcesProvider));
        }
        
        if (channelNameView != null) {
            channelNameView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        }
        if (channelTypeView != null) {
            channelTypeView.setTextColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, resourcesProvider));
        }
        if (messageTimeView != null) {
            messageTimeView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider));
        }
        if (messageTextView != null) {
            messageTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
        }
    }
    
    public static ProfileMessagePreviewComponent createSamplePreview(Context context, Theme.ResourcesProvider resourcesProvider) {
        ProfileMessagePreviewComponent preview = new ProfileMessagePreviewComponent(context, resourcesProvider);
        preview.setChannelInfo("Duck Artist", "personal channel", null);
        preview.setMessageInfo("Just tried the new iOS redesign — clean but kinda safe? Loving the sleek sh...", "12:45");
        return preview;
    }
}
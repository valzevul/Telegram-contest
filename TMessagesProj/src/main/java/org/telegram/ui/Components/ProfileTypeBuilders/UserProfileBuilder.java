package org.telegram.ui.Components.ProfileTypeBuilders;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import org.telegram.ui.Components.PullToExpandScrollView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.QrActivity;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.InviteLinkDialog;
import org.telegram.ui.Components.MuteOptionsDialog;
import org.telegram.ui.Components.ProfileActionsComponent;
import org.telegram.ui.Components.ProfileBioUsernameComponent;
import org.telegram.ui.Components.ProfileContentComponent;
import org.telegram.ui.Components.ProfileHeaderComponent;
import org.telegram.ui.Components.PullToExpandHeaderView;
import org.telegram.ui.Components.ProfileInfoComponent;
import org.telegram.ui.Components.ProfileMessagePreviewComponent;
import org.telegram.ui.Components.ProfileTabsComponent;
import org.telegram.ui.Components.ProfileThemeManager;
import org.telegram.ui.Components.SharedMediaLayout;

public class UserProfileBuilder {
    private Context context;
    private Theme.ResourcesProvider resourcesProvider;
    private ProfileThemeManager themeManager;
    private org.telegram.ui.ActionBar.BaseFragment parentFragment;
    private SharedMediaLayout.SharedMediaPreloader sharedMediaPreloader;
    
    private ProfileHeaderComponent headerComponent;
    private PullToExpandHeaderView expandableHeader;
    private ProfileInfoComponent infoComponent;
    private ProfileBioUsernameComponent bioUsernameComponent;
    private ProfileActionsComponent actionsComponent;
    private ProfileMessagePreviewComponent messagePreviewComponent;
    private ProfileTabsComponent tabsComponent;
    private ProfileContentComponent contentComponent;
    
    private TLRPC.User user;
    private TLRPC.UserFull userInfo;
    private long userId;
    private boolean isBot;
    private org.telegram.ui.Cells.ProfileChannelCell.ChannelMessageFetcher channelMessageFetcher;
    
    public UserProfileBuilder(Context context, Theme.ResourcesProvider resourcesProvider) {
        this.context = context;
        this.resourcesProvider = resourcesProvider;
        this.themeManager = ProfileThemeManager.create(resourcesProvider);
    }
    
    public UserProfileBuilder setParentFragment(org.telegram.ui.ActionBar.BaseFragment fragment) {
        this.parentFragment = fragment;
        return this;
    }
    
    public UserProfileBuilder setSharedMediaPreloader(SharedMediaLayout.SharedMediaPreloader preloader) {
        this.sharedMediaPreloader = preloader;
        return this;
    }
    
    private void initializeSharedMediaPreloader() {
        // SharedMediaPreloader should already be set by ProfileActivity via UserProfileViewBuilder
        // If not set, we'll rely on fallback methods in createMediaContent()
        if (sharedMediaPreloader != null) {
            android.util.Log.d("UserProfileBuilder", "SharedMediaPreloader is available, will use real media data");
        } else {
            android.util.Log.d("UserProfileBuilder", "SharedMediaPreloader is null, will use fallback methods");
        }
    }
    
    public UserProfileBuilder setUser(TLRPC.User user) {
        this.user = user;
        this.userId = user != null ? user.id : 0;
        this.isBot = user != null && user.bot;
        return this;
    }
    
    public UserProfileBuilder setUserInfo(TLRPC.UserFull userInfo) {
        this.userInfo = userInfo;
        return this;
    }
    
    public UserProfileBuilder setUserId(long userId) {
        this.userId = userId;
        this.user = MessagesController.getInstance(0).getUser(userId);
        this.isBot = user != null && user.bot;
        return this;
    }
    
    public View build() {
        if (user == null) {
            return new View(context);
        }
        
        // Create a container that holds header at top and scrollable content below
        FrameLayout container = new FrameLayout(context);
        container.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));
        
        // Create scroll view for content
        PullToExpandScrollView scrollView = new PullToExpandScrollView(context);
        scrollView.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        
        LinearLayout scrollContent = new LinearLayout(context);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        scrollContent.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        
        initializeSharedMediaPreloader();
        createComponents();
        setupComponents();
        
        // Add a spacer at the top of scroll content to push content below header
        View headerSpacer = new View(context);
        headerSpacer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            AndroidUtilities.dp(320))); // Initial header height
        scrollContent.addView(headerSpacer);
        
        // Add content components to scroll view
        if (messagePreviewComponent != null) {
            scrollContent.addView(messagePreviewComponent);
        }
        scrollContent.addView(bioUsernameComponent);
        scrollContent.addView(tabsComponent);
        scrollContent.addView(contentComponent);
        
        scrollView.addView(scrollContent);
        
        // Add scroll view to container first
        container.addView(scrollView);
        
        // Add header on top of scroll view (overlay)
        expandableHeader.setScrollView(scrollView);
        scrollView.setExpandableHeader(expandableHeader);
        container.addView(expandableHeader);
        
        // Update spacer height when header changes
        expandableHeader.setHeaderSpacerView(headerSpacer);
        
        return container;
    }
    
    private void createComponents() {
        // Create expandable header
        expandableHeader = new PullToExpandHeaderView(context, resourcesProvider);
        headerComponent = expandableHeader.getHeaderComponent();
        
        infoComponent = new ProfileInfoComponent(context, resourcesProvider);
        bioUsernameComponent = new ProfileBioUsernameComponent(context, resourcesProvider);
        actionsComponent = new ProfileActionsComponent(context, resourcesProvider);
        // Create message preview component for personal channel
        messagePreviewComponent = new ProfileMessagePreviewComponent(context, resourcesProvider);
        tabsComponent = new ProfileTabsComponent(context, resourcesProvider);
        contentComponent = new ProfileContentComponent(context, resourcesProvider);
    }
    
    private void setupComponents() {
        setupHeader();
        setupInfo();
        setupBioUsername();
        setupActions();
        setupMessagePreview();
        setupTabs();
        setupContent();
        
        themeManager.applyGradientForProfileType(headerComponent, ProfileThemeManager.ProfileType.USER);
    }
    
    private void setupHeader() {
        if (user == null) {
            return;
        }
        
        // Set user on expandable header
        expandableHeader.setUser(user);
        
        // Set name and status on header component (white text on gradient)
        String name = ContactsController.formatName(user.first_name, user.last_name);
        if (name != null && !name.trim().isEmpty()) {
            headerComponent.setName(name);
        }
        
        String status = getUserStatus();
        if (status != null && !status.isEmpty()) {
            headerComponent.setStatus(status);
        }
        
        // Set up menu click listener to delegate to parent fragment
        headerComponent.setOnMenuClickListener(this::onMenuClick);
        
        // Set up back click listener to delegate to parent fragment
        headerComponent.setOnBackClickListener(this::onBackClick);
        
        // Set up action click listener for integrated action buttons
        headerComponent.setOnActionClickListener(this::onHeaderActionClick);
    }
    
    private void setupInfo() {
        // Add null checks to prevent crashes
        if (user == null) {
            return;
        }
        
        // ProfileInfoComponent now handles other profile info only (not bio/username)
        // Bio and username are handled by ProfileBioUsernameComponent
        
        // Set up any other profile information if needed
        // (subscribers, verification, business hours, location, etc.)
    }
    
    private void setupBioUsername() {
        // Add null checks to prevent crashes
        if (user == null) {
            return;
        }
        
        try {
            // Set user and userInfo for bio and username display
            bioUsernameComponent.setUser(user, userInfo);
            
            // Set up QR code click listener
            bioUsernameComponent.setOnQrClickListener(this::onQrClick);
            
        } catch (Exception e) {
            // Log error but don't crash
            android.util.Log.e("UserProfileBuilder", "Error setting up bio/username component", e);
        }
    }
    
    private void setupActions() {
        ProfileActionsComponent.ActionButton[] actions;
        
        if (isBot) {
            actions = ProfileActionsComponent.getDefaultBotActions();
        } else {
            // Check if current user is premium to show gift button
            boolean currentUserIsPremium = false;
            try {
                if (parentFragment != null) {
                    org.telegram.messenger.UserConfig userConfig = org.telegram.messenger.UserConfig.getInstance(parentFragment.getCurrentAccount());
                    TLRPC.User currentUser = userConfig.getCurrentUser();
                    currentUserIsPremium = currentUser != null && currentUser.premium;
                }
            } catch (Exception e) {
                // Default to false if we can't determine premium status
            }
            
            if (currentUserIsPremium) {
                actions = ProfileActionsComponent.getUserActionsWithGift();
            } else {
                actions = ProfileActionsComponent.getUserActionsWithMute();
            }
        }
        
        actionsComponent.setActions(actions);
        actionsComponent.setOnActionClickListener(this::onActionClick);
    }
    
    private void setupMessagePreview() {
        if (messagePreviewComponent != null) {
            boolean channelShown = false;
            
            // Check for real personal channel first
            if (userInfo != null && userInfo.personal_channel_id != 0) {
                try {
                    int accountId = parentFragment != null ? parentFragment.getCurrentAccount() : 0;
                    TLRPC.Chat personalChannel = MessagesController.getInstance(accountId).getChat(userInfo.personal_channel_id);
                    if (personalChannel != null) {
                        // Set up basic channel info
                        messagePreviewComponent.setChannelName(personalChannel.title);
                        messagePreviewComponent.setChannelType("personal channel");
                        
                        // Load channel avatar
                        messagePreviewComponent.setChannelAvatar(personalChannel, accountId);
                        
                        // Use the proper ChannelMessageFetcher like the old ProfileActivity
                        if (channelMessageFetcher == null) {
                            channelMessageFetcher = new org.telegram.ui.Cells.ProfileChannelCell.ChannelMessageFetcher(accountId);
                        }
                        
                        // Set up the fetcher callback to update the preview when message loads
                        channelMessageFetcher.subscribe(() -> {
                            if (channelMessageFetcher.messageObject != null) {
                                setupChannelPreview(channelMessageFetcher.messageObject, personalChannel);
                            } else {
                                // Show basic channel info when no message is available
                                messagePreviewComponent.setMessageText("View Channel");
                                messagePreviewComponent.setMessageTime("Channel");
                            }
                        });
                        
                        // Fetch the channel message using the proper method
                        channelMessageFetcher.fetch(userInfo);
                        
                        // Set up click listener
                        messagePreviewComponent.setOnMessageClickListener(() -> {
                            if (parentFragment != null) {
                                android.os.Bundle args = new android.os.Bundle();
                                args.putLong("chat_id", personalChannel.id);
                                parentFragment.presentFragment(new org.telegram.ui.ChatActivity(args));
                            }
                        });
                        
                        messagePreviewComponent.setVisibility(View.VISIBLE);
                        channelShown = true;
                    }
                } catch (Exception e) {
                    // Continue to check for alternative channel sources
                }
            }
            
            // If no channel shown, hide the preview
            if (!channelShown) {
                messagePreviewComponent.setVisibility(View.GONE);
            }
        }
    }
    
    private MessageObject getLatestChannelMessage(long channelId) {
        try {
            if (parentFragment != null) {
                MessagesController messagesController = MessagesController.getInstance(parentFragment.getCurrentAccount());
                
                // Use negative ID for channels in dialog map
                long dialogId = -channelId;
                
                // Priority 1: Get from SharedMediaPreloader if available
                if (sharedMediaPreloader != null) {
                    try {
                        SharedMediaLayout.SharedMediaData[] sharedMediaData = sharedMediaPreloader.getSharedMediaData();
                        if (sharedMediaData != null) {
                            // Check all media types for messages from this channel
                            for (SharedMediaLayout.SharedMediaData data : sharedMediaData) {
                                if (data != null && data.messages != null && !data.messages.isEmpty()) {
                                    for (MessageObject msg : data.messages) {
                                        if (msg != null && msg.getDialogId() == dialogId) {
                                            return msg;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Continue with other methods
                    }
                }
                
                // Priority 2: Get from direct dialog message array
                java.util.ArrayList<MessageObject> dialogMessages = messagesController.dialogMessage.get(dialogId);
                if (dialogMessages != null && !dialogMessages.isEmpty()) {
                    MessageObject msg = dialogMessages.get(0);
                    if (msg != null && msg.messageOwner != null) {
                        return msg;
                    }
                }
                
                // Priority 3: Search all dialogs for any message from this channel
                for (int i = 0; i < messagesController.dialogMessage.size(); i++) {
                    long currentDialogId = messagesController.dialogMessage.keyAt(i);
                    java.util.ArrayList<MessageObject> messages = messagesController.dialogMessage.valueAt(i);
                    
                    if (messages != null && !messages.isEmpty()) {
                        for (MessageObject message : messages) {
                            if (message != null && message.getDialogId() == dialogId) {
                                return message;
                            }
                            
                            // Also check for forwarded messages from this channel
                            if (message != null && message.messageOwner != null && 
                                message.messageOwner.fwd_from != null &&
                                message.messageOwner.fwd_from.from_id instanceof TLRPC.TL_peerChannel) {
                                long fwdChannelId = ((TLRPC.TL_peerChannel) message.messageOwner.fwd_from.from_id).channel_id;
                                if (fwdChannelId == channelId) {
                                    return message;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Return null on error
        }
        return null;
    }
    
    private MessageObject findChannelMessageInAllDialogs(TLRPC.Chat channel) {
        try {
            if (parentFragment != null && channel != null) {
                MessagesController messagesController = MessagesController.getInstance(parentFragment.getCurrentAccount());
                long channelDialogId = -channel.id;
                
                // Search through all loaded dialogs
                for (int i = 0; i < messagesController.dialogMessage.size(); i++) {
                    long dialogId = messagesController.dialogMessage.keyAt(i);
                    
                    // Check if this is our channel's dialog
                    if (dialogId == channelDialogId) {
                        java.util.ArrayList<MessageObject> messages = messagesController.dialogMessage.valueAt(i);
                        if (messages != null && !messages.isEmpty()) {
                            MessageObject message = messages.get(0);
                            if (message != null && message.messageOwner != null) {
                                return message;
                            }
                        }
                    }
                }
                
                // Also check if any message is from this channel
                for (int i = 0; i < messagesController.dialogMessage.size(); i++) {
                    java.util.ArrayList<MessageObject> messages = messagesController.dialogMessage.valueAt(i);
                    if (messages != null && !messages.isEmpty()) {
                        for (MessageObject message : messages) {
                            if (message != null && message.messageOwner != null && 
                                message.messageOwner.peer_id instanceof TLRPC.TL_peerChannel) {
                                long messageChannelId = ((TLRPC.TL_peerChannel) message.messageOwner.peer_id).channel_id;
                                if (messageChannelId == channel.id) {
                                    return message;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Return null on error
        }
        return null;
    }
    
    private void setupChannelPreview(MessageObject message, TLRPC.Chat channel) {
        if (messagePreviewComponent == null || message == null || channel == null) {
            return;
        }
        
        try {
            // Set channel name and avatar
            messagePreviewComponent.setChannelName(channel.title);
            messagePreviewComponent.setChannelType("personal channel");
            
            // Load channel avatar (including fallback drawable if no photo)
            messagePreviewComponent.setChannelAvatar(channel, parentFragment != null ? parentFragment.getCurrentAccount() : 0);
            
            // Don't show timestamp for channel messages as per requirement
            messagePreviewComponent.setMessageTime("");
            
            // Set message content based on type with proper preview
            if (message.isPhoto()) {
                // Check if photo has a caption
                if (message.messageOwner != null && message.messageOwner.message != null && !message.messageOwner.message.trim().isEmpty()) {
                    String caption = message.messageOwner.message.trim();
                    if (caption.length() > 80) {
                        caption = caption.substring(0, 80) + "...";
                    }
                    messagePreviewComponent.setMessageText(caption);
                } else {
                    messagePreviewComponent.setMessageText("📷 Photo");
                }
                messagePreviewComponent.setMessageThumbnail(message);
            } else if (message.isVideo()) {
                // Check if video has a caption
                if (message.messageOwner != null && message.messageOwner.message != null && !message.messageOwner.message.trim().isEmpty()) {
                    String caption = message.messageOwner.message.trim();
                    if (caption.length() > 80) {
                        caption = caption.substring(0, 80) + "...";
                    }
                    messagePreviewComponent.setMessageText(caption);
                } else {
                    String duration = message.getDuration() > 0 ? 
                        String.format("%d:%02d", message.getDuration() / 60, message.getDuration() % 60) : "";
                    messagePreviewComponent.setMessageText("🎥 Video" + (duration.isEmpty() ? "" : " " + duration));
                }
                messagePreviewComponent.setMessageThumbnail(message);
            } else if (message.isGif()) {
                // Check if GIF has a caption
                if (message.messageOwner != null && message.messageOwner.message != null && !message.messageOwner.message.trim().isEmpty()) {
                    String caption = message.messageOwner.message.trim();
                    if (caption.length() > 80) {
                        caption = caption.substring(0, 80) + "...";
                    }
                    messagePreviewComponent.setMessageText(caption);
                } else {
                    messagePreviewComponent.setMessageText("🎞 GIF");
                }
                messagePreviewComponent.setMessageThumbnail(message);
            } else if (message.messageOwner != null && message.messageOwner.message != null && !message.messageOwner.message.isEmpty()) {
                String messageText = message.messageOwner.message.trim();
                if (messageText.length() > 80) {
                    messageText = messageText.substring(0, 80) + "...";
                }
                messagePreviewComponent.setMessageText(messageText);
                messagePreviewComponent.setMessageThumbnail(null);
            } else if (message.isVoice()) {
                String duration = message.getDuration() > 0 ? 
                    String.format("%d:%02d", message.getDuration() / 60, message.getDuration() % 60) : "";
                messagePreviewComponent.setMessageText("🎤 Voice message" + (duration.isEmpty() ? "" : " " + duration));
                messagePreviewComponent.setMessageThumbnail(null);
            } else if (message.getDocument() != null) {
                String fileName = message.getDocumentName();
                if (fileName != null && fileName.length() > 30) {
                    fileName = fileName.substring(0, 30) + "...";
                }
                messagePreviewComponent.setMessageText("📄 " + (fileName != null ? fileName : "Document"));
                messagePreviewComponent.setMessageThumbnail(null);
            } else {
                messagePreviewComponent.setMessageText("View Channel");
                messagePreviewComponent.setMessageThumbnail(null);
            }
            
            // Set click listener to open channel
            messagePreviewComponent.setOnMessageClickListener(() -> {
                if (parentFragment != null) {
                    android.os.Bundle args = new android.os.Bundle();
                    args.putLong("chat_id", channel.id);
                    parentFragment.presentFragment(new org.telegram.ui.ChatActivity(args));
                }
            });
            
            messagePreviewComponent.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            messagePreviewComponent.setVisibility(View.GONE);
        }
    }
    
    private void setupTabs() {
        String[] tabs;
        
        if (isBot) {
            tabs = ProfileTabsComponent.getDefaultBotTabs();
        } else {
            tabs = ProfileTabsComponent.getDefaultUserTabs();
        }
        
        tabsComponent.setTabs(tabs);
        tabsComponent.setOnTabSelectedListener(this::onTabSelected);
        
        // Select "Media" tab by default for user profiles
        if (!isBot) {
            // Find "Media" tab index and select it
            for (int i = 0; i < tabs.length; i++) {
                if ("Media".equals(tabs[i])) {
                    tabsComponent.selectTab(i);
                    break;
                }
            }
        }
    }
    
    private void setupContent() {
        // Start with media content as default (matches default Media tab selection)
        java.util.List<ProfileContentComponent.ContentItem> mediaItems = createMediaContent();
        android.util.Log.d("UserProfileBuilder", "Created media content with " + (mediaItems != null ? mediaItems.size() : 0) + " items");
        contentComponent.setContentItems(mediaItems, ProfileContentComponent.ContentItem.TYPE_PHOTO);
        contentComponent.setOnContentItemClickListener(this::onContentItemClick);
    }
    
    private String getUserStatus() {
        if (user == null) {
            return "";
        }
        
        if (isBot) {
            return LocaleController.getString("Bot", R.string.Bot);
        }
        
        // Use simpler status to avoid potential hanging in LocaleController.formatUserStatus
        if (user.status != null) {
            if (user.status instanceof TLRPC.TL_userStatusOnline) {
                return "online";
            } else if (user.status instanceof TLRPC.TL_userStatusOffline) {
                return "last seen recently";
            } else if (user.status instanceof TLRPC.TL_userStatusRecently) {
                return "last seen recently";
            } else if (user.status instanceof TLRPC.TL_userStatusLastWeek) {
                return "last seen within a week";
            } else if (user.status instanceof TLRPC.TL_userStatusLastMonth) {
                return "last seen within a month";
            }
        }
        
        return "last seen a long time ago";
    }
    
    private void onHeaderActionClick(String action) {
        if (action == null) return;
        
        // Convert string action to integer constant for existing onActionClick method
        switch (action) {
            case "Message":
                onActionClick(ProfileActionsComponent.ActionButton.TYPE_MESSAGE);
                break;
            case "Call":
                onActionClick(ProfileActionsComponent.ActionButton.TYPE_CALL);
                break;
            case "Video":
                onActionClick(ProfileActionsComponent.ActionButton.TYPE_VIDEO);
                break;
            case "Mute":
                onActionClick(ProfileActionsComponent.ActionButton.TYPE_MUTE);
                break;
            default:
                // Unknown action, ignore
                break;
        }
    }
    
    private void onActionClick(int actionType) {
        if (user == null || parentFragment == null) {
            return;
        }
        
        try {
            switch (actionType) {
                case ProfileActionsComponent.ActionButton.TYPE_MESSAGE:
                    // Open chat with user
                    android.os.Bundle args = new android.os.Bundle();
                    args.putLong("user_id", user.id);
                    parentFragment.presentFragment(new org.telegram.ui.ChatActivity(args));
                    break;
                case ProfileActionsComponent.ActionButton.TYPE_CALL:
                    // Check for call restrictions first
                    if (userInfo != null && userInfo.phone_calls_private) {
                        // Show invite link dialog instead of calling
                        InviteLinkDialog.show(context, user, parentFragment.getResourceProvider(), 
                            () -> {
                                // Handle invite link sent
                                try {
                                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                                    intent.setType("text/plain");
                                    String username = org.telegram.messenger.UserObject.getPublicUsername(user);
                                    if (username != null) {
                                        intent.putExtra(android.content.Intent.EXTRA_TEXT, "https://t.me/" + username);
                                        context.startActivity(android.content.Intent.createChooser(intent, "Send Invite"));
                                    }
                                } catch (Exception e) {
                                    // Ignore share errors
                                }
                            });
                    } else {
                        // Normal voice call
                        org.telegram.ui.Components.voip.VoIPHelper.startCall(user, false, 
                            userInfo != null && userInfo.video_calls_available, parentFragment.getParentActivity(), 
                            userInfo, parentFragment.getAccountInstance());
                    }
                    break;
                case ProfileActionsComponent.ActionButton.TYPE_VIDEO:
                    // Check for video call restrictions first
                    if (userInfo != null && !userInfo.video_calls_available) {
                        // Show invite link dialog for video calls too
                        InviteLinkDialog.show(context, user, parentFragment.getResourceProvider(),
                            () -> {
                                // Handle invite link sent - same as voice call
                                try {
                                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                                    intent.setType("text/plain");
                                    String username = org.telegram.messenger.UserObject.getPublicUsername(user);
                                    if (username != null) {
                                        intent.putExtra(android.content.Intent.EXTRA_TEXT, "https://t.me/" + username);
                                        context.startActivity(android.content.Intent.createChooser(intent, "Send Invite"));
                                    }
                                } catch (Exception e) {
                                    // Ignore share errors
                                }
                            });
                    } else {
                        // Normal video call
                        org.telegram.ui.Components.voip.VoIPHelper.startCall(user, true, 
                            userInfo != null && userInfo.video_calls_available, parentFragment.getParentActivity(), 
                            userInfo, parentFragment.getAccountInstance());
                    }
                    break;
                case ProfileActionsComponent.ActionButton.TYPE_MUTE:
                    // Show mute options menu instead of immediate mute
                    View muteButton = actionsComponent.findViewById(android.R.id.button1); // Find mute button
                    if (muteButton != null) {
                        MuteOptionsDialog.show(context, muteButton, parentFragment.getResourceProvider(),
                            new MuteOptionsDialog.OnMuteOptionSelectedListener() {
                                @Override
                                public void onDisableSound() {
                                    // Disable sound only
                                    org.telegram.messenger.NotificationsController.getInstance(parentFragment.getCurrentAccount())
                                        .muteDialog(user.id, 0, true);
                                }
                                
                                @Override
                                public void onMuteFor() {
                                    // Show duration picker (simplified - just mute for 1 hour)
                                    org.telegram.messenger.NotificationsController.getInstance(parentFragment.getCurrentAccount())
                                        .muteDialog(user.id, 3600, true);
                                }
                                
                                @Override
                                public void onCustomize() {
                                    // Open notification settings
                                    android.os.Bundle args = new android.os.Bundle();
                                    args.putLong("user_id", user.id);
                                    // Would open NotificationsSettingsActivity
                                }
                                
                                @Override
                                public void onMuteForever() {
                                    // Mute forever
                                    org.telegram.messenger.NotificationsController.getInstance(parentFragment.getCurrentAccount())
                                        .muteDialog(user.id, Integer.MAX_VALUE, true);
                                }
                            });
                    }
                    break;
                case ProfileActionsComponent.ActionButton.TYPE_UNMUTE:
                    // Handle unmute action
                    org.telegram.messenger.NotificationsController.getInstance(parentFragment.getCurrentAccount()).muteDialog(user.id, 0, false);
                    break;
                case ProfileActionsComponent.ActionButton.TYPE_GIFT:
                    // Handle gift action - open gift selection
                    if (user != null && parentFragment != null) {
                        // Apply gift profile theme temporarily
                        themeManager.applyGradientForProfileType(headerComponent, ProfileThemeManager.ProfileType.GIFT);
                        
                        // In real implementation, would open gift selection activity
                        android.os.Bundle giftArgs = new android.os.Bundle();
                        giftArgs.putLong("user_id", user.id);
                        giftArgs.putBoolean("is_gift", true);
                        // parentFragment.presentFragment(new GiftSelectionActivity(giftArgs));
                    }
                    break;
                case ProfileActionsComponent.ActionButton.TYPE_SHARE:
                    // Handle share action
                    try {
                        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        String username = UserObject.getPublicUsername(user);
                        if (username != null) {
                            intent.putExtra(android.content.Intent.EXTRA_TEXT, "https://t.me/" + username);
                            context.startActivity(android.content.Intent.createChooser(intent, "Share"));
                        }
                    } catch (Exception e) {
                        // Ignore share errors
                    }
                    break;
                case ProfileActionsComponent.ActionButton.TYPE_BLOCK:
                    // Handle block action
                    parentFragment.getMessagesController().blockPeer(user.id);
                    break;
            }
        } catch (Exception e) {
            // Ignore action errors to prevent crashes
        }
    }
    
    private void onQrClick() {
        if (user == null || parentFragment == null) {
            return;
        }
        
        try {
            // Open QR code activity
            android.os.Bundle args = new android.os.Bundle();
            args.putLong("user_id", user.id);
            args.putBoolean("is_profile", true);
            
            parentFragment.presentFragment(new QrActivity(args));
        } catch (Exception e) {
            // Ignore QR errors
        }
    }
    
    private void onTabSelected(int position, String title) {
        // Handle tab selection and load content for selected tab
        try {
            switch (title) {
                case "Posts":
                    contentComponent.setContentItems(createPostsContent(), ProfileContentComponent.ContentItem.TYPE_PHOTO);
                    break;
                case "Media":
                    contentComponent.setContentItems(createMediaContent(), ProfileContentComponent.ContentItem.TYPE_PHOTO);
                    break;
                case "Files":
                    contentComponent.setContentItems(createFilesContent(), ProfileContentComponent.ContentItem.TYPE_FILE);
                    break;
                case "Links":
                    contentComponent.setContentItems(createLinksContent(), ProfileContentComponent.ContentItem.TYPE_LINK);
                    break;
                case "Voice":
                    contentComponent.setContentItems(createVoiceContent(), ProfileContentComponent.ContentItem.TYPE_VOICE);
                    break;
                case "Music":
                    contentComponent.setContentItems(createMusicContent(), ProfileContentComponent.ContentItem.TYPE_MUSIC);
                    break;
                case "Info":
                    contentComponent.setContentItems(createInfoContent(), ProfileContentComponent.ContentItem.TYPE_PHOTO);
                    break;
                default:
                    // Default to media content
                    contentComponent.setContentItems(createMediaContent(), ProfileContentComponent.ContentItem.TYPE_PHOTO);
                    break;
            }
        } catch (Exception e) {
            // Fallback to empty list if there's an error
            contentComponent.setContentItems(new java.util.ArrayList<>());
        }
    }
    
    private void onContentItemClick(ProfileContentComponent.ContentItem item, int position) {
        // Handle content item click
        if (item == null || parentFragment == null) {
            return;
        }
        
        try {
            if (item.messageObject != null) {
                // For real message objects, open the media viewer
                if (item.messageObject.isPhoto() || item.messageObject.isVideo() || item.messageObject.isGif()) {
                    // Open photo/video viewer with correct method signature
                    try {
                        java.util.ArrayList<MessageObject> messages = new java.util.ArrayList<>();
                        messages.add(item.messageObject);
                        org.telegram.ui.PhotoViewer.getInstance().openPhoto(messages, position, 
                            user.id, 0, 0, null);
                    } catch (Exception e) {
                        // Fallback to toast
                        android.widget.Toast.makeText(parentFragment.getParentActivity(), 
                            "Media viewer not available", android.widget.Toast.LENGTH_SHORT).show();
                    }
                } else if (item.messageObject.getDocument() != null) {
                    // Open document
                    try {
                        org.telegram.messenger.AndroidUtilities.openDocument(item.messageObject, 
                            parentFragment.getParentActivity(), parentFragment);
                    } catch (Exception e) {
                        // Fallback to toast
                        android.widget.Toast.makeText(parentFragment.getParentActivity(), 
                            "Document viewer not available", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (Exception e) {
            // Fallback for any errors
            android.widget.Toast.makeText(parentFragment.getParentActivity(), 
                "Media viewer not available", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    private void onMenuClick(View view) {
        // Always use our custom menu implementation for proper positioning and functionality
        showRealMenu(view);
    }
    
    private void onBackClick() {
        if (parentFragment != null) {
            // Delegate to parent fragment's back handling
            parentFragment.finishFragment();
        }
    }
    
    private void showRealMenu(View view) {
        // Create a popup menu with real ProfileActivity functionality
        if (parentFragment == null || user == null) return;
        
        try {
            // Create popup layout
            org.telegram.ui.ActionBar.ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout = 
                new org.telegram.ui.ActionBar.ActionBarPopupWindow.ActionBarPopupWindowLayout(
                    parentFragment.getParentActivity(), 
                    org.telegram.messenger.R.drawable.popup_fixed_alert, 
                    resourcesProvider);
            
            // Add menu items based on user relationship  
            boolean isContact = user.contact;
            boolean isBlocked = parentFragment.getMessagesController().blockePeers.indexOfKey(user.id) >= 0;
            boolean isSelf = parentFragment.getUserConfig().getCurrentUser().id == user.id;
            
            if (!isSelf) {
                // Search in conversation
                org.telegram.ui.ActionBar.ActionBarMenuItem.addItem(popupLayout, 
                    org.telegram.messenger.R.drawable.msg_search, 
                    org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.Search), 
                    false, resourcesProvider)
                    .setOnClickListener(v -> {
                        // Open search in chat
                        android.os.Bundle args = new android.os.Bundle();
                        args.putLong("user_id", user.id);
                        args.putBoolean("search", true);
                        parentFragment.presentFragment(new org.telegram.ui.ChatActivity(args));
                    });
                
                // Notifications  
                org.telegram.ui.ActionBar.ActionBarMenuItem.addItem(popupLayout, 
                    org.telegram.messenger.R.drawable.msg_mute, 
                    org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.Notifications), 
                    false, resourcesProvider)
                    .setOnClickListener(v -> onActionClick(ProfileActionsComponent.ActionButton.TYPE_MUTE));
                
                if (!isContact) {
                    // Add Contact
                    org.telegram.ui.ActionBar.ActionBarMenuItem.addItem(popupLayout, 
                        org.telegram.messenger.R.drawable.msg_addcontact, 
                        org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.AddContact), 
                        false, resourcesProvider)
                        .setOnClickListener(v -> {
                            org.telegram.messenger.ContactsController.getInstance(parentFragment.getCurrentAccount())
                                .addContact(user, true);
                        });
                } else {
                    // Edit Contact  
                    org.telegram.ui.ActionBar.ActionBarMenuItem.addItem(popupLayout, 
                        org.telegram.messenger.R.drawable.msg_edit, 
                        org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.EditContact), 
                        false, resourcesProvider)
                        .setOnClickListener(v -> {
                            // Open contact editor
                            android.os.Bundle args = new android.os.Bundle();
                            args.putLong("user_id", user.id);
                            args.putBoolean("addContact", false);
                            parentFragment.presentFragment(new org.telegram.ui.ContactAddActivity(args));
                        });
                    
                    // Delete Contact
                    org.telegram.ui.ActionBar.ActionBarMenuItem.addItem(popupLayout, 
                        org.telegram.messenger.R.drawable.msg_delete, 
                        org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.DeleteContact), 
                        false, resourcesProvider)
                        .setOnClickListener(v -> {
                            // Show confirmation dialog
                            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(parentFragment.getParentActivity());
                            builder.setTitle(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.DeleteContact));
                            builder.setMessage(org.telegram.messenger.LocaleController.formatString(org.telegram.messenger.R.string.AreYouSureDeleteContact, org.telegram.messenger.UserObject.getUserName(user)));
                            builder.setPositiveButton(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.Delete), (dialog, which) -> {
                                java.util.ArrayList<TLRPC.User> users = new java.util.ArrayList<>();
                                users.add(user);
                                org.telegram.messenger.ContactsController.getInstance(parentFragment.getCurrentAccount()).deleteContact(users, true);
                            });
                            builder.setNegativeButton(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.Cancel), null);
                            builder.show();
                        });
                }
                
                // Share Contact
                org.telegram.ui.ActionBar.ActionBarMenuItem.addItem(popupLayout, 
                    org.telegram.messenger.R.drawable.msg_share, 
                    org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.ShareContact), 
                    false, resourcesProvider)
                    .setOnClickListener(v -> onActionClick(ProfileActionsComponent.ActionButton.TYPE_SHARE));
                
                // Block/Unblock User
                int blockStringRes = isBlocked ? org.telegram.messenger.R.string.Unblock : org.telegram.messenger.R.string.BlockContact;
                int blockIconRes = isBlocked ? org.telegram.messenger.R.drawable.msg_block : org.telegram.messenger.R.drawable.msg_block;
                
                org.telegram.ui.ActionBar.ActionBarMenuItem.addItem(popupLayout, 
                    blockIconRes, 
                    org.telegram.messenger.LocaleController.getString(blockStringRes), 
                    false, resourcesProvider)
                    .setOnClickListener(v -> {
                        if (isBlocked) {
                            parentFragment.getMessagesController().unblockPeer(user.id);
                        } else {
                            // Show confirmation dialog for blocking
                            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(parentFragment.getParentActivity());
                            builder.setTitle(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.BlockUser));
                            builder.setMessage(org.telegram.messenger.LocaleController.formatString(org.telegram.messenger.R.string.AreYouSureBlockContact2, org.telegram.messenger.UserObject.getUserName(user)));
                            builder.setPositiveButton(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.BlockContact), (dialog, which) -> {
                                parentFragment.getMessagesController().blockPeer(user.id);
                            });
                            builder.setNegativeButton(org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.Cancel), null);
                            builder.show();
                        }
                    });
            } else {
                // For self profile
                org.telegram.ui.ActionBar.ActionBarMenuItem.addItem(popupLayout, 
                    org.telegram.messenger.R.drawable.msg_qrcode, 
                    "QR Code", 
                    false, resourcesProvider)
                    .setOnClickListener(v -> onQrClick());
                
                org.telegram.ui.ActionBar.ActionBarMenuItem.addItem(popupLayout, 
                    org.telegram.messenger.R.drawable.msg_share, 
                    org.telegram.messenger.LocaleController.getString(org.telegram.messenger.R.string.ShareContact), 
                    false, resourcesProvider)
                    .setOnClickListener(v -> onActionClick(ProfileActionsComponent.ActionButton.TYPE_SHARE));
            }
            
            // Create and show popup window
            org.telegram.ui.ActionBar.ActionBarPopupWindow popupWindow = 
                new org.telegram.ui.ActionBar.ActionBarPopupWindow(popupLayout, 
                    org.telegram.ui.Components.LayoutHelper.WRAP_CONTENT, 
                    org.telegram.ui.Components.LayoutHelper.WRAP_CONTENT);
            
            popupWindow.setPauseNotifications(true);
            popupWindow.setDismissAnimationDuration(220);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setClippingEnabled(true);
            popupWindow.setAnimationStyle(org.telegram.messenger.R.style.PopupContextAnimation);
            popupWindow.setFocusable(true);
            
            // Measure the popup layout first
            popupLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), 
                               View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            
            // Calculate position relative to the menu button
            int[] location = new int[2];
            view.getLocationInWindow(location);
            
            // Position the menu properly below the button with better spacing
            int x = location[0] + view.getWidth() - popupLayout.getMeasuredWidth();
            int y = location[1] + view.getHeight() + AndroidUtilities.dp(8);
            
            // Ensure menu stays on screen horizontally
            if (x < AndroidUtilities.dp(16)) {
                x = AndroidUtilities.dp(16);
            }
            if (x + popupLayout.getMeasuredWidth() > AndroidUtilities.displaySize.x - AndroidUtilities.dp(16)) {
                x = AndroidUtilities.displaySize.x - popupLayout.getMeasuredWidth() - AndroidUtilities.dp(16);
            }
            
            // Ensure menu stays on screen vertically
            if (y + popupLayout.getMeasuredHeight() > AndroidUtilities.displaySize.y - AndroidUtilities.dp(16)) {
                y = location[1] - popupLayout.getMeasuredHeight() - AndroidUtilities.dp(8);
            }
            
            popupWindow.showAtLocation(view, android.view.Gravity.NO_GRAVITY, x, y);
            
        } catch (Exception e) {
            // Final fallback to toast
            android.widget.Toast.makeText(parentFragment.getParentActivity(), 
                "Menu not available", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    public ProfileHeaderComponent getHeaderComponent() {
        return headerComponent;
    }
    
    public ProfileInfoComponent getInfoComponent() {
        return infoComponent;
    }
    
    public ProfileActionsComponent getActionsComponent() {
        return actionsComponent;
    }
    
    public ProfileTabsComponent getTabsComponent() {
        return tabsComponent;
    }
    
    public ProfileMessagePreviewComponent getMessagePreviewComponent() {
        return messagePreviewComponent;
    }
    
    public ProfileContentComponent getContentComponent() {
        return contentComponent;
    }
    
    public void updateTheme() {
        themeManager.updateTheme(headerComponent, actionsComponent, infoComponent, tabsComponent, contentComponent);
        if (messagePreviewComponent != null) {
            messagePreviewComponent.updateTheme();
        }
        themeManager.applyGradientForProfileType(headerComponent, ProfileThemeManager.ProfileType.USER);
    }
    
    // Content creation methods for different tabs - load real data from SharedMediaLayout
    private java.util.List<ProfileContentComponent.ContentItem> createPostsContent() {
        // For Posts tab, show all types of content mixed together
        java.util.List<ProfileContentComponent.ContentItem> allItems = new java.util.ArrayList<>();
        
        try {
            // Load media items
            allItems.addAll(loadSharedMedia(SharedMediaLayout.TAB_PHOTOVIDEO));
            // Load files
            allItems.addAll(loadSharedMedia(SharedMediaLayout.TAB_FILES));
            // Load links  
            allItems.addAll(loadSharedMedia(SharedMediaLayout.TAB_LINKS));
            // Load voice messages
            allItems.addAll(loadSharedMedia(SharedMediaLayout.TAB_VOICE));
            // Load music
            allItems.addAll(loadSharedMedia(SharedMediaLayout.TAB_AUDIO));
            
            // Sort by most recent first (if possible)
            // For now just return the combined list
            
        } catch (Exception e) {
            // Return empty list on error
        }
        
        return allItems;
    }
    
    private java.util.List<ProfileContentComponent.ContentItem> createMediaContent() {
        // Load real photos and videos from conversation
        return loadSharedMedia(SharedMediaLayout.TAB_PHOTOVIDEO);
    }
    
    private java.util.List<ProfileContentComponent.ContentItem> createFilesContent() {
        // Load real files from conversation
        return loadSharedMedia(SharedMediaLayout.TAB_FILES);
    }
    
    private java.util.List<ProfileContentComponent.ContentItem> createLinksContent() {
        // Load real links from conversation
        return loadSharedMedia(SharedMediaLayout.TAB_LINKS);
    }
    
    private java.util.List<ProfileContentComponent.ContentItem> createVoiceContent() {
        // Load real voice messages from conversation
        return loadSharedMedia(SharedMediaLayout.TAB_VOICE);
    }
    
    private java.util.List<ProfileContentComponent.ContentItem> createMusicContent() {
        // Load real music from conversation
        return loadSharedMedia(SharedMediaLayout.TAB_AUDIO);
    }
    
    private java.util.List<ProfileContentComponent.ContentItem> loadSharedMedia(int mediaType) {
        java.util.List<ProfileContentComponent.ContentItem> items = new java.util.ArrayList<>();
        
        if (user == null || parentFragment == null) {
            return items;
        }
        
        try {
            // Priority 1: Use SharedMediaPreloader if available (this is the primary data source)
            if (sharedMediaPreloader != null) {
                try {
                    SharedMediaLayout.SharedMediaData[] sharedMediaData = sharedMediaPreloader.getSharedMediaData();
                    
                    if (sharedMediaData != null && mediaType < sharedMediaData.length) {
                        SharedMediaLayout.SharedMediaData data = sharedMediaData[mediaType];
                        
                        if (data != null && data.messages != null && !data.messages.isEmpty()) {
                            // Convert ALL loaded messages for this media type
                            for (MessageObject messageObject : data.messages) {
                                if (messageObject == null) continue;
                                
                                ProfileContentComponent.ContentItem item = convertMessageToContentItem(messageObject, mediaType);
                                if (item != null) {
                                    items.add(item);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Continue with fallback if SharedMediaPreloader fails
                }
            }
            
            // Priority 2: If SharedMediaPreloader didn't have data, search all loaded dialogs
            if (items.isEmpty()) {
                MessagesController messagesController = MessagesController.getInstance(parentFragment.getCurrentAccount());
                long dialogId = user.id;
                
                // Check direct dialog with user first
                java.util.ArrayList<MessageObject> directMessages = messagesController.dialogMessage.get(dialogId);
                if (directMessages != null && !directMessages.isEmpty()) {
                    for (MessageObject messageObject : directMessages) {
                        if (messageObject != null && checkMessageMatchesType(messageObject, mediaType)) {
                            ProfileContentComponent.ContentItem item = convertMessageToContentItem(messageObject, mediaType);
                            if (item != null) {
                                items.add(item);
                            }
                        }
                    }
                }
                
                // If still no items, search all dialogs
                if (items.isEmpty()) {
                    for (int i = 0; i < messagesController.dialogMessage.size() && items.size() < 20; i++) {
                        long currentDialogId = messagesController.dialogMessage.keyAt(i);
                        java.util.ArrayList<MessageObject> messages = messagesController.dialogMessage.valueAt(i);
                        
                        if (messages != null && !messages.isEmpty()) {
                            for (MessageObject messageObject : messages) {
                                if (messageObject != null && messageObject.messageOwner != null) {
                                    // Check if message is from our user OR in dialog with our user
                                    boolean isRelevant = false;
                                    
                                    // Check if from our user
                                    if (messageObject.messageOwner.from_id instanceof TLRPC.TL_peerUser) {
                                        isRelevant = ((TLRPC.TL_peerUser) messageObject.messageOwner.from_id).user_id == user.id;
                                    }
                                    
                                    // Or if this is a dialog with our user
                                    if (!isRelevant && currentDialogId == dialogId) {
                                        isRelevant = true;
                                    }
                                    
                                    if (isRelevant && checkMessageMatchesType(messageObject, mediaType)) {
                                        ProfileContentComponent.ContentItem item = convertMessageToContentItem(messageObject, mediaType);
                                        if (item != null) {
                                            items.add(item);
                                            if (items.size() >= 20) break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            // Return empty list on error - proper empty state will be shown
        }
        
        return items;
    }
    
    private java.util.List<ProfileContentComponent.ContentItem> loadMediaFromDataController(int mediaType) {
        java.util.List<ProfileContentComponent.ContentItem> items = new java.util.ArrayList<>();
        
        try {
            if (parentFragment == null || user == null) return items;
            
            MediaDataController mediaDataController = MediaDataController.getInstance(parentFragment.getCurrentAccount());
            long dialogId = user.id;
            
            // Try to get recent messages with media for this dialog
            MessagesController messagesController = MessagesController.getInstance(parentFragment.getCurrentAccount());
            
            // Load recent messages from this dialog
            messagesController.loadPeerSettings(user, null);
            
            // Load messages more comprehensively - try multiple approaches
            
            // Check dialog message (array of messages per dialog)
            java.util.ArrayList<MessageObject> directMessages = messagesController.dialogMessage.get(dialogId);
            if (directMessages != null && !directMessages.isEmpty()) {
                MessageObject directMessage = directMessages.get(0);
                if (directMessage != null && directMessage.messageOwner != null) {
                    boolean matches = checkMessageMatchesType(directMessage, mediaType);
                    if (matches) {
                        ProfileContentComponent.ContentItem item = convertMessageToContentItem(directMessage, mediaType);
                        if (item != null) {
                            items.add(item);
                        }
                    }
                }
            }
            
            // Search through all dialogs to find more messages from this user
            for (int i = 0; i < messagesController.dialogMessage.size() && items.size() < 20; i++) {
                long currentDialogId = messagesController.dialogMessage.keyAt(i);
                java.util.ArrayList<MessageObject> messages = messagesController.dialogMessage.valueAt(i);
                
                if (messages != null && !messages.isEmpty()) {
                    MessageObject message = messages.get(0);
                    if (message != null && message.messageOwner != null) {
                        // Check if message is from our user or in dialog with our user
                        boolean isRelevant = false;
                        
                        // Check if from our user
                        if (message.messageOwner.from_id instanceof TLRPC.TL_peerUser) {
                            isRelevant = ((TLRPC.TL_peerUser) message.messageOwner.from_id).user_id == user.id;
                        }
                        
                        // Or if this is a dialog with our user
                        if (!isRelevant && currentDialogId == dialogId) {
                            isRelevant = true;
                        }
                        
                        if (isRelevant && checkMessageMatchesType(message, mediaType)) {
                            ProfileContentComponent.ContentItem item = convertMessageToContentItem(message, mediaType);
                            if (item != null) {
                                items.add(item);
                            }
                        }
                    }
                }
            }
            
            // For additional media loading, we would need to implement proper message loading
            // This is simplified for the profile preview - in full implementation would use
            // proper SharedMediaLayout integration with the ProfileActivity
            
        } catch (Exception e) {
            // Return empty list on error
        }
        
        return items;
    }
    
    private boolean checkMessageMatchesType(MessageObject message, int mediaType) {
        if (message == null || message.messageOwner == null) return false;
        
        switch (mediaType) {
            case SharedMediaLayout.TAB_PHOTOVIDEO:
                return message.isPhoto() || message.isVideo() || message.isGif();
                
            case SharedMediaLayout.TAB_FILES:
                // Check for documents that aren't videos, GIFs, voice messages, or music
                if (message.getDocument() != null) {
                    return !message.isVideo() && !message.isGif() && !message.isVoice() && !message.isMusic();
                }
                // Also check for other file types
                return message.messageOwner.media instanceof TLRPC.TL_messageMediaDocument && 
                       !message.isVideo() && !message.isGif() && !message.isVoice() && !message.isMusic();
                       
            case SharedMediaLayout.TAB_VOICE:
                // Check for voice messages
                return message.isVoice() || (message.getDocument() != null && message.isVoice());
                
            case SharedMediaLayout.TAB_AUDIO:
                // Check for music/audio files
                return message.isMusic() || (message.getDocument() != null && message.isMusic());
                
            case SharedMediaLayout.TAB_LINKS:
                // Check for web page links and URL entities
                if (message.messageOwner.media instanceof TLRPC.TL_messageMediaWebPage) {
                    return true;
                }
                // Also check message text for URLs
                if (message.messageOwner.message != null && message.messageOwner.message.contains("http")) {
                    return true;
                }
                // Check entities for URLs
                if (message.messageOwner.entities != null) {
                    for (TLRPC.MessageEntity entity : message.messageOwner.entities) {
                        if (entity instanceof TLRPC.TL_messageEntityUrl || 
                            entity instanceof TLRPC.TL_messageEntityTextUrl) {
                            return true;
                        }
                    }
                }
                return false;
                
            default:
                return false;
        }
    }
    
    private java.util.List<ProfileContentComponent.ContentItem> loadMediaFromAllSources(int mediaType) {
        java.util.List<ProfileContentComponent.ContentItem> items = new java.util.ArrayList<>();
        
        try {
            if (parentFragment == null || user == null) return items;
            
            MessagesController messagesController = MessagesController.getInstance(parentFragment.getCurrentAccount());
            long dialogId = user.id;
            
            // Try to search through all loaded dialogs for media from this user
            for (int i = 0; i < messagesController.dialogMessage.size(); i++) {
                long currentDialogId = messagesController.dialogMessage.keyAt(i);
                java.util.ArrayList<MessageObject> messages = messagesController.dialogMessage.valueAt(i);
                
                if (messages != null && !messages.isEmpty()) {
                    MessageObject message = messages.get(0);
                    if (message != null && message.messageOwner != null) {
                        // Check if this message involves our user
                        boolean isFromUser = false;
                        
                        if (message.messageOwner.from_id != null) {
                            if (message.messageOwner.from_id instanceof TLRPC.TL_peerUser) {
                                isFromUser = ((TLRPC.TL_peerUser) message.messageOwner.from_id).user_id == user.id;
                            }
                        }
                        
                        // Also check if this is a dialog with our user
                        if (!isFromUser && currentDialogId == dialogId) {
                            isFromUser = true;
                        }
                        
                        if (isFromUser) {
                            // Filter by media type
                            boolean matches = false;
                            switch (mediaType) {
                                case SharedMediaLayout.TAB_PHOTOVIDEO:
                                    matches = message.isPhoto() || message.isVideo() || message.isGif();
                                    break;
                                case SharedMediaLayout.TAB_FILES:
                                    matches = message.getDocument() != null && !message.isVideo() && !message.isGif() && !message.isVoice() && !message.isMusic();
                                    break;
                                case SharedMediaLayout.TAB_VOICE:
                                    matches = message.isVoice();
                                    break;
                                case SharedMediaLayout.TAB_AUDIO:
                                    matches = message.isMusic();
                                    break;
                                case SharedMediaLayout.TAB_LINKS:
                                    matches = message.messageOwner.media instanceof TLRPC.TL_messageMediaWebPage;
                                    break;
                            }
                            
                            if (matches) {
                                ProfileContentComponent.ContentItem item = convertMessageToContentItem(message, mediaType);
                                if (item != null) {
                                    items.add(item);
                                    if (items.size() >= 15) break; // Limit for performance
                                }
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            // Return empty list on error
        }
        
        return items;
    }
    
    private java.util.List<ProfileContentComponent.ContentItem> createSampleItemsForType(int mediaType) {
        java.util.List<ProfileContentComponent.ContentItem> items = new java.util.ArrayList<>();
        
        // Create sample items based on type to demonstrate the UI
        int itemCount = 6; // Show 6 sample items
        
        for (int i = 0; i < itemCount; i++) {
            ProfileContentComponent.ContentItem item = null;
            
            switch (mediaType) {
                case SharedMediaLayout.TAB_PHOTOVIDEO:
                    item = new ProfileContentComponent.ContentItem(
                        i % 2 == 0 ? ProfileContentComponent.ContentItem.TYPE_PHOTO : ProfileContentComponent.ContentItem.TYPE_VIDEO);
                    item.title = i % 2 == 0 ? "Photo" : "Video";
                    if (i % 2 == 1) {
                        item.duration = String.format("0:%02d", 15 + i * 5);
                    }
                    break;
                    
                case SharedMediaLayout.TAB_FILES:
                    item = new ProfileContentComponent.ContentItem(ProfileContentComponent.ContentItem.TYPE_FILE);
                    item.title = "Document_" + (i + 1) + ".pdf";
                    item.subtitle = String.format("%.1f MB", 1.2 + i * 0.5);
                    break;
                    
                case SharedMediaLayout.TAB_LINKS:
                    item = new ProfileContentComponent.ContentItem(ProfileContentComponent.ContentItem.TYPE_LINK);
                    String[] domains = {"github.com", "stackoverflow.com", "medium.com", "twitter.com", "youtube.com", "telegram.org"};
                    item.title = "Link to " + domains[i % domains.length];
                    item.subtitle = "https://" + domains[i % domains.length];
                    break;
                    
                case SharedMediaLayout.TAB_VOICE:
                    item = new ProfileContentComponent.ContentItem(ProfileContentComponent.ContentItem.TYPE_VOICE);
                    item.title = "Voice message";
                    item.duration = String.format("0:%02d", 10 + i * 3);
                    break;
                    
                case SharedMediaLayout.TAB_AUDIO:
                    item = new ProfileContentComponent.ContentItem(ProfileContentComponent.ContentItem.TYPE_MUSIC);
                    String[] songs = {"Song One", "Track Two", "Music Three", "Audio Four", "Sound Five", "Tune Six"};
                    item.title = songs[i % songs.length];
                    item.subtitle = "Artist Name";
                    item.duration = String.format("%d:%02d", 3 + i / 2, 30 + i * 7);
                    break;
            }
            
            if (item != null) {
                items.add(item);
            }
        }
        
        return items;
    }
    
    
    private ProfileContentComponent.ContentItem convertMessageToContentItem(MessageObject messageObject, int mediaType) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return null;
        }
        
        ProfileContentComponent.ContentItem item = null;
        
        try {
            switch (mediaType) {
                case SharedMediaLayout.TAB_PHOTOVIDEO:
                    if (messageObject.isPhoto()) {
                        item = new ProfileContentComponent.ContentItem(ProfileContentComponent.ContentItem.TYPE_PHOTO);
                    } else if (messageObject.isVideo()) {
                        item = new ProfileContentComponent.ContentItem(ProfileContentComponent.ContentItem.TYPE_VIDEO);
                        if (messageObject.getDuration() > 0) {
                            int duration = (int) messageObject.getDuration();
                            int minutes = duration / 60;
                            int seconds = duration % 60;
                            item.duration = String.format("%d:%02d", minutes, seconds);
                        }
                    } else if (messageObject.isGif()) {
                        item = new ProfileContentComponent.ContentItem(ProfileContentComponent.ContentItem.TYPE_GIF);
                    }
                    break;
                    
                case SharedMediaLayout.TAB_FILES:
                    if (messageObject.getDocument() != null && !messageObject.isVideo() && !messageObject.isGif() && !messageObject.isVoice() && !messageObject.isMusic()) {
                        item = new ProfileContentComponent.ContentItem(ProfileContentComponent.ContentItem.TYPE_FILE);
                        item.title = messageObject.getDocumentName();
                        if (messageObject.getDocument().size > 0) {
                            item.subtitle = org.telegram.messenger.AndroidUtilities.formatFileSize(messageObject.getDocument().size);
                        }
                    }
                    break;
                    
                case SharedMediaLayout.TAB_LINKS:
                    if (messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaWebPage) {
                        item = new ProfileContentComponent.ContentItem(ProfileContentComponent.ContentItem.TYPE_LINK);
                        TLRPC.WebPage webPage = ((TLRPC.TL_messageMediaWebPage) messageObject.messageOwner.media).webpage;
                        if (webPage != null) {
                            item.title = webPage.title != null ? webPage.title : "Link";
                            item.subtitle = webPage.url != null ? webPage.url : webPage.display_url;
                        }
                    } else if (messageObject.messageOwner.message != null && messageObject.messageOwner.message.contains("http")) {
                        item = new ProfileContentComponent.ContentItem(ProfileContentComponent.ContentItem.TYPE_LINK);
                        item.title = "Link";
                        item.subtitle = messageObject.messageOwner.message;
                    }
                    break;
                    
                case SharedMediaLayout.TAB_VOICE:
                    if (messageObject.isVoice()) {
                        item = new ProfileContentComponent.ContentItem(ProfileContentComponent.ContentItem.TYPE_VOICE);
                        item.title = "Voice message";
                        if (messageObject.getDuration() > 0) {
                            int duration = (int) messageObject.getDuration();
                            int minutes = duration / 60;
                            int seconds = duration % 60;
                            item.duration = String.format("%d:%02d", minutes, seconds);
                        }
                    }
                    break;
                    
                case SharedMediaLayout.TAB_AUDIO:
                    if (messageObject.isMusic()) {
                        item = new ProfileContentComponent.ContentItem(ProfileContentComponent.ContentItem.TYPE_MUSIC);
                        item.title = messageObject.getMusicTitle();
                        item.subtitle = messageObject.getMusicAuthor();
                        if (messageObject.getDuration() > 0) {
                            int duration = (int) messageObject.getDuration();
                            int minutes = duration / 60;
                            int seconds = duration % 60;
                            item.duration = String.format("%d:%02d", minutes, seconds);
                        }
                    }
                    break;
            }
            
            if (item != null) {
                item.messageObject = messageObject;
            }
        } catch (Exception e) {
            // Return null on conversion error
            return null;
        }
        
        return item;
    }
    
    private java.util.List<ProfileContentComponent.ContentItem> loadMediaFromMessagesController(int mediaType) {
        java.util.List<ProfileContentComponent.ContentItem> items = new java.util.ArrayList<>();
        
        try {
            if (parentFragment != null && user != null) {
                MessagesController messagesController = MessagesController.getInstance(parentFragment.getCurrentAccount());
                
                // Try to get dialog messages
                long dialogId = user.id;
                java.util.ArrayList<MessageObject> dialogMessages = messagesController.dialogMessage.get(dialogId);
                
                if (dialogMessages != null && !dialogMessages.isEmpty()) {
                    for (MessageObject messageObject : dialogMessages) {
                        if (messageObject == null) continue;
                        
                        // Filter by media type
                        boolean matches = false;
                        switch (mediaType) {
                            case SharedMediaLayout.TAB_PHOTOVIDEO:
                                matches = messageObject.isPhoto() || messageObject.isVideo() || messageObject.isGif();
                                break;
                            case SharedMediaLayout.TAB_FILES:
                                matches = messageObject.getDocument() != null && !messageObject.isVideo() && !messageObject.isGif() && !messageObject.isVoice() && !messageObject.isMusic();
                                break;
                            case SharedMediaLayout.TAB_VOICE:
                                matches = messageObject.isVoice();
                                break;
                            case SharedMediaLayout.TAB_AUDIO:
                                matches = messageObject.isMusic();
                                break;
                            case SharedMediaLayout.TAB_LINKS:
                                matches = messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaWebPage;
                                break;
                        }
                        
                        if (matches) {
                            ProfileContentComponent.ContentItem item = convertMessageToContentItem(messageObject, mediaType);
                            if (item != null) {
                                items.add(item);
                            }
                            
                            // Limit results
                            if (items.size() >= 6) break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }
        
        return items;
    }
    
    private java.util.List<ProfileContentComponent.ContentItem> loadMediaFromConversationHistory(int mediaType) {
        java.util.List<ProfileContentComponent.ContentItem> items = new java.util.ArrayList<>();
        
        try {
            if (parentFragment != null && user != null) {
                MessagesController messagesController = MessagesController.getInstance(parentFragment.getCurrentAccount());
                MediaDataController mediaDataController = MediaDataController.getInstance(parentFragment.getCurrentAccount());
                
                long dialogId = user.id;
                
                // Load recent messages that might contain media
                try {
                    messagesController.loadPeerSettings(user, null);
                } catch (Exception e) {
                    // Continue with fallback methods
                }
                
                // Check all loaded messages for this dialog
                java.util.ArrayList<MessageObject> dialogMessages = messagesController.dialogMessage.get(dialogId);
                if (dialogMessages != null) {
                    for (MessageObject messageObject : dialogMessages) {
                        if (messageObject != null && checkMessageMatchesType(messageObject, mediaType)) {
                            ProfileContentComponent.ContentItem item = convertMessageToContentItem(messageObject, mediaType);
                            if (item != null) {
                                items.add(item);
                                if (items.size() >= 15) break;
                            }
                        }
                    }
                }
                
                // Also check all dialogs for messages from this user
                for (int i = 0; i < messagesController.dialogMessage.size() && items.size() < 15; i++) {
                    long currentDialogId = messagesController.dialogMessage.keyAt(i);
                    java.util.ArrayList<MessageObject> messages = messagesController.dialogMessage.valueAt(i);
                    
                    if (messages != null && !messages.isEmpty()) {
                        for (MessageObject messageObject : messages) {
                            if (messageObject != null && messageObject.messageOwner != null) {
                                // Check if message is from our user
                                boolean isFromUser = false;
                                if (messageObject.messageOwner.from_id instanceof TLRPC.TL_peerUser) {
                                    isFromUser = ((TLRPC.TL_peerUser) messageObject.messageOwner.from_id).user_id == user.id;
                                }
                                
                                if (isFromUser && checkMessageMatchesType(messageObject, mediaType)) {
                                    ProfileContentComponent.ContentItem item = convertMessageToContentItem(messageObject, mediaType);
                                    if (item != null) {
                                        items.add(item);
                                        if (items.size() >= 15) break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Return empty list on error
        }
        
        return items;
    }
    
    private java.util.List<ProfileContentComponent.ContentItem> createInfoContent() {
        // Empty list for bot info tab - different components would be shown
        return new java.util.ArrayList<>();
    }
    
    private MessageObject searchAllLoadedMessagesForChannel(TLRPC.Chat channel) {
        if (channel == null || parentFragment == null) {
            return null;
        }
        
        try {
            MessagesController messagesController = MessagesController.getInstance(parentFragment.getCurrentAccount());
            long channelDialogId = -channel.id;
            
            // Search through ALL loaded dialogs for ANY message from this channel
            for (int i = 0; i < messagesController.dialogMessage.size(); i++) {
                long dialogId = messagesController.dialogMessage.keyAt(i);
                java.util.ArrayList<MessageObject> messages = messagesController.dialogMessage.valueAt(i);
                
                if (messages != null && !messages.isEmpty()) {
                    for (MessageObject message : messages) {
                        if (message != null && message.getDialogId() == channelDialogId) {
                            return message;
                        }
                        
                        // Also check if message is FROM this channel in a forwarded message
                        if (message != null && message.messageOwner != null) {
                            if (message.messageOwner.fwd_from != null && 
                                message.messageOwner.fwd_from.from_id instanceof TLRPC.TL_peerChannel) {
                                long fwdChannelId = ((TLRPC.TL_peerChannel) message.messageOwner.fwd_from.from_id).channel_id;
                                if (fwdChannelId == channel.id) {
                                    return message;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Return null on error
        }
        
        return null;
    }
    
    private void showChannelFallback(TLRPC.Chat channel) {
        if (messagePreviewComponent == null || channel == null) {
            return;
        }
        
        try {
            // Show basic channel info when no messages are loaded yet
            messagePreviewComponent.setMessageText("View Channel");
            messagePreviewComponent.setMessageTime("Channel");
            messagePreviewComponent.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            // Ignore errors
        }
    }
    
    private void loadRecentChannelMessages(TLRPC.Chat channel) {
        if (channel == null || parentFragment == null || messagePreviewComponent == null) {
            return;
        }
        
        try {
            // Try to load messages asynchronously
            MessagesController messagesController = MessagesController.getInstance(parentFragment.getCurrentAccount());
            long channelDialogId = -channel.id;
            
            // Request messages for this channel
            try {
                // Check if we can find messages in other ways
                TLRPC.Dialog dialog = messagesController.dialogs_dict.get(channelDialogId);
                if (dialog != null && dialog.top_message > 0) {
                    // Try to get the message by ID
                    MessageObject messageObject = messagesController.dialogMessage.get(channelDialogId) != null && 
                        !messagesController.dialogMessage.get(channelDialogId).isEmpty() ? 
                        messagesController.dialogMessage.get(channelDialogId).get(0) : null;
                    
                    if (messageObject != null) {
                        setupChannelPreview(messageObject, channel);
                        return;
                    }
                }
                
                // Hide channel preview if no messages found
                showChannelFallback(channel);
                
            } catch (Exception e) {
                // Show fallback content on error
                showChannelFallback(channel);
            }
        } catch (Exception e) {
            // Show fallback content on any error
            showChannelFallback(channel);
        }
    }
}
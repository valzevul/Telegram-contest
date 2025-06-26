package org.telegram.ui.Components.ProfileTypeBuilders;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ProfileActionsComponent;
import org.telegram.ui.Components.ProfileBioUsernameComponent;
import org.telegram.ui.Components.ProfileContentComponent;
import org.telegram.ui.Components.ProfileHeaderComponent;
import org.telegram.ui.Components.ProfileInfoComponent;
import org.telegram.ui.Components.ProfileTabsComponent;
import org.telegram.ui.Components.ProfileThemeManager;
import org.telegram.ui.Components.SharedMediaLayout;
import org.telegram.messenger.MediaDataController;

public class ChannelProfileBuilder {
    private Context context;
    private Theme.ResourcesProvider resourcesProvider;
    private ProfileThemeManager themeManager;
    
    private ProfileHeaderComponent headerComponent;
    private ProfileInfoComponent infoComponent;
    private ProfileBioUsernameComponent bioUsernameComponent;
    private ProfileActionsComponent actionsComponent;
    private ProfileTabsComponent tabsComponent;
    private ProfileContentComponent contentComponent;
    
    private TLRPC.Chat channel;
    private TLRPC.ChatFull channelInfo;
    private long channelId;
    private boolean isJoined;
    private org.telegram.ui.ActionBar.BaseFragment parentFragment;
    private SharedMediaLayout.SharedMediaPreloader sharedMediaPreloader;
    
    public ChannelProfileBuilder(Context context, Theme.ResourcesProvider resourcesProvider) {
        this.context = context;
        this.resourcesProvider = resourcesProvider;
        this.themeManager = ProfileThemeManager.create(resourcesProvider);
    }
    
    public ChannelProfileBuilder setChannel(TLRPC.Chat channel) {
        this.channel = channel;
        this.channelId = channel != null ? channel.id : 0;
        this.isJoined = channel != null && !channel.left;
        return this;
    }
    
    public ChannelProfileBuilder setChannelInfo(TLRPC.ChatFull channelInfo) {
        this.channelInfo = channelInfo;
        return this;
    }
    
    public ChannelProfileBuilder setParentFragment(org.telegram.ui.ActionBar.BaseFragment fragment) {
        this.parentFragment = fragment;
        return this;
    }
    
    public ChannelProfileBuilder setSharedMediaPreloader(SharedMediaLayout.SharedMediaPreloader preloader) {
        this.sharedMediaPreloader = preloader;
        return this;
    }
    
    public ChannelProfileBuilder setChannelId(long channelId) {
        this.channelId = channelId;
        this.channel = MessagesController.getInstance(0).getChat(channelId);
        this.isJoined = channel != null && !channel.left;
        return this;
    }
    
    public View build() {
        if (channel == null) {
            return new View(context);
        }
        
        ScrollView scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT));
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        
        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
        
        createComponents();
        setupComponents();
        
        rootLayout.addView(headerComponent);
        rootLayout.addView(infoComponent);
        rootLayout.addView(bioUsernameComponent);
        
        if (isJoined) {
            rootLayout.addView(tabsComponent);
            rootLayout.addView(contentComponent);
        }
        
        scrollView.addView(rootLayout);
        
        // Add scroll listener for avatar transformation
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            int scrollY = scrollView.getScrollY();
            headerComponent.setScrollOffset(scrollY);
        });
        
        return scrollView;
    }
    
    private void createComponents() {
        headerComponent = new ProfileHeaderComponent(context, resourcesProvider);
        infoComponent = new ProfileInfoComponent(context, resourcesProvider);
        bioUsernameComponent = new ProfileBioUsernameComponent(context, resourcesProvider);
        actionsComponent = new ProfileActionsComponent(context, resourcesProvider);
        tabsComponent = new ProfileTabsComponent(context, resourcesProvider);
        contentComponent = new ProfileContentComponent(context, resourcesProvider);
    }
    
    private void setupComponents() {
        setupHeader();
        setupInfo();
        setupBioUsername();
        setupActions();
        setupTabs();
        setupContent();
        
        themeManager.applyGradientForProfileType(headerComponent, ProfileThemeManager.ProfileType.CHANNEL);
    }
    
    private void setupHeader() {
        headerComponent.setChat(channel);
        
        // Set name and status on header component (white text on gradient)
        String name = channel.title;
        headerComponent.setName(name);
        
        String subscribersText = getSubscribersText();
        headerComponent.setStatus(subscribersText);
        
        // Set up action click listener for channel-specific actions
        headerComponent.setOnActionClickListener(this::onActionClick);
        
        // Set channel-specific action buttons: Join, Unmute, Share, Report
        String[] actions = {"Join", "Unmute", "Share", "Report"};
        int[] icons = {R.drawable.msg2_chats_add, R.drawable.msg_mute, R.drawable.msg_share, R.drawable.msg_report};
        boolean[] isPrimary = {true, false, false, false};
        headerComponent.setCustomActions(actions, icons, isPrimary);
    }
    
    private void setupInfo() {
        // Set subscriber count
        String subscribersText = getSubscribersText();
        infoComponent.setSubscribers(subscribersText);
        
        // Set verification if channel is verified
        if (channel.verified) {
            infoComponent.setVerification("Verified channel");
        }
    }
    
    private void setupBioUsername() {
        // Set real channel description from channelInfo
        if (channelInfo != null && channelInfo.about != null && !channelInfo.about.isEmpty()) {
            bioUsernameComponent.setBio(channelInfo.about);
        }
        
        // Set real channel username
        if (channel.username != null && !channel.username.isEmpty()) {
            bioUsernameComponent.setUsername(channel.username);
        }
    }
    
    private void setupActions() {
        ProfileActionsComponent.ActionButton[] actions;
        
        if (isJoined) {
            actions = new ProfileActionsComponent.ActionButton[]{
                new ProfileActionsComponent.ActionButton(ProfileActionsComponent.ActionButton.TYPE_MESSAGE, "Message", 0, true),
                new ProfileActionsComponent.ActionButton(ProfileActionsComponent.ActionButton.TYPE_UNMUTE, "Unmute", 0, false),
                new ProfileActionsComponent.ActionButton(ProfileActionsComponent.ActionButton.TYPE_SHARE, "Share", 0, false),
                new ProfileActionsComponent.ActionButton(ProfileActionsComponent.ActionButton.TYPE_LEAVE, "Leave", 0, false)
            };
        } else {
            actions = ProfileActionsComponent.getDefaultChannelActions();
        }
        
        actionsComponent.setActions(actions);
        // Actions now handled in header component
    }
    
    private void setupTabs() {
        String[] tabs = ProfileTabsComponent.getDefaultChannelTabs();
        tabsComponent.setTabs(tabs);
        tabsComponent.setOnTabSelectedListener(this::onTabSelected);
    }
    
    private void setupContent() {
        // Load real content from SharedMediaPreloader for channels
        java.util.ArrayList<ProfileContentComponent.ContentItem> contentItems = new java.util.ArrayList<>();
        
        if (sharedMediaPreloader != null) {
            try {
                SharedMediaLayout.SharedMediaData[] sharedMediaData = sharedMediaPreloader.getSharedMediaData();
                
                // Use MediaDataController constants for media types
                for (int type : new int[]{MediaDataController.MEDIA_PHOTOVIDEO, MediaDataController.MEDIA_FILE, MediaDataController.MEDIA_URL, MediaDataController.MEDIA_MUSIC}) {
                    if (sharedMediaData != null && type < sharedMediaData.length) {
                        SharedMediaLayout.SharedMediaData data = sharedMediaData[type];
                        
                        if (data != null && data.messages != null && !data.messages.isEmpty()) {
                            for (org.telegram.messenger.MessageObject message : data.messages) {
                                if (message != null) {
                                    ProfileContentComponent.ContentItem item = createContentItemFromMessage(message, type);
                                    if (item != null) {
                                        contentItems.add(item);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("ChannelProfileBuilder", "Error loading channel content", e);
            }
        }
        
        contentComponent.setContentItems(contentItems);
        contentComponent.setOnContentItemClickListener(this::onContentItemClick);
    }
    
    private ProfileContentComponent.ContentItem createContentItemFromMessage(org.telegram.messenger.MessageObject message, int type) {
        try {
            ProfileContentComponent.ContentItem item = null;
            
            switch (type) {
                case MediaDataController.MEDIA_PHOTOVIDEO:
                    if (message.isPhoto()) {
                        item = new ProfileContentComponent.ContentItem(ProfileContentComponent.ContentItem.TYPE_PHOTO);
                    } else if (message.isVideo()) {
                        item = new ProfileContentComponent.ContentItem(ProfileContentComponent.ContentItem.TYPE_VIDEO);
                        if (message.getDocument() != null && message.getDocument().attributes != null) {
                            for (TLRPC.DocumentAttribute attr : message.getDocument().attributes) {
                                if (attr instanceof TLRPC.TL_documentAttributeVideo) {
                                    int duration = (int) ((TLRPC.TL_documentAttributeVideo) attr).duration;
                                    item.duration = String.format("%d:%02d", duration / 60, duration % 60);
                                    break;
                                }
                            }
                        }
                    }
                    break;
                case MediaDataController.MEDIA_FILE:
                    item = new ProfileContentComponent.ContentItem(ProfileContentComponent.ContentItem.TYPE_FILE);
                    if (message.getDocument() != null) {
                        item.title = message.getDocument().file_name;
                        if (item.title == null) {
                            item.title = "Document";
                        }
                    }
                    break;
                case MediaDataController.MEDIA_URL:
                    item = new ProfileContentComponent.ContentItem(ProfileContentComponent.ContentItem.TYPE_LINK);
                    item.title = "Link";
                    break;
                case MediaDataController.MEDIA_MUSIC:
                    if (message.isVoice()) {
                        item = new ProfileContentComponent.ContentItem(ProfileContentComponent.ContentItem.TYPE_VOICE);
                        int duration = (int) message.getDuration();
                        item.duration = String.format("%d:%02d", duration / 60, duration % 60);
                    } else if (message.isMusic()) {
                        item = new ProfileContentComponent.ContentItem(ProfileContentComponent.ContentItem.TYPE_MUSIC);
                        int duration = (int) message.getDuration();
                        item.duration = String.format("%d:%02d", duration / 60, duration % 60);
                        if (message.getMusicTitle() != null) {
                            item.title = message.getMusicTitle();
                        }
                        if (message.getMusicAuthor() != null) {
                            item.subtitle = message.getMusicAuthor();
                        }
                    }
                    break;
            }
            
            if (item != null) {
                item.messageObject = message;
            }
            
            return item;
        } catch (Exception e) {
            android.util.Log.e("ChannelProfileBuilder", "Error creating content item", e);
            return null;
        }
    }
    
    private String getSubscribersText() {
        // Get real subscriber count from channel or channelInfo
        int subscriberCount = 0;
        
        if (channelInfo != null && channelInfo.participants_count > 0) {
            subscriberCount = channelInfo.participants_count;
        } else if (channel.participants_count > 0) {
            subscriberCount = channel.participants_count;
        }
        
        if (subscriberCount > 0) {
            return LocaleController.formatPluralString("Subscribers", subscriberCount);
        }
        
        return "Channel"; // Fallback if no subscriber count available
    }
    
    private void onActionClick(String action) {
        if (channel == null || parentFragment == null) {
            return;
        }
        
        try {
            switch (action) {
                case "Join":
                    // Handle join channel
                    parentFragment.getMessagesController().addUserToChat(channel.id, 
                        parentFragment.getUserConfig().getCurrentUser(), 0, null, parentFragment, null);
                    isJoined = true;
                    break;
                case "Unmute":
                    // Handle unmute channel notifications
                    org.telegram.messenger.NotificationsController.getInstance(parentFragment.getCurrentAccount()).muteDialog(-channel.id, 0, false);
                    break;
                case "Share":
                    // Handle share channel
                    try {
                        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        if (channel.username != null) {
                            intent.putExtra(android.content.Intent.EXTRA_TEXT, "https://t.me/" + channel.username);
                            context.startActivity(android.content.Intent.createChooser(intent, "Share"));
                        }
                    } catch (Exception e) {
                        // Ignore share errors
                    }
                    break;
                case "Report":
                    // Handle report channel
                    // This would typically show a report dialog
                    break;
            }
        } catch (Exception e) {
            // Ignore action errors to prevent crashes
        }
    }
    
    private void onTabSelected(int position, String title) {
        // Handle tab selection for channel
    }
    
    private void onContentItemClick(ProfileContentComponent.ContentItem item, int position) {
        // Handle content item click for channel
    }
    
    public void updateTheme() {
        themeManager.updateTheme(headerComponent, actionsComponent, infoComponent, tabsComponent, contentComponent);
        themeManager.applyGradientForProfileType(headerComponent, ProfileThemeManager.ProfileType.CHANNEL);
    }
}
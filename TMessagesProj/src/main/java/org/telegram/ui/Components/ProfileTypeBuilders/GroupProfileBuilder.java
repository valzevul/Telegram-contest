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
import org.telegram.ui.Components.GroupMemberListComponent;
import org.telegram.ui.Components.ProfileActionsComponent;
import org.telegram.ui.Components.ProfileContentComponent;
import org.telegram.ui.Components.ProfileHeaderComponent;
import org.telegram.ui.Components.ProfileInfoComponent;
import org.telegram.ui.Components.ProfileTabsComponent;
import org.telegram.ui.Components.ProfileThemeManager;

public class GroupProfileBuilder {
    private Context context;
    private Theme.ResourcesProvider resourcesProvider;
    private ProfileThemeManager themeManager;
    
    private ProfileHeaderComponent headerComponent;
    private ProfileInfoComponent infoComponent;
    private ProfileActionsComponent actionsComponent;
    private GroupMemberListComponent memberListComponent;
    private ProfileTabsComponent tabsComponent;
    private ProfileContentComponent contentComponent;
    
    private TLRPC.Chat chat;
    private TLRPC.ChatFull chatInfo;
    private long chatId;
    private org.telegram.ui.ActionBar.BaseFragment parentFragment;
    
    public GroupProfileBuilder(Context context, Theme.ResourcesProvider resourcesProvider) {
        this.context = context;
        this.resourcesProvider = resourcesProvider;
        this.themeManager = ProfileThemeManager.create(resourcesProvider);
    }
    
    public GroupProfileBuilder setChat(TLRPC.Chat chat) {
        this.chat = chat;
        this.chatId = chat != null ? chat.id : 0;
        return this;
    }
    
    public GroupProfileBuilder setChatInfo(TLRPC.ChatFull chatInfo) {
        this.chatInfo = chatInfo;
        return this;
    }
    
    public GroupProfileBuilder setParentFragment(org.telegram.ui.ActionBar.BaseFragment fragment) {
        this.parentFragment = fragment;
        return this;
    }
    
    public GroupProfileBuilder setChatId(long chatId) {
        this.chatId = chatId;
        this.chat = MessagesController.getInstance(0).getChat(chatId);
        return this;
    }
    
    public View build() {
        if (chat == null) {
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
        
        // Add member list section for groups
        if (memberListComponent != null) {
            rootLayout.addView(memberListComponent);
        }
        
        rootLayout.addView(tabsComponent);
        rootLayout.addView(contentComponent);
        
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
        actionsComponent = new ProfileActionsComponent(context, resourcesProvider);
        memberListComponent = new GroupMemberListComponent(context, resourcesProvider);
        tabsComponent = new ProfileTabsComponent(context, resourcesProvider);
        contentComponent = new ProfileContentComponent(context, resourcesProvider);
    }
    
    private void setupComponents() {
        setupHeader();
        setupInfo();
        setupActions();
        setupMemberList();
        setupTabs();
        setupContent();
        
        themeManager.applyGradientForProfileType(headerComponent, ProfileThemeManager.ProfileType.GROUP);
    }
    
    private void setupHeader() {
        headerComponent.setChat(chat);
        
        // Set name and status on header component (white text on gradient)
        String name = chat.title;
        headerComponent.setName(name);
        
        String membersText = getMembersText();
        headerComponent.setStatus(membersText);
        
        // Set up menu click listener
        headerComponent.setOnMenuClickListener(this::onMenuClick);
        
        // Set up action click listener for group-specific actions
        headerComponent.setOnActionClickListener(this::onActionClick);
        
        // Set group-specific action buttons: Message, Unmute, Voice Chat, Leave
        String[] actions = {"Message", "Unmute", "Voice Chat", "Leave"};
        int[] icons = {R.drawable.msg_send, R.drawable.msg_mute, R.drawable.msg_voicechat, R.drawable.msg_leave};
        boolean[] isPrimary = {true, false, false, false};
        headerComponent.setCustomActions(actions, icons, isPrimary);
    }
    
    private void setupInfo() {
        if (chat.username != null && !chat.username.isEmpty()) {
            infoComponent.setUsername(chat.username);
        }
        
        // Description will be loaded from ChatFull info when available
    }
    
    private void setupActions() {
        ProfileActionsComponent.ActionButton[] actions = ProfileActionsComponent.getDefaultGroupActions();
        actionsComponent.setActions(actions);
        // Actions now handled in header component
    }
    
    private void setupTabs() {
        String[] tabs = ProfileTabsComponent.getDefaultGroupTabs();
        tabsComponent.setTabs(tabs);
        tabsComponent.setOnTabSelectedListener(this::onTabSelected);
    }
    
    private void setupMemberList() {
        if (memberListComponent != null) {
            // Load real group members from ChatFull
            if (chatInfo != null && chatInfo.participants != null) {
                java.util.List<TLRPC.User> members = new java.util.ArrayList<>();
                java.util.List<Boolean> adminStatus = new java.util.ArrayList<>();
                
                TLRPC.ChatParticipants participants = chatInfo.participants;
                MessagesController messagesController = MessagesController.getInstance(parentFragment != null ? parentFragment.getCurrentAccount() : 0);
                
                for (TLRPC.ChatParticipant participant : participants.participants) {
                    TLRPC.User member = messagesController.getUser(participant.user_id);
                    if (member != null) {
                        members.add(member);
                        // Check if participant is admin
                        boolean isAdmin = participant instanceof TLRPC.TL_chatParticipantAdmin ||
                                         participant instanceof TLRPC.TL_chatParticipantCreator;
                        adminStatus.add(isAdmin);
                        
                        // Limit to reasonable number for performance  
                        if (members.size() >= 10) break;
                    }
                }
                
                memberListComponent.setMembers(members, adminStatus);
            }
            
            // Set up click listeners
            memberListComponent.setOnMemberClickListener((user, isAdmin) -> {
                if (parentFragment != null) {
                    // Open user profile
                    android.os.Bundle args = new android.os.Bundle();
                    args.putLong("user_id", user.id);
                    parentFragment.presentFragment(new org.telegram.ui.ProfileActivity(args));
                }
            });
            
            memberListComponent.setOnAddMembersClickListener(() -> {
                if (parentFragment != null) {
                    // Open add members dialog
                    android.os.Bundle args = new android.os.Bundle();
                    args.putLong("chat_id", chat.id);
                    // parentFragment.presentFragment(new AddMembersActivity(args));
                }
            });
        }
    }
    
    private void setupContent() {
        // Load real content items only - no mock data
        contentComponent.setContentItems(new java.util.ArrayList<>());
        contentComponent.setOnContentItemClickListener(this::onContentItemClick);
    }
    
    private String getMembersText() {
        if (chat.participants_count > 0) {
            int onlineCount = getOnlineMembersCount();
            return LocaleController.formatPluralString("Members", chat.participants_count) + 
                   ", " + LocaleController.formatPluralString("OnlineCount", onlineCount);
        }
        return "";
    }
    
    private int getOnlineMembersCount() {
        // Mock online count
        return Math.min(chat.participants_count, 2);
    }
    
    private void onActionClick(String action) {
        switch (action) {
            case "Message":
                // Handle message group
                break;
            case "Unmute":
                // Handle unmute group
                break;
            case "Voice Chat":
                // Handle voice chat
                break;
            case "Leave":
                // Handle leave group
                break;
        }
    }
    
    private void onTabSelected(int position, String title) {
        // Handle tab selection for group
    }
    
    private void onContentItemClick(ProfileContentComponent.ContentItem item, int position) {
        // Handle content item click for group
    }
    
    private void onMenuClick(android.view.View view) {
        // Show group-specific menu
        if (parentFragment != null && parentFragment.getParentActivity() != null) {
            try {
                // Create a group-specific action menu
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(parentFragment.getParentActivity());
                String[] options = {"Share Group", "Search", "Mute", "Leave Group"};
                
                builder.setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Share
                            // Handle share group
                            break;
                        case 1: // Search
                            // Handle search in group
                            break;
                        case 2: // Mute
                            onActionClick("Unmute");
                            break;
                        case 3: // Leave
                            onActionClick("Leave");
                            break;
                    }
                });
                
                builder.show();
                
            } catch (Exception e) {
                // Fallback
                android.widget.Toast.makeText(parentFragment.getParentActivity(), 
                    "Group profile menu", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    public void updateTheme() {
        themeManager.updateTheme(headerComponent, actionsComponent, infoComponent, tabsComponent, contentComponent);
        if (memberListComponent != null) {
            memberListComponent.updateTheme();
        }
        themeManager.applyGradientForProfileType(headerComponent, ProfileThemeManager.ProfileType.GROUP);
    }
}
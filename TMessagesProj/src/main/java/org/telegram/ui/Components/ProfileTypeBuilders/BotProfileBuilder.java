package org.telegram.ui.Components.ProfileTypeBuilders;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;

import org.telegram.messenger.ContactsController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ProfileActionsComponent;
import org.telegram.ui.Components.ProfileContentComponent;
import org.telegram.ui.Components.ProfileHeaderComponent;
import org.telegram.ui.Components.ProfileInfoComponent;
import org.telegram.ui.Components.ProfileTabsComponent;
import org.telegram.ui.Components.ProfileThemeManager;

public class BotProfileBuilder {
    private Context context;
    private Theme.ResourcesProvider resourcesProvider;
    private ProfileThemeManager themeManager;
    
    private ProfileHeaderComponent headerComponent;
    private ProfileInfoComponent infoComponent;
    private ProfileActionsComponent actionsComponent;
    private ProfileTabsComponent tabsComponent;
    private ProfileContentComponent contentComponent;
    
    private TLRPC.User bot;
    private TLRPC.UserFull botInfo;
    private TLRPC.TL_userFull_layer131 botInfoLayer131; // For bot-specific info
    private long botId;
    private org.telegram.ui.ActionBar.BaseFragment parentFragment;
    
    public BotProfileBuilder(Context context, Theme.ResourcesProvider resourcesProvider) {
        this.context = context;
        this.resourcesProvider = resourcesProvider;
        this.themeManager = ProfileThemeManager.create(resourcesProvider);
    }
    
    public BotProfileBuilder setBot(TLRPC.User bot) {
        this.bot = bot;
        this.botId = bot != null ? bot.id : 0;
        return this;
    }
    
    public BotProfileBuilder setBotInfo(TLRPC.UserFull botInfo) {
        this.botInfo = botInfo;
        return this;
    }
    
    public BotProfileBuilder setParentFragment(org.telegram.ui.ActionBar.BaseFragment fragment) {
        this.parentFragment = fragment;
        return this;
    }
    
    public BotProfileBuilder setBotId(long botId) {
        this.botId = botId;
        this.bot = MessagesController.getInstance(0).getUser(botId);
        return this;
    }
    
    public View build() {
        if (bot == null) {
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
        
        if (hasUsefulContent()) {
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
        actionsComponent = new ProfileActionsComponent(context, resourcesProvider);
        tabsComponent = new ProfileTabsComponent(context, resourcesProvider);
        contentComponent = new ProfileContentComponent(context, resourcesProvider);
    }
    
    private void setupComponents() {
        setupHeader();
        setupInfo();
        setupActions();
        setupTabs();
        setupContent();
        
        themeManager.applyGradientForProfileType(headerComponent, ProfileThemeManager.ProfileType.BOT);
    }
    
    private void setupHeader() {
        headerComponent.setUser(bot);
        
        // Set name and status on header component (white text on gradient)
        String name = ContactsController.formatName(bot.first_name, bot.last_name);
        headerComponent.setName(name);
        
        String monthlyUsers = getMonthlyUsersText();
        if (monthlyUsers != null) {
            headerComponent.setStatus(monthlyUsers);
        } else {
            headerComponent.setStatus(LocaleController.getString("Bot", R.string.Bot));
        }
        
        // Set up menu click listener
        headerComponent.setOnMenuClickListener(this::onMenuClick);
        
        // Set up action click listener for bot-specific actions
        headerComponent.setOnActionClickListener(this::onActionClick);
        
        // Set bot-specific action buttons: Message, Unmute, Share, Stop
        String[] actions = {"Message", "Unmute", "Share", "Stop"};
        int[] icons = {R.drawable.msg_send, R.drawable.msg_mute, R.drawable.msg_share, R.drawable.msg_block};
        boolean[] isPrimary = {true, false, false, false};
        headerComponent.setCustomActions(actions, icons, isPrimary);
    }
    
    private void setupInfo() {
        // Set real username from bot data
        String username = UserObject.getPublicUsername(bot);
        if (username != null && !username.isEmpty()) {
            infoComponent.setUsername(username);
        }
        
        // Set real bot description from botInfo
        if (botInfo != null && botInfo.about != null && !botInfo.about.isEmpty()) {
            infoComponent.setBio(botInfo.about);
        }
        
        // Set bot commands count if available
        String commandsInfo = getBotCommandsInfo();
        if (commandsInfo != null) {
            infoComponent.setSubscribers(commandsInfo);
        }
        
        if (hasVerification()) {
            infoComponent.setVerification("This user was verified by the organization 'Check'.");
        }
    }
    
    private void setupActions() {
        ProfileActionsComponent.ActionButton[] actions = ProfileActionsComponent.getDefaultBotActions();
        actionsComponent.setActions(actions);
        // Actions now handled in header component
    }
    
    private void setupTabs() {
        String[] tabs = ProfileTabsComponent.getDefaultBotTabs();
        tabsComponent.setTabs(tabs);
        tabsComponent.setOnTabSelectedListener(this::onTabSelected);
    }
    
    private void setupContent() {
        // For bots, show mini app integration instead of media
        if (hasMiniApp()) {
            setupMiniAppContent();
        } else {
            contentComponent.setContentItems(new java.util.ArrayList<>());
        }
        contentComponent.setOnContentItemClickListener(this::onContentItemClick);
    }
    
    private boolean hasMiniApp() {
        // Simplified check - look for any menu_button presence
        return botInfo != null && botInfo.bot_info != null && 
               botInfo.bot_info.menu_button != null;
    }
    
    private void setupMiniAppContent() {
        // Create mini app integration UI
        LinearLayout miniAppLayout = createMiniAppLayout();
        
        // Replace content component with mini app layout
        // This would typically be integrated differently in the real implementation
    }
    
    private LinearLayout createMiniAppLayout() {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), 
                         AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        
        // Open App button
        TextView openAppButton = new TextView(context);
        openAppButton.setText("Open App");
        openAppButton.setTextSize(16);
        openAppButton.setTextColor(0xFFFFFFFF);
        openAppButton.setGravity(android.view.Gravity.CENTER);
        openAppButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(48));
        buttonParams.topMargin = AndroidUtilities.dp(16);
        openAppButton.setLayoutParams(buttonParams);
        
        android.graphics.drawable.GradientDrawable buttonBg = new android.graphics.drawable.GradientDrawable();
        buttonBg.setCornerRadius(AndroidUtilities.dp(8));
        buttonBg.setColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, resourcesProvider));
        openAppButton.setBackground(buttonBg);
        
        openAppButton.setOnClickListener(v -> {
            if (parentFragment != null && botInfo != null) {
                // Launch mini app
                try {
                    android.os.Bundle args = new android.os.Bundle();
                    args.putLong("bot_id", bot.id);
                    args.putString("app_name", "duckattack"); // From mockup
                    // parentFragment.presentFragment(new BotWebViewActivity(args));
                } catch (Exception e) {
                    // Handle mini app launch error
                }
            }
        });
        
        layout.addView(openAppButton);
        
        // Terms of service text
        TextView termsText = new TextView(context);
        String termsString = "By launching this mini app, you agree to the Terms of Service for Mini Apps.";
        termsText.setText(termsString);
        termsText.setTextSize(13);
        termsText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
        termsText.setGravity(android.view.Gravity.CENTER);
        
        LinearLayout.LayoutParams termsParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        termsParams.topMargin = AndroidUtilities.dp(12);
        termsText.setLayoutParams(termsParams);
        layout.addView(termsText);
        
        // Bot statistics (Toncoin, Stars)
        addBotStatistics(layout);
        
        // Add to Group option
        addGroupIntegration(layout);
        
        return layout;
    }
    
    private void addBotStatistics(LinearLayout layout) {
        // Toncoin balance
        LinearLayout tonLayout = createStatisticItem(
            R.drawable.msg_payment_provider, // Use existing drawable
            "Toncoin", 
            "12,290", // From mockup
            Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, resourcesProvider)
        );
        layout.addView(tonLayout);
        
        // Stars balance  
        LinearLayout starsLayout = createStatisticItem(
            R.drawable.msg_info, // Use existing drawable
            "Stars",
            "600", // From mockup  
            0xFFFFA500 // Orange color for stars
        );
        layout.addView(starsLayout);
    }
    
    private LinearLayout createStatisticItem(int iconRes, String title, String value, int valueColor) {
        LinearLayout item = new LinearLayout(context);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(android.view.Gravity.CENTER_VERTICAL);
        item.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), 
                       AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(48));
        itemParams.topMargin = AndroidUtilities.dp(8);
        item.setLayoutParams(itemParams);
        
        // Icon
        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
            AndroidUtilities.dp(24), AndroidUtilities.dp(24));
        iconParams.rightMargin = AndroidUtilities.dp(12);
        icon.setLayoutParams(iconParams);
        item.addView(icon);
        
        // Title
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(16);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleView.setLayoutParams(titleParams);
        item.addView(titleView);
        
        // Value
        TextView valueView = new TextView(context);
        valueView.setText(value);
        valueView.setTextSize(16);
        valueView.setTextColor(valueColor);
        valueView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        item.addView(valueView);
        
        return item;
    }
    
    private void addGroupIntegration(LinearLayout layout) {
        LinearLayout groupItem = new LinearLayout(context);
        groupItem.setOrientation(LinearLayout.HORIZONTAL);
        groupItem.setGravity(android.view.Gravity.CENTER_VERTICAL);
        groupItem.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), 
                            AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        
        LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(48));
        groupParams.topMargin = AndroidUtilities.dp(16);
        groupItem.setLayoutParams(groupParams);
        
        // Group icon
        ImageView groupIcon = new ImageView(context);
        groupIcon.setImageResource(R.drawable.msg_groups);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
            AndroidUtilities.dp(24), AndroidUtilities.dp(24));
        iconParams.rightMargin = AndroidUtilities.dp(12);
        groupIcon.setLayoutParams(iconParams);
        groupItem.addView(groupIcon);
        
        // Add to Group text
        TextView groupText = new TextView(context);
        groupText.setText("Add to Group or Channel");
        groupText.setTextSize(16);
        groupText.setTextColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, resourcesProvider));
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        groupText.setLayoutParams(textParams);
        groupItem.addView(groupText);
        
        groupItem.setOnClickListener(v -> {
            if (parentFragment != null) {
                // Show group/channel selection dialog
                android.os.Bundle args = new android.os.Bundle();
                args.putLong("bot_id", bot.id);
                // parentFragment.presentFragment(new GroupSelectionActivity(args));
            }
        });
        
        layout.addView(groupItem);
        
        // Management info text
        TextView infoText = new TextView(context);
        infoText.setText("This bot is able to manage a group or channel.");
        infoText.setTextSize(13);
        infoText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
        infoText.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), 
                           AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        infoText.setLayoutParams(infoParams);
        layout.addView(infoText);
    }
    
    private String getMonthlyUsersText() {
        // Try to get real bot usage statistics
        if (botInfo != null) {
            // Check if bot has usage statistics in the UserFull object
            // Note: Monthly users might not be directly available in TLRPC
            // This would typically come from bot analytics or special bot info
            try {
                // Look for bot-specific statistics
                if (botInfo.common_chats_count > 0) {
                    return botInfo.common_chats_count + " common chats";
                }
            } catch (Exception e) {
                // Fallback
            }
        }
        
        // For regular bots without specific statistics, just show "Bot"
        return null;
    }
    
    private String getBotCommandsInfo() {
        // Get bot commands information
        if (botInfo != null && botInfo.bot_info != null) {
            // Use the generic BotInfo class instead of TL_botInfo
            if (botInfo.bot_info.commands != null && !botInfo.bot_info.commands.isEmpty()) {
                int commandCount = botInfo.bot_info.commands.size();
                return commandCount + " command" + (commandCount != 1 ? "s" : "");
            }
        }
        return null;
    }
    
    private boolean hasVerification() {
        // Check if bot has verification status
        return bot != null && bot.verified;
    }
    
    private boolean hasUsefulContent() {
        // Check if bot has media or other content to show
        return true; // For now, always show content
    }
    
    private void onActionClick(String action) {
        if (bot == null || parentFragment == null) {
            return;
        }
        
        try {
            switch (action) {
                case "Message":
                    // Start chat with bot
                    android.os.Bundle args = new android.os.Bundle();
                    args.putLong("user_id", bot.id);
                    parentFragment.presentFragment(new org.telegram.ui.ChatActivity(args));
                    break;
                case "Unmute":
                    // Handle unmute bot notifications
                    org.telegram.messenger.NotificationsController.getInstance(parentFragment.getCurrentAccount()).muteDialog(bot.id, 0, false);
                    break;
                case "Share":
                    // Handle share bot
                    try {
                        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        String username = UserObject.getPublicUsername(bot);
                        if (username != null) {
                            intent.putExtra(android.content.Intent.EXTRA_TEXT, "https://t.me/" + username);
                            context.startActivity(android.content.Intent.createChooser(intent, "Share"));
                        }
                    } catch (Exception e) {
                        // Ignore share errors
                    }
                    break;
                case "Stop":
                    // Handle stop/block bot
                    parentFragment.getMessagesController().blockPeer(bot.id);
                    break;
            }
        } catch (Exception e) {
            // Ignore action errors to prevent crashes
        }
    }
    
    private void onTabSelected(int position, String title) {
        // Handle tab selection for bot
    }
    
    private void onContentItemClick(ProfileContentComponent.ContentItem item, int position) {
        // Handle content item click for bot
    }
    
    private void onMenuClick(android.view.View view) {
        // Show bot-specific menu
        if (parentFragment != null && parentFragment.getParentActivity() != null) {
            try {
                // Create a bot-specific action menu
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(parentFragment.getParentActivity());
                String[] options = {"Share Bot", "Add to Group", "Settings", "Stop Bot"};
                
                builder.setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Share
                            onActionClick("Share");
                            break;
                        case 1: // Add to Group
                            // Handle add to group
                            break;
                        case 2: // Settings
                            // Handle bot settings
                            break;
                        case 3: // Stop Bot
                            onActionClick("Stop");
                            break;
                    }
                });
                
                builder.show();
                
            } catch (Exception e) {
                // Fallback
                android.widget.Toast.makeText(parentFragment.getParentActivity(), 
                    "Bot profile menu", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    public void updateTheme() {
        themeManager.updateTheme(headerComponent, actionsComponent, infoComponent, tabsComponent, contentComponent);
        themeManager.applyGradientForProfileType(headerComponent, ProfileThemeManager.ProfileType.BOT);
    }
}
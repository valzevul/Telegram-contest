package org.telegram.ui.Components;

import android.content.Context;
import android.view.View;

import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ProfileTypeBuilders.BotProfileBuilder;
import org.telegram.ui.Components.ProfileTypeBuilders.BusinessProfileBuilder;
import org.telegram.ui.Components.ProfileTypeBuilders.ChannelProfileBuilder;
import org.telegram.ui.Components.ProfileTypeBuilders.GroupProfileBuilder;
import org.telegram.ui.Components.ProfileTypeBuilders.UserProfileBuilder;

/**
 * Helper for building the new profile layouts as per Telegram redesign mockups.
 * Uses the new modular component system with proper profile type builders.
 */
public class UserProfileViewBuilder {
    
    /**
     * Builds a user profile layout using the new component system.
     * @param context Android context
     * @param resourcesProvider Telegram Theme.ResourcesProvider
     * @param user the user object
     * @return the root view for the user profile
     */
    public static View buildUserProfile(Context context, Theme.ResourcesProvider resourcesProvider, TLRPC.User user) {
        UserProfileBuilder builder = new UserProfileBuilder(context, resourcesProvider);
        return builder.setUser(user).build();
    }
    
    /**
     * Builds a user profile layout using the new component system with full user info.
     * @param context Android context
     * @param resourcesProvider Telegram Theme.ResourcesProvider
     * @param user the user object
     * @param userInfo the full user info object containing bio and other details
     * @return the root view for the user profile
     */
    public static View buildUserProfile(Context context, Theme.ResourcesProvider resourcesProvider, TLRPC.User user, TLRPC.UserFull userInfo) {
        UserProfileBuilder builder = new UserProfileBuilder(context, resourcesProvider);
        return builder.setUser(user).setUserInfo(userInfo).build();
    }
    
    /**
     * Builds a user profile layout with parent fragment for functionality.
     * @param context Android context
     * @param resourcesProvider Telegram Theme.ResourcesProvider
     * @param user the user object
     * @param userInfo the full user info object containing bio and other details
     * @param parentFragment the parent fragment for handling actions
     * @return the root view for the user profile
     */
    public static View buildUserProfile(Context context, Theme.ResourcesProvider resourcesProvider, TLRPC.User user, TLRPC.UserFull userInfo, org.telegram.ui.ActionBar.BaseFragment parentFragment) {
        UserProfileBuilder builder = new UserProfileBuilder(context, resourcesProvider);
        return builder.setUser(user).setUserInfo(userInfo).setParentFragment(parentFragment).build();
    }
    
    /**
     * Builds a user profile layout with SharedMediaPreloader for real media loading.
     * @param context Android context
     * @param resourcesProvider Telegram Theme.ResourcesProvider  
     * @param user the user object
     * @param userInfo the full user info object containing bio and other details
     * @param parentFragment the parent fragment for handling actions
     * @param sharedMediaPreloader the preloader for loading conversation media
     * @return the root view for the user profile
     */
    public static View buildUserProfile(Context context, Theme.ResourcesProvider resourcesProvider, TLRPC.User user, TLRPC.UserFull userInfo, org.telegram.ui.ActionBar.BaseFragment parentFragment, org.telegram.ui.Components.SharedMediaLayout.SharedMediaPreloader sharedMediaPreloader) {
        UserProfileBuilder builder = new UserProfileBuilder(context, resourcesProvider);
        return builder.setUser(user).setUserInfo(userInfo).setParentFragment(parentFragment).setSharedMediaPreloader(sharedMediaPreloader).build();
    }
    
    /**
     * Builds a bot profile layout using the new component system.
     * @param context Android context
     * @param resourcesProvider Telegram Theme.ResourcesProvider
     * @param bot the bot user object
     * @return the root view for the bot profile
     */
    public static View buildBotProfile(Context context, Theme.ResourcesProvider resourcesProvider, TLRPC.User bot) {
        BotProfileBuilder builder = new BotProfileBuilder(context, resourcesProvider);
        return builder.setBot(bot).build();
    }
    
    /**
     * Builds a bot profile layout with full bot info and parent fragment.
     * @param context Android context
     * @param resourcesProvider Telegram Theme.ResourcesProvider
     * @param bot the bot user object
     * @param botInfo the full bot info containing commands, description, etc.
     * @param parentFragment the parent fragment for handling actions
     * @return the root view for the bot profile
     */
    public static View buildBotProfile(Context context, Theme.ResourcesProvider resourcesProvider, TLRPC.User bot, TLRPC.UserFull botInfo, org.telegram.ui.ActionBar.BaseFragment parentFragment) {
        BotProfileBuilder builder = new BotProfileBuilder(context, resourcesProvider);
        return builder.setBot(bot).setBotInfo(botInfo).setParentFragment(parentFragment).build();
    }
    
    /**
     * Builds a business profile layout using the new component system.
     * @param context Android context
     * @param resourcesProvider Telegram Theme.ResourcesProvider
     * @param business the business user object
     * @return the root view for the business profile
     */
    public static View buildBusinessProfile(Context context, Theme.ResourcesProvider resourcesProvider, TLRPC.User business) {
        BusinessProfileBuilder builder = new BusinessProfileBuilder(context, resourcesProvider);
        return builder.setBusiness(business).build();
    }
    
    /**
     * Builds a business profile layout with full business info and parent fragment.
     * @param context Android context
     * @param resourcesProvider Telegram Theme.ResourcesProvider
     * @param business the business user object
     * @param businessInfo the full business info containing hours, location, etc.
     * @param parentFragment the parent fragment for handling actions
     * @return the root view for the business profile
     */
    public static View buildBusinessProfile(Context context, Theme.ResourcesProvider resourcesProvider, TLRPC.User business, TLRPC.UserFull businessInfo, org.telegram.ui.ActionBar.BaseFragment parentFragment) {
        BusinessProfileBuilder builder = new BusinessProfileBuilder(context, resourcesProvider);
        return builder.setBusiness(business).setBusinessInfo(businessInfo).setParentFragment(parentFragment).build();
    }
    
    /**
     * Builds a group profile layout using the new component system.
     * @param context Android context
     * @param resourcesProvider Telegram Theme.ResourcesProvider
     * @param chat the group chat object
     * @return the root view for the group profile
     */
    public static View buildGroupProfile(Context context, Theme.ResourcesProvider resourcesProvider, TLRPC.Chat chat) {
        GroupProfileBuilder builder = new GroupProfileBuilder(context, resourcesProvider);
        return builder.setChat(chat).build();
    }
    
    /**
     * Builds a group profile layout with full group info and parent fragment.
     * @param context Android context
     * @param resourcesProvider Telegram Theme.ResourcesProvider
     * @param chat the group chat object
     * @param chatInfo the full group info containing description, member count, etc.
     * @param parentFragment the parent fragment for handling actions
     * @return the root view for the group profile
     */
    public static View buildGroupProfile(Context context, Theme.ResourcesProvider resourcesProvider, TLRPC.Chat chat, TLRPC.ChatFull chatInfo, org.telegram.ui.ActionBar.BaseFragment parentFragment) {
        GroupProfileBuilder builder = new GroupProfileBuilder(context, resourcesProvider);
        return builder.setChat(chat).setChatInfo(chatInfo).setParentFragment(parentFragment).build();
    }
    
    /**
     * Builds a channel profile layout using the new component system.
     * @param context Android context
     * @param resourcesProvider Telegram Theme.ResourcesProvider
     * @param channel the channel chat object
     * @return the root view for the channel profile
     */
    public static View buildChannelProfile(Context context, Theme.ResourcesProvider resourcesProvider, TLRPC.Chat channel) {
        ChannelProfileBuilder builder = new ChannelProfileBuilder(context, resourcesProvider);
        return builder.setChannel(channel).build();
    }
    
    /**
     * Builds a channel profile layout with full channel info and parent fragment.
     * @param context Android context
     * @param resourcesProvider Telegram Theme.ResourcesProvider
     * @param channel the channel chat object
     * @param channelInfo the full channel info containing description, participant count, etc.
     * @param parentFragment the parent fragment for handling actions
     * @return the root view for the channel profile
     */
    public static View buildChannelProfile(Context context, Theme.ResourcesProvider resourcesProvider, TLRPC.Chat channel, TLRPC.ChatFull channelInfo, org.telegram.ui.ActionBar.BaseFragment parentFragment) {
        ChannelProfileBuilder builder = new ChannelProfileBuilder(context, resourcesProvider);
        return builder.setChannel(channel).setChannelInfo(channelInfo).setParentFragment(parentFragment).build();
    }
    
    /**
     * Builds a channel profile layout with SharedMediaPreloader for real media loading.
     * @param context Android context
     * @param resourcesProvider Telegram Theme.ResourcesProvider
     * @param channel the channel chat object
     * @param channelInfo the full channel info containing description, participant count, etc.
     * @param parentFragment the parent fragment for handling actions
     * @param sharedMediaPreloader the preloader for loading channel media
     * @return the root view for the channel profile
     */
    public static View buildChannelProfile(Context context, Theme.ResourcesProvider resourcesProvider, TLRPC.Chat channel, TLRPC.ChatFull channelInfo, org.telegram.ui.ActionBar.BaseFragment parentFragment, org.telegram.ui.Components.SharedMediaLayout.SharedMediaPreloader sharedMediaPreloader) {
        ChannelProfileBuilder builder = new ChannelProfileBuilder(context, resourcesProvider);
        return builder.setChannel(channel).setChannelInfo(channelInfo).setParentFragment(parentFragment).setSharedMediaPreloader(sharedMediaPreloader).build();
    }
    
    /**
     * Legacy method for backward compatibility. Use buildUserProfile instead.
     * @deprecated Use {@link #buildUserProfile(Context, Theme.ResourcesProvider, TLRPC.User)} instead
     */
    @Deprecated
    public static View buildUserProfileLayout(
            Context context,
            Theme.ResourcesProvider themeProvider,
            String nameText,
            String statusText,
            String usernameText,
            String bioText,
            TLRPC.User user
    ) {
        return buildUserProfile(context, themeProvider, user);
    }
}

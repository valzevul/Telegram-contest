package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Color;

import org.telegram.ui.ActionBar.Theme;

public class ProfileThemeManager {
    private Theme.ResourcesProvider resourcesProvider;
    private boolean isDarkTheme;
    
    public ProfileThemeManager(Theme.ResourcesProvider resourcesProvider) {
        this.resourcesProvider = resourcesProvider;
        this.isDarkTheme = Theme.isCurrentThemeDark();
    }
    
    public void updateTheme(ProfileHeaderComponent header, ProfileActionsComponent actions, 
                          ProfileInfoComponent info, ProfileTabsComponent tabs, 
                          ProfileContentComponent content) {
        if (header != null) {
            header.updateTheme();
        }
        
        if (actions != null) {
            actions.updateTheme();
        }
        
        if (info != null) {
            info.updateTheme();
        }
        
        if (tabs != null) {
            tabs.updateTheme();
        }
        
        if (content != null) {
            content.updateTheme();
        }
    }
    
    public int getGradientStartColor() {
        if (isDarkTheme) {
            // Darker blue gradient for night mode user profiles
            return Color.parseColor("#1f3a93");
        } else {
            return Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, resourcesProvider);
        }
    }
    
    public int getGradientEndColor() {
        if (isDarkTheme) {
            // Darker blue gradient end for night mode user profiles
            return Color.parseColor("#3949ab");
        } else {
            return Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, resourcesProvider);
        }
    }
    
    public int getGiftGradientStartColor() {
        if (isDarkTheme) {
            // Darker purple for night mode gift profiles
            return Color.parseColor("#6b21a8");
        } else {
            return Color.parseColor("#A855F7");
        }
    }
    
    public int getGiftGradientEndColor() {
        if (isDarkTheme) {
            // Darker pink for night mode gift profiles
            return Color.parseColor("#be185d");
        } else {
            return Color.parseColor("#F472B6");
        }
    }
    
    public int getBotGradientStartColor() {
        if (isDarkTheme) {
            // Darker purple gradient for night mode bot profiles
            return Color.parseColor("#5b21b6");
        } else {
            return Theme.getColor(Theme.key_avatar_backgroundViolet, resourcesProvider);
        }
    }
    
    public int getBotGradientEndColor() {
        if (isDarkTheme) {
            // Darker purple gradient end for night mode bot profiles
            return Color.parseColor("#7c3aed");
        } else {
            return Theme.getColor(Theme.key_avatar_backgroundViolet, resourcesProvider);
        }
    }
    
    public int getBusinessGradientStartColor() {
        if (isDarkTheme) {
            // Darker green gradient for night mode business profiles
            return Color.parseColor("#166534");
        } else {
            return Theme.getColor(Theme.key_avatar_backgroundGreen, resourcesProvider);
        }
    }
    
    public int getBusinessGradientEndColor() {
        if (isDarkTheme) {
            // Darker green gradient end for night mode business profiles
            return Color.parseColor("#22c55e");
        } else {
            return Theme.getColor(Theme.key_avatar_backgroundGreen, resourcesProvider);
        }
    }
    
    public int getGroupGradientStartColor() {
        if (isDarkTheme) {
            // Darker orange gradient for night mode group profiles
            return Color.parseColor("#c2410c");
        } else {
            return Theme.getColor(Theme.key_avatar_backgroundOrange, resourcesProvider);
        }
    }
    
    public int getGroupGradientEndColor() {
        if (isDarkTheme) {
            // Darker orange gradient end for night mode group profiles
            return Color.parseColor("#f97316");
        } else {
            return Theme.getColor(Theme.key_avatar_backgroundOrange, resourcesProvider);
        }
    }
    
    public int getChannelGradientStartColor() {
        if (isDarkTheme) {
            // Darker red gradient for night mode channel profiles
            return Color.parseColor("#991b1b");
        } else {
            return Theme.getColor(Theme.key_avatar_backgroundRed, resourcesProvider);
        }
    }
    
    public int getChannelGradientEndColor() {
        if (isDarkTheme) {
            // Darker red gradient end for night mode channel profiles
            return Color.parseColor("#ef4444");
        } else {
            return Theme.getColor(Theme.key_avatar_backgroundRed, resourcesProvider);
        }
    }
    
    public void setDarkTheme(boolean isDarkTheme) {
        this.isDarkTheme = isDarkTheme;
    }
    
    public boolean isDarkTheme() {
        return isDarkTheme;
    }
    
    public Theme.ResourcesProvider getResourcesProvider() {
        return resourcesProvider;
    }
    
    public static ProfileThemeManager create(Theme.ResourcesProvider resourcesProvider) {
        return new ProfileThemeManager(resourcesProvider);
    }
    
    public void applyGradientForProfileType(ProfileHeaderComponent header, ProfileType type) {
        if (header == null) return;
        
        int startColor, endColor;
        
        switch (type) {
            case USER:
                startColor = getGradientStartColor();
                endColor = getGradientEndColor();
                break;
            case BOT:
                startColor = getBotGradientStartColor();
                endColor = getBotGradientEndColor();
                break;
            case BUSINESS:
                startColor = getBusinessGradientStartColor();
                endColor = getBusinessGradientEndColor();
                break;
            case GROUP:
                startColor = getGroupGradientStartColor();
                endColor = getGroupGradientEndColor();
                break;
            case CHANNEL:
                startColor = getChannelGradientStartColor();
                endColor = getChannelGradientEndColor();
                break;
            case GIFT:
                startColor = getGiftGradientStartColor();
                endColor = getGiftGradientEndColor();
                break;
            default:
                startColor = getGradientStartColor();
                endColor = getGradientEndColor();
                break;
        }
        
        header.setGradientColors(startColor, endColor);
    }
    
    public enum ProfileType {
        USER,
        BOT,
        BUSINESS,
        GROUP,
        CHANNEL,
        GIFT
    }
}
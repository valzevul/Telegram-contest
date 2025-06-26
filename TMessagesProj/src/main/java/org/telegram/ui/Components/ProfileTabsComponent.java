package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

public class ProfileTabsComponent extends HorizontalScrollView {
    private LinearLayout tabsContainer;
    private Theme.ResourcesProvider resourcesProvider;
    private OnTabSelectedListener listener;
    private int selectedTab = 0;
    private TextView[] tabViews;
    private String[] tabTitles;
    
    public interface OnTabSelectedListener {
        void onTabSelected(int position, String title);
    }
    
    public ProfileTabsComponent(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(View.OVER_SCROLL_NEVER);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = AndroidUtilities.dp(8); // Reduced by 4px
        params.bottomMargin = AndroidUtilities.dp(0);
        setLayoutParams(params);
        
        // White background for clean section look
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
        
        createTabsContainer();
    }
    
    private void createTabsContainer() {
        tabsContainer = new LinearLayout(getContext());
        tabsContainer.setOrientation(LinearLayout.HORIZONTAL);
        tabsContainer.setGravity(Gravity.CENTER_VERTICAL);
        tabsContainer.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(6), AndroidUtilities.dp(16), AndroidUtilities.dp(6)); // Tighter padding
        
        addView(tabsContainer);
    }
    
    public void setTabs(String[] titles) {
        this.tabTitles = titles;
        tabsContainer.removeAllViews();
        tabViews = new TextView[titles.length];
        
        for (int i = 0; i < titles.length; i++) {
            TextView tab = createTab(titles[i], i);
            tabViews[i] = tab;
            tabsContainer.addView(tab);
        }
        
        if (titles.length > 0) {
            selectTab(0);
        }
    }
    
    private TextView createTab(String title, int position) {
        TextView tab = new TextView(getContext());
        tab.setText(title);
        tab.setTextSize(13); // Reduced by 1px
        tab.setGravity(Gravity.CENTER);
        tab.setSingleLine(true);
        
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            AndroidUtilities.dp(40)); // Shorter tabs
        
        if (position > 0) {
            tabParams.leftMargin = AndroidUtilities.dp(18); // Tighter spacing
        }
        
        tab.setLayoutParams(tabParams);
        tab.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(12), 
                       AndroidUtilities.dp(4), AndroidUtilities.dp(12));
        
        // Add letter spacing for more compact appearance
        tab.setLetterSpacing(-0.02f); // Even tighter letter spacing
        
        // No background - will be handled by underline
        tab.setBackground(null);
        
        tab.setOnClickListener(v -> selectTab(position));
        
        return tab;
    }
    
    public void selectTab(int position) {
        if (position < 0 || position >= tabViews.length || position == selectedTab) {
            return;
        }
        
        if (selectedTab >= 0 && selectedTab < tabViews.length) {
            updateTabAppearance(tabViews[selectedTab], false);
        }
        
        selectedTab = position;
        updateTabAppearance(tabViews[selectedTab], true);
        
        scrollToTab(position);
        
        if (listener != null) {
            listener.onTabSelected(position, tabTitles[position]);
        }
    }
    
    private void updateTabAppearance(TextView tab, boolean selected) {
        if (selected) {
            // Selected tab: blue text with blue underline
            tab.setTextColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, resourcesProvider));
            tab.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            
            // Create custom underline background
            UnderlineDrawable underlineDrawable = new UnderlineDrawable(
                Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, resourcesProvider));
            tab.setBackground(underlineDrawable);
        } else {
            // Unselected tab: gray text with no underline
            tab.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
            tab.setAlpha(0.75f); // Further reduced opacity
            tab.setTypeface(AndroidUtilities.getTypeface("fonts/rregular.ttf"));
            tab.setBackground(null);
        }
    }
    
    // Custom drawable for underline effect
    private static class UnderlineDrawable extends android.graphics.drawable.Drawable {
        private Paint paint;
        private int color;
        
        public UnderlineDrawable(int color) {
            this.color = color;
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(color);
        }
        
        @Override
        public void draw(android.graphics.Canvas canvas) {
            android.graphics.Rect bounds = getBounds();
            // Draw underline closer to text
            canvas.drawRect(bounds.left + AndroidUtilities.dp(4), bounds.bottom - AndroidUtilities.dp(2), 
                          bounds.right - AndroidUtilities.dp(4), bounds.bottom - AndroidUtilities.dp(0.5f), paint); // Consistent thickness
        }
        
        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }
        
        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }
        
        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }
    }
    
    private void scrollToTab(int position) {
        if (position < 0 || position >= tabViews.length) {
            return;
        }
        
        TextView tab = tabViews[position];
        int scrollX = tab.getLeft() - (getWidth() - tab.getWidth()) / 2;
        smoothScrollTo(Math.max(0, scrollX), 0);
    }
    
    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        this.listener = listener;
    }
    
    public int getSelectedTab() {
        return selectedTab;
    }
    
    public void updateTheme() {
        for (int i = 0; i < tabViews.length; i++) {
            updateTabAppearance(tabViews[i], i == selectedTab);
        }
    }
    
    public static String[] getDefaultUserTabs() {
        return new String[]{"Posts", "Media", "Files", "Links", "Voice", "Music"};
    }
    
    public static String[] getDefaultBotTabs() {
        return new String[]{"Info", "Media", "Links"};
    }
    
    public static String[] getDefaultGroupTabs() {
        return new String[]{"Media", "Files", "Links", "Voice", "Music"};
    }
    
    public static String[] getDefaultChannelTabs() {
        return new String[]{"Posts", "Media", "Files", "Links"};
    }
    
    public static String[] getDefaultBusinessTabs() {
        return new String[]{"Posts", "Media", "Files", "Links", "Voice", "Music"};
    }
}
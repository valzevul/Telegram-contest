package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.tgnet.TLRPC;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;

public class ProfileHeaderComponent extends FrameLayout {
    private BackupImageView avatarImageView;
    private BackgroundImageView backgroundView;
    private TextView nameView;
    private TextView statusView;
    private ImageView backButton;
    private ImageView menuButton;
    private LinearLayout actionsContainer;
    
    // For minimized state positioning
    private float minimizeProgress = 0f;
    
    // For scroll-based animations
    private float extraHeight = 0f;
    private float expandProgress = 0f;
    private float avatarScale = 1f;
    private float avatarX = 0f;
    private float avatarY = 0f;
    
    private Paint gradientPaint;
    private LinearGradient gradient;
    private int headerHeight = AndroidUtilities.dp(340); // Increased height to prevent cut-off
    private Theme.ResourcesProvider resourcesProvider;
    private OnMenuClickListener menuClickListener;
    private OnBackClickListener backClickListener;
    private OnActionClickListener actionClickListener;
    private OnAvatarClickListener onAvatarClickListener;
    private float scrollOffset = 0f;
    
    public interface OnMenuClickListener {
        void onMenuClick(View view);
    }
    
    public interface OnBackClickListener {
        void onBackClick();
    }
    
    public interface OnActionClickListener {
        void onActionClick(String action);
    }
    
    public interface OnAvatarClickListener {
        void onAvatarClick();
    }

    private static class BackgroundImageView extends View {
        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private LinearGradient gradient;
        private int startColor = 0xFF8AC0FF;
        private int endColor = 0xFF6FAAE8;
        
        public BackgroundImageView(Context context) {
            super(context);
            setWillNotDraw(false);
            // Initialize paint with start color for immediate drawing
            paint.setColor(startColor);
        }
        
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            // Create gradient as soon as we have dimensions
            int height = MeasureSpec.getSize(heightMeasureSpec);
            if (height > 0 && gradient == null) {
                gradient = new LinearGradient(0, 0, 0, height, startColor, endColor, Shader.TileMode.CLAMP);
                paint.setShader(gradient);
                invalidate();
            }
        }
        
        public void setGradient(int startColor, int endColor) {
            // Store colors for initial drawing
            this.startColor = startColor;
            this.endColor = endColor;
            if (getHeight() > 0) {
                gradient = new LinearGradient(0, 0, 0, getHeight(), startColor, endColor, Shader.TileMode.CLAMP);
                paint.setShader(gradient);
            }
            invalidate();
        }
        
        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            // Create exact gradient to match reference design
            gradient = new LinearGradient(0, 0, 0, h, startColor, endColor, Shader.TileMode.CLAMP);
            paint.setShader(gradient);
            invalidate();
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            // Always fill the entire view with color
            if (getWidth() > 0 && getHeight() > 0) {
                if (gradient == null) {
                    // Create gradient if not exists
                    gradient = new LinearGradient(0, 0, 0, getHeight(), startColor, endColor, Shader.TileMode.CLAMP);
                    paint.setShader(gradient);
                }
                // Draw with gradient
                canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            } else {
                // Fallback solid color if no dimensions yet
                paint.setShader(null);
                paint.setColor(startColor);
                canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            }
        }
        
        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            // Force an immediate draw when attached
            invalidate();
            requestLayout();
        }
    }

    public ProfileHeaderComponent(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        
        // Don't set a fixed height - let parent control it
        setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 
            FrameLayout.LayoutParams.MATCH_PARENT));
        
        // Ensure the view is opaque and will draw
        setWillNotDraw(false);
        
        
        // Set clipping based on state - disable to allow views to animate outside bounds
        setClipChildren(false);
        setClipToPadding(false);
        
        // Also disable outline clipping for API 21+
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            setClipToOutline(false);
        }
        
        createBackgroundView();
        createAvatarView();
        createTextViews();
        createActionsContainer();
        createBackButton();
        createMenuButton();
        
        // Force initial draw and layout
        requestLayout();
        invalidate();
        if (backgroundView != null) {
            backgroundView.requestLayout();
            backgroundView.invalidate();
        }
    }
    
    private void createBackgroundView() {
        backgroundView = new BackgroundImageView(getContext());
        backgroundView.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));
        
        backgroundView.setGradient(
            0xFF8AC0FF, // Even softer top blue (reduced saturation)
            0xFF6FAAE8  // Even softer bottom blue (reduced saturation)
        );
        
        // Ensure it's visible initially
        backgroundView.setAlpha(1f);
        backgroundView.setVisibility(View.VISIBLE);
        backgroundView.setWillNotDraw(false);
        
        addView(backgroundView, 0); // Add at bottom
        
        // Force immediate redraw
        backgroundView.invalidate();
    }
    
    private void createAvatarView() {
        avatarImageView = new BackupImageView(getContext());
        
        // Position avatar within header bounds, centered horizontally and below status bar
        FrameLayout.LayoutParams avatarParams = new FrameLayout.LayoutParams(
            AndroidUtilities.dp(104), AndroidUtilities.dp(104)); // Reduced by 16px
        avatarParams.gravity = Gravity.CENTER_HORIZONTAL;
        avatarParams.topMargin = AndroidUtilities.dp(70); // Moved up to make room for buttons
        
        avatarImageView.setLayoutParams(avatarParams);
        avatarImageView.getImageReceiver().setRoundRadius(AndroidUtilities.dp(52));
        
        // Create white ring around avatar to match wireframes
        GradientDrawable avatarBackground = new GradientDrawable();
        avatarBackground.setShape(GradientDrawable.OVAL);
        avatarBackground.setColor(0x00000000); // Transparent center
        avatarBackground.setStroke(AndroidUtilities.dp(4), 
            Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
        
        avatarImageView.setBackground(avatarBackground);
        avatarImageView.setElevation(AndroidUtilities.dp(8));
        
        // Add click listener to avatar for expansion
        avatarImageView.setOnClickListener(v -> {
            if (onAvatarClickListener != null) {
                onAvatarClickListener.onAvatarClick();
            }
        });
        
        addView(avatarImageView);
    }
    
    private void createTextViews() {
        // Name text view - positioned below avatar
        nameView = new TextView(getContext());
        nameView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 28);
        nameView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"), Typeface.BOLD);
        nameView.setTextColor(0xFFFFFFFF); // White text
        nameView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL); // Left align from start
        nameView.setMaxLines(2);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        nameView.setShadowLayer(3, 0, 2, 0x99000000); // Stronger shadow for readability
        nameView.setElevation(AndroidUtilities.dp(2)); // Ensure it's above background
        
        FrameLayout.LayoutParams nameParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT);
        nameParams.gravity = Gravity.LEFT | Gravity.TOP;
        nameParams.topMargin = AndroidUtilities.dp(170); // Moved up with avatar
        nameParams.leftMargin = AndroidUtilities.dp(16);
        nameParams.rightMargin = AndroidUtilities.dp(16);
        nameView.setLayoutParams(nameParams);
        addView(nameView);
        
        // Status text view - positioned below name
        statusView = new TextView(getContext());
        statusView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 12);
        statusView.setTextColor(0xB3FFFFFF); // Slightly more visible for readability
        statusView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL); // Left align from start
        statusView.setMaxLines(1);
        statusView.setShadowLayer(2, 0, 1, 0x99000000); // Stronger shadow for readability
        statusView.setElevation(AndroidUtilities.dp(2)); // Ensure it's above background
        
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT);
        statusParams.gravity = Gravity.LEFT | Gravity.TOP;
        statusParams.topMargin = AndroidUtilities.dp(203); // 8px gap after name (170 + 25 text + 8)
        statusParams.leftMargin = AndroidUtilities.dp(16);
        statusParams.rightMargin = AndroidUtilities.dp(16);
        statusView.setLayoutParams(statusParams);
        addView(statusView);
    }
    
    private void createActionsContainer() {
        actionsContainer = new LinearLayout(getContext());
        actionsContainer.setOrientation(LinearLayout.HORIZONTAL);
        actionsContainer.setGravity(Gravity.LEFT); // Left align buttons
        
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT);
        containerParams.gravity = Gravity.TOP | Gravity.LEFT;
        containerParams.topMargin = AndroidUtilities.dp(227); // Lifted up by 8px for better spacing
        containerParams.leftMargin = AndroidUtilities.dp(16); // Consistent padding
        containerParams.rightMargin = AndroidUtilities.dp(16);
        actionsContainer.setLayoutParams(containerParams);
        actionsContainer.setVisibility(View.VISIBLE); // Ensure visibility
        
        // Create default user action buttons (can be overridden)
        setupDefaultUserActions();
        
        addView(actionsContainer);
        
        android.util.Log.d("ProfileHeader", "Created actions container with " + actionsContainer.getChildCount() + " buttons");
    }
    
    
    private void setupDefaultUserActions() {
        // Create all buttons in single horizontal row using correct icons
        createActionButton(actionsContainer, "Message", R.drawable.msg_send, true);
        createActionButton(actionsContainer, "Mute", R.drawable.msg_mute, false);
        createActionButton(actionsContainer, "Call", R.drawable.msg_callback, false);
        createActionButton(actionsContainer, "Video", R.drawable.msg_videocall, false);
    }
    
    public void setCustomActions(String[] actions, int[] icons, boolean[] isPrimary) {
        // Clear existing actions
        actionsContainer.removeAllViews();
        
        // Create custom actions
        for (int i = 0; i < actions.length && i < icons.length; i++) {
            boolean primary = isPrimary != null && i < isPrimary.length ? isPrimary[i] : false;
            createActionButton(actionsContainer, actions[i], icons[i], primary);
        }
    }
    
    private void createBackButton() {
        backButton = new ImageView(getContext());
        backButton.setImageResource(R.drawable.ic_ab_back);
        backButton.setColorFilter(0xFFFFFFFF); // White color
        backButton.setScaleType(ImageView.ScaleType.CENTER);
        backButton.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), 
                             AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        
        // Position below status bar
        FrameLayout.LayoutParams backParams = new FrameLayout.LayoutParams(
            AndroidUtilities.dp(48), AndroidUtilities.dp(48));
        backParams.gravity = Gravity.TOP | Gravity.LEFT;
        backParams.topMargin = AndroidUtilities.dp(40); // Add space for status bar
        backParams.leftMargin = AndroidUtilities.dp(8); // Closer to edge in expanded state
        backButton.setLayoutParams(backParams);
        
        backButton.setOnClickListener(v -> {
            if (backClickListener != null) {
                backClickListener.onBackClick();
            }
        });
        
        addView(backButton);
    }
    
    private void createActionButton(LinearLayout parent, String action, int iconRes, boolean isPrimary) {
        LinearLayout buttonContainer = new LinearLayout(getContext());
        buttonContainer.setOrientation(LinearLayout.VERTICAL);
        buttonContainer.setGravity(Gravity.CENTER);
        buttonContainer.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(12), 
                                  AndroidUtilities.dp(10), AndroidUtilities.dp(12));
        
        // iOS-style button dimensions
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            0, AndroidUtilities.dp(80), 1f); // Larger height for iOS style
        buttonParams.leftMargin = AndroidUtilities.dp(6);
        buttonParams.rightMargin = AndroidUtilities.dp(6);
        buttonContainer.setLayoutParams(buttonParams);
        
        // Create rounded rectangle background with iOS-style corner radius
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(AndroidUtilities.dp(20)); // iOS-style rounded corners
        // Semi-transparent white background
        background.setColor(0x2DFFFFFF); // Slightly less opacity for cleaner look
        buttonContainer.setBackground(background);
        
        // Create icon
        ImageView icon = new ImageView(getContext());
        try {
            icon.setImageResource(iconRes);
        } catch (Exception e) {
            icon.setImageResource(R.drawable.msg_info); // Fallback
        }
        
        icon.setColorFilter(0xFFFFFFFF); // White icons
        icon.setScaleType(ImageView.ScaleType.CENTER);
        
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
            AndroidUtilities.dp(24), AndroidUtilities.dp(24)); // Larger icon for iOS style
        iconParams.bottomMargin = AndroidUtilities.dp(6);
        icon.setLayoutParams(iconParams);
        buttonContainer.addView(icon);
        
        // Create label
        TextView label = new TextView(getContext());
        label.setText(action);
        label.setTextSize(11); // Larger text for better readability
        label.setTextColor(0xFFFFFFFF);
        label.setGravity(Gravity.CENTER);
        label.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        label.setSingleLine(true);
        
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        label.setLayoutParams(labelParams);
        buttonContainer.addView(label);
        
        buttonContainer.setOnClickListener(v -> {
            if (actionClickListener != null) {
                actionClickListener.onActionClick(action);
            }
        });
        
        parent.addView(buttonContainer);
    }
    
    private void createMenuButton() {
        menuButton = new ImageView(getContext());
        try {
            menuButton.setImageResource(R.drawable.ic_ab_other);
        } catch (Exception e) {
            // Fallback to a simpler drawable
            menuButton.setImageResource(R.drawable.msg_info);
        }
        menuButton.setColorFilter(0xFFFFFFFF); // White color
        menuButton.setScaleType(ImageView.ScaleType.CENTER);
        menuButton.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), 
                             AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        
        // Make sure it's visible
        menuButton.setVisibility(View.VISIBLE);
        
        FrameLayout.LayoutParams menuParams = new FrameLayout.LayoutParams(
            AndroidUtilities.dp(48), AndroidUtilities.dp(48));
        menuParams.gravity = Gravity.TOP | Gravity.RIGHT;
        menuParams.topMargin = AndroidUtilities.dp(40); // Add space for status bar
        menuParams.rightMargin = AndroidUtilities.dp(12);
        menuButton.setLayoutParams(menuParams);
        
        menuButton.setOnClickListener(v -> {
            if (menuClickListener != null) {
                menuClickListener.onMenuClick(v);
            }
        });
        
        addView(menuButton);
    }
    
    
    public void setOnMenuClickListener(OnMenuClickListener listener) {
        this.menuClickListener = listener;
    }
    
    public void setOnBackClickListener(OnBackClickListener listener) {
        this.backClickListener = listener;
    }
    
    public void setOnActionClickListener(OnActionClickListener listener) {
        this.actionClickListener = listener;
    }
    
    public void setOnAvatarClickListener(OnAvatarClickListener listener) {
        this.onAvatarClickListener = listener;
    }
    
    public void setName(String name) {
        if (nameView != null && !TextUtils.isEmpty(name)) {
            nameView.setText(name);
            nameView.setVisibility(View.VISIBLE);
        } else if (nameView != null) {
            nameView.setVisibility(View.GONE);
        }
    }
    
    public void setStatus(String status) {
        if (statusView != null && !TextUtils.isEmpty(status)) {
            statusView.setText(status);
            statusView.setVisibility(View.VISIBLE);
        } else if (statusView != null) {
            statusView.setVisibility(View.GONE);
        }
    }
    
    public void setUser(TLRPC.User user) {
        if (user != null) {
            AvatarDrawable avatarDrawable = new AvatarDrawable(user, true);
            avatarDrawable.setScaleSize(1.0f);
            
            if (user.photo != null && user.photo.photo_big != null) {
                ImageLocation imageLocation = ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_BIG);
                avatarImageView.setImage(imageLocation, "104_104", avatarDrawable, user);
            } else {
                avatarImageView.setImageDrawable(avatarDrawable);
            }
        }
    }
    
    public void setChat(TLRPC.Chat chat) {
        if (chat != null) {
            AvatarDrawable avatarDrawable = new AvatarDrawable(chat, true);
            avatarDrawable.setScaleSize(1.0f);
            
            if (chat.photo != null && chat.photo.photo_big != null) {
                ImageLocation imageLocation = ImageLocation.getForUserOrChat(chat, ImageLocation.TYPE_BIG);
                avatarImageView.setImage(imageLocation, "104_104", avatarDrawable, chat);
            } else {
                avatarImageView.setImageDrawable(avatarDrawable);
            }
        }
    }
    
    public void setGradientColors(int startColor, int endColor) {
        backgroundView.setGradient(startColor, endColor);
    }
    
    public void updateTheme() {
        backgroundView.setGradient(
            0xFF8AC0FF, // Even softer top blue (reduced saturation)
            0xFF6FAAE8  // Even softer bottom blue (reduced saturation)
        );
        
        GradientDrawable avatarBackground = (GradientDrawable) avatarImageView.getBackground();
        if (avatarBackground != null) {
            avatarBackground.setStroke(AndroidUtilities.dp(4), 
                0xE6FFFFFF); // Slightly transparent white stroke
        }
        
        // Text colors remain white as they're always on gradient background
        if (nameView != null) {
            nameView.setTextColor(0xFFFFFFFF);
        }
        if (statusView != null) {
            statusView.setTextColor(0xCCFFFFFF);
        }
        if (menuButton != null) {
            menuButton.setColorFilter(0xFFFFFFFF);
        }
    }
    
    public BackupImageView getAvatarImageView() {
        return avatarImageView;
    }
    
    public float getMinimizeProgress() {
        return minimizeProgress;
    }
    
    /**
     * Force update minimized state - useful for debugging
     * @param progress 0.0 for normal, 1.0 for fully minimized
     */
    public void forceMinimizeProgress(float progress) {
        setMinimizeProgress(progress);
    }
    
    public float getExpandProgress() {
        return expandProgress;
    }
    
    public void forceBackgroundRedraw() {
        if (backgroundView != null) {
            backgroundView.invalidate();
        }
    }
    
    /**
     * Update scroll offset for avatar transformation
     * @param offset Scroll offset in pixels (negative when scrolling up)
     */
    public void setScrollOffset(float offset) {
        this.scrollOffset = offset;
        
        // Calculate expansion based on scroll offset
        // Start expanding when scrolled up more than 50dp
        float expandThreshold = AndroidUtilities.dp(50);
        float expandRange = AndroidUtilities.dp(200);
        
        float expandProgress = 0f;
        if (offset < -expandThreshold) {
            expandProgress = Math.min(1f, (-offset - expandThreshold) / expandRange);
        }
        
        // Apply transformation
        applyAvatarTransformation(expandProgress);
    }
    
    private void applyAvatarTransformation(float progress) {
        // Simplified - just store progress for now
        // Actual gallery implementation would be done differently
    }
    
    /**
     * Set the alpha of the gradient background
     * @param alpha 0.0 for transparent, 1.0 for opaque
     */
    public void setGradientAlpha(float alpha) {
        if (backgroundView != null) {
            backgroundView.setAlpha(alpha);
        }
    }
    
    /**
     * Set the alpha of the avatar
     * @param alpha 0.0 for hidden, 1.0 for visible
     */
    public void setAvatarAlpha(float alpha) {
        if (avatarImageView != null) {
            avatarImageView.setAlpha(alpha);
        }
    }
    
    /**
     * Set the alpha of content (name, status, buttons)
     * @param alpha 0.0 for hidden, 1.0 for visible
     */
    public void setContentAlpha(float alpha) {
        if (nameView != null) nameView.setAlpha(alpha);
        if (statusView != null) statusView.setAlpha(alpha);
        if (actionsContainer != null) actionsContainer.setAlpha(alpha);
    }
    
    /**
     * Set content expansion state with button translation
     * @param progress 0.0 for collapsed, 1.0 for expanded
     */
    public void setContentExpansion(float progress) {
        if (actionsContainer != null) {
            if (progress > 0) {
                // Calculate translation to align with name/status at left edge
                float currentLeft = actionsContainer.getLeft();
                float targetX = AndroidUtilities.dp(8) - currentLeft; // Align with back button margin
                
                // Move down but keep within bounds - reduce movement to prevent cutoff
                float deltaY = AndroidUtilities.dp(80) * progress;
                
                actionsContainer.setTranslationX(targetX * progress);
                actionsContainer.setTranslationY(deltaY);
                
                // Don't scale - keep buttons at full size
                actionsContainer.setScaleX(1f);
                actionsContainer.setScaleY(1f);
                
                // Always keep visible
                actionsContainer.setAlpha(1f);
                actionsContainer.setVisibility(View.VISIBLE);
            } else {
                // Reset to default position when not expanding
                actionsContainer.setTranslationX(0);
                actionsContainer.setTranslationY(0);
                actionsContainer.setScaleX(1f);
                actionsContainer.setScaleY(1f);
                actionsContainer.setVisibility(View.VISIBLE);
                actionsContainer.setAlpha(1f); // Always keep visible
            }
        }
    }
    
    /**
     * Translate content (name, status) for expansion animation
     * @param x horizontal translation
     * @param y vertical translation
     */
    public void setContentTranslation(float x, float y) {
        if (nameView != null) {
            nameView.setTranslationX(x);
            nameView.setTranslationY(y);
        }
        if (statusView != null) {
            statusView.setTranslationX(x);
            statusView.setTranslationY(y + AndroidUtilities.dp(10)); // Keep status below name in expanded state
        }
        // Don't translate action buttons - they stay at bottom
    }
    
    /**
     * Update header based on scroll offset (similar to ProfileActivity)
     * @param scrollOffset Current scroll offset in pixels
     */
    public void updateScrollOffset(float scrollOffset) {
        // Store previous states for comparison
        float prevMinimizeProgress = minimizeProgress;
        float prevExpandProgress = expandProgress;
        
        // Calculate extra height based on scroll (negative offset means overscroll)
        extraHeight = Math.max(0, -scrollOffset);
        
        // Calculate expand progress (0 to 1)
        if (extraHeight > AndroidUtilities.dp(88)) {
            expandProgress = Math.min(1f, (extraHeight - AndroidUtilities.dp(88)) / AndroidUtilities.dp(200));
        } else {
            expandProgress = 0f;
        }
        
        // Calculate minimize progress based on positive scroll
        if (scrollOffset > 0) {
            // More responsive minimization - start earlier and complete faster
            minimizeProgress = Math.min(1f, scrollOffset / AndroidUtilities.dp(80));
        } else {
            minimizeProgress = 0f;
        }
        
        // Update avatar scale and position only when expanding
        if (expandProgress > 0) {
            final float diff = Math.min(1f, extraHeight / AndroidUtilities.dp(88f));
            avatarScale = AndroidUtilities.lerp(1f, 1.5f, diff);
            avatarX = -AndroidUtilities.dp(47) * diff;
            avatarY = AndroidUtilities.dp(20) * diff;
            
            if (avatarImageView != null) {
                avatarImageView.setScaleX(avatarScale);
                avatarImageView.setScaleY(avatarScale);
                avatarImageView.setTranslationX(avatarX);
                avatarImageView.setTranslationY(avatarY);
            }
        }
        
        // Update other elements
        setMinimizeProgress(minimizeProgress);
        setContentExpansion(expandProgress);
        
        // Check if we've returned to default state
        boolean isDefaultState = minimizeProgress < 0.01f && expandProgress < 0.01f;
        boolean wasInTransition = (prevMinimizeProgress > 0.01f || prevExpandProgress > 0.01f);
        
        if (isDefaultState && wasInTransition) {
            // Full state reset when returning to default
            android.util.Log.d("ProfileHeader", "Resetting to default state");
            resetToDefaultState();
        }
        
        invalidate();
    }
    
    private void resetToDefaultState() {
        // Reset action buttons
        actionButtonsHidden = false;
        if (actionsContainer != null) {
            actionsContainer.setVisibility(View.VISIBLE);
            actionsContainer.setAlpha(1f);
            actionsContainer.setTranslationX(0);
            actionsContainer.setTranslationY(0);
            actionsContainer.setScaleX(1f);
            actionsContainer.setScaleY(1f);
        }
        
        // Reset avatar
        if (avatarImageView != null) {
            avatarImageView.setScaleX(1f);
            avatarImageView.setScaleY(1f);
            avatarImageView.setTranslationX(0);
            avatarImageView.setTranslationY(0);
            avatarImageView.setAlpha(1f);
        }
        
        // Reset name/status
        if (nameView != null) {
            nameView.setTranslationX(0);
            nameView.setTranslationY(0);
            nameView.setScaleX(1f);
            nameView.setScaleY(1f);
            nameView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 28);
            nameView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            nameView.setMaxLines(2);
            nameView.setAlpha(1f);
            nameView.setVisibility(View.VISIBLE);
        }
        if (statusView != null) {
            statusView.setTranslationX(0);
            statusView.setTranslationY(0);
            statusView.setScaleX(1f);
            statusView.setScaleY(1f);
            statusView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 12);
            statusView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            statusView.setAlpha(1f);
            statusView.setVisibility(View.VISIBLE);
        }
        
        // Reset back/menu buttons
        if (backButton != null) {
            backButton.setTranslationY(0);
            backButton.setAlpha(1f);
        }
        if (menuButton != null) {
            menuButton.setTranslationY(0);
            menuButton.setAlpha(1f);
        }
        
        // Force a layout pass
        requestLayout();
    }
    
    private void ensureActionButtonsVisible() {
        // This method is now redundant - use resetToDefaultState() instead
        resetToDefaultState();
    }
    
    private boolean isUpdatingMinimizeProgress = false;
    
    // Hysteresis mechanism for action button visibility
    private boolean actionButtonsHidden = false;
    private static final float HIDE_THRESHOLD = 0.15f;
    private static final float SHOW_THRESHOLD = 0.05f;
    
    // Track last update time for debouncing
    private long lastProgressUpdateTime = 0;
    private static final long DEBOUNCE_DELAY = 16; // ~60fps
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Ensure action buttons are visible when view is attached
        if (minimizeProgress == 0f && expandProgress == 0f) {
            postDelayed(() -> {
                if (actionsContainer != null) {
                    android.util.Log.d("ProfileHeader", "onAttachedToWindow: Ensuring action buttons visible");
                    ensureActionButtonsVisible();
                }
            }, 200);
        }
    }
    
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        
        // Force a minimize progress update if we're in a minimized state
        // This ensures views are positioned correctly after layout
        // Add guard against recursive updates
        if (minimizeProgress > 0 && !isUpdatingMinimizeProgress) {
            isUpdatingMinimizeProgress = true;
            post(() -> {
                setMinimizeProgress(minimizeProgress);
                isUpdatingMinimizeProgress = false;
            });
        }
        
        // Ensure action buttons are visible in default state
        if (minimizeProgress == 0f && expandProgress == 0f && actionsContainer != null) {
            if (actionsContainer.getVisibility() != View.VISIBLE || actionsContainer.getAlpha() < 1f) {
                android.util.Log.d("ProfileHeader", "onLayout: Action buttons not visible, fixing...");
                ensureActionButtonsVisible();
            }
        }
    }
    
    
    /**
     * Set minimize progress for transitioning to navigation bar
     * @param progress 0.0 for normal, 1.0 for fully minimized
     */
    private void setMinimizeProgress(float progress) {
        // Debounce rapid updates
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastProgressUpdateTime < DEBOUNCE_DELAY && Math.abs(minimizeProgress - progress) < 0.01f) {
            return;
        }
        lastProgressUpdateTime = currentTime;
        
        minimizeProgress = progress;
        
        // Debug log
        android.util.Log.d("ProfileHeader", "setMinimizeProgress called with progress: " + progress + ", expandProgress: " + expandProgress);
        
        // Smoothly fade out avatar and action buttons
        float fadeOutAlpha = Math.max(0f, 1f - progress);
        if (avatarImageView != null) {
            avatarImageView.setAlpha(fadeOutAlpha);
            // Scale avatar down during minimize
            float avatarMinScale = 1f - progress * 0.3f;
            avatarImageView.setScaleX(avatarMinScale);
            avatarImageView.setScaleY(avatarMinScale);
            
            // Reset translation when returning to default (progress = 0)
            if (progress == 0f) {
                avatarImageView.setTranslationX(0);
                avatarImageView.setTranslationY(0);
            }
        }
        
        if (actionsContainer != null) {
            // Simple fade based on minimize progress
            float alpha = Math.max(0f, 1f - progress);
            actionsContainer.setAlpha(alpha);
            
            // Always keep visible, let alpha handle the appearance
            actionsContainer.setVisibility(View.VISIBLE);
            
            // Reset position if returning to default
            if (progress < 0.05f && expandProgress < 0.05f) {
                actionsContainer.setTranslationX(0);
                actionsContainer.setTranslationY(0);
                actionsContainer.setScaleX(1f);
                actionsContainer.setScaleY(1f);
            }
            
            actionButtonsHidden = progress > 0.9f;
            android.util.Log.d("ProfileHeader", "Action buttons - progress: " + progress + ", alpha: " + alpha);
        }
        
        // Keep navigation buttons visible and adjust their position
        if (backButton != null) {
            backButton.setAlpha(1f);
            // In minimized state, center the back button vertically
            if (progress > 0.5f) {
                // Calculate new position for minimized state
                // Total height is 100dp, center button vertically
                float targetTopMargin = (AndroidUtilities.dp(100) - AndroidUtilities.dp(48)) / 2f; // Center in 100dp
                float originalTopMargin = AndroidUtilities.dp(40);
                float deltaY = targetTopMargin - originalTopMargin;
                backButton.setTranslationY(deltaY * progress);
            } else {
                backButton.setTranslationY(0);
            }
        }
        if (menuButton != null) {
            menuButton.setAlpha(1f);
            // Also adjust menu button position
            if (progress > 0.5f) {
                float targetTopMargin = (AndroidUtilities.dp(100) - AndroidUtilities.dp(48)) / 2f;
                float originalTopMargin = AndroidUtilities.dp(40);
                float deltaY = targetTopMargin - originalTopMargin;
                menuButton.setTranslationY(deltaY * progress);
            } else {
                menuButton.setTranslationY(0);
            }
        }
        
        // Animate name and status to navbar position
        if (nameView != null && statusView != null) {
            // Skip if views not ready - don't force measurement during animation
            if (nameView.getWidth() == 0 || statusView.getWidth() == 0) {
                return;
            }
            
            // Calculate horizontal movement
            // Back button is at left margin 12dp and is 48dp wide, so text starts at 60dp + some padding
            float targetX = AndroidUtilities.dp(68); // Right after back button with small padding
            float currentX = nameView.getLeft(); // Current left position
            float deltaX = targetX - currentX;
            
            // Apply horizontal translation - move to absolute position
            nameView.setTranslationX(deltaX * progress);
            statusView.setTranslationX(deltaX * progress);
            
            // Calculate vertical positions
            // The parent view (PullToExpandHeaderView) handles the height animation
            // We need to position text within the current measured bounds
            
            // Get the current measured height of the parent
            float currentHeight = getMeasuredHeight();
            if (currentHeight == 0) {
                // Not measured yet, skip
                return;
            }
            
            // In minimized state, properly center align content vertically
            // Total navbar height is 100dp
            float navbarHeight = AndroidUtilities.dp(100);
            float contentHeight = AndroidUtilities.dp(40); // Approximate height of name + status
            float navbarCenterY = navbarHeight / 2f;
            float nameTargetY = navbarCenterY - AndroidUtilities.dp(12); // Name above center
            float statusTargetY = navbarCenterY + AndroidUtilities.dp(8); // Status below center
            
            // Get current positions
            float nameCurrentY = nameView.getTop() + nameView.getTranslationY();
            float statusCurrentY = statusView.getTop() + statusView.getTranslationY();
            
            // For minimized state, we need absolute positioning
            if (progress > 0.9f) {
                // Fully minimized - use absolute positions
                nameView.setTranslationY(nameTargetY - nameView.getTop());
                statusView.setTranslationY(statusTargetY - statusView.getTop());
            } else {
                // Animating - interpolate
                float nameOriginalY = nameView.getTop();
                float statusOriginalY = statusView.getTop();
                
                float nameY = nameOriginalY + (nameTargetY - nameOriginalY) * progress;
                float statusY = statusOriginalY + (statusTargetY - statusOriginalY) * progress;
                
                nameView.setTranslationY(nameY - nameView.getTop());
                statusView.setTranslationY(statusY - statusView.getTop());
            }
            
            // Scale adjustments - don't scale, just change text size
            // Keep scale at 1 to maintain proper alignment
            nameView.setScaleX(1f);
            nameView.setScaleY(1f);
            nameView.setPivotX(0);
            nameView.setPivotY(nameView.getHeight() / 2f);
            
            statusView.setScaleX(1f);
            statusView.setScaleY(1f);
            statusView.setPivotX(0);
            statusView.setPivotY(statusView.getHeight() / 2f);
            
            // Ensure visibility
            nameView.setAlpha(1f);
            statusView.setAlpha(1f);
            nameView.setVisibility(View.VISIBLE);
            statusView.setVisibility(View.VISIBLE);
            
            // Text properties based on state
            if (progress > 0.8f) {
                // Minimized state - use standard navbar text sizes
                nameView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 16);
                statusView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 13);
                nameView.setMaxLines(1);
                statusView.setMaxLines(1);
                nameView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
                statusView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
                // Set proper pivot for left alignment
                nameView.setPivotX(0);
                statusView.setPivotX(0);
                nameView.setEllipsize(android.text.TextUtils.TruncateAt.END);
                statusView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            } else if (progress < 0.2f) {
                // Normal state
                nameView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 28);
                statusView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 12);
                nameView.setMaxLines(2);
                statusView.setMaxLines(1);
                nameView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
                statusView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            }
            
            // Force proper z-ordering
            if (progress > 0) {
                backgroundView.bringToFront();
                backButton.bringToFront();
                nameView.bringToFront();
                statusView.bringToFront();
                menuButton.bringToFront();
            }
            
            // Remove bounds checking that might interfere with visibility
            // The increased header height should provide enough space
            
            // Debug logging
            if (progress > 0.9f) {
                android.util.Log.d("ProfileHeader", "=== Minimized State Debug ===");
                android.util.Log.d("ProfileHeader", "Progress: " + progress);
                android.util.Log.d("ProfileHeader", "Name target Y: " + nameTargetY);
                android.util.Log.d("ProfileHeader", "Status target Y: " + statusTargetY);
                android.util.Log.d("ProfileHeader", "Name TransY: " + nameView.getTranslationY());
                android.util.Log.d("ProfileHeader", "Status TransY: " + statusView.getTranslationY());
                android.util.Log.d("ProfileHeader", "Final Name Y: " + (nameView.getTop() + nameView.getTranslationY()));
                android.util.Log.d("ProfileHeader", "Final Status Y: " + (statusView.getTop() + statusView.getTranslationY()));
                android.util.Log.d("ProfileHeader", "Header height: " + getHeight());
                android.util.Log.d("ProfileHeader", "Header measured height: " + getMeasuredHeight());
                android.util.Log.d("ProfileHeader", "Name visibility: " + nameView.getVisibility() + ", alpha: " + nameView.getAlpha());
                android.util.Log.d("ProfileHeader", "Name text: " + nameView.getText());
            }
        }
        
        // Keep background visible
        if (backgroundView != null) {
            backgroundView.setAlpha(1f);
        }
        
        invalidate();
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Measure with our desired height
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        
        // Use our header height unless constrained
        int desiredHeight = headerHeight;
        int finalHeight;
        
        if (heightMode == MeasureSpec.EXACTLY) {
            finalHeight = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            finalHeight = Math.min(desiredHeight, heightSize);
        } else {
            finalHeight = desiredHeight;
        }
        
        // Log the measured dimensions
        android.util.Log.d("ProfileHeader", "onMeasure - desired: " + desiredHeight + 
            ", final: " + finalHeight + ", mode: " + heightMode + ", minimizeProgress: " + minimizeProgress);
        
        setMeasuredDimension(width, finalHeight);
        
        // Measure children with the final dimensions
        measureChildren(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY)
        );
        
        // Adjust layout based on minimize progress
        if (minimizeProgress > 0.5f) {
            // In minimized state, ensure views are positioned correctly
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child == nameView || child == statusView) {
                    // Measure text views with WRAP_CONTENT
                    child.measure(
                        MeasureSpec.makeMeasureSpec(width - AndroidUtilities.dp(144), MeasureSpec.AT_MOST),
                        MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.AT_MOST)
                    );
                }
            }
        }
    }
    
    /**
     * Align content to left for expanded state
     * @param progress 0.0 for center aligned, 1.0 for left aligned
     */
    public void setContentAlignment(float progress) {
        // Wait for layout before calculating positions
        if (nameView == null || nameView.getWidth() == 0) {
            post(() -> setContentAlignment(progress));
            return;
        }
        
        // Calculate positions
        int screenWidth = getWidth();
        float leftMargin = AndroidUtilities.dp(20);
        
        // For name
        if (nameView != null) {
            float centerX = (screenWidth - nameView.getWidth()) / 2f;
            float deltaX = leftMargin - centerX;
            float currentTranslationX = deltaX * progress;
            nameView.setTranslationX(currentTranslationX);
        }
        
        // For status
        if (statusView != null) {
            float centerX = (screenWidth - statusView.getWidth()) / 2f;
            float deltaX = leftMargin - centerX;
            float currentTranslationX = deltaX * progress;
            statusView.setTranslationX(currentTranslationX);
        }
        
        // Action buttons stay centered - don't translate them
    }
    
    /**
     * Set minimize progress for transitioning to navigation bar
     * @param progress 0.0 for normal, 1.0 for fully minimized
     */
}
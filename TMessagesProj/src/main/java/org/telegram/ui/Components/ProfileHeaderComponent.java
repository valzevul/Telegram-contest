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
    private int headerHeight = AndroidUtilities.dp(320); // Increased height for better spacing
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
        
        
        // Set clipping based on state
        setClipChildren(true);
        setClipToPadding(true);
        
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
        avatarParams.topMargin = AndroidUtilities.dp(80); // Position avatar below status bar and buttons
        
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
        nameView.setTextSize(28);
        nameView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"), Typeface.BOLD);
        nameView.setTextColor(0xFFFFFFFF); // White text
        nameView.setGravity(Gravity.LEFT); // Left align for expanded state
        nameView.setMaxLines(2);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        nameView.setShadowLayer(3, 0, 2, 0x99000000); // Stronger shadow for readability
        nameView.setElevation(AndroidUtilities.dp(2)); // Ensure it's above background
        
        FrameLayout.LayoutParams nameParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT);
        nameParams.gravity = Gravity.CENTER_HORIZONTAL;
        nameParams.topMargin = AndroidUtilities.dp(190); // Adjusted for new avatar position
        nameParams.leftMargin = AndroidUtilities.dp(16);
        nameParams.rightMargin = AndroidUtilities.dp(16);
        nameView.setLayoutParams(nameParams);
        addView(nameView);
        
        // Status text view - positioned below name
        statusView = new TextView(getContext());
        statusView.setTextSize(12); // Smaller for better hierarchy
        statusView.setTextColor(0xB3FFFFFF); // Slightly more visible for readability
        statusView.setGravity(Gravity.LEFT); // Left align for expanded state
        statusView.setMaxLines(1);
        statusView.setShadowLayer(2, 0, 1, 0x99000000); // Stronger shadow for readability
        statusView.setElevation(AndroidUtilities.dp(2)); // Ensure it's above background
        
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT);
        statusParams.gravity = Gravity.CENTER_HORIZONTAL;
        statusParams.topMargin = AndroidUtilities.dp(235); // Further increased spacing
        statusView.setLayoutParams(statusParams);
        addView(statusView);
    }
    
    private void createActionsContainer() {
        actionsContainer = new LinearLayout(getContext());
        actionsContainer.setOrientation(LinearLayout.HORIZONTAL);
        actionsContainer.setGravity(Gravity.CENTER);
        
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT);
        containerParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        containerParams.topMargin = AndroidUtilities.dp(270); // Fixed position
        containerParams.leftMargin = AndroidUtilities.dp(16); // Consistent padding
        containerParams.rightMargin = AndroidUtilities.dp(16);
        actionsContainer.setLayoutParams(containerParams);
        actionsContainer.setVisibility(View.VISIBLE); // Ensure visibility
        
        // Create default user action buttons (can be overridden)
        setupDefaultUserActions();
        
        addView(actionsContainer);
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
        backParams.leftMargin = AndroidUtilities.dp(12);
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
            // Keep buttons visible and translate them to bottom-left when expanded
            actionsContainer.setAlpha(1f); // Always visible
            
            if (progress > 0) {
                // Calculate translation to move buttons to bottom-left corner
                float centerX = (getWidth() - actionsContainer.getWidth()) / 2f;
                float targetX = AndroidUtilities.dp(20); // Left margin in expanded state
                float deltaX = targetX - centerX;
                
                // Move down slightly
                float deltaY = AndroidUtilities.dp(100) * progress;
                
                actionsContainer.setTranslationX(deltaX * progress);
                actionsContainer.setTranslationY(deltaY);
                
                // Scale down slightly
                float scale = 1f - (progress * 0.1f); // Scale to 90% when fully expanded
                actionsContainer.setScaleX(scale);
                actionsContainer.setScaleY(scale);
            } else {
                // Reset to default position
                actionsContainer.setTranslationX(0);
                actionsContainer.setTranslationY(0);
                actionsContainer.setScaleX(1f);
                actionsContainer.setScaleY(1f);
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
            statusView.setTranslationY(y);
        }
        // Don't translate action buttons - they stay at bottom
    }
    
    /**
     * Update header based on scroll offset (similar to ProfileActivity)
     * @param scrollOffset Current scroll offset in pixels
     */
    public void updateScrollOffset(float scrollOffset) {
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
            minimizeProgress = Math.min(1f, scrollOffset / AndroidUtilities.dp(100));
        } else {
            minimizeProgress = 0f;
        }
        
        // Update avatar scale and position
        final float diff = Math.min(1f, extraHeight / AndroidUtilities.dp(88f));
        avatarScale = AndroidUtilities.lerp(1f, 1.5f, diff);
        avatarX = -AndroidUtilities.dp(47) * diff;
        avatarY = AndroidUtilities.dp(20) * diff;
        
        // Apply transformations
        if (avatarImageView != null) {
            avatarImageView.setScaleX(avatarScale);
            avatarImageView.setScaleY(avatarScale);
            avatarImageView.setTranslationX(avatarX);
            avatarImageView.setTranslationY(avatarY);
        }
        
        // Update other elements
        setMinimizeProgress(minimizeProgress);
        setContentExpansion(expandProgress);
        
        invalidate();
    }
    
    /**
     * Set minimize progress for transitioning to navigation bar
     * @param progress 0.0 for normal, 1.0 for fully minimized
     */
    private void setMinimizeProgress(float progress) {
        minimizeProgress = progress;
        
        // Smoothly fade out avatar and action buttons
        float fadeOutAlpha = Math.max(0f, 1f - progress);
        if (avatarImageView != null) {
            avatarImageView.setAlpha(fadeOutAlpha);
            avatarImageView.setScaleX(1f - progress * 0.3f);
            avatarImageView.setScaleY(1f - progress * 0.3f);
        }
        if (actionsContainer != null) {
            // Keep action buttons visible in minimized state too
            actionsContainer.setAlpha(1f);
        }
        
        // Keep navigation buttons visible
        if (backButton != null) {
            backButton.setAlpha(1f);
            // Move back button up to center in 64dp navbar
            backButton.setTranslationY(-AndroidUtilities.dp(20) * progress);
        }
        if (menuButton != null) {
            menuButton.setAlpha(1f);
            // Move menu button up to center in navbar
            menuButton.setTranslationY(-AndroidUtilities.dp(20) * progress);
        }
        
        // Animate name and status to navbar position
        if (nameView != null && statusView != null) {
            // Skip if layout not ready
            if (getWidth() == 0 || nameView.getWidth() == 0) {
                // Don't recursively call - this causes hanging
                return;
            }
            
            // Calculate horizontal movement (center to left)
            float targetX = AndroidUtilities.dp(72); // After back button
            float centerX = (getWidth() - nameView.getWidth()) / 2f;
            float deltaX = targetX - centerX;
            
            // Apply horizontal translation
            nameView.setTranslationX(deltaX * progress);
            statusView.setTranslationX(deltaX * progress);
            
            // Calculate vertical movement
            // The header shrinks from 320dp to 64dp, so we need to move content up
            // In minimized state, name should be centered vertically in navbar
            float nameCurrentY = nameView.getTop(); // ~190dp
            float nameTargetY = AndroidUtilities.dp(12); // Center in 64dp navbar
            float nameDeltaY = nameTargetY - nameCurrentY; // Should be negative (moving up)
            
            float statusCurrentY = statusView.getTop(); // ~235dp
            float statusTargetY = AndroidUtilities.dp(32); // Below name in navbar
            float statusDeltaY = statusTargetY - statusCurrentY; // Should be negative
            
            nameView.setTranslationY(nameDeltaY * progress);
            statusView.setTranslationY(statusDeltaY * progress);
            
            // Scale text for navbar - name stays larger, status becomes smaller
            float nameScale = 1f - progress * 0.2f; // Only slight reduction for name
            nameView.setScaleX(nameScale);
            nameView.setScaleY(nameScale);
            nameView.setPivotX(0); // Scale from left edge
            nameView.setPivotY(nameView.getHeight() / 2f); // Scale from vertical center
            
            float statusScale = 1f - progress * 0.5f; // Status text much smaller
            statusView.setScaleX(statusScale);
            statusView.setScaleY(statusScale);
            statusView.setPivotX(0);
            statusView.setPivotY(statusView.getHeight() / 2f);
            
            // Keep text visible and on top
            nameView.setAlpha(1f);
            statusView.setAlpha(1f);
            
            // Ensure proper view ordering for minimized state
            if (progress > 0 && progress < 0.1f) {
                // Only reorder views once at the start of animation
                // First bring background to front to ensure it's visible
                backgroundView.bringToFront();
                // Then bring navigation elements on top of background
                backButton.bringToFront();
                nameView.bringToFront();
                statusView.bringToFront();
                menuButton.bringToFront();
            }
        }
        
        // Keep background visible
        if (backgroundView != null) {
            backgroundView.setAlpha(1f);
        }
        
        invalidate();
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
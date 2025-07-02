package org.telegram.ui.Components;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.HorizontalScrollView;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.PagerAdapter;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.ProfileActivity;
import android.widget.ImageView;
import java.util.ArrayList;


/**
 * A header view that expands to show profile photo as full background on pull down
 */
public class PullToExpandHeaderView extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {
    
    // Components
    private ProfileHeaderComponent headerComponent;
    private BackupImageView avatarView;
    private BackupImageView heroImageView; // For displaying the expanded profile image
    private View heroImageContainer; // Container for clipping animation
    private TextView pullIndicator;
    private View dimOverlay;
    private View pageIndicatorView; // For page indicators
    private Paint indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint selectedIndicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int currentPage = 0;
    private int totalPages = 0;
    private ViewPager galleryPager; // For swipeable gallery
    private GalleryPagerAdapter galleryAdapter;
    
    // State
    private enum HeaderState {
        COLLAPSED,
        EXPANDING,
        EXPANDED,
        COLLAPSING
    }
    private HeaderState currentState = HeaderState.COLLAPSED;
    private float expandProgress = 0f;
    private float minimizeProgress = 0f;
    private float startY;
    private boolean isPulling = false;
    private boolean isAnimating = false;
    
    // Animation and easing
    private TimeInterpolator expandInterpolator = new DecelerateInterpolator(2f); // Smoother expansion
    private TimeInterpolator collapseInterpolator = new AccelerateDecelerateInterpolator();
    private long lastFrameTime = 0;
    private float velocity = 0f;
    
    // Clip path for circular to rect transition
    private Path clipPath = new Path();
    private float clipRadius = 0f;
    private RectF clipRect = new RectF();
    private Paint scrimPaint = new Paint();
    
    // User data
    private TLRPC.User user;
    private TLRPC.Chat chat;
    private long dialogId;
    private MessagesController.DialogPhotos dialogPhotos;
    private int currentAccount = UserConfig.selectedAccount;
    
    // Header spacer management
    private View headerSpacerView;
    
    // Dimensions
    private static final int PULL_THRESHOLD = AndroidUtilities.dp(60); // Lower threshold for easier activation
    private static final int HEADER_HEIGHT = AndroidUtilities.dp(320); // Match updated header height
    private static final int MINIMIZED_HEIGHT = AndroidUtilities.dp(100); // Increased to accommodate name and status properly
    private static final int EXPANDED_HEIGHT = AndroidUtilities.displaySize.y; // Full screen height
    private static final float OVERSCROLL_FACTOR = 0.4f; // More responsive pull
    private static final float MIN_VELOCITY_TO_EXPAND = AndroidUtilities.dp(500); // Velocity threshold for auto-expand
    private static final float ELASTIC_CONSTANT = AndroidUtilities.dp(300); // For elastic resistance
    private static final int MIN_PULL_DISTANCE = AndroidUtilities.dp(40); // Minimum before expansion starts
    private static final int SCROLL_THRESHOLD = AndroidUtilities.dp(100); // Scroll distance to trigger minimize
    
    private Theme.ResourcesProvider resourcesProvider;
    private ScrollView parentScrollView;
    private org.telegram.ui.ActionBar.BaseFragment parentFragment;
    
    public PullToExpandHeaderView(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        init();
        
        // Register for notifications
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.dialogPhotosUpdate);
    }
    
    public void setParentFragment(org.telegram.ui.ActionBar.BaseFragment fragment) {
        this.parentFragment = fragment;
    }
    
    private void init() {
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);
        
        // Also disable outline clipping for API 21+
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            setClipToOutline(false);
        }
        
        scrimPaint = new Paint();
        
        // Set initial height - fixed to prevent layout changes
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, HEADER_HEIGHT);
        setLayoutParams(params);
        
        // Don't set background color here - let the gradient view handle it
        
        // Create hero image container and view
        heroImageContainer = new FrameLayout(getContext());
        heroImageContainer.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));
        heroImageContainer.setVisibility(View.GONE); // Start hidden
        heroImageContainer.setAlpha(0f); // Start transparent
        addView(heroImageContainer, 0); // Add at bottom
        
        // Hero image view will be replaced by gallery when dialog ID is set
        heroImageView = new BackupImageView(getContext());
        heroImageView.getImageReceiver().setAspectFit(false); // Fill the container
        heroImageView.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));
        heroImageView.setAlpha(0f); // Start invisible
        ((FrameLayout)heroImageContainer).addView(heroImageView);
        
        // Create gradient scrim for text readability
        dimOverlay = new View(getContext()) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                // Draw gradient scrim at top and bottom for text readability
                if (expandProgress > 0) {
                    int width = getWidth();
                    int height = getHeight();
                    int gradientHeight = AndroidUtilities.dp(200);
                    
                    // Top gradient
                    scrimPaint.setShader(new LinearGradient(0, 0, 0, gradientHeight,
                        new int[]{0x99000000, 0x00000000},
                        null, Shader.TileMode.CLAMP));
                    canvas.drawRect(0, 0, width, gradientHeight, scrimPaint);
                    
                    // Bottom gradient for action buttons - stronger for better contrast
                    scrimPaint.setShader(new LinearGradient(0, height - gradientHeight, 0, height,
                        new int[]{0x00000000, 0x99000000},
                        null, Shader.TileMode.CLAMP));
                    canvas.drawRect(0, height - gradientHeight, width, height, scrimPaint);
                    
                    // Additional gradient for lower-left corner where text/buttons are
                    int cornerSize = AndroidUtilities.dp(300);
                    scrimPaint.setShader(new RadialGradient(
                        0, height, cornerSize,
                        new int[]{0x99000000, 0x00000000},
                        null, Shader.TileMode.CLAMP));
                    canvas.drawRect(0, height - cornerSize, cornerSize, height, scrimPaint);
                }
            }
        };
        dimOverlay.setAlpha(0f);
        addView(dimOverlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        
        // Create header component (on top of everything)
        headerComponent = new ProfileHeaderComponent(getContext(), resourcesProvider);
        addView(headerComponent, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        
        // Force header background to draw immediately
        if (headerComponent != null) {
            headerComponent.forceBackgroundRedraw();
            headerComponent.requestLayout();
            headerComponent.invalidate();
        }
        
        // Create pull indicator
        pullIndicator = new TextView(getContext());
        pullIndicator.setText("Pull to expand");
        pullIndicator.setTextColor(0xFFFFFFFF);
        pullIndicator.setTextSize(14);
        pullIndicator.setGravity(android.view.Gravity.CENTER);
        pullIndicator.setAlpha(0f);
        pullIndicator.setShadowLayer(2, 0, 1, 0x80000000);
        FrameLayout.LayoutParams indicatorParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT);
        indicatorParams.gravity = android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.BOTTOM;
        indicatorParams.bottomMargin = AndroidUtilities.dp(20);
        pullIndicator.setLayoutParams(indicatorParams);
        addView(pullIndicator);
        
        // Create page indicator view
        indicatorPaint.setColor(0x55FFFFFF);
        selectedIndicatorPaint.setColor(0xFFFFFFFF);
        
        pageIndicatorView = new View(getContext()) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                if (totalPages <= 1) return;
                
                int width = getWidth();
                int height = getHeight();
                int indicatorHeight = AndroidUtilities.dp(2);
                int spacing = AndroidUtilities.dp(2);
                int marginX = AndroidUtilities.dp(5);
                
                // Calculate indicator width
                int totalSpacing = spacing * (totalPages - 1);
                int availableWidth = width - (marginX * 2) - totalSpacing;
                int indicatorWidth = availableWidth / totalPages;
                
                // Draw indicators
                for (int i = 0; i < totalPages; i++) {
                    float left = marginX + i * (indicatorWidth + spacing);
                    float top = AndroidUtilities.dp(4); // Fixed position at top
                    float right = left + indicatorWidth;
                    float bottom = top + indicatorHeight;
                    
                    Paint paint = (i == currentPage) ? selectedIndicatorPaint : indicatorPaint;
                    canvas.drawRoundRect(left, top, right, bottom, AndroidUtilities.dp(1), AndroidUtilities.dp(1), paint);
                }
            }
        };
        pageIndicatorView.setAlpha(0f); // Start invisible
        
        FrameLayout.LayoutParams pageIndicatorParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            AndroidUtilities.dp(20)); // Increased height to ensure visibility
        pageIndicatorParams.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
        pageIndicatorParams.topMargin = AndroidUtilities.dp(40); // Position below status bar
        pageIndicatorView.setLayoutParams(pageIndicatorParams);
        addView(pageIndicatorView);
        
        
        // Get reference to avatar from header - may be null initially
        updateAvatarReference();
    }
    
    public void setScrollView(ScrollView scrollView) {
        this.parentScrollView = scrollView;
    }
    
    public void setHeaderSpacerView(View spacer) {
        this.headerSpacerView = spacer;
        updateSpacerHeight();
    }
    
    public void setScrollOffset(float offset) {
        // Don't process scroll if in expanded state
        if (currentState == HeaderState.EXPANDED || currentState == HeaderState.EXPANDING) {
            return;
        }
        
        // Pass scroll offset to header component for unified handling
        if (headerComponent != null) {
            headerComponent.updateScrollOffset(offset);
        }
        
        // Update spacer height based on scroll
        updateSpacerHeight();
        
        // Request layout to update height based on minimize progress
        requestLayout();
        invalidate();
    }
    
    
    
    private void updateAvatarReference() {
        if (headerComponent != null) {
            avatarView = headerComponent.getAvatarImageView();
        }
    }
    
    public void setUser(TLRPC.User user) {
        this.user = user;
        this.chat = null;
        this.dialogId = user != null ? user.id : 0;
        headerComponent.setUser(user);
        updateAvatarReference();
        
        // Initialize dialog photos
        if (user != null && dialogId != 0) {
            dialogPhotos = MessagesController.getInstance(currentAccount).getDialogPhotos(dialogId);
            dialogPhotos.loadCache();
            initializeGallery();
        }
        
        // Load hero image as fallback
        if (user != null && heroImageView != null) {
            AvatarDrawable avatarDrawable = new AvatarDrawable(user, true);
            if (user.photo != null && user.photo.photo_big != null) {
                ImageLocation imageLocation = ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_BIG);
                heroImageView.setImage(imageLocation, "600_600", avatarDrawable, user);
            } else {
                heroImageView.setImageDrawable(avatarDrawable);
            }
        }
    }
    
    public void setChat(TLRPC.Chat chat) {
        this.chat = chat;
        this.user = null;
        this.dialogId = chat != null ? -chat.id : 0;
        headerComponent.setChat(chat);
        updateAvatarReference();
        
        // Initialize dialog photos
        if (chat != null && dialogId != 0) {
            dialogPhotos = MessagesController.getInstance(currentAccount).getDialogPhotos(dialogId);
            dialogPhotos.loadCache();
            initializeGallery();
        }
        
        // Load hero image as fallback
        if (chat != null && heroImageView != null) {
            AvatarDrawable avatarDrawable = new AvatarDrawable(chat, true);
            if (chat.photo != null && chat.photo.photo_big != null) {
                ImageLocation imageLocation = ImageLocation.getForUserOrChat(chat, ImageLocation.TYPE_BIG);
                heroImageView.setImage(imageLocation, "600_600", avatarDrawable, chat);
            } else {
                heroImageView.setImageDrawable(avatarDrawable);
            }
        }
    }
    
    private void initializeGallery() {
        if (galleryPager == null && dialogId != 0) {
            // Remove hero image view if it exists
            if (heroImageView != null && heroImageView.getParent() != null) {
                ((ViewGroup)heroImageView.getParent()).removeView(heroImageView);
                heroImageView = null;
            }
            
            // Create ViewPager for gallery
            galleryPager = new ViewPager(getContext());
            galleryPager.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
            galleryPager.setAlpha(0f); // Start invisible
            
            // Create and set adapter
            galleryAdapter = new GalleryPagerAdapter();
            galleryPager.setAdapter(galleryAdapter);
            
            // Add page change listener for tiles
            galleryPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
                
                @Override
                public void onPageSelected(int position) {
                    currentPage = position;
                    if (pageIndicatorView != null) {
                        pageIndicatorView.invalidate();
                    }
                }
                
                @Override
                public void onPageScrollStateChanged(int state) {}
            });
            
            ((FrameLayout)heroImageContainer).addView(galleryPager);
            
            // Load images
            loadGalleryImages();
        }
    }
    
    
    private void loadGalleryImages() {
        if (dialogPhotos == null || dialogPhotos.photos.isEmpty()) {
            // If no dialog photos, load current photo
            ArrayList<ImageLocation> imageLocations = new ArrayList<>();
            
            if (user != null) {
                ImageLocation mainLocation = ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_BIG);
                if (mainLocation != null) {
                    imageLocations.add(mainLocation);
                }
            } else if (chat != null) {
                ImageLocation mainLocation = ImageLocation.getForUserOrChat(chat, ImageLocation.TYPE_BIG);
                if (mainLocation != null) {
                    imageLocations.add(mainLocation);
                }
            }
            
            if (galleryAdapter != null) {
                galleryAdapter.setData(imageLocations, user != null ? user : chat);
                totalPages = imageLocations.size();
                android.util.Log.d("PullToExpandHeader", "Set " + totalPages + " images to gallery");
            }
        }
        // If dialogPhotos exists, wait for notification to load photos
    }
    
    
    
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            android.util.Log.d("PullToExpandHeader", "Header touch DOWN at Y: " + ev.getY());
            // Check if touch is in the header area (avatar/name/status area)
            if (ev.getY() < AndroidUtilities.dp(200)) { // Top portion of header
                android.util.Log.d("PullToExpandHeader", "Touch in header area, could intercept");
                return false; // Let onTouchEvent handle it
            }
        }
        return false;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = ev.getY();
                android.util.Log.d("PullToExpandHeader", "Header onTouchEvent DOWN at Y: " + startY);
                // Only allow pull-to-expand from header area when not already expanded
                if (currentState != HeaderState.EXPANDED && startY < HEADER_HEIGHT) {
                    isPulling = true;
                    android.util.Log.d("PullToExpandHeader", "Starting pull from header");
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    return true;
                }
                return false;
                
            case MotionEvent.ACTION_MOVE:
                if (isPulling) {
                    float deltaY = ev.getY() - startY;
                    android.util.Log.d("PullToExpandHeader", "Touch MOVE deltaY: " + deltaY);
                    
                    if (deltaY > MIN_PULL_DISTANCE) {
                        // Apply elastic resistance
                        float adjustedDelta = deltaY - MIN_PULL_DISTANCE;
                        float elasticProgress = adjustedDelta / (adjustedDelta + ELASTIC_CONSTANT);
                        float progress = elasticProgress * 1.2f; // Scale to reach 1.0
                        setExpandProgress(Math.min(1f, progress));
                    } else {
                        setExpandProgress(0f);
                    }
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isPulling) {
                    float finalDeltaY = ev.getY() - startY;
                    
                    // Use velocity and position to determine action
                    float currentVelocity = getScrollViewVelocity();
                    boolean shouldExpand = false;
                    boolean shouldCollapse = false;
                    
                    if (currentState != HeaderState.EXPANDED) {
                        // Check for expansion conditions with improved velocity detection
                        shouldExpand = (finalDeltaY > PULL_THRESHOLD) || 
                                      (expandProgress > 0.15f && Math.abs(currentVelocity) > MIN_VELOCITY_TO_EXPAND && currentVelocity > 0) ||
                                      (expandProgress > 0.4f);
                    } else {
                        // Check for collapse conditions with improved velocity detection
                        shouldCollapse = (finalDeltaY < -PULL_THRESHOLD) ||
                                        (expandProgress < 0.8f && Math.abs(currentVelocity) > MIN_VELOCITY_TO_EXPAND && currentVelocity < 0) ||
                                        (expandProgress < 0.3f);
                    }
                    
                    if (shouldExpand && currentState != HeaderState.EXPANDED) {
                        animateToExpanded();
                    } else if (shouldCollapse && currentState == HeaderState.EXPANDED) {
                        animateToCollapsed();
                    } else {
                        // Spring back to current state with elastic animation
                        animateToProgress(currentState == HeaderState.EXPANDED ? 1f : 0f);
                    }
                    
                    isPulling = false;
                    velocity = 0f;
                    lastFrameTime = 0;
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    return true;
                }
                break;
        }
        
        return super.onTouchEvent(ev);
    }
    
    private void setExpandProgress(float progress) {
        expandProgress = Math.max(0f, progress);
        
        android.util.Log.d("PullToExpandHeader", "setExpandProgress: " + progress);
        
        // Show gallery or hero image as header expands
        View expandedView = galleryPager != null ? galleryPager : heroImageView;
        if (expandedView != null) {
            // Fade in expanded view
            float viewAlpha = Math.min(1f, progress * 2f);
            expandedView.setAlpha(viewAlpha);
            
            // Make container visible when needed
            if (progress > 0.01f && heroImageContainer.getVisibility() != View.VISIBLE) {
                heroImageContainer.setVisibility(View.VISIBLE);
                heroImageContainer.setAlpha(1f); // Ensure container is visible
            } else if (progress <= 0.01f && heroImageContainer.getVisibility() == View.VISIBLE) {
                heroImageContainer.setVisibility(View.GONE);
            }
            
            android.util.Log.d("PullToExpandHeader", "Expanded view alpha: " + viewAlpha + ", container visible: " + heroImageContainer.getVisibility());
        }
        
        // Show gradient scrim for text readability
        if (dimOverlay != null) {
            float overlayAlpha = Math.min(1f, progress);
            dimOverlay.setAlpha(overlayAlpha);
            dimOverlay.invalidate(); // Redraw gradient
        }
        
        // Header component changes - keep action buttons visible
        if (headerComponent != null) {
            // Keep gradient background visible when collapsed
            float gradientAlpha = Math.max(0f, 1f - progress);
            headerComponent.setGradientAlpha(gradientAlpha);
            
            // Hide avatar as hero image takes over
            float avatarAlpha = Math.max(0f, 1f - (progress * 2f));
            headerComponent.setAvatarAlpha(avatarAlpha);
            
            // Keep content visible
            headerComponent.setContentAlpha(1f);
            
            // Move content to lower-left corner when expanded
            if (progress > 0) {
                float elasticProgress = (float)(1 - Math.pow(1 - progress, 3));
                
                // Calculate current height of the view
                float heightProgress = Math.min(1f, progress);
                int expandedHeight = AndroidUtilities.dp(450);
                int currentHeight = (int) (HEADER_HEIGHT + (expandedHeight - HEADER_HEIGHT) * elasticProgress);
                
                // Position text above action buttons
                // Action buttons are at bottom with 20dp margin and are ~74dp tall
                // We want text to be just above them
                int buttonsHeight = AndroidUtilities.dp(74);
                int buttonsBottomMargin = AndroidUtilities.dp(20);
                int textBottomMargin = AndroidUtilities.dp(10); // Space between text and buttons
                
                // Calculate target position for status text (which is at 195dp originally)
                int statusOriginalY = AndroidUtilities.dp(195);
                int targetStatusY = currentHeight - buttonsHeight - buttonsBottomMargin - textBottomMargin - AndroidUtilities.dp(20);
                float translateY = targetStatusY - statusOriginalY;
                
                headerComponent.setContentTranslation(0, translateY);
                
                // Also need to align content to left
                headerComponent.setContentAlignment(elasticProgress);
                
                // Scale down action buttons
                headerComponent.setContentExpansion(elasticProgress);
            } else {
                headerComponent.setContentTranslation(0, 0);
                headerComponent.setContentAlignment(0);
                headerComponent.setContentExpansion(0);
            }
        }
        
        // Update pull indicator
        if (pullIndicator != null) {
            if (progress > 0 && progress < 0.5f) {
                pullIndicator.setText("Pull to view photo");
                pullIndicator.setAlpha(Math.min(1f, progress * 3f));
                pullIndicator.setScaleX(1f + progress * 0.2f);
                pullIndicator.setScaleY(1f + progress * 0.2f);
            } else if (progress >= 0.5f && progress < 0.8f) {
                pullIndicator.setText("Release to expand");
                pullIndicator.setAlpha(1f - (progress - 0.5f) * 3f);
            } else {
                pullIndicator.setAlpha(0f);
            }
        }
        
        // Show page indicators when expanded
        if (pageIndicatorView != null) {
            float indicatorAlpha = progress > 0.7f ? Math.min(1f, (progress - 0.7f) * 3.33f) : 0f;
            pageIndicatorView.setAlpha(indicatorAlpha);
        }
        
        // Height changes are now handled in onMeasure
        // Just request layout to trigger proper measurement
        if (progress > 0.01f && minimizeProgress < 0.1f) {
            requestLayout();
        }
        
        // Update spacer to push content down when expanding
        updateSpacerHeight();
        
        invalidate();
    }
    
    private void updateSpacerHeight() {
        if (headerSpacerView != null && getParent() != null) {
            // Calculate the actual height we're taking up
            int currentHeight = getMeasuredHeight();
            if (currentHeight == 0) {
                // Fallback calculation if not measured yet
                if (expandProgress > 0) {
                    int expandedHeight = AndroidUtilities.dp(450);
                    currentHeight = (int)(HEADER_HEIGHT + (expandedHeight - HEADER_HEIGHT) * expandProgress);
                } else if (minimizeProgress > 0) {
                    currentHeight = (int)(HEADER_HEIGHT - (HEADER_HEIGHT - MINIMIZED_HEIGHT) * minimizeProgress);
                } else {
                    currentHeight = HEADER_HEIGHT;
                }
            }
            
            // Update spacer to match our height
            ViewGroup.LayoutParams params = headerSpacerView.getLayoutParams();
            if (params != null && params.height != currentHeight) {
                params.height = currentHeight;
                headerSpacerView.setLayoutParams(params);
            }
        }
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Calculate the target height based on current state
        int targetHeight;
        
        // Get minimize progress from header component for accurate height
        float currentMinimizeProgress = 0f;
        if (headerComponent != null) {
            currentMinimizeProgress = headerComponent.getMinimizeProgress();
        }
        
        if (expandProgress > 0) {
            // Expanding
            int expandedHeight = AndroidUtilities.dp(450);
            targetHeight = (int)(HEADER_HEIGHT + (expandedHeight - HEADER_HEIGHT) * expandProgress);
        } else if (currentMinimizeProgress > 0) {
            // Minimizing - use progress from header component
            targetHeight = (int)(HEADER_HEIGHT - (HEADER_HEIGHT - MINIMIZED_HEIGHT) * currentMinimizeProgress);
        } else {
            // Default
            targetHeight = HEADER_HEIGHT;
        }
        
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(widthSize, targetHeight);
        
        // Measure children with the calculated dimensions
        int childWidthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(targetHeight, MeasureSpec.EXACTLY);
        
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                child.measure(childWidthSpec, childHeightSpec);
            }
        }
    }
    
    @Override
    protected void dispatchDraw(Canvas canvas) {
        // Simply draw without any clipping - let the animation handle visibility
        super.dispatchDraw(canvas);
    }
    
    private void animateToExpanded() {
        android.util.Log.d("PullToExpandHeader", "Animating to expanded");
        currentState = HeaderState.EXPANDING;
        animateToProgress(1f, expandInterpolator, 500); // 0.5 second expansion
    }
    
    private void animateToCollapsed() {
        android.util.Log.d("PullToExpandHeader", "Animating to collapsed");
        currentState = HeaderState.COLLAPSING;
        animateToProgress(0f, collapseInterpolator, 350); // 0.35 second collapse
    }
    
    private void animateToProgress(float targetProgress) {
        animateToProgress(targetProgress, new AccelerateDecelerateInterpolator(), 200);
    }
    
    private void animateToProgress(float targetProgress, TimeInterpolator interpolator, long duration) {
        isAnimating = true;
        float startProgress = expandProgress;
        
        animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .setUpdateListener(animation -> {
                float animProgress = animation.getAnimatedFraction();
                float currentProgress = startProgress + (targetProgress - startProgress) * animProgress;
                setExpandProgress(currentProgress);
            })
            .withStartAction(() -> {
                isAnimating = true;
            })
            .withEndAction(() -> {
                isAnimating = false;
                setExpandProgress(targetProgress);
                
                // Clean up when collapsed
                if (targetProgress == 0f) {
                    if (pullIndicator != null) {
                        pullIndicator.setAlpha(0f);
                        pullIndicator.setScaleX(1f);
                        pullIndicator.setScaleY(1f);
                    }
                    if (heroImageView != null) {
                        heroImageView.setAlpha(0f);
                    }
                    if (galleryPager != null) {
                        galleryPager.setAlpha(0f);
                    }
                    if (heroImageContainer != null) {
                        heroImageContainer.setVisibility(View.GONE);
                    }
                    if (dimOverlay != null) {
                        dimOverlay.setAlpha(0f);
                    }
                    if (headerComponent != null) {
                        headerComponent.setGradientAlpha(1f);
                        headerComponent.setContentTranslation(0, 0);
                        headerComponent.setContentAlignment(0); // Reset to center
                        headerComponent.setAvatarAlpha(1f);
                        headerComponent.setContentAlpha(1f);
                    }
                    
                    // Height is now managed by onMeasure, just request layout
                    requestLayout();
                    
                }
                
                // Update state based on final progress
                if (targetProgress >= 1f) {
                    currentState = HeaderState.EXPANDED;
                    android.util.Log.d("PullToExpandHeader", "Animation complete - gallery is now expanded");
                } else if (targetProgress <= 0f) {
                    currentState = HeaderState.COLLAPSED;
                    android.util.Log.d("PullToExpandHeader", "Animation complete - gallery is now collapsed");
                }
            })
            .start();
    }
    
    
    public ProfileHeaderComponent getHeaderComponent() {
        return headerComponent;
    }
    
    public boolean isExpanded() {
        return currentState == HeaderState.EXPANDED;
    }
    
    
    public void startPulling(float startY) {
        this.startY = startY;
        this.isPulling = true;
        this.lastFrameTime = System.currentTimeMillis();
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }
    
    /**
     * Get velocity from parent scroll view for better transition decisions
     */
    private float getScrollViewVelocity() {
        if (parentScrollView instanceof PullToExpandScrollView) {
            return ((PullToExpandScrollView) parentScrollView).getCurrentVelocity();
        }
        return velocity; // Fallback to locally calculated velocity
    }
    
    public void startCollapsing() {
        if (currentState == HeaderState.EXPANDED) {
            animateToCollapsed();
        }
    }
    
    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.dialogPhotosUpdate) {
            MessagesController.DialogPhotos photos = (MessagesController.DialogPhotos) args[0];
            if (this.dialogPhotos == photos && photos.photos != null && !photos.photos.isEmpty()) {
                // Convert TLRPC.Photo objects to ImageLocations
                ArrayList<ImageLocation> imageLocations = new ArrayList<>();
                
                // First, add the current profile photo
                ImageLocation currentLocation = null;
                if (user != null) {
                    currentLocation = ImageLocation.getForUserOrChat(user, ImageLocation.TYPE_BIG);
                } else if (chat != null) {
                    currentLocation = ImageLocation.getForUserOrChat(chat, ImageLocation.TYPE_BIG);
                }
                if (currentLocation != null) {
                    imageLocations.add(currentLocation);
                }
                
                // Then add other photos from history
                for (TLRPC.Photo photo : photos.photos) {
                    if (photo != null && !(photo instanceof TLRPC.TL_photoEmpty)) {
                        TLRPC.PhotoSize sizeFull = org.telegram.messenger.FileLoader.getClosestPhotoSizeWithSize(photo.sizes, 640);
                        if (sizeFull != null) {
                            ImageLocation location = ImageLocation.getForPhoto(sizeFull, photo);
                            if (location != null) {
                                // Avoid duplicating current photo
                                boolean isDuplicate = false;
                                if (currentLocation != null && location.location != null && currentLocation.location != null) {
                                    isDuplicate = location.location.volume_id == currentLocation.location.volume_id &&
                                                location.location.local_id == currentLocation.location.local_id;
                                }
                                if (!isDuplicate) {
                                    imageLocations.add(location);
                                }
                            }
                        }
                    }
                }
                
                // Update gallery adapter
                if (galleryAdapter != null) {
                    galleryAdapter.setData(imageLocations, user != null ? user : chat);
                    totalPages = imageLocations.size();
                    if (pageIndicatorView != null) {
                        pageIndicatorView.invalidate();
                    }
                }
            }
        }
    }
    
    // Simple gallery adapter for ViewPager
    private class GalleryPagerAdapter extends PagerAdapter {
        private ArrayList<ImageLocation> images = new ArrayList<>();
        private Object parentObject;
        
        void setData(ArrayList<ImageLocation> imageLocations, Object parent) {
            images.clear();
            if (imageLocations != null) {
                images.addAll(imageLocations);
            }
            parentObject = parent;
            notifyDataSetChanged();
        }
        
        @Override
        public int getCount() {
            return images.size();
        }
        
        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
        
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            BackupImageView imageView = new BackupImageView(getContext());
            imageView.getImageReceiver().setAspectFit(false);
            
            if (position < images.size()) {
                ImageLocation location = images.get(position);
                // Use a placeholder drawable for debugging
                AvatarDrawable placeholder = new AvatarDrawable();
                if (parentObject instanceof TLRPC.User) {
                    placeholder.setInfo((TLRPC.User)parentObject);
                } else if (parentObject instanceof TLRPC.Chat) {
                    placeholder.setInfo((TLRPC.Chat)parentObject);
                }
                imageView.setImage(location, "600_600", placeholder, parentObject);
                android.util.Log.d("PullToExpandHeader", "Loading image at position " + position + ", location: " + location);
            }
            
            container.addView(imageView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            return imageView;
        }
        
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
        
        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.dialogPhotosUpdate);
    }
    
}
package org.telegram.ui.Components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ScrollView;

import org.telegram.messenger.AndroidUtilities;

/**
 * Helper method to get current velocity for the header
 */

/**
 * Custom ScrollView that allows pull-to-expand gesture at the top
 * with improved touch handling and velocity tracking for header background expansion
 */
public class PullToExpandScrollView extends ScrollView {
    
    private PullToExpandHeaderView expandableHeader;
    private float startY;
    private float lastY;
    private boolean isInterceptingForHeader = false;
    private boolean isDragging = false;
    private VelocityTracker velocityTracker;
    private final int touchSlop;
    private final int minimumVelocity;
    private final int maximumVelocity;
    private int lastScrollY = 0;
    private long lastTouchTime = 0;
    
    public PullToExpandScrollView(Context context) {
        super(context);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        touchSlop = configuration.getScaledTouchSlop();
        minimumVelocity = configuration.getScaledMinimumFlingVelocity();
        maximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }
    
    public PullToExpandScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        touchSlop = configuration.getScaledTouchSlop();
        minimumVelocity = configuration.getScaledMinimumFlingVelocity();
        maximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }
    
    public void setExpandableHeader(PullToExpandHeaderView header) {
        this.expandableHeader = header;
    }
    
    /**
     * Get current touch velocity for header transition calculations
     */
    public float getCurrentVelocity() {
        if (velocityTracker != null) {
            velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
            return velocityTracker.getYVelocity();
        }
        return 0f;
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (expandableHeader == null) {
            return super.onInterceptTouchEvent(ev);
        }
        
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = ev.getY();
                lastY = startY;
                isInterceptingForHeader = false;
                isDragging = false;
                lastTouchTime = System.currentTimeMillis();
                
                android.util.Log.d("PullToExpandScrollView", "ACTION_DOWN at Y: " + startY + ", scrollY: " + getScrollY());
                
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain();
                } else {
                    velocityTracker.clear();
                }
                velocityTracker.addMovement(ev);
                break;
                
            case MotionEvent.ACTION_MOVE:
                float deltaY = ev.getY() - startY;
                float moveDelta = Math.abs(ev.getY() - lastY);
                
                velocityTracker.addMovement(ev);
                lastY = ev.getY();
                
                // More sensitive detection for pull gesture
                if (!isDragging && moveDelta > touchSlop) {
                    isDragging = true;
                }
                
                // Intercept if at top and downward movement
                if (getScrollY() <= 0 && deltaY > touchSlop && !expandableHeader.isExpanded()) {
                    android.util.Log.d("PullToExpandScrollView", "Intercepting for header at deltaY: " + deltaY);
                    expandableHeader.startPulling(ev.getY()); // Use current Y instead of startY
                    isInterceptingForHeader = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true; // Intercept to handle pull gesture
                }
                
                // Also handle when header is already expanded and user pulls up to collapse
                if (expandableHeader.isExpanded() && deltaY < -touchSlop * 0.5f && isDragging) {
                    isInterceptingForHeader = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                isDragging = false;
                isInterceptingForHeader = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        
        return super.onInterceptTouchEvent(ev);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (expandableHeader != null && isInterceptingForHeader) {
            // Pass to header with velocity tracking
            if (velocityTracker != null) {
                velocityTracker.addMovement(ev);
            }
            
            boolean result = expandableHeader.onTouchEvent(ev);
            
            // Clean up velocity tracker on ACTION_UP/CANCEL
            if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                isInterceptingForHeader = false;
                isDragging = false;
                getParent().requestDisallowInterceptTouchEvent(false);
            }
            
            return result;
        }
        
        // Handle normally
        return super.onTouchEvent(ev);
    }
    
    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        
        if (expandableHeader != null) {
            // Only trigger collapse if header is expanded and user scrolls content
            if (expandableHeader.isExpanded()) {
                // If scrolled down from top position with smaller threshold
                if (t > AndroidUtilities.dp(5)) {
                    expandableHeader.startCollapsing();
                }
            } else {
                // Update header with scroll offset for minimize state
                expandableHeader.setScrollOffset(t);
            }
        }
        
        lastScrollY = t;
    }
}
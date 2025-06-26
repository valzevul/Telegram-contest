package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.SharedDocumentCell;
import org.telegram.ui.Cells.SharedLinkCell;
import org.telegram.ui.Cells.SharedAudioCell;

import java.util.ArrayList;
import java.util.List;

public class ProfileContentComponent extends LinearLayout {
    private Theme.ResourcesProvider resourcesProvider;
    private GridLayout mediaGrid;
    private RecyclerView listView;
    private ListContentAdapter listAdapter;
    private LinearLayout emptyStateLayout;
    private OnContentItemClickListener listener;
    private List<ContentItem> contentItems = new ArrayList<>();
    private int columns = 3;
    private int currentContentType = ContentItem.TYPE_PHOTO; // Track current content type for layout selection
    
    public interface OnContentItemClickListener {
        void onContentItemClick(ContentItem item, int position);
    }
    
    public static class ContentItem {
        public static final int TYPE_PHOTO = 0;
        public static final int TYPE_VIDEO = 1;
        public static final int TYPE_FILE = 2;
        public static final int TYPE_LINK = 3;
        public static final int TYPE_VOICE = 4;
        public static final int TYPE_MUSIC = 5;
        public static final int TYPE_GIF = 6;
        
        public int type;
        public String title;
        public String subtitle;
        public String thumbnailUrl;
        public String duration;
        public MessageObject messageObject;
        public Object data;
        
        public ContentItem(int type) {
            this.type = type;
        }
    }
    
    private class ListContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_DOCUMENT = 1;
        private static final int VIEW_TYPE_LINK = 2;
        private static final int VIEW_TYPE_AUDIO = 3;
        
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case VIEW_TYPE_DOCUMENT:
                    view = new SharedDocumentCell(getContext(), SharedDocumentCell.VIEW_TYPE_DEFAULT, resourcesProvider);
                    break;
                case VIEW_TYPE_LINK:
                    view = new SharedLinkCell(getContext(), SharedLinkCell.VIEW_TYPE_DEFAULT, resourcesProvider);
                    break;
                case VIEW_TYPE_AUDIO:
                    view = new SharedAudioCell(getContext(), resourcesProvider);
                    break;
                default:
                    view = new View(getContext());
                    break;
            }
            return new RecyclerView.ViewHolder(view) {};
        }
        
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (position >= contentItems.size()) return;
            
            ContentItem item = contentItems.get(position);
            if (item == null || item.messageObject == null) return;
            
            switch (holder.getItemViewType()) {
                case VIEW_TYPE_DOCUMENT:
                    SharedDocumentCell documentCell = (SharedDocumentCell) holder.itemView;
                    documentCell.setDocument(item.messageObject, position == 0);
                    break;
                case VIEW_TYPE_LINK:
                    SharedLinkCell linkCell = (SharedLinkCell) holder.itemView;
                    linkCell.setLink(item.messageObject, position == 0);
                    break;
                case VIEW_TYPE_AUDIO:
                    SharedAudioCell audioCell = (SharedAudioCell) holder.itemView;
                    audioCell.setMessageObject(item.messageObject, position == 0);
                    break;
            }
            
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onContentItemClick(item, position);
                }
            });
        }
        
        @Override
        public int getItemViewType(int position) {
            if (position >= contentItems.size()) return 0;
            
            ContentItem item = contentItems.get(position);
            if (item == null) return 0;
            
            switch (item.type) {
                case ContentItem.TYPE_FILE:
                    return VIEW_TYPE_DOCUMENT;
                case ContentItem.TYPE_LINK:
                    return VIEW_TYPE_LINK;
                case ContentItem.TYPE_VOICE:
                case ContentItem.TYPE_MUSIC:
                    return VIEW_TYPE_AUDIO;
                default:
                    return 0;
            }
        }
        
        @Override
        public int getItemCount() {
            return contentItems != null ? contentItems.size() : 0;
        }
    }
    
    private static class MediaItemView extends FrameLayout {
        private BackupImageView thumbnailView;
        private TextView durationView;
        private View overlayView;
        private Paint cornerPaint;
        private RectF rectF = new RectF();
        private float cornerRadius;
        
        public MediaItemView(Context context) {
            super(context);
            cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            cornerRadius = AndroidUtilities.dp(3); // Subtle rounding to match design
            
            thumbnailView = new BackupImageView(context);
            thumbnailView.getImageReceiver().setRoundRadius(AndroidUtilities.dp(3));
            thumbnailView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
            addView(thumbnailView);
            
            overlayView = new View(context);
            overlayView.setVisibility(View.GONE);
            overlayView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
            addView(overlayView);
            
            durationView = new TextView(context);
            durationView.setTextSize(12);
            durationView.setTextColor(Color.WHITE);
            durationView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            durationView.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(3), 
                                   AndroidUtilities.dp(6), AndroidUtilities.dp(3));
            durationView.setVisibility(View.GONE);
            
            GradientDrawable durationBg = new GradientDrawable();
            durationBg.setCornerRadius(AndroidUtilities.dp(4));
            durationBg.setColor(0x80000000);
            durationView.setBackground(durationBg);
            
            FrameLayout.LayoutParams durationParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
            durationParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
            durationParams.bottomMargin = AndroidUtilities.dp(4);
            durationParams.rightMargin = AndroidUtilities.dp(4);
            durationView.setLayoutParams(durationParams);
            addView(durationView);
        }
        
        public void setContentItem(ContentItem item) {
            GradientDrawable background = new GradientDrawable();
            background.setCornerRadius(cornerRadius);
            
            if (item.type == ContentItem.TYPE_PHOTO || item.type == ContentItem.TYPE_VIDEO || item.type == ContentItem.TYPE_GIF) {
                background.setColor(0xFFE0E0E0); // Light gray placeholder
                setBackground(background);
                
                // Load real images from MessageObject
                if (item.messageObject != null) {
                    try {
                        if (item.messageObject.isPhoto()) {
                            // Load photo with highest quality available
                            TLRPC.PhotoSize photoSize = org.telegram.messenger.FileLoader.getClosestPhotoSizeWithSize(
                                item.messageObject.photoThumbs, AndroidUtilities.dp(300));
                            if (photoSize != null) {
                                // Use high quality filtering for sharp images
                                thumbnailView.setImage(org.telegram.messenger.ImageLocation.getForPhoto(photoSize, item.messageObject.messageOwner.media.photo), 
                                    "150_150", null, null, 0, item.messageObject);
                            } else {
                                // Try medium size if large not available
                                photoSize = org.telegram.messenger.FileLoader.getClosestPhotoSizeWithSize(
                                    item.messageObject.photoThumbs, AndroidUtilities.dp(150));
                                if (photoSize != null) {
                                    thumbnailView.setImage(org.telegram.messenger.ImageLocation.getForPhoto(photoSize, item.messageObject.messageOwner.media.photo), 
                                        "150_150", null, null, 0, item.messageObject);
                                } else {
                                    // Fallback to smallest available
                                    photoSize = org.telegram.messenger.FileLoader.getClosestPhotoSizeWithSize(
                                        item.messageObject.photoThumbs, AndroidUtilities.dp(50));
                                    if (photoSize != null) {
                                        thumbnailView.setImage(org.telegram.messenger.ImageLocation.getForPhoto(photoSize, item.messageObject.messageOwner.media.photo), 
                                            "100_100", null, null, 0, item.messageObject);
                                    }
                                }
                            }
                        } else if (item.messageObject.isVideo() || item.messageObject.isGif()) {
                            // Load video/GIF thumbnail with high quality
                            TLRPC.PhotoSize thumb = null;
                            if (item.messageObject.getDocument() != null && item.messageObject.getDocument().thumbs != null) {
                                thumb = org.telegram.messenger.FileLoader.getClosestPhotoSizeWithSize(
                                    item.messageObject.getDocument().thumbs, AndroidUtilities.dp(300));
                            }
                            
                            if (thumb != null) {
                                thumbnailView.setImage(org.telegram.messenger.ImageLocation.getForDocument(thumb, item.messageObject.getDocument()), 
                                    "150_150", null, null, 0, item.messageObject);
                            } else {
                                // Try message photo thumbs as fallback
                                TLRPC.PhotoSize photoThumb = org.telegram.messenger.FileLoader.getClosestPhotoSizeWithSize(
                                    item.messageObject.photoThumbs, AndroidUtilities.dp(150));
                                if (photoThumb != null) {
                                    thumbnailView.setImage(org.telegram.messenger.ImageLocation.getForPhoto(photoThumb, item.messageObject.messageOwner.media.photo), 
                                        "150_150", null, null, 0, item.messageObject);
                                } else {
                                    // For videos without thumbnails, show a themed placeholder
                                    android.graphics.drawable.GradientDrawable videoPlaceholder = new android.graphics.drawable.GradientDrawable();
                                    videoPlaceholder.setColor(item.messageObject.isGif() ? 0xFF9C27B0 : 0xFF4A90E2); // Purple for GIFs, Blue for videos
                                    videoPlaceholder.setCornerRadius(AndroidUtilities.dp(3));
                                    thumbnailView.setImageDrawable(videoPlaceholder);
                                    
                                    // Add a play icon overlay for videos
                                    // This would need additional UI implementation
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Show colored placeholder on error
                        thumbnailView.setImageDrawable(null);
                    }
                } else {
                    // Show colored placeholder for mock data
                    thumbnailView.setImageDrawable(null);
                }
                
                if (item.duration != null && (item.type == ContentItem.TYPE_VIDEO || item.type == ContentItem.TYPE_GIF)) {
                    durationView.setText(item.duration);
                    durationView.setVisibility(View.VISIBLE);
                } else {
                    durationView.setVisibility(View.GONE);
                }
                
                if (item.type == ContentItem.TYPE_GIF) {
                    overlayView.setVisibility(View.VISIBLE);
                    overlayView.setBackgroundColor(0x40000000);
                } else {
                    overlayView.setVisibility(View.GONE);
                }
            } else {
                // Handle other content types (files, links, voice, music)
                // These should use proper Telegram cell implementations but for now show placeholders
                switch (item.type) {
                    case ContentItem.TYPE_FILE:
                        background.setColor(0xFF4285F4); // Blue for files
                        thumbnailView.setImageDrawable(null);
                        break;
                    case ContentItem.TYPE_LINK:
                        background.setColor(0xFF34A853); // Green for links
                        thumbnailView.setImageDrawable(null);
                        break;
                    case ContentItem.TYPE_VOICE:
                        background.setColor(0xFFEA4335); // Red for voice
                        thumbnailView.setImageDrawable(null);
                        if (item.duration != null) {
                            durationView.setText(item.duration);
                            durationView.setVisibility(View.VISIBLE);
                        }
                        break;
                    case ContentItem.TYPE_MUSIC:
                        background.setColor(0xFFFBBC05); // Yellow for music
                        thumbnailView.setImageDrawable(null);
                        if (item.duration != null) {
                            durationView.setText(item.duration);
                            durationView.setVisibility(View.VISIBLE);
                        }
                        break;
                    default:
                        background.setColor(0xFFEEEEEE);
                        break;
                }
                setBackground(background);
                overlayView.setVisibility(View.GONE);
                
                if (item.type != ContentItem.TYPE_VOICE && item.type != ContentItem.TYPE_MUSIC) {
                    durationView.setVisibility(View.GONE);
                }
            }
        }
        
        @Override
        protected void dispatchDraw(Canvas canvas) {
            rectF.set(0, 0, getWidth(), getHeight());
            canvas.clipRect(rectF);
            super.dispatchDraw(canvas);
        }
    }

    public ProfileContentComponent(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        
        setOrientation(LinearLayout.VERTICAL);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = AndroidUtilities.dp(2); // Minimal 2px spacing after tabs
        setLayoutParams(params);
        
        createMediaGrid();
        createListView();
        createEmptyState();
    }
    
    private void createMediaGrid() {
        mediaGrid = new GridLayout(getContext());
        mediaGrid.setColumnCount(columns);
        mediaGrid.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(2), AndroidUtilities.dp(16), AndroidUtilities.dp(8)); // Consistent padding
        
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        mediaGrid.setLayoutParams(gridParams);
        
        addView(mediaGrid);
    }
    
    private void createListView() {
        listView = new RecyclerView(getContext());
        listView.setLayoutManager(new LinearLayoutManager(getContext()));
        listAdapter = new ListContentAdapter();
        listView.setAdapter(listAdapter);
        listView.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));
        
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        listView.setLayoutParams(listParams);
        listView.setVisibility(View.GONE);
        
        addView(listView);
    }
    
    private void createEmptyState() {
        emptyStateLayout = new LinearLayout(getContext());
        emptyStateLayout.setOrientation(LinearLayout.VERTICAL);
        emptyStateLayout.setGravity(Gravity.CENTER);
        emptyStateLayout.setVisibility(View.GONE);
        
        LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            AndroidUtilities.dp(200));
        emptyParams.topMargin = AndroidUtilities.dp(32);
        emptyStateLayout.setLayoutParams(emptyParams);
        
        TextView emptyTitle = new TextView(getContext());
        emptyTitle.setText("No media yet");
        emptyTitle.setTextSize(17);
        emptyTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
        emptyTitle.setGravity(Gravity.CENTER);
        emptyStateLayout.addView(emptyTitle);
        
        TextView emptySubtitle = new TextView(getContext());
        emptySubtitle.setText("Photos and videos will appear here");
        emptySubtitle.setTextSize(14);
        emptySubtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider));
        emptySubtitle.setGravity(Gravity.CENTER);
        
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = AndroidUtilities.dp(8);
        emptySubtitle.setLayoutParams(subtitleParams);
        emptyStateLayout.addView(emptySubtitle);
        
        addView(emptyStateLayout);
    }
    
    public void setContentItems(List<ContentItem> items) {
        setContentItems(items, ContentItem.TYPE_PHOTO); // Default to photo/video grid
    }
    
    public void setContentItems(List<ContentItem> items, int contentType) {
        this.contentItems = items;
        this.currentContentType = contentType;
        updateContent();
    }
    
    private void updateContent() {
        try {
            if (mediaGrid == null || listView == null) return;
            
            mediaGrid.removeAllViews();
            
            if (contentItems == null || contentItems.isEmpty()) {
                mediaGrid.setVisibility(View.GONE);
                listView.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.VISIBLE);
                return;
            }
            
            emptyStateLayout.setVisibility(View.GONE);
            
            // Determine which layout to use based on content type
            boolean useGridLayout = (currentContentType == ContentItem.TYPE_PHOTO || 
                                   currentContentType == ContentItem.TYPE_VIDEO || 
                                   currentContentType == ContentItem.TYPE_GIF);
            
            if (useGridLayout) {
                // Use grid layout for photos/videos
                mediaGrid.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
                
                int screenWidth = AndroidUtilities.displaySize.x;
                // Calculate exact square size accounting for padding and gaps
                int totalPadding = AndroidUtilities.dp(32); // 16dp each side
                int totalGaps = AndroidUtilities.dp(3) * (columns - 1); // 3px gaps between items
                int itemSize = (screenWidth - totalPadding - totalGaps) / columns;
                
                int maxItems = Math.min(contentItems.size(), 15);
                
                for (int i = 0; i < maxItems; i++) {
                    ContentItem item = contentItems.get(i);
                    if (item == null) continue;
                    
                    try {
                        MediaItemView itemView = new MediaItemView(getContext());
                        itemView.setContentItem(item);
                        
                        GridLayout.LayoutParams itemParams = new GridLayout.LayoutParams();
                        itemParams.width = itemSize;
                        itemParams.height = itemSize;
                        itemParams.setMargins(AndroidUtilities.dp(1.5f), AndroidUtilities.dp(1.5f), 
                                            AndroidUtilities.dp(1.5f), AndroidUtilities.dp(1.5f)); // 3px total gap between items
                        itemView.setLayoutParams(itemParams);
                        
                        final int position = i;
                        itemView.setOnClickListener(v -> {
                            try {
                                if (listener != null) {
                                    listener.onContentItemClick(item, position);
                                }
                            } catch (Exception e) {
                                // Ignore click errors
                            }
                        });
                        
                        mediaGrid.addView(itemView);
                    } catch (Exception e) {
                        // Skip this item if there's an error creating it
                        continue;
                    }
                }
            } else {
                // Use list layout for files, links, voice, music
                mediaGrid.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
                
                listAdapter.notifyDataSetChanged();
            }
            
        } catch (Exception e) {
            // Fallback to empty state if there are any issues
            try {
                mediaGrid.setVisibility(View.GONE);
                listView.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.VISIBLE);
            } catch (Exception ex) {
                // Ignore even this fallback if it fails
            }
        }
    }
    
    public void setColumns(int columns) {
        this.columns = Math.max(1, Math.min(columns, 4));
        mediaGrid.setColumnCount(this.columns);
        updateContent();
    }
    
    public void setOnContentItemClickListener(OnContentItemClickListener listener) {
        this.listener = listener;
    }
    
    public void updateTheme() {
        if (emptyStateLayout != null) {
            TextView title = (TextView) emptyStateLayout.getChildAt(0);
            TextView subtitle = (TextView) emptyStateLayout.getChildAt(1);
            if (title != null) {
                title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
            }
            if (subtitle != null) {
                subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, resourcesProvider));
            }
        }
    }
    
}
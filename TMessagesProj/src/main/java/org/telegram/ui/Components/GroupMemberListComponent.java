package org.telegram.ui.Components;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;
import java.util.List;

public class GroupMemberListComponent extends LinearLayout {
    private Theme.ResourcesProvider resourcesProvider;
    private OnMemberClickListener memberClickListener;
    private OnAddMembersClickListener addMembersClickListener;
    
    public interface OnMemberClickListener {
        void onMemberClick(TLRPC.User user, boolean isAdmin);
    }
    
    public interface OnAddMembersClickListener {
        void onAddMembersClick();
    }
    
    public GroupMemberListComponent(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        
        setOrientation(LinearLayout.VERTICAL);
        setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), 
                   AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        setLayoutParams(params);
        
        createAddMembersRow();
    }
    
    private void createAddMembersRow() {
        LinearLayout addMembersRow = new LinearLayout(getContext());
        addMembersRow.setOrientation(LinearLayout.HORIZONTAL);
        addMembersRow.setGravity(Gravity.CENTER_VERTICAL);
        addMembersRow.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12),
                                AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(56));
        addMembersRow.setLayoutParams(rowParams);
        
        // Add icon
        ImageView addIcon = new ImageView(getContext());
        addIcon.setImageResource(R.drawable.msg_add);
        addIcon.setColorFilter(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, resourcesProvider));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
            AndroidUtilities.dp(24), AndroidUtilities.dp(24));
        iconParams.rightMargin = AndroidUtilities.dp(12);
        addIcon.setLayoutParams(iconParams);
        addMembersRow.addView(addIcon);
        
        // Add Members text
        TextView addMembersText = new TextView(getContext());
        addMembersText.setText("Add Members");
        addMembersText.setTextSize(16);
        addMembersText.setTextColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, resourcesProvider));
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        addMembersText.setLayoutParams(textParams);
        addMembersRow.addView(addMembersText);
        
        addMembersRow.setOnClickListener(v -> {
            if (addMembersClickListener != null) {
                addMembersClickListener.onAddMembersClick();
            }
        });
        
        addView(addMembersRow);
    }
    
    public void setMembers(List<TLRPC.User> members, List<Boolean> adminStatus) {
        // Remove existing member rows (keep Add Members row)
        while (getChildCount() > 1) {
            removeViewAt(getChildCount() - 1);
        }
        
        if (members == null || members.isEmpty()) {
            return;
        }
        
        for (int i = 0; i < members.size(); i++) {
            TLRPC.User member = members.get(i);
            boolean isAdmin = adminStatus != null && i < adminStatus.size() && adminStatus.get(i);
            
            if (member != null) {
                LinearLayout memberRow = createMemberRow(member, isAdmin);
                addView(memberRow);
            }
        }
    }
    
    private LinearLayout createMemberRow(TLRPC.User member, boolean isAdmin) {
        LinearLayout memberRow = new LinearLayout(getContext());
        memberRow.setOrientation(LinearLayout.HORIZONTAL);
        memberRow.setGravity(Gravity.CENTER_VERTICAL);
        memberRow.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8),
                            AndroidUtilities.dp(12), AndroidUtilities.dp(8));
        
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(56));
        memberRow.setLayoutParams(rowParams);
        
        // Member avatar
        BackupImageView avatarView = new BackupImageView(getContext());
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(
            AndroidUtilities.dp(40), AndroidUtilities.dp(40));
        avatarParams.rightMargin = AndroidUtilities.dp(12);
        avatarView.setLayoutParams(avatarParams);
        avatarView.getImageReceiver().setRoundRadius(AndroidUtilities.dp(20));
        
        // Set avatar image
        AvatarDrawable avatarDrawable = new AvatarDrawable(member, true);
        if (member.photo != null && member.photo.photo_small != null) {
            ImageLocation imageLocation = ImageLocation.getForUserOrChat(member, ImageLocation.TYPE_SMALL);
            avatarView.setImage(imageLocation, "50_50", avatarDrawable, member);
        } else {
            avatarView.setImageDrawable(avatarDrawable);
        }
        
        memberRow.addView(avatarView);
        
        // Member info container
        LinearLayout infoContainer = new LinearLayout(getContext());
        infoContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        infoContainer.setLayoutParams(infoParams);
        
        // Member name
        TextView nameView = new TextView(getContext());
        String memberName = ContactsController.formatName(member.first_name, member.last_name);
        nameView.setText(memberName);
        nameView.setTextSize(16);
        nameView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        nameView.setMaxLines(1);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        infoContainer.addView(nameView);
        
        // Member status
        TextView statusView = new TextView(getContext());
        String status = getUserStatus(member);
        statusView.setText(status);
        statusView.setTextSize(14);
        statusView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
        statusView.setMaxLines(1);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = AndroidUtilities.dp(2);
        statusView.setLayoutParams(statusParams);
        infoContainer.addView(statusView);
        
        memberRow.addView(infoContainer);
        
        // Admin badge
        if (isAdmin) {
            TextView adminBadge = new TextView(getContext());
            adminBadge.setText("admin");
            adminBadge.setTextSize(14);
            adminBadge.setTextColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, resourcesProvider));
            adminBadge.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            badgeParams.leftMargin = AndroidUtilities.dp(8);
            adminBadge.setLayoutParams(badgeParams);
            memberRow.addView(adminBadge);
        }
        
        memberRow.setOnClickListener(v -> {
            if (memberClickListener != null) {
                memberClickListener.onMemberClick(member, isAdmin);
            }
        });
        
        return memberRow;
    }
    
    private String getUserStatus(TLRPC.User user) {
        if (user == null || user.status == null) {
            return "last seen recently";
        }
        
        if (user.status instanceof TLRPC.TL_userStatusOnline) {
            return "online";
        } else if (user.status instanceof TLRPC.TL_userStatusOffline) {
            return "last seen recently";
        } else if (user.status instanceof TLRPC.TL_userStatusRecently) {
            return "last seen recently";
        } else if (user.status instanceof TLRPC.TL_userStatusLastWeek) {
            return "last seen within a week";
        } else if (user.status instanceof TLRPC.TL_userStatusLastMonth) {
            return "last seen within a month";
        }
        
        return "last seen recently";
    }
    
    public void setOnMemberClickListener(OnMemberClickListener listener) {
        this.memberClickListener = listener;
    }
    
    public void setOnAddMembersClickListener(OnAddMembersClickListener listener) {
        this.addMembersClickListener = listener;
    }
    
    public void updateTheme() {
        // Update all member row colors for theme changes
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof LinearLayout) {
                updateMemberRowTheme((LinearLayout) child);
            }
        }
    }
    
    private void updateMemberRowTheme(LinearLayout memberRow) {
        for (int i = 0; i < memberRow.getChildCount(); i++) {
            View child = memberRow.getChildAt(i);
            if (child instanceof TextView) {
                TextView textView = (TextView) child;
                String text = textView.getText().toString();
                
                if ("Add Members".equals(text)) {
                    textView.setTextColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, resourcesProvider));
                } else if ("admin".equals(text)) {
                    textView.setTextColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, resourcesProvider));
                } else {
                    // Regular text color
                    textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
                }
            } else if (child instanceof ImageView) {
                ImageView imageView = (ImageView) child;
                if (imageView.getDrawable() != null) {
                    imageView.setColorFilter(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, resourcesProvider));
                }
            }
        }
    }
}
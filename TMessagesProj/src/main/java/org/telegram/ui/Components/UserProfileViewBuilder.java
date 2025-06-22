package org.telegram.ui.components;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;

/**
 * Helper for building the new user profile layout as per Telegram redesign mockups.
 * Extend this for other profile types as needed.
 */
public class UserProfileViewBuilder {
    /**
     * Builds the new user profile layout. Integrate this in ProfileActivity.createView().
     * @param context Android context
     * @param themeProvider Telegram Theme.ResourcesProvider or null
     * @param nameText the user's name
     * @param usernameText the user's username
     * @param user the user object
     * @return the root view for the user profile
     */
    public static View buildUserProfileLayout(
            Context context,
            Theme.ResourcesProvider themeProvider,
            String nameText,
            String usernameText,
            TLRPC.User user
    ) {
        // Root layout
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite, themeProvider));

        // Example: Profile cover or header (placeholder)
        FrameLayout header = new FrameLayout(context);
        header.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                AndroidUtilities.dp(220)));
        header.setBackgroundColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue, themeProvider));
        // TODO: Add cover image, gradient, or color as per mockup
        root.addView(header);

        // Example: Avatar
        ImageView avatar = new ImageView(context);
        FrameLayout.LayoutParams avatarParams = new FrameLayout.LayoutParams(
                AndroidUtilities.dp(96), AndroidUtilities.dp(96));
        avatarParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        avatarParams.bottomMargin = -AndroidUtilities.dp(48);
        avatar.setLayoutParams(avatarParams);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setBackgroundColor(Color.LTGRAY); // Placeholder
        // TODO: Load user's avatar image using user.photo
        header.addView(avatar);

        // Example: Name and username
        LinearLayout infoLayout = new LinearLayout(context);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        infoLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        infoLayout.setPadding(0, AndroidUtilities.dp(56), 0, 0);

        TextView nameView = new TextView(context);
        nameView.setText(nameText); // Set actual name
        nameView.setTextSize(22);
        nameView.setTextColor(Theme.getColor(Theme.key_profile_title, themeProvider));
        nameView.setGravity(Gravity.CENTER);
        infoLayout.addView(nameView);

        TextView usernameView = new TextView(context);
        usernameView.setText(usernameText); // Set actual username
        usernameView.setTextSize(15);
        usernameView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3, themeProvider));
        usernameView.setGravity(Gravity.CENTER);
        infoLayout.addView(usernameView);

        root.addView(infoLayout);

        // TODO: Add action buttons (call, message, gift, etc.) using provided SVG icons
        // TODO: Add tabs (e.g., Media, Files, Links, Gifts) as per mockup
        // TODO: Add about/bio, shared media, and other sections

        return root;
    }
}

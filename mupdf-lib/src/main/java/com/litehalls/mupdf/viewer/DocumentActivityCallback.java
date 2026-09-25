package com.litehalls.mupdf.viewer;

import android.content.Context;
import android.net.Uri;

/**
 * Callback interface for custom actions in DocumentActivity.
 * Implement this interface to handle custom menu items and override default behaviors.
 */
public interface DocumentActivityCallback {
    
    /**
     * Called when a custom menu item is clicked.
     * 
     * @param context The activity context
     * @param menuItemId The ID of the menu item that was clicked
     * @param documentUri The URI of the current document
     * @param documentTitle The title of the current document
     */
    void onCustomMenuItemClick(Context context, String menuItemId, Uri documentUri, String documentTitle);
    
    /**
     * Called when the share button is clicked (only if custom share is enabled).
     * Return true to override default share behavior, false to use default.
     * 
     * @param context The activity context
     * @param documentUri The URI of the document to share
     * @param documentTitle The title of the document
     * @return true if you handled the share action, false to use default behavior
     */
    boolean onShareDocument(Context context, Uri documentUri, String documentTitle);
    
    /**
     * Called when user clicks thumbs up button (votes or removes vote).
     * 
     * @param context The activity context
     * @param documentUri The URI of the document
     * @param documentTitle The title of the document
     * @param isVoted true if user is voting, false if removing vote
     * @param newVoteCount The new vote count after this action
     */
    void onVoteChanged(Context context, Uri documentUri, String documentTitle, boolean isVoted, int newVoteCount);
}

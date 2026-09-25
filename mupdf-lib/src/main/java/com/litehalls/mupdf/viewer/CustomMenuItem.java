package com.litehalls.mupdf.viewer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a custom menu item for the DocumentActivity overflow menu.
 */
public class CustomMenuItem implements Parcelable {
    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_FLOATING_MESSAGE = 1;
    
    private String id;
    private String title;
    private int iconResId; // Optional icon resource ID (0 for no icon)
    private int type; // Menu item type
    private String floatingMessage; // Message to display in floating card
    private int floatingMessageStyle; // Text style flags (bold/italic)
    
    public CustomMenuItem(String id, String title) {
        this(id, title, 0, TYPE_NORMAL);
    }
    
    public CustomMenuItem(String id, String title, int iconResId) {
        this(id, title, iconResId, TYPE_NORMAL);
    }
    
    public CustomMenuItem(String id, String title, int iconResId, int type) {
        this.id = id;
        this.title = title;
        this.iconResId = iconResId;
        this.type = type;
    }
    
    /**
     * Create a floating message menu item.
     * @param id Menu item ID
     * @param title Menu title
     * @param message Message to show in floating card
     * @param style Text style (Typeface.BOLD, Typeface.ITALIC, Typeface.BOLD_ITALIC, or Typeface.NORMAL)
     */
    public static CustomMenuItem createFloatingMessage(String id, String title, String message, int style) {
        CustomMenuItem item = new CustomMenuItem(id, title, 0, TYPE_FLOATING_MESSAGE);
        item.floatingMessage = message;
        item.floatingMessageStyle = style;
        return item;
    }
    
    public String getId() {
        return id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public int getIconResId() {
        return iconResId;
    }
    
    public int getType() {
        return type;
    }
    
    public String getFloatingMessage() {
        return floatingMessage;
    }
    
    public int getFloatingMessageStyle() {
        return floatingMessageStyle;
    }
    
    // Parcelable implementation
    protected CustomMenuItem(Parcel in) {
        id = in.readString();
        title = in.readString();
        iconResId = in.readInt();
        type = in.readInt();
        floatingMessage = in.readString();
        floatingMessageStyle = in.readInt();
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeInt(iconResId);
        dest.writeInt(type);
        dest.writeString(floatingMessage);
        dest.writeInt(floatingMessageStyle);
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    public static final Creator<CustomMenuItem> CREATOR = new Creator<CustomMenuItem>() {
        @Override
        public CustomMenuItem createFromParcel(Parcel in) {
            return new CustomMenuItem(in);
        }
        
        @Override
        public CustomMenuItem[] newArray(int size) {
            return new CustomMenuItem[size];
        }
    };
}

package com.motondon.contactsmanagerapp.view.adapter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.widget.CursorAdapter;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.QuickContactBadge;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.motondon.contactsmanagerapp.R;
import com.motondon.contactsmanagerapp.view.fragment.ContactsListFragment;

import java.util.Locale;

/**
 * This is a subclass of CursorAdapter that supports binding Cursor columns to a view layout.
 * If those items are part of search results, the search string is marked by highlighting the
 * query text. An {@link AlphabetIndexer} is used to allow quicker navigation up and down the
 * ListView.
 *
 * This class was copied by the Android Training about Contacts Provider.
 * You can find it at: https://developer.android.com/training/contacts-provider/retrieve-names.html
 * 
 * It was slightly changed to better fit on this app. Check link above for implementation details.
 * 
 *
 */
public class ContactsListAdapter extends CursorAdapter implements SectionIndexer {

    private static final String TAG = ContactsListAdapter.class.getSimpleName();

    private final LayoutInflater mInflater; // Stores the layout inflater
    private final AlphabetIndexer mAlphabetIndexer; // Stores the AlphabetIndexer instance
    private final TextAppearanceSpan highlightTextSpan; // Stores the highlight text appearance style

    private String mSearchTerm;
    /**
     * Instantiates a new Contacts Adapter.
     * @param context A context that has access to the app's layout.
     */
    public ContactsListAdapter(Context context) {
        super(context, null, 0);

        Log.v(TAG, "ContactsListAdapter::ctor() - Begin");

        // Stores inflater for use later
        mInflater = LayoutInflater.from(context);

        // Loads a string containing the English alphabet. To fully localize the app, provide a
        // strings.xml file in res/values-<x> directories, where <x> is a locale. In the file,
        // define a string with android:name="alphabet" and contents set to all of the
        // alphabetic characters in the language in their proper sort order, in upper case if
        // applicable.
        final String alphabet = context.getString(R.string.alphabet);

        // Instantiates a new AlphabetIndexer bound to the column used to sort menu_contact_details names.
        // The cursor is left null, because it has not yet been retrieved.
        mAlphabetIndexer = new AlphabetIndexer(null, ContactsListFragment.ContactsQuery.SORT_KEY, alphabet);

        // Defines a span for highlighting the part of a display name that matches the search
        // string
        highlightTextSpan = new TextAppearanceSpan(context, R.style.searchTextHiglight);

        Log.v(TAG, "ContactsListAdapter::ctor() - End");
    }

    /**
     * Identifies the start of the search string in the display name column of a Cursor row.
     * E.g. If displayName was "Adam" and search query (mSearchTerm) was "da" this would
     * return 1.
     *
     * @param displayName The menu_contact_details display name.
     * @return The starting position of the search string in the display name, 0-based. The
     * method returns -1 if the string is not found in the display name, or if the search
     * string is empty or null.
     */
    private int indexOfSearchQuery(String displayName) {
        if (!TextUtils.isEmpty(mSearchTerm)) {
            return displayName.toLowerCase(Locale.getDefault()).indexOf(
                    mSearchTerm.toLowerCase(Locale.getDefault()));
        }
        return -1;
    }

    public void setSearchTerm(String searchTerm) {
        this.mSearchTerm = searchTerm;
    }

    /**
     * Overrides newView() to inflate the list item views.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        // Inflates the list item layout.
        final View itemLayout =
                mInflater.inflate(R.layout.contact_list_item, viewGroup, false);

        // Creates a new ViewHolder in which to store handles to each view resource. This
        // allows bindView() to retrieve stored references instead of calling findViewById for
        // each instance of the layout.
        final ViewHolder holder = new ViewHolder();
        holder.text1 = (TextView) itemLayout.findViewById(android.R.id.text1);
        holder.text2 = (TextView) itemLayout.findViewById(android.R.id.text2);
        holder.icon = (QuickContactBadge) itemLayout.findViewById(android.R.id.icon);

        // This will remove bottom right corner arrow
        // See: http://stackoverflow.com/questions/14789194/quickcontactbadge-overlay
        holder.icon.setOverlay(null);

        // Stores the resourceHolder instance in itemLayout. This makes resourceHolder
        // available to bindView and other methods that receive a handle to the item view.
        itemLayout.setTag(holder);

        // Returns the item layout view
        return itemLayout;
    }

    /**
     * Binds data from the Cursor to the provided view.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Gets handles to individual view resources
        final ViewHolder holder = (ViewHolder) view.getTag();

        // For Android 3.0 and later, gets the thumbnail image Uri from the current Cursor row.
        // For platforms earlier than 3.0, this isn't necessary, because the thumbnail is
        // generated from the other fields in the row.
        //final String photoUri = cursor.getString(ContactsListFragment.ContactsQuery.PHOTO_THUMBNAIL_DATA);
        final String displayName = cursor.getString(ContactsListFragment.ContactsQuery.DISPLAY_NAME);

        String firstLetter = displayName.substring(0, 1).toUpperCase();

        ColorGenerator generator = ColorGenerator.MATERIAL; // or use DEFAULT
        // generate random color
        int color1 = generator.getRandomColor();
        TextDrawable drawable = TextDrawable.builder().buildRound(firstLetter, color1);
        holder.icon.setImageDrawable(drawable);


        final int startIndex = indexOfSearchQuery(displayName);

        if (startIndex == -1) {
            // If the user didn't do a search, or the search string didn't match a display
            // name, show the display name without highlighting
            holder.text1.setText(displayName);

            if (TextUtils.isEmpty(mSearchTerm)) {
                // If the search search is empty, hide the second line of text
                holder.text2.setVisibility(View.GONE);
            } else {
                // Shows a second line of text that indicates the search string matched
                // something other than the display name
                holder.text2.setVisibility(View.VISIBLE);
            }
        } else {
            // If the search string matched the display name, applies a SpannableString to
            // highlight the search string with the displayed display name

            // Wraps the display name in the SpannableString
            final SpannableString highlightedName = new SpannableString(displayName);

            // Sets the span to start at the starting point of the match and end at "length"
            // characters beyond the starting point
            highlightedName.setSpan(highlightTextSpan, startIndex,
                    startIndex + mSearchTerm.length(), 0);

            // Binds the SpannableString to the display name View object
            holder.text1.setText(highlightedName);

            // Since the search string matched the name, this hides the secondary message
            holder.text2.setVisibility(View.GONE);
        }

        // Processes the QuickContactBadge. A QuickContactBadge first appears as a menu_contact_details's
        // thumbnail image with styling that indicates it can be touched for additional
        // information. When the user clicks the image, the badge expands into a dialog box
        // containing the menu_contact_details's details and icons for the built-in apps that can handle
        // each detail type.

        // Generates the menu_contact_details lookup Uri
        final Uri contactUri = ContactsContract.Contacts.getLookupUri(
                cursor.getLong(ContactsListFragment.ContactsQuery.ID),
                cursor.getString(ContactsListFragment.ContactsQuery.LOOKUP_KEY));

        // Binds the menu_contact_details's lookup Uri to the QuickContactBadge
        holder.icon.assignContactUri(contactUri);

        // Loads the thumbnail image pointed to by photoUri into the QuickContactBadge in a
        // background worker thread
        //mImageLoader.loadImage(photoUri, holder.icon);
    }

    /**
     * Overrides swapCursor to move the new Cursor into the AlphabetIndex as well as the
     * CursorAdapter.
     */
    @Override
    public Cursor swapCursor(Cursor newCursor) {
        // Update the AlphabetIndexer with new cursor as well
        mAlphabetIndexer.setCursor(newCursor);
        return super.swapCursor(newCursor);
    }

    /**
     * An override of getCount that simplifies accessing the Cursor. If the Cursor is null,
     * getCount returns zero. As a result, no test for Cursor == null is needed.
     */
    @Override
    public int getCount() {
        if (getCursor() == null) {
            return 0;
        }
        return super.getCount();
    }

    /**
     * Defines the SectionIndexer.getSections() interface.
     */
    @Override
    public Object[] getSections() {
        return mAlphabetIndexer.getSections();
    }

    /**
     * Defines the SectionIndexer.getPositionForSection() interface.
     */
    @Override
    public int getPositionForSection(int i) {
        if (getCursor() == null) {
            return 0;
        }
        return mAlphabetIndexer.getPositionForSection(i);
    }

    /**
     * Defines the SectionIndexer.getSectionForPosition() interface.
     */
    @Override
    public int getSectionForPosition(int i) {
        if (getCursor() == null) {
            return 0;
        }
        return mAlphabetIndexer.getSectionForPosition(i);
    }

    /**
     * A class that defines fields for each resource ID in the list item layout. This allows
     * ContactsAdapter.newView() to store the IDs once, when it inflates the layout, instead of
     * calling findViewById in each iteration of bindView.
     */
    private class ViewHolder {
        TextView text1;
        TextView text2;
        QuickContactBadge icon;
    }
}
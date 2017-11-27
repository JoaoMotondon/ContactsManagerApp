package com.motondon.contactsmanagerapp.view.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.motondon.contactsmanagerapp.R;
import com.motondon.contactsmanagerapp.util.Constants;
import com.motondon.contactsmanagerapp.view.activity.ContactAddEditActivity;
import com.motondon.contactsmanagerapp.view.activity.ContactDetailsActivity;
import com.motondon.contactsmanagerapp.view.adapter.ContactsListAdapter;

public class ContactsListFragment extends android.support.v4.app.Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = ContactsListFragment.class.getSimpleName();

    private static final String SEARCH_KEY = "search";

    private ContactsListAdapter mAdapter;

    // Used to restore a search string in case of orientation change
    private SearchView mSearchView;
    private String mSearchString;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate() - Begin");

        super.onCreate(savedInstanceState);

        if ((ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED))) {

            requestPermissions(new String[]{
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.WRITE_CONTACTS},
                    Constants.FULL_PERMISSION);
        }

        // Enable menu
        this.setHasOptionsMenu(true);

        // retrieve search string  (if any) after an orientation change
        if (savedInstanceState != null) {
            mSearchString = savedInstanceState.getString(SEARCH_KEY);
            Log.d(TAG, "onCreate() - Retrieved search string: " + mSearchString);
        }

        Log.v(TAG, "onCreate() - End");
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume() - Restart Loader...");
        super.onResume();

        // Restart loader whenever this fragment becomes visible.
        getLoaderManager().restartLoader(ContactsQuery.QUERY_ID, null, ContactsListFragment.this);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView() - Begin");

        View root = inflater.inflate(R.layout.fragment_contact_list, container, false);

        ListView lvContactsList = (ListView) root.findViewById(R.id.lvContactsList);

        FloatingActionButton fabAddContact = (FloatingActionButton) root.findViewById(R.id.fab_add_contact);

        fabAddContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d(TAG, "fabAddContact::onClick() - Calling AddContactActivity activity...");
                onAddContact();
            }
        });

        // Create the main contacts adapter
        mAdapter = new ContactsListAdapter(getActivity());
        lvContactsList.setAdapter(mAdapter);
        lvContactsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = mAdapter.getCursor();
                cursor.moveToPosition(position);

                String lookupKey = cursor.getString(ContactsQuery.LOOKUP_KEY);

                // When a contact is clicked, call ContactDetailsActivity class which will query Contacts Provider for the contact details.
                Intent intent = new Intent(getActivity(), ContactDetailsActivity.class);
                intent.putExtra(Constants.LOOKUP_KEY, lookupKey); // This will be used to query the right contact details.
                intent.putExtra(Constants.CONTACT_NAME,  cursor.getString(ContactsQuery.DISPLAY_NAME)); // Used only for log purpose.
                startActivity(intent);
            }
        });

        getLoaderManager().initLoader(ContactsQuery.QUERY_ID, null, this);

        Log.v(TAG, "onCreateView() - End");

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSearchView = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.v(TAG, "onSaveInstanceState() - Begin");

        super.onSaveInstanceState(outState);

        // Save search string (if any) before an orientation change. Restore it later in the onViewCreated() method.
        if (mSearchView != null && mSearchView.getQuery() != null) {
            mSearchString = mSearchView.getQuery().toString();

            if (!mSearchString.isEmpty()) {
                Log.d(TAG, "onSaveInstanceState() - Saving search string " + mSearchString + "...");
            }
            outState.putString(SEARCH_KEY, mSearchString);
        }

        Log.v(TAG, "onSaveInstanceState() - End");
    }

    /**
     * Process search menu item
     *
     * @param menu
     * @param inflater
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.v(TAG, "onCreateOptionsMenu() - Begin");

        inflater.inflate(R.menu.menu_contact_list, menu);

        // Configure SearchView by adding a listener for onTextChange and onTextSubmit
        MenuItem searchMenuItem = menu.findItem(R.id.searchContacts);
        mSearchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);

        if (mSearchView != null) {
            mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    return false;
                }
                @Override
                public boolean onQueryTextChange(String newText) {
                    Log.d(TAG, "onCreateOptionsMenu()::onQueryTextChange() - String: " + newText);

                    String newFilter = !TextUtils.isEmpty(newText) ? newText : null;

                    // If filter is empty, there is no need to do anything
                    if (mSearchString == null && newFilter == null) {
                        return false;
                    }

                    // If the new filter is the equals than the current filter, just return
                    if (mSearchString != null && mSearchString.equals(newFilter)) {
                        return false;
                    }

                    // Updates current filter to new filter
                    mSearchString = newFilter;
                    mAdapter.setSearchTerm(mSearchString);

                    // Restarts the loader. This triggers onCreateLoader(), which builds the
                    // necessary content Uri from mSearchTerm.
                    getLoaderManager().restartLoader(
                            ContactsQuery.QUERY_ID, null, ContactsListFragment.this);
                    return false;
                }
            });
        }

        // This method (onCreateOptionsMenu) is called at the startup and also after an orientation change. Normally mSearchString will contain some string
        // if user is making a search and rotate the device. Then, we must keep same search.
        if (mSearchString != null && !mSearchString.isEmpty()) {
            Log.d(TAG, "onCreateOptionsMenu() - mSearchString not null: " + mSearchString);

            // Stores the search term (as it will be wiped out by
            // onQueryTextChange() when the menu item is expanded).
            final String savedSearchTerm = mSearchString;

            searchMenuItem.expandActionView();
            mSearchView.setQuery(savedSearchTerm, true);
            mSearchView.clearFocus();
        }

        Log.v(TAG, "onDestroyView() - End");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(TAG, "onOptionsItemSelected() - Begin");

        switch (item.getItemId()) {
            case R.id.menu_settings:
                Log.d(TAG, "onOptionsItemSelected() - Menu settings clicked");
                // Currently doing nothing, but if you want to add any kind of settings, here is the place for you to add your code.
                return true;

            case R.id.menu_sync_contacts:
                Log.d(TAG, "onOptionsItemSelected() - Menu sync contacts clicked. Calling Loader::restartLoader() method...");
                // The idea for this menu item it to re-query contacts provider, since during our development we sometimes changed (add/change/remove)
                // a contact in the official contact app and then checked on this app for the changes. So, this button was so useful. Maybe
                // it can be removed now.
                getLoaderManager().restartLoader(ContactsQuery.QUERY_ID, null, this);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Support to Marshmallow version and higher
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.v(TAG, "onRequestPermissionsResult() - Begin");

        switch (requestCode) {
            case Constants.FULL_PERMISSION:
                // TODO: We should treat here when user does not grant access to READ_CONTACTS and/or WRITE_CONTACTS
                break;
        }
    }

    private void onAddContact() {
        Log.d(TAG, "onAddContact() - Calling " + ContactAddEditActivity.class.getSimpleName() + "...");
        Intent i = new Intent(getActivity(), ContactAddEditActivity.class);
        startActivity(i);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.v(TAG, "onCreateLoader() - Begin");

        // This is the only supported ID, so, maybe we could remove this if clause.
        if (id == ContactsQuery.QUERY_ID) {
            Uri contentUri;

            // There are two types of searches, one which displays all contacts and
            // another one which filters contacts by a search query. If mSearchTerm is set
            // then a search query has been entered and it should be used.

            if (mSearchString == null || mSearchString.isEmpty()) {
                Log.d(TAG, "onCreateLoader() - mSearchString is null. Requesting all the contacts...");
                // Since there's no search string, use the content URI that searches the entire
                // Contacts table
                contentUri = ContactsQuery.CONTENT_URI;
            } else {
                Log.d(TAG, "onCreateLoader() - mSearchString: "  + mSearchString + ". Requesting only contacts that match to this string...");
                // Since there's a search string, use the special content Uri that searches the
                // Contacts table. The URI consists of a base Uri and the search string.
                contentUri = Uri.withAppendedPath(ContactsQuery.FILTER_URI, Uri.encode(mSearchString));
            }

            // Query the contact provider for the contacts list
            return new CursorLoader(getActivity(),
                    contentUri,
                    ContactsQuery.PROJECTION,
                    ContactsQuery.SELECTION,
                    null,
                    ContactsQuery.SORT_ORDER);
        }

        Log.v(TAG, "onCreateLoader() - End");

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.v(TAG, "onLoadFinished()");

        // This swaps the new cursor into the adapter.
        if (loader.getId() == ContactsQuery.QUERY_ID) {
            Log.e(TAG, "onLoadFinished - Swapping cursor...");
            mAdapter.swapCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.v(TAG, "onLoaderReset()");

        if (loader.getId() == ContactsQuery.QUERY_ID) {
            // When the loader is being reset, clear the cursor from the adapter. This allows the
            // cursor resources to be freed.
            Log.e(TAG, "onLoaderReset - Reset cursor...");
            mAdapter.swapCursor(null);
        }
    }

    /**
     * This interface defines constants for the Cursor and CursorLoader, based on constants defined
     * in the {@link android.provider.ContactsContract.Contacts} class.
     * 
     * This interface was copied by the Android Training about Contacts Provider. We kept all the comments for a better understanding.
     * You can find it at: https://developer.android.com/training/contacts-provider/retrieve-names.html
     *  
     */
    public interface ContactsQuery {

        // An identifier for the loader
        int QUERY_ID = 1;

        // A content URI for the Contacts table
        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;

        // The search/filter query Uri
        Uri FILTER_URI = ContactsContract.Contacts.CONTENT_FILTER_URI;

        // The selection clause for the CursorLoader query. The search criteria defined here
        // restrict results to contacts that have a display name and are linked to visible groups.
        // Notice that the search on the string provided by the user is implemented by appending
        // the search string to CONTENT_FILTER_URI.
        @SuppressLint("InlinedApi")
        String SELECTION =ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + "<>''" + " AND " + ContactsContract.Contacts.IN_VISIBLE_GROUP + "=1";

        // The desired sort order for the returned Cursor. In Android 3.0 and later, the primary
        // sort key allows for localization. In earlier versions. use the display name as the sort
        // key.
        @SuppressLint("InlinedApi")
        String SORT_ORDER = ContactsContract.Contacts.SORT_KEY_PRIMARY;

        // The projection for the CursorLoader query. This is a list of columns that the Contacts
        // Provider should return in the Cursor.
        @SuppressLint("InlinedApi")
        String[] PROJECTION = {

                // The menu_contact_details's row id
                ContactsContract.Contacts._ID,

                // A pointer to the menu_contact_details that is guaranteed to be more permanent than _ID. Given
                // a menu_contact_details's current _ID value and LOOKUP_KEY, the Contacts Provider can generate
                // a "permanent" menu_contact_details URI.
                ContactsContract.Contacts.LOOKUP_KEY,

                // In platform version 3.0 and later, the Contacts table contains
                // DISPLAY_NAME_PRIMARY, which either contains the menu_contact_details's displayable name or
                // some other useful identifier such as an email address. This column isn't
                // available in earlier versions of Android, so you must use Contacts.DISPLAY_NAME
                // instead.
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,

                // In Android 3.0 and later, the thumbnail image is pointed to by
                // PHOTO_THUMBNAIL_URI. In earlier versions, there is no direct pointer; instead,
                // you generate the pointer from the menu_contact_details's ID value and constants defined in
                // android.provider.ContactsContract.Contacts.
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,

                // The sort order column for the returned Cursor, used by the AlphabetIndexer
                SORT_ORDER,
        };

        // The query column numbers which map to each value in the projection
        int ID = 0;
        int LOOKUP_KEY = 1;
        int DISPLAY_NAME = 2;
        int PHOTO_THUMBNAIL_DATA = 3;
        int SORT_KEY = 4;
    }
}

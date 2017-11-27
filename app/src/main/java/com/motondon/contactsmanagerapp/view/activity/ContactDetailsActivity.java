package com.motondon.contactsmanagerapp.view.activity;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;

import com.motondon.contactsmanagerapp.R;
import com.motondon.contactsmanagerapp.view.fragment.ContactDetailsFragment;

import java.io.IOException;

public class ContactDetailsActivity extends AppCompatActivity {

    private static final String TAG = ContactDetailsActivity.class.getSimpleName();

    private ImageView contactImage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container_scrollable);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        
        // This attribute will be used to change the contact photo when user add or change it in the ContactsAddEditFragment.
        contactImage = (ImageView) findViewById(R.id.contact_image);

        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        ContactDetailsFragment fragment = (ContactDetailsFragment) getSupportFragmentManager().findFragmentByTag(ContactDetailsFragment.TAG);
        if (fragment == null) {
            try {
                fragment = new ContactDetailsFragment();
                fragment.setArguments(getIntent().getExtras());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.flContent, fragment, ContactDetailsFragment.TAG).commit();
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            // In case user clicks on "home" menu option, return false, so it will be handled by the fragment, which will
            // take some actions.
            case android.R.id.home:
                return false;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * When user changes a contact name (in the ContactsAddEditFragment class), when go back to the ContactsDetailFragment, this method will be
     * called to update action bar title.
     * 
     * @param contactName
     */
    public void setActivityTitle(String contactName) {
        Log.d(TAG, "setActivityTitle() - contactName: " + contactName);
        getSupportActionBar().setTitle(contactName);
    }

    /**
     * When user adds or changes a contact photo(in the ContactsAddEditFragment class), when go back to the ContactsDetailFragment, this method will be 
     * called to update the image.
     * 
     * @param contactImageUri
     */
    public void setContactImage(String contactImageUri) {
        Log.v(TAG, "setContactImage() - Begin");
        try {

            Log.d(TAG, "setContactImage() - Getting bitmap from URI: " + contactImageUri);

            // Get the bitmap from the content URI
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(contactImageUri));

            contactImage.setImageBitmap(bitmap);
            contactImage.setImageURI(Uri.parse(contactImageUri));

        } catch (IOException e) {
            Log.w(TAG, "setContactImage() - Fail while trying to get bitmap from URI: " + contactImageUri + ". Error: " + e.getMessage());
        }

        Log.v(TAG, "setContactImage() - End");
    }
}

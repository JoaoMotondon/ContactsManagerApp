package com.motondon.contactsmanagerapp.view.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.motondon.contactsmanagerapp.R;
import com.motondon.contactsmanagerapp.view.fragment.ContactAddEditFragment;

public class ContactAddEditActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container_basic);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        // When there is no extra, means this class was called in order to to add a new contact. So change the title accordingly.
        if (getIntent().getExtras() == null) {
            setTitle(R.string.title_activity_add_contact);
        } else {
        	// If there is an extra object, set title to "Edit contact".
            setTitle(getString(R.string.title_activity_edit_contact));
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        ContactAddEditFragment fragment = (ContactAddEditFragment) getSupportFragmentManager().findFragmentByTag(ContactAddEditFragment.TAG);
        if (fragment == null) {
            try {
                fragment = new ContactAddEditFragment();
                fragment.setArguments(getIntent().getExtras());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.flContent, fragment, ContactAddEditFragment.TAG).commit();
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
}

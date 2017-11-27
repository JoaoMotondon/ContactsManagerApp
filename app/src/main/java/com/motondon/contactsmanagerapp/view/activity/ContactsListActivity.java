package com.motondon.contactsmanagerapp.view.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.motondon.contactsmanagerapp.R;
import com.motondon.contactsmanagerapp.view.fragment.ContactsListFragment;

public class ContactsListActivity extends AppCompatActivity {

    private static final String TAG = ContactsListActivity.class.getSimpleName();
    private static final String STATE_SELECTED_POSITION = "STATE_SELECTED_POSITION";

    private Integer mCurrentSelectedPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container_basic);

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
        }

       Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        setTitle("Contacts Manager");

        ContactsListFragment mContactsListFragment = (ContactsListFragment) getSupportFragmentManager().findFragmentByTag(ContactsListFragment.TAG);
        if (mContactsListFragment == null) {
            mContactsListFragment =  new ContactsListFragment();
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.flContent, mContactsListFragment, ContactsListFragment.TAG).commit();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION, -1);


    }
}

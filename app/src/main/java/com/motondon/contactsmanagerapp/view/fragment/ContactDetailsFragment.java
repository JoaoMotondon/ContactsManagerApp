package com.motondon.contactsmanagerapp.view.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.motondon.contactsmanagerapp.R;
import com.motondon.contactsmanagerapp.model.Contact;
import com.motondon.contactsmanagerapp.model.Email;
import com.motondon.contactsmanagerapp.model.Phone;
import com.motondon.contactsmanagerapp.model.Photo;
import com.motondon.contactsmanagerapp.model.PostalAddress;
import com.motondon.contactsmanagerapp.provider.ContactsManager;
import com.motondon.contactsmanagerapp.util.Constants;
import com.motondon.contactsmanagerapp.view.activity.ContactAddEditActivity;
import com.motondon.contactsmanagerapp.view.activity.ContactDetailsActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class ContactDetailsFragment extends Fragment {

    public static final String TAG = ContactDetailsFragment.class.getSimpleName();

    //private LinearLayout mContainer;
    private View mViewRoot;
    private LayoutInflater mInflater;
    private Activity mActivity;

    private Contact mContact;
    private String mContactName;

    private String mLookupKey;

    // Handler to receive Intents related to contact actions. These actions are sent when a contact is changed or deleted, so that
    // this fragment can take the right action.
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            Log.d(TAG, "ContactDetailsFragment::BroadcastReceiver::onReceive() - Got action: " + action);

            switch (action) {

                case Constants.CONTACT_UPDATED:
                    Contact updatedContact = (Contact) intent.getSerializableExtra(Constants.CONTACT);
                    Log.d(TAG, "Contact " + updatedContact.getDisplayName() + " updated successfully. Updating views...");

                    // When a contact was updated we must update all the views on this fragment.
                    updateContactDetailsViews(updatedContact);
                    //Snackbar.make(mContainer, "Contact " + updatedContact.getDisplayName() + " updated successfully", Snackbar.LENGTH_SHORT).show(); // Don’t forget to show!
                    break;

                // A contact can be deleted by either this fragment or by ContactAddEditFragment. No matter which fragment deletes a contact, this broadcast will
                // receive the intent. So, close this fragment and let the ContactsListFragment to catch this event and re-query contacts provider in order to
                // update its contacts list.
                case Constants.CONTACT_DELETED:
                    Contact deletedContact = (Contact) intent.getSerializableExtra(Constants.CONTACT);
                    Log.d(TAG, "Contact " + deletedContact.getDisplayName() + " deleted successfully. Closing this fragment...");

                    mActivity.finish();
                    break;
            }
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate() - Begin");

        super.onCreate(savedInstanceState);

        // Enable menu
        this.setHasOptionsMenu(true);

        Log.d(TAG, "onCreate() - Registering Broadcast receivers...");

        // Register the broadcasters 
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.CONTACT_UPDATED);
        intentFilter.addAction(Constants.CONTACT_DELETED);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mMessageReceiver, intentFilter);

        Log.v(TAG, "onCreate() - End");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView() - Begin");

        // Set LayoutInflater and rootView in the class scope, since they will be used later when adding/removing dynamic views.
        this.mInflater = inflater;
        mViewRoot = inflater.inflate(R.layout.fragment_contact_details, container, false);

        // Used for the snackbar
        //mContainer = (LinearLayout) mViewRoot.findViewById(R.id.edit_contact_container);

        // Restore the contact after a configuration change
        if (savedInstanceState != null) {
            Log.d(TAG, "onCreateView() - savedInstanceState IS NOT NULL. Retrieving mContact from it...");
            Contact contact = (Contact) savedInstanceState.getSerializable(Constants.CONTACT);

            // Update views after a screen rotate.
            updateContactDetailsViews(contact);

            mLookupKey = mContact.getLookupKey();
            mContactName = mContact.getDisplayName();

        } else {
            // Create and populate the args bundle
            final Bundle extras = getArguments();
            mLookupKey = extras.getString(Constants.LOOKUP_KEY);
            mContactName = extras.getString(Constants.CONTACT_NAME);

            // Only load the contact's details when savedInstanceState is null. When it is not null, contact's details were already retrieved from the savedInstanceState bundle.

            // Start an AsyncTask which will query the Contacts Provider in a worker thread. 
            Log.d(TAG, "onCreateView() - Fetching contact details for contact: " + mContactName);
            new ContactDetailsTask().execute(Constants.ACTION_FETCH);
        }

        ((ContactDetailsActivity)getActivity()).setActivityTitle(mContactName);

        Log.v(TAG, "onCreateView() - End");

        return mViewRoot;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mContact != null) {
            Log.d(TAG, "onResume() - contact display name: " + mContact.getDisplayName());
            ((ContactDetailsActivity) getActivity()).setActivityTitle(mContact.getDisplayName());
        }
    }

    @Override
    public void onAttach(Context context) {
        Log.v(TAG, "onAttach()");

        super.onAttach(context);
        if (context instanceof Activity){
            mActivity  = (Activity) context;
        }
    }

    @Override
    public void onDetach() {
        Log.v(TAG, "onDetach()");

        super.onDetach();
        mActivity = null;
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy() - Un-registering broadcast receiver...");
        super.onDestroy();
        
        // Do not forget to un-register the broadcast.
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mMessageReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.v(TAG, "onSaveInstanceState() - Begin");

        super.onSaveInstanceState(outState);
        
        // Save the current contact before a screen rotate.
        outState.putSerializable(Constants.CONTACT, mContact);

        Log.v(TAG, "onSaveInstanceState() - End");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.v(TAG, "onCreateOptionsMenu()");
        inflater.inflate(R.menu.menu_contact_details, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(TAG, "onOptionsItemSelected() - Begin");

        switch (item.getItemId()) {

            case android.R.id.home:
                Log.d(TAG, "onOptionsItemSelected() - Home menu item clicked");
                getActivity().onBackPressed();
                return true;

            case R.id.menu_contact_delete:
                Log.d(TAG, "onOptionsItemSelected() - Delete Contact menu item clicked");
                onDeleteContact();
                return true;

            case R.id.menu_contact_edit:
                Log.d(TAG, "onOptionsItemSelected() - Edit Contact menu item clicked for contact " +mContactName + ". Starting TaskEditActivity...");
                onEditContact();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This action is done by an AsyncTask, in order to be executed in a worker thread.
     * 
     */
    private void onDeleteContact() {
        Log.v(TAG, "onDeleteContact() - Begin");

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getResources().getString(R.string.str_are_you_sure_you_want_to_delete_this_contact));

        builder.setPositiveButton(getResources().getString(R.string.btn_caption_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // Request Contacts Provider to delete current contact.
                Log.d(TAG, "onDeleteContact() - Starting ContactDetailsTask() task in order to request Contact Provider to delete contact " + mContact.getDisplayName());
                new ContactDetailsTask().execute(Constants.ACTION_DELETE);
            }
        });

        builder.setNegativeButton(getResources().getString(R.string.str_btn_cancel), null);

        AlertDialog dialog = builder.create();
        // display dialog
        dialog.show();

        Log.v(TAG, "onDeleteContact() - End");
    }

    /**
     * In order to edit a contact, open another fragment, which will show the user all the contacts attributes supported by this app.
     * 
     */
    private void onEditContact() {
        Log.v(TAG, "onEditContact() - Calling " + ContactAddEditActivity.class.getSimpleName() + "...");

        Intent intent = new Intent(mActivity, ContactAddEditActivity.class);
        intent.putExtra(Constants.LOOKUP_KEY, mLookupKey);
        intent.putExtra(Constants.CONTACT, mContact);
        startActivity(intent);

        Log.v(TAG, "onEditContact() - End");
    }

    /**
     * This method is called when this fragment is opened and also after a contact update. 
     * 
     * It reads Contact object attributes and update all the related views.
     * 
     * When it is called after a contact change, it will first remove all the dynamic views (e.g.: all the phones for a contact)
     * and then add another views for the contact.
     * 
     * @param contact
     */
    private void updateContactDetailsViews(Contact contact) {
        Log.v(TAG, "updateContactDetailsViews() - Contact: " + contact.getDisplayName());

        mContact = new Contact(contact);
        // ((ContactDetailsActivity)getActivity()).setActivityTitle(contact.getDisplayName());

        Photo photo = contact.getPhoto();
        if (photo.getContactImageUri() != null) {
            Log.d(TAG, "updateContactDetailsViews() - Updating contact: " + contact.getDisplayName() + " - Image URI: " + photo.getContactImageUri());
            ((ContactDetailsActivity)getActivity()).setContactImage(photo.getContactImageUri());
        }

        // Add one dynamic view for each phone
        ArrayList<Phone> phones = contact.getPhones();
        if (phones == null || phones.size() == 0) {
            Log.d(TAG, "updateContactDetailsViews() - Detected contact: " + contact.getDisplayName() + " has no phone numbers.");
            CardView phoneCardContainer = (CardView) mViewRoot.findViewById(R.id.contact_phone_card_container);
            phoneCardContainer.setVisibility(View.GONE);

        } else {
            Log.d(TAG, "updateContactDetailsViews() - Detected contact: " + contact.getDisplayName() + " has " + phones.size() + " phone number(s). Updating views...");
            LinearLayout phoneLayout = (LinearLayout) mViewRoot.findViewById(R.id.contact_phone_container);

            // First remove all phones since this method can be called after user add or edit a contact. So, the easiest way to update
            // all the phones is to first remove all of them and only then update the phones
            phoneLayout.removeAllViews();

            for (final Phone phone : phones) {
                Log.d(TAG, "updateContactDetailsViews() - Contact: " + contact.getDisplayName() + " - adding phone number: " + phone + "...");

                View view = mInflater.inflate(R.layout.element_contact_details_phone, null);
                TextView tvContactPhoneNumber = (TextView) view.findViewById(R.id.contact_phone_number);
                TextView tvContactPhoneType = (TextView) view.findViewById(R.id.contact_phone_type);

                // Set a listener which will be called when user clicks over a phone. It will call the default app (if any) to dial to the contact number
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "updateContactDetailsViews::onClick() - Request ACTION_DIAL in order to open an external app to dial to number: " + phone.getPhoneNumber() + "...");

                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + phone.getPhoneNumber()));
                        startActivity(intent);
                    }
                });
                tvContactPhoneNumber.setText(phone.getPhoneNumber());
                tvContactPhoneType.setText(phone.getPhoneType());
                
                // Now add the dynamic phone view.
                phoneLayout.addView(view);
            }
        }

        // Add one dynamic view for each email
        ArrayList<Email> emails = contact.getEmails();
        if (emails == null || emails.size() == 0) {
            Log.d(TAG, "updateContactDetailsViews() - Detected contact: " + contact.getDisplayName() + " has no e-mail addresses.");
            CardView emailCardContainer = (CardView) mViewRoot.findViewById(R.id.contact_email_card_container);
            emailCardContainer.setVisibility(View.GONE);

        } else {
            Log.d(TAG, "updateContactDetailsViews() - Detected contact: " + contact.getDisplayName() + " has " + emails.size() + " email address(es). Updating views...");
            LinearLayout mMailLayout = (LinearLayout) mViewRoot.findViewById(R.id.contact_mail_container);
            // First remove all emails since this method can be called after user add or edit a contact. So, the easiest way to update
            // all the emails is to first remove all of them and only then update the emails
            mMailLayout.removeAllViews();

            for (final Email mail : emails) {
                Log.d(TAG, "updateContactDetailsViews() - Contact: " + contact.getDisplayName() + " - adding email address: " + mail + "...");

                View view = mInflater.inflate(R.layout.element_contact_details_email, null);
                TextView tvContactEmailAddress = (TextView) view.findViewById(R.id.contact_email_address);
                TextView tvContactEmailType = (TextView) view.findViewById(R.id.contact_email_type);

              // Set a listener which will be called when user clicks over an email address. It will call the default app (if any) to send an email to the contact email
              view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "updateContactDetailsViews::onClick() - Request ACTION_SEND in order to open an external app to send an e-mail to : " + mail.getEmailAddress() + "...");

                        Intent emailIntent = new Intent(Intent.ACTION_SEND);
                        emailIntent.setType("message/rfc822");
                        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{
                                mail.getEmailAddress()
                        });
                        startActivity(Intent.createChooser(emailIntent, getString(R.string.chooser_mail_title)));
                    }
                });

                tvContactEmailAddress.setText(mail.getEmailAddress());
                tvContactEmailType.setText(mail.getEmailType());
                
                // Now add the email dynamic view
                mMailLayout.addView(view);
            }
        }

        // Add one dynamic view for each address
        ArrayList<PostalAddress> postalAddresses = contact.getAddresses();
        if (postalAddresses == null || postalAddresses.size() == 0) {
            Log.d(TAG, "updateContactDetailsViews() - Detected contact: " + contact.getDisplayName() + " has no addresses.");
            CardView addressCardContainer = (CardView) mViewRoot.findViewById(R.id.contact_address_card_container);
            addressCardContainer.setVisibility(View.GONE);

        } else {
            Log.d(TAG, "updateContactDetailsViews() - Detected contact: " + contact.getDisplayName() + " has " + postalAddresses.size() + " address(es). Updating views...");LinearLayout postalAddressLayout = (LinearLayout) mViewRoot.findViewById(R.id.contact_address_container);
            // First remove all address since this method can be called after user add or edit a contact. So, the easiest way to update
            // all the addresses is to first remove all of them and only then update the addresses
            postalAddressLayout.removeAllViews();

            for (final PostalAddress postalAddress : postalAddresses) {
                Log.d(TAG, "updateContactDetailsViews() - Contact: " + contact.getDisplayName() + " - adding address: " + postalAddress + "...");

                View view = mInflater.inflate(R.layout.element_contact_details_address_, null);
                TextView tvContactAddressName = (TextView) view.findViewById(R.id.contact_address_name);
                TextView tvContactAddressType = (TextView) view.findViewById(R.id.contact_address_type);

                // Set a listener which will be called when user clicks over an address. It will call the default maps app (if any).
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            Log.d(TAG, "updateContactDetailsViews::onClick() - Request ACTION_VIEW in order to open an external app to show geo location for address: " + postalAddress.getStreet() + "...");

                            // We expect here user entered a complete address. So, according to the documentation, when the query has a single result, we can use this
                            // intent to display a  pint at a particular place or address. We could also inform a label
                            // See link below for details:
                            // https://developers.google.com/maps/documentation/android-api/intents#display_a_map
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(
                                    "geo:0,0?q=%s", URLEncoder.encode(postalAddress.getStreet(), "UTF-8"))));
                            startActivity(intent);
                        } catch (UnsupportedEncodingException e) {
                        	Log.e(TAG, "updateContactDetailsViews::onClick() - UnsupportedEncodingException - Cannot find maps app.");
                            Toast.makeText(getContext(), "Cannot find maps app", Toast.LENGTH_LONG).show();
                        }
                    }
                });

                tvContactAddressName.setText(postalAddress.getStreet());
                tvContactAddressType.setText(postalAddress.getType());
                
                // Now add the dynamic address view
                postalAddressLayout.addView(view);
            }
        }

        Log.v(TAG, "updateContactDetailsViews() - End");
    }
    
    /**
     * This is  the AsyncTask responsible to make requests to the Contacts Provider. 
     * 
     * Actually it will delegate the requests to the ContactsManager class, but in a worker thread.
     * 
     * When task is finished, onPostExecute() method will be called and be executed in the Main thread again. It will
     * contain the contact retrieved by the Contacts Provider.
     *
     */
    private class ContactDetailsTask extends AsyncTask<String, Void, Contact> {
        public final String TAG = ContactDetailsFragment.class.getSimpleName();

        private String action;
        
        @Override
        protected Contact doInBackground(String... params) {
            Log.v(TAG, "doInBackground() - Begin");
        	
        	Contact contact = null;
        	this.action = params[0];
        	
        	switch (action) {
	        	case Constants.ACTION_DELETE:
		        	Log.d(TAG, "doInBackground() - Calling ContactsManager.onDeleteContact() method in order to request Contact Provider to delete contact " + mContact.getDisplayName());
		            ContactsManager.getInstance(getContext()).deleteContact(mContact, getActivity().getContentResolver(), mLookupKey);
		            break;

	        	case Constants.ACTION_FETCH:
		            Log.d(TAG, "doInBackground() - Calling ContactsManager.queryContactDetails() method in order to request Contact Provider to  fetch details for contact: " + mContactName);
		            contact = ContactsManager.getInstance(getContext()).queryContactDetails(mLookupKey);
		            break;
        	}

            Log.v(TAG, "doInBackground() - End");
        	
            return contact;
        }

        @Override
        protected void onPostExecute(Contact contact) {
            Log.v(TAG, "onPostExecute() - Begin");
        	
        	switch (action) {
	        	case Constants.ACTION_FETCH:
		            mContact = contact;
		            Log.d(TAG, "onPostExecute() - Action ACTION_FETCH. Populating data for contact: " + contact.getDisplayName());
		            // After get contact details from the Contacts Provider, update all the related views on this fragment.
		            updateContactDetailsViews(contact);
		            break;
	
	        	case Constants.ACTION_DELETE:
	        		// WARNING: Do not try to access contact parameter, since it is null for action ACTION_DELETE!!!
	        		// After delete a contact successfully, we just need to close the activity which holds this fragment.
	        		// Then, when ContactsListFragment gets visible again, onResume() will restart a loader in order to update its
	        		// contacts list.
		        	Log.d(TAG, "doInBackground() - Action ACTION_DELETE. Finishing fragment...");
		            mActivity.finish();
		            break;
        	}

            Log.v(TAG, "onPostExecute() - End");
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}

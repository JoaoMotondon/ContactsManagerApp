package com.motondon.contactsmanagerapp.view.fragment;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.motondon.contactsmanagerapp.R;
import com.motondon.contactsmanagerapp.model.Contact;
import com.motondon.contactsmanagerapp.model.Email;
import com.motondon.contactsmanagerapp.model.Organization;
import com.motondon.contactsmanagerapp.model.Phone;
import com.motondon.contactsmanagerapp.model.Photo;
import com.motondon.contactsmanagerapp.model.PostalAddress;
import com.motondon.contactsmanagerapp.model.Status;
import com.motondon.contactsmanagerapp.model.StructuredName;
import com.motondon.contactsmanagerapp.provider.ContactsManager;
import com.motondon.contactsmanagerapp.util.Constants;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ContactAddEditFragment extends android.support.v4.app.Fragment {

    public static final String TAG = ContactAddEditFragment.class.getSimpleName();

    private static final int TAKE_PICTURE = 1;
    private static final int SELECT_PHOTO = 2;
    private static final int CROP_PHOTO = 3;

    private LayoutInflater mInflater;
    private Activity mActivity;

    private ImageView contactImage;
    private EditText contactFirstName;
    private EditText contactLastName;
    private LinearLayout contactPhoneLayout;
    private LinearLayout contactMailLayout;
    private LinearLayout contactAddressLayout;
    private EditText contactCompanyName;
    private EditText contactCompanyTitle;

    // Initialize it here since it will not be initialized in the onCreateView if this fragment is being used to add a new contact, but
    // will be accessed later.
    private Contact mContact = new Contact();

    // This object will be used when a contact is being changed to compare if there was any change when discard or confirm button is clicked.
    private Contact mContactOrigin;

    private String mLookupKey;
    private boolean addAction;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate() - Begin");

        super.onCreate(savedInstanceState);

        // Enable menu
        this.setHasOptionsMenu(true);

        Log.v(TAG, "onCreate() - End");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView() - Begin");

        this.mInflater = inflater;
        View viewRoot = inflater.inflate(R.layout.fragment_contact_add_edit, container, false);

        // Restore the Contact object after an screen rotate
        if (savedInstanceState != null) {
            Log.d(TAG, "onCreateView() - savedInstanceState IS NOT NULL. Retrieving mContact from it...");
            mContact = (Contact) savedInstanceState.getSerializable(Constants.CONTACT);

            // Also create another contact (by value) based on the contact retrieved from the bundle. It will be used when confirm or
            // discard button is clicked.
            mContactOrigin = new Contact(mContact);
        }

        final Bundle extras = getArguments();
        if (extras == null) {
            addAction = true;
            Log.d(TAG, "onCreateView() - Detected 'mContact' IS NULL. Preparing this fragment to add a new contact...");

        } else {
            // Only take mContact from the extras if savedInstanceState is null. When it is not null (i.e.: after a screen rotate),
            // we should have taken it up already.
            if (savedInstanceState == null) {
                Log.d(TAG, "onCreateView() - Detected 'mContact' IS NULL. Getting mContact from the extras...");
                mContact = (Contact) extras.getSerializable(Constants.CONTACT);

                mContactOrigin = new Contact(mContact);
            }

            addAction = false;

            mLookupKey = extras.getString(Constants.LOOKUP_KEY);
            Log.d(TAG, "onCreateView() - Detected 'mContact' is not null (Contact: " + mContact.getDisplayName() + ". Filling up views...");
        }

        // Load views
        contactImage = (ImageView) viewRoot.findViewById(R.id.contact_photo_imageview);
        contactFirstName = (EditText) viewRoot.findViewById(R.id.et_contact_first_name);
        contactLastName = (EditText) viewRoot.findViewById(R.id.et_contact_last_name);
        contactCompanyName = (EditText) viewRoot.findViewById(R.id.et_contact_company_name);
        contactCompanyTitle = (EditText) viewRoot.findViewById(R.id.et_contact_company_title);

        contactPhoneLayout = (LinearLayout) viewRoot.findViewById(R.id.addcontact_phone_list);
        contactMailLayout = (LinearLayout) viewRoot.findViewById(R.id.addcontact_mail_list);
        contactAddressLayout = (LinearLayout) viewRoot.findViewById(R.id.edit_contact_address_list);

        // Adding listener
        final ImageButton btnAddContactPhone = (ImageButton) viewRoot.findViewById(R.id.btn_contact_add_phone);
        btnAddContactPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "btnAddContactPhone::onClick() - Calling showAddEditPhoneDialog() method...");
                // When user clicks over the phone imageButton, shows a dialog
                // in order to allow a phone to be changed/added
                showAddEditPhoneDialog(null, null);
            }
        });

        ImageButton btnAddContactEmail = (ImageButton) viewRoot.findViewById(R.id.btn_contact_add_email);
        btnAddContactEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "btnAddContactEmail::onClick() - Calling showAddEditEmailDialog() method...");
                // When user clicks over the email imageButton, shows a dialog
                // in order to allow an email to be changed/added
                showAddEditEmailDialog(null, null);
            }
        });

        ImageButton btnAddContactAddress = (ImageButton) viewRoot.findViewById(R.id.btn_contact_add_address);
        btnAddContactAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "btnAddContactAddress::onClick() - Calling showAddEditAddressDialog() method...");
                // When user clicks over the address imageButton, shows a dialog
                // in order to allow an address to be changed/added
                showAddEditAddressDialog(null, null);
            }
        });

        FloatingActionButton btnPhoto = (FloatingActionButton) viewRoot.findViewById(R.id.fab_contact_add_photo);
        btnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "btnPhoto::onClick() - Calling addContactPhoto() method...");
                addContactPhoto();
            }
        });

        // Finally, if mContact is not null (this happens only when editing a contact), update all the views with the mContact attribute values.
        if (mContact != null) {
            Log.d(TAG, "onCreateView() - Updating view with contact data...");
            updateContactViews(mContact);
        }

        Log.v(TAG, "onCreateView() - End");

        return viewRoot;
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
    public void onSaveInstanceState(Bundle outState) {
        Log.v(TAG, "onSaveInstanceState() - Begin");

        super.onSaveInstanceState(outState);
        outState.putSerializable(Constants.CONTACT, mContact);

        Log.v(TAG, "onSaveInstanceState() - End");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.v(TAG, "onCreateOptionsMenu()");
        inflater.inflate(R.menu.menu_contact_edit, menu);

        if (addAction) {
            // When adding a contact, does not show 'delete' option
            menu.removeItem(R.id.menu_contact_delete);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(TAG, "onOptionsItemSelected() - Begin");

        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.menu_contact_discard_changes:
                Log.d(TAG, "onOptionsItemSelected() - Discard Contact changes menu item clicked");

                // First check if any contact attribute has changed. If so, ask user if he/she wants do discard changes prior to leave this fragment
                if (contactChanged()) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(getResources().getString(R.string.str_discard_your_changes));

                    builder.setPositiveButton(getResources().getString(R.string.btn_caption_ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                        // If user confirm discard changes, just return to the previous activity
                            mActivity.finish();
                            }
                        });

                    builder.setNegativeButton(getResources().getString(R.string.btn_caption_cancel), null);

                    AlertDialog dialog = builder.create();
                    // display dialog
                    dialog.show();
                } else {
                    mActivity.finish();
                }
                return true;

            case R.id.menu_contact_confirm_changes:
                if (addAction) {
                    Log.d(TAG, "onOptionsItemSelected() - Confirm Add Contact menu item clicked");
                } else {
                    Log.d(TAG, "onOptionsItemSelected() - Confirm Edit Contact menu item clicked");
                }

                if (contactChanged()) {
                    onAddEditContact();
                } else {
                    // If detect no changes in the contact when 'confirm' menu item is clicked, just close the fragment
                    mActivity.finish();
                }

                return true;

            case R.id.menu_contact_delete:
                Log.d(TAG, "onOptionsItemSelected() - Delete Contact menu item clicked");
                onDeleteContact();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean contactChanged() {
        Log.v(TAG, "contactChanged() - Begin");

        String contactImageUri = mContact.getPhoto().getContactImageUri();

        String contactFirstName = this.contactFirstName.getText().toString();
        String contactLastName = this.contactLastName.getText().toString();

        String contactCompanyName = this.contactCompanyName.getText().toString();
        String contactCompanyTitle = this.contactCompanyTitle.getText().toString();

        ArrayList<Phone> contactPhones = mContact.getPhones();
        ArrayList<Email> contactEmails = mContact.getEmails();
        ArrayList<PostalAddress> contactAddresses = mContact.getAddresses();

        if (addAction) {
            if (contactImageUri != null && !contactImageUri.isEmpty()) {
                Log.d(TAG, "contactChanged() - Detected contactImageUri is not null.");
                return true;
            }

            if (!contactFirstName.isEmpty()) {
                Log.d(TAG, "contactChanged() - Detected contactFirstName is not empty.");
                return true;
            }

            if (!contactLastName.isEmpty()) {
                Log.d(TAG, "contactChanged() - Detected contactLastName is not empty.");
                return true;
            }

            if (!contactCompanyName.isEmpty()) {
                Log.d(TAG, "contactChanged() - Detected contactCompanyName is not empty.");
                return true;
            }

            if (!contactCompanyTitle.isEmpty()) {
                Log.d(TAG, "contactChanged() - Detected contactCompanyTitle is not empty.");
                return true;
            }

            if (contactPhones != null && contactPhones.size() > 0) {
                Log.d(TAG, "contactChanged() - Detected contactPhones is not empty.");
                return true;
            }

            if (contactEmails != null && contactEmails.size() > 0) {
                Log.d(TAG, "contactChanged() - Detected contactEmails is not empty.");
                return true;
            }

            if (contactAddresses != null && contactAddresses.size() > 0) {
                Log.d(TAG, "contactChanged() - Detected contactAddresses is not empty.");
                return true;
            }

        } else {
            if (contactImageUri != null && !contactImageUri.equals(mContactOrigin.getPhoto().getContactImageUri())) {
                Log.d(TAG, "contactChanged() - Detected contactImageUri changed.");
                return true;
            }

            if (!contactFirstName.equals(mContactOrigin.getFirstName())) {
                Log.d(TAG, "contactChanged() - Detected contactFirstName changed.");
                return true;
            }

            if (!contactLastName.equals(mContactOrigin.getLastName())) {
                Log.d(TAG, "contactChanged() - Detected contactLastName changed.");
                return true;
            }

            if (!contactCompanyName.equals(mContactOrigin.getOrganization().getName())) {
                Log.d(TAG, "contactChanged() - Detected contactCompanyName changed.");
                return true;
            }

            if (!contactCompanyTitle.equals(mContactOrigin.getOrganization().getTitle())) {
                Log.d(TAG, "contactChanged() - Detected contactCompanyTitle changed.");
                return true;
            }

            if (contactPhones != null && !contactPhones.equals(mContactOrigin.getPhones())) {
                Log.d(TAG, "contactChanged() - Detected contactPhones (list) changed.");
                return true;
            }

            if (contactEmails != null && !contactEmails.equals(mContactOrigin.getEmails())) {
                Log.d(TAG, "contactChanged() - Detected contactEmails (list) changed.");
                return true;
            }

            if (contactAddresses != null && !contactAddresses.equals(mContactOrigin.getAddresses())) {
                Log.d(TAG, "contactChanged() - Detected contactAddresses (list) changed.");
                return true;
            }
        }

        Log.v(TAG, "contactChanged() - Detected contact WAS NOT CHANGED");

        return false;
    }

    /**
     * This method is called by the onCreateView() when editing a contact.
     *
     * It will fill all the views up with the contact information.
     *
     * @param contact
     */
    private void updateContactViews(Contact contact) {
        Log.v(TAG, "updateContactViews() - Populating data for contact: " + contact.getDisplayName());

        Photo photo = contact.getPhoto();
        if (photo.getContactImageUri() != null && !photo.getContactImageUri().isEmpty()) {
            Log.d(TAG, "updateContactViews() - Contact: " + contact.getDisplayName() + " - Image URI: " + photo.getContactImageUri());
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), Uri.parse(photo.getContactImageUri()));
                contactImage.setImageURI(Uri.parse(photo.getContactImageUri()));
                contactImage.setImageBitmap(bitmap);
            } catch (IOException e) {
                Log.w(TAG, "updateContactViews() - Fail when trying to load image URI: " + photo.getContactImageUri() + ". Error: " + e.getMessage());
            }
        }

        contactFirstName.setText(contact.getFirstName());
        contactLastName.setText(contact.getLastName());

        for (final Phone phone : mContact.getPhones()) {
            Log.d(TAG, "updateContactViews() - Contact: " + contact.getDisplayName() + " - Adding phone: " + phone.getPhoneNumber() + " to the appropriate view...");
            updateContactPhoneView(phone);
        }

        for (final Email mail : mContact.getEmails()) {
            Log.d(TAG, "updateContactViews() - Contact: " + contact.getDisplayName() + " - Adding phone: " + mail.getEmailAddress() + " to the appropriate view...");
            updateContactEmailView(mail);
        }

        for (final PostalAddress address : mContact.getAddresses()) {
            Log.d(TAG, "updateContactViews() - Contact: " + contact.getDisplayName() + " - Adding phone: " + address.getStreet() + " to the appropriate view...");
            updateContactAddressView(address);
        }

        if (contact.getOrganization() != null && contact.getOrganization().getName() != null && !contact.getOrganization().getName().isEmpty()) {
            Log.d(TAG, "updateContactViews() - Contact: " + contact.getDisplayName() + " - Adding organization: " + contact.getOrganization().getName() + " and title: " + contact.getOrganization().getTitle() + " to the appropriate view...");
            contactCompanyName.setText(contact.getOrganization().getName());
            contactCompanyTitle.setText(contact.getOrganization().getTitle());
        }

        Log.v(TAG, "updateContactViews() - End");
    }

    /**
     * Method responsible to update a phone view for the current contact.
     *
     * It will add a dynamic view for the phone.
     *
     * Also it will create a listener when user clicks on the phone number allowing it to be changed and also another listener when user clicks on
     * the imageButton in order to allow it to be removed.
     *
     * @param phone
     */
    private void updateContactPhoneView(final Phone phone) {
        Log.v(TAG, "updateContactPhoneView() - Phone: " + phone.getPhoneNumber());

        // When a phone status is DELETED, it means that it was marked to be deleted and after that device was rotated. So, we cannot add it to the screen.
        if (phone.getStatus() == Status.DELETED) {
            Log.i(TAG, "updateContactPhoneView() - Detected phone: " + phone.getPhoneNumber() + " is marked as DELETED. Normally it happens after user deletes a phone and rotates the screen. It will not be added to the screen.");
            return;
        }

        // Load an especial template which contains a TextView and an ImageView (with a delete image)
        final View contactPhoneView = mInflater.inflate(R.layout.element_contact_edit, null);
        TextView contactPhoneTextView = (TextView) contactPhoneView.findViewById(R.id.tv_element_text);
        ImageView contactPhoneDelImageView = (ImageView) contactPhoneView.findViewById(R.id.img_delete_element);

        // Set contact phone to the TextView view
        contactPhoneTextView.setText(phone.toString());

        // Set a click listener for the phone TextView view. When user clicks over it, allow user to modify it.
        contactPhoneTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Pass the view that is being changed to the showAddEditEmailDialog() method, in order to allow it to remove it
                // prior to add the changed email view.
                Log.d(TAG, "contactPhoneTextView::onClick() - Calling showAddEditPhoneDialog() method in order to allow user to edit phone " + phone.getPhoneNumber() + "...");
                showAddEditPhoneDialog(contactPhoneView, phone);
            }
        });

        // Set a click listener for the delete ImageView view. When user clicks over it, this contact phone
        // will be deleted.
        contactPhoneDelImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "contactPhoneDelImageView::onClick() - Calling onDeleteContactPhone() method in order to delete phone " + phone.getPhoneNumber() + "...");
                onDeleteContactPhone(contactPhoneView, phone);
            }
        });

        contactPhoneLayout.addView(contactPhoneView);

        Log.v(TAG, "updateContactPhoneView() - End");
    }

    /**
     * Method responsible to update view an email for the current contact.
     *
     * It will add a dynamic view for the email.
     *
     * Also it will create a listener when user clicks on the email address allowing it to be changed and also another listener when user clicks on
     * the imageButton in order to allow it to be removed.
     *
     * @param mail
     */
    private void updateContactEmailView(final Email mail) {
        Log.v(TAG, "updateContactEmailView() - Email: " + mail.getEmailAddress());

        // When an mail status is DELETED, means that it was marked to be deleted and after that device was rotated. So, we cannot add it to the screen.
        if (mail.getStatus() == Status.DELETED) {
            Log.i(TAG, "updateContactEmailView() - Detected mail: " + mail.getEmailAddress() + " is marked as DELETED. Normally it happens after user deletes an email and rotates the screen. It will not be added to the screen.");
            return;
        }

        // Load an especial template which contains a TextView and an ImageView (with a delete image)
        final View contactEmailView = mInflater.inflate(R.layout.element_contact_edit, null);
        TextView contactEmailTextView = (TextView) contactEmailView.findViewById(R.id.tv_element_text);
        ImageView contactEmailDelImageView = (ImageView) contactEmailView.findViewById(R.id.img_delete_element);

        // Set email address to the TextView view
        contactEmailTextView.setText(mail.toString());

        // Set a click listener for the email TextView view. When user clicks over it, allow user to modify it.
        contactEmailTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Pass the view that is being changed to the showAddEditEmailDialog() method, in order to allow it to remove it
                // prior to add the changed email view.
                Log.d(TAG, "contactEmailTextView::onClick() - Calling showAddEditEmailDialog() method in order to allow user to edit mail " + mail.getEmailAddress() + "...");
                showAddEditEmailDialog(contactEmailView, mail);
            }
        });

        // Set a click listener for the delete ImageView view. When user clicks over it, this email
        // will be deleted.
        contactEmailDelImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "contactEmailDelImageView::onClick() - Calling onDeleteContactEmail() method in order to delete mail " + mail.getEmailAddress() + "...");
                onDeleteContactEmail(contactEmailView, mail);
            }
        });

        // Now add the view to the main layout.
        contactMailLayout.addView(contactEmailView);

        Log.v(TAG, "updateContactEmailView() - End");
    }

    /**
     * Method responsible to update view an address for the current contact.
     *
     * It will add a dynamic view for the address.
     *
     * Also it will create a listener when user clicks on the address address allowing it to be changed and also another listener when user clicks on
     * the imageButton in order to allow it to be removed.
     *
     * @param address
     */
    private void updateContactAddressView(final PostalAddress address) {
        Log.v(TAG, "updateContactAddressView() - Address: " + address.getStreet());

        // When an address status is DELETED, means that it was marked to be deleted and after that device was rotated. So, we cannot add it to the screen.
        if (address.getStatus() == Status.DELETED) {
            Log.i(TAG, "updateContactAddressView() - Detected address: " + address.getStreet() + " is marked as DELETED. Normally it happens after user deletes an address and rotates the screen. It will not be added to the screen.");
            return;
        }

        // Load an especial template which contains a TextView and an ImageView (with a delete image)
        final View contactAddressView = mInflater.inflate(R.layout.element_contact_edit, null);
        TextView contactAddressTextView = (TextView) contactAddressView.findViewById(R.id.tv_element_text);
        ImageView contactAddressDelImageView = (ImageView) contactAddressView.findViewById(R.id.img_delete_element);

        // Set contact address to the TextView view
        contactAddressTextView.setText(address.toString());

        // Set a click listener for the address TextView view. When user clicks over it, allow user to modify it.
        contactAddressTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Pass the view that is being changed to the showAddEditAddressDialog() method, in order to allow it to remove it
                // prior to add the changed address view.
                Log.d(TAG, "contactAddressTextView::onClick() - Calling showAddEditAddressDialog() method in order to allow user to edit address " + address.getStreet() + "...");
                showAddEditAddressDialog(contactAddressView, address);
            }
        });

        // Set a click listener for the delete ImageView view. When user clicks over it, this email address
        // will be deleted.
        contactAddressDelImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                Log.d(TAG, "contactAddressDelImageView::onClick() - Calling onDeleteContactAddress() method in order to delete address " + address.getStreet() + "...");
                        onDeleteContactAddress(contactAddressView, address);
                    }
                });
        contactAddressLayout.addView(contactAddressView);

        Log.v(TAG, "updateContactAddressView() - End");
    }

    /**
     * This method presents to the user a dialog which will be possible to add or change a contact phone.
     *
     * Along to the default phone types, it will be possible to type a custom phone type.
     *
     * TODO: Limit the number of characters user can type for the custom phone type.
     *
     * @param contactPhoneTextView
     * @param phoneToEdit
     */
    private void showAddEditPhoneDialog(final View contactPhoneTextView,
                                        final Phone phoneToEdit) {
        Log.v(TAG, "showAddEditPhoneDialog() - Begin");

        final View dialogView = mInflater.inflate(R.layout.dialog_add_phone, null);

        final EditText tvContactPhoneNumber = (EditText) dialogView.findViewById(R.id.tv_contact_phone_number);
        final Spinner spinnerContactPhoneType = (Spinner) dialogView.findViewById(R.id.sp_contact_phone_type);
        final String[] contactPhoneTypes = getResources().getStringArray(R.array.contact_phone_types);
        final EditText etContactPhoneCustomType = (EditText) dialogView.findViewById(R.id.tv_contact_phone_custom_type);

        // Load the phone types adapter and set it to the spinner view
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1, contactPhoneTypes);
        spinnerContactPhoneType.setAdapter(adapter);

        // If phoneToEdit is not null, it means this method was called to change an existent contact phone. So, fill the dialogView with data from the
        // phone that is being changed. When detecting a phone custom type, select the last spinner member which is related the custom type and
        // set custom textView as visible and finally fill it up with the phone custom text
        if (phoneToEdit != null) {
            Log.i(TAG, "showAddEditPhoneDialog() - phoneToEdit NOT NULL (Phone: " + phoneToEdit.getPhoneNumber() + "). Populating DialogView with its data, so that user can edit them.");

            tvContactPhoneNumber.setText(phoneToEdit.getPhoneNumber());
            int spinnerPosition = adapter.getPosition(phoneToEdit.getPhoneType());
            if (spinnerPosition > -1) {
                spinnerContactPhoneType.setSelection(spinnerPosition);
            } else {
                spinnerContactPhoneType.setSelection(contactPhoneTypes.length - 1); // custom tag
                etContactPhoneCustomType.setVisibility(View.VISIBLE);
                etContactPhoneCustomType.setText(phoneToEdit.getPhoneType());
            }
        }

        spinnerContactPhoneType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == (contactPhoneTypes.length - 1)) // Last position means custom tag
                    etContactPhoneCustomType.setVisibility(View.VISIBLE);
                        else
                            etContactPhoneCustomType.setVisibility(View.GONE);
                    }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                etContactPhoneCustomType.setVisibility(View.GONE);
            }
        });

        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle(getString(R.string.dialog_add_phone_title));
        dialog.setView(dialogView);

        dialog.setPositiveButton(getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {

                // If user confirm, create a new dynamic view for the phone.
                String phoneType = (String) spinnerContactPhoneType.getSelectedItem();
                if (phoneType.equals(getString(R.string.custom_type))) {
                    phoneType = etContactPhoneCustomType.getText().toString();
                }
                onAddEditContactPhone(tvContactPhoneNumber.getText().toString(), phoneType, phoneToEdit, contactPhoneTextView);
            }
        });
        dialog.setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        dialog.show();

        Log.v(TAG, "showAddEditPhoneDialog() - End");
    }

    /**
     * This method presents to the user a dialog which will be possible to add or change a contact email.
     *
     * Along to the default email types, it will be possible to type a custom email type.
     *
     * TODO: Limit the number of characters user can type for the custom email type.
     *
     * @param contactEmailView
     * @param emailToEdit
     */
    private void showAddEditEmailDialog(final View contactEmailView, final Email emailToEdit) {
        Log.v(TAG, "showAddEditEmailDialog() - Begin");

        final View dialogView = mInflater.inflate(R.layout.dialog_add_mail, null);

        final EditText etMail = (EditText) dialogView.findViewById(R.id.et_contact_mail_address);
        final Spinner spinnerContactMailType = (Spinner) dialogView.findViewById(R.id.sp_contact_mail_type);
        final String[] contactMailTypes = getResources().getStringArray(R.array.contact_mail_types);
        final EditText etContactMailCustomType = (EditText) dialogView.findViewById(R.id.et_contact_mail_custom_type);

        // Load the email types adapter and set it to the spinner view
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, contactMailTypes);
        spinnerContactMailType.setAdapter(adapter);

        // If emailToEdit is not null, means this method was called to change an existent contact email. So, fill the dialogView with data from the
        // email that is being changed. When detecting a email custom type, select the last spinner member which is related the custom type and
        // set custom textView as visible and finally fill it up with the email custom text
        if (emailToEdit != null) {
            Log.i(TAG, "showAddEditEmailDialog() - emailToEdit NOT NULL (Email: " + emailToEdit.getEmailAddress() + "). Populating DialogView with its data, so that user can edit them.");

            etMail.setText(emailToEdit.getEmailAddress());
            int spinnerPosition = adapter.getPosition(emailToEdit.getEmailType());
            if (spinnerPosition > -1) {
                spinnerContactMailType.setSelection(spinnerPosition);
            } else {
                spinnerContactMailType.setSelection(contactMailTypes.length - 1); // custom tag
                etContactMailCustomType.setVisibility(View.VISIBLE);
                etContactMailCustomType.setText(emailToEdit.getEmailType());
            }
        }

        spinnerContactMailType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == (contactMailTypes.length - 1))  // Last position means custom tag
                    etContactMailCustomType.setVisibility(View.VISIBLE);
                else
                    etContactMailCustomType.setVisibility(View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                etContactMailCustomType.setVisibility(View.GONE);
            }
        });

        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle(getString(R.string.dialog_add_mail_title));
        dialog.setView(dialogView);

        dialog.setPositiveButton(getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {

                // If user confirm, create a new dynamic view for the email.
                String emailType = (String) spinnerContactMailType.getSelectedItem();
                if (emailType.equals(getString(R.string.add_custom_email_type))) {
                    emailType = etContactMailCustomType.getText().toString();
                }

                onAddEditContactEmail(etMail.getText().toString(), emailType, emailToEdit, contactEmailView);
            }
        });
        dialog.setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        dialog.show();

        Log.v(TAG, "showAddEditEmailDialog() - End");
    }

    /**
     * This method presents to the user a dialog which will be possible to add or change a contact address.
     *
     * Along to the default address types, it will be possible to type a custom address type.
     *
     * TODO: Limit the number of characters user can type for the custom address type.
     *
     * @param contactAddressView
     * @param addressToEdit
     */
    private void showAddEditAddressDialog(final View contactAddressView, final PostalAddress addressToEdit) {
        Log.v(TAG, "showAddEditAddressDialog() - Begin");

        final View dialogView = mInflater.inflate(R.layout.dialog_add_address, null);

        final EditText etContactAddress = (EditText) dialogView.findViewById(R.id.et_contact_address);
        final Spinner spinnerContactAddressType = (Spinner) dialogView.findViewById(R.id.sp_contact_address_type);
        final String[] contactAddressTypes = getResources().getStringArray(R.array.contact_address_types);
        final EditText etContactAddressCustomType = (EditText) dialogView.findViewById(R.id.et_contact_address_custom_type);

        // Load the address types adapter and set it to the spinner view
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, contactAddressTypes);
        spinnerContactAddressType.setAdapter(adapter);

        // If addressToEdit is not null, means this method was called to change an existent contact address. So, fill the dialogView with data from the
        // address that is being changed. When detecting a address custom type, select the last spinner member which is related the custom type and
        // set custom textView as visible and finally fill it up with the email custom text
        if (addressToEdit != null) {
            Log.i(TAG, "showAddEditAddressDialog() - addressToEdit NOT NULL (Address: " + addressToEdit.getStreet() + "). Populating DialogView with its data, so that user can edit them.");

            etContactAddress.setText(addressToEdit.getStreet());
            int spinnerPosition = adapter.getPosition(addressToEdit.getType());
            if (spinnerPosition > -1) {
                spinnerContactAddressType.setSelection(spinnerPosition);
            } else {
                spinnerContactAddressType.setSelection(contactAddressTypes.length - 1); // custom tag
                etContactAddressCustomType.setVisibility(View.VISIBLE);
                etContactAddressCustomType.setText(addressToEdit.getType());
            }
        }

        spinnerContactAddressType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == (contactAddressTypes.length - 1)) // Last position means custom tag
                    etContactAddressCustomType.setVisibility(View.VISIBLE);
                else
                    etContactAddressCustomType.setVisibility(View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                etContactAddressCustomType.setVisibility(View.GONE);
            }
        });

        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle(getString(R.string.dialog_add_address_title));
        dialog.setView(dialogView);

        dialog.setPositiveButton(getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {

                // If user confirm, create a new dynamic view for the address.
                String addressType = (String) spinnerContactAddressType.getSelectedItem();
                if (addressType.equals(getString(R.string.add_custom_email_type))) {
                    addressType = etContactAddressCustomType.getText().toString();
                }
                onAddEditContactAddress(etContactAddress.getText().toString(), addressType, addressToEdit, contactAddressView);
            }
        });

        dialog.setNegativeButton(getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        dialog.show();

        Log.v(TAG, "showAddEditAddressDialog() - End");
    }

    /**
     * This method will add or change a phone based on the user input.
     *
     * When changing a phone, it will mark the status phone attribute as UPDATED. This will be used by the ContactsManager class in order to update it
     * in the Contacts Provider.
     *
     * @param phoneNumber
     * @param phoneType
     * @param phoneToEdit
     * @param contactPhoneTextView
     */
    private void onAddEditContactPhone(String phoneNumber, String phoneType, Phone phoneToEdit, View contactPhoneTextView) {
        Log.v(TAG, "onAddEditContactPhone() - Begin");

        if (phoneToEdit == null) {
            Log.d(TAG, "onAddEditContactPhone() - User confirmed a new phone creation (Phone: " + phoneNumber + "). Applying them...");

            final Phone phone = new Phone();
            phone.setPhoneNumber(phoneNumber);
            phone.setPhoneType(phoneType);
            // Mark this phone status as NEW. This will allow ContactsManager to know what to do when updating this contact
            phone.setStatus(Status.NEW);

            mContact.addPhone(phone);

            updateContactPhoneView(phone);

        } else {
            Log.d(TAG, "onAddEditContactPhone() - User confirmed changes in the phone data (Phone: " + phoneToEdit.getPhoneNumber() + "). Applying them...");

            phoneToEdit.setPhoneNumber(phoneNumber);
            phoneToEdit.setPhoneType(phoneType);

            // Only mark this phone status as UPDATED if it is different from NEW, since user can be changing a phone he added during this session and it is not persisted in provider yet.
            // In this case, we need to keep it as NEW in order for the ContactsManger to know what to do when updating contacts provider.
            // Otherwise, mark it as UPDATED.
            if (phoneToEdit.getStatus() != Status.NEW) {
                Log.d(TAG, "onAddEditContactPhone() - Status for phone that is being changed is" + phoneToEdit.getStatus() + " Changing it to UPDATED....");
                phoneToEdit.setStatus(Status.UPDATED);
            } else {
                Log.i(TAG, "onAddEditContactPhone() - Detected a changed in a phone that was added before, but not updated in the contact provider. Keeping it as NEW...");
            }

            mContact.addPhone(phoneToEdit);

            // IMPORTANT: When editing a phone, first remove current view from the layout, and only then call updateContactPhoneView() method, which
            // will add a new view to the layout containing the changed phone.
            contactPhoneLayout.removeView(contactPhoneTextView);

            updateContactPhoneView(phoneToEdit);

            Log.v(TAG, "onAddEditContactPhone() - End");
        }
    }

    /**
     * This method will add or change an email based on the user input.
     *
     * When changing an email, it will mark the status email attribute as UPDATED. This will be used by the ContactsManager class in order to update it
     * in the Contacts Provider.
     *
     * @param emailAddress
     * @param emailType
     * @param emailToEdit
     * @param contactEmailView
     */
    private void onAddEditContactEmail(String emailAddress, String emailType, Email emailToEdit, View contactEmailView) {
        Log.v(TAG, "onAddEditContactEmail() - Begin");

        if (emailToEdit == null) {
            Log.d(TAG, "onAddEditContactEmail() - User confirmed a new email creation (Email: " + emailAddress + "). Applying them...");

            final Email mail = new Email();
            mail.setEmailAddress(emailAddress);
            mail.setEmailType(emailType);

            // Mark this email status as NEW. This will allow ContactsManager to know what to do when updating this contact
            mail.setStatus(Status.NEW);

            mContact.addMail(mail);

            updateContactEmailView(mail);

        } else {
            Log.d(TAG, "onAddEditContactEmail() - User confirmed changes in the email data (Email: " + emailToEdit.getEmailAddress() + "). Applying them...");

            emailToEdit.setEmailAddress(emailAddress);
            emailToEdit.setEmailType(emailType);

            // Only mark this email status as UPDATED if it is different from NEW, since user can be changing an email he added during this session and it is not
            // persisted in provider.
            // In this case, we need to keep it as NEW in order to the ContactsManger to know what to do when updating contacts provider.
            // Otherwise, mark it as UPDATED.
            if (emailToEdit.getStatus() != Status.NEW) {
                Log.d(TAG, "onAddEditContactEmail() - Status for email that is being changed is " + emailToEdit.getStatus() + " Changing it to UPDATED....");
                emailToEdit.setStatus(Status.UPDATED);
            } else {
                Log.i(TAG, "onAddEditContactEmail() - Detected a changed in an email that was added before, but not updated in the contact provider. Keeping it as NEW...");
            }

            mContact.addMail(emailToEdit);

            // IMPORTANT: When editing an email, first remove current view from the layout, and only then call updateContactEmailView() method, which
            // will add a new view to the layout containing the changed email.
            contactMailLayout.removeView(contactEmailView);

            updateContactEmailView(emailToEdit);

            Log.v(TAG, "onAddEditContactEmail() - End");
        }
    }

    /**
     * This method will add or change an address based on the user input.
     *
     * When changing an address, it will mark the status address attribute as UPDATED. This will be used by the ContactsManager class in order to update it
     * in the Contacts Provider.
     *
     * @param addressName
     * @param addressType
     * @param addressToEdit
     * @param contactAddressView
     */
    private void onAddEditContactAddress(String addressName, String addressType, PostalAddress addressToEdit, View contactAddressView) {
        Log.v(TAG, "onAddEditContactAddress() - Begin");

        if (addressToEdit == null) {
            Log.d(TAG, "onAddEditContactAddress() - User confirmed a new address creation (Address: " + addressName + "). Applying them...");

            final PostalAddress address = new PostalAddress();
            address.setStreet(addressName);
            address.setType(addressType);

            // Mark this address status as NEW. This will allow ContactsManager to know what to do when updating this contact
            address.setStatus(Status.NEW);

            mContact.addAddress(address);

            updateContactAddressView(address);

        } else {
            Log.d(TAG, "onAddEditContactAddress() - User confirmed changes in the address data (Address: " + addressToEdit.getStreet() + "). Applying them...");

            addressToEdit.setStreet(addressName);
            addressToEdit.setType(addressType);

            // Only mark this address status as UPDATED if it is different from NEW, since user can be changing an address he added during this session and it is not persisted in provider.
            // In this case, we need to keep it as NEW in order to the ContactsManger to know what to do when updating contacts provider.
            // Otherwise, mark it as UPDATED.
            if (addressToEdit.getStatus() != Status.NEW) {
                Log.d(TAG, "onAddEditContactAddress() - Status for address that is being changed is " + addressToEdit.getStatus() + " Changing it to UPDATED....");
                addressToEdit.setStatus(Status.UPDATED);
            } else {
                Log.i(TAG, "onAddEditContactAddress() - Detected a changed in an address that was added before, but not updated in the contact provider. Keeping it as NEW...");
            }

            mContact.addAddress(addressToEdit);

            // IMPORTANT: When editing an address, first remove current view from the layout, and only then call updateContactAddressView() method, which
            // will add a new view to the layout containing the changed address.
            contactAddressLayout.removeView(contactAddressView);

            updateContactAddressView(addressToEdit);

            Log.v(TAG, "onAddEditContactAddress() - End");
        }
    }

    /**
     * This method will delete a phone after user confirms it.
     *
     * Actually it will not remove this phone from the contact phone list, but change its status attribute to DELETED. This will be used by the ContactsManager class
     * when updating this contact by deleting this phone in the Contacts Provider.
     *
     * @param contactPhoneView
     * @param phone
     */
    private void onDeleteContactPhone(View contactPhoneView, Phone phone) {
        Log.i(TAG, "onDeleteContactPhone() - Phone: " + phone.getPhoneNumber() + ". Changing its status to DELETED, so that it will be deleted later by the contacts provider.");

        // Set a DELETED status for this phone instead of remove it from the contact phone list. This will allow ContactsManager to know what to
        // do with this phone when updating it.
        phone.setStatus(Status.DELETED);

        // Update the contact phone list with this deleted phone
        mContact.addPhone(phone);

        // When user delete a phone number, also delete its view
        contactPhoneLayout.removeView(contactPhoneView);

        Log.v(TAG, "onDeleteContactPhone() - End");
    }

    /**
     * This method will delete an email after user confirms it.
     *
     * Actually it will not remove this email from the contact email list, but change its status attribute to DELETED. This will be used by the ContactsManager class
     * when updating this contact by deleting this email in the Contacts Provider.
     *
     * @param contactEmailView
     * @param mail
     */
    private void onDeleteContactEmail(View contactEmailView, Email mail) {
        Log.i(TAG, "onDeleteContactEmail() - Email: " + mail.getEmailAddress() + ". Changing its status to DELETED, so that it will be deleted later by the contacts provider.");

        // Set a DELETED status for this email instead of remove it from the contact email list. This will allow ContactsManager to know what to
        // do with this email when updating this contact.
        mail.setStatus(Status.DELETED);

        // Update the contact email list with this deleted email
        mContact.addMail(mail);

        // When user delete an email, also delete its view
        contactMailLayout.removeView(contactEmailView);

        Log.v(TAG, "onDeleteContactEmail() - End");
    }

    /**
     * This method will delete an address after user confirms it.
     *
     * Actually it will not remove this address from the contact address list, but change its status attribute to DELETED. This will be used by the ContactsManager class
     * when updating this contact by deleting this address in the Contacts Provider.
     *
     * @param contactAddressView
     * @param address
     */
    private void onDeleteContactAddress(View contactAddressView, PostalAddress address) {
        Log.i(TAG, "onDeleteContactAddress() - Address: " + address.getStreet() + ". Changing its status to DELETED, so that it will be deleted later by the contacts provider.");

        // Set a DELETED status for this address instead of remove it from the contact address list. This will allow ContactsManager to know what to
        // do with this address when updating this contact.
        address.setStatus(Status.DELETED);

        // Update the contact address list with this deleted address
        mContact.addAddress(address);

        // When user delete an address, also delete its view
        contactAddressLayout.removeView(contactAddressView);

        Log.v(TAG, "onDeleteContactAddress() - End");
    }

    /**
     * This method will be called when user clicks on the photo FAB button. It will first present to the user a dialog which user
     * can choose either to take a photo from camera or pick a photo from the gallery. In both options it will create intents in order for
     * an external app to process this request (either a camera app or a gallery chooser app)
     *
     */
    private void addContactPhoto() {
        Log.v(TAG, "addContactPhoto() - Begin");

        String[] addPhoto = new String[]{
                getResources().getString(R.string.start_camera_dialog),
                getResources().getString(R.string.show_gallery_dialog) };

        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle(getResources().getString(R.string.add_photo_dialog_title));

        dialog.setItems(addPhoto,new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int id) {
                if(id==0){
                    Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    // If you call startActivityForResult() using an intent that no app can handle, your app will crash.
                    // So as long as the result is not null, it's safe to use the intent.
                    if (takePicture.resolveActivity(getActivity().getPackageManager()) != null) {
                        Log.i(TAG, "addContactPhoto() - Sending ACTION_IMAGE_CAPTURE intent...");
                        startActivityForResult(takePicture, TAKE_PICTURE);
                    } else {
                        Log.w(TAG, "addContactPhoto() - This device does not support to take a photo from the camera.");
                    }

                }
                if(id==1){
                    Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    // If you call startActivityForResult() using an intent that no app can handle, your app will crash.
                    // So as long as the result is not null, it's safe to use the intent.
                    if (pickPhoto.resolveActivity(getActivity().getPackageManager()) != null) {
                        // Bring up gallery to select a photo
                        Log.i(TAG, "addContactPhoto() - Sending ACTION_PICK intent (EXTERNAL_CONTENT_URI)...");
                        startActivityForResult(pickPhoto, SELECT_PHOTO);
                    } else {
                        Log.w(TAG, "addContactPhoto() - This device does not support accessing Media Store.");
                    }
                }
            }
        });

        dialog.show();

        Log.v(TAG, "addContactPhoto() - End");
    }

    /**
     * After user chooses a photo from the gallery or take a photo from the camera, this method will be called by the system. It will then
     * start another activity in order for the photo to be cropped.
     *
     * After a photo is cropped by the user, this method will be also called called. Then the photo will be updated in the GUI and in the
     * Contact object.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG, "onActivityResult() - Begin");

        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {

            case SELECT_PHOTO:
                Log.d(TAG, "onActivityResult() - SELECT_PHOTO");
                if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {

                    // Update photo status to NEW or UPDATED based on the current status. It will be used later for the ContactsManager
                    // in order to take the right action when querying Contacts Provider
                    Photo photo = mContact.getPhoto();
                    if (photo.getContactImageUri() == null || photo.getContactImageUri().isEmpty()) {
                        photo.setStatus(Status.NEW);
                    }
                    if (photo.getContactImageUri() != null && !photo.getContactImageUri().isEmpty()) {
                        photo.setStatus(Status.UPDATED);
                    }

                    // Now update photo URI
                    photo.setContactImageUri(data.getData().toString());

                    //perform Crop on the Image Selected from Gallery
                    Log.i(TAG, "onActivityResult() - contactImageUri: " + photo.getContactImageUri() + ". Calling performCrop() method...");
                    performCrop();
                }
                break;

            case TAKE_PICTURE:
                Log.d(TAG, "onActivityResult() - TAKE_PICTURE");
                if(resultCode == Activity.RESULT_OK){
                    Photo photo = mContact.getPhoto();

                    // Get the bitmap
                    Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                    // Get the URI from the bitmap
                    Uri uri = getImageUri(bitmap);

                    // And now save the image URI in the Photo object
                    photo.setContactImageUri(uri.toString());

                    // Update photo status to NEW or UPDATED based on the current status. It will be used later for the ContactsManager
                    // in order to take the right action when querying Contacts Provider
                    if (photo.getContactImageUri() == null || photo.getContactImageUri().isEmpty()) {
                        photo.setStatus(Status.NEW);
                    }
                    if (photo.getContactImageUri() != null && !photo.getContactImageUri().isEmpty()) {
                        photo.setStatus(Status.UPDATED);
                    }

                    //perform Crop on the Image Selected from Gallery
                    Log.i(TAG, "onActivityResult() - contactImageUri: " + photo.getContactImageUri() + ". Calling performCrop() method...");
                    performCrop();
                }
                break;

            case CROP_PHOTO:
                Log.d(TAG, "onActivityResult() - CROP_PHOTO");
                if (resultCode == Activity.RESULT_OK) {
                    Photo photo = mContact.getPhoto();
                    try {
                        Log.i(TAG, "onActivityResult() - Updating contact image with: " + photo.getContactImageUri());

                        Bundle extras = data.getExtras();
                        Bitmap selectedBitmap = extras.getParcelable("data");
                        contactImage.setImageURI(Uri.parse(photo.getContactImageUri()));
                        contactImage.setImageBitmap(selectedBitmap);

                    } catch (Exception e) {
                        Log.w(TAG, "onActivityResult() - Failure while trying to set image (" + photo.getContactImageUri() + "). Error: " + e.getMessage());
                    }
                }
                break;
        }

        Log.v(TAG, "onActivityResult() - End");
    }

    /**
     * This method gets the URI from a bitmap. Useful after take a photo using a camera.
     * See below:
     * http://stackoverflow.com/questions/26059748/is-there-away-to-get-uri-of-bitmap-with-out-save-it-to-sdcard
     *
     * @param inImage
     * @return
     */
    public Uri getImageUri(Bitmap inImage) {

        // See this: http://stackoverflow.com/questions/12230942/why-images-media-insertimage-return-null
        new File("/sdcard/Pictures").mkdirs();

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContext().getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    /**
     * Helper method to prepare a crop intent request.
     *
     */
    private void performCrop() {
        Log.v(TAG, "performCrop() - End");

        try {
            //Start Crop Activity

            Intent cropIntent = new Intent("com.android.camera.action.CROP");

            cropIntent.setDataAndType(Uri.parse(mContact.getPhoto().getContactImageUri()), "image/*");
            // set crop properties
            cropIntent.putExtra("crop", "true");
            // indicate aspect of desired crop
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            // indicate output X and Y
            cropIntent.putExtra("outputX", 280);
            cropIntent.putExtra("outputY", 280);

            // retrieve data on return
            cropIntent.putExtra("return-data", true);

            // start the activity - we handle returning in onActivityResult
            Log.i(TAG, "addContactPhoto() - Sending [com.android.camera.action.CROP] intent...");
            startActivityForResult(cropIntent, CROP_PHOTO);
        }
        // respond to users whose devices do not support the crop action
        catch (ActivityNotFoundException e) {
            Log.w(TAG, "performCrop() - Device does not support the crop action");
        }

        Log.v(TAG, "performCrop() - End");
    }

    /**
     * This method is called after user confirms to add a contact.
     *
     * It will read some views and update Contact object. It will not update phone, email and address lists, since all of them were already
     * updated when user added/changed/deleted an item.
     *
     * In the end it will start an AsyncTask in order for the request to be processed in a thread another than the main thread.
     *
     */
    private void onAddEditContact() {
        Log.v(TAG, "onAddEditContact() - Begin");

        String contactFirstName = this.contactFirstName.getText().toString();
        String contactLastName = this.contactLastName.getText().toString();

        StructuredName structuredName = new StructuredName();
        // When changing a contact, add structured name ID to the mContact, since it will  be used by the ContactsManager to update structuredNames attributes.
        if (!addAction) {
            structuredName.setId(mContactOrigin.getStructuredName().getId());
        }
        structuredName.setDisplayName(contactFirstName + " " + contactLastName);
        structuredName.setFirstName(contactFirstName);
        structuredName.setLastName(contactLastName);
        mContact.setStructuredName(structuredName);

        Organization contactOrganization = new Organization();
        String contactCompanyName = this.contactCompanyName.getText().toString();
        String contactCompanyTitle = this.contactCompanyTitle.getText().toString();
        // When changing a contact, add organization  ID to the mContact, since it will  be used by the ContactsManager to update organization attributes.
        if (!addAction) {
            contactOrganization.setId(mContactOrigin.getOrganization().getId());
        }
        contactOrganization.setName(contactCompanyName);
        contactOrganization.setTitle(contactCompanyTitle);
        mContact.setOrganization(contactOrganization);

        if (addAction) { // Adding contact
        	// Request Contacts Provider to add a new contact.
            Log.d(TAG, "onAddEditContact() - Starting ContactDetailsTask() task in order to request Contact Provider to add the new contact " + mContact.getDisplayName());
            new ContactDetailsTask().execute(Constants.ACTION_CREATE);
            
        } else { // Editing contact
        	// Request Contacts Provider to update a contact.
            Log.d(TAG, "onAddEditContact() - Starting ContactDetailsTask() task in order to request Contact Provider to update contact " + mContact.getDisplayName());
            new ContactDetailsTask().execute(Constants.ACTION_UPDATE);
        }

        Log.v(TAG, "onAddEditContact() - End");
    }

    /**
     * After user confirms to delete a contact, this method is called.
     *
     * It will start an AsyncTask in order for the request to be processed in a thread another than the main thread.
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
     * This is a helper method which is called after a contact is created or updated in the Contacts Provider.
     *
     * When removing a phone, email or an address for a contact, we do not remove it from the contact list,
     * but instead we only update a status attribute with the Status::DELETED. Later, this attribute will guide
     * ContactsManager class methods to request Contacts Manager to delete those properties from the contacts provider.
     *
     * After that we must remove these items from the list since they will be used to update ContactsDetailsFragment views. This is
     * what this method is for.
     *
     */
    private void fixContact() {

        ArrayList<Phone> finalPhoneList = new ArrayList<>();
        for (final Phone phone : mContact.getPhones()) {
            if (phone.getStatus() != Status.DELETED) {
                finalPhoneList.add(phone);
            } else {
                Log.i(TAG, "fixContact() - Detected phone: " + phone.getPhoneNumber() + " was marked to be deleted. Removing it from the list...");
            }
        }
        mContact.setPhones(finalPhoneList);

        ArrayList<Email> finalEmailList = new ArrayList<>();
        for (final Email email : mContact.getEmails()) {
            if (email.getStatus() != Status.DELETED) {
                finalEmailList.add(email);
            } else {
                Log.i(TAG, "fixContact() - Detected email: " + email.getEmailAddress() + " was marked to be deleted. Removing it from the list...");
            }
        }
        mContact.setEmails(finalEmailList);

        ArrayList<PostalAddress> finalAddressList = new ArrayList<>();
        for (final PostalAddress address : mContact.getAddresses()) {
            if (address.getStatus() != Status.DELETED) {
                finalAddressList.add(address);
            } else {
                Log.i(TAG, "fixContact() - Detected address: " + address.getStreet() + " was marked to be deleted. Removing it from the list...");
            }
        }
        mContact.setAddresses(finalAddressList);
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
	        	case Constants.ACTION_CREATE:
	        		Log.i(TAG, "doInBackground() - Calling ContactsManager.addNewContact() method in order to request Contact Provider to add the new contact " + mContact.getDisplayName());
	                ContactsManager.getInstance(getContext()).addNewContact(mContact, getActivity().getContentResolver());
		            break;

	        	case Constants.ACTION_UPDATE:
	        		Log.i(TAG, "onAddEditContact() - Calling ContactsManager.editContact() method in order to request Contact Provider to edit contact " + mContact.getDisplayName());
	                ContactsManager.getInstance(getContext()).editContact(mContact, getActivity().getContentResolver());
	                break;

        		case Constants.ACTION_DELETE:
	        		Log.i(TAG, "doInBackground() - Calling ContactsManager.onDeleteContact() method in order to request Contact Provider to delete contact " + mContact.getDisplayName());
	                ContactsManager.getInstance(getContext()).deleteContact(mContact, getActivity().getContentResolver(), mLookupKey);
		        	break;
        	}
        	
        	Log.v(TAG, "doInBackground() - End");
        	
            return contact;
        }

        @Override
        protected void onPostExecute(Contact contact) {
        	Log.v(TAG, "onPostExecute() - Begin");
        	
        	Intent intent = new Intent();
        	switch (action) {
	        	case Constants.ACTION_CREATE:
	        		// This method will remove all DELETED phones/emails/addresses from the contact object, so that ContactDetailsFragment will just have to
	                // update its views without have to know what happened with the contact
	                fixContact();

	                // Send a broadcast with CONTACT_CREATED action or "CONTACT_CREATED". The Intent sent should be received by the ContactsDetailsFragment.
	                Log.i(TAG, "onPostExecute() - Action ACTION_CREATE. Sending CONTACT_CREATED broadcast action...");
	                intent.setAction(Constants.CONTACT_CREATED);
	                break;
	        		
	        	case Constants.ACTION_UPDATE:
	        		// This method will remove all DELETED phones/emails/addresses from the contact object, so that ContactDetailsFragment will just have to
	                // update its views without have to know what happened with the contact
	                fixContact();

	                // Send a broadcast with CONTACT_CREATED action or "CONTACT_UPDATED". The Intent sent should be received by the ContactsDetailsFragment.
	                Log.i(TAG, "onPostExecute() - Action ACTION_UPDATE. Sending CONTACT_UPDATED broadcast action...");
	                intent.setAction(Constants.CONTACT_UPDATED);
	                break;
	                
	        	case Constants.ACTION_DELETE:
	        		// WARNING: Do not try to access contact parameter, since it is null for action ACTION_DELETE!!!
		        	// Send a broadcast with CONTACT_DELETED action. The Intent should be received by the ContactsDetailsFragment.
	                Log.i(TAG, "onPostExecute() - Action ACTION_DELETE. Sending CONTACT_DELETED broadcast action...");
	                intent.setAction(Constants.CONTACT_DELETED);
	                break;
        	}
        	
        	intent.putExtra(Constants.CONTACT, mContact);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
            mActivity.finish();
            
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

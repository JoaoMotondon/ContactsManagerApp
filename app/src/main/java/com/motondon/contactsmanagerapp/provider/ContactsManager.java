package com.motondon.contactsmanagerapp.provider;

/**
 * This is the heart of this app. It contains all the business logic to fetch/add/change/delete contacts by querying 
 * the Contacts Provider.
 *
 */
import android.annotation.TargetApi;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;

import com.motondon.contactsmanagerapp.model.Contact;
import com.motondon.contactsmanagerapp.model.Email;
import com.motondon.contactsmanagerapp.model.Group;
import com.motondon.contactsmanagerapp.model.Organization;
import com.motondon.contactsmanagerapp.model.Phone;
import com.motondon.contactsmanagerapp.model.Photo;
import com.motondon.contactsmanagerapp.model.PostalAddress;
import com.motondon.contactsmanagerapp.model.Status;
import com.motondon.contactsmanagerapp.model.StructuredName;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class responsible to centralize all the access to the Contacts Provider.
 *
 */
public class ContactsManager {

    private static final String TAG = ContactsManager.class.getSimpleName();

    private static ContactsManager instance;

    private final Context mContext;

    private ContactsManager(Context context) {
        this.mContext = context;
    }

    public static ContactsManager getInstance(Context context) {
        if (instance == null) {
            instance = new ContactsManager(context);
        }
        return instance;
    }

    /**
     * As the name implies, this method queries Contacts Provider for details for a specific contact based on its lookup key. This key was
     * retrieved when querying for the contacts list during the app startup
     *
     * @param mLookupKey
     * @return
     */
    public Contact queryContactDetails(String mLookupKey) {

        Contact contact = new Contact();

        // First get basic details for the contact. Later, based on the lookup key we will get some other details such as phones,
        // email accounts and groups that contact belongs to.
        ContentResolver contactResolver = mContext.getContentResolver();

        final String SELECTION = ContactsContract.Data.LOOKUP_KEY + " = ?";
        // Define an array that will hold the search criteria
        String[] mSelectionArgs = {mLookupKey};

        // Query the Contacts Provider
        Cursor cursor = contactResolver.query(ContactsContract.Contacts.CONTENT_URI, null, SELECTION, mSelectionArgs, null);

        if (cursor != null && cursor.moveToFirst()) {

        	// Now extract some columns used by this app
            Integer contactId = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            Log.d(TAG, "Contact ID: " + contactId);
            String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
            Log.d(TAG, "Contact Lookup Key: " + lookupKey);
            String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            Log.d(TAG, "Contact Name: " + displayName);
            String photoUri = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI));
            Log.d(TAG, "Contact Photo URI: " + photoUri);

            // Get the Raw ContactId. It will be used later when adding to a contact a phone, an email or an address
            int rawContactId = getRawContactId(contactResolver, contactId);
            contact.setRawContactId(rawContactId);
            
            // Store retrieved data to the 'Contact' object
            contact.setContactID(contactId); // Store contactId. It will be used when updating/deleting this contact.
            contact.setLookupKey(lookupKey);

            // Store contact photo uri
            if (photoUri != null) {
                Photo photo = new Photo();
                photo.setContactImageUri(photoUri);
                contact.setPhoto(photo);
            }

            // Now query for the structured names.
            queryContactStructuredNames(contact, contactResolver, mLookupKey, displayName);

            // Contacts table has a column HAS_PHONE_NUMBER which contains a number of phone numbers for the current contact. If it is greater than zero,
            // let's query for the phone details.
            if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                Log.d(TAG, "ContactDetailsTask::queryContactPhones() - Detected contact (" + displayName + ") contains phone number(s). Querying it...");
                queryContactPhones(contact, contactResolver, mLookupKey, displayName);
            }

            // Query for the e-mail accounts details
            queryContactEmailAccounts(contact, contactResolver, mLookupKey, displayName);

            // Query for the postal addresses details
            queryContactPostalAddresses(contact, contactResolver, mLookupKey, displayName);

            // Query for the organization details (a contact can contains only a single organization)
            queryContactOrganization(contact, contactResolver, mLookupKey, displayName);

            // Now, if contact belongs to at least one visible group, query it. Actually groups are not being used by this app, but let it here for 
            // future reference
            if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.IN_VISIBLE_GROUP))) == 1) {
                Log.d(TAG, "ContactDetailsTask::queryContactGroups() - Detected contact (" + displayName + ") belongs to a visible group. Querying it...");
                queryContactGroups(contact, contactResolver, mLookupKey, displayName);
            }

            // Never forget to close cursor. 
            cursor.close();
        }

        // Return a Contact object with all the attributes used by this app. 
        return contact;
    }

    /**
     * This method queries for structured names for a contact based on its lookup key. It queries Data table for StructuredName MIMETYPE.
     * 
     * @param contact
     * @param contactResolver
     * @param mLookupKey
     * @param displayName
     */
    private void queryContactStructuredNames(Contact contact, ContentResolver contactResolver, String mLookupKey, String displayName) {
        Log.d(TAG, "queryContactStructuredNames() - Querying contact (" + displayName + ") for structured names...");

        // First we have to query for the groups IDs that contact belongs to and only then query for their names.
        String SELECTION =
                ContactsContract.Data.LOOKUP_KEY + " = ?" +
                        " AND " +
                        ContactsContract.Data.MIMETYPE + " = " +
                        "'" + ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE + "'";

        // Define an array that will hold the search criteria
        String[] mSelectionArgs = {mLookupKey};

        Cursor cursor = contactResolver.query(
                ContactsContract.Data.CONTENT_URI,
                null,
                SELECTION, mSelectionArgs, null);

        // A contact has only ONE StructuredName register in the Data table. So just check for the Cursor::moveToFirst().
        if (cursor.moveToFirst()) {

        	// Get the columns we will use on this app.
        	String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Data._ID));
            Log.d(TAG, "ID : " + id);
            String given = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
            Log.d(TAG, "Contact Given Name: " + given);
            String family = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
            Log.d(TAG, "Contact Family Name: " + family);
            String display = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
            Log.d(TAG, "Contact Display Name: " + display);

            // Fill the Contact object up with data from the StructuredName 
            StructuredName structuredName = new StructuredName();
            structuredName.setId(Integer.parseInt(id)); // This id will  be used later when changing structured names (such as given name, etc)
            structuredName.setDisplayName(given + " " + family);
            structuredName.setFirstName(given);
            structuredName.setLastName(family);
            contact.setStructuredName(structuredName);
        }

        // Never, ever forget to close the cursor.
        cursor.close();
    }

    /**
     * This method queries for phones details for a contact based on its lookup key. It queries Data table for Phone MIMETYPE.
     * 
     * Note that for the Contacts Provider, a contact can have none phones attached to it. If that is the case, cursor would be empty.
     * But for this app, it should never happen, since this method is only called when Contacts.HAS_PHONE_NUMBER is greater than zero.
     * 
     * @param contact
     * @param contactResolver
     * @param mLookupKey
     * @param displayName
     */
    private void queryContactPhones(Contact contact, ContentResolver contactResolver, String mLookupKey, String displayName) {
        Log.d(TAG, "queryContactPhones() - Querying contact (" + displayName + ") for phone numbers...");

        String SELECTION =
                ContactsContract.Data.LOOKUP_KEY + " = ?" +
                        " AND " +
                        ContactsContract.Data.MIMETYPE + " = " +
                        "'" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";

        // Define an array that will hold the search criteria
        String[] mSelectionArgs = {mLookupKey};

        Cursor pCur = contactResolver.query(
                ContactsContract.Data.CONTENT_URI,
                null,
                SELECTION, mSelectionArgs, null);

        List<Phone> contactPhones = new ArrayList<>();
        while (pCur.moveToNext()) {

            // This id will be used later when changing or deleting this organization
        	String id = pCur.getString(pCur.getColumnIndex(ContactsContract.Data._ID));
            Log.d(TAG, "ID : " + id);
            String phoneNumber = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            Log.d(TAG, "Phone Number: " + phoneNumber);
            Integer type = pCur.getInt(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
            Log.d(TAG, "Phone type: " + type);
            
            // When detect a phone type is of custom type (Phone.TYPE_CUSTOM), extract custom type name from the LABEL column
            String typeName;
            if (type == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) {
            	typeName = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL));
            } else {
            	typeName = (String) ContactsContract.CommonDataKinds.Phone.getTypeLabel(mContext.getResources(), type, "");
            }
            Log.d(TAG, "Phone Type Name: " + typeName);
            
            Phone contactPhone = new Phone();
            contactPhone.setId(Integer.parseInt(id));
            contactPhone.setPhoneNumber(phoneNumber);
            contactPhone.setPhoneType(typeName);
            
            // Add contactPhone object to the list of Phone's object
            contactPhones.add(contactPhone);

            Log.d(TAG, "Contact: " + displayName + " - Phone: " + phoneNumber + " - Type: " + typeName);
        }

        // Finally add the list of Phone's to the Contact object (no matter whether it is empty or not)
        contact.setPhones((ArrayList<Phone>) contactPhones);

        // Do not forget to close the cursor.
        pCur.close();
    }

    /**
     * This method queries for email accounts details for a contact based on its lookup key. It queries Data table for Email MIMETYPE.
     * 
     * Note that for the Contacts Provider, a contact can have none email account attached to it. If that is the case, cursor would be empty.
     * 
     * @param contact
     * @param contactResolver
     * @param mLookupKey
     * @param displayName
     */
    private void queryContactEmailAccounts(Contact contact, ContentResolver contactResolver, String mLookupKey, String displayName) {
        Log.d(TAG, "queryContactEmailAccounts() - Querying contact (" + displayName + ") for email addresses...");

        String SELECTION =
                ContactsContract.Data.LOOKUP_KEY + " = ?" +
                        " AND " +
                        ContactsContract.Data.MIMETYPE + " = " +
                        "'" + ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE + "'";

        // Define an array that will hold the search criteria
        String[] mSelectionArgs = {mLookupKey};

        Cursor emailCursor = contactResolver.query(
                ContactsContract.Data.CONTENT_URI,
                null,
                SELECTION, mSelectionArgs, null);



        List<Email> contactEmails = new ArrayList<>();
        while (emailCursor.moveToNext()) {

            // This id will be used later when changing or deleting this organization
            String id = emailCursor.getString(emailCursor.getColumnIndex(ContactsContract.Data._ID));
            Log.d(TAG, "ID : " + id);
            String email = emailCursor.getString(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
            Log.d(TAG, "Email Address: " + email);
            int type = emailCursor.getInt(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));
            Log.d(TAG, "Email type: " + type);

            // When detect an email type is of custom type (Phone.TYPE_CUSTOM), extract custom type name from the LABEL column
            String typeName;
            if (type == ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM) {
                typeName = emailCursor.getString(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.LABEL));
            } else {
                typeName = (String) ContactsContract.CommonDataKinds.Email.getTypeLabel(mContext.getResources(), type, "");
            }
            Log.d(TAG, "Email Type Name: " + typeName);

            Email contactEmail = new Email();
            contactEmail.setId(Integer.parseInt(id));
            contactEmail.setEmailAddress(email);
            contactEmail.setEmailType(typeName);
            
            // Add contactEmail object to the list of Email's object
            contactEmails.add(contactEmail);

            Log.d(TAG, "Contact: " + displayName + " - Email: " + email + " - Type: " + typeName);
        }

        // Finally add the list of Email's to the Contact object (no matter whether it is empty or not)
        contact.setEmails((ArrayList<Email>) contactEmails);

        // Once again, do not forget to close the cursor.
        emailCursor.close();
    }

    /**
     * This method queries for addresses details for a contact based on its lookup key. It queries Data table for StructuredPostal MIMETYPE.
     * 
     * Note that for the Contacts Provider, a contact can have zero or more addresses attached to it.
     * 
     * @param contact
     * @param contactResolver
     * @param mLookupKey
     * @param displayName
     */
    private void queryContactPostalAddresses(Contact contact, ContentResolver contactResolver, String mLookupKey, String displayName) {
        Log.d(TAG, "queryContactPostalAddresses() - Querying contact (" + displayName + ") for postal addresses...");

        // First we have to query for the groups IDs that contact belongs to and only then query for their names.
        String SELECTION =
                ContactsContract.Data.LOOKUP_KEY + " = ?" +
                        " AND " +
                        ContactsContract.Data.MIMETYPE + " = " +
                        "'" + ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE + "'";

        // Define an array that will hold the search criteria
        String[] mSelectionArgs = {mLookupKey};

        Cursor addrCur = contactResolver.query(
                ContactsContract.Data.CONTENT_URI,
                null,
                SELECTION, mSelectionArgs, null);

        List<PostalAddress> postalAddresses = new ArrayList<>();
        while (addrCur.moveToNext()) {

        	// Although we are extracting all these StructuredPostal fields, this app only is using StructuredPostal.TYPE and StructuredPostal.STREET on this app.
            // We will let all the others fields stored in the PostalAddress object, so you can use it later if you want
        	String id = addrCur.getString(addrCur.getColumnIndex(ContactsContract.Data._ID)); // This id will  be used later when changing or deleting this address
            Log.d(TAG, "ID : " + id);
            String poBox = addrCur.getString(addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POBOX));
            Log.d(TAG, "Contact PoBox: " + poBox);
            String street = addrCur.getString(addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET));
            Log.d(TAG, "Contact Street: " + street);
            String city = addrCur.getString(addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY));
            Log.d(TAG, "Contact City: " + city);
            String state = addrCur.getString(addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION));
            Log.d(TAG, "Contact State: " + state);
            String postalCode = addrCur.getString(addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE));
            Log.d(TAG, "Contact PostalCode: " + postalCode);
            String country = addrCur.getString(addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY));
            Log.d(TAG, "Contact Country: " + country);
            int type = addrCur.getInt(addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE));
            
            // Address type of a custom type? Retrieve its label from the LABEL column
            String typeName;
            if (type == ContactsContract.CommonDataKinds.StructuredPostal.TYPE_CUSTOM) {
            	typeName = addrCur.getString(addrCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.LABEL));
            } else {
            	typeName = (String) ContactsContract.CommonDataKinds.StructuredPostal.getTypeLabel(mContext.getResources(), type, "");
            }
            Log.d(TAG, "Contact Address Type: " + typeName);

            PostalAddress postalAddress = new PostalAddress();
            postalAddress.setId(Integer.parseInt(id));
            postalAddress.setPoBox(poBox);
            postalAddress.setStreet(street);
            postalAddress.setCity(city);
            postalAddress.setState(state);
            postalAddress.setPostalCode(postalCode);
            postalAddress.setCountry(country);
            postalAddress.setType(typeName);

            // Add postalAddress object to the list of PostalAddress's object
            postalAddresses.add(postalAddress);
        }

        // Finally add the list of PostalAddress's to the Contact object (no matter whether it is empty or not)
        contact.setAddresses((ArrayList<PostalAddress>) postalAddresses);

        // Close the cursor!!!
        addrCur.close();
    }

    /**
     * This method queries for organization details for a contact based on its lookup key. It queries Data table for Organization MIMETYPE.
     *
     * @param contact
     * @param contactResolver
     * @param mLookupKey
     * @param displayName
     */
    private void queryContactOrganization(Contact contact, ContentResolver contactResolver, String mLookupKey, String displayName) {
        Log.d(TAG, "queryContactOrganization() - Querying contact (" + displayName + ") for organization...");

        // First we have to query for the groups IDs that contact belongs to and only then query for their names.
        String SELECTION =
                ContactsContract.Data.LOOKUP_KEY + " = ?" +
                        " AND " +
                        ContactsContract.Data.MIMETYPE + " = " +
                        "'" + ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE + "'";

        // Define an array that will hold the search criteria
        String[] mSelectionArgs = {mLookupKey};

        Cursor cursor = contactResolver.query(
                ContactsContract.Data.CONTENT_URI,
                null,
                SELECTION, mSelectionArgs, null);

        if (cursor.moveToFirst()) {

            // This id will  be used later when changing or deleting this organization
        	String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Data._ID));
            Log.d(TAG, "ID : " + id);
            String companyName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY));
            Log.d(TAG, "Contact Company Name: " + companyName);
            String title = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE));
            Log.d(TAG, "Contact Company Title: " + title);

            Organization organization = new Organization();
            organization.setId(Integer.parseInt(id));
            organization.setName(companyName);
            organization.setTitle(title);

            contact.setOrganization(organization);
        }

        cursor.close();
    }

    /**
     * This method queries for organization details for a contact based on its lookup key. It queries Data table for GroupMembership MIMETYPE.
     * 
     * Groups are not being used by this app. Let it here just for future references
     *
     * @param contact
     * @param contactResolver
     * @param mLookupKey
     */
    private void queryContactGroups(Contact contact, ContentResolver contactResolver, String mLookupKey, String displayName) {
        Log.d(TAG, "queryContactGroups() - Querying contact (" + displayName + ") for groups that it belongs to...");

        // First we have to query for the groups IDs that contact belongs to and only then query for their names.
        String SELECTION =
                ContactsContract.Data.LOOKUP_KEY + " = ?" +
                        " AND " +
                        ContactsContract.Data.MIMETYPE + " = " +
                        "'" + ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE + "'";

        // Define an array that will hold the search criteria
        String[] mSelectionArgs = {mLookupKey};

        Cursor pCur = contactResolver.query(
                ContactsContract.Data.CONTENT_URI,
                null,
                SELECTION, mSelectionArgs, null);


        List<Group> contactGroups = new ArrayList<>();

        while (pCur.moveToNext()) {

            // Data table gives us only groupId. So we will hold it in order to query ContactsContract.Groups.CONTENT_URI for the group details.
            String groupId = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID));

            // Now it is time to query group name. For projection we could pass null to retrieve all the columns, but since the idea is to use only
            // the title, let's query only it.
            String[] PROJECTION = new String[]{
                    ContactsContract.Groups.TITLE
            };

            // Inform the groupId retrieved from the Data table in the selection clause
            SELECTION = String.format("%s = ?", ContactsContract.Groups._ID);
            mSelectionArgs = new String[]{groupId};

            Cursor c = contactResolver.query(
                    ContactsContract.Groups.CONTENT_URI,
                    PROJECTION,
                    SELECTION,
                    mSelectionArgs,
                    null);

            // There is no need to use a while block here, since groupId has a 1x1 relationship with its name
            if (c.moveToFirst()) {

                // Since group title is not well formatted, do some formatting prior to store it in the 'Contact' object
                String gTitle = (c.getString(c.getColumnIndex(ContactsContract.Groups.TITLE)));

                if (gTitle.contains("Group:")) {
                    gTitle = gTitle.substring(gTitle.indexOf("Group:") + 6).trim();

                }
                if (gTitle.contains("Favorite_")) {
                    gTitle = "Favorites";
                }

                // Create a Group object and store its id and name on it.
                Group gObj = new Group();
                gObj.setId(Long.parseLong(groupId));
                gObj.setTitle(gTitle);

                // And finally add it to groups list
                contactGroups.add(gObj);

                if (gTitle.contains("Starred in Android") || gTitle.contains("My Contacts")) {
                    continue;
                }
            }

            // Close the group cursor
            c.close();
        }

        // Close the cursor
        pCur.close();

        // Add the Group list to the Contact object
        contact.setGroups((ArrayList<Group>) contactGroups);
    }

    /**
     * Although it is possible to have multiples raw contacts entries for a contact (e.g.: when user has an account for google, another
     * for whatsapp, etc), this app assumes only a single RawContact per one Contact. So if you have multiples accounts, you will need to
     * change this code to fit your needs.
     *
     * See http://stackoverflow.com/questions/9012263/get-rawcontactid-of-specific-contact-based-off-phonelookup
     *
     * @param contactId
     * @return
     */
    public int getRawContactId(ContentResolver contentResolver, int contactId) {
    	Log.d(TAG, "getRawContactId() - contactId:" + contactId);

        int rawContactId = -1;
        
        // Request RawContacts table for the raw contact id based on its contactId.
    	Cursor c = contentResolver.query(ContactsContract.RawContacts.CONTENT_URI,
    		    new String[]{ContactsContract.RawContacts._ID},
    		    ContactsContract.RawContacts.CONTACT_ID + "=?",
    		    new String[]{String.valueOf(contactId)}, null);
    	
        if (c != null && c.moveToFirst()) {
	        rawContactId = c.getInt(c.getColumnIndex(ContactsContract.RawContacts._ID));
	        Log.d(TAG,"Contact Id: "+contactId+" Raw Contact Id: " + rawContactId);
        }

        c.close();
        
        // TODO: We are not catching any errors here. Implement it later
        return rawContactId;
    }
    
    /**
     * This method is called when user wants to create a new contact entry in the Contacts Provider.
     * 
     * It expects Contact parameter to hold all the necessary information
     * 
     * TODO: Check contacts attributes consistency prior to use them (e.g. fields size, whether they are empty, null, etc).
     * TODO: If an error happens, we are just logging it. We should to warn user about it (maybe throw a business exception)
     * 
     * Note that in all newInsert() method call, we use withValueBackReference() method to inform back the rawId that was created in
     * the first ContentProviderOperation list item. This is a convenience method provided by the system. See link  below for details
     * (Modification back references section):
     * https://developer.android.com/guide/topics/providers/contacts-provider.html
     * 
     * @param contact
     * @param contentResolver
     */
    public void addNewContact(Contact contact, ContentResolver contentResolver) {
        Log.d(TAG, "addNewContact() - Contact:" + contact.getDisplayName());

        try {

        	// ContentProviderOperation allows us to create a batch operation on a Content Provider. Since we will add entries in different tables 
        	// (and different mimetypes), we will use it to add each operation individually (but they will run at once as a single transaction).
        	// It is also possible to create yield points in a batch process. When doing so, all entries between a yield point will act as a single
        	// transaction. See link below for details (On Batch Operations section):
        	// https://developer.android.com/reference/android/provider/ContactsContract.Data.html
            ArrayList<ContentProviderOperation> cpo = new ArrayList<>();
            int contactIndex = cpo.size();

            // First add a Raw contact entry
            // A raw contact will be inserted ContactsContract.RawContacts table in contacts database.
            cpo.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());

            // ------ Image -------
            // If there is a photo URI in the Contact object:
            //   - load its bitmap
            //   - scale it to the maximum size supported by the contact provider
            //   - compress it
            //   - save it to the contact provider
            //
            // If we do not compress it, or try to add an image bigger than the maximum allowed size, we will get a TransactionTooLargeException
            Photo photo = contact.getPhoto();
            if (photo.getStatus() != Status.UNCHANGED) {    // If an image was selected
                    
            	Log.d(TAG, "addNewContact() - Preparing to add contact image URI:" + photo.getContactImageUri());
                
            	// Request Contact Provider the max image size
                final int size = getThumbnailSize(contentResolver);
                
                // Create a bitmap based on the contact photo URI
                Bitmap originalPhoto = MediaStore.Images.Media.getBitmap(contentResolver, Uri.parse(photo.getContactImageUri()));
                
                // Re-scale contact photo by using the max size retrieved from the Contacts Provider.
                final Bitmap scaledPhoto = Bitmap.createScaledBitmap(originalPhoto, size, size, false);
                
                // Convert bitmap to PNG
                byte[] scaledPhotoData   = bitmapToPNGByteArray(scaledPhoto);
    			scaledPhoto.recycle();

                // Adding an insert operation to operations list in order to insert a contact photo
                cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactIndex)
                        .withValue(ContactsContract.Data.IS_SUPER_PRIMARY, 1)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, scaledPhotoData) // Use the scaledPhotoData
                        .build());
            }

            // ------ StructuredNames -------
            Log.d(TAG, "addNewContact() - Preparing StructuredNames...");
            // Adding an insert operation to operations list in order to insert the StructuredName attributes in the Data table
            cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.getDisplayName()) // Name of the contact
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contact.getLastName()) // Name of the person
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contact.getFirstName()) // Name of the person
                    .build());

            // ------ Phones -------
            // For each phone, add an insert operation to operations list in order to insert it to the Data table
            ArrayList<Phone> contactPhones = contact.getPhones();
            for (Phone phone : contactPhones) {
            	Log.d(TAG, "addNewContact() - Preparing to add phone:" + phone.getPhoneNumber());
                int phoneType = ContactsProviderDataConverter.converterPhoneType(mContext, phone.getPhoneType());

                // When phone has a custom label, we need to add it in the LABEL column.
                if (phoneType == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) {
                    cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.getPhoneNumber()) 
                            .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, phone.getPhoneType()) // For custom phone type, add our own label
                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType) // Here it will be value related to CUSTOM type
                            .build());
                
                } else {                 
	                cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
	                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactIndex)
	                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
	                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.getPhoneNumber()) 
	                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType)
	                        .build());
                }
            }

            // ------ E-mails -------
            // For each email, add an insert operation to operations list in order to insert it to the Data table
            ArrayList<Email> contactEmails = contact.getEmails();
            for (Email email : contactEmails) {
            	Log.d(TAG, "addNewContact() - Preparing to add email:" + email.getEmailAddress());
                int emailType = ContactsProviderDataConverter.converterEmailType(mContext, email.getEmailType());

                // When email has a custom label type, we need to add it in the LABEL column.
                if (emailType == ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM) {
                	cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.getEmailAddress())
                            .withValue(ContactsContract.CommonDataKinds.Email.LABEL, email.getEmailType()) // For custom email type, add our own label
                            .withValue(ContactsContract.CommonDataKinds.Email.TYPE, emailType) // Here it will be value related to CUSTOM type
                            .build());
                
                } else {
                    cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.getEmailAddress())
                            .withValue(ContactsContract.CommonDataKinds.Email.TYPE, emailType)
                            .build());
                }
            }

            // ------ Addresses -------
            // For each address, add an insert operation to operations list in order to insert it to the Data table
            ArrayList<PostalAddress> contactAddresses = contact.getAddresses();
            for (PostalAddress address : contactAddresses) {
            	Log.d(TAG, "addNewContact() - Preparing to add address:" + address.getStreet());
                int addressType = ContactsProviderDataConverter.converterAddressType(mContext, address.getType());

                // When address has a custom label type, we need to add it in the LABEL column.
                if (addressType == ContactsContract.CommonDataKinds.StructuredPostal.TYPE_CUSTOM) {                	
                	cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
	                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactIndex)
	                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
	                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address.getStreet())
	                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK)
	                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.STREET, address.getStreet())
	                        //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.CITY, m_szCity)
	                        //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.REGION, m_szState)
	                        //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, m_szZip)
	                        //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, m_szCountry)
	                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.LABEL, address.getType()) // For custom address type, add our own label
                            .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, addressType) // Here it will be value related to CUSTOM type
                            .build());
                
                } else {
                    cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address.getStreet())
                            .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK)
                            .withValue(ContactsContract.CommonDataKinds.StructuredPostal.STREET, address.getStreet())
                            //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.CITY, m_szCity)
                            //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.REGION, m_szState)
                            //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, m_szZip)
                            //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, m_szCountry)
                            .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, addressType)
                            .build());
                }
            }

            // ------ Organization -------
            // Add an insert operation to operations list in order to insert the organization attributes to the Data table
            Organization contactOrganization = contact.getOrganization();
            if (contactOrganization != null) {
            	Log.d(TAG, "addNewContact() - Preparing to add organization:" + contactOrganization.getName());
                cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, contactOrganization.getName())
                        .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK) // Currently we only support WORK organization type
                        .withValue(ContactsContract.CommonDataKinds.Organization.TITLE, contactOrganization.getTitle())
                        .build());
            }
            
            try {

            	// Finally apply the batch
            	Log.i(TAG, "addNewContact() - Applying batch...");
                ContentProviderResult[] res = contentResolver.applyBatch(ContactsContract.AUTHORITY, cpo);
                Uri newContactUri;

                // If success, it will return the new contact URI. We actually do nothing with it. 
                if (res != null && res[0] != null) {
                    newContactUri = res[0].uri;
                    Log.i(TAG, "addNewContact() - URI added contact:" + newContactUri);
                    
                    return;

                } else {
                	// TODO: Here we should return something or throw a business exception so that caller could be notified that contact could not be added.
                    Log.e(TAG, "addNewContact() - Contact not added.");
                }
            } catch (RemoteException e) {
                // TODO: Here we should return something or throw a business exception so that caller could be notified that contact could not be added.
                Log.e(TAG, "addNewContact() - RemoteException while adding contact: " + e.getMessage());

            } catch (OperationApplicationException e) {
                // TODO: Here we should return something or throw a business exception so that caller could be notified that contact could not be added.
                Log.e(TAG, "addNewContact() - OperationApplicationException while adding contact: " + e.getMessage());
            }

        } catch (Exception e) {
            // TODO: Here we should return something or throw a business exception so that caller could be notified that contact could not be added.
            Log.e(TAG, "addNewContact() - Exception adding contact: " + e.getMessage());
        }

        Log.v(TAG, "addNewContact() - End");
    }

    /**
     * This method is called when user wants to change an existent contact entry in the Contacts Provider.
     * 
     * It expects Contact parameter to hold all the necessary information
     *
     * TODO: Check contacts attributes consistency prior to use them (e.g. fields size, whether they are empty, null, etc).
     * TODO: If an error happens, we are just logging it. We should to warn user about it (maybe throw a business exception)
     * TODO: This method is pretty similar to the addNewContact() method, so it would be nice to create a single method to handle both situation.
     *
     * Note that in all newInsert() method call, we use withValueBackReference() method to inform back the rawId that was created in
     * the first ContentProviderOperation list item. This is a convenience method provided by the system. See link  below for details
     * (Modification back references section):
     * https://developer.android.com/guide/topics/providers/contacts-provider.html
     * 
     * @param contact
     * @param contentResolver
     */
    public void editContact(Contact contact, ContentResolver contentResolver) {
        
    	Log.d(TAG, "editContact() - Contact:" + contact.getDisplayName());

    	try {

            ArrayList<ContentProviderOperation> cpo = new ArrayList<>();

            // ------ Image -------
            Photo photo = contact.getPhoto();
            if (photo.getStatus() != Status.UNCHANGED) {    // If an image was selected

            	// Request Contact Provider the max image size
                final int size = getThumbnailSize(contentResolver);
                
                // Create a bitmap based on the contact photo URI
                Bitmap originalPhoto = MediaStore.Images.Media.getBitmap(contentResolver, Uri.parse(photo.getContactImageUri()));
                
                // Re-scale contact photo by using the max size retrieved from the Contacts Provider.
                final Bitmap scaledPhoto = Bitmap.createScaledBitmap(originalPhoto, size, size, false);
                
                // Convert bitmap to PNG
                byte[] scaledPhotoData   = bitmapToPNGByteArray(scaledPhoto);
    			scaledPhoto.recycle();

    			// When changing a contact, if there was no photo before, we need to add it by calling newInsert() method. Note that, instead of using
    			// 'withValueBackReference()' in the add operation (as we did in the addContact method) , we use 'withValue()', since we already 
    			// have the rawContactId.
                if (photo.getStatus() == Status.NEW) {
                    Log.d(TAG, "editContact() - Preparing to add a new contact image. URI:" + photo.getContactImageUri());

                    cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.getRawContactId())
                            .withValue(ContactsContract.Data.IS_SUPER_PRIMARY, 1)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, scaledPhotoData) // Use the scaledPhotoData
                            .build());
                }

                // If there was a photo in the contact already , we must call newUpdate() instead of newInsert().
                if (photo.getStatus() == Status.UPDATED) {
                    Log.d(TAG, "editContact() - Preparing to update contact image. URI:" + photo.getContactImageUri());

                    // Adding newUpdate operation to operations list to update Photo in the table ContactsContract.Data
                    cpo.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                            .withSelection(ContactsContract.Data.RAW_CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'",
                                    new String[]{String.valueOf(contact.getRawContactId())})
                            .withValue(ContactsContract.Data.IS_SUPER_PRIMARY, 1)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, scaledPhotoData) // Use the scaledPhotoData
                            .build());
                }

                // We currently do not support delete a contact photo. If you want to add this feature, just put your code inside this block. Also
                // you have to change GUI layer in order to add Status.DELETED flag when removing a contact photo.
                if (photo.getStatus() == Status.DELETED) {
                    Log.d(TAG, "editContact() - Preparing to delete contact image. URI:" + photo.getContactImageUri());
                }

            }

            // ------ StructuredNames -------
            Log.d(TAG, "editContact() - Preparing structured names...");
            cpo.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(ContactsContract.Data._ID + " = ? AND " + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE + "'",
                            new String[]{String.valueOf(contact.getStructuredName().getId())})
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contact.getFirstName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contact.getLastName())
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.getDisplayName())
                    .build());
            
            // ------ Phones -------
            ArrayList<Phone> contactPhones = contact.getPhones();
            for (Phone phone : contactPhones) {
            	
            	// When adding a new phone, use newInsert() method.
            	if (phone.getStatus() == Status.NEW) {
            		Log.d(TAG, "editContact() - Preparing to add a new phone:" + phone.getPhoneNumber());
                    
            		int phoneType = ContactsProviderDataConverter.converterPhoneType(mContext, phone.getPhoneType());

                    // When phone has a custom label, we need to add it in the LABEL column.
                    if (phoneType == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) {
                        cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        		.withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.getRawContactId()) // Add the Raw Contact Id
    	                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.getPhoneNumber()) // Number to be added
                                .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, phone.getPhoneType()) // For custom phone type, add our own label
                                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType) // Here it will be value related to CUSTOM type
                                .build());
                    
                    } else {                 
    	                cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
    	                		.withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.getRawContactId()) // Add the Raw Contact Id
    	                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
    	                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.getPhoneNumber()) // Number to be added
    	                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType)
    	                        .build());
                    }
                }
            	
            	// When change an existent phone, use newUpdate() method.
            	if (phone.getStatus() == Status.UPDATED) {
            		Log.d(TAG, "editContact() - Preparing to change phone:" + phone.getPhoneNumber());
                    
            		int phoneType = ContactsProviderDataConverter.converterPhoneType(mContext, phone.getPhoneType());
	
            		// When phone has a custom label, we need to add it in the LABEL column.
                    if (phoneType == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) {
                    	cpo.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
		                		.withSelection( ContactsContract.Data._ID + " = ? AND " + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'",
		                                new String[] { String.valueOf( phone.getId() ) } )
		                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.getPhoneNumber()) // Number to be added
		                        .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, phone.getPhoneType()) // For custom phone type, add our own label
                                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType)
		                        .build());
                    } else {
		                cpo.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
		                		.withSelection( ContactsContract.Data._ID + " = ? AND " + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'",
		                                new String[] { String.valueOf( phone.getId() ) } )
		                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.getPhoneNumber()) // Number to be added
		                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType)
		                        .build());
                    }
            	}
            	
            	// When removing a phone, use newDelete() method.
            	if (phone.getStatus() == Status.DELETED) {
            		Log.d(TAG, "editContact() - Preparing to delete phone:" + phone.getPhoneNumber());
                    
	                // Mobile number will be deleted from the ContactsContract.Data table
	                cpo.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
	                		.withSelection( ContactsContract.Data._ID + " = ? AND " + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'",
	                                new String[] { String.valueOf( phone.getId() ) } )
	                        .build());
            	}
            }
            
            // ------ E-mails -------
            ArrayList<Email> contactEmails = contact.getEmails();
            for (Email email : contactEmails) {
            	
            	// When adding a new email, use newInsert() method.
            	if (email.getStatus() == Status.NEW) {
            		Log.d(TAG, "editContact() - Preparing to add new email:" + email.getEmailAddress());
                    
            		int emailType = ContactsProviderDataConverter.converterEmailType(mContext, email.getEmailType());
	
            		// When email has a custom label type, we need to add it in the LABEL column.
                    if (emailType == ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM) {
                    	cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    			.withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.getRawContactId()) // Add the Raw Contact Id
    	                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.getEmailAddress())
                                .withValue(ContactsContract.CommonDataKinds.Email.LABEL, email.getEmailType()) // For custom email type, add our own label
                                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, emailType) // Here it will be value related to CUSTOM type
                                .build());
                    
                    } else {  
    	                cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
    	                		.withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.getRawContactId()) // Add the Raw Contact Id
    	                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
    	                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.getEmailAddress())
    	                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, emailType)
    	                        .build());
                    }
            	}
            	
            	// When change an existent email, use newUpdate() method.
            	if (email.getStatus() == Status.UPDATED) {
            		Log.d(TAG, "editContact() - Preparing to change email:" + email.getEmailAddress());
                    
            		int emailType = ContactsProviderDataConverter.converterEmailType(mContext, email.getEmailType());
            		
            		// When email has a custom label type, we need to add it in the LABEL column.
                    if (emailType == ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM) {
                    
		                cpo.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
		                		.withSelection( ContactsContract.Data._ID + " = ? AND " + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE + "'",
		                                new String[] { String.valueOf( email.getId() ) } )
		                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.getEmailAddress())
		                        .withValue(ContactsContract.CommonDataKinds.Email.LABEL, email.getEmailType()) // For custom email type, add our own label
                                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, emailType)
		                        .build());
                    
                    } else {
                    	cpo.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
		                		.withSelection( ContactsContract.Data._ID + " = ? AND " + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE + "'",
		                                new String[] { String.valueOf( email.getId() ) } )
		                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.getEmailAddress())
		                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, emailType)
		                        .build());
                    }
            	}
            	
            	// When removing an email, use newDelete() method.
            	if (email.getStatus() == Status.DELETED) {
            		Log.d(TAG, "editContact() - Preparing to delete email:" + email.getEmailAddress());
                    
            		// Email account will be deleted from the ContactsContract.Data table
	                cpo.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
	                		.withSelection( ContactsContract.Data._ID + " = ? AND " + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE + "'",
	                                new String[] { String.valueOf( email.getId() ) } )
	                        .build());
            	}
            }
            
            // ------ Addresses -------
            ArrayList<PostalAddress> contactAddresses = contact.getAddresses();
            for (PostalAddress address : contactAddresses) {
            	
            	// When adding a new address, use newInsert() method.
            	if (address.getStatus() == Status.NEW) {
            		Log.d(TAG, "editContact() - Preparing to add new address:" + address.getStreet());
                    
            		int addressType = ContactsProviderDataConverter.converterAddressType(mContext, address.getType());
	
            		if (addressType == ContactsContract.CommonDataKinds.StructuredPostal.TYPE_CUSTOM) {
                    	cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    			.withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.getRawContactId()) // Add the Raw Contact Id
    	                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
    	                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address.getStreet())
    	                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK)
    	                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.STREET, address.getStreet())
    	                        //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.CITY, m_szCity)
    	                        //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.REGION, m_szState)
    	                        //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, m_szZip)
    	                        //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, m_szCountry)
    	                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.LABEL, address.getType()) // For custom address type, add our own label
                                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, addressType) // Here it will be value related to CUSTOM type
                                .build());
                    
                    } else {  
    	            	cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
    	            			.withValue(ContactsContract.Data.RAW_CONTACT_ID, contact.getRawContactId()) // Add the Raw Contact Id
    	                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
    	                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address.getStreet())
    	                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK)
    	                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.STREET, address.getStreet())
    	                        //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.CITY, m_szCity)
    	                        //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.REGION, m_szState)
    	                        //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, m_szZip)
    	                        //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, m_szCountry)
    	                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, addressType)
    	                        .build());
                    }
            	}
            	
            	// When change an existent address, use newUpdate() method.
            	if (address.getStatus() == Status.UPDATED) {
            		Log.d(TAG, "editContact() - Preparing to change address:" + address.getStreet());
                    
            		int addressType = ContactsProviderDataConverter.converterAddressType(mContext, address.getType());
	            	
            		if (addressType == ContactsContract.CommonDataKinds.StructuredPostal.TYPE_CUSTOM) {
		            	cpo.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
		            			.withSelection( ContactsContract.Data._ID + " = ? AND " + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE + "'",
		                                new String[] { String.valueOf( address.getId() ) } )
		                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address.getStreet())
		                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK)
		                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.STREET, address.getStreet())
		                        //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.CITY, m_szCity)
		                        //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.REGION, m_szState)
		                        //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, m_szZip)
		                        //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, m_szCountry)
		                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.LABEL, address.getType()) // For custom address type, add our own label
                                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, addressType)
		                        .build());
		            	
            		} else {
		            	cpo.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
		            			.withSelection( ContactsContract.Data._ID + " = ? AND " + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE + "'",
		                                new String[] { String.valueOf( address.getId() ) } )
		                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address.getStreet())
		                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK)
		                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.STREET, address.getStreet())
		                        //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.CITY, m_szCity)
		                        //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.REGION, m_szState)
		                        //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, m_szZip)
		                        //.withValue(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, m_szCountry)
		                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, addressType)
		                        .build());

            		}
            	}
            	
            	// When removing an address, use newDelete() method.
            	if (address.getStatus() == Status.DELETED) {
            		Log.d(TAG, "editContact() - Preparing to delete address:" + address.getStreet());
                    
            		// Address will be deleted from the ContactsContract.Data table
	                cpo.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
	                		.withSelection( ContactsContract.Data._ID + " = ? AND " + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE + "'",
	                                new String[] { String.valueOf( address.getId() ) } )
	                        .build());
            	}
            }
            
            // ------ Organization -------
            Organization contactOrganization = contact.getOrganization();
            if (contactOrganization != null) {
            	Log.d(TAG, "editContact() - Preparing to change organization:" + contactOrganization.getName());
                
            	cpo.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection( ContactsContract.Data._ID + " = ? AND " + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE + "'",
                        		new String[] { String.valueOf( contact.getOrganization().getId() ) } )
                        .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, contactOrganization.getName())
                        .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK) // Currently we only support WORK organization type
                        .withValue(ContactsContract.CommonDataKinds.Organization.TITLE, contactOrganization.getTitle())
                        .build());
            }
            
            try {

            	// Now apply the batch operation
            	Log.i(TAG, "editContact() - Applying batch...");
                ContentProviderResult[] res = contentResolver.applyBatch(ContactsContract.AUTHORITY, cpo);
                Uri updatedContactUri;

                if (res != null && res[0] != null) {
                	updatedContactUri = res[0].uri;
                    Log.i(TAG, "editContact() - URI changed contact:" + updatedContactUri);
                    return;

                } else {
                	// TODO: Here we should return something or throw an exception so that caller could be notified that contact could not be changed.
                    Log.e(TAG, "editContact() - Contact not changed.");
                }
            } catch (RemoteException e) {
            	// TODO: Here we should return something or throw an exception so that caller could be notified that contact could not be changed.
                Log.e(TAG, "editContact() - RemoteException while changing contact: " + e.getMessage());

            } catch (OperationApplicationException e) {
            	// TODO: Here we should return something or throw an exception so that caller could be notified that contact could not be changed.
                Log.e(TAG, "editContact() - OperationApplicationException while changing contact: " + e.getMessage());
            }
            
        } catch (Exception e) {
        	// TODO: Here we should return something or throw an exception so that caller could be notified that contact could not be changed.
            Log.e(TAG, "editContact() - Exception editing contact: " + e.getMessage());
        }

        Log.v(TAG, "editContact() - End");
    }

    public void deleteContact(Contact contact, ContentResolver contentResolver, String mLookupKey) {
    	Log.v(TAG, "deleteContact() - Begin");

        try {
        	// When deleting a contact, just delete it from the ContactsContract.Contacts.CONTENT_LOOKUP_URI. All data associated to it
        	// in the Data table will be also deleted.
            Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, mLookupKey);
            int numberOfRows = contentResolver.delete(uri, null, null);
            
            if (numberOfRows > 0) { 
            	Log.i(TAG, "deleteContact() - Contact: " + contact.getDisplayName() + " deleted successfully.");
            } else {
            	// TODO: Here we should return something or throw an exception so that caller could be notified that contact could not be deleted.
                Log.i(TAG, "deleteContact() - Could not delete contact: " + contact.getDisplayName());
            }
        }
        catch(Exception e) {
        	// TODO: Here we should return something or throw an exception so that caller could be notified that contact could not be deleted.
        	Log.e(TAG, "deleteContact() - Exception when deleting a contact: " + e.getMessage());
        }

        Log.v(TAG, "deleteContact() - End");
    }
        
    /**
     * This method queries contacts provider for the maximum thumbnail size. It will be use to re-scale contact photo prior to save it.
     *
     * Code downloaded from http://stackoverflow.com/questions/17834815/how-to-find-the-max-image-size-supported-for-contacts-images
     *
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static int getThumbnailSize(ContentResolver contentResolver) {
    
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // Note that this URI is safe to call on the UI thread.
            final Uri uri = ContactsContract.DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI;
            final String[] projection = new String[] { ContactsContract.DisplayPhoto.DISPLAY_MAX_DIM };
            final Cursor c = contentResolver.query(uri, projection, null, null, null);

            try {
                c.moveToFirst();

                return c.getInt(0);

            } catch (Exception e) {
                Log.w(TAG, "Unable to get thumbnail size: " + e.toString());
            } finally {
                c.close();
            }
        }
        // fallback: 96x96 is the max contact photo size for pre-ICS versions
        return 96;
    }
    
    /**
     * This code basically compresses the bitmap and return a byte array containing the image.
     *
     * Code downloaded from https://gist.github.com/slightfoot/5985900
     *
     */
    public static byte[] bitmapToPNGByteArray(Bitmap bitmap)
	{
		final int size = bitmap.getWidth() * bitmap.getHeight() * 4;
		final ByteArrayOutputStream out = new ByteArrayOutputStream(size);
		try {
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
			out.flush();
			out.close();
			return out.toByteArray();
		}
		catch(IOException e){
			Log.w(TAG, "Unable to serialize photo: " + e.toString());
			return null;
		}
	}
}

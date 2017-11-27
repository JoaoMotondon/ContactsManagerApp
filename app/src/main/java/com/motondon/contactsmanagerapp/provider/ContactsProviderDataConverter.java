package com.motondon.contactsmanagerapp.provider;

import android.content.Context;
import android.provider.ContactsContract;

import com.motondon.contactsmanagerapp.R;

/**
 *
 * This class is intended to convert Phone, Email and Address data from the local format to the Contacts Provider format.
 * 
 * This is not the best way to do this, since it is using resources (string array in the string.xml file) and relying in
 * the pre-defined position. If later we want to add a new type (ex, adding work address), we need to change multiple files. 
 *
 * Actually, there are many parts on this app to be improved, but since the main focus of this app is to demonstrate how to
 * manually query the Contacts provider, we will not improve them at all.
 *
 */
class ContactsProviderDataConverter {

    public static int converterPhoneType(Context context, String type) {
        final String[] contactPhoneTypes =  context.getResources().getStringArray(R.array.contact_phone_types);

        if (type.equals(contactPhoneTypes[0])) { // Home
            return ContactsContract.CommonDataKinds.Phone.TYPE_HOME;

        } else if (type.equals(contactPhoneTypes[1])) { // Mobile
            return ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;

        } else if (type.equals(contactPhoneTypes[2])) { // Work
            return ContactsContract.CommonDataKinds.Phone.TYPE_WORK;

        } else { // Custom
            return ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM;
        }
    }
    
    public static int converterEmailType(Context context, String type) {
        final String[] contactEmailTypes =  context.getResources().getStringArray(R.array.contact_mail_types);

        if (type.equals(contactEmailTypes[0])) { // Home
            return ContactsContract.CommonDataKinds.Email.TYPE_HOME;

        } else if (type.equals(contactEmailTypes[1])) { // Work
            return ContactsContract.CommonDataKinds.Email.TYPE_WORK;

        } else { // Custom
            return ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM;
        }
    }
    
    public static int converterAddressType(Context context, String type) {
        final String[] contactEmailTypes =  context.getResources().getStringArray(R.array.contact_address_types);

        if (type.equals(contactEmailTypes[0])) { // Home
            return ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME;

        } else if (type.equals(contactEmailTypes[1])) { // Work
            return ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK;

        } else { // Custom
            return ContactsContract.CommonDataKinds.StructuredPostal.TYPE_CUSTOM;
        }
    }
}

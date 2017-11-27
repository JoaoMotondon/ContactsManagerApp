package com.motondon.contactsmanagerapp.model;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * This is the POJO class which holds all contact attributes used by this app. It is used when fetching a contact from 
 * the Contact Provider as well as when adding/changing a contact by this app
 *  
 */
public class Contact implements Serializable {
    private int id;
    private int rawContactId;
    private long contactID;
    private String lookupKey;
    private Photo photo;
    private StructuredName structuredName;
    private ArrayList<Phone> phones;
    private ArrayList<Email> emails;
    private ArrayList<PostalAddress> addresses;
    private ArrayList<Group> groups;
    private Organization organization;

    public Contact() {
        photo = new Photo();
        structuredName = new StructuredName();
        phones = new ArrayList<>();
        emails = new ArrayList<>();
        addresses = new ArrayList<>();
        groups = new ArrayList<>();
        organization = new Organization();
    }

    /**
     * Copy Constructor. It creates a Contact object from another one by value. For the lists, it 
     * iterates over them and copy each item one by one.
     * 
     * @param another
     */
    public Contact(Contact another) {
        this();

        id = another.id;
        rawContactId = another.rawContactId;
        contactID = another.contactID;
        lookupKey = another.lookupKey;
        photo = new Photo(another.photo);
        structuredName = new StructuredName(another.structuredName);

        for(Phone phone : another.getPhones()) {
            phones.add(phone);
        }

        for(Email email : another.getEmails()) {
            emails.add(email);
        }

        for(PostalAddress address : another.getAddresses()) {
            addresses.add(address);
        }

        for(Group group : another.getGroups()) {
            groups.add(group);
        }

        organization = new Organization(another.organization);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRawContactId() {
    	return this.rawContactId;
    }
    
	public void setRawContactId(int rawContactId) {
		this.rawContactId = rawContactId;		
	}
    
	public Long getContactID() {
        return contactID;
    }

    public void setContactID(long contactID) {
        this.contactID = contactID;
    }

    public String getLookupKey() {
        return this.lookupKey;
    }

    public void setLookupKey(String lookupKey) {
        this.lookupKey = lookupKey;
    }

    public Photo getPhoto() {
        return photo;
    }

    public void setPhoto(Photo photo) {
        this.photo = photo;
    }

    public StructuredName getStructuredName() {
    	return this.structuredName;
    }
    
    public String getDisplayName() {
        return this.structuredName.getDisplayName();
    }

	public String getLastName() {
        return this.structuredName.getLastName();
    }

    public String getFirstName() {
        return this.structuredName.getFirstName();
    }

    public void setStructuredName(StructuredName structuredName) {
        this.structuredName = structuredName;
    }

    public ArrayList<Phone> getPhones() {
        return phones;
    }

    public void setPhones(ArrayList<Phone> phones) {
        this.phones = phones;
    }

    public ArrayList<Email> getEmails() {
        return emails;
    }

    public void setEmails(ArrayList<Email> emails) {
        this.emails = emails;
    }

    public ArrayList<PostalAddress> getAddresses() {
        return addresses;
    }

    public void setAddresses(ArrayList<PostalAddress> addresses) {
        this.addresses = addresses;
    }

    public ArrayList<Group> getGroups() {
        return groups;
    }

    public void setGroups(ArrayList<Group> groups) {
        this.groups = groups;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    /**
     * Helper method to add a phone to the phone's list or update when it already exists
     * 
     */
    public void addPhone(Phone p) {
        int index = phones.indexOf(p);
        if (index == -1) {
            phones.add(p);
        } else {
            phones.set(index, p);
        }
    }

    /**
     * Helper method to add an email to the email's list or update when it already exists
     * 
     */
    public void addMail(Email m) {
        int index = emails.indexOf(m);
        if (index == -1) {
            emails.add(m);
        } else {
            emails.set(index, m);
        }
    }

    /**
     * Helper method to add an address to the address'es list or update when it already exists
     * 
     */
    public void addAddress(PostalAddress address) {
        int index = addresses.indexOf(address);
        if (index == -1) {
            addresses.add(address);
        } else {
            addresses.set(index, address);
        }
    }
}

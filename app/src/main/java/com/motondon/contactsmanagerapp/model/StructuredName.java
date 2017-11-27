package com.motondon.contactsmanagerapp.model;

import java.io.Serializable;

public class StructuredName implements Serializable {
    
	private int id;
	private String displayName;
	private String firstName;
	private String lastName;
    
    public StructuredName() {
    }

    public StructuredName(StructuredName another) {
        id = another.getId();
        displayName = another.getDisplayName();
        firstName = another.getFirstName();
        lastName = another.getLastName();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

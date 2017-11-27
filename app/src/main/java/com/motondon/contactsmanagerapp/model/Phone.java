package com.motondon.contactsmanagerapp.model;

import java.io.Serializable;

public class Phone implements Serializable {
    
	private enum PHONE_TYPE {
		TYPE_HOME ("Home"),
        TYPE_MOBILE ("Mobile"),
        TYPE_WORK ("Work"),
        TYPE_CUSTOM ("Custom");
		
		private final String type;
		
		PHONE_TYPE(String type) {
			this.type = type;
		}
		
		public boolean equalsName(String otherType) {
	        return (otherType == null) ? false : type.equals(otherType);
	    }

	    public String toString() {
	       return this.type;
	    }
	}
	
	private int id;
	private String phoneNumber;
    private Status status;
    private String phoneType;

    public Phone() {
        status = Status.UNCHANGED;
    }

    public Phone(Phone another) {
        id = another.getId();
        phoneNumber = another.getPhoneNumber();
        status = another.getStatus();
        phoneType = another.getPhoneType();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneType() {
        return phoneType;
    }

    public void setPhoneType(String tag) {
        this.phoneType = tag;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return phoneType + " : " + phoneNumber;
    }
}

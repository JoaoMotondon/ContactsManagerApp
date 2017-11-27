package com.motondon.contactsmanagerapp.model;

import java.io.Serializable;

public class Email implements Serializable {
    
	private int id;
    private String emailAddress;
    private String emailType;
    private Status status;

    public Email() {
        status = Status.UNCHANGED;
    }

    public Email(Email another) {
        id = another.id;
        emailAddress = another.emailAddress;
        emailType = another.emailType;
        status = another.status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getEmailType() {
        return emailType;
    }

    public void setEmailType(String tag) {
        this.emailType = tag;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return emailType + " : " + emailAddress;
    }
}

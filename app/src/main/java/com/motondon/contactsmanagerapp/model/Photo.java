package com.motondon.contactsmanagerapp.model;

import java.io.Serializable;

public class Photo implements Serializable {

    private int id;
    private String contactImageUri;
    private Status status;

    public Photo() {
        status = Status.UNCHANGED;
    }

    public Photo(Photo another) {
        id = another.getId();
        contactImageUri = another.getContactImageUri();
        status = another.getStatus();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContactImageUri() {
        return contactImageUri;
    }

    public void setContactImageUri(String contactImageUri) {
        this.contactImageUri = contactImageUri;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return contactImageUri;
    }
}

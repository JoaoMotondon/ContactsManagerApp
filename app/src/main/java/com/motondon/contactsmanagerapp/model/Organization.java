package com.motondon.contactsmanagerapp.model;

import java.io.Serializable;

public class Organization implements Serializable {

	private int id;
    private String name;
    private String title;
    private Status status;

    public Organization() {
    }

    public Organization(Organization another) {
        id = another.getId();
        name = another.getName();
        title = another.getTitle();
        status = another.getStatus();
    }

    public int getId() {
    	return this.id;
    }
    
    public void setId(int id) {
    	this.id = id;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}

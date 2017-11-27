package com.motondon.contactsmanagerapp.model;

import java.io.Serializable;

public class Group implements Serializable {
    private long id;
    private String title;

    public Group() {
    }

    public Group(Group another) {
        id = another.id;
        title = another.title;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return title;
    }
}

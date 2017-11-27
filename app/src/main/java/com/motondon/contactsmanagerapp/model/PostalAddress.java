package com.motondon.contactsmanagerapp.model;

import java.io.Serializable;

public class PostalAddress implements Serializable {

	private int id;
    private String poBox;
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String type;
    private Status status;

    public PostalAddress() {
        status = Status.UNCHANGED;
    }

    public PostalAddress(PostalAddress another) {
        id = another.getId();
        poBox = another.getPoBox();
        street = another.getStreet();
        city = another.getCity();
        state = another.getState();
        postalCode = another.getPostalCode();
        country = another.getCountry();
        type = another.getType();
        status = another.getStatus();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPoBox() {
        return poBox;
    }

    public void setPoBox(String poBox) {
        this.poBox = poBox;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return type + " : " + street;
    }
}

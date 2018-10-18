package org.servantscode.person;

import java.util.List;

public class Family {
    private int id;
    private String surname;
    private List<Person> memebers;
    private Address address;

    public Family() {}

    public Family(int id, String surname) {
        this.id = id;
        this.surname = surname;
    }

    // ----- Accessors -----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public List<Person> getMembers() { return memebers; }
    public void setMemebers(List<Person> memebers) { this.memebers = memebers; }

    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }
}

package org.servantscode.person;

import java.util.Date;

public class Person {
    private String name;
    private Date birthdate;
    private Date memeberSince;
    private String phoneNumber;
    private boolean headOfHousehold;
    private Address address;
    private String id;

    public Person(String name) {
        this.name = name;
    }

    // ----- Accessors -----
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Date getBirthdate() { return birthdate; }
    public void setBirthdate(Date birthdate) { this.birthdate = birthdate; }

    public Date getMemeberSince() { return memeberSince; }
    public void setMemeberSince(Date memeberSince) { this.memeberSince = memeberSince; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public boolean isHeadOfHousehold() { return headOfHousehold; }
    public void setHeadOfHousehold(boolean headOfHousehold) { this.headOfHousehold = headOfHousehold; }

    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }
}

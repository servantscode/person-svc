package org.servantscode.person;

import java.util.Date;

public class Person {
    private int id;
    private String name;
    private Date birthdate;
    private Date memeberSince;
    private long phoneNumber;
    private String email;

    public Person() {}
    public Person(String name) {
        this.name = name;
    }
    public Person(int id, String name) {
        this.id = id;
        this.name = name;
    }

    // ----- Accessors -----

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Date getBirthdate() { return birthdate; }
    public void setBirthdate(Date birthdate) { this.birthdate = birthdate; }

    public Date getMemeberSince() { return memeberSince; }
    public void setMemeberSince(Date memeberSince) { this.memeberSince = memeberSince; }

    public long getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(long phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

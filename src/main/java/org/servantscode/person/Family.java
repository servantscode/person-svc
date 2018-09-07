package org.servantscode.person;

import java.util.List;

public class Family {
    private String surname;
    private Person headOfHousehold;
    private List<Person> memebers;
    private String id;

    // ----- Accessors -----
    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public Person getHeadOfHousehold() { return headOfHousehold; }
    public void setHeadOfHousehold(Person headOfHousehold) { this.headOfHousehold = headOfHousehold; }

    public List<Person> getMemebers() { return memebers; }
    public void setMemebers(List<Person> memebers) { this.memebers = memebers; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}

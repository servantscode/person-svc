package org.servantscode.person;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.ZonedDateTime;

public class Person {
    private int id;
    private String name;

    private String phoneNumber;
    private String email;
    private Family family;
    private boolean headOfHousehold;
    private ZonedDateTime birthdate;
    private ZonedDateTime memberSince;
    private String photoGuid;

    @JsonIgnore
    private int familyId;

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

    public ZonedDateTime getBirthdate() { return birthdate; }
    public void setBirthdate(ZonedDateTime birthdate) { this.birthdate = birthdate; }

    public ZonedDateTime getMemberSince() { return memberSince; }
    public void setMemberSince(ZonedDateTime memberSince) { this.memberSince = memberSince; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email;}

    public Family getFamily() { return family; }
    public void setFamily(Family family) { this.family = family; }

    public int getFamilyId() { return familyId; }
    public void setFamilyId(int familyId) { this.familyId = familyId; }

    public boolean isHeadOfHousehold() { return headOfHousehold; }
    public void setHeadOfHousehold(boolean headOfHousehold) { this.headOfHousehold = headOfHousehold; }

    public String getPhotoGuid() { return photoGuid; }
    public void setPhotoGuid(String photoGuid) { this.photoGuid = photoGuid; }
}

package org.servantscode.person;

import java.time.LocalDate;
import java.util.List;

public class Family {
    private int id;
    private String surname;
    private String homePhone;
    private int envelopeNumber;
    private List<Person> members;
    private Address address;
    private String photoGuid;
    private boolean inactive;
    private LocalDate inactiveSince;

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

    public String getHomePhone() { return homePhone; }
    public void setHomePhone(String homePhone) { this.homePhone = homePhone; }

    public int getEnvelopeNumber() { return envelopeNumber; }
    public void setEnvelopeNumber(int envelopeNumber) { this.envelopeNumber = envelopeNumber; }

    public List<Person> getMembers() { return members; }
    public void setMembers(List<Person> members) { this.members = members; }

    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }

    public String getPhotoGuid() { return photoGuid; }
    public void setPhotoGuid(String photoGuid) { this.photoGuid = photoGuid; }

    public boolean isInactive() { return inactive; }
    public void setInactive(boolean inactive) { this.inactive = inactive; }

    public LocalDate getInactiveSince() { return inactiveSince; }
    public void setInactiveSince(LocalDate inactiveSince) { this.inactiveSince = inactiveSince; }
}

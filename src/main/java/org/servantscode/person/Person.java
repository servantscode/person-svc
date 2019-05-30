package org.servantscode.person;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;

public class Person {
    public enum MaritalStatus { SINGLE, MARRIED_IN_CHURCH, MARRIED_OUTSIDE_CHURCH, SEPARATED, DIVORCED, ANNULLED, WIDOWED };
    public enum Ethnicity { AFRICAN_AMERICAN, ASIAN, EUROPEAN_AMERICAN, HINDU, LATINO, VIETNAMESE};
    public enum Language { ENGLISH, SPANISH, VIETNAMESE };

    private int id;
    private String name;
    private boolean male;

    private String phoneNumber;
    private String email;
    private Family family;
    private boolean headOfHousehold;
    private LocalDate birthdate;
    private LocalDate memberSince;
    private String photoGuid;

    private boolean inactive;
    private boolean parishioner;
    private boolean baptized;
    private boolean confession;
    private boolean communion;
    private boolean confirmed;
    private MaritalStatus maritalStatus;
    private Ethnicity ethnicity;
    private Language primaryLanguage;

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

    public boolean isMale() { return male; }
    public void setMale(boolean male) { this.male = male; }

    public LocalDate getBirthdate() { return birthdate; }
    public void setBirthdate(LocalDate birthdate) { this.birthdate = birthdate; }

    public LocalDate getMemberSince() { return memberSince; }
    public void setMemberSince(LocalDate memberSince) { this.memberSince = memberSince; }

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

    public boolean isInactive() { return inactive; }
    public void setInactive(boolean inactive) { this.inactive = inactive; }

    public boolean isParishioner() { return parishioner; }
    public void setParishioner(boolean parishioner) { this.parishioner = parishioner; }

    public boolean isBaptized() { return baptized; }
    public void setBaptized(boolean baptized) { this.baptized = baptized; }

    public boolean isConfession() { return confession; }
    public void setConfession(boolean confession) { this.confession = confession; }

    public boolean isCommunion() { return communion; }
    public void setCommunion(boolean communion) { this.communion = communion; }

    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }

    public MaritalStatus getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(MaritalStatus maritalStatus) { this.maritalStatus = maritalStatus; }

    public Ethnicity getEthnicity() { return ethnicity; }
    public void setEthnicity(Ethnicity ethnicity) { this.ethnicity = ethnicity; }

    public Language getPrimaryLanguage() { return primaryLanguage; }
    public void setPrimaryLanguage(Language primaryLanguage) { this.primaryLanguage = primaryLanguage; }
}

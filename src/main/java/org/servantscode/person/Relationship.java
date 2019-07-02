package org.servantscode.person;

public class Relationship {
    public enum RelationshipType { SPOUSE, FATHER, MOTHER, CHILD, SIBLING,
                                   STEP_FATHER, STEP_MOTHER, STEP_CHILD, STEP_SIBLING,
                                   GRANDFATHER, GRANDMOTHER, GRANDCHILD,
                                   OTHER };

    private int personId;
    private String personName;
    private int otherId;
    private String otherName;
    private RelationshipType relationship;
    private boolean guardian;
    private int contactPreference;
    private boolean doNotContact;

    public Relationship() {};

    // ----- Accessors -----
    public int getPersonId() { return personId; }
    public void setPersonId(int personId) { this.personId = personId; }

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }

    public int getOtherId() { return otherId; }
    public void setOtherId(int otherId) { this.otherId = otherId; }

    public String getOtherName() { return otherName; }
    public void setOtherName(String otherName) { this.otherName = otherName; }

    public RelationshipType getRelationship() { return relationship; }
    public void setRelationship(RelationshipType relationship) { this.relationship = relationship; }

    public boolean isGuardian() { return guardian; }
    public void setGuardian(boolean guardian) { this.guardian = guardian; }

    public int getContactPreference() { return contactPreference; }
    public void setContactPreference(int contactPreference) { this.contactPreference = contactPreference; }

    public boolean isDoNotContact() { return doNotContact; }
    public void setDoNotContact(boolean doNotContact) { this.doNotContact = doNotContact; }
}

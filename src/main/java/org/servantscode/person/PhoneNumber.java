package org.servantscode.person;

public class PhoneNumber {
    public enum PhoneNumberType { CELL, HOME, WORK, OTHER }

    private String phoneNumber;
    private PhoneNumberType type;
    private boolean primary;

    // ----- Accessors -----
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public PhoneNumberType getType() { return type; }
    public void setType(PhoneNumberType type) { this.type = type; }

    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }
}

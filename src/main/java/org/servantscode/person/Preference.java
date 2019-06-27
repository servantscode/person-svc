package org.servantscode.person;

public class Preference {
    public enum PreferenceType { BOOLEAN };
    public enum ObjectType { PERSON, FAMILY };

    private int id;
    private String name;
    private PreferenceType type;
    private ObjectType objectType;
    private String defaultValue;

    public Preference() {}

    // ----- Accessors -----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public PreferenceType getType() { return type; }
    public void setType(PreferenceType type) { this.type = type; }

    public ObjectType getObjectType() { return objectType; }
    public void setObjectType(ObjectType objectType) { this.objectType = objectType; }

    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
}

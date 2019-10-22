package org.servantscode.person.db;

import org.servantscode.person.Family;
import org.servantscode.person.Person;

import java.util.List;
import java.util.stream.Collectors;

public class FamilyReconciler {
    private PersonDB personDb;
    private FamilyDB familyDb;
    private PreferenceDB prefDb;

    public FamilyReconciler(PersonDB personDb, FamilyDB familyDb, PreferenceDB preferencesDb) {
        this.personDb = personDb;
        this.familyDb = familyDb;
        this.prefDb = preferencesDb;
    }

    public Person createPerson(Person person) {
        resolveFamily(person.getFamily());
        personDb.create(person);
        if(person.getPreferences() != null && !person.getPreferences().isEmpty())
            prefDb.updatePersonalPreferences(person.getId(), person.getPreferences());

        return person;
    }

    public Person updatePerson(Person person) {
        Family family = person.getFamily();
        if(family != null) {
            resolveFamily(family);

            family.setMembers(personDb.getFamilyMembers(family.getId(), false));
            family.getMembers().removeIf((p) -> p.getId() == person.getId()); //Remove the returned person
        }

        personDb.update(person);
        if(person.getPreferences() != null && !person.getPreferences().isEmpty())
            prefDb.updatePersonalPreferences(person.getId(), person.getPreferences());

        return getPerson(person.getId(), false);
    }

    public Person getPerson(int id, boolean includeInactive) {
        Person person = personDb.getPerson(id);
        if(person == null)
            return null;
        Family family = retrieveFamilyWithMembers(person.getFamilyId(), includeInactive);
        if(family != null) {
            family.getMembers().removeIf((p) -> p.getId() == person.getId()); //Remove the returned person
        }

        person.setFamily(family);
        return person;
    }

    public boolean deletePerson(Person person) {
        List<Person> familyMembers = personDb.getFamilyMembers(person.getFamily().getId(), true);

        boolean success = personDb.delete(person);
        if(success && familyMembers.size() == 1 && familyMembers.get(0).getId() == person.getId())
            success = familyDb.delete(person.getFamily());

        return success;
    }

    public boolean deactivatePerson(Person person) {
        List<Person> activeFamilyMembers = personDb.getFamilyMembers(person.getFamily().getId(), false);

        boolean success = personDb.deactivate(person);
        if(success && activeFamilyMembers.size() == 1 && activeFamilyMembers.get(0).getId() == person.getId())
            success = familyDb.deactivate(person.getFamily());

        return success;
    }

    public Family createFamily(Family family) {
        familyDb.create(family);
        if(family.getPreferences() != null && !family.getPreferences().isEmpty())
            prefDb.updateFamilialPreferences(family.getId(), family.getPreferences());

        if(family.getMembers() != null) {
            for (Person person : family.getMembers()) {
                person.setFamilyId(family.getId());
                personDb.create(person);
                if(person.getPreferences() != null && !person.getPreferences().isEmpty())
                    prefDb.updatePersonalPreferences(person.getId(), person.getPreferences());
            }
        }
        return family;
    }

    //Does not presume that missing members should be removed.
    public Family updateFamily(Family family) {
        familyDb.update(family);
        if(family.getPreferences() != null && !family.getPreferences().isEmpty())
            prefDb.updateFamilialPreferences(family.getId(), family.getPreferences());
        if(family.getMembers() != null) {
            for (Person person : family.getMembers()) {
                person.setFamilyId(family.getId());
                if(person.getId() > 0)
                    personDb.update(person);
                else
                    personDb.create(person);

                if(person.getPreferences() != null && !person.getPreferences().isEmpty())
                    prefDb.updatePersonalPreferences(person.getId(), person.getPreferences());
            }
        }
        return getFamily(family.getId(), false);
    }

    public Family getFamily(int id, boolean includeInactive) {
        return retrieveFamilyWithMembers(id, includeInactive);
    }

    public void populateFamilyMembers(List<Family> families, boolean includeInactive) {
        for(Family fam: families)
            fam.setMembers(personDb.getFamilyMembers(fam.getId(), includeInactive | fam.isInactive()));
    }

    public boolean deleteFamily(Family family) {
        personDb.deleteByFamilyId(family.getId());
        return familyDb.delete(family);
    }

    public boolean deactivateFamily(Family family) {
        personDb.deactivateByFamilyId(family.getId());
        return familyDb.deactivate(family);
    }
    // ----- Private -----
    private void resolveFamily(Family family) {
        if(family.getId() > 0) {
            familyDb.update(family);
        } else {
            familyDb.create(family);
        }

        if(family.getPreferences() != null && !family.getPreferences().isEmpty())
            prefDb.updateFamilialPreferences(family.getId(), family.getPreferences());
    }

    private Family retrieveFamilyWithMembers(int familyId, boolean includeInactive) {
        Family family = familyDb.getFamily(familyId);
        if(family != null) {
            family.setMembers(personDb.getFamilyMembers(family.getId(), includeInactive | family.isInactive()));
        }
        return family;
    }
}

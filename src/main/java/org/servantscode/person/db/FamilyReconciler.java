package org.servantscode.person.db;

import org.servantscode.person.Family;
import org.servantscode.person.Person;

import java.util.List;

public class FamilyReconciler {
    private PersonDB personDb;
    private FamilyDB familyDb;

    public FamilyReconciler(PersonDB personDb, FamilyDB familyDb) {
        this.personDb = personDb;
        this.familyDb = familyDb;
    }

    public Person createPerson(Person person) {
        resolveFamily(person.getFamily());
        personDb.create(person);
        return person;
    }

    public Person updatePerson(Person person) {
        resolveFamily(person.getFamily());
        personDb.update(person);
        return person;
    }

    public Person getPerson(int id) {
        Person person = personDb.getPerson(id);
        Family family = retrieveFamilyWithMembers(person.getFamilyId());
        if(family != null) {
            family.getMembers().removeIf((p) -> p.getId() == person.getId()); //Remove the returned person
        }

        person.setFamily(family);
        return person;
    }

    public boolean deletePerson(Person person) {
        List<Person> familyMembers = personDb.getFamilyMembers(person.getFamily().getId());

        boolean success = personDb.delete(person);
        if(success && familyMembers.size() == 1 && familyMembers.get(0).getId() == person.getId())
            success = familyDb.delete(person.getFamily());

        return success;
    }

    public Family createFamily(Family family) {
        familyDb.create(family);
        for(Person person: family.getMembers()) {
            personDb.create(person);
        }
        return family;
    }

    public Family updateFamily(Family family) {
        familyDb.update(family);
        for(Person person: family.getMembers()) {
            personDb.update(person);
        }
        return family;
    }

    public Family getFamily(int id) {
        return retrieveFamilyWithMembers(id);
    }

    public boolean deleteFamily(Family family) {
        personDb.deleteByFamilyId(family.getId());
        return familyDb.delete(family);
    }

    // ----- Private -----
    private void resolveFamily(Family family) {
        if(family.getId() > 0) {
            familyDb.update(family);
        } else {
            familyDb.create(family);
        }
    }

    private Family retrieveFamilyWithMembers(int familyId) {
        Family family = familyDb.getFamily(familyId);
        if(family != null) {
            family.setMembers(personDb.getFamilyMembers(family.getId()));
        }
        return family;
    }
}

package org.servantscode.person.rest;

import org.servantscode.person.Person;
import org.servantscode.person.db.PersonDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("/person")
public class PersonSvc {

    @GET @Produces(MediaType.APPLICATION_JSON)
    public List<Person> getPeople() {
        return new PersonDB().getPeople();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Person createPerson(Person person) {
        new PersonDB().addPerson(person);
        return person;
    }
}

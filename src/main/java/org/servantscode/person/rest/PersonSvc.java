package org.servantscode.person.rest;

import org.servantscode.person.Person;
import org.servantscode.person.db.PersonDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Path("/person")
public class PersonSvc {

    @GET @Produces(MediaType.APPLICATION_JSON)
    public List<Person> getPeople() {
        try {
            System.out.println("Retrieving people");
            return new PersonDB().getPeople();
        } catch (Throwable t) {
            System.out.println("Retrieving people failed:");
            t.printStackTrace();
        }
        return Collections.emptyList();
    }

    @GET @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Person getPerson(@PathParam("id") int id) {
        try {
            System.out.println("Retrieving person: " + id);
            return new PersonDB().getPerson(id);
        } catch (Throwable t) {
            System.out.println("Retrieving person failed:");
            t.printStackTrace();
        }
        return null;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Person createPerson(Person person) {
        try {
            System.out.println("Creating person");
            new PersonDB().addPerson(person);
            System.out.println("Created person: " + person.getId());
            return person;
        } catch (Throwable t) {
            System.out.println("Creating person failed:");
            t.printStackTrace();
        }
        return null;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Person updatePerson(Person person) {
        try {
            System.out.println("Updating person: " + person.getId());
            new PersonDB().updatePerson(person);
            return person;
        } catch (Throwable t) {
            System.out.println("Updating person failed:");
            t.printStackTrace();
        }

        return null;
    }
}

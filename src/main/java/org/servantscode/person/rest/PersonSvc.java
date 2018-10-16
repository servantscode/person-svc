package org.servantscode.person.rest;

import org.servantscode.person.Person;
import org.servantscode.person.db.PersonDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/person")
public class PersonSvc {

    @GET @Produces(MediaType.APPLICATION_JSON)
    public PersonQueryResponse getPeople(@QueryParam("start") @DefaultValue("0") int start,
                                         @QueryParam("count") @DefaultValue("100") int count,
                                         @QueryParam("sort_field") @DefaultValue("id") String sortField) {

        try {
            System.out.println("Retrieving people (" + sortField + "page: " + start + "; " + count + ")");
            PersonDB db = new PersonDB();
            int totalPeople = db.getCount();
            List<Person> results = db.getPeople(sortField, start, count);
            return new PersonQueryResponse(start, results.size(), totalPeople, results);
        } catch (Throwable t) {
            System.out.println("Retrieving people failed:");
            t.printStackTrace();
        }
        return null;
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

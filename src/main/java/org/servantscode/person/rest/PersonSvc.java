package org.servantscode.person.rest;

import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.servantscode.person.Person;
import org.servantscode.person.db.FamilyDB;
import org.servantscode.person.db.FamilyReconciler;
import org.servantscode.person.db.PersonDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/person")
public class PersonSvc {

    @GET @Path("/autocomplete") @Produces(MediaType.APPLICATION_JSON)
    public List<String> getPeopleNames(@QueryParam("start") @DefaultValue("0") int start,
                                       @QueryParam("count") @DefaultValue("100") int count,
                                       @QueryParam("sort_field") @DefaultValue("id") String sortField,
                                       @QueryParam("partial_name") @DefaultValue("") String nameSearch) {

        try {
            System.out.println(String.format("Retrieving people names (%s, %s, page: %d; %d)", nameSearch, sortField, start, count));
            PersonDB db = new PersonDB();
            return db.getPeopleNames(nameSearch, count);
        } catch (Throwable t) {
            System.out.println("Retrieving people failed:");
            t.printStackTrace();
        }
        return null;
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    public PersonQueryResponse getPeople(@QueryParam("start") @DefaultValue("0") int start,
                                         @QueryParam("count") @DefaultValue("100") int count,
                                         @QueryParam("sort_field") @DefaultValue("id") String sortField,
                                         @QueryParam("partial_name") @DefaultValue("") String nameSearch) {

        try {
            System.out.println(String.format("Retrieving people (%s, %s, page: %d; %d)", nameSearch, sortField, start, count));
            PersonDB db = new PersonDB();
            int totalPeople = db.getCount(nameSearch);
            List<Person> results = db.getPeople(nameSearch, sortField, start, count);
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
            return getReconciler().getPerson(id);
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
            getReconciler().createPerson(person);
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
            getReconciler().updatePerson(person);
            return person;
        } catch (Throwable t) {
            System.out.println("Updating person failed:");
            t.printStackTrace();
        }
        return null;
    }

    @DELETE @Path("/{id}")
    public void deletePerson(@PathParam("id") int id) {
        if(id <= 0)
            throw new NotFoundException();
        try {
            Person person = getReconciler().getPerson(id);
            if(person == null || getReconciler().deletePerson(person))
                throw new NotFoundException();
        } catch (Throwable t) {
            System.out.println("Deleting person failed:");
            t.printStackTrace();
        }
    }

    // ----- Private -----
    private FamilyReconciler getReconciler() {
        return new FamilyReconciler(new PersonDB(), new FamilyDB());
    }
}

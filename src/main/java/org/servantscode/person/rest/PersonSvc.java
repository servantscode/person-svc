package org.servantscode.person.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.rest.PaginatedResponse;
import org.servantscode.person.Person;
import org.servantscode.person.db.FamilyDB;
import org.servantscode.person.db.FamilyReconciler;
import org.servantscode.person.db.PersonDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.ZonedDateTime;
import java.util.List;

@Path("/person")
public class PersonSvc {
    private static final Logger LOG = LogManager.getLogger(PersonSvc.class);

    @GET @Path("/autocomplete") @Produces(MediaType.APPLICATION_JSON)
    public List<String> getPeopleNames(@QueryParam("start") @DefaultValue("0") int start,
                                       @QueryParam("count") @DefaultValue("100") int count,
                                       @QueryParam("sort_field") @DefaultValue("id") String sortField,
                                       @QueryParam("partial_name") @DefaultValue("") String nameSearch) {

        try {
            LOG.trace(String.format("Retrieving people names (%s, %s, page: %d; %d)", nameSearch, sortField, start, count));
            PersonDB db = new PersonDB();
            return db.getPeopleNames(nameSearch, count);
        } catch (Throwable t) {
            LOG.error("Retrieving people failed:", t);
            throw t;
        }
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<Person> getPeople(@QueryParam("start") @DefaultValue("0") int start,
                                       @QueryParam("count") @DefaultValue("10") int count,
                                       @QueryParam("sort_field") @DefaultValue("id") String sortField,
                                       @QueryParam("partial_name") @DefaultValue("") String nameSearch,
                                       @QueryParam("families") @DefaultValue("false") boolean includeFamilies) {

        try {
            LOG.trace(String.format("Retrieving people (%s, %s, page: %d; %d, families: %b)", nameSearch, sortField, start, count, includeFamilies));
            PersonDB db = new PersonDB();
            int totalPeople = db.getCount(nameSearch);

            List<Person> results ;
            if(!includeFamilies) {
                results = db.getPeople(nameSearch, sortField, start, count);
            } else {
                results = db.getPeopleWithFamilies(nameSearch, sortField, start, count);
            }

            return new PaginatedResponse<>(start, results.size(), totalPeople, results);
        } catch (Throwable t) {
            LOG.error("Retrieving people failed:", t);
            throw t;
        }
    }

    @GET @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Person getPerson(@PathParam("id") int id) {
        try {
            return getReconciler().getPerson(id);
        } catch (Throwable t) {
            LOG.error("Retrieving person failed:", t);
            throw t;
        }
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Person createPerson(Person person) {
        try {
            if(person.getMemberSince() == null)
                person.setMemberSince(ZonedDateTime.now());

            getReconciler().createPerson(person);
            LOG.info("Created parishoner: " + person.getName());
            return person;
        } catch (Throwable t) {
            LOG.error("Creating person failed:", t);
            throw t;
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Person updatePerson(Person person) {
        try {
            getReconciler().updatePerson(person);
            LOG.info("Edited parishoner: " + person.getName());
            return person;
        } catch (Throwable t) {
            LOG.error("Updating person failed:", t);
            throw t;
        }
    }

    @DELETE @Path("/{id}")
    public void deletePerson(@PathParam("id") int id) {
        if(id <= 0)
            throw new NotFoundException();
        try {
            Person person = getReconciler().getPerson(id);
            if(person == null || getReconciler().deletePerson(person))
                throw new NotFoundException();
            LOG.info("Deleted parishoner: " + person.getName());
        } catch (Throwable t) {
            LOG.error("Deleting person failed:", t);
            throw t;
        }
    }

    // ----- Private -----
    private FamilyReconciler getReconciler() {
        return new FamilyReconciler(new PersonDB(), new FamilyDB());
    }
}

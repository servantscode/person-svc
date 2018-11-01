package org.servantscode.person.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.person.Family;
import org.servantscode.person.db.FamilyDB;
import org.servantscode.person.db.FamilyReconciler;
import org.servantscode.person.db.PersonDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/family")
public class FamilySvc {
    private static final Logger logger = LogManager.getLogger(FamilySvc.class);

    @GET @Path("/autocomplete") @Produces(MediaType.APPLICATION_JSON)
    public List<String> getFamilyNames(@QueryParam("start") @DefaultValue("0") int start,
                                       @QueryParam("count") @DefaultValue("100") int count,
                                       @QueryParam("sort_field") @DefaultValue("id") String sortField,
                                       @QueryParam("partial_name") @DefaultValue("") String nameSearch) {

        try {
            logger.trace(String.format("Retrieving family names (%s, %s, page: %d; %d)", nameSearch, sortField, start, count));
            FamilyDB db = new FamilyDB();
            return db.getFamilySurnames(nameSearch, count);
        } catch (Throwable t) {
            logger.error("Retrieving families failed:");
            t.printStackTrace();
        }
        return null;
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    public FamilyQueryResponse getFamilies(@QueryParam("start") @DefaultValue("0") int start,
                                         @QueryParam("count") @DefaultValue("100") int count,
                                         @QueryParam("sort_field") @DefaultValue("id") String sortField,
                                         @QueryParam("partial_name") @DefaultValue("") String nameSearch) {

        try {
            logger.trace(String.format("Retrieving families (%s, %s, page: %d; %d)", nameSearch, sortField, start, count));
            FamilyDB db = new FamilyDB();
            int totalFamilies = db.getCount(nameSearch);
            List<Family> results = db.getFamilies(nameSearch, sortField, start, count);
            return new FamilyQueryResponse(start, results.size(), totalFamilies, results);
        } catch (Throwable t) {
            logger.error("Retrieving families failed:");
            t.printStackTrace();
        }
        return null;
    }

    @GET @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Family getFamily(@PathParam("id") int id) {
        try {
            return getReconciler().getFamily(id);
        } catch (Throwable t) {
            logger.error("Retrieving family failed:");
            t.printStackTrace();
        }
        return null;
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Family createFamily(Family family) {
        try {
            getReconciler().createFamily(family);
            logger.info("Created family: " + family.getSurname());
            return family;
        } catch (Throwable t) {
            logger.error("Creating family failed:");
            t.printStackTrace();
        }
        return null;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Family updateFamily(Family family) {
        try {
            getReconciler().updateFamily(family);
            logger.info("Edited family: " + family.getSurname());
            return family;
        } catch (Throwable t) {
            logger.error("Updating family failed:");
            t.printStackTrace();
        }
        return null;
    }

    @DELETE @Path("/{id}")
    public void deleteFamily(@PathParam("id") int id) {
        if(id <= 0)
            throw new NotFoundException();
        try {
            Family family = getReconciler().getFamily(id);
            if(family == null || getReconciler().deleteFamily(family))
                throw new NotFoundException();
            logger.info("Deleted family: " + family.getSurname());
        } catch (Throwable t) {
            logger.error("Deleting family failed:");
            t.printStackTrace();
        }
    }

    // ----- Private -----
    private FamilyReconciler getReconciler() {
        return new FamilyReconciler(new PersonDB(), new FamilyDB());
    }
}

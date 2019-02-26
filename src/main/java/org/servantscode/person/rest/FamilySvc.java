package org.servantscode.person.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.rest.PaginatedResponse;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.person.Family;
import org.servantscode.person.db.FamilyDB;
import org.servantscode.person.db.FamilyReconciler;
import org.servantscode.person.db.PersonDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/family")
public class FamilySvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(FamilySvc.class);

    private FamilyDB db;

    public FamilySvc() {
        db = new FamilyDB();
    }

    @GET @Path("/autocomplete") @Produces(MediaType.APPLICATION_JSON)
    public List<String> getFamilyNames(@QueryParam("start") @DefaultValue("0") int start,
                                       @QueryParam("count") @DefaultValue("100") int count,
                                       @QueryParam("sort_field") @DefaultValue("id") String sortField,
                                       @QueryParam("partial_name") @DefaultValue("") String nameSearch) {

        verifyUserAccess("family.list");
        try {
            LOG.trace(String.format("Retrieving family names (%s, %s, page: %d; %d)", nameSearch, sortField, start, count));
            return db.getFamilySurnames(nameSearch, count);
        } catch (Throwable t) {
            LOG.error("Retrieving families failed:");
            t.printStackTrace();
        }
        return null;
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<Family> getFamilies(@QueryParam("start") @DefaultValue("0") int start,
                                         @QueryParam("count") @DefaultValue("100") int count,
                                         @QueryParam("sort_field") @DefaultValue("id") String sortField,
                                         @QueryParam("partial_name") @DefaultValue("") String nameSearch) {

        verifyUserAccess("family.list");
        try {
            LOG.trace(String.format("Retrieving families (%s, %s, page: %d; %d)", nameSearch, sortField, start, count));
            int totalFamilies = db.getCount(nameSearch);
            List<Family> results = db.getFamilies(nameSearch, sortField, start, count);
            return new PaginatedResponse<>(start, results.size(), totalFamilies, results);
        } catch (Throwable t) {
            LOG.error("Retrieving families failed:");
            t.printStackTrace();
        }
        return null;
    }

    @GET @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Family getFamily(@PathParam("id") int id) {
        verifyUserAccess("family.read");
        try {
            return getReconciler().getFamily(id);
        } catch (Throwable t) {
            LOG.error("Retrieving family failed:");
            t.printStackTrace();
        }
        return null;
    }

    @PUT @Path("/{id}/photo") @Consumes(MediaType.TEXT_PLAIN)
    public void attachPhoto(@PathParam("id") int id,
                            String guid) {
        verifyUserAccess("family.update");

        LOG.debug("Attaching photo: " + guid);
        try {
            db.attchPhoto(id, guid);
        } catch (Throwable t) {
            LOG.error("Attaching photo to person failed.", t);
            throw t;
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Family createFamily(Family family) {
        verifyUserAccess("family.create");
        try {
            getReconciler().createFamily(family);
            LOG.info("Created family: " + family.getSurname());
            return family;
        } catch (Throwable t) {
            LOG.error("Creating family failed:");
            t.printStackTrace();
        }
        return null;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Family updateFamily(Family family) {
        verifyUserAccess("family.update");
        try {
            Family updatedFamily = getReconciler().updateFamily(family);
            LOG.info("Edited family: " + family.getSurname());
            return updatedFamily;
        } catch (Throwable t) {
            LOG.error("Updating family failed:");
            t.printStackTrace();
        }
        return null;
    }

    @DELETE @Path("/{id}")
    public void deleteFamily(@PathParam("id") int id) {
        verifyUserAccess("family.delete");
        if(id <= 0)
            throw new NotFoundException();
        try {
            Family family = getReconciler().getFamily(id);
            if(family == null || getReconciler().deleteFamily(family))
                throw new NotFoundException();
            LOG.info("Deleted family: " + family.getSurname());
        } catch (Throwable t) {
            LOG.error("Deleting family failed:");
            t.printStackTrace();
        }
    }

    // ----- Private -----
    private FamilyReconciler getReconciler() {
        return new FamilyReconciler(new PersonDB(), db);
    }
}

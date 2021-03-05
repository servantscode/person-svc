package org.servantscode.person.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.rest.PaginatedResponse;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.person.Family;
import org.servantscode.person.Person;
import org.servantscode.person.db.FamilyDB;
import org.servantscode.person.db.FamilyReconciler;
import org.servantscode.person.db.PersonDB;
import org.servantscode.person.db.PreferenceDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/family")
public class FamilySvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(FamilySvc.class);

//    private static List<String> EXPORTABLE_FIELDS = Arrays.asList("id", "surname", "formal_greeting", "head_name", "spouse_name", "home_phone", "envelope_number", "addr_street1", "addr_street2", "addr_city", "addr_state", "addr_zip", "inactive");

    private static List<String> EXPORTABLE_FIELDS = Arrays.asList("id","surname","home_phone", "envelope_number", "addr_street1", "addr_street2", "addr_city", "addr_state","addr_zip",
        "inactive", "inactive_since", "head_name", "spouse_name", "formal_greeting");

    private FamilyDB db;
    private PreferenceDB prefDb;

    public FamilySvc() {
        db = new FamilyDB();
        prefDb = new PreferenceDB();
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<Family> getFamilies(@QueryParam("start") @DefaultValue("0") int start,
                                         @QueryParam("count") @DefaultValue("10") int count,
                                         @QueryParam("sort_field") @DefaultValue("surname") String sortField,
                                         @QueryParam("search") @DefaultValue("") String search,
                                         @QueryParam("members") @DefaultValue("false") boolean includeFamilyMembers,
                                         @QueryParam("include_inactive") @DefaultValue("false") boolean includeInactive) {

        verifyUserAccess("family.list");
        try {
            LOG.trace(String.format("Retrieving families (%s, %s, page: %d; %d)", search, sortField, start, count));
            int totalFamilies = db.getCount(search, includeInactive);
            List<Family> results = db.getFamilies(search, sortField, start, count, includeInactive);
            if(includeFamilyMembers)
                getReconciler().populateFamilyMembers(results, includeInactive);

            return new PaginatedResponse<>(start, results.size(), totalFamilies, results);
        } catch (Throwable t) {
            LOG.error("Retrieving families failed:", t);
            throw t;
        }
    }

    @GET @Path("/report") @Produces(MediaType.TEXT_PLAIN)
    public Response getFamilyReport(@QueryParam("search") @DefaultValue("") String search,
                                    @QueryParam("include_inactive") @DefaultValue("false") boolean includeInactive) {
        verifyUserAccess("family.export");

        try {
            LOG.trace(String.format("Retrieving family report(%s)", search));
            return Response.ok(db.getReportReader(search, includeInactive, EXPORTABLE_FIELDS)).build();
        } catch (Throwable t) {
            LOG.error("Retrieving family report failed:", t);
            throw t;
        }
    }


    @GET @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Family getFamily(@PathParam("id") int id,
                            @QueryParam("include_inactive") @DefaultValue("false") boolean includeInactive) {
        verifyUserAccess("family.read");
        try {
            return getReconciler().getFamily(id, includeInactive);
        } catch (Throwable t) {
            LOG.error("Retrieving family failed:", t);
            throw t;
        }
    }

    @GET @Path("/nextEnvelope") @Produces(MediaType.APPLICATION_JSON)
    public int getNextEnvelopeNumber() {
        verifyUserAccess("family.list");
        try {
            return db.getNextUnusedEnvelopeNumber();
        } catch (Throwable t) {
            LOG.error("Determining next envelope number failed.", t);
            throw t;
        }
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

    @GET @Path("/{id}/preferences") @Produces(APPLICATION_JSON)
    public Map<String, String> getPreferences(@PathParam("id") int id) {
        verifyUserAccess("preference.read");

        if(id <= 0)
            throw new BadRequestException();

        Family family = db.getFamily(id);
        if(family == null)
            throw new NotFoundException();

        try {
            return prefDb.getFamilialPreferences(id);
        } catch (Throwable t) {
            LOG.error("Retrieving familial preferences failed for: " + family.getSurname(), t);
            throw t;
        }
    }

    @PUT @Path("/{id}/preferences") @Consumes(APPLICATION_JSON)
    public void updatePreferences(@PathParam("id") int id,
                                  Map<String, String> prefs) {
        verifyUserAccess("preference.update");

        if(id <= 0 || prefs == null)
            throw new BadRequestException();

        Family family = db.getFamily(id);
        if(family == null)
            throw new NotFoundException();

        try {
            prefDb.updateFamilialPreferences(id, prefs);
            LOG.info("Updated familial preferences for: "  + family.getSurname());
        } catch (Throwable t) {
            LOG.error("Updating familial preferences failed for: " + family.getSurname(), t);
            throw t;
        }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Family createFamily(Family family) {
        verifyUserAccess("family.create");
        try {
            getReconciler().createFamily(family);
            LOG.info("Created family: " + family.getSurname());
            return family;
        } catch (Throwable t) {
            LOG.error("Creating family failed:", t);
            throw t;
        }
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
            LOG.error("Updating family failed:", t);
            throw t;
        }
    }

    @DELETE @Path("/{id}")
    public void deleteFamily(@PathParam("id") int id,
                             @QueryParam("delete_permenantly") @DefaultValue("false") boolean permenantDelete) {
        if(permenantDelete)
            verifyUserAccess("admin.family.delete");
        else
            verifyUserAccess("family.delete");

        if(id <= 0)
            throw new NotFoundException();
        try {
            Family family = getReconciler().getFamily(id, false);
            if(family == null)
                throw new NotFoundException();
            if(permenantDelete) {
                if (!getReconciler().deleteFamily(family))
                    throw new NotFoundException();
                LOG.info("Deleted family: " + family.getSurname());
            } else {
                if (!getReconciler().deactivateFamily(family))
                    throw new NotFoundException();
                LOG.info("Deactivated family: " + family.getSurname());
            }
        } catch (Throwable t) {
            LOG.error("Deleting family failed:", t);
            throw t;
        }
    }

    // ----- Private -----
    private FamilyReconciler getReconciler() {
        return new FamilyReconciler(new PersonDB(), db, prefDb);
    }
}

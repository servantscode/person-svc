package org.servantscode.person.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.EnumUtils;
import org.servantscode.commons.rest.PaginatedResponse;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.person.Preference;
import org.servantscode.person.db.PreferenceDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/preference")
public class PreferenceSvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(PreferenceSvc.class);

    private PreferenceDB db;

    public PreferenceSvc() {
        this.db = new PreferenceDB();
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<Preference> getPreferences(@QueryParam("start") @DefaultValue("0") int start,
                                                        @QueryParam("count") @DefaultValue("10") int count,
                                                        @QueryParam("sort_field") @DefaultValue("name") String sortField,
                                                        @QueryParam("partial_name") @DefaultValue("") String search) {

        verifyUserAccess("preference.list");

        try {
            int totalPreferences = db.getCount(search);
            List<Preference> results = db.getPreferences(search, sortField, start, count);

            return new PaginatedResponse<>(start, results.size(), totalPreferences, results);
        } catch (Throwable t) {
            LOG.error("Retrieving preferences failed:", t);
            throw t;
        }
    }


    @GET @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Preference getPreference(@PathParam("id") int id) {
        verifyUserAccess("preference.read");

        try {
            return db.getPreference(id);
        } catch (Throwable t) {
            LOG.error("Retrieving preference failed:", t);
            throw t;
        }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Preference createPreference(Preference preference) {
        verifyUserAccess("admin.preference.create");
        try {
            db.create(preference);
            LOG.info("Created preference: " + preference.getName());
            return preference;
        } catch (Throwable t) {
            LOG.error("Creating preference failed:", t);
            throw t;
        }
    }

    @PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Preference updatePreference(Preference preference) {
        verifyUserAccess("admin.preference.update");
        try {
            db.update(preference);
            LOG.info("Edited preference: " + preference.getName());
            return preference;
        } catch (Throwable t) {
            LOG.error("Updating preference failed:", t);
            throw t;
        }
    }

    @DELETE @Path("/{id}")
    public void deletePreference(@PathParam("id") int id) {
        verifyUserAccess("admin.preference.delete");

        if (id <= 0)
            throw new NotFoundException();
        try {
            Preference preference = db.getPreference(id);
            if (preference == null)
                throw new NotFoundException();

            if (!db.delete(preference))
                throw new NotFoundException();
            LOG.info("Deleted preference: " + preference.getName());
        } catch (Throwable t) {
            LOG.error("Deleting preference failed:", t);
            throw t;
        }
    }

    @GET @Path("/type") @Produces(APPLICATION_JSON)
    public List<String> getPreferenceTypes() { return EnumUtils.listValues(Preference.PreferenceType.class); }
}

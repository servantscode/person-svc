package org.servantscode.person.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.EnumUtils;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.person.Relationship;
import org.servantscode.person.Relationship.RelationshipType;
import org.servantscode.person.db.PersonDB;
import org.servantscode.person.db.RelationshipDB;

import javax.ws.rs.*;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/relationship")
public class RelationshipSvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(RelationshipSvc.class);

    private RelationshipDB db;
    private PersonDB personDb;

    public RelationshipSvc() {
        this.db = new RelationshipDB();
        this.personDb = new PersonDB();
    }

    @GET @Path("/{id}") @Produces(APPLICATION_JSON)
    public List<Relationship> getRelationships(@PathParam("id") int personId) {
        userHasAccess("relationship.list");

        if (personId <= 0)
            throw new BadRequestException();

        try {
            return db.getRelationships(personId);
        } catch (Throwable t) {
            LOG.error("Failed while retrieving relationships for person: " + personId, t);
            throw t;
        }
    }


    @PUT @Produces(APPLICATION_JSON) @Consumes(APPLICATION_JSON)
    public void storeRelationships(@QueryParam("addReciprocals") @DefaultValue("false") boolean addReciprocals,
                                   List<Relationship> relationships) {
        userHasAccess("relationship.update");

        if (relationships == null || relationships.isEmpty())
            throw new BadRequestException();

        int failures = 0;
        for (Relationship r : relationships) {
            try {
                db.upsertRelationship(r);
                LOG.info("Updated relationship between %d and %d", r.getPersonId(), r.getOtherId());
                if(addReciprocals) {
                    db.upsertRelationship(calculateReciprocal(r));
                    LOG.info("Updated relationship between %d and %d", r.getOtherId(), r.getPersonId());
                }
            } catch (Throwable t) {
                LOG.error("Failed while storing relationships: ", t);
                failures++;
            }
        }

        if (failures > 0)
            throw new ServerErrorException("Failed to store some relationships.", 500);
    }

    @GET @Path("/types") @Produces(APPLICATION_JSON)
    public List<String> getRelationshipTypes() {
        return EnumUtils.listValues(RelationshipType.class);
    }

    // ----- Private -----
    private Relationship calculateReciprocal(Relationship r) {
        Relationship result = new Relationship();
        result.setPersonId(r.getOtherId());
        result.setOtherId(r.getPersonId());
        result.setContactPreference(r.getContactPreference());
        RelationshipType relationship = Relationship.invert(r.getRelationship(), personDb.isMale(result.getOtherId()));
        result.setRelationship(relationship);
        switch (relationship) {
            case MOTHER:
            case FATHER:
                result.setGuardian(true);
            case SPOUSE:
                result.setContactPreference(1);
                break;
            default:
                result.setContactPreference(0);
        }
        result.setDoNotContact(r.isDoNotContact());
        return result;
    }


}

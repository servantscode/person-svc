package org.servantscode.person.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.person.Relationship;
import org.servantscode.person.db.RelationshipDB;

import javax.ws.rs.*;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/relationship")
public class RelationshipSvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(RelationshipSvc.class);

    private RelationshipDB db;

    public RelationshipSvc() {
        this.db = new RelationshipDB();
    }

    @GET @Path("/{id}") @Produces(APPLICATION_JSON)
    public List<Relationship> getRelationships(@PathParam("id") int personId) {
        userHasAccess("relationship.list");

        if(personId <= 0)
            throw new BadRequestException();

        try {
            return db.getRelationships(personId);
        } catch (Throwable t) {
            LOG.error("Failed while retrieving relationships for person: " + personId, t);
            throw t;
        }
    }


    @PUT @Produces(APPLICATION_JSON) @Consumes(APPLICATION_JSON)
    public void storeRelationships(List<Relationship> relationships) {
        if(relationships == null || relationships.isEmpty())
            throw new BadRequestException();

        int failures = 0;
        for(Relationship r: relationships) {
            try {
                db.upsertRelationship(r);
            } catch (Throwable t) {
                LOG.error("Failed while storing relationships: ", t);
                failures++;
            }
        }

        if(failures > 0)
            throw new ServerErrorException("Failed to store some relationships.", 500);
    }
}

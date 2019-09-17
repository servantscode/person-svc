package org.servantscode.person.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.rest.PaginatedResponse;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.person.Family;
import org.servantscode.person.Person;
import org.servantscode.person.RegistrationRequest;
import org.servantscode.person.RegistrationRequest.ApprovalStatus;
import org.servantscode.person.db.FamilyDB;
import org.servantscode.person.db.FamilyReconciler;
import org.servantscode.person.db.PersonDB;
import org.servantscode.person.db.RegistrationRequestDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.time.ZonedDateTime;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.servantscode.commons.StringUtils.isEmpty;
import static org.servantscode.person.RegistrationRequest.ApprovalStatus.APPLIED;
import static org.servantscode.person.RegistrationRequest.ApprovalStatus.APPROVED;
import static org.servantscode.person.RegistrationRequest.ApprovalStatus.REQUESTED;

@Path("/registration")
public class RegistrationSvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(RegistrationSvc.class);

    private RegistrationRequestDB db;

    public RegistrationSvc() {
        db = new RegistrationRequestDB();
    }

    @GET @Produces(APPLICATION_JSON)
    public PaginatedResponse<RegistrationRequest> getRequests(@QueryParam("start") @DefaultValue("0") int start,
                                                              @QueryParam("count") @DefaultValue("10") int count,
                                                              @QueryParam("sort_field") @DefaultValue("request_time") String sortField,
                                                              @QueryParam("search") @DefaultValue("") String search,
                                                              @QueryParam("include_completed") @DefaultValue("false") boolean includeCompleted) {
        verifyUserAccess("registration.request.list");

        try {
            int totalRequests = db.getCount(search, includeCompleted);

            List<RegistrationRequest> results = db.getRegistrationRequests(search, sortField, start, count, includeCompleted);

            return new PaginatedResponse<>(start, results.size(), totalRequests, results);
        } catch (Throwable t) {
            LOG.error("Retrieving registration requests failed:", t);
            throw t;
        }
    }

    @GET @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public RegistrationRequest getRequest(@PathParam("id") int id) {
        verifyUserAccess("registration.request.read");

        try {
            return db.getRegistrationRequest(id);
        } catch (Throwable t) {
            LOG.error("Retrieving registration request failed:", t);
            throw t;
        }
    }


    @PUT @Path("/{id}/status/{status}") @Produces(MediaType.APPLICATION_JSON)
    public RegistrationRequest updateApproval(@PathParam("id") int id,
                                              @PathParam("status") ApprovalStatus status) {
        verifyUserAccess("registration.request.approve");

        if(id <= 0)
            throw new NotFoundException();

        try {
            RegistrationRequest request = getRequest(id);
            if(request == null)
                throw new NotFoundException();

            if(status == APPLIED)
                throw new BadRequestException("Cannot change approval status after data has been accepted.");

            request.setApprovalStatus(status);
            request.setApprovalTime(ZonedDateTime.now());
            db.update(request);
            LOG.info("Updated reservation request status: " + request.getFamilyName() + " Status now: " + status);

            if(status == APPROVED) {
                FamilyReconciler familyRec = new FamilyReconciler(new PersonDB(), new FamilyDB());
                familyRec.createFamily(request.getFamilyData());
                request.setApprovalStatus(APPLIED);
                db.update(request);

                LOG.info("Family registration stored: " + request.getFamilyName());
            }
            return request;
        } catch (Throwable t) {
            LOG.error("Updating reservation request status failed:", t);
            throw t;
        }
    }

    @POST @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public RegistrationRequest submitRegistration(Family family) {
        if(family == null ||
                isEmpty(family.getSurname()) ||
                family.getMembers().isEmpty())
            throw new BadRequestException();

        RegistrationRequest request = new RegistrationRequest();
        request.setFamilyData(family);
        request.setFamilyName(family.getSurname());
        request.setRequestTime(ZonedDateTime.now());
        request.setApprovalStatus(REQUESTED);

        try {
            return db.create(request);
        } catch (Throwable t) {
            LOG.error("Failed to create registration request", t);
            throw t;
        }
    }

    @DELETE @Path("/{id}")
    public void deleteRequest(@PathParam("id") int id) {

        verifyUserAccess("registration.request.delete");

        if(id <= 0)
            throw new NotFoundException();
        try {
            RegistrationRequest request = getRequest(id);
            if(request == null)
                throw new NotFoundException();

            if(!db.delete(request))
                throw new NotFoundException();
            LOG.info("Deleted reservation request: " + request.getFamilyName());
        } catch (Throwable t) {
            LOG.error("Deleting reservation request failed:", t);
            throw t;
        }
    }
}

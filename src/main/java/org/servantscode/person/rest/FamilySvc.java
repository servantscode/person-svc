package org.servantscode.person.rest;

import org.servantscode.person.Family;
import org.servantscode.person.db.FamilyDB;
import org.servantscode.person.db.FamilyReconciler;
import org.servantscode.person.db.PersonDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/family")
public class FamilySvc {

    @GET @Path("/autocomplete") @Produces(MediaType.APPLICATION_JSON)
    public List<String> getFamilyNames(@QueryParam("start") @DefaultValue("0") int start,
                                       @QueryParam("count") @DefaultValue("100") int count,
                                       @QueryParam("sort_field") @DefaultValue("id") String sortField,
                                       @QueryParam("partial_name") @DefaultValue("") String nameSearch) {

        try {
            System.out.println(String.format("Retrieving family names (%s, %s, page: %d; %d)", nameSearch, sortField, start, count));
            FamilyDB db = new FamilyDB();
            return db.getFamilySurnames(nameSearch, count);
        } catch (Throwable t) {
            System.out.println("Retrieving families failed:");
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
            System.out.println(String.format("Retrieving families (%s, %s, page: %d; %d)", nameSearch, sortField, start, count));
            FamilyDB db = new FamilyDB();
            int totalFamilies = db.getCount(nameSearch);
            List<Family> results = db.getFamilies(nameSearch, sortField, start, count);
            return new FamilyQueryResponse(start, results.size(), totalFamilies, results);
        } catch (Throwable t) {
            System.out.println("Retrieving families failed:");
            t.printStackTrace();
        }
        return null;
    }

    @GET @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Family getFamily(@PathParam("id") int id) {
        try {
            return getReconciler().getFamily(id);
        } catch (Throwable t) {
            System.out.println("Retrieving family failed:");
            t.printStackTrace();
        }
        return null;
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Family createFamily(Family family) {
        try {
            getReconciler().createFamily(family);
            return family;
        } catch (Throwable t) {
            System.out.println("Creating family failed:");
            t.printStackTrace();
        }
        return null;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Family updateFamily(Family family) {
        try {
            getReconciler().updateFamily(family);
            return family;
        } catch (Throwable t) {
            System.out.println("Updating family failed:");
            t.printStackTrace();
        }
        return null;
    }

    // ----- Private -----
    private FamilyReconciler getReconciler() {
        return new FamilyReconciler(new PersonDB(), new FamilyDB());
    }
}

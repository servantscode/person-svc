package org.servantscode.person.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.EnumUtils;
import org.servantscode.commons.rest.AuthFilter;
import org.servantscode.commons.rest.PaginatedResponse;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.person.Person;
import org.servantscode.person.PhoneNumber;
import org.servantscode.person.db.FamilyDB;
import org.servantscode.person.db.FamilyReconciler;
import org.servantscode.person.db.PersonDB;
import org.servantscode.person.db.PreferenceDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/person")
public class PersonSvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(PersonSvc.class);

//    private static final List<String> EXPORTABLE_FIELDS = Arrays.asList("id", "name", "birthdate", "male", "phone_number", "email", "family_id", "head_of_house", "member_since", "inactive", "addr_street1", "addr_street2", "addr_city", "addr_state", "addr_zip");

    private static final List<String> EXPORTABLE_FIELDS = Arrays.asList("id", "name", "birthdate", "male", "salutation","suffix","maiden_name","nickname","email","family_id","head_of_house","member_since","parishioner",
            "baptized", "confession", "communion", "confirmed", "holy_orders", "marital_status", "ethnicity", "primary_language",
            "religion", "special_needs", "occupation", "inactive", "inactive_since", "deceased", "death_date", "allergies");

    private static final List<String> PHONE_FIELDS = Arrays.asList("person_id", "number", "type", "is_primary");


    private PersonDB db;
    private PreferenceDB prefDb;

    public PersonSvc() {
        this.db = new PersonDB();
        this.prefDb = new PreferenceDB();
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<Person> getPeople(@QueryParam("start") @DefaultValue("0") int start,
                                       @QueryParam("count") @DefaultValue("10") int count,
                                       @QueryParam("sort_field") @DefaultValue("f.surname, family_id, head_of_house DESC, birthdate") String sortField,
                                       @QueryParam("search") @DefaultValue("") String nameSearch,
                                       @QueryParam("families") @DefaultValue("false") boolean includeFamilies,
                                       @QueryParam("include_inactive") @DefaultValue("false") boolean includeInactive) {

        verifyUserAccess("person.list");

        try {
            LOG.trace(String.format("Retrieving people (%s, %s, page: %d; %d, families: %b)", nameSearch, sortField, start, count, includeFamilies));
            int totalPeople = db.getCount(nameSearch, includeInactive);

            List<Person> results ;
            if(!includeFamilies) {
                results = db.getPeople(nameSearch, sortField, start, count, includeInactive);
            } else {
                results = db.getPeopleWithFamilies(nameSearch, sortField.equals("name")? "lastName": sortField, start, count, includeInactive);
            }

            return new PaginatedResponse<>(start, results.size(), totalPeople, results);
        } catch (Throwable t) {
            LOG.error("Retrieving people failed:", t);
            throw t;
        }
    }

    @GET @Path("/report") @Produces(MediaType.TEXT_PLAIN)
    public Response getPeopleReport(@QueryParam("search") @DefaultValue("") String nameSearch,
                                    @QueryParam("include_inactive") @DefaultValue("false") boolean includeInactive) {

        verifyUserAccess("person.export");

        try {
            LOG.trace(String.format("Retrieving people report(%s)", nameSearch));

            return Response.ok(db.getReportReader(nameSearch, includeInactive, EXPORTABLE_FIELDS)).build();
        } catch (Throwable t) {
            LOG.error("Retrieving people report failed:", t);
            throw t;
        }
    }

    @GET @Path("/phone/report") @Produces(MediaType.TEXT_PLAIN)
    public Response getPeopleReport() {

        verifyUserAccess("person.export");

        try {
            LOG.trace(String.format("Retrieving people phone number report"));

            return Response.ok(db.getPhoneReportReader(PHONE_FIELDS)).build();
        } catch (Throwable t) {
            LOG.error("Retrieving people report failed:", t);
            throw t;
        }
    }

    @GET @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Person getPerson(@PathParam("id") int id,
                            @QueryParam("include_inactive") @DefaultValue("false") boolean includeInactive) {
        verifyUserAccess("person.read");

        try {
            return getReconciler().getPerson(id, includeInactive);
        } catch (Throwable t) {
            LOG.error("Retrieving person failed:", t);
            throw t;
        }
    }

    @PUT @Path("/{id}/photo") @Consumes(MediaType.TEXT_PLAIN)
    public void attachPhoto(@PathParam("id") int id,
                            String guid) {
        verifyUserAccess("person.update");

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

        Person person = db.getPerson(id);
        if(person == null)
            throw new NotFoundException();

        try {
            return prefDb.getPersonalPreferences(id);
        } catch (Throwable t) {
            LOG.error("Retrieving personal preferences failed for: " + person.getName(), t);
            throw t;
        }
    }

    @PUT @Path("/{id}/preferences") @Consumes(APPLICATION_JSON)
    public void updatePreferences(@PathParam("id") int id,
                                  Map<String, String> prefs) {
        verifyUserAccess("preference.update");

        if(id <= 0 || prefs == null)
            throw new BadRequestException();

        Person person = db.getPerson(id);
        if(person == null)
            throw new NotFoundException();

        try {
            prefDb.updatePersonalPreferences(id, prefs);
            LOG.info("Updated personal preferences for: "  + person.getName());
        } catch (Throwable t) {
            LOG.error("Updating personal preferences failed for: " + person.getName(), t);
            throw t;
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Person createPerson(Person person) {
        verifyUserAccess("person.create");
        try {
            if(person.getMemberSince() == null)
                person.setMemberSince(LocalDate.now());

            verifyPrimaryPhone(person);

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
        verifyUserAccess("person.update");
        try {
            verifyPrimaryPhone(person);
            Person updatedPerson = getReconciler().updatePerson(person);
            LOG.info("Edited parishoner: " + person.getName());
            return updatedPerson;
        } catch (Throwable t) {
            LOG.error("Updating person failed:", t);
            throw t;
        }
    }

    @DELETE @Path("/{id}")
    public void deletePerson(@PathParam("id") int id,
                             @QueryParam("delete_permenantly") @DefaultValue("false") boolean permenantDelete) {

        if(permenantDelete)
            verifyUserAccess("admin.person.delete");
        else
            verifyUserAccess("person.delete");

        if(id <= 0)
            throw new NotFoundException();
        try {
            Person person = getReconciler().getPerson(id, false);
            if(person == null)
                throw new NotFoundException();

            if(permenantDelete) {
                if(!getReconciler().deletePerson(person))
                    throw new NotFoundException();
                LOG.info("Deleted parishoner: " + person.getName());
            } else {
                if(!getReconciler().deactivatePerson(person))
                    throw new NotFoundException();
                LOG.info("Deactivated parishoner: " + person.getName());
            }
        } catch (Throwable t) {
            LOG.error("Deleting person failed:", t);
            throw t;
        }
    }

    @GET @Path("/maritalStatuses") @Produces(APPLICATION_JSON)
    public List<String> getMaritalStatuses() { return EnumUtils.listValues(Person.MaritalStatus.class); }

    @GET @Path("/ethnicities") @Produces(APPLICATION_JSON)
    public List<String> getEthnicities() { return EnumUtils.listValues(Person.Ethnicity.class); }

    @GET @Path("/languages") @Produces(APPLICATION_JSON)
    public List<String> getLanguages() { return EnumUtils.listValues(Person.Language.class); }

    @GET @Path("/religions") @Produces(APPLICATION_JSON)
    public List<String> getReligions() { return EnumUtils.listValues(Person.Religion.class); }

    @GET @Path("/specialNeeds") @Produces(APPLICATION_JSON)
    public List<String> getSpecialNeeds() { return EnumUtils.listValues(Person.SpecialNeeds.class); }

    @GET @Path("/phoneNumberTypes") @Produces(APPLICATION_JSON)
    public List<String> getPhoneNumberTypes() { return EnumUtils.listValues(PhoneNumber.PhoneNumberType.class); }

    // ----- Private -----
    private FamilyReconciler getReconciler() { return new FamilyReconciler(db, new FamilyDB(), prefDb); }

    private void verifyPrimaryPhone(Person person) {
        if(person.getPhoneNumbers() == null)
            return;

        List<PhoneNumber> phoneNumbers = person.getPhoneNumbers();

        if(phoneNumbers.size() == 1) {
            phoneNumbers.get(0).setPrimary(true);
        } else if (phoneNumbers.stream().filter(PhoneNumber::isPrimary).count() > 1){
            throw new BadRequestException("Only one primary phone number is allowed");
        }
    }
}

package org.servantscode.person.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.Address;
import org.servantscode.commons.db.EasyDB;
import org.servantscode.commons.db.ReportStreamingOutput;
import org.servantscode.commons.search.*;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.person.Family;
import org.servantscode.person.Person;
import org.servantscode.person.PhoneNumber;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.*;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static org.servantscode.commons.StringUtils.isEmpty;

@SuppressWarnings({"SqlNoDataSourceInspection", "SqlResolve"})
public class PersonDB extends EasyDB<Person> {
    private static final Logger LOG = LogManager.getLogger(PersonDB.class);

    static FieldTransformer FIELD_TRANSFORMER = new FieldTransformer();

    static {
        FIELD_TRANSFORMER.put("lastName","f.surname");
        FIELD_TRANSFORMER.put("memberSince","member_since");
        FIELD_TRANSFORMER.put("birthMonth", "date_part('month', birthdate)", (String val) -> Month.valueOf(val).getValue());
        FIELD_TRANSFORMER.put("birthDayOfMonth", "date_part('day', birthdate)", Integer:: parseInt);
    }

    public PersonDB() {
        super(Person.class, "name", FIELD_TRANSFORMER);
    }

    public int getCount(String search, boolean includeInactive) {
        QueryBuilder query = count().from("people").search(searchParser.parse(search)).inOrg();
        if(!includeInactive)
            query.where("inactive=false");
        return getCount(query);
    }

    public List<Person> getPeople(String search, String sortField, int start, int count, boolean includeInactive) {
        QueryBuilder query = selectAll().from("people p").search(searchParser.parse(search)).inOrg();
        if(!includeInactive)
            query.where("p.inactive=false");
        query.sort(FIELD_TRANSFORMER.transformFieldName(sortField)).limit(count).offset(start);
        return get(query);
    }

    public List<Person> getPeopleWithFamilies(String search, String sortField, int start, int count, boolean includeInactive) {
        QueryBuilder query = select("p.*", "f.surname", "f.addr_street1", "f.addr_street2", "f.addr_city", "f.addr_state", "f.addr_zip")
                .from("people p")
                .join("LEFT JOIN families f ON p.family_id=f.id")
                .search(searchParser.parse(search)).inOrg("p.org_id");
        if(!includeInactive)
            query.where("p.inactive=false");
        query.sort(FIELD_TRANSFORMER.transformFieldName(sortField)).limit(count).offset(start);

        try ( Connection conn = getConnection();
              PreparedStatement stmt = query.prepareStatement(conn)
            ) {

            return processPeopleResultsWithFamilies(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve people containing '" + search + "'", e);
        }
    }

    public Person getPerson(int id) {
        return getOne(selectAll().from("people").with("id", id).inOrg());
    }

    public StreamingOutput getReportReader(String search, boolean includeInactive, final List<String> fields) {
        final QueryBuilder query = select("p.*", "pn.number AS phone_number", "f.surname", "f.addr_street1", "f.addr_street2", "f.addr_city", "f.addr_state", "f.addr_zip")
                .from("people p")
                .leftJoin("families f ON p.family_id=f.id")
                .leftJoin("person_phone_numbers pn ON pn.person_id=p.id AND pn.is_primary=true")
                .search(searchParser.parse(search)).inOrg("p.org_id");
        if(!includeInactive)
            query.where("p.inactive=false");

        return new ReportStreamingOutput(fields) {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                try ( Connection conn = getConnection();
                      PreparedStatement stmt = query.prepareStatement(conn);
                      ResultSet rs = stmt.executeQuery()) {

                    writeCsv(output, rs);
               } catch (SQLException | IOException e) {
                    throw new RuntimeException("Could not retrieve people containing '" + search + "'", e);
                }
            }
        };
    }

    public List<Person> getFamilyMembers(int familyId, boolean includeInactive) {
        QueryBuilder query = selectAll().from("people p").with("family_id", familyId).inOrg();
        if(!includeInactive)
            query.where("p.inactive=false");
        query.sort("p.birthdate");
        return get(query);
    }

    public boolean isMale(int id) {
        QueryBuilder query = select("male").from("people").withId(id).inOrg();
        try ( Connection conn = getConnection();
              PreparedStatement stmt = query.prepareStatement(conn);
              ResultSet rs = stmt.executeQuery()) {

            if(!rs.next())
                throw new RuntimeException("Could not find person with id: " + id);

            return rs.getBoolean(1);
        } catch (SQLException e) {
            throw new RuntimeException("Could not find gender of person id: " + id, e);
        }
    }

    public Person create(Person person) {
        InsertBuilder cmd = insertInto("people")
                .value("name", person.getName())
                .value("birthdate", convert(person.getBirthdate()))
                .value("male", person.isMale())
                .value("salutation", person.getSalutation())
                .value("suffix", person.getSuffix())
                .value("maiden_name", person.getMaidenName())
                .value("nickname", person.getNickname())
                .value("email", person.getEmail())
                .value("family_id", getFamilyId(person))
                .value("head_of_house", person.isHeadOfHousehold())
                .value("member_since", person.getMemberSince())
                .value("parishioner", person.isParishioner())
                .value("baptized", person.isBaptized())
                .value("confession", person.isConfession())
                .value("communion", person.isCommunion())
                .value("confirmed", person.isConfirmed())
                .value("holy_orders", person.isHolyOrders())
                .value("marital_status", stringify(person.getMaritalStatus()))
                .value("ethnicity", stringify(person.getEthnicity()))
                .value("primary_language", stringify(person.getPrimaryLanguage()))
                .value("religion", stringify(person.getReligion()))
                .value("special_needs", storeEnumList(person.getSpecialNeeds()))
                .value("occupation", person.getOccupation())
                .value("inactive", person.isInactive())
                .value("inactive_since", convert(person.getInactiveSince()))
                .value("deceased", person.isDeceased())
                .value("death_date", convert(person.getDeathDate()))
                .value("allergies", encodeList(person.getAllergies()))
                .value("org_id", OrganizationContext.orgId());
        person.setId(createAndReturnKey(cmd));

        storePhoneNumbers(person);
        return person;
    }

    private int getFamilyId(Person person) {
        int familyId = person.getFamily() != null? person.getFamily().getId(): person.getFamilyId();
        if(familyId <= 0)
            throw new IllegalArgumentException("No family information present on person record.");
        return familyId;
    }

    public void update(Person person) {
        UpdateBuilder cmd = update("people")
                .value("name", person.getName())
                .value("birthdate", convert(person.getBirthdate()))
                .value("male", person.isMale())
                .value("salutation", person.getSalutation())
                .value("suffix", person.getSuffix())
                .value("maiden_name", person.getMaidenName())
                .value("nickname", person.getNickname())
                .value("email", person.getEmail())
                .value("family_id", getFamilyId(person))
                .value("head_of_house", person.isHeadOfHousehold())
                .value("member_since", person.getMemberSince())
                .value("parishioner", person.isParishioner())
                .value("baptized", person.isBaptized())
                .value("confession", person.isConfession())
                .value("communion", person.isCommunion())
                .value("confirmed", person.isConfirmed())
                .value("holy_orders", person.isHolyOrders())
                .value("marital_status", stringify(person.getMaritalStatus()))
                .value("ethnicity", stringify(person.getEthnicity()))
                .value("primary_language", stringify(person.getPrimaryLanguage()))
                .value("religion", stringify(person.getReligion()))
                .value("special_needs", storeEnumList(person.getSpecialNeeds()))
                .value("occupation", person.getOccupation())
                .value("inactive", person.isInactive())
                .value("inactive_since", convert(person.getInactiveSince()))
                .value("deceased", person.isDeceased())
                .value("death_date", convert(person.getDeathDate()))
                .value("allergies", encodeList(person.getAllergies()))
                .withId(person.getId()).inOrg();
        if(!update(cmd))
            throw new RuntimeException("Could not update person: " + person.getName());

        storePhoneNumbers(person);
    }

    public boolean deactivate(Person person) {
        return update(update("people").value("inactive", true).value("inactive_since", convert(LocalDate.now())).withId(person.getId()).inOrg());
    }

    public boolean delete(Person person) {
        return delete(deleteFrom("people").withId(person.getId()).inOrg());
    }

    public void deleteByFamilyId(int familyId) {
        delete(deleteFrom("people").with("family_id", familyId).inOrg());
    }

    public void activateByFamilyId(int familyId) {
        update(update("people").value("inactive", false).value("inactive_since", null).with("family_id", familyId).inOrg());
    }

    public void deactivateByFamilyId(int familyId) {
        update(update("people").value("inactive", true).value("inactive_since", convert(LocalDate.now())).with("family_id", familyId).inOrg());
    }

    public void attchPhoto(int id, String guid) {
        update(update("people").value("photo_guid", guid).withId(id).inOrg());
    }

    // ----- Private ------
    private List<Person> processPeopleResultsWithFamilies(PreparedStatement stmt) throws SQLException {
        LOG.debug("Processing people with family information");
        try (ResultSet rs = stmt.executeQuery()){
            List<Person> people = new ArrayList<>();
            while(rs.next()) {
                Person person = processRow(rs);

                String surname = rs.getString("surname");
                if(!isEmpty(surname)) {
                    Family family = new Family(rs.getInt("family_id"), surname);
                    person.setFamily(family);

                    String street1 = rs.getString("addr_street1");
                    if(!isEmpty(street1)) {
                        Address addr = new Address(street1,
                                rs.getString("addr_street2"),
                                rs.getString("addr_city"),
                                rs.getString("addr_state"),
                                rs.getInt("addr_zip"));
                        family.setAddress(addr);
                    }
                }
                people.add(person);
            }
            return people;
        }
    }

    protected Person processRow(ResultSet rs) throws SQLException {
        Person person = new Person(rs.getInt("id"), rs.getString("name"));
        person.setBirthdate(convert(rs.getDate("birthdate")));
        person.setMale(rs.getBoolean("male"));
        person.setEmail(rs.getString("email"));
        person.setFamilyId(rs.getInt("family_id"));
        person.setHeadOfHousehold(rs.getBoolean("head_of_house"));
        person.setPhotoGuid(rs.getString("photo_guid"));
        person.setParishioner(rs.getBoolean("parishioner"));
        person.setMemberSince(convert(rs.getDate("member_since")));
        person.setInactive(rs.getBoolean("inactive"));
        person.setInactiveSince(convert(rs.getDate("inactive_since")));
        person.setDeceased(rs.getBoolean("deceased"));
        person.setDeathDate(convert(rs.getDate("death_date")));
        person.setBaptized(rs.getBoolean("baptized"));
        person.setConfession(rs.getBoolean("confession"));
        person.setCommunion(rs.getBoolean("communion"));
        person.setConfirmed(rs.getBoolean("confirmed"));
        person.setHolyOrders(rs.getBoolean("holy_orders"));
        person.setMaritalStatus(parse(Person.MaritalStatus.class, rs.getString("marital_status")));
        person.setEthnicity(parse(Person.Ethnicity.class, rs.getString("ethnicity")));
        person.setPrimaryLanguage(parse(Person.Language.class, rs.getString("primary_language")));
        person.setReligion(parse(Person.Religion.class, rs.getString("religion")));
        person.setSpecialNeeds(parseEnumList(Person.SpecialNeeds.class, rs.getString("special_needs")));
        person.setOccupation(rs.getString("occupation"));
        person.setSalutation(rs.getString("salutation"));
        person.setSuffix(rs.getString("suffix"));
        person.setMaidenName(rs.getString("maiden_name"));
        person.setNickname(rs.getString("nickname"));
        person.setAllergies(decodeList(rs.getString("allergies")));

        person.setPhoneNumbers(getPhoneNumbers(person.getId(), rs.getStatement().getConnection()));
        return person;
    }

    private void storePhoneNumbers(Person person) {
        DeleteBuilder clearCmd = deleteFrom("person_phone_numbers").with("person_id", person.getId());
        delete(clearCmd);

        if(person.getPhoneNumbers() == null)
            return;

        // TODO: Add batching to EasyDB;
        for(PhoneNumber phoneNumber: person.getPhoneNumbers()) {
            InsertBuilder cmd = insertInto("person_phone_numbers")
                    .value("person_id", person.getId())
                    .value("number", phoneNumber.getPhoneNumber())
                    .value("type", stringify(phoneNumber.getType()))
                    .value("is_primary", phoneNumber.isPrimary());
            create(cmd);
        }
    }

    private List<PhoneNumber> getPhoneNumbers(int personId, Connection conn) throws SQLException {
        QueryBuilder query = selectAll().from("person_phone_numbers").with("person_id", personId);
        try (PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery()) {

            LinkedList<PhoneNumber> results = new LinkedList<>();
            while(rs.next()) {
                PhoneNumber number = new PhoneNumber();
                number.setPhoneNumber(rs.getString("number"));
                number.setType(parse(PhoneNumber.PhoneNumberType.class, rs.getString("type")));
                number.setPrimary(rs.getBoolean("is_primary"));
                results.add(number);
            }
            return results;
        }
    }
}

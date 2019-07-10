package org.servantscode.person.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.db.ReportStreamingOutput;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.SearchParser;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.person.Address;
import org.servantscode.person.Family;
import org.servantscode.person.Person;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.servantscode.commons.StringUtils.isEmpty;

@SuppressWarnings({"SqlNoDataSourceInspection", "SqlResolve"})
public class PersonDB extends DBAccess {
    private static final Logger LOG = LogManager.getLogger(PersonDB.class);

    private SearchParser<Person> searchParser;
    static HashMap<String, String> FIELD_MAP = new HashMap<>(8);

    static {
        FIELD_MAP.put("lastName","f.surname");
        FIELD_MAP.put("memberSince","member_since");
    }

    public PersonDB() {
        this.searchParser = new SearchParser<>(Person.class, "name", FIELD_MAP);
    }

    public int getCount(String search, boolean includeInactive) {
        QueryBuilder query = count().from("people").search(searchParser.parse(search)).inOrg();
        if(!includeInactive)
            query.where("inactive=false");

        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve people count '" + search + "'", e);
        }
        return 0;
    }

    public List<Person> getPeople(String search, String sortField, int start, int count, boolean includeInactive) {
        QueryBuilder query = selectAll().from("people p").search(searchParser.parse(search)).inOrg();
        if(!includeInactive)
            query.where("p.inactive=false");
        query.sort(FIELD_MAP.getOrDefault(sortField, sortField)).limit(count).offset(start);

        try ( Connection conn = getConnection();
              PreparedStatement stmt = query.prepareStatement(conn)
        ) {

            return processPeopleResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve people containing '" + search + "'", e);
        }
    }

    public List<Person> getPeopleWithFamilies(String search, String sortField, int start, int count, boolean includeInactive) {
        QueryBuilder query = select("p.*", "f.surname", "f.addr_street1", "f.addr_street2", "f.addr_city", "f.addr_state", "f.addr_zip")
                .from("people p")
                .join("LEFT JOIN families f ON p.family_id=f.id")
                .search(searchParser.parse(search)).inOrg("p.org_id");
        if(!includeInactive)
            query.where("p.inactive=false");
        query.sort(FIELD_MAP.getOrDefault(sortField, sortField)).limit(count).offset(start);

        try ( Connection conn = getConnection();
              PreparedStatement stmt = query.prepareStatement(conn)
            ) {

            return processPeopleResultsWithFamilies(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve people containing '" + search + "'", e);
        }
    }

    public Person getPerson(int id) {
        QueryBuilder query = selectAll().from("people").where("id=?", id).inOrg();
        try ( Connection conn = getConnection();
              PreparedStatement stmt = query.prepareStatement(conn);
            ) {

            return firstOrNull(processPeopleResults(stmt));
        } catch (SQLException e) {
            throw new RuntimeException("Could not find person by id " + id, e);
        }
    }

    public StreamingOutput getReportReader(String search, boolean includeInactive, final List<String> fields) {
        final QueryBuilder query = selectAll().from("people p").search(searchParser.parse(search)).inOrg();
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

        QueryBuilder query = selectAll().from("people p").where("family_id=?", familyId).inOrg();
        if(!includeInactive)
            query.where("p.inactive=false");

        try ( Connection conn = getConnection();
              PreparedStatement stmt = query.prepareStatement(conn)
            ) {

            return processPeopleResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not find family memebers for family id " + familyId, e);
        }
    }

    public boolean isMale(int id) {
        QueryBuilder query = select("male").from("people").withId(id).inOrg();
        try ( Connection conn = getConnection();
              PreparedStatement stmt = query.prepareStatement(conn);
              ResultSet rs = stmt.executeQuery()
        ) {
            if(!rs.next())
                throw new RuntimeException("Could not find person with id: " + id);

            return rs.getBoolean(1);
        } catch (SQLException e) {
            throw new RuntimeException("Could not find gender of person id: " + id, e);
        }
    }

    public void create(Person person) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO people" +
                     "(name, birthdate, male, phonenumber, email, family_id, head_of_house, member_since, parishioner, " +
                     "baptized, confession, communion, confirmed, marital_status, " +
                     "ethnicity, primary_language, religion, special_needs, occupation, org_id) " +
                     "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?)", Statement.RETURN_GENERATED_KEYS)
        ){
            stmt.setString(1, person.getName());
            stmt.setDate(2, convert(person.getBirthdate()));
            stmt.setBoolean(3, person.isMale());
            stmt.setString(4, person.getPhoneNumber());
            stmt.setString(5, person.getEmail());
            stmt.setInt(6, person.getFamily().getId());
            stmt.setBoolean(7, person.isHeadOfHousehold());
            stmt.setDate(8, convert(person.getMemberSince()));
            stmt.setBoolean(9, person.isParishioner());
            stmt.setBoolean(10, person.isBaptized());
            stmt.setBoolean(11, person.isConfession());
            stmt.setBoolean(12, person.isCommunion());
            stmt.setBoolean(13, person.isConfirmed());
            stmt.setString(14, stringify(person.getMaritalStatus()));
            stmt.setString(15, stringify(person.getEthnicity()));
            stmt.setString(16, stringify(person.getPrimaryLanguage()));
            stmt.setString(17, stringify(person.getReligion()));
            stmt.setString(18, storeEnumList(person.getSpecialNeeds()));
            stmt.setString(19, person.getOccupation());
            stmt.setInt(20, OrganizationContext.orgId());

            if(stmt.executeUpdate() == 0) {
                throw new RuntimeException("Could not create person: " + person.getName());
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next())
                    person.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not add person: " + person.getName(), e);
        }
    }

    public void update(Person person) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("UPDATE people " +
                      "SET name=?, birthdate=?, male=?, phonenumber=?, email=?, family_id=?, head_of_house=?, member_since=?, " +
                      "inactive=?, parishioner=?, baptized=?, confession=?, communion=?, confirmed=?, marital_status=?, " +
                      "ethnicity=?, primary_language=?, religion=?, special_needs=?, occupation=?, org_id=? " +
                      "WHERE id=?")
            ){

            stmt.setString(1, person.getName());
            stmt.setDate(2, convert(person.getBirthdate()));
            stmt.setBoolean(3, person.isMale());
            stmt.setString(4, person.getPhoneNumber());
            stmt.setString(5, person.getEmail());
            stmt.setInt(6, person.getFamily().getId());
            stmt.setBoolean(7, person.isHeadOfHousehold());
            stmt.setDate(8, convert(person.getMemberSince()));
            stmt.setBoolean(9, person.isInactive());
            stmt.setBoolean(10, person.isParishioner());
            stmt.setBoolean(11, person.isBaptized());
            stmt.setBoolean(12, person.isConfession());
            stmt.setBoolean(13, person.isCommunion());
            stmt.setBoolean(14, person.isConfirmed());
            stmt.setString(15, stringify(person.getMaritalStatus()));
            stmt.setString(16, stringify(person.getEthnicity()));
            stmt.setString(17, stringify(person.getPrimaryLanguage()));
            stmt.setString(18, stringify(person.getReligion()));
            stmt.setString(19, storeEnumList(person.getSpecialNeeds()));
            stmt.setString(20, person.getOccupation());
            stmt.setInt(21, OrganizationContext.orgId());
            stmt.setInt(22, person.getId());

            if(stmt.executeUpdate() == 0)
                throw new RuntimeException("Could not update person: " + person.getName());

        } catch (SQLException e) {
            throw new RuntimeException("Could not update person: " + person.getName(), e);
        }
    }

    public boolean deactivate(Person person) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("UPDATE people SET inactive=? WHERE id=? AND org_id=?")
        ){

            stmt.setBoolean(1, true);
            stmt.setInt(2, person.getId());
            stmt.setInt(3, OrganizationContext.orgId());

            return stmt.executeUpdate() != 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not deactivate person: " + person.getName(), e);
        }
    }

    public boolean delete(Person person) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("DELETE FROM people WHERE id=? AND org_id=?")
        ){

            stmt.setInt(1, person.getId());
            stmt.setInt(2, OrganizationContext.orgId());

            return stmt.executeUpdate() != 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete person: " + person.getName(), e);
        }
    }

    public void deleteByFamilyId(int familyId) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("DELETE FROM people WHERE family_id=? AND org_id=?")
            ){

            stmt.setInt(1, familyId);
            stmt.setInt(2, OrganizationContext.orgId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete people by family id: " + familyId, e);
        }
    }

    public void activateByFamilyId(int familyId) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("UPDATE people SET inactive=? WHERE family_id=? AND org_id=?")
        ){

            stmt.setBoolean(1, false);
            stmt.setInt(2, familyId);
            stmt.setInt(3, OrganizationContext.orgId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not activate people by family id: " + familyId, e);
        }
    }

    public void deactivateByFamilyId(int familyId) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("UPDATE people SET inactive=? WHERE family_id=? AND org_id=?")
        ){

            stmt.setBoolean(1, true);
            stmt.setInt(2, familyId);
            stmt.setInt(3, OrganizationContext.orgId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not deactivate people by family id: " + familyId, e);
        }
    }

    public void attchPhoto(int id, String guid) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("UPDATE people SET photo_guid=? WHERE id=? AND org_id=?");
        ){
            stmt.setString(1, guid);
            stmt.setInt(2, id);
            stmt.setInt(3, OrganizationContext.orgId());

            if(stmt.executeUpdate() == 0)
                throw new NotFoundException("Could not attach photo to person: " + id);

        } catch (SQLException e) {
            throw new RuntimeException("Could not attach photo to person: " + id, e);
        }
    }

    // ----- Private ------
    private List<Person> processPeopleResults(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()){
            List<Person> people = new ArrayList<>();
            while(rs.next()) {
                people.add(extractPerson(rs));
            }
            return people;
        }
    }

    private List<Person> processPeopleResultsWithFamilies(PreparedStatement stmt) throws SQLException {
        LOG.debug("Processing people with family information");
        try (ResultSet rs = stmt.executeQuery()){
            List<Person> people = new ArrayList<>();
            while(rs.next()) {
                Person person = extractPerson(rs);

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

    private Person extractPerson(ResultSet rs) throws SQLException {
        Person person = new Person(rs.getInt("id"), rs.getString("name"));
        person.setBirthdate(convert(rs.getDate("birthdate")));
        person.setMale(rs.getBoolean("male"));
        person.setPhoneNumber(rs.getString("phonenumber"));
        person.setEmail(rs.getString("email"));
        person.setFamilyId(rs.getInt("family_id"));
        person.setHeadOfHousehold(rs.getBoolean("head_of_house"));
        person.setMemberSince(convert(rs.getDate("member_since")));
        person.setPhotoGuid(rs.getString("photo_guid"));
        person.setInactive(rs.getBoolean("inactive"));
        person.setParishioner(rs.getBoolean("parishioner"));
        person.setBaptized(rs.getBoolean("baptized"));
        person.setConfession(rs.getBoolean("confession"));
        person.setCommunion(rs.getBoolean("communion"));
        person.setConfirmed(rs.getBoolean("confirmed"));
        person.setMaritalStatus(parse(Person.MaritalStatus.class, rs.getString("marital_status")));
        person.setEthnicity(parse(Person.Ethnicity.class, rs.getString("ethnicity")));
        person.setPrimaryLanguage(parse(Person.Language.class, rs.getString("primary_language")));
        person.setReligion(parse(Person.Religion.class, rs.getString("religion")));
        person.setSpecialNeeds(parseEnumList(Person.SpecialNeeds.class, rs.getString("special_needs")));
        person.setOccupation(rs.getString("occupation"));
        return person;
    }

}

package org.servantscode.person.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.db.ReportStreamingOutput;
import org.servantscode.commons.search.SearchParser;
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
import java.util.List;

import static java.lang.String.format;
import static org.servantscode.commons.StringUtils.isEmpty;
import static org.servantscode.commons.StringUtils.isSet;

public class PersonDB extends DBAccess {
    private static final Logger LOG = LogManager.getLogger(PersonDB.class);

    public int getCount(String search, boolean includeInactive) {
        String sql = format("Select count(1) from people p%s", optionalWhereClause(search, includeInactive));
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve people count '" + search + "'", e);
        }
        return 0;
    }

    public List<Person> getPeople(String search, String sortField, int start, int count, boolean includeInactive) {
        String sql = format("SELECT * FROM people p%s ORDER BY %s LIMIT ? OFFSET ?", optionalWhereClause(search, includeInactive), sortField);
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, count);
            stmt.setInt(2, start);

            return processPeopleResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve people containing '" + search + "'", e);
        }
    }

    public List<Person> getPeopleWithFamilies(String search, String sortField, int start, int count, boolean includeInactive) {
        String sql = format("SELECT p.*, f.surname, f.addr_street1, f.addr_street2, f.addr_city, f.addr_state, f.addr_zip FROM people p " +
                            "LEFT JOIN families f ON p.family_id=f.id" +
                            "%s ORDER BY %s LIMIT ? OFFSET ?",
                optionalWhereClause(search, includeInactive), sortField);
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, count);
            stmt.setInt(2, start);

            return processPeopleResultsWithFamilies(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve people containing '" + search + "'", e);
        }
    }

    public Person getPerson(int id) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("SELECT * from people where id=?")
        ) {
            stmt.setInt(1, id);
            List<Person> results = processPeopleResults(stmt);

            return results.isEmpty()? null: results.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Could not find person by id " + id, e);
        }
    }

    public StreamingOutput getReportReader(String search, boolean includeInactive, final List<String> fields) {
        final String sql = format("SELECT * FROM people p%s", optionalWhereClause(search, includeInactive));

        return new ReportStreamingOutput(fields) {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                try ( Connection conn = getConnection();
                      PreparedStatement stmt = conn.prepareStatement(sql);
                      ResultSet rs = stmt.executeQuery()) {

                    writeCsv(output, rs);
               } catch (SQLException | IOException e) {
                    throw new RuntimeException("Could not retrieve people containing '" + search + "'", e);
                }
            }
        };
    }

    public List<Person> getFamilyMembers(int familyId, boolean includeInactive) {
        String sql = "SELECT * FROM people p WHERE family_id=?";
        if(!includeInactive) sql += " AND inactive=false";
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement(sql)
            ) {

            stmt.setInt(1, familyId);
            return processPeopleResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not find family memebers for family id " + familyId, e);
        }
    }

    public void create(Person person) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO people(name, birthdate, male, phonenumber, email, family_id, head_of_house, member_since) values (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
        ){
            stmt.setString(1, person.getName());
            stmt.setTimestamp(2, convert(person.getBirthdate()));
            stmt.setBoolean(3, person.isMale());
            stmt.setString(4, person.getPhoneNumber());
            stmt.setString(5, person.getEmail());
            stmt.setInt(6, person.getFamily().getId());
            stmt.setBoolean(7, person.isHeadOfHousehold());
            stmt.setTimestamp(8, convert(person.getMemberSince()));

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
              PreparedStatement stmt = conn.prepareStatement("UPDATE people SET name=?, birthdate=?, male=?, phonenumber=?, email=?, family_id=?, head_of_house=?, member_since=?, inactive=? WHERE id=?")
            ){

            stmt.setString(1, person.getName());
            stmt.setTimestamp(2, convert(person.getBirthdate()));
            stmt.setBoolean(3, person.isMale());
            stmt.setString(4, person.getPhoneNumber());
            stmt.setString(5, person.getEmail());
            stmt.setInt(6, person.getFamily().getId());
            stmt.setBoolean(7, person.isHeadOfHousehold());
            stmt.setTimestamp(8, convert(person.getMemberSince()));
            stmt.setBoolean(9, person.isInactive());
            stmt.setInt(10, person.getId());

            if(stmt.executeUpdate() == 0)
                throw new RuntimeException("Could not update person: " + person.getName());

        } catch (SQLException e) {
            throw new RuntimeException("Could not update person: " + person.getName(), e);
        }
    }

    public boolean deactivate(Person person) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("UPDATE people SET inactive=? WHERE id=?")
        ){

            stmt.setBoolean(1, true);
            stmt.setInt(2, person.getId());

            return stmt.executeUpdate() != 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not deactivate person: " + person.getName(), e);
        }
    }

    public boolean delete(Person person) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("DELETE FROM people WHERE id=?")
        ){

            stmt.setInt(1, person.getId());

            return stmt.executeUpdate() != 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete person: " + person.getName(), e);
        }
    }

    public void deleteByFamilyId(int familyId) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("DELETE FROM people WHERE family_id=?")
            ){

            stmt.setInt(1, familyId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete people by family id: " + familyId, e);
        }
    }

    public void activateByFamilyId(int familyId) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("UPDATE people SET inactive=? WHERE family_id=?")
        ){

            stmt.setBoolean(1, false);
            stmt.setInt(2, familyId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not activate people by family id: " + familyId, e);
        }
    }

    public void deactivateByFamilyId(int familyId) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("UPDATE people SET inactive=? WHERE family_id=?")
        ){

            stmt.setBoolean(1, true);
            stmt.setInt(2, familyId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not deactivate people by family id: " + familyId, e);
        }
    }

    public void attchPhoto(int id, String guid) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("UPDATE people SET photo_guid=? WHERE id=?");
        ){
            stmt.setString(1, guid);
            stmt.setInt(2, id);

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
        person.setBirthdate(convert(rs.getTimestamp("birthdate")));
        person.setMale(rs.getBoolean("male"));
        person.setPhoneNumber(rs.getString("phonenumber"));
        person.setEmail(rs.getString("email"));
        person.setFamilyId(rs.getInt("family_id"));
        person.setHeadOfHousehold(rs.getBoolean("head_of_house"));
        person.setMemberSince(convert(rs.getTimestamp("member_since")));
        person.setPhotoGuid(rs.getString("photo_guid"));
        person.setInactive(rs.getBoolean("inactive"));
        return person;
    }

    private String optionalWhereClause(String search, boolean includeInactive) {
        String sqlClause = !includeInactive? "p.inactive=false": "";
        if(isSet(search)) {
            if(isSet(sqlClause)) sqlClause += " AND ";
            sqlClause += new SearchParser(Person.class).parse(search).getDBQueryString();
        }
        String where = isEmpty(sqlClause) ? "" : " WHERE " + sqlClause;
        return where;
    }
}

package org.servantscode.person.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.AutoCompleteComparator;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.person.Person;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.servantscode.commons.StringUtils.isEmpty;

public class PersonDB extends DBAccess {
    private static final Logger LOG = LogManager.getLogger(PersonDB.class);

    public int getCount(String search) {
        String sql = format("Select count(1) from people%s", optionalWhereClause(search));
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve people count '" + search + "'", e);
        }
        return 0;
    }

    public List<Person> getPeople(String search, String sortField, int start, int count) {
        String sql = format("SELECT * FROM people%s ORDER BY %s LIMIT ? OFFSET ?", optionalWhereClause(search), sortField);
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

    public List<String> getPeopleNames(String search, int count) {
        String sql = format("SELECT name FROM people%s", optionalWhereClause(search));
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            List<String> names = new ArrayList<>();

            while (rs.next())
                names.add(rs.getString(1));

            long start = System.currentTimeMillis();
            names.sort(new AutoCompleteComparator(search));
            LOG.debug(String.format("Sorted %d names in %d ms.", names.size(), System.currentTimeMillis()-start));

            return (count < names.size()) ? names : names.subList(0, count);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve names containing '" + search + "'", e);
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

    public List<Person> getFamilyMembers(int familyId) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("SELECT * FROM people WHERE family_id=?")
            ) {

            stmt.setInt(1, familyId);
            return processPeopleResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not find family memebers for family id " + familyId, e);
        }
    }

    public void create(Person person) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO people(name, birthdate, phonenumber, email, family_id, head_of_house) values (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
        ){
            stmt.setString(1, person.getName());
            stmt.setDate(2, convert(person.getBirthdate()));
            stmt.setString(3, person.getPhoneNumber());
            stmt.setString(4, person.getEmail());
            stmt.setInt(5, person.getFamily().getId());
            stmt.setBoolean(6, person.isHeadOfHousehold());

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
              PreparedStatement stmt = conn.prepareStatement("UPDATE people SET name=?, birthdate=?, phonenumber=?, email=?, family_id=?, head_of_house=? WHERE id=?")
            ){

            stmt.setString(1, person.getName());
            stmt.setDate(2, convert(person.getBirthdate()));
            stmt.setString(3, person.getPhoneNumber());
            stmt.setString(4, person.getEmail());
            stmt.setInt(5, person.getFamily().getId());
            stmt.setBoolean(6, person.isHeadOfHousehold());
            stmt.setInt(7, person.getId());

            if(stmt.executeUpdate() == 0)
                throw new RuntimeException("Could not update person: " + person.getName());

        } catch (SQLException e) {
            throw new RuntimeException("Could not update person: " + person.getName(), e);
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

        } catch (SQLException e) {
            throw new RuntimeException("Could not delete people by family id: " + familyId, e);
        }
    }

    // ----- Private ------
    private List<Person> processPeopleResults(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()){
            List<Person> people = new ArrayList<>();
            while(rs.next()) {
                Person person = new Person(rs.getInt("id"), rs.getString("name"));
                person.setBirthdate(rs.getDate("birthdate"));
                person.setPhoneNumber(rs.getString("phonenumber"));
                person.setEmail(rs.getString("email"));
                person.setFamilyId(rs.getInt("family_id"));
                person.setHeadOfHousehold(rs.getBoolean("head_of_house"));
                people.add(person);
            }
            return people;
        }
    }

    private String optionalWhereClause(String search) {
        return !isEmpty(search)? format(" WHERE name ILIKE '%%%s%%'", search.replace("'", "''")) : "";
    }
}

package org.servantscode.person.db;

import org.servantscode.person.Person;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static org.servantscode.person.StringUtils.isEmpty;

public class PersonDB extends DBAccess {

    public int getCount(String search) {
        String sql = format("Select count(1) from people%s", optionalWhereClause(search));
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            try ( ResultSet rs = stmt.executeQuery() ){
                if(rs.next())
                    return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    public List<String> getPeopleNames(String search, int count) {
        String sql = format("SELECT name FROM people%s", optionalWhereClause(search));
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            try ( ResultSet rs = stmt.executeQuery()){
                List<String> names = new ArrayList<>();

                while(rs.next())
                    names.add(rs.getString(1));

                names.sort(new AutoCompleteComparator(search));
                return (count < names.size())? names: names.subList(0, count);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    public Person getPerson(int id) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("SELECT * from people where id=?")
        ) {
            stmt.setInt(1, id);
            List<Person> results = processPeopleResults(stmt);

            return results.isEmpty()? null: results.get(0);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void addPerson(Person person) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO people(name, birthdate, phonenumber, email) values (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
        ){
            stmt.setString(1, person.getName());
            stmt.setDate(2, convert(person.getBirthdate()));
            stmt.setLong(3, person.getPhoneNumber());
            stmt.setString(4, person.getEmail());

            if(stmt.executeUpdate() == 0) {
                throw new RuntimeException("Could not create person: " + person.getName());
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next())
                    person.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updatePerson(Person person) {
    try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("UPDATE people SET name=?, birthdate=?, phonenumber=?, email=? WHERE id=?")
            ){
            stmt.setString(1, person.getName());
            stmt.setDate(2, convert(person.getBirthdate()));
            stmt.setLong(3, person.getPhoneNumber());
            stmt.setString(4, person.getEmail());
            stmt.setInt(5, person.getId());

            if(stmt.executeUpdate() == 0)
                throw new RuntimeException("Could not update person: " + person.getName());

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ----- Private ------
    private List<Person> processPeopleResults(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()){
            List<Person> people = new ArrayList<>();
            while(rs.next()) {
                Person person = new Person(rs.getInt("id"), rs.getString("name"));
                person.setBirthdate(rs.getDate("birthdate"));
                person.setPhoneNumber(rs.getLong("phonenumber"));
                person.setEmail(rs.getString("email"));
                people.add(person);
            }
            return people;
        }
    }

    private String optionalWhereClause(String search) {
        return !isEmpty(search)? format(" WHERE name ILIKE '%%%s%%'", search) : "";
    }

    private java.sql.Date convert(Date input) {
        if(input == null) return null;
        return new java.sql.Date(input.getTime());
    }
}

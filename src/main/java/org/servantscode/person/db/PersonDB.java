package org.servantscode.person.db;

import org.servantscode.person.Person;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class PersonDB extends DBAccess {

    public List<Person> getPeople() {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("Select * from people");
            ) {
            try ( ResultSet rs = stmt.executeQuery(); ){
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
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    public Person getPerson(int id) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("Select * from people where id=?");
            ) {
            stmt.setInt(1, id);
            try ( ResultSet rs = stmt.executeQuery(); ){
                if(rs.next()) {
                    Person person = new Person(rs.getInt("id"), rs.getString("name"));
                    person.setBirthdate(rs.getDate("birthdate"));
                    person.setPhoneNumber(rs.getLong("phonenumber"));
                    person.setEmail(rs.getString("email"));
                    return person;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void addPerson(Person person) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("INSERT INTO people(name, birthdate, phonenumber, email) values (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ){
            stmt.setString(1, person.getName());
            stmt.setDate(2, convert(person.getBirthdate()));
            stmt.setLong(3, person.getPhoneNumber());
            stmt.setString(4, person.getEmail());

            if(stmt.executeUpdate() == 0) {
                throw new RuntimeException("Could not create person: " + person.getName());
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    person.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updatePerson(Person person) {
    try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("UPDATE people SET name=?, birthdate=?, phonenumber=?, email=? WHERE id=?");
            ){
            stmt.setString(1, person.getName());
            stmt.setDate(2, convert(person.getBirthdate()));
            stmt.setLong(3, person.getPhoneNumber());
            stmt.setString(4, person.getEmail());
            stmt.setInt(5, person.getId());

            if(stmt.executeUpdate() == 0) {
                throw new RuntimeException("Could not update person: " + person.getName());
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private java.sql.Date convert(Date input) {
        if(input == null) return null;
        return new java.sql.Date(input.getTime());
    };
}

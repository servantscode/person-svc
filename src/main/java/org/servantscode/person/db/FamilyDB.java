package org.servantscode.person.db;

import org.servantscode.commons.AutoCompleteComparator;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.person.Address;
import org.servantscode.person.Family;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.servantscode.commons.StringUtils.isEmpty;

public class FamilyDB extends DBAccess {

    public int getCount(String search) {
        String sql = format("Select count(1) from families%s", optionalWhereClause(search));
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery() ){

            return rs.next()? rs.getInt(1): 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not get families with surnames containing " + search, e);
        }
    }

    public List<Family> getFamilies(String search, String sortField, int start, int count) {
        String sql = format("SELECT * FROM families%s ORDER BY %s LIMIT ? OFFSET ?", optionalWhereClause(search), sortField);
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, count);
            stmt.setInt(2, start);

            return processFamilyResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not get families with surnames containing " + search, e);
        }
    }

    public List<String> getFamilySurnames(String search, int count) {
        String sql = format("SELECT surname FROM families%s", optionalWhereClause(search));
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
            throw new RuntimeException("Could not get family surnames containing " + search, e);
        }
    }

    public Family getFamily(int id) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("SELECT * FROM families WHERE id=?")
            ) {

            stmt.setInt(1, id);
            List<Family> results = processFamilyResults(stmt);
            return results.isEmpty()? null: results.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Could not get family by id: " + id, e);
        }
    }

    public void create(Family family) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO families(surname, addr_street1, addr_street2, addr_city, addr_state, addr_zip) values (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
        ){
            Address addr = family.getAddress();

            stmt.setString(1, family.getSurname());
            stmt.setString(2, addr.getStreet1());
            stmt.setString(3, addr.getStreet2());
            stmt.setString(4, addr.getCity());
            stmt.setString(5, addr.getState());
            stmt.setInt(6, addr.getZip());

            if(stmt.executeUpdate() == 0) {
                throw new RuntimeException("Could not create family: " + family.getSurname());
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next())
                    family.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not create family: " + family.getSurname(), e);
        }
    }

    public void update(Family family) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("UPDATE families SET surname=?, addr_street1=?, addr_street2=?, addr_city=?, addr_state=?, addr_zip=? WHERE id=?")
            ){

            Address addr = family.getAddress();

            stmt.setString(1, family.getSurname());
            stmt.setString(2, addr.getStreet1());
            stmt.setString(3, addr.getStreet2());
            stmt.setString(4, addr.getCity());
            stmt.setString(5, addr.getState());
            stmt.setInt(6, addr.getZip());
            stmt.setInt(7, family.getId());

            if(stmt.executeUpdate() == 0)
                throw new RuntimeException("Could not update family: " + family.getSurname());

        } catch (SQLException e) {
            throw new RuntimeException("Could not update family: " + family.getSurname(), e);
        }
    }

    public boolean delete(Family family) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("DELETE FROM families WHERE id=?")
            ){

            stmt.setInt(1, family.getId());

            return stmt.executeUpdate() != 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete family: " + family.getSurname(), e);
        }
    }

    // ----- Private ------
    private List<Family> processFamilyResults(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()){
            List<Family> families = new ArrayList<>();
            while(rs.next()) {
                Family family = new Family(rs.getInt("id"), rs.getString("surname"));
                Address addr = new Address(rs.getString("addr_street1"),
                                           rs.getString("addr_street2"),
                                           rs.getString("addr_city"),
                                           rs.getString("addr_state"),
                                           rs.getInt("addr_zip"));
                family.setAddress(addr);
                families.add(family);
            }
            return families;
        }
    }

    private String optionalWhereClause(String search) {
    return !isEmpty(search)? format(" WHERE surname ILIKE '%%%s%%'", search.replace("'", "''")) : "";
    }
}

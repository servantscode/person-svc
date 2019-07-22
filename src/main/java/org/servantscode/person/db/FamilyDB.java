package org.servantscode.person.db;

import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.db.ReportStreamingOutput;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.SearchParser;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.person.Address;
import org.servantscode.person.Family;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FamilyDB extends DBAccess {

    private SearchParser<Family> searchParser;
    static HashMap<String, String> FIELD_MAP = new HashMap<>(8);

    static {
        FIELD_MAP.put("envelopeNumber","envelope_number");
        FIELD_MAP.put("address.street1","addr_street1");
        FIELD_MAP.put("address.city","addr_city");
        FIELD_MAP.put("address.state","addr_state");
        FIELD_MAP.put("address.zip","addr_zip");
    }

    public FamilyDB() {
        this.searchParser = new SearchParser<>(Family.class, "surname", FIELD_MAP);
    }

    public int getCount(String search, boolean includeInactive) {
        QueryBuilder query = count().from("families").search(searchParser.parse(search)).inOrg();
        if(!includeInactive)
            query.where("inactive=false");

        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery() ){

            return rs.next()? rs.getInt(1): 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not get families with surnames containing " + search, e);
        }
    }

    public StreamingOutput getReportReader(String search, boolean includeInactive, final List<String> fields) {
        final QueryBuilder query = selectAll().from("families").search(searchParser.parse(search)).inOrg();
        if(!includeInactive)
            query.where("inactive=false");

        return new ReportStreamingOutput(fields) {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                try ( Connection conn = getConnection();
                      PreparedStatement stmt = query.prepareStatement(conn);
                      ResultSet rs = stmt.executeQuery()) {

                    writeCsv(output, rs);
                } catch (SQLException | IOException e) {
                    throw new RuntimeException("Could not retrieve families containing '" + search + "'", e);
                }
            }
        };
    }

    public List<Family> getFamilies(String search, String sortField, int start, int count, boolean includeInactive) {
        QueryBuilder query = selectAll().from("families").search(searchParser.parse(search)).inOrg();
        if(!includeInactive)
            query.where("inactive=false");
        query.sort(sortField).limit(count).offset(start);

        try ( Connection conn = getConnection();
              PreparedStatement stmt = query.prepareStatement(conn)
        ) {

            return processFamilyResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not get families with surnames containing " + search, e);
        }
    }

    public Family getFamily(int id) {
        QueryBuilder query = selectAll().from("families").where("id=?", id).inOrg();

        try ( Connection conn = getConnection();
              PreparedStatement stmt = query.prepareStatement(conn)
            ) {

            List<Family> results = processFamilyResults(stmt);
            return results.isEmpty()? null: results.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Could not get family by id: " + id, e);
        }
    }

    public void create(Family family) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO families(surname, home_phone, envelope_number, addr_street1, addr_street2, addr_city, addr_state, addr_zip, org_id) values (?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
        ){
            Address addr = family.getAddress();

            stmt.setString(1, family.getSurname());
            stmt.setString(2, family.getHomePhone());
            stmt.setInt(3, family.getEnvelopeNumber());
            stmt.setString(4, addr.getStreet1());
            stmt.setString(5, addr.getStreet2());
            stmt.setString(6, addr.getCity());
            stmt.setString(7, addr.getState());
            stmt.setInt(8, addr.getZip());
            stmt.setInt(9, OrganizationContext.orgId());

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
              PreparedStatement stmt = conn.prepareStatement("UPDATE families SET surname=?, home_phone=?, envelope_number=?, addr_street1=?, addr_street2=?, addr_city=?, addr_state=?, addr_zip=?, inactive=?, org_id=? WHERE id=?")
            ){

            Address addr = family.getAddress();

            stmt.setString(1, family.getSurname());
            stmt.setString(2, family.getHomePhone());
            stmt.setInt(3, family.getEnvelopeNumber());
            stmt.setString(4, addr.getStreet1());
            stmt.setString(5, addr.getStreet2());
            stmt.setString(6, addr.getCity());
            stmt.setString(7, addr.getState());
            stmt.setInt(8, addr.getZip());
            stmt.setBoolean(9, family.isInactive());
            stmt.setInt(10, OrganizationContext.orgId());
            stmt.setInt(11, family.getId());

            if(stmt.executeUpdate() == 0)
                throw new RuntimeException("Could not update family: " + family.getSurname());

        } catch (SQLException e) {
            throw new RuntimeException("Could not update family: " + family.getSurname(), e);
        }
    }

    public boolean delete(Family family) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("DELETE FROM families WHERE id=? AND org_id=?")
            ){

            stmt.setInt(1, family.getId());
            stmt.setInt(2, OrganizationContext.orgId());

            return stmt.executeUpdate() != 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete family: " + family.getSurname(), e);
        }
    }

    public boolean deactivate(Family family) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("UPDATE families SET inactive=? WHERE id=? AND org_id=?")
        ){

            stmt.setBoolean(1, true);
            stmt.setInt(2, family.getId());
            stmt.setInt(3, OrganizationContext.orgId());

            return stmt.executeUpdate() != 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not deactivate family: " + family.getSurname(), e);
        }
    }

    public void attchPhoto(int id, String guid) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("UPDATE families SET photo_guid=? WHERE id=? AND org_id=?");
        ){
            stmt.setString(1, guid);
            stmt.setInt(2, id);
            stmt.setInt(3, OrganizationContext.orgId());

            if(stmt.executeUpdate() == 0)
                throw new NotFoundException("Could not attach photo to family: " + id);

        } catch (SQLException e) {
            throw new RuntimeException("Could not attach photo to family: " + id, e);
        }
    }

    // ----- Private ------
    private List<Family> processFamilyResults(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()){
            List<Family> families = new ArrayList<>();
            while(rs.next()) {
                Family family = new Family(rs.getInt("id"), rs.getString("surname"));
                family.setHomePhone(rs.getString("home_phone"));
                family.setEnvelopeNumber(rs.getInt("envelope_number"));
                Address addr = new Address(rs.getString("addr_street1"),
                                           rs.getString("addr_street2"),
                                           rs.getString("addr_city"),
                                           rs.getString("addr_state"),
                                           rs.getInt("addr_zip"));
                family.setAddress(addr);
                family.setPhotoGuid(rs.getString("photo_guid"));
                family.setInactive(rs.getBoolean("inactive"));
                families.add(family);
            }
            return families;
        }
    }
}

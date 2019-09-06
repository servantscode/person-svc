package org.servantscode.person.db;

import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.db.EasyDB;
import org.servantscode.commons.db.ReportStreamingOutput;
import org.servantscode.commons.search.InsertBuilder;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.SearchParser;
import org.servantscode.commons.search.UpdateBuilder;
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

public class FamilyDB extends EasyDB<Family> {

    static HashMap<String, String> FIELD_MAP = new HashMap<>(8);

    static {
        FIELD_MAP.put("envelopeNumber","envelope_number");
        FIELD_MAP.put("address.street1","addr_street1");
        FIELD_MAP.put("address.city","addr_city");
        FIELD_MAP.put("address.state","addr_state");
        FIELD_MAP.put("address.zip","addr_zip");
    }

    public FamilyDB() {
        super(Family.class, "surname", FIELD_MAP);
    }

    public int getCount(String search, boolean includeInactive) {
        QueryBuilder query = count().from("families").search(searchParser.parse(search)).inOrg();
        if(!includeInactive)
            query.where("inactive=false");

        return getCount(query);
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
        query.page(sortField, start, count);
        return get(query);
    }

    public Family getFamily(int id) {
        QueryBuilder query = selectAll().from("families").where("id=?", id).inOrg();
        return getOne(query);
    }

    public int getNextUnusedEnvelopeNumber() {
        QueryBuilder query = select("MAX(envelope_number)+1 AS next_envelope").from("families").inOrg();

        try(Connection conn = getConnection();
            PreparedStatement stmt = query.prepareStatement(conn);
            ResultSet rs = stmt.executeQuery()) {
            return rs.next()? rs.getInt("next_envelope"): 1;
        } catch (SQLException e) {
            throw new RuntimeException("Could not get next unused envelope number.", e);
        }
    }

    public void create(Family family) {
        InsertBuilder cmd = insertInto("families")
                .value("surname", family.getSurname())
                .value("home_phone", family.getHomePhone())
                .value("envelope_number", family.getEnvelopeNumber())
                .value("inactive", family.isInactive())
                .value("org_id", OrganizationContext.orgId());

        Address addr = family.getAddress();
        if (addr != null) {
            cmd.value("addr_street1", addr.getStreet1())
                    .value("addr_street2", addr.getStreet2())
                    .value("addr_city", addr.getCity())
                    .value("addr_state", addr.getState())
                    .value("addr_zip", addr.getZip());
        }
        family.setId(createAndReturnKey(cmd));
    }

    public void update(Family family) {
        Address addr = family.getAddress();
        UpdateBuilder cmd = update("families")
                .value("surname", family.getSurname())
                .value("home_phone", family.getHomePhone())
                .value("envelope_number", family.getEnvelopeNumber())
                .value("addr_street1", addr != null? addr.getStreet1(): null)
                .value("addr_street2", addr != null? addr.getStreet2(): null)
                .value("addr_city", addr != null? addr.getCity(): null)
                .value("addr_state", addr != null? addr.getState(): null)
                .value("addr_zip", addr != null? addr.getZip(): null)
                .value("inactive", family.isInactive())
                .withId(family.getId()).inOrg();

        if (!update(cmd))
            throw new RuntimeException("Could not update family: " + family.getSurname());
    }

    public boolean delete(Family family) {
        return delete(deleteFrom("families").withId(family.getId()).inOrg());
    }

    public boolean deactivate(Family family) {
        UpdateBuilder cmd = update("families")
                .value("inactive", true)
                .withId(family.getId()).inOrg();
        return update(cmd);
    }

    public void attchPhoto(int id, String guid) {
        UpdateBuilder cmd = update("families")
                .value("photo_guid", guid)
                .withId(id).inOrg();
        if(!update(cmd))
            throw new NotFoundException("Could not attach photo to family: " + id);
    }

    // ----- Private ------
    @Override
    protected Family processRow(ResultSet rs) throws SQLException {
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
        return family;
    }
}

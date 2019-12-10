package org.servantscode.person.db;

import org.servantscode.commons.Address;
import org.servantscode.commons.db.EasyDB;
import org.servantscode.commons.db.ReportStreamingOutput;
import org.servantscode.commons.search.InsertBuilder;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.UpdateBuilder;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.person.Family;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
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

    private QueryBuilder all() {
        return select("f.*", "h.name AS head_name", "s.name AS spouse_name")
                .select("CASE WHEN h.id is null THEN concat(f.surname, ' family') " +
                        "WHEN s.id is null THEN trim(concat(h.salutation, ' ', h.name)) " +
                        "WHEN regexp_replace(h.name, '^.* ', '') <> regexp_replace(s.name, '^.* ', '') THEN " +
//                            "concat(h.salutation, ' ', regexp_replace(h.name, '^.* ', ''), ' and ', s.salutation, ' ', regexp_replace(s.name, '^.* ', '')) " + //Just the last names
                            "concat(trim(concat(h.salutation, ' ', h.name)), ' and ', trim(concat(s.salutation, ' ', s.name))) " +
                        "WHEN h.salutation IS NULL OR s.salutation IS NULL THEN concat(h.name, ' family') " +
                        "ELSE concat(h.salutation, ' and ', s.salutation, ' ', h.name) END AS formal_greeting");
    }

    private QueryBuilder select(QueryBuilder select, boolean includeInactive) {
        QueryBuilder query = select.from("families f").
                leftJoin("people h ON h.family_id=f.id AND h.head_of_house=true" + (includeInactive? "": " AND h.inactive=false")).
                leftJoin("relationships r ON r.other_id=h.id AND r.relationship='SPOUSE'").
                leftJoin("people s ON s.id=r.subject_id" + (includeInactive? "": " AND s.inactive=false")).inOrg("f.org_id");

        if(!includeInactive)
            query.where("f.inactive=false");

        return query;
    }

    public int getCount(String search, boolean includeInactive) {
        QueryBuilder query = select(count(), includeInactive).search(searchParser.parse(search));
        return getCount(query);
    }

    public StreamingOutput getReportReader(String search, boolean includeInactive, final List<String> fields) {
        final QueryBuilder query = select(all(), includeInactive).search(searchParser.parse(search));

        return new ReportStreamingOutput(fields) {
            @Override
            public void write(OutputStream output) throws WebApplicationException {
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
        QueryBuilder query = select(all(), includeInactive).search(searchParser.parse(search)).
                page(sortField, start, count);
        return get(query);
    }

    public List<Family> getPossibleMatches(Family family) {
        QueryBuilder query = select(all(), false).with("surname", family.getSurname());
        if(family.getAddress() != null)
            query.or()
                 .with("addr_street1", family.getAddress().getStreet1())
                 .with("addr_street2", family.getAddress().getStreet2())
                 .with("addr_city", family.getAddress().getCity());
        return get(query);
    }

    public Family getFamily(int id) {
        QueryBuilder query = select(all(), true).with("f.id", id);
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
                .value("inactive_since", convert(family.getInactiveSince()))
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
                .value("inactive_since", convert(family.getInactiveSince()))
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
                .value("inactive_since", convert(LocalDate.now()))
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
        family.setInactiveSince(convert(rs.getDate("inactive_since")));
        family.setHeadName(rs.getString("head_name"));
        family.setSpouseName(rs.getString("spouse_name"));
        family.setFormalGreeting(rs.getString("formal_greeting"));
        return family;
    }
}

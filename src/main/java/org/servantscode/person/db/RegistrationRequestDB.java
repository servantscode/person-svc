package org.servantscode.person.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.servantscode.commons.ObjectMapperFactory;
import org.servantscode.commons.db.EasyDB;
import org.servantscode.commons.search.InsertBuilder;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.UpdateBuilder;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.person.Family;
import org.servantscode.person.RegistrationRequest;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class RegistrationRequestDB extends EasyDB<RegistrationRequest> {

    private static final ObjectMapper JSON_MAPPER = ObjectMapperFactory.getMapper();

    public RegistrationRequestDB() {
        super(RegistrationRequest.class, "family_name");
    }

    private QueryBuilder baseQuery(QueryBuilder selectQuery, boolean includeCopmleted) {
        QueryBuilder query = selectQuery.from("registration_requests r").leftJoin("people p ON r.approver_id=p.id").inOrg("r.org_id");
        if(!includeCopmleted)
            query = query.where("approval_status <> 'APPLIED' AND approval_status <> 'REJECTED'");
        return query;
    }


    public int getCount(String search, boolean includeCopmleted) {
        QueryBuilder query = baseQuery(count(), includeCopmleted).search(searchParser.parse(search));
        return getCount(query);
    }

    public List<RegistrationRequest> getRegistrationRequests(String search, String sortField, int start, int count, boolean includeCopmleted) {
        return get(baseQuery(select("r.*", "p.name AS approver_name"), includeCopmleted).search(searchParser.parse(search)).page(sortField, start, count));
    }

    public RegistrationRequest getRegistrationRequest(int id) {
        return getOne(baseQuery(select("r.*", "p.name AS approver_name"), true).with("r.id", id));
    }

    public RegistrationRequest create(RegistrationRequest req) {
        try {
            InsertBuilder cmd = insertInto("registration_requests")
                .value("request_time", req.getRequestTime())
                .value("family_name", req.getFamilyName())
                .value("family_data", JSON_MAPPER.writeValueAsString(req.getFamilyData()))
                .value("approver_id", req.getApproverId())
                .value("approval_time", req.getApprovalTime())
                .value("approval_status", req.getApprovalStatus())
                .value("org_id", OrganizationContext.orgId());
            req.setId(createAndReturnKey(cmd));
            return req;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not serialize family from registration request.", e);
        }
    }

    public void update(RegistrationRequest req) {
        try {
            UpdateBuilder cmd = update("registration_requests")
                .value("request_time", req.getRequestTime())
                .value("family_name", req.getFamilyName())
                .value("family_data", JSON_MAPPER.writeValueAsString(req.getFamilyData()))
                .value("approver_id", req.getApproverId())
                .value("approval_time", req.getApprovalTime())
                .value("approval_status", req.getApprovalStatus())
                .withId(req.getId())
                .inOrg();
            if(!update(cmd))
                throw new RuntimeException("Could not update registration request for family: " + req.getFamilyName());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not serialize family from registration request.", e);
        }
    }

    public boolean delete(RegistrationRequest req) {
        return delete(deleteFrom("registration_requests").withId(req.getId()).inOrg());
    }

    // ----- Private ----
    @Override
    protected RegistrationRequest processRow(ResultSet rs) throws SQLException {
        RegistrationRequest req = new RegistrationRequest();
        req.setId(rs.getInt("id"));
        req.setApprovalStatus(parse(RegistrationRequest.ApprovalStatus.class, rs.getString("approval_status")));
        req.setApprovalTime(convert(rs.getTimestamp("approval_time")));
        req.setApproverId(rs.getInt("approver_id"));
        req.setApproverName(rs.getString("approver_name"));
        req.setRequestTime(convert(rs.getTimestamp("request_time")));
        req.setFamilyName(rs.getString("family_name"));
        try {
            req.setFamilyData(JSON_MAPPER.readValue(rs.getString("family_data"), Family.class));
        } catch (IOException e) {
            //Beware failures when the family structure changes.
            throw new RuntimeException("Unreadable family registration request encountered!", e);
        }
        return req;
    }
}

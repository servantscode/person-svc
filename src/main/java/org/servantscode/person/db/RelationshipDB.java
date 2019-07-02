package org.servantscode.person.db;

import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.person.Relationship;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public class RelationshipDB extends DBAccess {
    public List<Relationship> getRelationships(int personId) {
        QueryBuilder query = select("r.*", "p1.name AS subject", "p2.name AS other").from("relationships r")
                .join("LEFT JOIN people p1 ON r.subject_id=p1.id")
                .join("LEFT JOIN people p2 ON r.other_id=p2.id")
                .where("subject_id=?", personId);
        try(Connection conn = getConnection();
            PreparedStatement stmt = query.prepareStatement(conn)) {

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrive relationship information for: " + personId, e);
        }
    }

    public void upsertRelationship(Relationship r) {
        String sql = "INSERT INTO relationships (subject_id, other_id, relationship, guardian, contact_preference) VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT ON CONSTRAINT relationships_pkey DO UPDATE SET relationship=EXCLUDED.relationship, guardian=EXCLUDED.guardian, contact_preference=EXCLUDED.contact_preference";
        try(Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, r.getPersonId());
            stmt.setInt(2, r.getOtherId());
            stmt.setString(3, stringify(r.getRelationship()));
            stmt.setBoolean(4, r.isGuardian());
            stmt.setInt(5, r.isDoNotContact()? -1: r.getContactPreference());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not create relationship.", e);
        }
    }

   public void deleteRelationship(Relationship r) {
        String sql = "DELETE FROM relationships WHERE subject_id=? AND other_id=?";
        try(Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, r.getPersonId());
            stmt.setInt(2, r.getOtherId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not create relationship.", e);
        }
    }

    // ----- Private -----
   private List<Relationship> processResults(PreparedStatement stmt) throws SQLException {
       List<Relationship> results = new LinkedList<>();
       try(ResultSet rs = stmt.executeQuery()) {
           while(rs.next()) {
               Relationship r = new Relationship();
               r.setPersonId(rs.getInt("subject_id"));
               r.setPersonName(rs.getString("subject"));
               r.setOtherId(rs.getInt("other_id"));
               r.setOtherName(rs.getString("other"));
               r.setRelationship(parse(Relationship.RelationshipType.class, rs.getString("relationship")));
               r.setContactPreference(rs.getInt("contact_preference"));
               r.setDoNotContact(r.getContactPreference() < 0);
               r.setGuardian(rs.getBoolean("guardian"));
               results.add(r);
           }
       }

       return results;
   }
}

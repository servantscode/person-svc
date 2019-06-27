package org.servantscode.person.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.SearchParser;
import org.servantscode.person.Preference;
import org.servantscode.person.Preference.ObjectType;
import org.servantscode.person.Preference.PreferenceType;

import java.sql.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PreferenceDB extends DBAccess {
    private static final Logger LOG = LogManager.getLogger(PreferenceDB.class);

    private SearchParser<Preference> searchParser;
    static HashMap<String, String> FIELD_MAP = new HashMap<>(8);

    static {
        FIELD_MAP.put("objectType", "object_type");
    }

    public PreferenceDB() {
        this.searchParser = new SearchParser<>(Preference.class, "name", FIELD_MAP);
    }

    public int getCount(String search) {
        QueryBuilder query = count().from("preferences").search(searchParser.parse(search));

        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve preferences count '" + search + "'", e);
        }
        return 0;
    }

    public List<Preference> getPreferences(String search, String sortField, int start, int count) {
        QueryBuilder query = selectAll().from("preferences").search(searchParser.parse(search));
        query.sort(FIELD_MAP.getOrDefault(sortField, sortField)).limit(count).offset(start);

        try ( Connection conn = getConnection();
              PreparedStatement stmt = query.prepareStatement(conn)
        ) {

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve preferences containing '" + search + "'", e);
        }
    }

    public Preference getPreference(int id) {
        QueryBuilder query = selectAll().from("preferences").withId(id);

        try(Connection conn = getConnection();
            PreparedStatement stmt = query.prepareStatement(conn)) {

            return firstOrNull(processResults(stmt));
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve preference with id: " + id , e);
        }
    }
    public void create(Preference preference) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO preferences" +
                     "(name, object_type, type, default_value) " +
                     "values (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
        ){
            stmt.setString(1, preference.getName());
            stmt.setString(2, stringify(preference.getObjectType()));
            stmt.setString(3, stringify(preference.getType()));
            stmt.setString(4, preference.getDefaultValue());

            if(stmt.executeUpdate() == 0) {
                throw new RuntimeException("Could not create preference: " + preference.getName());
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next())
                    preference.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
                throw new RuntimeException("Could not add preference: " + preference.getName(), e);
            }
        }

    public void update(Preference preference) {
        try ( Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement("UPDATE preferences " +
                    "SET name=?, object_type=?, type=?, default_value=? " +
                    "WHERE id=?")
            ){

        stmt.setString(1, preference.getName());
        stmt.setString(2, stringify(preference.getObjectType()));
        stmt.setString(3, stringify(preference.getType()));
        stmt.setString(4, preference.getDefaultValue());
        stmt.setInt(5, preference.getId());

        if(stmt.executeUpdate() == 0)
            throw new RuntimeException("Could not update preference: " + preference.getName());

        } catch (SQLException e) {
            throw new RuntimeException("Could not update preference: " + preference.getName(), e);
        }
    }


    public boolean delete(Preference preference) {
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement("DELETE FROM preferences WHERE id=?")
            ){

            stmt.setInt(1, preference.getId());

            return stmt.executeUpdate() != 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete preference: " + preference.getName(), e);
        }
    }

    public Map<String, String> getPersonalPreferences(int personId) {
        QueryBuilder query = select("p.name", "pp.value").from("preferences p")
                .join("LEFT JOIN person_preferences pp ON p.id=pp.preference_id")
                .where("p.object_type='PERSON'").where("pp.person_id=?", personId);
        return getPreferencesFromQuery(query);
    }

    public void updatePersonalPreferences(int personId, Map<String, String> prefs) {
        String sql = "INSERT INTO person_preferences (preference_id, person_id, value) (SELECT id ,? ,? FROM preferences WHERE name=? AND object_type='PERSON') " +
                     "ON CONFLICT ON CONSTRAINT person_preferences_pkey DO UPDATE SET value=EXCLUDED.value";
        try(Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, personId);
            for(Map.Entry<String, String> entry: prefs.entrySet()) {
                stmt.setString(2, entry.getValue());
                stmt.setString(3, entry.getKey());
                stmt.executeUpdate();
            };
        } catch (SQLException e) {
            throw new RuntimeException("Could not store preferences for person: " + personId, e);
        }
    }

    public Map<String, String> getFamilialPreferences(int familyId) {
        QueryBuilder query = select("p.name", "fp.value").from("preferences p")
                .join("LEFT JOIN family_preferences fp ON p.id=fp.preference_id")
                .where("p.object_type='FAMILY'").where("fp.family_id=?", familyId);
        return getPreferencesFromQuery(query);
    }

    public void updateFamilialPreferences(int familyId, Map<String, String> prefs) {
        String sql = "INSERT INTO family_preferences (preference_id, family_id, value) (SELECT id ,? ,? FROM preferences WHERE name=? AND object_type='FAMILY') " +
                "ON CONFLICT ON CONSTRAINT family_preferences_pkey DO UPDATE SET value=EXCLUDED.value";
        try(Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, familyId);
            for(Map.Entry<String, String> entry: prefs.entrySet()) {
                stmt.setString(2, entry.getValue());
                stmt.setString(3, entry.getKey());
                stmt.executeUpdate();
            };
        } catch (SQLException e) {
            throw new RuntimeException("Could not store preferences for family: " + familyId, e);
        }
    }

    // ----- Private -----
    private List<Preference> processResults(PreparedStatement stmt) throws SQLException {
        try(ResultSet rs = stmt.executeQuery()) {
            List<Preference> preferences = new LinkedList<>();
            while(rs.next()) {
                Preference pref = new Preference();
                pref.setId(rs.getInt("id"));
                pref.setName(rs.getString("name"));
                pref.setObjectType(ObjectType.valueOf(rs.getString("object_type")));
                pref.setType(PreferenceType.valueOf(rs.getString("type")));
                pref.setDefaultValue(rs.getString("default_value"));
                preferences.add(pref);
            }
            return preferences;
        }
    }

    private Map<String, String> getPreferencesFromQuery(QueryBuilder query) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery()){

            HashMap<String, String> preferences = new HashMap<>(16);
            while(rs.next())
                preferences.put(rs.getString("name"), rs.getString("value"));

            return preferences;
        } catch (SQLException e) {
            throw new RuntimeException("Could not get preferences.", e);
        }
    }
}


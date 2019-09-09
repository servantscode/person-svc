package org.servantscode.person.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.db.AbstractDBUpgrade;

import java.sql.SQLException;

public class DBUpgrade extends AbstractDBUpgrade {
    private static final Logger LOG = LogManager.getLogger(DBUpgrade.class);

    @Override
    public void doUpgrade() throws SQLException {
        LOG.info("Verifying database structures.");

        if(!tableExists("families")) {
            LOG.info("-- Creating families table");
            runSql("CREATE TABLE families (id SERIAL PRIMARY KEY, " +
                                          "surname TEXT, " +
                                          "home_phone TEXT, " +
                                          "envelope_number INTEGER, " +
                                          "addr_street1 TEXT, " +
                                          "addr_street2 TEXT, " +
                                          "addr_city TEXT, " +
                                          "addr_state TEXT, " +
                                          "addr_zip INTEGER, " +
                                          "photo_guid TEXT, " +
                                          "inactive boolean DEFAULT false, " +
                                          "org_id INTEGER references organizations(id) ON DELETE CASCADE)");
        }

        if(!tableExists("people")) {
            LOG.info("-- Creating people table");
            runSql("CREATE TABLE people (id SERIAL PRIMARY KEY, " +
                                        "name TEXT, " +
                                        "birthdate DATE, " +
                                        "email TEXT, " +
                                        "male BOOLEAN, " +
                                        "phonenumber TEXT, " +
                                        "head_of_house boolean, " +
                                        "family_id INTEGER, " +
                                        "member_since DATE, " +
                                        "photo_guid TEXT, " +
                                        "inactive boolean DEFAULT false, " +
                                        "parishioner BOOLEAN, " +
                                        "baptized BOOLEAN, " +
                                        "confession BOOLEAN, " +
                                        "communion BOOLEAN, " +
                                        "confirmed BOOLEAN, " +
                                        "marital_status TEXT, " +
                                        "ethnicity TEXT, " +
                                        "primary_language TEXT, " +
                                        "religion TEXT, " +
                                        "special_needs TEXT, " +
                                        "occupation TEXT, " +
                                        "org_id INTEGER references organizations(id) ON DELETE CASCADE)");
        }

        if(!tableExists("relationships")) {
            LOG.info("-- Creating relationships table");
            runSql("CREATE TABLE relationships (subject_id INTEGER REFERENCES people(id) ON DELETE CASCADE, " +
                                               "other_id INTEGER REFERENCES people(id) ON DELETE CASCADE, " +
                                               "relationship TEXT, " +
                                               "guardian BOOLEAN, " +
                                               "contact_preference INTEGER, " +
                                               "PRIMARY KEY (subject_id, other_id))");
        }

        if(!tableExists("preferences")) {
            LOG.info("-- Creating preferences table");
            runSql("CREATE TABLE preferences (id SERIAL PRIMARY KEY, " +
                                             "name TEXT NOT NULL, " +
                                             "object_type TEXT NOT NULL, " +
                                             "type TEXT NOT NULL, " +
                                             "default_value TEXT, " +
                                             "org_id INTEGER references organizations(id) ON DELETE CASCADE)");
        }

        if(!tableExists("person_preferences")) {
            LOG.info("-- Creating person_preferences table");
            runSql("CREATE TABLE person_preferences (preference_id INTEGER REFERENCES preferences(id) ON DELETE CASCADE, " +
                                                    "person_id INTEGER REFERENCES people(id) ON DELETE CASCADE, " +
                                                    "value TEXT, " +
                                                    "PRIMARY KEY (preference_id, person_id))");
        }

        if(!tableExists("family_preferences")) {
            LOG.info("-- Creating family_preferences table");
            runSql("CREATE TABLE family_preferences (preference_id INTEGER REFERENCES preferences(id) ON DELETE CASCADE, " +
                                                    "family_id INTEGER REFERENCES families(id) ON DELETE CASCADE, " +
                                                    "value TEXT, " +
                                                    "PRIMARY KEY (preference_id, family_id))");
        }

        if(!tableExists("configuration")) {
            LOG.info("-- Creating configuration table");
            runSql("CREATE TABLE configuration (config TEXT, " +
                                               "value TEXT, " +
                                               "org_id INTEGER references organizations(id) ON DELETE CASCADE, " +
                                               "PRIMARY KEY (config, org_id))");
        }

        if(!columnExists("people", "salutation")) {
            LOG.info("--- Adding column people(salutation)");
            runSql("ALTER TABLE people ADD COLUMN salutation TEXT");
        }

        if(!columnExists("people", "suffix")) {
            LOG.info("--- Adding column people(suffix)");
            runSql("ALTER TABLE people ADD COLUMN suffix TEXT");
        }

        if(!columnExists("people", "maiden_name")) {
            LOG.info("--- Adding column people(maiden_name)");
            runSql("ALTER TABLE people ADD COLUMN maiden_name TEXT");
        }

        if(!columnExists("people", "nickname")) {
            LOG.info("--- Adding column people(nickname)");
            runSql("ALTER TABLE people ADD COLUMN nickname TEXT");
        }

        if(!tableExists("person_phone_numbers")) {
            LOG.info("-- Creating table person_phone_numbers");
            runSql("CREATE TABLE person_phone_numbers (person_id INTEGER REFERENCES people(id) ON DELETE CASCADE, " +
                                                      "number TEXT NOT NULL, " +
                                                      "type TEXT, " +
                                                      "is_primary BOOLEAN)");

            LOG.info("--- Transferring data to table person_phone_numbers");
            runSql("INSERT INTO person_phone_numbers (person_id, number, type, is_primary) SELECT id, phonenumber, 'OTHER', true FROM people WHERE phonenumber IS NOT NULL");

            LOG.info("--- Dropping column phone_number from table people");
            runSql("ALTER TABLE people DROP COLUMN phonenumber");
        }
    }
}

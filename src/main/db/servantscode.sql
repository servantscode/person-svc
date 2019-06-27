CREATE TABLE families (id SERIAL PRIMARY KEY, surname TEXT, home_phone TEXT, envelope_number INTEGER, addr_street1 TEXT, addr_street2 TEXT, addr_city TEXT, addr_state TEXT, addr_zip INTEGER, photo_guid TEXT, inactive boolean DEFAULT false);
CREATE TABLE people (id SERIAL PRIMARY KEY, name TEXT, birthdate DATE, email TEXT, male BOOLEAN, phonenumber TEXT, head_of_house boolean, family_id INTEGER, member_since DATE, photo_guid TEXT, inactive boolean DEFAULT false, parishioner BOOLEAN, baptized BOOLEAN, confession BOOLEAN, communion BOOLEAN, confirmed BOOLEAN, marital_status TEXT, ethnicity TEXT, primary_language TEXT);
CREATE TABLE ministries (id SERIAL PRIMARY KEY, name TEXT, description TEXT);
CREATE TABLE ministry_roles (id SERIAL PRIMARY KEY, ministry_id INTEGER REFERENCES ministries(id) ON DELETE CASCADE NOT NULL, name TEXT NOT NULL, contact BOOLEAN, leader BOOLEAN);
CREATE TABLE ministry_enrollments (person_id INTEGER REFERENCES people(id) ON DELETE CASCADE, ministry_id INTEGER REFERENCES ministries(id) ON DELETE CASCADE, role_id INTEGER REFERENCES ministry_roles(id) ON DELETE SET NULL);
CREATE TABLE funds (id SERIAL PRIMARY KEY, name TEXT NOT NULL);
INSERT INTO funds (name) values ('general');
CREATE TABLE pledges (id SERIAL PRIMARY KEY, family_id INTEGER REFERENCES families(id), fund_id INTEGER REFERENCES funds(id) DEFAULT 1 NOT NULL, pledge_type TEXT, pledge_date DATE, pledge_start DATE, pledge_end DATE, frequency TEXT, pledge_increment FLOAT, total_pledge FLOAT);
CREATE TABLE donations (id BIGSERIAL PRIMARY KEY, family_id INTEGER REFERENCES families(id), fund_id INTEGER REFERENCES funds(id) DEFAULT 1 NOT NULL, amount FLOAT, date DATE, type TEXT, check_number INTEGER, transaction_id bigint);
CREATE TABLE events (id SERIAL PRIMARY KEY, start_time TIMESTAMP WITH TIME ZONE, recurring_meeting_id INTEGER, end_time TIMESTAMP WITH TIME ZONE, title TEXT, description TEXT, private_event BOOLEAN, scheduler_id INTEGER REFERENCES people(id) ON DELETE SET NULL,contact_id INTEGER REFERENCES people(id) ON DELETE SET NULL, ministry_id INTEGER REFERENCES ministries(id) ON DELETE CASCADE, departments TEXT, categories TEXT);
CREATE TABLE rooms (id SERIAL PRIMARY KEY, name TEXT, type TEXT, capacity INTEGER);
CREATE TABLE equipment (id SERIAL PRIMARY KEY, name TEXT, manufacturer TEXT, description TEXT);
CREATE TABLE reservations (id BIGSERIAL PRIMARY KEY, resource_type TEXT, resource_id INTEGER, reserving_person_id INTEGER REFERENCES people(id) ON DELETE SET NULL, event_id INTEGER, start_time TIMESTAMP WITH TIME ZONE, end_time TIMESTAMP WITH TIME ZONE);
CREATE TABLE recurrences(id SERIAL PRIMARY KEY, cycle TEXT, frequency INTEGER, end_date DATE, weekly_days INTEGER, excluded_days TEXT);
CREATE TABLE roles (id SERIAL PRIMARY KEY, name TEXT);
INSERT INTO roles(name) values ('system');
CREATE TABLE permissions (role_id INTEGER REFERENCES roles(id) ON DELETE CASCADE, permission TEXT);
INSERT INTO permissions(role_id, permission) values (1, '*');
CREATE TABLE logins (person_id INTEGER PRIMARY KEY REFERENCES people(id) ON DELETE CASCADE, hashed_password TEXT, role_id INTEGER REFERENCES roles(id) ON DELETE CASCADE, reset_password BOOLEAN DEFAULT false, reset_token TEXT);
CREATE TABLE sessions (pseron_id INTEGER REFERENCES people(id), token TEXT NOT NULL, expiration TIMESTAMP WITH TIME ZONE);
CREATE TABLE photos (guid TEXT PRIMARY KEY, filetype TEXT, bytes BYTEA);
CREATE TABLE notes (id SERIAL PRIMARY KEY, creator_id INTEGER REFERENCES people(id), created_time TIMESTAMP WITH TIME ZONE, edited BOOLEAN, private BOOLEAN, resource_type TEXT, resource_id INTEGER, note TEXT);
CREATE TABLE configuration (config TEXT PRIMARY KEY, value TEXT);
CREATE TABLE baptisms (id SERIAL PRIMARY KEY, name TEXT NOT NULL, person_id INTEGER, father_name TEXT, father_id INTEGER, mother_name TEXT, mother_id INTEGER, baptism_date DATE NOT NULL, baptism_location TEXT, birth_date DATE, birth_location TEXT, minister_name TEXT NOT NULL, minister_id INTEGER, godfather_name TEXT, godfather_id INTEGER, godmother_name TEXT, godmother_id INTEGER, witness_name TEXT, witness_id INTEGER, conditional BOOLEAN DEFAULT FALSE, reception BOOLEAN DEFAULT FALSE, notations TEXT);
CREATE TABLE confirmations (id SERIAL PRIMARY KEY, name TEXT NOT NULL, person_id INTEGER, father_name TEXT, father_id INTEGER, mother_name TEXT, mother_id INTEGER, baptism_id INTEGER REFERENCES baptisms(id), baptism_date DATE NOT NULL, baptism_location TEXT, sponsor_name TEXT NOT NULL, sponsor_id INTEGER, confirmation_date DATE NOT NULL, confirmation_location TEXT NOT NULL, minister_name TEXT NOT NULL, minister_id INTEGER, notations TEXT);
CREATE TABLE marriages (id SERIAL PRIMARY KEY, groom_name TEXT NOT NULL, groom_id INTEGER, groom_father_name TEXT, groom_father_id INTEGER, groom_mother_name TEXT, groom_mother_id INTEGER, groom_baptism_id INTEGER REFERENCES baptisms(id), groom_baptism_date DATE, groom_baptism_location TEXT, bride_name TEXT NOT NULL, bride_id INTEGER, bride_father_name TEXT, bride_father_id INTEGER, bride_mother_name TEXT, bride_mother_id INTEGER, bride_baptism_id INTEGER REFERENCES baptisms(id), bride_baptism_date DATE, bride_baptism_location TEXT, wedding_date DATE NOT NULL, wedding_location TEXT, minister_name TEXT NOT NULL, minister_id INTEGER, witness_1_name TEXT NOT NULL, witness_1_id INTEGER, witness_2_name TEXT NOT NULL, witness_2_id INTEGER, notations TEXT);
CREATE TABLE mass_intentions (id SERIAL PRIMARY KEY, eventId INTEGER REFERENCES events(id) NOT NULL, personName TEXT, personId INTEGER REFERENCES people(id), intentionType TEXT, requesterName TEXT, requesterId INTEGER REFERENCES people(id), requesterPhone TEXT);
CREATE TABLE preferences (id SERIAL PRIMARY KEY, name TEXT NOT NULL,  object_type TEXT NOT NULL,type TEXT NOT NULL, default_value TEXT);
CREATE TABLE person_preferences (preference_id INTEGER REFERENCES preferences(id) ON DELETE CASCADE, person_id INTEGER REFERENCES people(id) ON DELETE CASCADE, value TEXT, PRIMARY KEY (preference_id, person_id));
CREATE TABLE family_preferences (preference_id INTEGER REFERENCES preferences(id) ON DELETE CASCADE, family_id INTEGER REFERENCES families(id) ON DELETE CASCADE, value TEXT, PRIMARY KEY (preference_id, family_id));

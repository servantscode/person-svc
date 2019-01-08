-- TODO: Make all this readable by the average dev

CREATE TABLE families (id SERIAL PRIMARY KEY, surname TEXT, addr_street1 TEXT, addr_street2 TEXT, addr_city TEXT, addr_state TEXT, addr_zip INTEGER);
CREATE TABLE people (id SERIAL PRIMARY KEY, name TEXT, birthdate date, email TEXT, phonenumber TEXT, head_of_house boolean, family_id INTEGER, member_since date);
CREATE TABLE logins (person_id INTEGER PRIMARY KEY REFERENCES people(id) ON DELETE CASCADE, hashed_password TEXT, role TEXT);
CREATE TABLE ministries (id SERIAL PRIMARY KEY, name TEXT, description TEXT);
CREATE TABLE ministry_enrollments (person_id INTEGER REFERENCES people(id) ON DELETE CASCADE, ministry_id INTEGER REFERENCES ministries(id) ON DELETE CASCADE, role TEXT);
CREATE TABLE pledges (id SERIAL, family_id INTEGER REFERENCES families(id), pledge_type TEXT, envelope_number INTEGER, pledge_date TIMESTAMP, pledge_start TIMESTAMP, pledge_end TIMESTAMP, frequency TEXT, pledge_increment FLOAT, total_pledge FLOAT);
CREATE TABLE donations (id BIGSERIAL, family_id INTEGER REFERENCES families(id), amount FLOAT, date TIMESTAMP, type TEXT, check_number INTEGER, transaction_id bigint);
CREATE TABLE events (id SERIAL, event_datetime TIMESTAMP, description TEXT, scheduler_id INTEGER REFERENCES people(id), ministry_id INTEGER REFERENCES ministries(id));
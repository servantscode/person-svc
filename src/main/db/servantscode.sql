-- TODO: Make all this readable by the average dev

CREATE TABLE families (id SERIAL PRIMARY KEY, surname TEXT, addr_street1 TEXT, addr_street2 TEXT, addr_city TEXT, addr_state TEXT, addr_zip INTEGER);
CREATE TABLE people (id SERIAL PRIMARY KEY, name TEXT, birthdate TIMESTAMP WITH TIME ZONE, email TEXT, phonenumber TEXT, head_of_house boolean, family_id INTEGER, member_since TIMESTAMP WITH TIME ZONE);
CREATE TABLE logins (person_id INTEGER PRIMARY KEY REFERENCES people(id) ON DELETE CASCADE, hashed_password TEXT, role TEXT);
CREATE TABLE ministries (id SERIAL PRIMARY KEY, name TEXT, description TEXT);
CREATE TABLE ministry_enrollments (person_id INTEGER REFERENCES people(id) ON DELETE CASCADE, ministry_id INTEGER REFERENCES ministries(id) ON DELETE CASCADE, role TEXT);
CREATE TABLE pledges (id SERIAL, family_id INTEGER REFERENCES families(id), pledge_type TEXT, envelope_number INTEGER, pledge_date TIMESTAMP WITH TIME ZONE, pledge_start TIMESTAMP WITH TIME ZONE, pledge_end TIMESTAMP WITH TIME ZONE, frequency TEXT, pledge_increment FLOAT, total_pledge FLOAT);
CREATE TABLE donations (id BIGSERIAL, family_id INTEGER REFERENCES families(id), amount FLOAT, date TIMESTAMP WITH TIME ZONE, type TEXT, check_number INTEGER, transaction_id bigint);
CREATE TABLE events (id SERIAL, start_time TIMESTAMP WITH TIME ZONE, recurring_meeting_id INTEGER, end_time TIMESTAMP WITH TIME ZONE, description TEXT, scheduler_id INTEGER REFERENCES people(id), ministry_id INTEGER REFERENCES ministries(id));
CREATE TABLE rooms (id SERIAL, name TEXT, type TEXT, capacity INTEGER);
CREATE TABLE equipment (id SERIAL, name TEXT, manufacturer TEXT, description TEXT);
CREATE TABLE reservations (id BIGSERIAL, resource_type TEXT, resource_id INTEGER, reserving_person_id INTEGER, event_id INTEGER, start_time TIMESTAMP WITH TIME ZONE, end_time TIMESTAMP WITH TIME ZONE);
CREATE TABLE recurrences(id SERIAL, cycle TEXT, frequency INTEGER, end_date TIMESTAMP WITH TIME ZONE, weekly_days INTEGER);

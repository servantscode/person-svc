-- TODO: Make all this readable by the average dev

CREATE TABLE families (id SERIAL PRIMARY KEY, surname TEXT, home_phone TEXT, envelope_number INTEGER, addr_street1 TEXT, addr_street2 TEXT, addr_city TEXT, addr_state TEXT, addr_zip INTEGER, photo_guid TEXT);
CREATE TABLE people (id SERIAL PRIMARY KEY, name TEXT, birthdate TIMESTAMP WITH TIME ZONE, email TEXT, phonenumber TEXT, head_of_house boolean, family_id INTEGER, member_since TIMESTAMP WITH TIME ZONE, photo_guid TEXT);
CREATE TABLE ministries (id SERIAL PRIMARY KEY, name TEXT, description TEXT);
CREATE TABLE ministry_enrollments (person_id INTEGER REFERENCES people(id) ON DELETE CASCADE, ministry_id INTEGER REFERENCES ministries(id) ON DELETE CASCADE, role TEXT);
CREATE TABLE pledges (id SERIAL PRIMARY KEY, family_id INTEGER REFERENCES families(id), pledge_type TEXT, pledge_date TIMESTAMP WITH TIME ZONE, pledge_start TIMESTAMP WITH TIME ZONE, pledge_end TIMESTAMP WITH TIME ZONE, frequency TEXT, pledge_increment FLOAT, total_pledge FLOAT);
CREATE TABLE donations (id BIGSERIAL PRIMARY KEY, family_id INTEGER REFERENCES families(id), amount FLOAT, date TIMESTAMP WITH TIME ZONE, type TEXT, check_number INTEGER, transaction_id bigint);
CREATE TABLE events (id SERIAL PRIMARY KEY, start_time TIMESTAMP WITH TIME ZONE, recurring_meeting_id INTEGER, end_time TIMESTAMP WITH TIME ZONE, description TEXT, scheduler_id INTEGER REFERENCES people(id), ministry_id INTEGER REFERENCES ministries(id));
CREATE TABLE rooms (id SERIAL PRIMARY KEY, name TEXT, type TEXT, capacity INTEGER);
CREATE TABLE equipment (id SERIAL PRIMARY KEY, name TEXT, manufacturer TEXT, description TEXT);
CREATE TABLE reservations (id BIGSERIAL PRIMARY KEY, resource_type TEXT, resource_id INTEGER, reserving_person_id INTEGER, event_id INTEGER, start_time TIMESTAMP WITH TIME ZONE, end_time TIMESTAMP WITH TIME ZONE);
CREATE TABLE recurrences(id SERIAL PRIMARY KEY, cycle TEXT, frequency INTEGER, end_date TIMESTAMP WITH TIME ZONE, weekly_days INTEGER);
CREATE TABLE roles (id SERIAL PRIMARY KEY, name TEXT);
CREATE TABLE permissions (role_id INTEGER REFERENCES roles(id) ON DELETE CASCADE, permission TEXT);
CREATE TABLE logins (person_id INTEGER PRIMARY KEY REFERENCES people(id) ON DELETE CASCADE, hashed_password TEXT, role_id INTEGER REFERENCES roles(id) ON DELETE CASCADE);
CREATE TABLE photos (guid TEXT PRIMARY KEY, filetype TEXT, bytes BYTEA);
CREATE TABLE notes (id SERIAL PRIMARY KEY, creator_id INTEGER REFERENCES people(id), created_time TIMESTAMP WITH TIME ZONE, edited BOOLEAN, private BOOLEAN, resource_type TEXT, resource_id INTEGER, note TEXT);

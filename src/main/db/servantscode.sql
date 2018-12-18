-- TODO: Make all this readable by the average dev

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;
COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';

SET default_tablespace = '';

SET default_with_oids = false;

-- Families
CREATE TABLE public.families (
  id integer NOT NULL,
  surname text,
  addr_street1 text,
  addr_street2 text,
  addr_city text,
  addr_state text,
  addr_zip integer
);
ALTER TABLE public.families OWNER TO servant1;
CREATE SEQUENCE public.families_id_seq
  AS integer
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;
ALTER TABLE public.families_id_seq OWNER TO servant1;
ALTER SEQUENCE public.families_id_seq OWNED BY public.families.id;

-- Logins
CREATE TABLE public.logins (
  person_id integer NOT NULL,
  hashed_password text,
  role text
);
ALTER TABLE public.logins OWNER TO servant1;

-- Ministries
CREATE TABLE public.ministries (
  id integer NOT NULL,
  name text,
  description text
);
ALTER TABLE public.ministries OWNER TO servant1;
CREATE SEQUENCE public.ministries_id_seq
  AS integer
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;
ALTER TABLE public.ministries_id_seq OWNER TO servant1;
ALTER SEQUENCE public.ministries_id_seq OWNED BY public.ministries.id;

-- Enrollments
CREATE TABLE public.ministry_enrollments (
  person_id integer,
  ministry_id integer,
  role text
);
ALTER TABLE public.ministry_enrollments OWNER TO servant1;

-- People
CREATE TABLE public.people (
  id integer NOT NULL,
  name text,
  birthdate date,
  email text,
  phonenumber text,
  head_of_house boolean,
  family_id integer,
  member_since date
);
ALTER TABLE public.people OWNER TO servant1;
CREATE SEQUENCE public.people_id_seq
  AS integer
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;
ALTER TABLE public.people_id_seq OWNER TO servant1;
ALTER SEQUENCE public.people_id_seq OWNED BY public.people.id;

-- Sequences and keys
ALTER TABLE ONLY public.families ALTER COLUMN id SET DEFAULT nextval('public.families_id_seq'::regclass);
ALTER TABLE ONLY public.ministries ALTER COLUMN id SET DEFAULT nextval('public.ministries_id_seq'::regclass);
ALTER TABLE ONLY public.people ALTER COLUMN id SET DEFAULT nextval('public.people_id_seq'::regclass);
ALTER TABLE ONLY public.families
  ADD CONSTRAINT families_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.logins
  ADD CONSTRAINT logins_pkey PRIMARY KEY (person_id);
ALTER TABLE ONLY public.ministries
  ADD CONSTRAINT ministries_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.people
  ADD CONSTRAINT people_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.logins
  ADD CONSTRAINT logins_person_id_fkey FOREIGN KEY (person_id) REFERENCES public.people(id) ON DELETE CASCADE;
ALTER TABLE ONLY public.ministry_enrollments
  ADD CONSTRAINT ministry_enrollments_ministry_id_fkey FOREIGN KEY (ministry_id) REFERENCES public.ministries(id) ON DELETE CASCADE;
ALTER TABLE ONLY public.ministry_enrollments
  ADD CONSTRAINT ministry_enrollments_person_id_fkey FOREIGN KEY (person_id) REFERENCES public.people(id) ON DELETE CASCADE;

-- Newer tables
CREATE TABLE pledges (id SERIAL, family_id integer references families(id), pledge_type text, envelope_number integer,
                      pledge_date timestamp, pledge_start timestamp, pledge_end timestamp, frequency text, pledge_increment float, total_pledge float);
CREATE TABLE donations (id BIGSERIAL, family_id integer references families(id), amount float, date timestamp, type text, check_number integer, transaction_id bigint);
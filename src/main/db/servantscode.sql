--
-- PostgreSQL database dump
--

-- Dumped from database version 10.5 (Debian 10.5-1.pgdg90+1)
-- Dumped by pg_dump version 10.5 (Debian 10.5-1.pgdg90+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner:
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: families; Type: TABLE; Schema: public; Owner: servant1
--

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

--
-- Name: families_id_seq; Type: SEQUENCE; Schema: public; Owner: servant1
--

CREATE SEQUENCE public.families_id_seq
  AS integer
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;


ALTER TABLE public.families_id_seq OWNER TO servant1;

--
-- Name: families_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: servant1
--

ALTER SEQUENCE public.families_id_seq OWNED BY public.families.id;


--
-- Name: logins; Type: TABLE; Schema: public; Owner: servant1
--

CREATE TABLE public.logins (
  person_id integer NOT NULL,
  hashed_password text,
  role text
);


ALTER TABLE public.logins OWNER TO servant1;

--
-- Name: ministries; Type: TABLE; Schema: public; Owner: servant1
--

CREATE TABLE public.ministries (
  id integer NOT NULL,
  name text,
  description text
);


ALTER TABLE public.ministries OWNER TO servant1;

--
-- Name: ministries_id_seq; Type: SEQUENCE; Schema: public; Owner: servant1
--

CREATE SEQUENCE public.ministries_id_seq
  AS integer
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;


ALTER TABLE public.ministries_id_seq OWNER TO servant1;

--
-- Name: ministries_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: servant1
--

ALTER SEQUENCE public.ministries_id_seq OWNED BY public.ministries.id;


--
-- Name: ministry_enrollments; Type: TABLE; Schema: public; Owner: servant1
--

CREATE TABLE public.ministry_enrollments (
  person_id integer,
  ministry_id integer,
  role text
);


ALTER TABLE public.ministry_enrollments OWNER TO servant1;

--
-- Name: people; Type: TABLE; Schema: public; Owner: servant1
--

CREATE TABLE public.people (
  id integer NOT NULL,
  name text,
  birthdate date,
  email text,
  phonenumber text,
  head_of_house boolean,
  family_id integer
);


ALTER TABLE public.people OWNER TO servant1;

--
-- Name: people_id_seq; Type: SEQUENCE; Schema: public; Owner: servant1
--

CREATE SEQUENCE public.people_id_seq
  AS integer
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE
  CACHE 1;


ALTER TABLE public.people_id_seq OWNER TO servant1;

--
-- Name: people_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: servant1
--

ALTER SEQUENCE public.people_id_seq OWNED BY public.people.id;


--
-- Name: families id; Type: DEFAULT; Schema: public; Owner: servant1
--

ALTER TABLE ONLY public.families ALTER COLUMN id SET DEFAULT nextval('public.families_id_seq'::regclass);


--
-- Name: ministries id; Type: DEFAULT; Schema: public; Owner: servant1
--

ALTER TABLE ONLY public.ministries ALTER COLUMN id SET DEFAULT nextval('public.ministries_id_seq'::regclass);


--
-- Name: people id; Type: DEFAULT; Schema: public; Owner: servant1
--

ALTER TABLE ONLY public.people ALTER COLUMN id SET DEFAULT nextval('public.people_id_seq'::regclass);


--
-- Name: families families_pkey; Type: CONSTRAINT; Schema: public; Owner: servant1
--

ALTER TABLE ONLY public.families
  ADD CONSTRAINT families_pkey PRIMARY KEY (id);


--
-- Name: logins logins_pkey; Type: CONSTRAINT; Schema: public; Owner: servant1
--

ALTER TABLE ONLY public.logins
  ADD CONSTRAINT logins_pkey PRIMARY KEY (person_id);


--
-- Name: ministries ministries_pkey; Type: CONSTRAINT; Schema: public; Owner: servant1
--

ALTER TABLE ONLY public.ministries
  ADD CONSTRAINT ministries_pkey PRIMARY KEY (id);


--
-- Name: people people_pkey; Type: CONSTRAINT; Schema: public; Owner: servant1
--

ALTER TABLE ONLY public.people
  ADD CONSTRAINT people_pkey PRIMARY KEY (id);


--
-- Name: logins logins_person_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: servant1
--

ALTER TABLE ONLY public.logins
  ADD CONSTRAINT logins_person_id_fkey FOREIGN KEY (person_id) REFERENCES public.people(id);


--
-- Name: ministry_enrollments ministry_enrollments_ministry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: servant1
--

ALTER TABLE ONLY public.ministry_enrollments
  ADD CONSTRAINT ministry_enrollments_ministry_id_fkey FOREIGN KEY (ministry_id) REFERENCES public.ministries(id);


--
-- Name: ministry_enrollments ministry_enrollments_person_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: servant1
--

ALTER TABLE ONLY public.ministry_enrollments
  ADD CONSTRAINT ministry_enrollments_person_id_fkey FOREIGN KEY (person_id) REFERENCES public.people(id);


--
-- PostgreSQL database dump complete
--
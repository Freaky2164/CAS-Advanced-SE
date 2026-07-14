--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.10
-- Dumped by pg_dump version 9.6.10

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
-- Name: _einf_gefehler; Type: TABLE; Schema: public; Owner: rebasedata
--

CREATE TABLE public._einf_gefehler (
    "Feld0" character varying(12) DEFAULT NULL::character varying
);


ALTER TABLE public._einf_gefehler OWNER TO rebasedata;

--
-- Data for Name: _einf_gefehler; Type: TABLE DATA; Schema: public; Owner: rebasedata
--

COPY public._einf_gefehler ("Feld0") FROM stdin;
Musterhausen
\.


--
-- PostgreSQL database dump complete
--


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * Author:  Pablo Diaz Saavedra, pabloandres.diazsaavedra@gmail.com
 * Created: 27-07-2020
 */

CREATE USER user_test WITH PASSWORD 'pass_test';

CREATE DATABASE test
  WITH OWNER = user_test
       ENCODING = 'UTF8'
       TABLESPACE = pg_default
       LC_COLLATE = 'es_CL.UTF-8'
       LC_CTYPE = 'es_CL.UTF-8'
       CONNECTION LIMIT = -1;


CREATE TABLE tmp_test_type
(
    pk_test_type integer NOT NULL,
    name character varying(10) NOT NULL,
    CONSTRAINT tmp_test_type_pkey PRIMARY KEY (pk_test_type)
);
ALTER TABLE tmp_test_type OWNER TO user_test;

INSERT INTO tmp_test_type(pk_test_type, name) VALUES(1, 'ONE');
INSERT INTO tmp_test_type(pk_test_type, name) VALUES(2, 'TWO');


 CREATE TABLE tmp_test_data
 (
     pk_test_data serial NOT NULL,
     td_system_id integer NOT NULL,
     td_login_id character varying(10) NOT NULL,
     td_date timestamp without time zone NOT NULL,
     td_local_date date NOT NULL,
     td_local_date_time timestamp without time zone NOT NULL,
     td_text character varying(100) NOT NULL,
     td_status character varying(10) NOT NULL,
     td_color_id integer,
     td_color_json JSONB,
     td_data_json JSONB,
     td_list_json JSONB,
     fk_test_type integer NOT NULL,
    CONSTRAINT tmp_test_data_pkey PRIMARY KEY (pk_test_data),
     CONSTRAINT tmp_test_type_fk_test_type FOREIGN KEY (fk_test_type)
      REFERENCES tmp_test_type (pk_test_type) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE
 );
ALTER TABLE tmp_test_data OWNER TO user_test;

INSERT INTO tmp_test_data(td_system_id, td_login_id, td_date, td_local_date, td_local_date_time, td_text, td_status, td_color_id, fk_test_type) VALUES(10000, 'pd47753', now(), now(), now(), 'text1', 'SUCCESS', 1, 1);
INSERT INTO tmp_test_data(td_system_id, td_login_id, td_date, td_local_date, td_local_date_time, td_text, td_status, td_color_id, fk_test_type) VALUES(20000, 'lx43224', now(), now(), now(), 'text2', 'ERROR', 2, 2);


CREATE TABLE tmp_test_data_history
(
    pk_test_data_history serial NOT NULL,
    fk_test_data integer NOT NULL,
    info character varying(100) NOT NULL,
     CONSTRAINT tmp_test_data_history_pkey PRIMARY KEY (pk_test_data_history),
     CONSTRAINT tmp_test_data_history_fk_test_data FOREIGN KEY (fk_test_data)
      REFERENCES tmp_test_data (pk_test_data) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE
);
ALTER TABLE tmp_test_data_history OWNER TO user_test;
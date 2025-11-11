 CREATE TABLE tmp_test_data
  (
    pk_test_data serial NOT NULL,
    td_system_id integer NOT NULL,
    td_login_id VARCHAR(10) NOT NULL,
    td_date timestamp without time zone NOT NULL,
    td_local_date date NOT NULL,
    td_local_date_time timestamp without time zone NOT NULL,
    td_text VARCHAR(100) NOT NULL,
    td_status VARCHAR(10) NOT NULL,
    td_color_id integer,
    td_color_json JSONB,
    td_data_json JSONB,
    td_list_json JSONB,
    fk_test_type integer NOT NULL
  );

  -- Primary key
  ALTER TABLE tmp_test_data ADD CONSTRAINT PK_TEST PRIMARY KEY (pk_test_data);


  CREATE TABLE tmp_test_data_empty
  (
  pk_test_data_empty integer NOT NULL
 );


  CREATE TABLE tmp_test_data_history
   (
   pk_test_data_history serial NOT NULL,
   fk_test_data integer NOT NULL,
   info character varying(100) NOT NULL,
   CONSTRAINT tmp_test_data_history_pkey PRIMARY KEY (pk_test_data_history),
   CONSTRAINT tmp_test_data_history_fk_test_data FOREIGN KEY (fk_test_data)
   REFERENCES tmp_test_data (pk_test_data) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE CASCADE
   );


CREATE TABLE tmp_test_type(
     pk_test_type integer NOT NULL,
     name character varying(10) NOT NULL,
     CONSTRAINT tmp_test_type_pkey PRIMARY KEY (pk_test_type)
);

ALTER TABLE tmp_test_type OWNER TO user_test;

INSERT INTO tmp_test_type(pk_test_type, name) VALUES(1, 'ONE');
INSERT INTO tmp_test_type(pk_test_type, name) VALUES(2, 'TWO');
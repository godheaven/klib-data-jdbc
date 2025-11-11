-- H2-compatible schema for tests
CREATE TABLE tmp_test_data (
  pk_test_data INTEGER AUTO_INCREMENT PRIMARY KEY,
  td_system_id INTEGER NOT NULL,
  td_login_id VARCHAR(10) NOT NULL,
  td_date TIMESTAMP NOT NULL,
  td_local_date DATE NOT NULL,
  td_local_date_time TIMESTAMP NOT NULL,
  td_text VARCHAR(100) NOT NULL,
  td_status VARCHAR(10) NOT NULL,
  td_color_id INTEGER,
  td_color_json CLOB,
  td_data_json CLOB,
  td_list_json CLOB,
  fk_test_type INTEGER NOT NULL
);

CREATE TABLE tmp_test_data_empty (
  pk_test_data_empty INTEGER NOT NULL
);

CREATE TABLE tmp_test_data_history (
  pk_test_data_history INTEGER AUTO_INCREMENT PRIMARY KEY,
  fk_test_data INTEGER NOT NULL,
  info VARCHAR(100) NOT NULL,
  CONSTRAINT tmp_test_data_history_fk_test_data FOREIGN KEY (fk_test_data)
    REFERENCES tmp_test_data (pk_test_data) ON DELETE CASCADE
);

CREATE TABLE tmp_test_type (
  pk_test_type INTEGER NOT NULL PRIMARY KEY,
  name VARCHAR(10) NOT NULL
);

INSERT INTO tmp_test_type(pk_test_type, name) VALUES(1, 'ONE');
INSERT INTO tmp_test_type(pk_test_type, name) VALUES(2, 'TWO');


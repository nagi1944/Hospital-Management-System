-- =====================================================================
--  Hospital Management System  -  ORACLE schema + sample data
--  Works on Oracle 11g / 12c / 18c / 19c / 21c (Express Edition is fine).
--
--  HOW TO RUN: connect in SQL Developer as the same user that is in db.properties
--  (SYSTEM), open this file and press "Run Script" (F5). Run it ONCE.
--  It drops and re-creates the 10 hospital tables in that user's schema.
-- =====================================================================

SET DEFINE OFF;
SET SQLBLANKLINES ON;

-- ---- 1. remove old copies (safe to run when they do not exist) ----
BEGIN
  FOR t IN (SELECT table_name FROM user_tables WHERE table_name IN
            ('MESSAGES','LAB_TESTS','MEDICINES','BILLS','PRESCRIPTIONS','MEDICAL_RECORDS',
             'APPOINTMENTS','USERS','PATIENTS','DOCTORS')) LOOP
    EXECUTE IMMEDIATE 'DROP TABLE ' || t.table_name || ' CASCADE CONSTRAINTS PURGE';
  END LOOP;
  FOR s IN (SELECT sequence_name FROM user_sequences WHERE sequence_name LIKE 'HMS\_SEQ\_%' ESCAPE '\') LOOP
    EXECUTE IMMEDIATE 'DROP SEQUENCE ' || s.sequence_name;
  END LOOP;
END;
/

-- ---- 2. tables ----
CREATE TABLE doctors (
  id             NUMBER(10) PRIMARY KEY,
  name           VARCHAR2(100) NOT NULL,
  specialization VARCHAR2(100) NOT NULL,
  qualification  VARCHAR2(100),
  phone          VARCHAR2(20)  NOT NULL,
  email          VARCHAR2(100),
  experience     NUMBER(3),
  fee            NUMBER(10,2) DEFAULT 0 NOT NULL,
  available_days VARCHAR2(100)
);

CREATE TABLE patients (
  id            NUMBER(10) PRIMARY KEY,
  name          VARCHAR2(100) NOT NULL,
  age           NUMBER(3) NOT NULL,
  gender        VARCHAR2(10) NOT NULL,
  phone         VARCHAR2(20) NOT NULL,
  email         VARCHAR2(100),
  blood_group   VARCHAR2(5),
  address       VARCHAR2(255),
  registered_on DATE NOT NULL
);

CREATE TABLE users (
  id            NUMBER(10) PRIMARY KEY,
  username      VARCHAR2(50) NOT NULL UNIQUE,
  password_hash CHAR(64) NOT NULL,
  full_name     VARCHAR2(100) NOT NULL,
  email         VARCHAR2(100) NOT NULL,
  role          VARCHAR2(20) NOT NULL,
  doctor_id     NUMBER(10) REFERENCES doctors(id) ON DELETE SET NULL
);

CREATE TABLE appointments (
  id         NUMBER(10) PRIMARY KEY,
  patient_id NUMBER(10) NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  doctor_id  NUMBER(10) NOT NULL REFERENCES doctors(id)  ON DELETE CASCADE,
  appt_date  DATE NOT NULL,
  appt_time  VARCHAR2(8) NOT NULL,            -- stored as HH:MM text (Oracle has no TIME type)
  reason     VARCHAR2(255),
  status     VARCHAR2(20) DEFAULT 'Scheduled' NOT NULL
);

CREATE TABLE medical_records (
  id         NUMBER(10) PRIMARY KEY,
  patient_id NUMBER(10) NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  doctor_id  NUMBER(10) NOT NULL REFERENCES doctors(id)  ON DELETE CASCADE,
  visit_date DATE NOT NULL,
  diagnosis  VARCHAR2(255) NOT NULL,
  symptoms   VARCHAR2(4000) NOT NULL,
  treatment  VARCHAR2(4000) NOT NULL,
  notes      VARCHAR2(4000)
);

CREATE TABLE prescriptions (
  id           NUMBER(10) PRIMARY KEY,
  patient_id   NUMBER(10) NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  doctor_id    NUMBER(10) NOT NULL REFERENCES doctors(id)  ON DELETE CASCADE,
  presc_date   DATE NOT NULL,
  medicines    VARCHAR2(4000) NOT NULL,
  instructions VARCHAR2(4000)
);

CREATE TABLE bills (
  id           NUMBER(10) PRIMARY KEY,
  patient_id   NUMBER(10) NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  bill_date    DATE NOT NULL,
  consultation NUMBER(10,2) DEFAULT 0,
  medicine     NUMBER(10,2) DEFAULT 0,
  lab          NUMBER(10,2) DEFAULT 0,
  other        NUMBER(10,2) DEFAULT 0,
  total        NUMBER(10,2) DEFAULT 0 NOT NULL,
  status       VARCHAR2(20) DEFAULT 'Pending' NOT NULL
);

CREATE TABLE medicines (
  id            NUMBER(10) PRIMARY KEY,
  name          VARCHAR2(100) NOT NULL,
  category      VARCHAR2(100) NOT NULL,
  quantity      NUMBER(10) NOT NULL,
  price         NUMBER(10,2) NOT NULL,
  expiry_date   DATE NOT NULL,
  reorder_level NUMBER(10) DEFAULT 10 NOT NULL
);

CREATE TABLE lab_tests (
  id         NUMBER(10) PRIMARY KEY,
  patient_id NUMBER(10) NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
  doctor_id  NUMBER(10) NOT NULL REFERENCES doctors(id)  ON DELETE CASCADE,
  test_name  VARCHAR2(100) NOT NULL,
  test_date  DATE NOT NULL,
  cost       NUMBER(10,2),
  status     VARCHAR2(20) DEFAULT 'Pending' NOT NULL,
  result     VARCHAR2(4000)
);

CREATE TABLE messages (
  id         NUMBER(10) PRIMARY KEY,
  name       VARCHAR2(100) NOT NULL,
  email      VARCHAR2(100) NOT NULL,
  subject    VARCHAR2(150) NOT NULL,
  message    VARCHAR2(4000) NOT NULL,
  created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

-- ---- 3. auto-numbering: one sequence + one trigger per table (works on every Oracle version) ----
BEGIN
  FOR t IN (SELECT column_value AS tn FROM TABLE(sys.odcivarchar2list(
            'DOCTORS','PATIENTS','USERS','APPOINTMENTS','MEDICAL_RECORDS',
            'PRESCRIPTIONS','BILLS','MEDICINES','LAB_TESTS','MESSAGES'))) LOOP
    EXECUTE IMMEDIATE 'CREATE SEQUENCE hms_seq_' || t.tn || ' START WITH 1 INCREMENT BY 1 NOCACHE';
    EXECUTE IMMEDIATE 'CREATE OR REPLACE TRIGGER hms_trg_' || t.tn
      || ' BEFORE INSERT ON ' || t.tn || ' FOR EACH ROW WHEN (NEW.id IS NULL) BEGIN :NEW.id := hms_seq_'
      || t.tn || '.NEXTVAL; END;';
  END LOOP;
END;
/

-- ---- 4. sample data ----
INSERT INTO doctors (name, specialization, qualification, phone, email, experience, fee, available_days) VALUES ('Anil Kumar', 'Cardiology', 'MBBS, MD, DM', '9000000001', 'anil@medicare.example', 15, 800, 'Mon,Wed,Fri');
INSERT INTO doctors (name, specialization, qualification, phone, email, experience, fee, available_days) VALUES ('Sunita Rao', 'Paediatrics', 'MBBS, MD', '9000000002', 'sunita@medicare.example', 10, 600, 'Mon-Sat');
INSERT INTO doctors (name, specialization, qualification, phone, email, experience, fee, available_days) VALUES ('Rajesh Verma', 'Orthopaedics', 'MBBS, MS', '9000000003', 'rajesh@medicare.example', 12, 700, 'Tue,Thu,Sat');
INSERT INTO doctors (name, specialization, qualification, phone, email, experience, fee, available_days) VALUES ('Meena Iyer', 'Dermatology', 'MBBS, MD', '9000000004', 'meena@medicare.example', 8, 500, 'Mon,Tue,Thu');
INSERT INTO doctors (name, specialization, qualification, phone, email, experience, fee, available_days) VALUES ('Vikram Singh', 'General Medicine', 'MBBS, MD', '9000000005', 'vikram@medicare.example', 20, 400, 'Mon-Sat');

-- Logins: admin/admin123, doctor1/doctor123, doctor2/doctor123, reception1/recep123 (stored as SHA-256 hashes)
INSERT INTO users (username, password_hash, full_name, email, role, doctor_id) VALUES ('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'System Administrator', 'admin@medicare.example', 'admin', NULL);
INSERT INTO users (username, password_hash, full_name, email, role, doctor_id) VALUES ('doctor1', 'f348d5628621f3d8f59c8cabda0f8eb0aa7e0514a90be7571020b1336f26c113', 'Dr. Anil Kumar', 'anil@medicare.example', 'doctor', 1);
INSERT INTO users (username, password_hash, full_name, email, role, doctor_id) VALUES ('doctor2', 'f348d5628621f3d8f59c8cabda0f8eb0aa7e0514a90be7571020b1336f26c113', 'Dr. Sunita Rao', 'sunita@medicare.example', 'doctor', 2);
INSERT INTO users (username, password_hash, full_name, email, role, doctor_id) VALUES ('reception1', '5d37ed314cf2b5c8462b52b12cd512e2ac4a180e75598da4f12bfb0dea6d0a67', 'Ravi Receptionist', 'reception@medicare.example', 'receptionist', NULL);

INSERT INTO patients (name, age, gender, phone, email, blood_group, address, registered_on) VALUES ('Ramesh Babu', 45, 'Male', '9100000001', 'ramesh@example.com', 'O+', 'Eluru, Andhra Pradesh', TRUNC(SYSDATE) - 40);
INSERT INTO patients (name, age, gender, phone, email, blood_group, address, registered_on) VALUES ('Lakshmi Devi', 32, 'Female', '9100000002', 'lakshmi@example.com', 'A+', 'Bhimavaram, Andhra Pradesh', TRUNC(SYSDATE) - 30);
INSERT INTO patients (name, age, gender, phone, email, blood_group, address, registered_on) VALUES ('Suresh Reddy', 58, 'Male', '9100000003', NULL, 'B+', 'Vijayawada, Andhra Pradesh', TRUNC(SYSDATE) - 25);
INSERT INTO patients (name, age, gender, phone, email, blood_group, address, registered_on) VALUES ('Anjali Sharma', 7, 'Female', '9100000004', NULL, 'AB+', 'Guntur, Andhra Pradesh', TRUNC(SYSDATE) - 20);
INSERT INTO patients (name, age, gender, phone, email, blood_group, address, registered_on) VALUES ('Mohammed Ali', 39, 'Male', '9100000005', 'ali@example.com', 'O-', 'Hyderabad, Telangana', TRUNC(SYSDATE) - 15);
INSERT INTO patients (name, age, gender, phone, email, blood_group, address, registered_on) VALUES ('Priya Nair', 27, 'Female', '9100000006', 'priya@example.com', 'A-', 'Rajahmundry, Andhra Pradesh', TRUNC(SYSDATE) - 10);
INSERT INTO patients (name, age, gender, phone, email, blood_group, address, registered_on) VALUES ('Venkat Rao', 63, 'Male', '9100000007', NULL, 'B-', 'Kakinada, Andhra Pradesh', TRUNC(SYSDATE) - 5);
INSERT INTO patients (name, age, gender, phone, email, blood_group, address, registered_on) VALUES ('Kavitha Menon', 51, 'Female', '9100000008', 'kavitha@example.com', 'O+', 'Visakhapatnam, Andhra Pradesh', TRUNC(SYSDATE));

INSERT INTO appointments (patient_id, doctor_id, appt_date, appt_time, reason, status) VALUES (1, 1, TRUNC(SYSDATE), '10:00', 'Chest pain follow-up', 'Scheduled');
INSERT INTO appointments (patient_id, doctor_id, appt_date, appt_time, reason, status) VALUES (2, 5, TRUNC(SYSDATE), '11:30', 'Fever and cold', 'Scheduled');
INSERT INTO appointments (patient_id, doctor_id, appt_date, appt_time, reason, status) VALUES (4, 2, TRUNC(SYSDATE) + 1, '09:30', 'Vaccination', 'Scheduled');
INSERT INTO appointments (patient_id, doctor_id, appt_date, appt_time, reason, status) VALUES (3, 1, TRUNC(SYSDATE) + 2, '10:30', 'ECG review', 'Scheduled');
INSERT INTO appointments (patient_id, doctor_id, appt_date, appt_time, reason, status) VALUES (5, 3, TRUNC(SYSDATE) + 3, '15:00', 'Knee pain', 'Scheduled');
INSERT INTO appointments (patient_id, doctor_id, appt_date, appt_time, reason, status) VALUES (6, 4, TRUNC(SYSDATE) - 3, '12:00', 'Skin allergy', 'Completed');
INSERT INTO appointments (patient_id, doctor_id, appt_date, appt_time, reason, status) VALUES (7, 1, TRUNC(SYSDATE) - 5, '09:00', 'Blood pressure check', 'Completed');
INSERT INTO appointments (patient_id, doctor_id, appt_date, appt_time, reason, status) VALUES (8, 5, TRUNC(SYSDATE) - 1, '16:00', 'General check-up', 'Cancelled');

INSERT INTO medical_records (patient_id, doctor_id, visit_date, diagnosis, symptoms, treatment, notes) VALUES (7, 1, TRUNC(SYSDATE) - 5, 'Hypertension', 'Headache, dizziness', 'Start amlodipine; reduce salt intake', 'Review after 2 weeks');
INSERT INTO medical_records (patient_id, doctor_id, visit_date, diagnosis, symptoms, treatment, notes) VALUES (6, 4, TRUNC(SYSDATE) - 3, 'Allergic dermatitis', 'Itching, red rashes', 'Antihistamine and moisturiser', NULL);
INSERT INTO medical_records (patient_id, doctor_id, visit_date, diagnosis, symptoms, treatment, notes) VALUES (1, 1, TRUNC(SYSDATE) - 20, 'Angina (stable)', 'Chest discomfort on exertion', 'Aspirin, statin, stress test advised', 'Avoid heavy exercise');
INSERT INTO medical_records (patient_id, doctor_id, visit_date, diagnosis, symptoms, treatment, notes) VALUES (2, 5, TRUNC(SYSDATE) - 12, 'Viral fever', 'Fever, body ache', 'Paracetamol, fluids, rest', NULL);

INSERT INTO prescriptions (patient_id, doctor_id, presc_date, medicines, instructions) VALUES (7, 1, TRUNC(SYSDATE) - 5, 'Amlodipine 5mg - 1 tablet daily - 30 days' || CHR(10) || 'Atorvastatin 10mg - 1 tablet at night - 30 days', 'Low salt diet. Check BP daily.');
INSERT INTO prescriptions (patient_id, doctor_id, presc_date, medicines, instructions) VALUES (6, 4, TRUNC(SYSDATE) - 3, 'Cetirizine 10mg - 1 tablet at night - 7 days' || CHR(10) || 'Moisturising lotion - twice daily - 14 days', 'Avoid known allergens.');
INSERT INTO prescriptions (patient_id, doctor_id, presc_date, medicines, instructions) VALUES (2, 5, TRUNC(SYSDATE) - 12, 'Paracetamol 500mg - 1 tablet three times daily - 3 days', 'Drink plenty of water.');

INSERT INTO bills (patient_id, bill_date, consultation, medicine, lab, other, total, status) VALUES (7, TRUNC(SYSDATE) - 5, 800, 450, 0, 0, 1250, 'Paid');
INSERT INTO bills (patient_id, bill_date, consultation, medicine, lab, other, total, status) VALUES (6, TRUNC(SYSDATE) - 3, 500, 220, 0, 0, 720, 'Paid');
INSERT INTO bills (patient_id, bill_date, consultation, medicine, lab, other, total, status) VALUES (1, TRUNC(SYSDATE) - 20, 800, 600, 900, 100, 2400, 'Paid');
INSERT INTO bills (patient_id, bill_date, consultation, medicine, lab, other, total, status) VALUES (2, TRUNC(SYSDATE) - 12, 400, 60, 0, 0, 460, 'Paid');
INSERT INTO bills (patient_id, bill_date, consultation, medicine, lab, other, total, status) VALUES (3, TRUNC(SYSDATE) - 1, 800, 0, 350, 0, 1150, 'Pending');
INSERT INTO bills (patient_id, bill_date, consultation, medicine, lab, other, total, status) VALUES (5, TRUNC(SYSDATE), 700, 0, 0, 150, 850, 'Pending');

INSERT INTO medicines (name, category, quantity, price, expiry_date, reorder_level) VALUES ('Paracetamol 500mg', 'Analgesic', 500, 2.0, TRUNC(SYSDATE) + 400, 100);
INSERT INTO medicines (name, category, quantity, price, expiry_date, reorder_level) VALUES ('Amoxicillin 250mg', 'Antibiotic', 120, 8.5, TRUNC(SYSDATE) + 200, 50);
INSERT INTO medicines (name, category, quantity, price, expiry_date, reorder_level) VALUES ('Cetirizine 10mg', 'Antihistamine', 300, 3.0, TRUNC(SYSDATE) + 300, 50);
INSERT INTO medicines (name, category, quantity, price, expiry_date, reorder_level) VALUES ('Amlodipine 5mg', 'Cardiac', 200, 4.0, TRUNC(SYSDATE) + 500, 50);
INSERT INTO medicines (name, category, quantity, price, expiry_date, reorder_level) VALUES ('Atorvastatin 10mg', 'Cardiac', 18, 6.5, TRUNC(SYSDATE) + 450, 40);
INSERT INTO medicines (name, category, quantity, price, expiry_date, reorder_level) VALUES ('Metformin 500mg', 'Antidiabetic', 250, 2.5, TRUNC(SYSDATE) + 350, 50);
INSERT INTO medicines (name, category, quantity, price, expiry_date, reorder_level) VALUES ('Insulin Glargine', 'Antidiabetic', 30, 450.0, TRUNC(SYSDATE) + 90, 10);
INSERT INTO medicines (name, category, quantity, price, expiry_date, reorder_level) VALUES ('ORS Sachet', 'Rehydration', 400, 12.0, TRUNC(SYSDATE) + 600, 80);
INSERT INTO medicines (name, category, quantity, price, expiry_date, reorder_level) VALUES ('Cough Syrup 100ml', 'Cough and Cold', 45, 55.0, TRUNC(SYSDATE) - 15, 20);
INSERT INTO medicines (name, category, quantity, price, expiry_date, reorder_level) VALUES ('Ibuprofen 400mg', 'Analgesic', 150, 3.5, TRUNC(SYSDATE) + 250, 50);

INSERT INTO lab_tests (patient_id, doctor_id, test_name, test_date, cost, status, result) VALUES (1, 1, 'Lipid Profile', TRUNC(SYSDATE) - 19, 600, 'Completed', 'LDL slightly high (145 mg/dL)');
INSERT INTO lab_tests (patient_id, doctor_id, test_name, test_date, cost, status, result) VALUES (3, 1, 'ECG', TRUNC(SYSDATE) + 2, 350, 'Pending', NULL);
INSERT INTO lab_tests (patient_id, doctor_id, test_name, test_date, cost, status, result) VALUES (7, 1, 'Blood Sugar', TRUNC(SYSDATE) - 5, 150, 'Completed', 'Fasting 98 mg/dL (normal)');
INSERT INTO lab_tests (patient_id, doctor_id, test_name, test_date, cost, status, result) VALUES (2, 5, 'CBC', TRUNC(SYSDATE) - 12, 300, 'Completed', 'WBC mildly elevated');
INSERT INTO lab_tests (patient_id, doctor_id, test_name, test_date, cost, status, result) VALUES (5, 3, 'X-Ray Knee', TRUNC(SYSDATE) + 3, 500, 'Pending', NULL);

INSERT INTO messages (name, email, subject, message) VALUES ('Sita Ram', 'sita@example.com', 'Visiting hours', 'What are the visiting hours for the general ward?');
INSERT INTO messages (name, email, subject, message) VALUES ('Gopal K', 'gopal@example.com', 'Health check-up packages', 'Do you offer full-body health check-up packages for seniors?');

COMMIT;

-- Quick check: should return 4 rows
SELECT username, role FROM users;

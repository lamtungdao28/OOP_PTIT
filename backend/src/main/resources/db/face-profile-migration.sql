-- Run once against an existing MySQL database before starting with JPA_DDL_AUTO=validate.
-- A 128-dimension float32 embedding occupies 512 bytes; the old default binary
-- column may be too short. Hibernate's @Lob maps this field to LONGBLOB on MySQL.
ALTER TABLE FACE_PROFILES MODIFY COLUMN embedding LONGBLOB NOT NULL;

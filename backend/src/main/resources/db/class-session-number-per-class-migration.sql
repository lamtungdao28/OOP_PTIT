-- Run once on an existing MySQL database before starting the updated backend.
-- API paths use CLASS_SESSIONS.id, so renumbering session_no does not change references.

SET @old_session_no_index = (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND LOWER(table_name) = 'class_sessions'
      AND non_unique = 0
    GROUP BY index_name
    HAVING COUNT(*) = 1
       AND SUM(LOWER(column_name) = 'session_no') = 1
    LIMIT 1
);

SET @drop_old_session_no_index = IF(
    @old_session_no_index IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE class_sessions DROP INDEX `', @old_session_no_index, '`')
);
PREPARE drop_session_no_index FROM @drop_old_session_no_index;
EXECUTE drop_session_no_index;
DEALLOCATE PREPARE drop_session_no_index;

UPDATE class_sessions target
JOIN (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY course_classesid
               ORDER BY session_date, start_time, id
           ) AS new_session_no
    FROM class_sessions
) numbered ON numbered.id = target.id
SET target.session_no = numbered.new_session_no;

ALTER TABLE class_sessions
    ADD CONSTRAINT uk_class_sessions_class_session_no
    UNIQUE (course_classesid, session_no);

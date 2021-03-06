BEGIN WORK;
-- Need a bit more room for event descriptions
ALTER TABLE candidate_events ALTER test_event_type SET DATA TYPE varchar(32);
ALTER TABLE candidate_events ALTER item_event_type SET DATA TYPE varchar(32);
-- Had to rename candidate_sequence.terminated for MySQL compatibility
ALTER TABLE candidate_sessions ADD column is_terminated boolean;
UPDATE candidate_sessions SET is_terminated = terminated;
ALTER TABLE candidate_sessions ALTER COLUMN is_terminated SET NOT NULL;
ALTER TABLE candidate_sessions DROP COLUMN terminated;
-- Further tweaks to @SequenceGenerator annotations
ALTER SEQUENCE assessment_package_sequence START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE assessment_sequence START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE candidate_event_notification_sequence START WITH 1 INCREMENT BY 10;
ALTER SEQUENCE candidate_event_sequence START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE candidate_file_submission_sequence START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE candidate_response_sequence START WITH 1 INCREMENT BY 5;
ALTER SEQUENCE candidate_session_outcome_sequence START WITH 1 INCREMENT BY 10;
ALTER SEQUENCE candidate_session_sequence START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE delivery_sequence START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE delivery_settings_sequence START WITH 1 INCREMENT BY 1;
ALTER SEQUENCE sample_category_sequence START WITH 1 INCREMENT BY 10;
ALTER SEQUENCE user_sequence START WITH 1000 INCREMENT BY 1;
-- Some event names were changed in DEV26
UPDATE candidate_events SET test_event_type='ENTER_TEST' WHERE test_event_type='INIT';
UPDATE candidate_events SET test_event_type='ADVANCE_TEST_PART' WHERE test_event_type='EXIT_TEST_PART';
--
COMMIT WORK;

-- Flyway callback: runs only when an existing database without migration history is
-- adopted at V1 (baseline-on-migrate). V1 is skipped for such a database, so refuse
-- anything that is not the 4.0.x schema V1 describes. user_ranks.ai_config_edit is
-- the newest column (added in 4.0.0).

DO $$
BEGIN
    IF (SELECT count(*) FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name IN ('user_ranks', 'users', 'lessons', 'exercises', 'comments', 'comment_flags',
                             'user_groups', 'user_groups_meta', 'student_sessions', 'ai_interactions',
                             'ai_config')) <> 11
       OR NOT EXISTS (SELECT 1 FROM information_schema.columns
                      WHERE table_schema = current_schema()
                        AND table_name = 'user_ranks' AND column_name = 'ai_config_edit') THEN
        RAISE EXCEPTION 'Existing database is not on the 4.0.x schema. Upgrade to the latest 4.0.x release first.';
    END IF;
END $$;

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

-- --------------------------------------------------------

--
-- Structure for table `exercises`
--

CREATE TABLE `exercises` (
  `id` bigint UNSIGNED NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint UNSIGNED DEFAULT NULL,
  `lesson_id` bigint UNSIGNED DEFAULT NULL,
  `published` tinyint(1) NOT NULL DEFAULT 0,
  `commentable` tinyint(1) NOT NULL DEFAULT 0,
  `created` datetime NOT NULL DEFAULT current_timestamp(),
  `last_edit` datetime DEFAULT NULL ON UPDATE current_timestamp(),
  `graspable_enabled` tinyint(1) DEFAULT 0,
  `graspable_initial_expression` text COLLATE utf8mb4_unicode_ci,
  `graspable_target_expression` text COLLATE utf8mb4_unicode_ci,
  `graspable_allowed_operations` text COLLATE utf8mb4_unicode_ci,
  `graspable_difficulty` varchar(50) COLLATE utf8mb4_unicode_ci,
  `graspable_hints` text COLLATE utf8mb4_unicode_ci,
  `graspable_config` text COLLATE utf8mb4_unicode_ci
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Relations for table `exercises`:
--   `user_id`
--       `users` -> `id`
--   `lesson_id`
--       `lessons` -> `id`
--

-- --------------------------------------------------------

--
-- Structure for table `lessons`
--

CREATE TABLE `lessons` (
  `id` bigint UNSIGNED NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `parent_id` bigint UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Relations for table `lessons`:
--   `parent_id`
--       `lessons` -> `id`
--

-- --------------------------------------------------------

--
-- Structure for table `comments`
--

CREATE TABLE `comments` (
  `id` bigint UNSIGNED NOT NULL,
  `content` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `exercise_id` bigint UNSIGNED NOT NULL,
  `user_id` bigint UNSIGNED,
  `created` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Relations for table `comments`:
--   `user_id`
--       `users` -> `id`
--   `exercise_id`
--       `exercises` -> `id`
--

-- --------------------------------------------------------

--
-- Structure for table `users`
--

CREATE TABLE `users` (
  `id` bigint UNSIGNED NOT NULL,
  `username` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `salt` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `rank_id` bigint UNSIGNED NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `banned` tinyint(1) NOT NULL DEFAULT 0,
  `activated` tinyint(1) NOT NULL DEFAULT 0,
  `activation_key` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created` datetime NOT NULL DEFAULT current_timestamp(),
  `last_login` datetime NOT NULL DEFAULT current_timestamp(),
  `user_avatar_emoji` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '🧒',
  `tutor_avatar_emoji` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '🤖'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Relations for table `users`:
--   `rank_id`
--       `user_ranks` -> `id`
--

--
-- Inserts for table `users`
--

INSERT INTO `users` (`id`, `username`, `password`, `salt`, `rank_id`, `activated`) VALUES
(1, 'admin', '3HWqMv8tiSEbBcsUfxqBx7kY4vw+cSvG7OQXp9uzM0w=', '0l/SGC6gqKwYWjw7sm2IrwzIcAjq/QkO9xXcG/LC56c=', 1, 1),
(2, 'teacher', 'gqjX9Myv2T0+cSsc7Mk5uP00vWN74acNaV8aVJvvK8Q=', 'Oz3c7v4qJJqqbPHlTzAhilp4O7o+DdW4iBYQMJRABQo=', 2, 1),
(3, 'student1', 't/NeeExH/6i3y2DBq77LXyOkGvnk6TCaE1p/lLObE98=', 'tpINgKObPWkbOrylflSrEECZi5ZHvhv2Wjkzlr9HW3E=', 3, 1),
(4, 'student2', '0hCDh1yJvbG4VDOqtZWF3qgL3YPUYneknACoEQ6G8Kc=', '4G1YeLz6tsTH98j9zOoEcxvSK0uZnM51uLhF6O6H7pM=', 3, 1);

-- --------------------------------------------------------

--
-- Structure for table `user_groups`
--

CREATE TABLE `user_groups` (
  `id` bigint UNSIGNED NOT NULL,
  `name` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Inserts for table `user_groups`
--

INSERT INTO `user_groups` (`id`, `name`) VALUES
(1, 'Teacher'),
(2, 'Class 8A'),
(3, 'Class 8B'),
(4, 'Class 9A'),
(5, 'Class 9B');

-- --------------------------------------------------------

--
-- Structure for table `user_groups_meta`
--

CREATE TABLE `user_groups_meta` (
  `id` bigint UNSIGNED NOT NULL,
  `user_id` bigint UNSIGNED NOT NULL,
  `group_id` bigint UNSIGNED NOT NULL,
  `timestamp` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Relations for table `user_groups_meta`:
--   `user_id`
--       `users` -> `id`
--   `group_id`
--       `user_groups` -> `id`
--

--
-- Inserts for table `user_groups_meta`
--

INSERT INTO `user_groups_meta` (`id`, `user_id`, `group_id`) VALUES
(1, 2, 1),
(2, 3, 4),
(3, 4, 4);

-- --------------------------------------------------------

--
-- Structure for table `user_ranks`
--

CREATE TABLE `user_ranks` (
  `id` bigint UNSIGNED NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `admin_view` tinyint(1) NOT NULL DEFAULT 0,
  `exercise_add` tinyint(1) NOT NULL DEFAULT 0,
  `exercise_delete` tinyint(1) NOT NULL DEFAULT 0,
  `exercise_edit` tinyint(1) NOT NULL DEFAULT 0,
  `lesson_add` tinyint(1) NOT NULL DEFAULT 0,
  `lesson_delete` tinyint(1) NOT NULL DEFAULT 0,
  `lesson_edit` tinyint(1) NOT NULL DEFAULT 0,
  `comment_add` tinyint(1) NOT NULL DEFAULT 0,
  `comment_delete` tinyint(1) NOT NULL DEFAULT 0,
  `comment_edit` tinyint(1) NOT NULL DEFAULT 0,
  `user_add` tinyint(1) NOT NULL DEFAULT 0,
  `user_delete` tinyint(1) NOT NULL DEFAULT 0,
  `user_edit` tinyint(1) NOT NULL DEFAULT 0,
  `user_group_add` tinyint(1) NOT NULL DEFAULT 0,
  `user_group_delete` tinyint(1) NOT NULL DEFAULT 0,
  `user_group_edit` tinyint(1) NOT NULL DEFAULT 0,
  `user_rank_add` tinyint(1) NOT NULL DEFAULT 0,
  `user_rank_delete` tinyint(1) NOT NULL DEFAULT 0,
  `user_rank_edit` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Relations for table `user_ranks`:
--

--
-- Inserts for table `user_ranks`
--

INSERT INTO `user_ranks` (`id`, `name`, `admin_view`, `exercise_add`, `exercise_delete`, `exercise_edit`, `lesson_add`, `lesson_delete`, `lesson_edit`, `comment_add`, `comment_delete`, `comment_edit`, `user_add`, `user_delete`, `user_edit`, `user_group_add`, `user_group_delete`, `user_group_edit`, `user_rank_add`, `user_rank_delete`, `user_rank_edit`) VALUES
(1, 'Admin', 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
(2, 'Teacher', 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0),
(3, 'Student', 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

--
-- Indexes for exported tables
--

--
-- Indexes for table `exercises`
--
ALTER TABLE `exercises`
  ADD PRIMARY KEY (`id`),
  ADD KEY `exercises_ibfk_1` (`user_id`),
  ADD KEY `exercises_ibfk_2` (`lesson_id`);

ALTER TABLE `exercises`
  ADD FULLTEXT KEY `content` (`content`);

--
-- Indexes for table `lessons`
--
ALTER TABLE `lessons`
  ADD PRIMARY KEY (`id`),
  ADD KEY `lessons_ibfk_1` (`parent_id`);

--
-- Indexes for table `comments`
--
ALTER TABLE `comments`
  ADD PRIMARY KEY (`id`),
  ADD KEY `comments_ibfk_1` (`user_id`),
  ADD KEY `comments_ibfk_2` (`exercise_id`);

ALTER TABLE `comments` 
  ADD FULLTEXT KEY `content` (`content`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `users_username` (`username`),
  ADD UNIQUE KEY `users_email` (`email`),
  ADD KEY `users_ibfk_1` (`rank_id`);

--
-- Indexes for table `user_groups`
--
ALTER TABLE `user_groups`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `user_groups_meta`
--
ALTER TABLE `user_groups_meta`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `user_id` (`user_id`,`group_id`),
  ADD KEY `group_id` (`group_id`);

--
-- Indexes for table `user_ranks`
--
ALTER TABLE `user_ranks`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `user_ranks_name` (`name`);

--
-- AUTO_INCREMENT for exported tables
--

--
-- AUTO_INCREMENT for table `exercises`
--
ALTER TABLE `exercises`
  MODIFY `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `lessons`
--
ALTER TABLE `lessons`
  MODIFY `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `comments`
--
ALTER TABLE `comments`
  MODIFY `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT for table `user_groups`
--
ALTER TABLE `user_groups`
  MODIFY `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `user_groups_meta`
--
ALTER TABLE `user_groups_meta`
  MODIFY `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `user_ranks`
--
ALTER TABLE `user_ranks`
  MODIFY `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- Constraints for exported tables
--

--
-- Constraints for table `exercises`
--
ALTER TABLE `exercises`
  ADD CONSTRAINT `exercises_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `exercises_ibfk_2` FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Constraints for table `lessons`
--
ALTER TABLE `lessons`
  ADD CONSTRAINT `lessons_ibfk_1` FOREIGN KEY (`parent_id`) REFERENCES `lessons` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Constraints for table `comments`
--
ALTER TABLE `comments`
  ADD CONSTRAINT `comments_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `comments_ibfk_2` FOREIGN KEY (`exercise_id`) REFERENCES `exercises` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `users`
--
ALTER TABLE `users`
  ADD CONSTRAINT `users_ibfk_1` FOREIGN KEY (`rank_id`) REFERENCES `user_ranks` (`id`) ON UPDATE CASCADE;

--
-- Constraints for table `user_groups_meta`
--
ALTER TABLE `user_groups_meta`
  ADD CONSTRAINT `user_groups_meta_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `user_groups_meta_ibfk_2` FOREIGN KEY (`group_id`) REFERENCES `user_groups` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- --------------------------------------------------------

--
-- Structure for table `student_sessions`
--

CREATE TABLE `student_sessions` (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `session_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint UNSIGNED NOT NULL,
  `exercise_id` bigint UNSIGNED NOT NULL,
  `start_time` datetime NOT NULL DEFAULT current_timestamp(),
  `end_time` datetime DEFAULT NULL,
  `completed` tinyint(1) NOT NULL DEFAULT 0,
  `actions_count` int NOT NULL DEFAULT 0,
  `correct_actions` int NOT NULL DEFAULT 0,
  `hints_used` int NOT NULL DEFAULT 0,
  `final_expression` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `session_id` (`session_id`),
  KEY `user_id` (`user_id`),
  KEY `exercise_id` (`exercise_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Constraints for table `student_sessions`
--
ALTER TABLE `student_sessions`
  ADD CONSTRAINT `student_sessions_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `student_sessions_ibfk_2` FOREIGN KEY (`exercise_id`) REFERENCES `exercises` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

-- --------------------------------------------------------

--
-- Structure for table `ai_interactions`
--

CREATE TABLE `ai_interactions` (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `session_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_id` bigint UNSIGNED DEFAULT NULL,
  `exercise_id` bigint UNSIGNED DEFAULT NULL,
  `event_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expression_before` text COLLATE utf8mb4_unicode_ci,
  `expression_after` text COLLATE utf8mb4_unicode_ci,
  `feedback_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `feedback_message` text COLLATE utf8mb4_unicode_ci,
  `confidence_score` double DEFAULT NULL,
  `action_correct` tinyint(1) DEFAULT NULL,
  `conversation_context` text COLLATE utf8mb4_unicode_ci,
  `timestamp` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `session_id` (`session_id`),
  KEY `user_id` (`user_id`),
  KEY `exercise_id` (`exercise_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Constraints for table `ai_interactions`
--
ALTER TABLE `ai_interactions`
  ADD CONSTRAINT `ai_interactions_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `ai_interactions_ibfk_2` FOREIGN KEY (`exercise_id`) REFERENCES `exercises` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;

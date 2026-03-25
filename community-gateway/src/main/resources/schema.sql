-- Disable foreign key checks to allow dropping tables in any order
SET FOREIGN_KEY_CHECKS = 0;

-- Drop tables if they exist
DROP TABLE IF EXISTS `t_notification`;
DROP TABLE IF EXISTS `t_audit_log`;
DROP TABLE IF EXISTS `t_prohibited_word`;
DROP TABLE IF EXISTS `t_notice_read`;
DROP TABLE IF EXISTS `t_notice`;
DROP TABLE IF EXISTS `t_club_finance`;
DROP TABLE IF EXISTS `t_resource_application`;
DROP TABLE IF EXISTS `t_resource`;
DROP TABLE IF EXISTS `t_recruit_application`;
DROP TABLE IF EXISTS `t_recruit_form_field`;
DROP TABLE IF EXISTS `t_recruit_batch`;
DROP TABLE IF EXISTS `t_member_point_record`;
DROP TABLE IF EXISTS `t_activity_attendance`;
DROP TABLE IF EXISTS `t_activity_signup`;
DROP TABLE IF EXISTS `t_activity`;
DROP TABLE IF EXISTS `t_member`;
DROP TABLE IF EXISTS `t_club_tag`;
DROP TABLE IF EXISTS `t_club`;
DROP TABLE IF EXISTS `t_user_role`;
DROP TABLE IF EXISTS `t_role`;
DROP TABLE IF EXISTS `t_user`;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- 1. User Table
CREATE TABLE `t_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `username` varchar(50) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `real_name` varchar(50) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `mobile` varchar(20) DEFAULT NULL,
  `avatar_url` varchar(255) DEFAULT NULL,
  `interests` varchar(500) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Role Table
CREATE TABLE `t_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `code` varchar(32) NOT NULL,
  `name` varchar(64) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. User-Role Relation Table
CREATE TABLE `t_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_role_id` (`role_id`),
  CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `t_role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Club Table
CREATE TABLE `t_club` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `name` varchar(100) NOT NULL,
  `short_name` varchar(50) DEFAULT NULL,
  `category` varchar(50) NOT NULL,
  `logo_url` varchar(255) DEFAULT NULL,
  `description` text,
  `founded_year` int DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `created_by` bigint NOT NULL,
  `dissolution_reason` varchar(255) DEFAULT NULL,
  `dissolution_date` datetime(6) DEFAULT NULL,
  `visit_count` int DEFAULT '0',
  `balance` decimal(10,2) DEFAULT '0.00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_club_name` (`name`),
  KEY `idx_club_created_by` (`created_by`),
  CONSTRAINT `chk_club_balance` CHECK (`balance` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Club Tag Table
CREATE TABLE `t_club_tag` (
  `club_id` bigint NOT NULL,
  `tag` varchar(50) DEFAULT NULL,
  KEY `fk_tag_club` (`club_id`),
  CONSTRAINT `fk_tag_club` FOREIGN KEY (`club_id`) REFERENCES `t_club` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Member Table
CREATE TABLE `t_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `club_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `role_code` varchar(32) NOT NULL DEFAULT 'MEMBER',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `points` int NOT NULL DEFAULT '0',
  `join_at` datetime(6) NOT NULL,
  `extra` json DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_club_user` (`club_id`,`user_id`),
  CONSTRAINT `fk_member_club` FOREIGN KEY (`club_id`) REFERENCES `t_club` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_member_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Member Point Record Table
CREATE TABLE `t_member_point_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `club_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `delta_points` int NOT NULL,
  `balance_after` int NOT NULL,
  `source_type` varchar(32) NOT NULL,
  `source_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `operator_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_member_point_record_club_user` (`club_id`,`user_id`),
  KEY `idx_member_point_record_source` (`source_type`,`source_id`),
  CONSTRAINT `fk_member_point_record_club` FOREIGN KEY (`club_id`) REFERENCES `t_club` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_member_point_record_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_member_point_record_operator` FOREIGN KEY (`operator_id`) REFERENCES `t_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Activity Table
CREATE TABLE `t_activity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `club_id` bigint NOT NULL,
  `title` varchar(200) NOT NULL,
  `description` text,
  `cover_url` varchar(255) DEFAULT NULL,
  `type` varchar(50) NOT NULL,
  `location` varchar(255) DEFAULT NULL,
  `start_time` datetime(6) NOT NULL,
  `end_time` datetime(6) NOT NULL,
  `signup_start_time` datetime(6) DEFAULT NULL,
  `signup_end_time` datetime(6) DEFAULT NULL,
  `max_participants` int DEFAULT NULL,
  `need_attendance` bit(1) DEFAULT b'0',
  `checkin_code` varchar(20) DEFAULT NULL,
  `reward_points` int NOT NULL DEFAULT '0',
  `settlement_status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `settled_at` datetime(6) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  PRIMARY KEY (`id`),
  KEY `idx_activity_club` (`club_id`),
  KEY `idx_activity_status` (`status`),
  CONSTRAINT `fk_activity_club` FOREIGN KEY (`club_id`) REFERENCES `t_club` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Activity Signup Table
CREATE TABLE `t_activity_signup` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `activity_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'SIGNED',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_user` (`activity_id`,`user_id`),
  CONSTRAINT `fk_signup_activity` FOREIGN KEY (`activity_id`) REFERENCES `t_activity` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_signup_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Activity Attendance Table
CREATE TABLE `t_activity_attendance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `activity_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `sign_time` datetime(6) NOT NULL,
  `source` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_attend_activity_user` (`activity_id`,`user_id`),
  CONSTRAINT `fk_attend_activity` FOREIGN KEY (`activity_id`) REFERENCES `t_activity` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_attend_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Recruit Batch Table
CREATE TABLE `t_recruit_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `club_id` bigint NOT NULL,
  `title` varchar(100) NOT NULL,
  `start_time` datetime(6) NOT NULL,
  `end_time` datetime(6) NOT NULL,
  `quota` int DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  PRIMARY KEY (`id`),
  KEY `idx_recruit_club` (`club_id`),
  CONSTRAINT `fk_recruit_club` FOREIGN KEY (`club_id`) REFERENCES `t_club` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. Recruit Form Field Table
CREATE TABLE `t_recruit_form_field` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `batch_id` bigint NOT NULL,
  `field_key` varchar(50) NOT NULL,
  `label` varchar(100) NOT NULL,
  `type` varchar(50) NOT NULL,
  `options` json DEFAULT NULL,
  `required` bit(1) DEFAULT b'1',
  `sort_order` int DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_field_batch` (`batch_id`),
  CONSTRAINT `fk_field_batch` FOREIGN KEY (`batch_id`) REFERENCES `t_recruit_batch` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. Recruit Application Table
CREATE TABLE `t_recruit_application` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `batch_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `apply_data` json NOT NULL,
  `first_review_status` varchar(20) DEFAULT 'PENDING',
  `first_review_comment` varchar(255) DEFAULT NULL,
  `final_review_status` varchar(20) DEFAULT 'PENDING',
  `final_review_comment` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_user` (`batch_id`,`user_id`),
  CONSTRAINT `fk_app_batch` FOREIGN KEY (`batch_id`) REFERENCES `t_recruit_batch` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_app_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. Notice Table
CREATE TABLE `t_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `club_id` bigint DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `scope` varchar(20) NOT NULL,
  `published_by` bigint NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  PRIMARY KEY (`id`),
  KEY `idx_notice_published_by` (`published_by`),
  CONSTRAINT `fk_notice_publisher` FOREIGN KEY (`published_by`) REFERENCES `t_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `t_prohibited_word` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `word` varchar(100) NOT NULL,
  `enabled` bit(1) NOT NULL DEFAULT b'1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_prohibited_word_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `t_notice_read` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `notice_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `read_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notice_user` (`notice_id`,`user_id`),
  CONSTRAINT `fk_read_notice` FOREIGN KEY (`notice_id`) REFERENCES `t_notice` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_read_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. Audit Log Table
CREATE TABLE `t_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `action` varchar(100) NOT NULL,
  `resource_type` varchar(50) DEFAULT NULL,
  `resource_id` varchar(50) DEFAULT NULL,
  `detail` text,
  `ip` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_audit_user_id` (`user_id`),
  KEY `idx_audit_action` (`action`),
  KEY `idx_audit_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE `t_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `type` varchar(20) NOT NULL,
  `is_read` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_notify_user` (`user_id`),
  CONSTRAINT `fk_notify_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. Club Finance Table
CREATE TABLE `t_club_finance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `club_id` bigint NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `type` varchar(20) NOT NULL,
  `title` varchar(200) NOT NULL,
  `description` text,
  `status` varchar(20) NOT NULL,
  `applicant_id` bigint NOT NULL,
  `approver_id` bigint DEFAULT NULL,
  `proof_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_finance_club` (`club_id`),
  KEY `idx_finance_applicant` (`applicant_id`),
  CONSTRAINT `fk_finance_club` FOREIGN KEY (`club_id`) REFERENCES `t_club` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_finance_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `t_user` (`id`),
  CONSTRAINT `fk_finance_approver` FOREIGN KEY (`approver_id`) REFERENCES `t_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 18. Resource Table
CREATE TABLE `t_resource` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `name` varchar(100) NOT NULL,
  `type` varchar(20) NOT NULL,
  `description` text,
  `location` varchar(200) DEFAULT NULL,
  `capacity` int DEFAULT NULL,
  `total_quantity` int DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'AVAILABLE',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 19. Resource Application Table
CREATE TABLE `t_resource_application` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `club_id` bigint NOT NULL,
  `activity_id` bigint DEFAULT NULL,
  `resource_id` bigint NOT NULL,
  `start_time` datetime(6) NOT NULL,
  `end_time` datetime(6) NOT NULL,
  `quantity` int NOT NULL DEFAULT '1',
  `description` text,
  `status` varchar(20) NOT NULL,
  `applicant_id` bigint NOT NULL,
  `approver_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_resource_app_club` (`club_id`),
  KEY `idx_resource_app_activity` (`activity_id`),
  KEY `idx_resource_app_resource` (`resource_id`),
  KEY `idx_resource_app_applicant` (`applicant_id`),
  CONSTRAINT `fk_resource_app_club` FOREIGN KEY (`club_id`) REFERENCES `t_club` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_resource_app_activity` FOREIGN KEY (`activity_id`) REFERENCES `t_activity` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_resource_app_resource` FOREIGN KEY (`resource_id`) REFERENCES `t_resource` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_resource_app_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `t_user` (`id`),
  CONSTRAINT `fk_resource_app_approver` FOREIGN KEY (`approver_id`) REFERENCES `t_user` (`id`),
  CONSTRAINT `chk_resource_app_quantity` CHECK (`quantity` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

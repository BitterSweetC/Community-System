-- 1. Initialize Roles
INSERT INTO `t_role` (`code`, `name`, `description`, `created_at`, `updated_at`) VALUES 
('USER', '学生', '普通注册用户', NOW(), NOW()),
('ADMIN', '系统管理员', '拥有系统最高权限', NOW(), NOW()),
('CLUB_ADMIN', '社团管理员', '负责社团内部管理', NOW(), NOW()),
('VISITOR', '游客', '未登录访客', NOW(), NOW());

-- 2. Initialize Users
-- Admin: admin / 123456
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES 
('admin', '123456', '系统管理员', '13800000000', 'ACTIVE', NOW(), NOW());

-- Student: student / 123456
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES 
('student', '123456', '张三', '13900000001', 'ACTIVE', NOW(), NOW());

-- Club Admin: club_admin / 123456
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES 
('club_admin', '123456', '李社长', '13900000002', 'ACTIVE', NOW(), NOW());

-- 2.1 Additional Users (Generated)

-- Adding 系统管理员s
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('admin2', '123456', '朱平', '13900001000', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('admin3', '123456', '朱娜强', '13900001001', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('admin4', '123456', '何英娟', '13900001002', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('admin5', '123456', '宋艳', '13900001003', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('admin6', '123456', '罗平桂英', '13900001004', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('admin7', '123456', '张娜', '13900001005', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('admin8', '123456', '萧洋伟', '13900001006', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('admin9', '123456', '孙强静', '13900001007', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('admin10', '123456', '谢娟超', '13900001008', 'ACTIVE', NOW(), NOW());

-- Adding 学生s
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('student2', '123456', '韩飞明', '13900001009', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('student3', '123456', '马军军', '13900001010', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('student4', '123456', '杨飞', '13900001011', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('student5', '123456', '赵华', '13900001012', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('student6', '123456', '林英英', '13900001013', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('student7', '123456', '赵霞', '13900001014', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('student8', '123456', '李娜磊', '13900001015', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('student9', '123456', '梁伟杰', '13900001016', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('student10', '123456', '唐刚强', '13900001017', 'ACTIVE', NOW(), NOW());

-- Adding 社团管理员s
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('club_admin2', '123456', '胡勇刚', '13900001018', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('club_admin3', '123456', '李娟', '13900001019', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('club_admin4', '123456', '韩飞', '13900001020', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('club_admin5', '123456', '冯勇伟', '13900001021', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('club_admin6', '123456', '林浩', '13900001022', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('club_admin7', '123456', '胡非敏', '13900001023', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('club_admin8', '123456', '高伟', '13900001024', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('club_admin9', '123456', '李霞艳', '13900001025', 'ACTIVE', NOW(), NOW());
INSERT INTO `t_user` (`username`, `password_hash`, `real_name`, `mobile`, `status`, `created_at`, `updated_at`) VALUES ('club_admin10', '123456', '胡 敏强', '13900001026', 'ACTIVE', NOW(), NOW());

-- 3. Assign Roles
-- Admin -> ADMIN
INSERT INTO `t_user_role` (`user_id`, `role_id`) 
SELECT u.id, r.id FROM `t_user` u, `t_role` r WHERE u.username LIKE 'admin%' AND r.code = 'ADMIN';

-- Student -> USER
INSERT INTO `t_user_role` (`user_id`, `role_id`) 
SELECT u.id, r.id FROM `t_user` u, `t_role` r WHERE u.username LIKE 'student%' AND r.code = 'USER';

-- Club Admin -> CLUB_ADMIN
INSERT INTO `t_user_role` (`user_id`, `role_id`) 
SELECT u.id, r.id FROM `t_user` u, `t_role` r WHERE u.username LIKE 'club_admin%' AND r.code = 'CLUB_ADMIN';

-- 4. Initialize Clubs
INSERT INTO `t_club` (`name`, `short_name`, `category`, `description`, `founded_year`, `status`, `created_by`, `logo_url`, `created_at`, `updated_at`) 
SELECT '编程俱乐部', 'Coding Club', '学术科技', '编程俱乐部致力于推广计算机技术，组织编程比赛和技术分享会。', 2020, 'ACTIVE', u.id, 'https://placeholder.com/coding.png', NOW(), NOW()
FROM `t_user` u WHERE u.username = 'club_admin';

INSERT INTO `t_club` (`name`, `short_name`, `category`, `description`, `founded_year`, `status`, `created_by`, `logo_url`, `created_at`, `updated_at`) 
SELECT '吉他社', 'Guitar Society', '文化艺术', '吉他社聚集了热爱音乐的同学，每周举办教学和演出活动。', 2021, 'ACTIVE', u.id, 'https://placeholder.com/guitar.png', NOW(), NOW()
FROM `t_user` u WHERE u.username = 'club_admin';

INSERT INTO `t_club` (`name`, `short_name`, `category`, `description`, `founded_year`, `status`, `created_by`, `logo_url`, `created_at`, `updated_at`) 
SELECT '篮球协会', 'Basketball Assoc', '体育竞技', '篮球协会定期举办校内联赛，丰富同学们的课余生活。', 2019, 'ACTIVE', u.id, 'https://placeholder.com/basketball.png', NOW(), NOW()
FROM `t_user` u WHERE u.username = 'club_admin';

-- 5. Initialize Members (Club Admin as President)
INSERT INTO `t_member` (`club_id`, `user_id`, `role_code`, `status`, `points`, `join_at`, `created_at`, `updated_at`)
SELECT c.id, u.id, 'PRESIDENT', 'ACTIVE', 100, NOW(), NOW(), NOW()
FROM `t_club` c, `t_user` u 
WHERE c.name = '编程俱乐部' AND u.username = 'club_admin';

-- 6. Initialize Activities
INSERT INTO `t_activity` (`club_id`, `title`, `type`, `start_time`, `end_time`, `status`, `created_at`, `updated_at`) 
SELECT id, '新生编程挑战赛', '比赛', DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 8 DAY), 'OPEN', NOW(), NOW()
FROM `t_club` WHERE name = '编程俱乐部';

INSERT INTO `t_activity` (`club_id`, `title`, `type`, `start_time`, `end_time`, `status`, `created_at`, `updated_at`) 
SELECT id, 'Python入门讲座', '讲座', DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY), 'OPEN', NOW(), NOW()
FROM `t_club` WHERE name = '编程俱乐部';

INSERT INTO `t_activity` (`club_id`, `title`, `type`, `start_time`, `end_time`, `status`, `created_at`, `updated_at`) 
SELECT id, '草坪音乐节', '演出', DATE_ADD(NOW(), INTERVAL 14 DAY), DATE_ADD(NOW(), INTERVAL 14 DAY), 'OPEN', NOW(), NOW()
FROM `t_club` WHERE name = '吉他社';

-- 7. Initialize Recruit Batches
INSERT INTO `t_recruit_batch` (`club_id`, `title`, `start_time`, `end_time`, `quota`, `status`, `created_at`, `updated_at`)
SELECT id, '2024春季招新', DATE_ADD(NOW(), INTERVAL -1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 50, 'OPEN', NOW(), NOW()
FROM `t_club` WHERE name = '编程俱乐部';

-- 8. Initialize Form Fields for Batch
INSERT INTO `t_recruit_form_field` (`batch_id`, `field_key`, `label`, `type`, `required`, `sort_order`, `created_at`, `updated_at`)
SELECT b.id, 'reason', '申请理由', 'TEXT', 1, 1, NOW(), NOW()
FROM `t_recruit_batch` b WHERE b.title = '2024春季招新';

INSERT INTO `t_recruit_form_field` (`batch_id`, `field_key`, `label`, `type`, `required`, `sort_order`, `created_at`, `updated_at`)
SELECT b.id, 'skill', '掌握技能', 'TEXT', 1, 2, NOW(), NOW()
FROM `t_recruit_batch` b WHERE b.title = '2024春季招新';

-- 9. Initialize Notices
INSERT INTO `t_notice` (`club_id`, `title`, `content`, `scope`, `published_by`, `published_at`, `status`, `created_at`, `updated_at`)
SELECT c.id, '编程俱乐部招新启动', '欢迎各位同学报名参加！', 'PUBLIC', u.id, NOW(), 'PUBLISHED', NOW(), NOW()
FROM `t_club` c, `t_user` u 
WHERE c.name = '编程俱乐部' AND u.username = 'club_admin';

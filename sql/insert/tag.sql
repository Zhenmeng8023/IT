-- 插入顶级分类
INSERT INTO `tag` (`id`, `name`, `parent_id`, `category`, `description`, `created_at`)
VALUES
(1, '编程语言', NULL, 'language', '各类编程语言', NOW()),
(5, '框架', NULL, 'framework', '开发框架集合', NOW()),
(9, '工具', NULL, 'tool', '开发工具类', NOW());

-- 插入二级标签
INSERT INTO `tag` (`id`, `name`, `parent_id`, `category`, `description`, `created_at`)
VALUES
(2, 'Java', 1, 'language', '面向对象的编程语言', NOW()),
(3, 'Python', 1, 'language', '解释型高级语言', NOW()),
(4, 'JavaScript', 1, 'language', '前端/后端脚本语言', NOW()),
(6, 'Spring Boot', 5, 'framework', 'Java 微服务框架', NOW()),
(7, 'Django', 5, 'framework', 'Python Web 框架', NOW()),
(8, 'React', 5, 'framework', '前端 UI 库', NOW()),
(10, 'Git', 9, 'tool', '分布式版本控制', NOW()),
(11, 'Docker', 9, 'tool', '容器化平台', NOW()),
(12, 'Maven', 9, 'tool', 'Java 项目构建工具', NOW());
-- 插入省份
INSERT INTO `region` (`id`, `name`, `parent_id`, `level`, `code`, `created_at`)
VALUES
(1, '北京市', NULL, 'province', 'CN-110000', NOW()),
(4, '河北省', NULL, 'province', 'CN-130000', NOW());

-- 插入城市（依赖省份）
INSERT INTO `region` (`id`, `name`, `parent_id`, `level`, `code`, `created_at`)
VALUES
(5, '石家庄市', 4, 'city', 'CN-130100', NOW());

-- 插入区县（依赖城市或直辖市）
INSERT INTO `region` (`id`, `name`, `parent_id`, `level`, `code`, `created_at`)
VALUES
(2, '朝阳区', 1, 'district', 'CN-110105', NOW()),
(3, '海淀区', 1, 'district', 'CN-110108', NOW()),
(6, '长安区', 5, 'district', 'CN-130102', NOW()),
(7, '桥西区', 5, 'district', 'CN-130104', NOW());
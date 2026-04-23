-- =============================================
-- IT RBAC Front/Admin Seed (2026-04-20)
-- 目标：
-- 1) 重设 role / permission / menu / role_permission 关键关联
-- 2) user_info|userinfo 仅更新 role_id，不覆盖业务字段
-- 3) 兼容 role_permission(role_id,menu_id) 与 role_permission(role_id,permission_id) 两种结构
-- 4) 可重复执行
-- =============================================

SET NAMES utf8mb4;
SET @OLD_FOREIGN_KEY_CHECKS := @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

START TRANSACTION;

-- -----------------------------
-- 0) 结构探测
-- -----------------------------
SET @SCHEMA_NAME := DATABASE();

SET @USER_TABLE := (
    SELECT CASE
        WHEN EXISTS (
            SELECT 1
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @SCHEMA_NAME AND TABLE_NAME = 'user_info'
        ) THEN 'user_info'
        WHEN EXISTS (
            SELECT 1
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = @SCHEMA_NAME AND TABLE_NAME = 'userinfo'
        ) THEN 'userinfo'
        ELSE NULL
    END
);

SET @RP_HAS_MENU_ID := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @SCHEMA_NAME
      AND TABLE_NAME = 'role_permission'
      AND COLUMN_NAME = 'menu_id'
);

SET @RP_HAS_PERMISSION_ID := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @SCHEMA_NAME
      AND TABLE_NAME = 'role_permission'
      AND COLUMN_NAME = 'permission_id'
);

SET @RP_MODE := (
    CASE
        WHEN @RP_HAS_MENU_ID > 0 THEN 'ROLE_MENU'
        WHEN @RP_HAS_PERMISSION_ID > 0 THEN 'ROLE_PERMISSION'
        ELSE 'UNSUPPORTED'
    END
);

SELECT
    @SCHEMA_NAME AS schema_name,
    @USER_TABLE AS detected_user_table,
    @RP_MODE AS role_permission_mode;

-- -----------------------------
-- 1) 角色基础数据（固定 1~4）
-- -----------------------------
INSERT INTO `role` (`id`, `role_name`, `description`, `created_at`, `updated_at`) VALUES
    (1, '超级管理员', '拥有全部后台、前台及按钮权限', NOW(), NOW()),
    (2, '管理员', '拥有大部分后台管理权限', NOW(), NOW()),
    (3, '审查员', '拥有审核类后台权限与必要只读统计', NOW(), NOW()),
    (4, '用户', '前台登录后页面权限', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `role_name` = VALUES(`role_name`),
    `description` = VALUES(`description`),
    `updated_at` = NOW();

-- -----------------------------
-- 2) user_info|userinfo：仅更新 role_id
-- -----------------------------
SET @SQL_TEXT := IF(
    @USER_TABLE IS NULL,
    'SELECT ''[WARN] 未发现 user_info 或 userinfo，已跳过用户角色回写。'' AS warning_message',
    CONCAT(
        'UPDATE `', @USER_TABLE, '` ',
        'SET role_id = 4 ',
        'WHERE role_id IS NULL OR role_id NOT IN (1,2,3,4)'
    )
);
PREPARE STMT FROM @SQL_TEXT;
EXECUTE STMT;
DEALLOCATE PREPARE STMT;

SET @SQL_TEXT := IF(
    @USER_TABLE IS NULL,
    'SELECT ''[INFO] 跳过示例账号角色修正。'' AS info_message',
    CONCAT(
        'UPDATE `', @USER_TABLE, '` SET role_id = 1 ',
        'WHERE username IN (''admin'',''root'',''superadmin'')'
    )
);
PREPARE STMT FROM @SQL_TEXT;
EXECUTE STMT;
DEALLOCATE PREPARE STMT;

SET @SQL_TEXT := IF(
    @USER_TABLE IS NULL,
    'SELECT ''[INFO] 跳过示例账号角色修正。'' AS info_message',
    CONCAT(
        'UPDATE `', @USER_TABLE, '` SET role_id = 2 ',
        'WHERE username IN (''manager'',''admin2'',''ops'')'
    )
);
PREPARE STMT FROM @SQL_TEXT;
EXECUTE STMT;
DEALLOCATE PREPARE STMT;

SET @SQL_TEXT := IF(
    @USER_TABLE IS NULL,
    'SELECT ''[INFO] 跳过示例账号角色修正。'' AS info_message',
    CONCAT(
        'UPDATE `', @USER_TABLE, '` SET role_id = 3 ',
        'WHERE username IN (''reviewer'',''auditor'',''checker'')'
    )
);
PREPARE STMT FROM @SQL_TEXT;
EXECUTE STMT;
DEALLOCATE PREPARE STMT;

-- -----------------------------
-- 3) 清理并重建 permission / menu / role_permission
-- -----------------------------
DELETE FROM `role_permission`;
DELETE FROM `menu`;
DELETE FROM `permission`;

-- 权限：前台 view
INSERT INTO `permission` (`id`, `permission_code`, `description`, `created_at`) VALUES
    (1001, 'view:front:user:center', '前台-用户中心', NOW()),
    (1002, 'view:front:user:profile', '前台-个人资料', NOW()),
    (1003, 'view:front:user:collection', '前台-我的收藏', NOW()),
    (1004, 'view:front:user:history', '前台-浏览历史', NOW()),
    (1005, 'view:front:user:notification', '前台-消息通知', NOW()),
    (1006, 'view:front:blog:write', '前台-写博客', NOW()),
    (1007, 'view:front:project:mine', '前台-我的项目', NOW()),
    (1008, 'view:front:project:template', '前台-项目模板', NOW()),
    (1009, 'view:front:project:collection', '前台-项目收藏', NOW()),
    (1010, 'view:front:project:manage', '前台-项目管理', NOW()),
    (1011, 'view:front:finance:wallet', '前台-钱包', NOW()),
    (1012, 'view:front:finance:vip', '前台-VIP', NOW()),
    (1013, 'view:front:finance:orders', '前台-订单', NOW()),
    (1014, 'view:front:finance:payment', '前台-支付', NOW()),
    (1015, 'view:front:finance:coupons', '前台-优惠券', NOW()),

    -- 权限：后台 view
    (1101, 'view:admin:home', '后台-首页', NOW()),
    (1102, 'view:admin:dashboard', '后台-仪表盘', NOW()),
    (1103, 'view:admin:user:info', '后台-用户信息', NOW()),
    (1104, 'view:admin:user:account', '后台-账户管理', NOW()),
    (1105, 'view:admin:rbac:role', '后台-角色管理', NOW()),
    (1106, 'view:admin:rbac:menu', '后台-菜单管理', NOW()),
    (1107, 'view:admin:rbac:permission', '后台-权限管理', NOW()),
    (1108, 'view:admin:system:log', '后台-系统日志', NOW()),
    (1109, 'view:admin:system:notification', '后台-通知管理', NOW()),
    (1110, 'view:admin:finance:order', '后台-订单管理', NOW()),
    (1111, 'view:admin:finance:membership', '后台-会员管理', NOW()),
    (1112, 'view:admin:finance:coupon', '后台-优惠券管理', NOW()),
    (1113, 'view:admin:finance:withdraw', '后台-提现管理', NOW()),
    (1114, 'view:admin:blog:audit', '后台-博客审核', NOW()),
    (1115, 'view:admin:blog:recommend', '后台-博客推荐', NOW()),
    (1116, 'view:admin:content:tag', '后台-标签管理', NOW()),
    (1117, 'view:admin:circle:manage', '后台-圈子管理', NOW()),
    (1118, 'view:admin:circle:audit', '后台-圈子审核', NOW()),
    (1119, 'view:admin:project:audit', '后台-项目审核', NOW()),
    (1120, 'view:admin:project:offline', '后台-项目下架', NOW()),
    (1121, 'view:admin:project:recommend', '后台-项目推荐', NOW()),
    (1122, 'view:admin:ai:knowledge', '后台-知识库', NOW()),
    (1123, 'view:admin:ai:model', '后台-模型管理', NOW()),
    (1124, 'view:admin:ai:prompt', '后台-提示词模板', NOW()),
    (1125, 'view:admin:ai:log', '后台-AI日志', NOW()),
    (1305, 'view:front:ai:assistant', '前台-AI助手访问', NOW()),
    (1306, 'view:front:ai:kb:self', '前台-个人知识库查看', NOW()),
    (1307, 'edit:front:ai:kb:self', '前台-个人知识库编辑', NOW()),
    (1308, 'view:front:ai:kb:project', '前台-项目知识库查看', NOW()),
    (1309, 'edit:front:ai:kb:project', '前台-项目知识库编辑', NOW()),
    (1310, 'manage:front:ai:kb:member', '前台-知识库成员管理', NOW()),

    -- 权限：后台按钮 btn
    (1201, 'btn:admin:user:edit', '后台-用户编辑', NOW()),
    (1202, 'btn:admin:user:disable', '后台-用户禁用', NOW()),
    (1203, 'btn:admin:rbac:role:create', '后台-角色新增', NOW()),
    (1204, 'btn:admin:rbac:role:assign', '后台-角色授权', NOW()),
    (1205, 'btn:admin:rbac:menu:edit', '后台-菜单编辑', NOW()),
    (1206, 'btn:admin:rbac:permission:edit', '后台-权限编辑', NOW()),
    (1207, 'btn:admin:system:log:export', '后台-日志导出', NOW()),
    (1208, 'btn:admin:system:notification:send', '后台-通知发送', NOW()),
    (1209, 'btn:admin:finance:order:export', '后台-订单导出', NOW()),
    (1210, 'btn:admin:finance:membership:grant', '后台-会员发放', NOW()),
    (1211, 'btn:admin:finance:coupon:issue', '后台-优惠券发放', NOW()),
    (1212, 'btn:admin:finance:withdraw:review', '后台-提现审核', NOW()),
    (1213, 'btn:admin:blog:audit:approve', '后台-博客审核通过', NOW()),
    (1214, 'btn:admin:blog:audit:reject', '后台-博客审核驳回', NOW()),
    (1215, 'btn:admin:blog:recommend:set', '后台-博客推荐设置', NOW()),
    (1216, 'btn:admin:content:tag:edit', '后台-标签编辑', NOW()),
    (1217, 'btn:admin:circle:manage:edit', '后台-圈子管理编辑', NOW()),
    (1218, 'btn:admin:circle:audit:approve', '后台-圈子审核通过', NOW()),
    (1219, 'btn:admin:circle:audit:reject', '后台-圈子审核驳回', NOW()),
    (1220, 'btn:admin:project:audit:approve', '后台-项目审核通过', NOW()),
    (1221, 'btn:admin:project:audit:reject', '后台-项目审核驳回', NOW()),
    (1222, 'btn:admin:project:offline:execute', '后台-项目下架执行', NOW()),
    (1223, 'btn:admin:project:recommend:set', '后台-项目推荐设置', NOW()),
    (1224, 'btn:admin:ai:model:enable', '后台-AI模型启用', NOW()),
    (1225, 'btn:admin:ai:prompt:publish', '后台-提示词发布', NOW()),

    -- 权限：前台按钮 btn
    (1301, 'btn:front:blog:publish', '前台-发布博客', NOW()),
    (1302, 'btn:front:project:create', '前台-创建项目', NOW()),
    (1303, 'btn:front:project:submit', '前台-提交项目', NOW()),
    (1304, 'btn:front:finance:recharge', '前台-钱包充值', NOW());

-- 菜单（后台可见 menu，is_hidden=0；前台桥接/按钮 is_hidden=1）
INSERT INTO `menu`
(`id`, `name`, `parent_id`, `path`, `component`, `icon`, `sort_order`, `is_hidden`, `permission_id`, `created_at`) VALUES
    -- 后台首页
    (200, '后台首页', NULL, '/admin', 'layout/manage', 'el-icon-s-home', 10, 0, NULL, NOW()),
    (201, '首页', 200, '/admin/home', 'pages/f_homepage/homepage.vue', 'el-icon-house', 11, 0, 1101, NOW()),
    (202, '仪表盘', 200, '/admin/dashboard', 'pages/f_blogmanage/dashboard/dashboard.vue', 'el-icon-data-analysis', 12, 0, 1102, NOW()),

    -- 用户管理
    (210, '用户管理', NULL, '/admin/users', 'layout/manage', 'el-icon-user', 20, 0, NULL, NOW()),
    (211, '用户信息', 210, '/admin/users/info', 'pages/f_systemmanage/usermanage/info/info.vue', 'el-icon-user-solid', 21, 0, 1103, NOW()),
    (212, '账户管理', 210, '/admin/users/account', 'pages/f_systemmanage/usermanage/count/count.vue', 'el-icon-wallet', 22, 0, 1104, NOW()),

    -- 权限管理
    (220, '权限管理', NULL, '/admin/rbac', 'layout/manage', 'el-icon-lock', 30, 0, NULL, NOW()),
    (221, '角色管理', 220, '/admin/rbac/role', 'pages/f_systemmanage/role/role.vue', 'el-icon-s-custom', 31, 0, 1105, NOW()),
    (222, '菜单管理', 220, '/admin/rbac/menu', 'pages/f_systemmanage/menu/menu.vue', 'el-icon-menu', 32, 0, 1106, NOW()),
    (223, '权限管理', 220, '/admin/rbac/permission', 'pages/f_systemmanage/permission/permission.vue', 'el-icon-key', 33, 0, 1107, NOW()),

    -- 内容管理
    (230, '内容管理', NULL, '/admin/content', 'layout/manage', 'el-icon-notebook-2', 40, 0, NULL, NOW()),
    (231, '博客审核', 230, '/admin/content/blog/audit', 'pages/f_blogmanage/audit/audit.vue', 'el-icon-check', 41, 0, 1114, NOW()),
    (232, '博客推荐', 230, '/admin/content/blog/recommend', 'pages/f_blogmanage/algoreco/algoreco.vue', 'el-icon-star-on', 42, 0, 1115, NOW()),
    (233, '标签管理', 230, '/admin/content/tag', 'pages/f_systemmanage/label/label.vue', 'el-icon-collection-tag', 43, 0, 1116, NOW()),
    (234, '圈子管理', 230, '/admin/content/circle/manage', 'pages/f_circlemanage/circlemanage/circlemanage.vue', 'el-icon-s-management', 44, 0, 1117, NOW()),
    (235, '圈子审核', 230, '/admin/content/circle/audit', 'pages/f_circlemanage/circleaudit/circleaudit.vue', 'el-icon-s-check', 45, 0, 1118, NOW()),

    -- 项目管理
    (240, '项目管理', NULL, '/admin/project', 'layout/manage', 'el-icon-folder-opened', 50, 0, NULL, NOW()),
    (241, '项目审核', 240, '/admin/project/audit', 'pages/f_projectmanage/projectaudit/projectaudit.vue', 'el-icon-document-checked', 51, 0, 1119, NOW()),
    (242, '项目下架', 240, '/admin/project/offline', 'pages/f_projectmanage/projectmiss/projectmiss.vue', 'el-icon-remove-outline', 52, 0, 1120, NOW()),
    (243, '项目推荐', 240, '/admin/project/recommend', 'pages/f_projectmanage/algoreco/algoreco.vue', 'el-icon-trophy', 53, 0, 1121, NOW()),

    -- 财务管理
    (250, '财务管理', NULL, '/admin/finance', 'layout/manage', 'el-icon-money', 60, 0, NULL, NOW()),
    (251, '订单管理', 250, '/admin/finance/order', 'pages/f_systemmanage/order/order.vue', 'el-icon-tickets', 61, 0, 1110, NOW()),
    (252, '会员管理', 250, '/admin/finance/membership', 'pages/f_systemmanage/membership/membership.vue', 'el-icon-medal', 62, 0, 1111, NOW()),
    (253, '优惠券管理', 250, '/admin/finance/coupon', 'pages/f_systemmanage/coupon/couponmanage.vue', 'el-icon-s-ticket', 63, 0, 1112, NOW()),
    (254, '提现管理', 250, '/admin/finance/withdraw', 'pages/f_systemmanage/withdraw/withdraw.vue', 'el-icon-bank-card', 64, 0, 1113, NOW()),

    -- AI 管理
    (260, 'AI 管理', NULL, '/admin/ai', 'layout/manage', 'el-icon-cpu', 70, 0, NULL, NOW()),
    (261, '知识库治理', 260, '/admin/ai/knowledge-base', 'pages/ai/AdminKnowledgeGovernance.vue', 'el-icon-reading', 71, 0, 1122, NOW()),
    (262, '模型管理', 260, '/admin/ai/models', 'pages/ai/ModelAdmin.vue', 'el-icon-setting', 72, 0, 1123, NOW()),
    (263, '提示词模板', 260, '/admin/ai/prompts', 'pages/ai/PromptTemplate.vue', 'el-icon-document', 73, 0, 1124, NOW()),
    (264, 'AI 日志', 260, '/admin/ai/logs', 'pages/ai/AiLog.vue', 'el-icon-data-line', 74, 0, 1125, NOW()),
    (265, '用户知识库使用管理', 260, '/admin/ai/knowledge-usage', 'pages/ai/AdminKnowledgeUsage.vue', 'el-icon-user', 75, 0, 1122, NOW()),

    -- 系统管理
    (270, '系统管理', NULL, '/admin/system', 'layout/manage', 'el-icon-setting', 80, 0, NULL, NOW()),
    (271, '系统日志', 270, '/admin/system/log', 'pages/f_systemmanage/log/log.vue', 'el-icon-document-copy', 81, 0, 1108, NOW()),
    (272, '通知管理', 270, '/admin/system/notification', 'pages/f_systemmanage/notification/notification.vue', 'el-icon-bell', 82, 0, 1109, NOW()),

    -- 前台权限桥接菜单（隐藏，不参与后台导航）
    (300, '前台-用户中心', NULL, '/front/user/center', 'pages/Z_userpage/peoplehome.vue', 'el-icon-user', 200, 1, 1001, NOW()),
    (301, '前台-个人资料', NULL, '/front/user/profile', 'pages/Z_userpage/peoplehome.vue', 'el-icon-user-solid', 201, 1, 1002, NOW()),
    (302, '前台-我的收藏', NULL, '/front/user/collection', 'pages/Z_collectionpage/collectionpage.vue', 'el-icon-star-off', 202, 1, 1003, NOW()),
    (303, '前台-浏览历史', NULL, '/front/user/history', 'pages/Z_historypage/historypage.vue', 'el-icon-time', 203, 1, 1004, NOW()),
    (304, '前台-消息通知', NULL, '/front/user/notification', 'pages/Z_notification/notification.vue', 'el-icon-message', 204, 1, 1005, NOW()),
    (305, '前台-写博客', NULL, '/front/blog/write', 'pages/Z_blogwrite/blogwritepage.vue', 'el-icon-edit-outline', 205, 1, 1006, NOW()),
    (306, '前台-我的项目', NULL, '/front/project/mine', 'pages/f_project/myproject/myproject.vue', 'el-icon-folder', 206, 1, 1007, NOW()),
    (307, '前台-项目模板', NULL, '/front/project/template', 'pages/f_project/projecttemplates/index.vue', 'el-icon-document', 207, 1, 1008, NOW()),
    (308, '前台-项目收藏', NULL, '/front/project/collection', 'pages/f_project/projectcollection/projectcollection.vue', 'el-icon-collection', 208, 1, 1009, NOW()),
    (309, '前台-项目管理', NULL, '/front/project/manage', 'pages/f_project/projectmanage/projectmanage.vue', 'el-icon-s-operation', 209, 1, 1010, NOW()),
    (315, '前台-AI助手访问', NULL, '/front/ai/assistant', NULL, NULL, 215, 1, 1305, NOW()),
    (316, '前台-个人知识库查看', NULL, '/user/ai/knowledge', NULL, NULL, 216, 1, 1306, NOW()),
    (317, '前台-个人知识库编辑', NULL, '/user/ai/knowledge', NULL, NULL, 217, 1, 1307, NOW()),
    (318, '前台-项目知识库查看', NULL, '/projectmanage?tab=knowledge', NULL, NULL, 218, 1, 1308, NOW()),
    (319, '前台-项目知识库编辑', NULL, '/projectmanage?tab=knowledge', NULL, NULL, 219, 1, 1309, NOW()),
    (320, '前台-知识库成员管理', NULL, '/front/ai/knowledge-base/member', NULL, NULL, 220, 1, 1310, NOW()),
    (310, '前台-钱包', NULL, '/front/finance/wallet', 'pages/Z_wallet/wallet.vue', 'el-icon-wallet', 210, 1, 1011, NOW()),
    (311, '前台-VIP', NULL, '/front/finance/vip', 'pages/Z_vip/vip.vue', 'el-icon-medal-1', 211, 1, 1012, NOW()),
    (312, '前台-订单', NULL, '/front/finance/orders', 'pages/Z_userpage/orders_purchases.vue', 'el-icon-s-order', 212, 1, 1013, NOW()),
    (313, '前台-支付', NULL, '/front/finance/payment', 'pages/Z_payment/payment.vue', 'el-icon-bank-card', 213, 1, 1014, NOW()),
    (314, '前台-优惠券', NULL, '/front/finance/coupons', 'pages/Z_userpage/coupons.vue', 'el-icon-s-ticket', 214, 1, 1015, NOW()),

    -- 后台按钮桥接菜单（按钮权限，path/component 必须为 NULL）
    (401, '按钮-用户编辑', 211, NULL, NULL, NULL, 401, 1, 1201, NOW()),
    (402, '按钮-用户禁用', 212, NULL, NULL, NULL, 402, 1, 1202, NOW()),
    (403, '按钮-角色新增', 221, NULL, NULL, NULL, 403, 1, 1203, NOW()),
    (404, '按钮-角色授权', 221, NULL, NULL, NULL, 404, 1, 1204, NOW()),
    (405, '按钮-菜单编辑', 222, NULL, NULL, NULL, 405, 1, 1205, NOW()),
    (406, '按钮-权限编辑', 223, NULL, NULL, NULL, 406, 1, 1206, NOW()),
    (407, '按钮-日志导出', 271, NULL, NULL, NULL, 407, 1, 1207, NOW()),
    (408, '按钮-通知发送', 272, NULL, NULL, NULL, 408, 1, 1208, NOW()),
    (409, '按钮-订单导出', 251, NULL, NULL, NULL, 409, 1, 1209, NOW()),
    (410, '按钮-会员发放', 252, NULL, NULL, NULL, 410, 1, 1210, NOW()),
    (411, '按钮-优惠券发放', 253, NULL, NULL, NULL, 411, 1, 1211, NOW()),
    (412, '按钮-提现审核', 254, NULL, NULL, NULL, 412, 1, 1212, NOW()),
    (413, '按钮-博客审核通过', 231, NULL, NULL, NULL, 413, 1, 1213, NOW()),
    (414, '按钮-博客审核驳回', 231, NULL, NULL, NULL, 414, 1, 1214, NOW()),
    (415, '按钮-博客推荐设置', 232, NULL, NULL, NULL, 415, 1, 1215, NOW()),
    (416, '按钮-标签编辑', 233, NULL, NULL, NULL, 416, 1, 1216, NOW()),
    (417, '按钮-圈子管理编辑', 234, NULL, NULL, NULL, 417, 1, 1217, NOW()),
    (418, '按钮-圈子审核通过', 235, NULL, NULL, NULL, 418, 1, 1218, NOW()),
    (419, '按钮-圈子审核驳回', 235, NULL, NULL, NULL, 419, 1, 1219, NOW()),
    (420, '按钮-项目审核通过', 241, NULL, NULL, NULL, 420, 1, 1220, NOW()),
    (421, '按钮-项目审核驳回', 241, NULL, NULL, NULL, 421, 1, 1221, NOW()),
    (422, '按钮-项目下架执行', 242, NULL, NULL, NULL, 422, 1, 1222, NOW()),
    (423, '按钮-项目推荐设置', 243, NULL, NULL, NULL, 423, 1, 1223, NOW()),
    (424, '按钮-AI模型启用', 262, NULL, NULL, NULL, 424, 1, 1224, NOW()),
    (425, '按钮-提示词发布', 263, NULL, NULL, NULL, 425, 1, 1225, NOW()),

    -- 前台按钮桥接菜单（按钮权限，path/component 必须为 NULL）
    (431, '按钮-发布博客', 305, NULL, NULL, NULL, 431, 1, 1301, NOW()),
    (432, '按钮-创建项目', 306, NULL, NULL, NULL, 432, 1, 1302, NOW()),
    (433, '按钮-提交项目', 309, NULL, NULL, NULL, 433, 1, 1303, NOW()),
    (434, '按钮-钱包充值', 310, NULL, NULL, NULL, 434, 1, 1304, NOW());

-- -----------------------------
-- 4) 角色 -> 权限模板（按 permission_id 维护）
-- -----------------------------
DROP TEMPORARY TABLE IF EXISTS `tmp_role_permission_seed`;
CREATE TEMPORARY TABLE `tmp_role_permission_seed` (
    `role_id` INT NOT NULL,
    `permission_id` INT NOT NULL,
    PRIMARY KEY (`role_id`, `permission_id`)
) ENGINE = MEMORY;

-- 1 超级管理员：全部权限（后台 + 前台 + 按钮）
INSERT INTO `tmp_role_permission_seed` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `permission`;

-- 2 管理员：大部分后台权限（不含高危权限）
INSERT INTO `tmp_role_permission_seed` (`role_id`, `permission_id`) VALUES
    (2, 1101), (2, 1102), (2, 1103), (2, 1104), (2, 1105), (2, 1106), (2, 1107),
    (2, 1108), (2, 1109), (2, 1110), (2, 1111), (2, 1112), (2, 1113),
    (2, 1114), (2, 1115), (2, 1116), (2, 1117), (2, 1118),
    (2, 1119), (2, 1120), (2, 1121), (2, 1122), (2, 1123), (2, 1124), (2, 1125),
    (2, 1201), (2, 1204), (2, 1205), (2, 1206), (2, 1207), (2, 1208),
    (2, 1209), (2, 1210), (2, 1211), (2, 1212), (2, 1213), (2, 1214),
    (2, 1215), (2, 1216), (2, 1217), (2, 1218), (2, 1219),
    (2, 1220), (2, 1221), (2, 1223), (2, 1224), (2, 1225);

-- 3 审查员：审核类后台权限 + 必要只读统计
INSERT INTO `tmp_role_permission_seed` (`role_id`, `permission_id`) VALUES
    (3, 1101), (3, 1102), (3, 1114), (3, 1115), (3, 1116), (3, 1118), (3, 1119),
    (3, 1213), (3, 1214), (3, 1218), (3, 1219), (3, 1220), (3, 1221);

-- 4 普通用户：前台登录后页面权限 + 前台按钮
INSERT INTO `tmp_role_permission_seed` (`role_id`, `permission_id`)
SELECT 4, `id`
FROM `permission`
WHERE `permission_code` LIKE 'view:front:%'
   OR `permission_code` LIKE 'btn:front:%';

-- -----------------------------
-- 5) 兼容写入 role_permission
-- -----------------------------
SET @SQL_TEXT := CASE
    WHEN @RP_MODE = 'ROLE_MENU' THEN
        'INSERT INTO `role_permission` (`role_id`, `menu_id`) SELECT t.role_id, m.id FROM `tmp_role_permission_seed` t JOIN `menu` m ON m.permission_id = t.permission_id'
    WHEN @RP_MODE = 'ROLE_PERMISSION' THEN
        'INSERT INTO `role_permission` (`role_id`, `permission_id`) SELECT role_id, permission_id FROM `tmp_role_permission_seed`'
    ELSE
        'SELECT ''[WARN] role_permission 表结构不支持（既无 menu_id 也无 permission_id），请先迁移结构后重跑。'' AS warning_message'
END;

PREPARE STMT FROM @SQL_TEXT;
EXECUTE STMT;
DEALLOCATE PREPARE STMT;

DROP TEMPORARY TABLE IF EXISTS `tmp_role_permission_seed`;

-- -----------------------------
-- 6) AUTO_INCREMENT 调整
-- -----------------------------
ALTER TABLE `role` AUTO_INCREMENT = 5;
ALTER TABLE `permission` AUTO_INCREMENT = 1400;
ALTER TABLE `menu` AUTO_INCREMENT = 500;

SET @SQL_TEXT := IF(
    @USER_TABLE IS NULL,
    'SELECT ''[INFO] 跳过 user 表 AUTO_INCREMENT 调整。'' AS info_message',
    CONCAT(
        'SET @NEXT_USER_ID := (SELECT COALESCE(MAX(id), 0) + 1 FROM `', @USER_TABLE, '`);'
    )
);
PREPARE STMT FROM @SQL_TEXT;
EXECUTE STMT;
DEALLOCATE PREPARE STMT;

SET @SQL_TEXT := IF(
    @USER_TABLE IS NULL,
    'SELECT ''[INFO] 跳过 user 表 AUTO_INCREMENT 调整。'' AS info_message',
    CONCAT(
        'ALTER TABLE `', @USER_TABLE, '` AUTO_INCREMENT = ', COALESCE(@NEXT_USER_ID, 1)
    )
);
PREPARE STMT FROM @SQL_TEXT;
EXECUTE STMT;
DEALLOCATE PREPARE STMT;

COMMIT;
SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;

-- -----------------------------
-- 7) 迁移建议（默认不执行）
-- -----------------------------
-- A) 若你要把 role_permission 正式更名为 role_menu（需同步后端实体）：
--    RENAME TABLE `role_permission` TO `role_menu`;
--    并把 Role.java 的 @JoinTable(name = "role_permission") 改为 role_menu。
--
-- B) 若当前库仍是 role_permission(role_id, permission_id) 老结构，建议迁移到新结构：
--    1) 新建 role_menu(role_id, menu_id)
--    2) 按 permission_id -> menu.id 回填
--    3) 应用侧改为角色-菜单模型

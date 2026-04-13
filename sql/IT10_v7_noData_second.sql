-- 2026-04-13 Notification Center idempotent migration
-- Target: MySQL 8.0+, database: it9_data

CREATE DATABASE IF NOT EXISTS `it9_data` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `it9_data`;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `receiver_id` BIGINT NOT NULL,
    `sender_id` BIGINT NULL,
    `category` VARCHAR(50) NOT NULL DEFAULT 'system',
    `type` VARCHAR(50) NOT NULL DEFAULT 'system',
    `title` VARCHAR(120) NOT NULL DEFAULT '',
    `content` TEXT NOT NULL,
    `read_status` TINYINT(1) NOT NULL DEFAULT 0,
    `read_at` TIMESTAMP NULL DEFAULT NULL,
    `target_type` VARCHAR(80) NULL,
    `target_id` BIGINT NULL,
    `source_type` VARCHAR(80) NULL,
    `source_id` BIGINT NULL,
    `event_key` VARCHAR(191) NULL,
    `action_url` VARCHAR(500) NULL,
    `business_status` VARCHAR(30) NOT NULL DEFAULT 'open',
    `priority` INT NOT NULL DEFAULT 0,
    `payload_json` JSON NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `expires_at` TIMESTAMP NULL DEFAULT NULL,
    `deleted_at` TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notification_event_key` (`event_key`),
    KEY `idx_notification_receiver_read_created` (`receiver_id`, `read_status`, `created_at` DESC),
    KEY `idx_notification_receiver_category_created` (`receiver_id`, `category`, `created_at` DESC),
    KEY `idx_notification_admin_query` (`deleted_at`, `receiver_id`, `sender_id`, `category`, `type`, `created_at` DESC),
    KEY `idx_notification_source` (`source_type`, `source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Unified in-app notifications';

CREATE TABLE IF NOT EXISTS `notification_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(80) NOT NULL,
    `category` VARCHAR(50) NOT NULL DEFAULT 'system',
    `type` VARCHAR(80) NOT NULL DEFAULT 'system',
    `title_template` VARCHAR(200) NOT NULL,
    `content_template` TEXT NOT NULL,
    `action_url_template` VARCHAR(500) NULL,
    `default_priority` INT NOT NULL DEFAULT 0,
    `enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `remark` VARCHAR(500) NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notification_template_code` (`code`),
    KEY `idx_notification_template_query` (`deleted_at`, `category`, `type`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Notification templates';

CREATE TABLE IF NOT EXISTS `notification_event_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `event_key` VARCHAR(191) NOT NULL,
    `category` VARCHAR(50) NOT NULL,
    `type` VARCHAR(50) NOT NULL,
    `sender_id` BIGINT NULL,
    `source_type` VARCHAR(80) NULL,
    `source_id` BIGINT NULL,
    `target_type` VARCHAR(80) NULL,
    `target_id` BIGINT NULL,
    `receiver_count` INT NOT NULL DEFAULT 0,
    `status` VARCHAR(30) NOT NULL DEFAULT 'created',
    `error_message` VARCHAR(500) NULL,
    `payload_json` JSON NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notification_event_log_key` (`event_key`),
    KEY `idx_notification_event_log_source` (`source_type`, `source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Notification event log';

CREATE TABLE IF NOT EXISTS `notification_delivery` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `notification_id` BIGINT NOT NULL,
    `channel` VARCHAR(30) NOT NULL DEFAULT 'in_app',
    `status` VARCHAR(30) NOT NULL DEFAULT 'pending',
    `retry_count` INT NOT NULL DEFAULT 0,
    `last_error` VARCHAR(500) NULL,
    `sent_at` TIMESTAMP NULL DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notification_delivery_channel` (`notification_id`, `channel`),
    KEY `idx_notification_delivery_status_created` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Notification delivery status';

INSERT INTO `notification_template`
(`code`, `category`, `type`, `title_template`, `content_template`, `action_url_template`, `default_priority`, `enabled`, `remark`)
VALUES
('system.notice.created', 'system', 'system', '系统通知', '{{content}}', '{{actionUrl}}', 0, 1, '管理员系统通知'),
('project.invitation.created', 'invite', 'project_invitation', '项目邀请', '{{senderName}} 邀请你加入项目「{{projectName}}」', '/myproject?tab=invitations&invitationId={{sourceId}}', 5, 1, '项目邀请'),
('project.join.request.created', 'request', 'project_join_request', '加入申请', '{{senderName}} 申请加入项目「{{projectName}}」', '/projectmanage?projectId={{projectId}}&tab=member-manage&requestId={{sourceId}}', 5, 1, '项目加入申请'),
('comment.blog.created', 'interaction', 'comment', '新的评论', '{{senderName}} 评论了你的内容「{{targetTitle}}」', '/blog/{{blogId}}?commentId={{commentId}}&highlight=true', 0, 1, '评论通知')
ON DUPLICATE KEY UPDATE
    `category` = VALUES(`category`),
    `type` = VALUES(`type`),
    `title_template` = VALUES(`title_template`),
    `content_template` = VALUES(`content_template`),
    `action_url_template` = VALUES(`action_url_template`),
    `default_priority` = VALUES(`default_priority`),
    `enabled` = VALUES(`enabled`),
    `remark` = VALUES(`remark`);

-- Menu and permission seed. Adjust parent_id if your system menu id differs.
INSERT INTO `permission` (`permission_code`, `description`)
SELECT 'view:notification', '允许访问后台消息通知管理'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `permission_code` = 'view:notification');

INSERT INTO `permission` (`permission_code`, `description`)
SELECT 'btn:notification:send', '允许管理员发送系统通知'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `permission_code` = 'btn:notification:send');

INSERT INTO `permission` (`permission_code`, `description`)
SELECT 'btn:notification:delete', '允许管理员删除通知'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `permission_code` = 'btn:notification:delete');

INSERT INTO `permission` (`permission_code`, `description`)
SELECT 'btn:notification:template', '允许管理员维护通知模板'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `permission_code` = 'btn:notification:template');

INSERT INTO `menu` (`name`, `path`, `component`, `icon`, `parent_id`, `sort_order`, `is_hidden`, `permission_id`)
SELECT '消息通知管理', '/notificationmanage', 'pages/f_systemmanage/notification/notification.vue', 'el-icon-message-solid',
       (SELECT `id` FROM `menu` WHERE `path` = '/system' LIMIT 1), 95, 0,
       (SELECT `id` FROM `permission` WHERE `permission_code` = 'view:notification' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM `menu` WHERE `path` = '/notificationmanage');

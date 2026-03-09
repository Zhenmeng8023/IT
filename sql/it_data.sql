-- 1. 基础数据表：这些表不依赖于其他自定义表，应最先创建

-- 角色表 (role) - 用于用户权限管理
CREATE TABLE role (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID，主键',
    role_name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称，例如：管理员、审核员、普通用户',
    description TEXT COMMENT '角色的详细描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '角色创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '角色信息最后更新时间'
) COMMENT = '用户角色表';

-- 权限点表 (permission) - 用于角色权限管理
CREATE TABLE permission (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID，主键',
    permission_code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限代码，例如：blog:review, user:disable',
    description TEXT COMMENT '权限的详细描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '权限创建时间'
) COMMENT = '系统权限点表';

-- 地区信息表 (region) - 用于用户地理位置
CREATE TABLE region (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '地区ID，主键',
    name VARCHAR(100) NOT NULL COMMENT '地区名称，例如：北京市、朝阳区',
    parent_id BIGINT DEFAULT NULL COMMENT '父级地区ID，用于构建省市区三级树状结构，顶级区域为NULL',
    level ENUM('province', 'city', 'district') NOT NULL COMMENT '地区层级：province-省, city-市, district-区县',
    code VARCHAR(20) COMMENT '地区编码，例如国标码 CN-110000',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    FOREIGN KEY (parent_id) REFERENCES region(id) ON DELETE CASCADE
) COMMENT = '地区信息表，支持多级行政区域划分';

-- 标签信息表 (tag) - 用于博客、项目等分类
CREATE TABLE tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID，主键',
    name VARCHAR(50) NOT NULL COMMENT '标签名称，例如：Java, Spring Boot, Web开发',
    parent_id BIGINT DEFAULT NULL COMMENT '父级标签ID，用于构建技术分类树，顶级分类为NULL',
    category ENUM('tech', 'language', 'framework', 'tool', 'other') DEFAULT 'tech' COMMENT '标签大类，方便顶层分类',
    description TEXT COMMENT '标签的详细描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '标签创建时间',
    FOREIGN KEY (parent_id) REFERENCES tag(id) ON DELETE CASCADE
) COMMENT = '标签信息表，支持树状层级结构，用于对博客和项目进行分类';

-- 2. 核心用户表：依赖于 role 和 region
-- 用户基本信息表 (user_info) - 系统核心，被多个表引用
CREATE TABLE user_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID，主键',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名，唯一标识',
    password_hash VARCHAR(255) NOT NULL COMMENT '用户登录密码的哈希值',
    email VARCHAR(100) UNIQUE COMMENT '用户邮箱，唯一',
    phone VARCHAR(20) COMMENT '用户手机号',
    region_id BIGINT COMMENT '所属地区ID，关联region表',
    status ENUM('active', 'inactive', 'deleted') DEFAULT 'active' COMMENT '账户状态：active-正常, inactive-禁用, deleted-已注销',
    identity_card VARCHAR(18) UNIQUE COMMENT '身份证号，唯一绑定，用于实名认证',
    role_id INT DEFAULT 1 COMMENT '用户角色ID，关联role表，决定用户权限',
    last_active_at TIMESTAMP NULL DEFAULT NULL COMMENT '最后活跃时间，用于计算用户活跃度',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '账号创建时间',
    last_login_at TIMESTAMP NULL DEFAULT NULL COMMENT '最后登录时间',
    login_count INT DEFAULT 0 COMMENT '累计登录次数',
    avatar_url VARCHAR(500) COMMENT '用户头像URL地址',
    bio TEXT COMMENT '用户个人简介或签名',
    FOREIGN KEY (region_id) REFERENCES region(id) ON DELETE SET NULL,
    FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE RESTRICT
) COMMENT = '用户基本信息表';

-- 3. 系统配置表：依赖于 user_info
-- 菜单表 (menu) - 用于权限控制
CREATE TABLE menu (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '菜单ID，主键',
    name VARCHAR(100) NOT NULL COMMENT '菜单名称',
    parent_id INT DEFAULT NULL COMMENT '父级菜单ID，用于构建菜单树，顶级菜单为NULL',
    path VARCHAR(200) COMMENT '前端路由路径',
    component VARCHAR(200) COMMENT '前端组件路径',
    icon VARCHAR(50) COMMENT '菜单图标',
    sort_order INT DEFAULT 0 COMMENT '排序序号',
    is_hidden BOOLEAN DEFAULT FALSE COMMENT '是否隐藏：TRUE-隐藏, FALSE-显示',
    permission_id INT COMMENT '关联的权限ID，用于控制菜单可见性',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '菜单创建时间',
    FOREIGN KEY (parent_id) REFERENCES menu(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permission(id) ON DELETE SET NULL
) COMMENT = '前端菜单及权限配置表';

-- 4. 内容与项目表：依赖于 user_info
-- 项目资源表 (project) - 依赖 user_info
CREATE TABLE project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '项目ID，主键',
    name VARCHAR(100) NOT NULL COMMENT '项目名称',
    description TEXT COMMENT '项目详细描述',
    category VARCHAR(50) COMMENT '项目分类，例如：Web应用、移动应用、工具类',
    size_mb DECIMAL(10,2) DEFAULT 0 COMMENT '项目文件大小，单位MB',
    stars INT DEFAULT 0 COMMENT '项目星标数/点赞数',
    downloads INT DEFAULT 0 COMMENT '项目总下载次数',
    views INT DEFAULT 0 COMMENT '项目总浏览次数',
    author_id BIGINT NOT NULL COMMENT '项目创建者ID，关联user_info表',
    status ENUM('draft', 'pending', 'published', 'rejected', 'archived') DEFAULT 'draft' COMMENT '项目状态：draft-草稿, pending-待审核, published-已发布, rejected-已驳回, archived-已归档',
    tags JSON COMMENT '项目关联的标签ID列表，以JSON数组形式存储，例如 [3, 4, 6]',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '项目创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '项目最后更新时间',
    FOREIGN KEY (author_id) REFERENCES user_info(id) ON DELETE CASCADE,
    INDEX idx_author_status (author_id, status)
) COMMENT = '项目资源表';

-- 博客内容表 (blog) - 依赖 user_info 和 project
CREATE TABLE blog (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '博客ID，主键',
    title VARCHAR(200) NOT NULL COMMENT '博客标题',
    content LONGTEXT NOT NULL COMMENT '博客正文内容',
    cover_image_url VARCHAR(500) COMMENT '封面图片URL地址',
    tags JSON COMMENT '博客关联的标签ID列表，以JSON数组形式存储，例如 [1, 2, 5]',
    author_id BIGINT NOT NULL COMMENT '作者ID，关联user_info表',
    project_id BIGINT DEFAULT NULL COMMENT '关联的项目ID，如果博客是介绍某个项目，则关联此处',
    status ENUM('draft', 'pending', 'published', 'rejected', 'archived') DEFAULT 'draft' COMMENT '博客状态：draft-草稿, pending-待审核, published-已发布, rejected-已驳回, archived-已归档',
    is_marked BOOLEAN DEFAULT FALSE COMMENT '是否被标记为待审核，用于快速筛选未处理内容',
    publish_time TIMESTAMP NULL DEFAULT NULL COMMENT '博客正式发布时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '博客创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '博客最后更新时间',
    view_count INT DEFAULT 0 COMMENT '浏览量',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    collect_count INT DEFAULT 0 COMMENT '收藏数',
    download_count INT DEFAULT 0 COMMENT '下载数',
    FOREIGN KEY (author_id) REFERENCES user_info(id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE SET NULL,
    INDEX idx_author_status (author_id, status),
    INDEX idx_publish_time (publish_time),
    INDEX idx_is_marked (is_marked)
) COMMENT = '博客内容表';

-- 5. 项目附属表：依赖于 project
-- 项目附件文件表 (project_file) - 依赖 project
CREATE TABLE project_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件ID，主键',
    project_id BIGINT NOT NULL COMMENT '所属项目ID，关联project表',
    file_name VARCHAR(255) NOT NULL COMMENT '文件原始名称',
    file_path VARCHAR(500) NOT NULL COMMENT '文件在服务器上的存储路径',
    file_size_bytes BIGINT COMMENT '文件大小，单位字节(Bytes)',
    file_type VARCHAR(50) COMMENT '文件类型，例如 zip, jar, war, exe, pdf',
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '文件上传时间',
    is_main BOOLEAN DEFAULT FALSE COMMENT '是否为主要文件，默认FALSE。一个项目应有且仅有一个主文件',
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    UNIQUE KEY uk_project_file_name (project_id, file_name) COMMENT '确保同一项目内文件名不重复'
) COMMENT = '项目附件文件表，存储项目的各种资源文件';

-- 6. 社交与圈子表：依赖于 user_info
-- 兴趣圈子表 (circle) - 依赖 user_info
CREATE TABLE circle (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '圈子ID，主键',
    name VARCHAR(100) NOT NULL COMMENT '圈子名称',
    type ENUM('official', 'private', 'public') DEFAULT 'public' COMMENT '圈子类型：official-官方, private-私密, public-公开',
    description TEXT COMMENT '圈子描述',
    creator_id BIGINT NOT NULL COMMENT '创建者ID，关联user_info表',
    visibility ENUM('public', 'private') DEFAULT 'public' COMMENT '可见性：public-公开, private-私有',
    max_members INT DEFAULT 500 COMMENT '最大成员数量限制',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '圈子创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '圈子最后更新时间',
    FOREIGN KEY (creator_id) REFERENCES user_info(id) ON DELETE CASCADE
) COMMENT = '兴趣圈子表，支持群聊功能';

-- 圈子成员关系及权限表 (circle_member) - 依赖 circle 和 user_info
CREATE TABLE circle_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '成员关系ID，主键',
    circle_id BIGINT NOT NULL COMMENT '圈子ID，关联circle表',
    user_id BIGINT NOT NULL COMMENT '用户ID，关联user_info表',
    join_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '加入圈子的时间',
    status ENUM('active', 'banned', 'left') DEFAULT 'active' COMMENT '成员状态：active-活跃, banned-被封禁, left-已退出',
    role ENUM('owner', 'admin', 'moderator', 'member') DEFAULT 'member' COMMENT '成员角色：owner-圈主, admin-管理员, moderator-版主, member-普通成员',
    FOREIGN KEY (circle_id) REFERENCES circle(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE,
    UNIQUE KEY uk_circle_user (circle_id, user_id) COMMENT '确保一个用户在一个圈子中只有一条记录',
    INDEX idx_user_status (user_id, status)
) COMMENT = '圈子成员关系及权限表';

-- 7. 通信相关表：依赖于 user_info 和 circle (如果需要)
-- 私信/群聊会话表 (conversation) - 依赖 user_info (可选)
CREATE TABLE conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID，主键',
    type ENUM('private', 'group') NOT NULL COMMENT '会话类型：private-私信, group-群聊',
    name VARCHAR(100) COMMENT '会话名称（群聊时使用）',
    creator_id BIGINT COMMENT '创建者ID（群聊时使用）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '会话最后更新时间（最后一条消息时间）',
    FOREIGN KEY (creator_id) REFERENCES user_info(id) ON DELETE SET NULL
) COMMENT = '私信/群聊会话表';

-- 私信/群聊消息表 (message) - 依赖 conversation 和 user_info
CREATE TABLE message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID，主键',
    conversation_id BIGINT NOT NULL COMMENT '所属会话ID，关联conversation表',
    sender_id BIGINT NOT NULL COMMENT '发送者ID，关联user_info表',
    content TEXT NOT NULL COMMENT '消息内容',
    message_type ENUM('text', 'image', 'file', 'emoji') DEFAULT 'text' COMMENT '消息类型',
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '消息发送时间',
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES user_info(id) ON DELETE CASCADE,
    INDEX idx_conversation_sent (conversation_id, sent_at)
) COMMENT = '私信/群聊消息表';

-- 8. 交互与记录表：依赖于 user_info, blog, project, circle, conversation (部分可选)
-- 评论表 (comment) - 依赖 user_info 和 comment (self-referencing)
CREATE TABLE comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID，主键',
    content TEXT NOT NULL COMMENT '评论正文内容',
    parent_comment_id BIGINT DEFAULT NULL COMMENT '父级评论ID，用于实现多级评论回复，顶级评论为NULL',
    -- post_type ENUM('blog', 'circle') NOT NULL COMMENT '被评论的内容类型：blog-博客, circle-圈子动态/聊天',
    post_id BIGINT NOT NULL COMMENT '被评论内容的ID',
    author_id BIGINT NOT NULL COMMENT '评论者ID，关联user_info表',
    likes INT DEFAULT 0 COMMENT '该条评论的点赞数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '评论创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '评论最后更新时间',
    status ENUM('normal', 'hidden', 'deleted') DEFAULT 'normal' COMMENT '评论状态：normal-正常显示, hidden-隐藏, deleted-已删除',
    FOREIGN KEY (author_id) REFERENCES user_info(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_comment_id) REFERENCES comment(id) ON DELETE CASCADE,
    -- INDEX idx_post_type_post_id (post_type, post_id) COMMENT '按内容类型和ID快速查找评论',
    INDEX idx_post_type_post_id (post_id) COMMENT '按ID快速查找评论',
    INDEX idx_author (author_id),
    INDEX idx_parent (parent_comment_id)
) COMMENT = '评论表，支持对博客和圈子进行评论和回复';

-- 用户点赞行为记录表 (like_record) - 依赖 user_info
CREATE TABLE like_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '点赞记录ID，主键',
    user_id BIGINT NOT NULL COMMENT '点赞用户ID，关联user_info表',
    target_type ENUM('blog', 'comment') NOT NULL COMMENT '被点赞的目标类型：blog-博客, comment-评论',
    target_id BIGINT NOT NULL COMMENT '被点赞目标的ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '点赞发生时间',
    UNIQUE KEY uk_user_target (user_id, target_type, target_id) COMMENT '防止同一用户重复对同一目标点赞',
    FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE,
    -- 外键约束根据target_type动态指向不同表，这里只做通用约束
    INDEX idx_target (target_type, target_id)
) COMMENT = '用户点赞行为记录表';

-- 用户收藏夹记录表 (collect_record) - 依赖 user_info
CREATE TABLE collect_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏记录ID，主键',
    user_id BIGINT NOT NULL COMMENT '收藏者ID，关联user_info表',
    target_type ENUM('blog', 'project') NOT NULL COMMENT '被收藏的目标类型：blog-博客, project-项目',
    target_id BIGINT NOT NULL COMMENT '被收藏目标的ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    UNIQUE KEY uk_user_target (user_id, target_type, target_id) COMMENT '防止同一用户重复收藏同一内容',
    FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE,
    -- 外键约束根据target_type动态指向不同表，这里只做通用约束
    INDEX idx_target (target_type, target_id)
) COMMENT = '用户收藏夹记录表';

-- 内容浏览日志表 (view_log) - 依赖 user_info
CREATE TABLE view_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '浏览记录ID，主键',
    user_id BIGINT COMMENT '浏览者ID，关联user_info表，匿名浏览则为NULL',
    target_type ENUM('blog', 'project') NOT NULL COMMENT '被浏览的目标类型：blog-博客, project-项目',
    target_id BIGINT NOT NULL COMMENT '被浏览目标的ID',
    ip_address VARCHAR(45) COMMENT '浏览者IP地址，用于统计和安全分析',
    user_agent TEXT COMMENT '用户代理字符串，记录浏览器等客户端信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '浏览发生时间',
    FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE SET NULL,
    INDEX idx_user_target (user_id, target_type, target_id),
    INDEX idx_created_at (created_at DESC)
) COMMENT = '内容浏览日志表，用于统计和数据分析';

-- 用户举报内容记录表 (report) - 依赖 user_info
CREATE TABLE report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '举报ID，主键',
    reporter_id BIGINT NOT NULL COMMENT '举报人ID，关联user_info表',
    target_type ENUM('blog', 'project', 'comment', 'user', 'circle') NOT NULL COMMENT '被举报的目标类型：blog-博客, project-项目, comment-评论, user-用户, circle-圈子',
    target_id BIGINT NOT NULL COMMENT '被举报目标的ID',
    reason VARCHAR(500) NOT NULL COMMENT '举报原因描述',
    status ENUM('pending', 'processed', 'ignored') DEFAULT 'pending' COMMENT '处理状态：pending-待处理, processed-已处理, ignored-已忽略',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '举报提交时间',
    processed_at TIMESTAMP NULL DEFAULT NULL COMMENT '处理完成时间',
    processor_id BIGINT DEFAULT NULL COMMENT '处理该举报的管理员ID，关联user_info表',
    FOREIGN KEY (reporter_id) REFERENCES user_info(id) ON DELETE CASCADE,
    FOREIGN KEY (processor_id) REFERENCES user_info(id) ON DELETE SET NULL,
    INDEX idx_status (status),
    INDEX idx_target (target_type, target_id)
) COMMENT = '用户举报内容记录表';

-- 用户关注关系表（单向） (follow) - 依赖 user_info
CREATE TABLE follow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关注关系ID，主键',
    follower_id BIGINT NOT NULL COMMENT '关注者ID，关联user_info表',
    followee_id BIGINT NOT NULL COMMENT '被关注者ID，关联user_info表',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '关注建立时间',
    status ENUM('active', 'blocked') DEFAULT 'active' COMMENT '关注状态：active-正常关注, blocked-已拉黑',
    FOREIGN KEY (follower_id) REFERENCES user_info(id) ON DELETE CASCADE,
    FOREIGN KEY (followee_id) REFERENCES user_info(id) ON DELETE CASCADE,
    UNIQUE KEY uk_follower_followee (follower_id, followee_id) COMMENT '确保关注关系唯一',
    INDEX idx_followee (followee_id)
) COMMENT = '用户关注关系表（单向）';

-- 用户好友关系表（双向确认） (friendship) - 依赖 user_info
CREATE TABLE friendship (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '好友关系ID，主键',
    user1_id BIGINT NOT NULL COMMENT '用户A的ID，关联user_info表',
    user2_id BIGINT NOT NULL COMMENT '用户B的ID，关联user_info表',
    status ENUM('pending', 'accepted', 'rejected', 'blocked') DEFAULT 'pending' COMMENT '好友请求状态',
    request_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '请求发起时间',
    response_time TIMESTAMP NULL DEFAULT NULL COMMENT '请求响应时间',
    FOREIGN KEY (user1_id) REFERENCES user_info(id) ON DELETE CASCADE,
    FOREIGN KEY (user2_id) REFERENCES user_info(id) ON DELETE CASCADE,
    -- 使用检查约束确保 user1_id <= user2_id
    -- CHECK (user1_id <= user2_id), -- MySQL 8.0.16+ 支持
    -- 在应用层保证此约束
    -- 唯一索引确保一对用户只有一个记录
    UNIQUE KEY uk_user_pair (user1_id, user2_id),
    INDEX idx_user1_status (user1_id, status),
    INDEX idx_user2_status (user2_id, status)
) COMMENT = '用户好友关系表（双向确认）';

-- 9. 关系中间表：依赖于上面的多个表
-- 角色与权限的多对多关联表 (role_permission) - 依赖 role and permission
CREATE TABLE role_permission (
    role_id INT NOT NULL COMMENT '角色ID',
    permission_id INT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permission(id) ON DELETE CASCADE
) COMMENT = '角色与权限的多对多关联表';

-- 用户技能标签关联表 (user_skill) - 依赖 user_info and tag
CREATE TABLE user_skill (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID，主键',
    user_id BIGINT NOT NULL COMMENT '用户ID，关联user_info表',
    tag_id BIGINT NOT NULL COMMENT '技能标签ID，关联tag表',
    proficiency_level ENUM('beginner', 'intermediate', 'advanced', 'expert') DEFAULT 'intermediate' COMMENT '熟练程度',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '关联创建时间',
    FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_tag (user_id, tag_id) COMMENT '确保一个用户与一个技能标签只有一条记录'
) COMMENT = '用户技能标签关联表，用于展示用户擅长的技术';

-- 10. 通知与日志表：依赖于 user_info, blog, project, comment, circle, conversation (部分可选)
-- 用户消息通知中心表 (notification) - 依赖 user_info
-- 注意：这里移除了对 blog(id) 的硬性外键，因为 target_id 可能指向多种类型，应在应用层逻辑处理
CREATE TABLE notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID，主键',
    receiver_id BIGINT NOT NULL COMMENT '接收者ID，关联user_info表',
    sender_id BIGINT DEFAULT NULL COMMENT '发送者ID，关联user_info表，系统通知则为NULL',
    type ENUM('comment', 'like', 'follow', 'reply', 'system', 'friend_request', 'message') NOT NULL COMMENT '通知类型：comment-评论, like-点赞, follow-关注, reply-回复, system-系统通知, friend_request-好友请求, message-新消息',
    content TEXT NOT NULL COMMENT '通知的具体内容',
    read_status BOOLEAN DEFAULT FALSE COMMENT '阅读状态：FALSE-未读, TRUE-已读',
    target_type ENUM('blog', 'project', 'comment', 'circle', 'conversation') DEFAULT NULL COMMENT '被操作的目标类型',
    target_id BIGINT DEFAULT NULL COMMENT '被操作的目标ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '通知创建时间',
    FOREIGN KEY (receiver_id) REFERENCES user_info(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES user_info(id) ON DELETE SET NULL
    -- 修正：外键约束根据target_type指向不同表，这里不做硬性约束，由应用层保证
    -- FOREIGN KEY (target_id) REFERENCES blog(id) ON DELETE SET NULL,
    -- 示例，实际需要动态处理或通过业务层保证
    -- 此处不加外键约束
    -- 但保留索引
    , INDEX idx_receiver_read (receiver_id, read_status) COMMENT '快速查找用户的未读通知',
    INDEX idx_created_at (created_at DESC)
) COMMENT = '用户消息通知中心表';

-- 用户行为详细记录表 (user_behavior) - 依赖 user_info
CREATE TABLE user_behavior (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '行为记录ID，主键',
    user_id BIGINT NOT NULL COMMENT '用户ID，关联user_info表',
    behavior_type ENUM('login', 'logout', 'view', 'like', 'collect', 'comment', 'share', 'upload', 'download') NOT NULL COMMENT '行为类型',
    target_type ENUM('blog', 'project', 'comment', 'circle', 'user') COMMENT '行为对象类型',
    target_id BIGINT COMMENT '行为对象ID',
    extra_data JSON COMMENT '额外的行为数据，如搜索关键词、操作参数等',
    ip_address VARCHAR(45) COMMENT '用户IP地址',
    user_agent TEXT COMMENT '用户代理字符串',
    occurred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '行为发生时间',
    FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE,
    INDEX idx_user_behavior (user_id, behavior_type),
    INDEX idx_occurred_at (occurred_at DESC)
) COMMENT = '用户行为详细记录表，用于分析和推荐';

-- 系统操作审计日志表 (audit_log) - 依赖 user_info
CREATE TABLE audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '审计日志ID，主键',
    user_id BIGINT COMMENT '操作者ID，关联user_info表，系统操作则为NULL',
    action VARCHAR(50) NOT NULL COMMENT '执行的操作，例如：create_blog, delete_project, update_user_profile',
    target_type VARCHAR(50) COMMENT '操作目标的类型，例如：blog, project, user',
    target_id BIGINT COMMENT '操作目标的ID',
    ip_address VARCHAR(45) COMMENT '操作者的IP地址',
    user_agent TEXT COMMENT '操作者使用的浏览器或客户端信息',
    details JSON COMMENT '操作的详细信息，以JSON格式记录，例如旧值、新值等',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '操作发生时间',
    FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE SET NULL
) COMMENT = '系统操作审计日志表，用于安全审计和问题追溯';

-- 11. 推荐与搜索表：依赖于 user_info
-- 为用户生成的个性化推荐列表缓存表 (recommendation_result) - 依赖 user_info
CREATE TABLE recommendation_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '推荐结果ID，主键',
    user_id BIGINT NOT NULL COMMENT '目标用户ID，关联user_info表',
    algorithm_version VARCHAR(50) NOT NULL COMMENT '生成本次推荐的算法版本号',
    recommended_items JSON NOT NULL COMMENT '推荐的项目列表，JSON格式，例如 [{"type": "blog", "id": 1}, {"type": "project", "id": 2}]',
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '推荐结果生成时间',
    consumed BOOLEAN DEFAULT FALSE COMMENT '是否已被消费（例如：是否已推送给用户）',
    consumed_at TIMESTAMP NULL DEFAULT NULL COMMENT '消费时间',
    FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE,
    INDEX idx_user_generated (user_id, generated_at DESC),
    INDEX idx_consumed (consumed)
) COMMENT = '为用户生成的个性化推荐列表缓存表';

-- 推荐系统规则配置表 (recommendation_rule)
CREATE TABLE recommendation_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '规则ID，主键',
    name VARCHAR(100) NOT NULL COMMENT '规则名称',
    description TEXT COMMENT '规则描述',
    rule_type ENUM('content_based', 'collaborative', 'hot_trending', 'user_based') DEFAULT 'content_based' COMMENT '推荐算法类型：content_based-基于内容, collaborative-协同过滤, hot_trending-热门趋势, user_based-基于用户',
    weight DECIMAL(5,2) DEFAULT 1.0 COMMENT '该规则在综合推荐中的权重',
    enabled BOOLEAN DEFAULT TRUE COMMENT '规则是否启用：TRUE-启用, FALSE-禁用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '规则创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '规则最后更新时间'
) COMMENT = '推荐系统规则配置表';

-- 全文搜索引擎的索引表 (search_index)
CREATE TABLE search_index (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '索引ID，主键',
    content TEXT COMMENT '用于搜索的文本内容',
    doc_type ENUM('blog', 'project', 'user') NOT NULL COMMENT '文档类型：blog-博客, project-项目, user-用户',
    doc_id BIGINT NOT NULL COMMENT '对应文档的ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '索引创建时间',
    FULLTEXT INDEX idx_content (content) COMMENT '为内容字段创建全文索引，提升搜索性能'
) COMMENT = '全文搜索引擎的索引表，用于加速内容检索';
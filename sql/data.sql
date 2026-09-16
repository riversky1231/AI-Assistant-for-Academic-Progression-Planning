INSERT INTO school (id, name, province, city, level, description) VALUES
    (1, '厦门大学', '福建', '厦门', '985', '综合性研究型大学。'),
    (2, '福州大学', '福建', '福州', '211', '国家双一流建设高校。'),
    (3, '华侨大学', '福建', '泉州', '省重点', '面向海内外学生的综合性大学。'),
    (4, '福建师范大学', '福建', '福州', '省重点', '师范教育特色鲜明的综合性大学。'),
    (5, '集美大学', '福建', '厦门', '省重点', '航海、水产与工程学科特色突出。'),
    (6, '浙江大学', '浙江', '杭州', '985', '综合性研究型大学。'),
    (7, '杭州电子科技大学', '浙江', '杭州', '省重点', '电子信息与计算机学科优势明显。'),
    (8, '宁波大学', '浙江', '宁波', '双一流', '综合性大学。'),
    (9, '上海大学', '上海', '上海', '211', '上海市属综合性大学。'),
    (10, '东华大学', '上海', '上海', '211', '材料、纺织与信息学科协调发展。'),
    (11, '苏州大学', '江苏', '苏州', '211', '江苏省属综合性大学。'),
    (12, '南京邮电大学', '江苏', '南京', '双一流', '信息通信与电子信息特色高校。'),
    (13, '北京邮电大学', '北京', '北京', '211', '信息通信与计算机学科特色高校。'),
    (14, '哈尔滨工业大学', '黑龙江', '哈尔滨', '985', '以工科见长的研究型大学。'),
    (15, '武汉理工大学', '湖北', '武汉', '211', '材料、交通与工程学科特色高校。'),
    (16, '中南大学', '湖南', '长沙', '985', '综合性研究型大学。'),
    (17, '合肥工业大学', '安徽', '合肥', '211', '工程教育与科研特色高校。'),
    (18, '河海大学', '江苏', '南京', '211', '水利、环境与工程学科特色高校。');

INSERT INTO major (school_id, name, category, description)
SELECT s.id, templates.name, '工学', templates.description
FROM school s
CROSS JOIN (
    SELECT '计算机科学与技术' AS name, '学习计算机系统、软件与算法基础。' AS description
    UNION ALL SELECT '软件工程', '培养软件分析、设计、开发与测试能力。'
    UNION ALL SELECT '人工智能', '学习机器学习、数据分析与智能系统基础。'
) templates
ON DUPLICATE KEY UPDATE
    category = VALUES(category),
    description = VALUES(description);

INSERT INTO admission (school_id, major_id, province, subject_type, year, min_score, min_rank)
SELECT ranked.school_id,
       ranked.major_id,
       '福建',
       '物理类',
       2025,
       GREATEST(500, 650 - FLOOR(ranked.final_rank / 300)),
       ranked.final_rank
FROM (
    SELECT s.id AS school_id,
           m.id AS major_id,
           CASE s.name
               WHEN '厦门大学' THEN 2800
               WHEN '福州大学' THEN 12500
               WHEN '华侨大学' THEN 19000
               WHEN '福建师范大学' THEN 21000
               WHEN '集美大学' THEN 26000
               WHEN '浙江大学' THEN 900
               WHEN '杭州电子科技大学' THEN 14500
               WHEN '宁波大学' THEN 18000
               WHEN '上海大学' THEN 8500
               WHEN '东华大学' THEN 11000
               WHEN '苏州大学' THEN 6200
               WHEN '南京邮电大学' THEN 10500
               WHEN '北京邮电大学' THEN 5000
               WHEN '哈尔滨工业大学' THEN 3800
               WHEN '武汉理工大学' THEN 9500
               WHEN '中南大学' THEN 4200
               WHEN '合肥工业大学' THEN 10500
               WHEN '河海大学' THEN 11000
           END
           + CASE m.name
               WHEN '计算机科学与技术' THEN 0
               WHEN '软件工程' THEN 800
               ELSE 1400
           END AS final_rank
    FROM school s
    JOIN major m ON m.school_id = s.id
) ranked
ON DUPLICATE KEY UPDATE
    min_score = VALUES(min_score),
    min_rank = VALUES(min_rank);

INSERT INTO sys_user (id, username, password_hash, enabled) VALUES
    (1, 'admin', 'pbkdf2_sha256$120000$2IS1AXELLN4mCwn9TmRCgA==$ahceD1SndLoDTO6DCJc/ELoqocUk/6EQ1TEWjyr5Ww0=', TRUE)
ON DUPLICATE KEY UPDATE enabled = VALUES(enabled);

INSERT INTO sys_role (id, role_code, role_name) VALUES
    (1, 'admin', '管理员')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

INSERT INTO sys_permission (id, permission_code, permission_name) VALUES
    (1, 'school:read', '查看院校数据'),
    (2, 'recommend:use', '使用冲稳保推荐')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (1, 1);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES (1, 1), (1, 2);

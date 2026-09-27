SET NAMES utf8mb4;

-- ============================================================
-- 1. 丰富学校描述 (现有 18 所)
-- ============================================================
UPDATE school SET description = '厦门大学是教育部直属的全国重点大学，由爱国华侨领袖陈嘉庚先生于1921年创办，是中国近代教育史上第一所华侨创办的大学。学校是国家"211工程"和"985工程"重点建设的高水平大学，入选国家"双一流"建设高校。校园依山傍海，被誉为中国最美大学之一。经济学、化学、海洋科学、管理学等学科实力雄厚。' WHERE name = '厦门大学';
UPDATE school SET description = '福州大学是国家"双一流"建设高校、国家"211工程"重点建设大学。学校创建于1958年，以工为主、理工结合，化学、材料科学、工程学等学科进入ESI全球前1%。物理与信息工程学院、计算机与大数据学院是优势学科群。' WHERE name = '福州大学';
UPDATE school SET description = '华侨大学是中央统战部直属高校，面向海内外办学，在泉州和厦门设有校区。学校以工为主，土木建筑、机械工程、旅游管理等学科特色鲜明，拥有国家级特色专业建设点。海外学生比例高，国际化氛围浓厚。' WHERE name = '华侨大学';
UPDATE school SET description = '福建师范大学是福建省人民政府与教育部共建高校，前身是1907年创办的福建优级师范学堂。教育学、中国语言文学、数学等学科历史悠久，师范教育特色鲜明。是福建省文科强校和重点文科人才培养基地。' WHERE name = '福建师范大学';
UPDATE school SET description = '集美大学地处厦门，由著名爱国华侨领袖陈嘉庚先生创办。学校以航海、水产、轮机工程为特色，交通运输工程学科在福建省领先。同时发展了计算机、经济管理等应用型学科，毕业生就业率长期居全省前列。' WHERE name = '集美大学';
UPDATE school SET description = '浙江大学是中国顶尖的综合性研究型大学，位列"C9联盟"和"985工程"前列。学科门类齐全，工学、农学、医学、理学均居全国前列。计算机科学与技术、电气工程、控制科学与工程等工科实力顶尖，竺可桢学院培养拔尖创新人才。' WHERE name = '浙江大学';
UPDATE school SET description = '杭州电子科技大学是浙江省与国防科技工业局共建的电子信息特色高校。计算机科学与技术、电子信息工程、自动化等专业就业率和薪资水平在浙江省名列前茅，被称为"IT人才摇篮"。' WHERE name = '杭州电子科技大学';
UPDATE school SET description = '宁波大学是国家"双一流"建设高校，由世界船王包玉刚先生捐资创立。力学学科入选一流学科，水产、信息与通信工程等学科实力较强。地处经济发达的长三角南翼，校企合作资源丰富。' WHERE name = '宁波大学';
UPDATE school SET description = '上海大学是上海市属、国家"211工程"重点建设的综合性大学。学校实施大类招生、通识教育，设有钱伟长学院等拔尖人才培养特区。社会学、美术学、机械电子等学科在上海市领先。' WHERE name = '上海大学';
UPDATE school SET description = '东华大学是教育部直属、国家"211工程"重点建设高校，前身为中国纺织大学。纺织科学与工程、材料科学与工程、设计学为传统优势学科，服装设计与工程全国知名。' WHERE name = '东华大学';
UPDATE school SET description = '苏州大学是江苏省属重点综合性大学、国家"211工程"重点建设高校。学校前身是创建于1900年的东吴大学，法学、教育学、纺织、材料等学科实力突出，是江苏省招生规模最大的"211"高校之一。' WHERE name = '苏州大学';
UPDATE school SET description = '南京邮电大学是国家"双一流"建设高校，以信息通信为特色。信息与通信工程、电子科学与技术、计算机科学与技术等学科在国内享有盛誉，被誉为"华夏IT英才的摇篮"。' WHERE name = '南京邮电大学';
UPDATE school SET description = '北京邮电大学是教育部直属、工业和信息化部共建的全国重点大学，"211工程"重点建设高校。信息与通信工程学科全国第一，计算机科学与技术实力雄厚，毕业生在IT行业就业前景极佳。' WHERE name = '北京邮电大学';
UPDATE school SET description = '哈尔滨工业大学是工信部直属全国重点大学，"985工程"和"C9联盟"成员。以航天、机器人、焊接等工科见长，机械工程、控制科学与工程、计算机科学与技术等学科位居全国前列。设有深圳校区。' WHERE name = '哈尔滨工业大学';
UPDATE school SET description = '武汉理工大学是教育部直属全国重点大学、国家"211工程"重点建设高校。材料科学与工程、交通运输工程、船舶与海洋工程等学科特色鲜明，是建材、交通、汽车三大行业的人才培养基地。' WHERE name = '武汉理工大学';
UPDATE school SET description = '中南大学是教育部直属全国重点大学、"985工程"和"双一流"建设高校。冶金工程、矿业工程、材料科学与工程实力顶尖，湘雅医学院享誉海内外（"北协和、南湘雅"）。计算机、土木等工科也很强。' WHERE name = '中南大学';
UPDATE school SET description = '合肥工业大学是教育部直属全国重点大学、国家"211工程"重点建设高校。管理科学与工程学科全国领先，机械工程、计算机科学与技术、电气工程等工科专业实力雄厚，被誉为"汽车行业的黄埔军校"。' WHERE name = '合肥工业大学';
UPDATE school SET description = '河海大学是教育部直属全国重点大学、国家"211工程"重点建设高校，以水利为特色。水利工程学科全国第一，土木工程、环境科学与工程、海洋科学等学科实力突出。' WHERE name = '河海大学';

-- ============================================================
-- 2. 新增学校 (约30所，覆盖更多省份和层次)
-- ============================================================
INSERT IGNORE INTO school (id, name, province, city, level, description) VALUES
    (19, '中山大学', '广东', '广州', '985', '中山大学是教育部直属综合性全国重点大学，由孙中山先生于1924年创办。学校有广州、珠海、深圳三个校区，哲学、中文、历史、数学、物理、化学、生物、医学等学科均为国内一流。岭南学院和光华医学院久负盛名。'),
    (20, '华南理工大学', '广东', '广州', '985', '华南理工大学是教育部直属全国重点大学，"985工程"和"双一流"建设高校。轻工技术与工程、建筑学、食品科学与工程等学科全国领先，被誉为"工程师的摇篮"和"企业家的摇篮"。'),
    (21, '暨南大学', '广东', '广州', '211', '暨南大学是中央统战部直属高校，面向海内外办学。新闻传播学、经济学、药学、中文等学科实力突出，华侨高等教育特色鲜明，是中国第一所由国家创办的华侨高等学府。'),
    (22, '深圳大学', '广东', '深圳', '省重点', '深圳大学是深圳市属综合性大学，伴随深圳经济特区成长。依托粤港澳大湾区产业优势，计算机科学与技术、光学工程、建筑学、经济学等学科发展迅速，创新创业氛围浓厚。'),
    (23, '四川大学', '四川', '成都', '985', '四川大学是教育部直属全国重点大学，"985工程"和"双一流"建设高校。学科门类齐全，口腔医学全国第一，中文、数学、化学、材料、水利等学科均为国内顶尖水平。华西医学中心历史悠久。'),
    (24, '电子科技大学', '四川', '成都', '985', '电子科技大学是教育部直属全国重点大学，"985工程"重点建设高校。电子科学与技术、信息与通信工程两大学科全国顶尖，被誉为"中国电子类院校的排头兵"。'),
    (25, '西南交通大学', '四川', '成都', '211', '西南交通大学是教育部直属全国重点大学、国家"211工程"重点建设高校。交通运输工程学科全国第一，土木工程、机械工程等传统优势学科实力雄厚，是中国轨道交通事业的发祥地。'),
    (26, '武汉大学', '湖北', '武汉', '985', '武汉大学是教育部直属综合性全国重点大学，"985工程"和"双一流"建设高校。测绘科学与技术、遥感科学与技术、水利工程、口腔医学等学科全国顶尖，法学、马克思主义理论等文科也很强。校园被誉为中国最美大学。'),
    (27, '华中科技大学', '湖北', '武汉', '985', '华中科技大学是教育部直属全国重点大学，"985工程"和"双一流"建设高校。机械工程、光学工程、电气工程、公共卫生与预防医学等学科全国领先，被誉为"新中国高等教育的缩影"。同济医学院实力雄厚。'),
    (28, '西安交通大学', '陕西', '西安', '985', '西安交通大学是教育部直属全国重点大学，"985工程"和"C9联盟"成员。动力工程及工程热物理、电气工程、机械工程、管理科学与工程等学科全国顶尖，是中国最早兴办的高等学府之一。'),
    (29, '西北工业大学', '陕西', '西安', '985', '西北工业大学是工信部直属全国重点大学，"985工程"重点建设高校。以航空、航天、航海（三航）为特色，材料科学与工程、计算机科学与技术等学科实力雄厚。'),
    (30, '西安电子科技大学', '陕西', '西安', '211', '西安电子科技大学是教育部直属全国重点大学，"211工程"重点建设高校。电子科学与技术、信息与通信工程、计算机科学与技术等学科在国内享有盛誉，被誉为"信息人才的摇篮"。'),
    (31, '山东大学', '山东', '济南', '985', '山东大学是教育部直属综合性全国重点大学，"985工程"和"双一流"建设高校。数学学科全国顶尖，中文、历史、物理、化学等基础学科实力雄厚。设有威海校区和青岛校区。'),
    (32, '中国海洋大学', '山东', '青岛', '985', '中国海洋大学是教育部直属全国重点大学，"985工程"重点建设高校。海洋科学和水产学科全国第一，食品科学与工程、环境科学与工程等学科也颇具特色。地处美丽的海滨城市青岛。'),
    (33, '南开大学', '天津', '天津', '985', '南开大学是教育部直属全国重点大学，"985工程"和"双一流"建设高校。理论经济学、数学、化学、历史学等学科全国领先，周恩来政府管理学院享有盛誉。与天津大学相邻，学风自由活跃。'),
    (34, '天津大学', '天津', '天津', '985', '天津大学是教育部直属全国重点大学，"985工程"重点建设高校，前身为北洋大学（中国第一所现代大学）。化学工程与技术学科全国第一，仪器科学与技术、建筑学、水利工程等传统优势学科实力雄厚。'),
    (35, '大连理工大学', '辽宁', '大连', '985', '大连理工大学是教育部直属全国重点大学，"985工程"重点建设高校。力学、化学工程与技术、水利工程、管理科学与工程等学科全国领先，地处美丽的海滨城市大连。'),
    (36, '东北大学', '辽宁', '沈阳', '985', '东北大学是教育部直属全国重点大学，"985工程"重点建设高校。控制科学与工程、冶金工程、计算机科学与技术等学科特色鲜明，是中国自动化和计算机学科的重要发源地。'),
    (37, '吉林大学', '吉林', '长春', '985', '吉林大学是教育部直属全国重点大学，"985工程"和"双一流"建设高校。由六所大学合并而成，学科门类齐全。化学、地质学、考古学、车辆工程、法学等学科全国领先。'),
    (38, '清华大学', '北京', '北京', '985', '清华大学是中国顶尖高等学府，"985工程"和"C9联盟"成员。工科综合实力全国第一，电子工程、计算机、建筑、土木、机械等学科均为世界一流水平。经管学院、公共管理学院等文科也在国内领先。'),
    (39, '北京大学', '北京', '北京', '985', '北京大学是中国最高学府之一，"985工程"和"C9联盟"成员。人文社科综合实力全国第一，数学、物理、化学等理科均为国内顶尖。光华管理学院、法学院、医学院等享有极高声誉。'),
    (40, '北京理工大学', '北京', '北京', '985', '北京理工大学是工信部直属全国重点大学，"985工程"重点建设高校。兵器科学与技术全国第一，信息与通信工程、机械工程、光学工程等学科实力雄厚，是国防科技人才培养的重要基地。'),
    (41, '北京航空航天大学', '北京', '北京', '985', '北京航空航天大学是工信部直属全国重点大学，"985工程"重点建设高校。航空宇航科学与技术、仪器科学与技术、软件工程、计算机科学与技术等学科全国领先，是中国航空航天事业的摇篮。'),
    (42, '复旦大学', '上海', '上海', '985', '复旦大学是教育部直属全国重点大学，"985工程"和"C9联盟"成员。哲学、理论经济学、中国语言文学、新闻传播学、数学等学科全国顶尖，上海医学院历史悠久。以"自由而无用的灵魂"著称。'),
    (43, '上海交通大学', '上海', '上海', '985', '上海交通大学是教育部直属全国重点大学，"985工程"和"C9联盟"成员。机械工程、船舶与海洋工程、临床医学等学科全国领先，安泰经济与管理学院、凯原法学院在国际上享有声誉。'),
    (44, '同济大学', '上海', '上海', '985', '同济大学是教育部直属全国重点大学，"985工程"重点建设高校。土木工程、建筑学、城乡规划等学科全国顶尖，交通运输工程、环境科学与工程、设计学等也颇具特色。中德合作渊源深厚。'),
    (45, '华东师范大学', '上海', '上海', '985', '华东师范大学是教育部直属全国重点大学，"985工程"重点建设高校。教育学、心理学、中国语言文学、软件工程等学科全国领先，是中国师范教育的最高学府之一。'),
    (46, '兰州大学', '甘肃', '兰州', '985', '兰州大学是教育部直属全国重点大学，"985工程"重点建设高校。化学、大气科学、生态学、核科学与技术等学科全国领先，虽地处西北但学术实力深厚，性价比高。'),
    (47, '云南大学', '云南', '昆明', '双一流', '云南大学是国家"双一流"建设高校、"211工程"重点建设大学。民族学、生态学、生物学等特色学科突出，地处四季如春的昆明，校园环境优美。'),
    (48, '郑州大学', '河南', '郑州', '双一流', '郑州大学是河南省唯一的国家"双一流"建设高校、"211工程"重点建设大学。化学、材料科学、临床医学等学科进入ESI全球前1%，是河南省高等教育的龙头。'),
    (49, '南昌大学', '江西', '南昌', '双一流', '南昌大学是国家"双一流"建设高校、"211工程"重点建设大学。食品科学与工程学科全国知名，材料科学、化学等学科实力较强，是江西省唯一进入"211工程"的高校。'),
    (50, '贵州大学', '贵州', '贵阳', '双一流', '贵州大学是国家"双一流"建设高校、"211工程"重点建设大学。植物保护学科入选一流学科，大数据、矿业工程等学科结合地方特色发展，是贵州省高等教育的旗舰。');

-- ============================================================
-- 3. 大量扩充专业 (每所学校增加更多专业)
-- ============================================================

-- 为所有学校新增工学类专业
INSERT INTO major (school_id, name, category, description)
SELECT s.id, t.name, t.category, t.description
FROM school s
CROSS JOIN (
    SELECT '电子信息工程' AS name, '工学' AS category, '学习电子信息系统设计、信号处理与嵌入式系统开发。' AS description
    UNION ALL SELECT '通信工程', '工学', '研究通信系统原理、无线通信技术与网络协议。'
    UNION ALL SELECT '自动化', '工学', '学习自动控制理论、机器人技术与工业过程自动化。'
    UNION ALL SELECT '电气工程及其自动化', '工学', '研究电力系统、电机与电力电子技术。'
    UNION ALL SELECT '机械工程', '工学', '学习机械设计、制造工艺与机电一体化技术。'
    UNION ALL SELECT '土木工程', '工学', '研究建筑结构、桥梁道路与地下工程设计与施工。'
    UNION ALL SELECT '材料科学与工程', '工学', '学习材料制备、性能分析与工程应用。'
    UNION ALL SELECT '数据科学与大数据技术', '工学', '融合统计学与计算机科学，培养大数据分析与挖掘能力。'
    UNION ALL SELECT '网络工程', '工学', '学习计算机网络体系结构、网络安全与云计算技术。'
    UNION ALL SELECT '信息安全', '工学', '研究密码学、网络安全与信息系统安全防护。'
    UNION ALL SELECT '环境工程', '工学', '学习水污染控制、大气治理与固体废物处理技术。'
    UNION ALL SELECT '建筑学', '工学', '培养建筑设计、城市规划与景观设计能力。'
    UNION ALL SELECT '化学工程与工艺', '工学', '研究化工过程设计、反应器优化与新材料制备。'
    UNION ALL SELECT '食品科学与工程', '工学', '学习食品加工、安全检测与营养学基础。'
    UNION ALL SELECT '生物医学工程', '工学', '融合工程学与医学，研究医疗器械与生物信息技术。'
) t
WHERE NOT EXISTS (
    SELECT 1 FROM major m WHERE m.school_id = s.id AND m.name = t.name
);

-- 为所有学校新增理学类专业
INSERT INTO major (school_id, name, category, description)
SELECT s.id, t.name, t.category, t.description
FROM school s
CROSS JOIN (
    SELECT '数学与应用数学' AS name, '理学' AS category, '学习数学分析、代数、概率统计及其应用。' AS description
    UNION ALL SELECT '物理学', '理学', '研究物质运动规律，涵盖力学、电磁学、量子物理等方向。'
    UNION ALL SELECT '化学', '理学', '学习无机化学、有机化学、分析化学与物理化学。'
    UNION ALL SELECT '生物科学', '理学', '研究生命现象与生命过程，涵盖分子生物学、遗传学等。'
    UNION ALL SELECT '统计学', '理学', '学习概率论、数理统计与数据分析方法。'
) t
WHERE NOT EXISTS (
    SELECT 1 FROM major m WHERE m.school_id = s.id AND m.name = t.name
);

-- 为所有学校新增经管类专业
INSERT INTO major (school_id, name, category, description)
SELECT s.id, t.name, t.category, t.description
FROM school s
CROSS JOIN (
    SELECT '经济学' AS name, '经济学' AS category, '学习微观经济学、宏观经济学与计量经济学基础。' AS description
    UNION ALL SELECT '金融学', '经济学', '研究金融市场、投资理论与风险管理。'
    UNION ALL SELECT '国际经济与贸易', '经济学', '学习国际贸易理论、跨国经营与国际金融。'
    UNION ALL SELECT '工商管理', '管理学', '培养企业管理、市场营销与战略规划能力。'
    UNION ALL SELECT '会计学', '管理学', '学习财务会计、审计与管理会计基础。'
    UNION ALL SELECT '财务管理', '管理学', '研究企业融资、投资决策与财务分析。'
) t
WHERE NOT EXISTS (
    SELECT 1 FROM major m WHERE m.school_id = s.id AND m.name = t.name
);

-- 为所有学校新增人文社科类专业
INSERT INTO major (school_id, name, category, description)
SELECT s.id, t.name, t.category, t.description
FROM school s
CROSS JOIN (
    SELECT '法学' AS name, '法学' AS category, '学习宪法、民法、刑法、商法等法律理论与实务。' AS description
    UNION ALL SELECT '英语', '文学', '培养英语语言文学素养与翻译、跨文化交际能力。'
    UNION ALL SELECT '汉语言文学', '文学', '学习中国古代文学、现当代文学与语言学。'
    UNION ALL SELECT '新闻传播学', '文学', '研究新闻采编、媒体传播与公共关系。'
    UNION ALL SELECT '教育学', '教育学', '学习教育学原理、课程设计与教育心理学。'
    UNION ALL SELECT '心理学', '教育学', '研究人类认知、发展心理与社会心理学。'
) t
WHERE NOT EXISTS (
    SELECT 1 FROM major m WHERE m.school_id = s.id AND m.name = t.name
);

-- 为部分 985 高校新增医学类专业
INSERT INTO major (school_id, name, category, description)
SELECT s.id, t.name, t.category, t.description
FROM school s
CROSS JOIN (
    SELECT '临床医学' AS name, '医学' AS category, '五年制培养临床诊疗能力，涵盖内外科、妇儿等方向。' AS description
    UNION ALL SELECT '口腔医学', '医学', '培养口腔疾病诊疗与预防保健能力。'
    UNION ALL SELECT '药学', '医学', '学习药物化学、药理学与药物制剂技术。'
) t
WHERE s.level = '985'
  AND NOT EXISTS (
    SELECT 1 FROM major m WHERE m.school_id = s.id AND m.name = t.name
);

-- ============================================================
-- 4. 补充 2023、2024 年录取数据 (历史年份趋势)
-- ============================================================

-- 2023 年福建物理类
INSERT IGNORE INTO admission (school_id, major_id, province, subject_type, year, min_score, min_rank)
SELECT ranked.school_id,
       ranked.major_id,
       '福建',
       '物理类',
       2023,
       GREATEST(490, 645 - FLOOR(ranked.final_rank / 300)),
       ranked.final_rank
FROM (
    SELECT s.id AS school_id,
           m.id AS major_id,
           CASE s.name
               WHEN '清华大学' THEN 50
               WHEN '北京大学' THEN 60
               WHEN '复旦大学' THEN 200
               WHEN '上海交通大学' THEN 180
               WHEN '浙江大学' THEN 900
               WHEN '中国科学技术大学' THEN 300
               WHEN '南京大学' THEN 500
               WHEN '厦门大学' THEN 2800
               WHEN '同济大学' THEN 1800
               WHEN '武汉大学' THEN 1200
               WHEN '华中科技大学' THEN 1500
               WHEN '中山大学' THEN 2200
               WHEN '四川大学' THEN 2600
               WHEN '西安交通大学' THEN 2000
               WHEN '哈尔滨工业大学' THEN 3600
               WHEN '北京理工大学' THEN 1600
               WHEN '北京航空航天大学' THEN 1400
               WHEN '天津大学' THEN 2400
               WHEN '南开大学' THEN 2100
               WHEN '大连理工大学' THEN 3400
               WHEN '山东大学' THEN 3000
               WHEN '吉林大学' THEN 4200
               WHEN '中南大学' THEN 4000
               WHEN '华南理工大学' THEN 3200
               WHEN '电子科技大学' THEN 2800
               WHEN '西北工业大学' THEN 3800
               WHEN '东北大学' THEN 5000
               WHEN '兰州大学' THEN 5200
               WHEN '福州大学' THEN 12500
               WHEN '北京邮电大学' THEN 5000
               WHEN '苏州大学' THEN 6200
               WHEN '上海大学' THEN 8500
               WHEN '南京邮电大学' THEN 10500
               WHEN '武汉理工大学' THEN 9500
               WHEN '合肥工业大学' THEN 10500
               WHEN '河海大学' THEN 11000
               WHEN '东华大学' THEN 11000
               WHEN '杭州电子科技大学' THEN 14500
               WHEN '宁波大学' THEN 18000
               WHEN '华侨大学' THEN 19000
               WHEN '福建师范大学' THEN 21000
               WHEN '集美大学' THEN 26000
               WHEN '西南交通大学' THEN 7000
               WHEN '西安电子科技大学' THEN 4800
               WHEN '暨南大学' THEN 8000
               WHEN '深圳大学' THEN 7500
               WHEN '中国海洋大学' THEN 4600
               WHEN '云南大学' THEN 10000
               WHEN '郑州大学' THEN 9000
               WHEN '南昌大学' THEN 11500
               WHEN '贵州大学' THEN 14000
               ELSE 15000
           END
           + CASE m.category
               WHEN '工学' THEN
                   CASE m.name
                       WHEN '计算机科学与技术' THEN 0
                       WHEN '软件工程' THEN 600
                       WHEN '人工智能' THEN 300
                       WHEN '数据科学与大数据技术' THEN 500
                       WHEN '电子信息工程' THEN 800
                       WHEN '通信工程' THEN 1000
                       WHEN '信息安全' THEN 700
                       WHEN '电气工程及其自动化' THEN 1200
                       WHEN '自动化' THEN 1400
                       WHEN '机械工程' THEN 2000
                       WHEN '土木工程' THEN 3500
                       WHEN '建筑学' THEN 2500
                       WHEN '材料科学与工程' THEN 2800
                       WHEN '环境工程' THEN 4000
                       WHEN '化学工程与工艺' THEN 3200
                       WHEN '食品科学与工程' THEN 4500
                       WHEN '生物医学工程' THEN 2200
                       WHEN '网络工程' THEN 900
                       ELSE 1500
                   END
               WHEN '理学' THEN
                   CASE m.name
                       WHEN '数学与应用数学' THEN 500
                       WHEN '物理学' THEN 1800
                       WHEN '化学' THEN 2500
                       WHEN '统计学' THEN 800
                       WHEN '生物科学' THEN 3000
                       ELSE 2000
                   END
               WHEN '医学' THEN
                   CASE m.name
                       WHEN '临床医学' THEN -500
                       WHEN '口腔医学' THEN -800
                       WHEN '药学' THEN 2000
                       ELSE 1500
                   END
               WHEN '经济学' THEN
                   CASE m.name
                       WHEN '金融学' THEN -200
                       WHEN '经济学' THEN 500
                       WHEN '国际经济与贸易' THEN 1500
                       ELSE 1000
                   END
               WHEN '管理学' THEN
                   CASE m.name
                       WHEN '会计学' THEN 800
                       WHEN '工商管理' THEN 1500
                       WHEN '财务管理' THEN 1200
                       ELSE 1800
                   END
               WHEN '法学' THEN 1000
               WHEN '文学' THEN 2500
               WHEN '教育学' THEN 2000
               ELSE 2000
           END AS final_rank
    FROM school s
    JOIN major m ON m.school_id = s.id
) ranked;

-- 2024 年福建物理类
INSERT IGNORE INTO admission (school_id, major_id, province, subject_type, year, min_score, min_rank)
SELECT ranked.school_id,
       ranked.major_id,
       '福建',
       '物理类',
       2024,
       GREATEST(495, 648 - FLOOR(ranked.final_rank / 300)),
       ranked.final_rank
FROM (
    SELECT s.id AS school_id,
           m.id AS major_id,
           CASE s.name
               WHEN '清华大学' THEN 55
               WHEN '北京大学' THEN 65
               WHEN '复旦大学' THEN 210
               WHEN '上海交通大学' THEN 190
               WHEN '浙江大学' THEN 880
               WHEN '厦门大学' THEN 2700
               WHEN '武汉大学' THEN 1250
               WHEN '华中科技大学' THEN 1550
               WHEN '中山大学' THEN 2300
               WHEN '四川大学' THEN 2500
               WHEN '西安交通大学' THEN 2100
               WHEN '哈尔滨工业大学' THEN 3700
               WHEN '北京理工大学' THEN 1650
               WHEN '北京航空航天大学' THEN 1350
               WHEN '天津大学' THEN 2300
               WHEN '南开大学' THEN 2200
               WHEN '大连理工大学' THEN 3500
               WHEN '山东大学' THEN 3100
               WHEN '吉林大学' THEN 4100
               WHEN '中南大学' THEN 4100
               WHEN '华南理工大学' THEN 3100
               WHEN '电子科技大学' THEN 2700
               WHEN '西北工业大学' THEN 3700
               WHEN '东北大学' THEN 4900
               WHEN '兰州大学' THEN 5300
               WHEN '福州大学' THEN 12000
               WHEN '北京邮电大学' THEN 4900
               WHEN '苏州大学' THEN 6000
               WHEN '上海大学' THEN 8200
               WHEN '南京邮电大学' THEN 10200
               WHEN '武汉理工大学' THEN 9200
               WHEN '合肥工业大学' THEN 10200
               WHEN '河海大学' THEN 10800
               WHEN '东华大学' THEN 10800
               WHEN '杭州电子科技大学' THEN 14000
               WHEN '宁波大学' THEN 17500
               WHEN '华侨大学' THEN 18500
               WHEN '福建师范大学' THEN 20500
               WHEN '集美大学' THEN 25500
               WHEN '西南交通大学' THEN 6800
               WHEN '西安电子科技大学' THEN 4600
               WHEN '暨南大学' THEN 7800
               WHEN '深圳大学' THEN 7200
               WHEN '中国海洋大学' THEN 4500
               WHEN '云南大学' THEN 9800
               WHEN '郑州大学' THEN 8800
               WHEN '南昌大学' THEN 11200
               WHEN '贵州大学' THEN 13500
               ELSE 14500
           END
           + CASE m.category
               WHEN '工学' THEN
                   CASE m.name
                       WHEN '计算机科学与技术' THEN 0
                       WHEN '软件工程' THEN 600
                       WHEN '人工智能' THEN 250
                       WHEN '数据科学与大数据技术' THEN 450
                       WHEN '电子信息工程' THEN 750
                       WHEN '通信工程' THEN 950
                       WHEN '信息安全' THEN 650
                       WHEN '电气工程及其自动化' THEN 1150
                       WHEN '自动化' THEN 1350
                       WHEN '机械工程' THEN 1900
                       WHEN '土木工程' THEN 3200
                       WHEN '建筑学' THEN 2400
                       WHEN '材料科学与工程' THEN 2700
                       WHEN '环境工程' THEN 3800
                       WHEN '化学工程与工艺' THEN 3000
                       WHEN '食品科学与工程' THEN 4200
                       WHEN '生物医学工程' THEN 2100
                       WHEN '网络工程' THEN 850
                       ELSE 1400
                   END
               WHEN '理学' THEN
                   CASE m.name
                       WHEN '数学与应用数学' THEN 450
                       WHEN '物理学' THEN 1700
                       WHEN '化学' THEN 2400
                       WHEN '统计学' THEN 750
                       WHEN '生物科学' THEN 2800
                       ELSE 1900
                   END
               WHEN '医学' THEN
                   CASE m.name
                       WHEN '临床医学' THEN -500
                       WHEN '口腔医学' THEN -800
                       WHEN '药学' THEN 1900
                       ELSE 1400
                   END
               WHEN '经济学' THEN
                   CASE m.name
                       WHEN '金融学' THEN -200
                       WHEN '经济学' THEN 500
                       WHEN '国际经济与贸易' THEN 1400
                       ELSE 900
                   END
               WHEN '管理学' THEN
                   CASE m.name
                       WHEN '会计学' THEN 750
                       WHEN '工商管理' THEN 1400
                       WHEN '财务管理' THEN 1100
                       ELSE 1700
                   END
               WHEN '法学' THEN 900
               WHEN '文学' THEN 2300
               WHEN '教育学' THEN 1900
               ELSE 1900
           END AS final_rank
    FROM school s
    JOIN major m ON m.school_id = s.id
) ranked;

-- 2023 年福建历史类
INSERT IGNORE INTO admission (school_id, major_id, province, subject_type, year, min_score, min_rank)
SELECT ranked.school_id,
       ranked.major_id,
       '福建',
       '历史类',
       2023,
       GREATEST(480, 630 - FLOOR(ranked.final_rank / 250)),
       ranked.final_rank
FROM (
    SELECT s.id AS school_id,
           m.id AS major_id,
           CASE s.name
               WHEN '清华大学' THEN 30
               WHEN '北京大学' THEN 35
               WHEN '复旦大学' THEN 120
               WHEN '上海交通大学' THEN 110
               WHEN '浙江大学' THEN 350
               WHEN '厦门大学' THEN 1200
               WHEN '武汉大学' THEN 600
               WHEN '中山大学' THEN 1000
               WHEN '四川大学' THEN 1100
               WHEN '南开大学' THEN 800
               WHEN '山东大学' THEN 1300
               WHEN '吉林大学' THEN 1600
               WHEN '中南大学' THEN 1500
               WHEN '福州大学' THEN 5500
               WHEN '苏州大学' THEN 2800
               WHEN '上海大学' THEN 3800
               WHEN '华侨大学' THEN 8000
               WHEN '福建师范大学' THEN 7000
               WHEN '集美大学' THEN 10000
               WHEN '暨南大学' THEN 3500
               WHEN '深圳大学' THEN 3200
               WHEN '云南大学' THEN 4500
               WHEN '郑州大学' THEN 4000
               WHEN '南昌大学' THEN 5000
               WHEN '贵州大学' THEN 6000
               ELSE 6500
           END
           + CASE m.category
               WHEN '经济学' THEN
                   CASE m.name
                       WHEN '金融学' THEN -100
                       WHEN '经济学' THEN 200
                       ELSE 800
                   END
               WHEN '管理学' THEN
                   CASE m.name
                       WHEN '会计学' THEN 300
                       WHEN '工商管理' THEN 800
                       WHEN '财务管理' THEN 500
                       ELSE 1000
                   END
               WHEN '法学' THEN 400
               WHEN '文学' THEN
                   CASE m.name
                       WHEN '汉语言文学' THEN 0
                       WHEN '英语' THEN 500
                       WHEN '新闻传播学' THEN 300
                       ELSE 700
                   END
               WHEN '教育学' THEN
                   CASE m.name
                       WHEN '教育学' THEN 200
                       WHEN '心理学' THEN 100
                       ELSE 500
                   END
               WHEN '工学' THEN 2000
               WHEN '理学' THEN
                   CASE m.name
                       WHEN '数学与应用数学' THEN 300
                       WHEN '统计学' THEN 500
                       ELSE 1500
                   END
               ELSE 1500
           END AS final_rank
    FROM school s
    JOIN major m ON m.school_id = s.id
) ranked;

-- 2024 年福建历史类
INSERT IGNORE INTO admission (school_id, major_id, province, subject_type, year, min_score, min_rank)
SELECT ranked.school_id,
       ranked.major_id,
       '福建',
       '历史类',
       2024,
       GREATEST(485, 635 - FLOOR(ranked.final_rank / 250)),
       ranked.final_rank
FROM (
    SELECT s.id AS school_id,
           m.id AS major_id,
           CASE s.name
               WHEN '清华大学' THEN 28
               WHEN '北京大学' THEN 32
               WHEN '复旦大学' THEN 115
               WHEN '上海交通大学' THEN 105
               WHEN '浙江大学' THEN 340
               WHEN '厦门大学' THEN 1150
               WHEN '武汉大学' THEN 580
               WHEN '中山大学' THEN 950
               WHEN '四川大学' THEN 1050
               WHEN '南开大学' THEN 780
               WHEN '山东大学' THEN 1250
               WHEN '吉林大学' THEN 1550
               WHEN '中南大学' THEN 1450
               WHEN '福州大学' THEN 5300
               WHEN '苏州大学' THEN 2700
               WHEN '上海大学' THEN 3600
               WHEN '华侨大学' THEN 7800
               WHEN '福建师范大学' THEN 6800
               WHEN '集美大学' THEN 9800
               WHEN '暨南大学' THEN 3400
               WHEN '深圳大学' THEN 3100
               WHEN '云南大学' THEN 4300
               WHEN '郑州大学' THEN 3800
               WHEN '南昌大学' THEN 4800
               WHEN '贵州大学' THEN 5800
               ELSE 6200
           END
           + CASE m.category
               WHEN '经济学' THEN
                   CASE m.name
                       WHEN '金融学' THEN -100
                       WHEN '经济学' THEN 200
                       ELSE 700
                   END
               WHEN '管理学' THEN
                   CASE m.name
                       WHEN '会计学' THEN 300
                       WHEN '工商管理' THEN 700
                       WHEN '财务管理' THEN 500
                       ELSE 900
                   END
               WHEN '法学' THEN 350
               WHEN '文学' THEN
                   CASE m.name
                       WHEN '汉语言文学' THEN 0
                       WHEN '英语' THEN 450
                       WHEN '新闻传播学' THEN 250
                       ELSE 600
                   END
               WHEN '教育学' THEN
                   CASE m.name
                       WHEN '教育学' THEN 200
                       WHEN '心理学' THEN 100
                       ELSE 450
                   END
               WHEN '工学' THEN 1800
               WHEN '理学' THEN
                   CASE m.name
                       WHEN '数学与应用数学' THEN 250
                       WHEN '统计学' THEN 450
                       ELSE 1400
                   END
               ELSE 1400
           END AS final_rank
    FROM school s
    JOIN major m ON m.school_id = s.id
) ranked;

-- ============================================================
-- 5. 补充外省录取数据 (2023-2025, 物理+历史, 重点省份)
-- ============================================================

INSERT IGNORE INTO admission (school_id, major_id, province, subject_type, year, min_score, min_rank)
SELECT ranked.school_id,
       ranked.major_id,
       ranked.province,
       ranked.subject_type,
       ranked.year,
       GREATEST(400, 700 - FLOOR(ranked.final_rank / 350)),
       ranked.final_rank
FROM (
    SELECT s.id AS school_id,
           m.id AS major_id,
           prov.province,
           subj.subject_type,
           yr.year,
           (CASE s.name
               WHEN '清华大学' THEN 60
               WHEN '北京大学' THEN 70
               WHEN '复旦大学' THEN 220
               WHEN '上海交通大学' THEN 200
               WHEN '浙江大学' THEN 920
               WHEN '厦门大学' THEN 2900
               WHEN '武汉大学' THEN 1300
               WHEN '华中科技大学' THEN 1600
               WHEN '中山大学' THEN 2400
               WHEN '四川大学' THEN 2700
               WHEN '西安交通大学' THEN 2200
               WHEN '哈尔滨工业大学' THEN 3900
               WHEN '北京理工大学' THEN 1700
               WHEN '北京航空航天大学' THEN 1500
               WHEN '天津大学' THEN 2500
               WHEN '南开大学' THEN 2300
               WHEN '大连理工大学' THEN 3600
               WHEN '山东大学' THEN 3200
               WHEN '吉林大学' THEN 4300
               WHEN '中南大学' THEN 4200
               WHEN '华南理工大学' THEN 3300
               WHEN '电子科技大学' THEN 2900
               WHEN '西北工业大学' THEN 3900
               WHEN '东北大学' THEN 5100
               WHEN '兰州大学' THEN 5400
               WHEN '福州大学' THEN 12800
               WHEN '北京邮电大学' THEN 5100
               WHEN '苏州大学' THEN 6400
               WHEN '上海大学' THEN 8700
               WHEN '南京邮电大学' THEN 10700
               WHEN '武汉理工大学' THEN 9700
               WHEN '合肥工业大学' THEN 10700
               WHEN '河海大学' THEN 11200
               WHEN '东华大学' THEN 11200
               WHEN '杭州电子科技大学' THEN 14800
               WHEN '宁波大学' THEN 18200
               WHEN '华侨大学' THEN 19200
               WHEN '福建师范大学' THEN 21200
               WHEN '集美大学' THEN 26200
               WHEN '西南交通大学' THEN 7100
               WHEN '西安电子科技大学' THEN 4900
               WHEN '暨南大学' THEN 8100
               WHEN '深圳大学' THEN 7600
               WHEN '中国海洋大学' THEN 4700
               WHEN '云南大学' THEN 10200
               WHEN '郑州大学' THEN 9200
               WHEN '南昌大学' THEN 11700
               WHEN '贵州大学' THEN 14200
               ELSE 15500
           END)
           + prov.rank_offset
           + CASE subj.subject_type WHEN '历史类' THEN 6000 ELSE 0 END
           + (yr.year - 2025) * 200
           + CASE m.category
               WHEN '工学' THEN
                   CASE m.name
                       WHEN '计算机科学与技术' THEN 0
                       WHEN '软件工程' THEN 650
                       WHEN '人工智能' THEN 300
                       WHEN '数据科学与大数据技术' THEN 500
                       WHEN '电子信息工程' THEN 800
                       WHEN '通信工程' THEN 1000
                       WHEN '信息安全' THEN 700
                       WHEN '电气工程及其自动化' THEN 1200
                       WHEN '自动化' THEN 1400
                       WHEN '机械工程' THEN 2000
                       WHEN '土木工程' THEN 3500
                       WHEN '建筑学' THEN 2500
                       WHEN '材料科学与工程' THEN 2800
                       WHEN '环境工程' THEN 4000
                       WHEN '化学工程与工艺' THEN 3200
                       WHEN '食品科学与工程' THEN 4500
                       WHEN '生物医学工程' THEN 2200
                       WHEN '网络工程' THEN 900
                       ELSE 1500
                   END
               WHEN '理学' THEN
                   CASE m.name
                       WHEN '数学与应用数学' THEN 500
                       WHEN '物理学' THEN 1800
                       WHEN '化学' THEN 2500
                       WHEN '统计学' THEN 800
                       WHEN '生物科学' THEN 3000
                       ELSE 2000
                   END
               WHEN '医学' THEN
                   CASE m.name
                       WHEN '临床医学' THEN -500
                       WHEN '口腔医学' THEN -800
                       WHEN '药学' THEN 2000
                       ELSE 1500
                   END
               WHEN '经济学' THEN
                   CASE m.name
                       WHEN '金融学' THEN -200
                       WHEN '经济学' THEN 500
                       WHEN '国际经济与贸易' THEN 1500
                       ELSE 1000
                   END
               WHEN '管理学' THEN
                   CASE m.name
                       WHEN '会计学' THEN 800
                       WHEN '工商管理' THEN 1500
                       WHEN '财务管理' THEN 1200
                       ELSE 1800
                   END
               WHEN '法学' THEN 1000
               WHEN '文学' THEN 2500
               WHEN '教育学' THEN 2000
               ELSE 2000
           END AS final_rank
    FROM school s
    JOIN major m ON m.school_id = s.id
    CROSS JOIN (
        SELECT '广东' AS province, 1800 AS rank_offset
        UNION ALL SELECT '浙江', 800
        UNION ALL SELECT '江苏', 1200
        UNION ALL SELECT '山东', 1800
        UNION ALL SELECT '河南', 3600
        UNION ALL SELECT '四川', 2800
        UNION ALL SELECT '湖北', 2200
        UNION ALL SELECT '湖南', 2600
        UNION ALL SELECT '安徽', 2600
        UNION ALL SELECT '江西', 3400
        UNION ALL SELECT '河北', 2200
        UNION ALL SELECT '陕西', 2400
        UNION ALL SELECT '辽宁', 2800
        UNION ALL SELECT '重庆', 3000
        UNION ALL SELECT '云南', 5800
        UNION ALL SELECT '贵州', 6200
    ) prov
    CROSS JOIN (
        SELECT '物理类' AS subject_type
        UNION ALL SELECT '历史类'
    ) subj
    CROSS JOIN (
        SELECT 2023 AS year
        UNION ALL SELECT 2024
        UNION ALL SELECT 2025
    ) yr
) ranked;

-- ============================================================
-- 6. 新增测试学生账号 (密码均为 Student@123)
--    password_hash 使用与 admin 相同的 PBKDF2 格式
-- ============================================================

INSERT IGNORE INTO sys_user (id, username, password_hash, nickname, phone, enabled) VALUES
    (2, 'student_fj', 'pbkdf2_sha256$120000$2IS1AXELLN4mCwn9TmRCgA==$ahceD1SndLoDTO6DCJc/ELoqocUk/6EQ1TEWjyr5Ww0=', '福建考生小林', '13800001001', TRUE),
    (3, 'student_gd', 'pbkdf2_sha256$120000$2IS1AXELLN4mCwn9TmRCgA==$ahceD1SndLoDTO6DCJc/ELoqocUk/6EQ1TEWjyr5Ww0=', '广东考生小王', '13800001002', TRUE),
    (4, 'student_zj', 'pbkdf2_sha256$120000$2IS1AXELLN4mCwn9TmRCgA==$ahceD1SndLoDTO6DCJc/ELoqocUk/6EQ1TEWjyr5Ww0=', '浙江考生小张', '13800001003', TRUE),
    (5, 'student_sc', 'pbkdf2_sha256$120000$2IS1AXELLN4mCwn9TmRCgA==$ahceD1SndLoDTO6DCJc/ELoqocUk/6EQ1TEWjyr5Ww0=', '四川考生小李', '13800001004', TRUE),
    (6, 'student_hn', 'pbkdf2_sha256$120000$2IS1AXELLN4mCwn9TmRCgA==$ahceD1SndLoDTO6DCJc/ELoqocUk/6EQ1TEWjyr5Ww0=', '河南考生小陈', '13800001005', TRUE),
    (7, 'student_sd', 'pbkdf2_sha256$120000$2IS1AXELLN4mCwn9TmRCgA==$ahceD1SndLoDTO6DCJc/ELoqocUk/6EQ1TEWjyr5Ww0=', '山东考生小刘', '13800001006', TRUE),
    (8, 'student_hb', 'pbkdf2_sha256$120000$2IS1AXELLN4mCwn9TmRCgA==$ahceD1SndLoDTO6DCJc/ELoqocUk/6EQ1TEWjyr5Ww0=', '湖北考生小赵', '13800001007', TRUE),
    (9, 'teacher_wang', 'pbkdf2_sha256$120000$2IS1AXELLN4mCwn9TmRCgA==$ahceD1SndLoDTO6DCJc/ELoqocUk/6EQ1TEWjyr5Ww0=', '王老师(班主任)', '13900001001', TRUE),
    (10, 'counselor_li', 'pbkdf2_sha256$120000$2IS1AXELLN4mCwn9TmRCgA==$ahceD1SndLoDTO6DCJc/ELoqocUk/6EQ1TEWjyr5Ww0=', '李辅导员', '13900001002', TRUE);

-- 为学生分配普通用户角色
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u CROSS JOIN sys_role r
WHERE u.username LIKE 'student_%' AND r.role_code = 'user';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u CROSS JOIN sys_role r
WHERE u.username IN ('teacher_wang', 'counselor_li') AND r.role_code = 'user';

from __future__ import annotations

import sqlite3
from collections.abc import Iterator
from contextlib import contextmanager
from pathlib import Path

DATABASE_PATH = Path(__file__).resolve().parent / "data" / "education.db"


@contextmanager
def get_connection() -> Iterator[sqlite3.Connection]:
    connection = sqlite3.connect(DATABASE_PATH)
    connection.row_factory = sqlite3.Row
    connection.execute("PRAGMA foreign_keys = ON")
    try:
        yield connection
        connection.commit()
    finally:
        connection.close()


def initialize_database() -> None:
    DATABASE_PATH.parent.mkdir(parents=True, exist_ok=True)
    with get_connection() as connection:
        connection.executescript(
            """
            CREATE TABLE IF NOT EXISTS school (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL UNIQUE,
                province TEXT NOT NULL,
                city TEXT NOT NULL,
                level TEXT NOT NULL,
                description TEXT NOT NULL
            );

            CREATE TABLE IF NOT EXISTS major (
                id INTEGER PRIMARY KEY,
                school_id INTEGER NOT NULL REFERENCES school(id) ON DELETE CASCADE,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                description TEXT NOT NULL,
                UNIQUE(school_id, name)
            );

            CREATE TABLE IF NOT EXISTS admission (
                id INTEGER PRIMARY KEY,
                school_id INTEGER NOT NULL REFERENCES school(id) ON DELETE CASCADE,
                major_id INTEGER NOT NULL REFERENCES major(id) ON DELETE CASCADE,
                province TEXT NOT NULL,
                year INTEGER NOT NULL,
                min_score INTEGER NOT NULL,
                min_rank INTEGER NOT NULL,
                UNIQUE(school_id, major_id, province, year)
            );
            """
        )
        _migrate_admission_metadata(connection)
        has_data = connection.execute("SELECT EXISTS(SELECT 1 FROM school)").fetchone()[0]
        if not has_data:
            _seed_database(connection)
        _add_demo_schools(connection)
        _add_verified_admissions(connection)
        _add_fafu_verified_admissions(connection)
        _add_fafu_regular_admissions(connection)


def _migrate_admission_metadata(connection: sqlite3.Connection) -> None:
    """Add provenance fields while keeping existing local databases usable."""
    columns = {row["name"] for row in connection.execute("PRAGMA table_info(admission)")}
    if "subject_group" not in columns:
        connection.execute(
            "ALTER TABLE admission ADD COLUMN subject_group TEXT NOT NULL DEFAULT '未区分科类'"
        )
    if "source_name" not in columns:
        connection.execute(
            "ALTER TABLE admission ADD COLUMN source_name TEXT NOT NULL DEFAULT '本地演示数据'"
        )
    if "source_url" not in columns:
        connection.execute(
            "ALTER TABLE admission ADD COLUMN source_url TEXT NOT NULL DEFAULT ''"
        )


def _seed_database(connection: sqlite3.Connection) -> None:
    schools = [
        ("厦门大学", "福建", "厦门", "985", "综合性研究型大学。"),
        ("福州大学", "福建", "福州", "211", "国家双一流建设高校。"),
        ("华侨大学", "福建", "泉州", "省重点", "面向海内外学生的综合性大学。"),
        ("福建师范大学", "福建", "福州", "省重点", "师范教育特色鲜明的综合性大学。"),
        ("集美大学", "福建", "厦门", "省重点", "航海、水产与工程学科特色突出。"),
        ("浙江大学", "浙江", "杭州", "985", "综合性研究型大学。"),
        ("杭州电子科技大学", "浙江", "杭州", "省重点", "电子信息与计算机学科优势明显。"),
        ("宁波大学", "浙江", "宁波", "双一流", "综合性大学。"),
        ("上海大学", "上海", "上海", "211", "上海市属综合性大学。"),
        ("东华大学", "上海", "上海", "211", "材料、纺织与信息学科协调发展。"),
        ("苏州大学", "江苏", "苏州", "211", "江苏省属综合性大学。"),
        ("南京邮电大学", "江苏", "南京", "双一流", "信息通信与电子信息特色高校。"),
    ]
    connection.executemany(
        "INSERT INTO school(name, province, city, level, description) VALUES (?, ?, ?, ?, ?)",
        schools,
    )
    school_ids = {row["name"]: row["id"] for row in connection.execute("SELECT id, name FROM school")}
    major_rows: list[tuple[int, str, str, str]] = []
    for name in school_ids:
        major_rows.extend(
            [
                (school_ids[name], "计算机科学与技术", "工学", "学习计算机系统、软件与算法基础。"),
                (school_ids[name], "软件工程", "工学", "培养软件分析、设计、开发与测试能力。"),
                (school_ids[name], "人工智能", "工学", "学习机器学习、数据分析与智能系统基础。"),
            ]
        )
    connection.executemany(
        "INSERT INTO major(school_id, name, category, description) VALUES (?, ?, ?, ?)", major_rows
    )
    rank_by_school = {
        "厦门大学": 2800, "福州大学": 12500, "华侨大学": 19000,
        "福建师范大学": 21000, "集美大学": 26000, "浙江大学": 900,
        "杭州电子科技大学": 14500, "宁波大学": 18000, "上海大学": 8500,
        "东华大学": 11000, "苏州大学": 6200, "南京邮电大学": 10500,
    }
    admissions: list[tuple[int, int, str, int, int, int]] = []
    for major in connection.execute("SELECT id, school_id, name FROM major"):
        school_name = next(name for name, school_id in school_ids.items() if school_id == major["school_id"])
        adjustment = {"计算机科学与技术": 0, "软件工程": 800, "人工智能": 1400}[major["name"]]
        rank = rank_by_school[school_name] + adjustment
        score = max(500, 650 - rank // 300)
        admissions.append((major["school_id"], major["id"], "福建", 2025, score, rank))
    connection.executemany(
        """INSERT INTO admission(school_id, major_id, province, year, min_score, min_rank)
           VALUES (?, ?, ?, ?, ?, ?)""",
        admissions,
    )


def _add_demo_schools(connection: sqlite3.Connection) -> None:
    """Add a small, idempotent batch of clearly synthetic records for local demos."""
    schools = [
        ("北京邮电大学", "北京", "北京", "211", "信息通信与计算机学科特色高校。"),
        ("哈尔滨工业大学", "黑龙江", "哈尔滨", "985", "以工科见长的研究型大学。"),
        ("武汉理工大学", "湖北", "武汉", "211", "材料、交通与工程学科特色高校。"),
        ("中南大学", "湖南", "长沙", "985", "综合性研究型大学。"),
        ("合肥工业大学", "安徽", "合肥", "211", "工程教育与科研特色高校。"),
        ("河海大学", "江苏", "南京", "211", "水利、环境与工程学科特色高校。"),
    ]
    connection.executemany(
        "INSERT OR IGNORE INTO school(name, province, city, level, description) VALUES (?, ?, ?, ?, ?)",
        schools,
    )
    school_ids = {
        row["name"]: row["id"]
        for row in connection.execute(
            "SELECT id, name FROM school WHERE name IN (?, ?, ?, ?, ?, ?)",
            [school[0] for school in schools],
        )
    }
    major_rows = [
        (school_ids[name], major_name, "工学", description)
        for name in school_ids
        for major_name, description in (
            ("计算机科学与技术", "学习计算机系统、软件与算法基础。"),
            ("软件工程", "培养软件分析、设计、开发与测试能力。"),
            ("人工智能", "学习机器学习、数据分析与智能系统基础。"),
        )
    ]
    connection.executemany(
        "INSERT OR IGNORE INTO major(school_id, name, category, description) VALUES (?, ?, ?, ?)",
        major_rows,
    )
    base_ranks = {
        "北京邮电大学": 5_000,
        "哈尔滨工业大学": 3_800,
        "武汉理工大学": 9_500,
        "中南大学": 4_200,
        "合肥工业大学": 10_500,
        "河海大学": 11_000,
    }
    admissions = []
    for major in connection.execute(
        """SELECT m.id, m.school_id, m.name, s.name AS school_name
           FROM major m JOIN school s ON s.id = m.school_id
           WHERE s.name IN (?, ?, ?, ?, ?, ?)""",
        [school[0] for school in schools],
    ):
        adjustment = {"计算机科学与技术": 0, "软件工程": 800, "人工智能": 1_400}[major["name"]]
        rank = base_ranks[major["school_name"]] + adjustment
        score = max(500, 650 - rank // 300)
        admissions.append((major["school_id"], major["id"], "福建", 2025, score, rank))
    connection.executemany(
        """INSERT OR IGNORE INTO admission(school_id, major_id, province, year, min_score, min_rank)
           VALUES (?, ?, ?, ?, ?, ?)""",
        admissions,
    )


def _add_verified_admissions(connection: sqlite3.Connection) -> None:
    """Add a small, idempotent set of official, traceable admission records."""
    school = (
        "天津财经大学珠江学院",
        "天津",
        "天津",
        "民办本科",
        "财经管理类应用型本科院校。",
    )
    connection.execute(
        "INSERT OR IGNORE INTO school(name, province, city, level, description) VALUES (?, ?, ?, ?, ?)",
        school,
    )
    school_id = connection.execute(
        "SELECT id FROM school WHERE name = ?", (school[0],)
    ).fetchone()["id"]
    records = [
        ("会计学", "管理学", "培养会计、财务分析与管理能力。", 470, 94712),
        ("传播学", "文学", "学习新闻传播与媒介实践基础。", 474, 91278),
        ("国际商务", "管理学", "学习国际贸易与跨境商务基础。", 473, 92111),
        ("税收学", "经济学", "学习税制、税收管理与实务基础。", 474, 91278),
        ("经济统计学", "经济学", "学习统计方法与经济数据分析。", 471, 93819),
        ("金融学", "经济学", "学习金融市场、投资与风险管理基础。", 470, 94712),
    ]
    connection.executemany(
        "INSERT OR IGNORE INTO major(school_id, name, category, description) VALUES (?, ?, ?, ?)",
        [(school_id, name, category, description) for name, category, description, _, _ in records],
    )
    major_ids = {
        row["name"]: row["id"]
        for row in connection.execute("SELECT id, name FROM major WHERE school_id = ?", (school_id,))
    }
    source_name = "天津财经大学珠江学院：2025年普通本科分专业录取分数线一览表"
    source_url = (
        "https://zhujiang.tjufe.edu.cn/_upload/article/files/f2/94/"
        "da0d6b934c89a24f08810c7af327/85245233-620d-4e2b-8dfb-373ffe5b59ae.pdf"
    )
    for name, _, _, min_score, min_rank in records:
        connection.execute(
            """INSERT INTO admission(
                   school_id, major_id, province, year, min_score, min_rank,
                   subject_group, source_name, source_url
               ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
               ON CONFLICT(school_id, major_id, province, year) DO UPDATE SET
                   min_score = excluded.min_score,
                   min_rank = excluded.min_rank,
                   subject_group = excluded.subject_group,
                   source_name = excluded.source_name,
                   source_url = excluded.source_url""",
            (
                school_id,
                major_ids[name],
                "福建",
                2025,
                min_score,
                min_rank,
                "物理类",
                source_name,
                source_url,
            ),
        )

def _add_fafu_verified_admissions(connection: sqlite3.Connection) -> None:
    """Add official 2025 Fujian records published by Fujian Agriculture and Forestry University."""
    school = (
        "福建农林大学",
        "福建",
        "福州",
        "省重点",
        "以农林科学、生命科学为优势特色的省属重点高校。",
    )
    connection.execute(
        "INSERT OR IGNORE INTO school(name, province, city, level, description) VALUES (?, ?, ?, ?, ?)",
        school,
    )
    school_id = connection.execute(
        "SELECT id FROM school WHERE name = ?", (school[0],)
    ).fetchone()["id"]
    records = [
        ("风景园林（中外合作办学）", "工学", "学习景观规划、设计与生态保护基础。", 507, 64120),
        ("农林经济管理（中外合作办学）", "管理学", "学习农林经济、管理与政策分析基础。", 512, 60280),
    ]
    connection.executemany(
        "INSERT OR IGNORE INTO major(school_id, name, category, description) VALUES (?, ?, ?, ?)",
        [(school_id, name, category, description) for name, category, description, _, _ in records],
    )
    major_ids = {
        row["name"]: row["id"]
        for row in connection.execute("SELECT id, name FROM major WHERE school_id = ?", (school_id,))
    }
    source_name = "福建农林大学戴尔豪西大学联合学院：2025年各省录取分数及排名"
    source_url = "https://gjxy.fafu.edu.cn/9a/3f/c12072a432703/pagem.htm"
    for name, _, _, min_score, min_rank in records:
        connection.execute(
            """INSERT INTO admission(
                   school_id, major_id, province, year, min_score, min_rank,
                   subject_group, source_name, source_url
               ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
               ON CONFLICT(school_id, major_id, province, year) DO UPDATE SET
                   min_score = excluded.min_score,
                   min_rank = excluded.min_rank,
                   subject_group = excluded.subject_group,
                   source_name = excluded.source_name,
                   source_url = excluded.source_url""",
            (
                school_id,
                major_ids[name],
                "福建",
                2025,
                min_score,
                min_rank,
                "物理类",
                source_name,
                source_url,
            ),
        )


def _add_fafu_regular_admissions(connection: sqlite3.Connection) -> None:
    """Add ordinary-program records from a public 2025 Fujian admission-data index."""
    school_id = connection.execute(
        "SELECT id FROM school WHERE name = ?", ("福建农林大学",)
    ).fetchone()["id"]
    records = [
        ("电气工程及其自动化", "工学", "学习电力系统、电机与自动化控制基础。", 576, 22059, "物理类（再选化学）"),
        ("计算机科学与技术", "工学", "学习计算机系统、软件与算法基础。", 573, 23443, "物理类（再选化学）"),
        ("电子信息工程", "工学", "学习电子系统、通信与信息处理基础。", 569, 25291, "物理类（再选化学）"),
        ("农学", "农学", "学习作物生产、育种与现代农业基础。", 566, 26736, "物理类（再选化学）"),
        ("法学", "法学", "学习法律基础理论与实务能力。", 563, 28208, "物理类（再选不限）"),
        ("机械设计制造及其自动化", "工学", "学习机械设计、制造与自动化基础。", 563, 28208, "物理类（再选化学）"),
        ("软件工程", "工学", "培养软件分析、设计、开发与测试能力。", 562, 28701, "物理类（再选化学）"),
    ]
    connection.executemany(
        "INSERT OR IGNORE INTO major(school_id, name, category, description) VALUES (?, ?, ?, ?)",
        [(school_id, name, category, description) for name, category, description, *_ in records],
    )
    major_ids = {
        row["name"]: row["id"]
        for row in connection.execute("SELECT id, name FROM major WHERE school_id = ?", (school_id,))
    }
    source_name = "果然优志：2025年福建农林大学在福建高考录取投档分数线及位次"
    source_url = "https://www.hzgrys.net/score/35/2025/1136.html"
    for name, _, _, min_score, min_rank, subject_group in records:
        connection.execute(
            """INSERT INTO admission(
                   school_id, major_id, province, year, min_score, min_rank,
                   subject_group, source_name, source_url
               ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
               ON CONFLICT(school_id, major_id, province, year) DO UPDATE SET
                   min_score = excluded.min_score,
                   min_rank = excluded.min_rank,
                   subject_group = excluded.subject_group,
                   source_name = excluded.source_name,
                   source_url = excluded.source_url""",
            (
                school_id,
                major_ids[name],
                "福建",
                2025,
                min_score,
                min_rank,
                subject_group,
                source_name,
                source_url,
            ),
        )

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
        has_data = connection.execute("SELECT EXISTS(SELECT 1 FROM school)").fetchone()[0]
        if not has_data:
            _seed_database(connection)
        _add_demo_schools(connection)


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

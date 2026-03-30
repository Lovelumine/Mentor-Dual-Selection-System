#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import mimetypes
import os
import re
from dataclasses import dataclass
from io import BytesIO
from pathlib import Path
from typing import Iterable
from urllib.parse import urlparse

import pymysql
import requests
from minio import Minio


ENV_FILE = Path("/home/yingying/.mentor_dual_selection_dev.env")
MINIO_ENDPOINT = "127.0.0.1:9000"
MINIO_PUBLIC_BASE = "https://minio.lumoxuan.cn"
MINIO_BUCKET = "mentor-dual-selection-system"
MINIO_ACCESS_KEY = "lovelumine"
MINIO_SECRET_KEY = "lovelumine90"
OBJECT_PREFIX = "teacher-photo-mirror"
REQUEST_TIMEOUT = 30


@dataclass
class TeacherPhoto:
    uid: int
    full_name: str
    username: str
    photo_url: str


def load_env(path: Path) -> dict[str, str]:
    env: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        env[key.strip()] = value.strip()
    return env


def parse_jdbc_url(jdbc_url: str) -> tuple[str, int, str]:
    cleaned = jdbc_url.removeprefix("jdbc:mysql://")
    host_port_db, _, _ = cleaned.partition("?")
    host_port, _, db_name = host_port_db.partition("/")
    host, _, port_text = host_port.partition(":")
    return host or "127.0.0.1", int(port_text or "3306"), db_name


def connect_db(env: dict[str, str]):
    host, port, db_name = parse_jdbc_url(env["SPRING_DATASOURCE_URL"])
    return pymysql.connect(
        host=host,
        port=port,
        user=env["SPRING_DATASOURCE_USERNAME"],
        password=env["SPRING_DATASOURCE_PASSWORD"],
        database=db_name,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
        autocommit=False,
    )


def get_external_teacher_photos(connection) -> list[TeacherPhoto]:
    sql = """
        SELECT u.uid, u.full_name, u.username, ud.photo_url
        FROM users u
        JOIN roles r ON r.role_id = u.role_id
        JOIN user_details ud ON ud.uid = u.uid
        WHERE r.role_name = 'TEACHER'
          AND ud.photo_url IS NOT NULL
          AND TRIM(ud.photo_url) <> ''
          AND ud.photo_url NOT LIKE 'https://minio.lumoxuan.cn/%'
        ORDER BY u.uid
    """
    with connection.cursor() as cursor:
        cursor.execute(sql)
        rows = cursor.fetchall()

    return [
        TeacherPhoto(
            uid=row["uid"],
            full_name=row["full_name"],
            username=row["username"],
            photo_url=row["photo_url"],
        )
        for row in rows
    ]


def sanitize_slug(value: str) -> str:
    cleaned = re.sub(r"[^A-Za-z0-9._-]+", "-", value).strip("-")
    return cleaned or "teacher"


def guess_extension(source_url: str, content_type: str | None) -> str:
    path_ext = os.path.splitext(urlparse(source_url).path)[1].lower()
    if path_ext and len(path_ext) <= 6:
        return path_ext

    if content_type:
        guessed = mimetypes.guess_extension(content_type.split(";")[0].strip())
        if guessed:
            return guessed

    return ".jpg"


def object_name_for(record: TeacherPhoto, content: bytes, content_type: str | None) -> str:
    digest = hashlib.sha1(content).hexdigest()[:12]
    ext = guess_extension(record.photo_url, content_type)
    username = sanitize_slug(record.username)
    return f"{OBJECT_PREFIX}/{record.uid}/{username}-{digest}{ext}"


def minio_public_url(object_name: str) -> str:
    return f"{MINIO_PUBLIC_BASE}/{MINIO_BUCKET}/{object_name}"


def download_photo(session: requests.Session, source_url: str) -> tuple[bytes, str | None]:
    response = session.get(source_url, timeout=REQUEST_TIMEOUT, allow_redirects=True)
    response.raise_for_status()
    return response.content, response.headers.get("Content-Type")


def upload_photo(client: Minio, object_name: str, content: bytes, content_type: str | None) -> None:
    client.put_object(
        MINIO_BUCKET,
        object_name,
        BytesIO(content),
        length=len(content),
        content_type=content_type or "application/octet-stream",
    )


def update_photo_url(connection, uid: int, new_url: str) -> None:
    with connection.cursor() as cursor:
        cursor.execute(
            "UPDATE user_details SET photo_url = %s WHERE uid = %s",
            (new_url, uid),
        )


def migrate(records: Iterable[TeacherPhoto]) -> tuple[int, list[tuple[TeacherPhoto, str]]]:
    env = load_env(ENV_FILE)
    connection = connect_db(env)
    client = Minio(
        MINIO_ENDPOINT,
        access_key=MINIO_ACCESS_KEY,
        secret_key=MINIO_SECRET_KEY,
        secure=False,
    )
    session = requests.Session()
    session.headers.update(
        {
            "User-Agent": (
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
                "(KHTML, like Gecko) Chrome/124.0 Safari/537.36"
            )
        }
    )

    migrated = 0
    failures: list[tuple[TeacherPhoto, str]] = []

    try:
        for record in records:
            try:
                content, content_type = download_photo(session, record.photo_url)
                object_name = object_name_for(record, content, content_type)

                try:
                    client.stat_object(MINIO_BUCKET, object_name)
                except Exception:
                    upload_photo(client, object_name, content, content_type)

                update_photo_url(connection, record.uid, minio_public_url(object_name))
                migrated += 1
                print(f"[OK] {record.uid} {record.full_name} -> {object_name}")
            except Exception as exc:  # noqa: BLE001
                failures.append((record, str(exc)))
                print(f"[FAIL] {record.uid} {record.full_name}: {exc}")

        connection.commit()
        return migrated, failures
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()


def main() -> int:
    env = load_env(ENV_FILE)
    with connect_db(env) as connection:
        records = get_external_teacher_photos(connection)

    if not records:
        print("No external teacher photo URLs found.")
        return 0

    print(f"Found {len(records)} external teacher photo URLs to mirror.")
    migrated, failures = migrate(records)
    print(f"Migrated: {migrated}")
    print(f"Failed: {len(failures)}")
    if failures:
        for record, error in failures:
            print(f" - {record.uid} {record.full_name}: {error}")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

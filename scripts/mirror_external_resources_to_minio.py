#!/usr/bin/env python3
from __future__ import annotations

import mimetypes
import os
import re
from dataclasses import dataclass
from io import BytesIO
from pathlib import Path
from urllib.parse import urlparse

import pymysql
import requests
from minio import Minio
from minio.error import S3Error
from PIL import Image


ENV_FILE = Path("/home/yingying/.mentor_dual_selection_dev.env")
MINIO_ENDPOINT = "127.0.0.1:9000"
MINIO_PUBLIC_BASE = "https://minio.lumoxuan.cn"
MINIO_BUCKET = "mentor-dual-selection-system"
MINIO_ACCESS_KEY = "lovelumine"
MINIO_SECRET_KEY = "lovelumine90"
THUMBNAIL_SUFFIX = ".thumb.jpg"
THUMBNAIL_MAX_SIZE = 320
REQUEST_TIMEOUT = 30
DEFAULT_AVATAR_URL = f"{MINIO_PUBLIC_BASE}/{MINIO_BUCKET}/defaultavater.webp"


@dataclass(frozen=True)
class ResourceField:
    table: str
    id_column: str
    url_column: str
    object_prefix: str


RESOURCE_FIELDS = [
    ResourceField("users", "uid", "avatar_url", "users-avatar"),
    ResourceField("user_details", "uid", "photo_url", "user-details-photo"),
    ResourceField("announcements", "id", "attachment_url", "announcements-attachment"),
]


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


def get_minio_client() -> Minio:
    return Minio(
        MINIO_ENDPOINT,
        access_key=MINIO_ACCESS_KEY,
        secret_key=MINIO_SECRET_KEY,
        secure=False,
    )


def sanitize_slug(value: str) -> str:
    cleaned = re.sub(r"[^A-Za-z0-9._-]+", "-", value).strip("-")
    return cleaned or "resource"


def is_minio_public_url(url: str) -> bool:
    return url.startswith(f"{MINIO_PUBLIC_BASE}/{MINIO_BUCKET}/")


def public_url_to_object_name(url: str) -> str:
    prefix = f"{MINIO_PUBLIC_BASE}/{MINIO_BUCKET}/"
    if not url.startswith(prefix):
        raise ValueError(f"URL is not in MinIO bucket: {url}")
    return url[len(prefix):]


def is_image_type(url: str, content_type: str | None) -> bool:
    if content_type and content_type.lower().startswith("image/"):
        return True
    lower = urlparse(url).path.lower()
    return lower.endswith((".jpg", ".jpeg", ".png", ".webp", ".bmp", ".gif"))


def build_object_name(field: ResourceField, record_id: int, source_url: str, content_type: str | None) -> str:
    parsed = urlparse(source_url)
    original_name = os.path.basename(parsed.path) or f"{field.url_column}-{record_id}"
    original_name = sanitize_slug(original_name)
    if "." not in original_name:
        ext = mimetypes.guess_extension((content_type or "").split(";")[0].strip()) or ""
        original_name = f"{original_name}{ext}"
    return f"{field.object_prefix}/{record_id}/{original_name}"


def thumbnail_object_name(object_name: str) -> str:
    return f"{object_name}{THUMBNAIL_SUFFIX}"


def minio_public_url(object_name: str) -> str:
    return f"{MINIO_PUBLIC_BASE}/{MINIO_BUCKET}/{object_name}"


def stat_object(client: Minio, object_name: str) -> bool:
    try:
        client.stat_object(MINIO_BUCKET, object_name)
        return True
    except S3Error:
        return False


def download_external(session: requests.Session, source_url: str) -> tuple[bytes, str | None]:
    response = session.get(source_url, timeout=REQUEST_TIMEOUT, allow_redirects=True)
    response.raise_for_status()
    return response.content, response.headers.get("Content-Type")


def download_minio_object(client: Minio, object_name: str) -> tuple[bytes, str | None]:
    response = client.get_object(MINIO_BUCKET, object_name)
    try:
        content = response.read()
        return content, response.headers.get("Content-Type")
    finally:
        response.close()
        response.release_conn()


def upload_object(client: Minio, object_name: str, payload: bytes, content_type: str | None) -> None:
    client.put_object(
        MINIO_BUCKET,
        object_name,
        BytesIO(payload),
        length=len(payload),
        content_type=content_type or "application/octet-stream",
    )


def create_thumbnail_bytes(payload: bytes) -> bytes | None:
    try:
        with Image.open(BytesIO(payload)) as image:
            image = image.convert("RGB")
            image.thumbnail((THUMBNAIL_MAX_SIZE, THUMBNAIL_MAX_SIZE))
            output = BytesIO()
            image.save(output, format="JPEG", quality=82, optimize=True)
            return output.getvalue()
    except Exception:
        return None


def fetch_rows(connection, field: ResourceField):
    sql = f"""
        SELECT {field.id_column} AS record_id, {field.url_column} AS resource_url
        FROM {field.table}
        WHERE {field.url_column} IS NOT NULL
          AND TRIM({field.url_column}) <> ''
        ORDER BY {field.id_column}
    """
    with connection.cursor() as cursor:
        cursor.execute(sql)
        return cursor.fetchall()


def update_url(connection, field: ResourceField, record_id: int, new_url: str) -> None:
    sql = f"UPDATE {field.table} SET {field.url_column} = %s WHERE {field.id_column} = %s"
    with connection.cursor() as cursor:
        cursor.execute(sql, (new_url, record_id))


def resolve_missing_minio_url(connection, field: ResourceField, record_id: int, current_url: str) -> str:
    if field.table == "users" and field.url_column == "avatar_url":
        update_url(connection, field, record_id, DEFAULT_AVATAR_URL)
        print(f"[REPAIRED] {field.table}.{field.url_column}#{record_id} -> {DEFAULT_AVATAR_URL}")
        return DEFAULT_AVATAR_URL
    raise FileNotFoundError(f"Missing MinIO object for URL: {current_url}")


def main() -> int:
    env = load_env(ENV_FILE)
    client = get_minio_client()
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
    thumbnails_created = 0
    failures: list[str] = []

    connection = connect_db(env)
    try:
        for field in RESOURCE_FIELDS:
            rows = fetch_rows(connection, field)
            for row in rows:
                record_id = int(row["record_id"])
                source_url = str(row["resource_url"]).strip()
                try:
                    current_url = source_url
                    payload: bytes | None = None
                    content_type: str | None = None

                    if not is_minio_public_url(source_url):
                        payload, content_type = download_external(session, source_url)
                        object_name = build_object_name(field, record_id, source_url, content_type)
                        if not stat_object(client, object_name):
                            upload_object(client, object_name, payload, content_type)
                        current_url = minio_public_url(object_name)
                        update_url(connection, field, record_id, current_url)
                        migrated += 1
                        print(f"[MIGRATED] {field.table}.{field.url_column}#{record_id} -> {current_url}")

                    if is_image_type(current_url, content_type):
                        object_name = public_url_to_object_name(current_url)
                        thumb_name = thumbnail_object_name(object_name)
                        if not stat_object(client, thumb_name):
                            if payload is None:
                                try:
                                    payload, content_type = download_minio_object(client, object_name)
                                except S3Error as exc:
                                    if exc.code == "NoSuchKey":
                                        current_url = resolve_missing_minio_url(connection, field, record_id, current_url)
                                        object_name = public_url_to_object_name(current_url)
                                        thumb_name = thumbnail_object_name(object_name)
                                        payload, content_type = download_minio_object(client, object_name)
                                    else:
                                        raise
                            thumb_bytes = create_thumbnail_bytes(payload)
                            if thumb_bytes:
                                upload_object(client, thumb_name, thumb_bytes, "image/jpeg")
                                thumbnails_created += 1
                                print(f"[THUMB] {thumb_name}")
                except Exception as exc:  # noqa: BLE001
                    failures.append(f"{field.table}.{field.url_column}#{record_id}: {exc}")
                    print(f"[FAIL] {field.table}.{field.url_column}#{record_id}: {exc}")

        if failures:
            connection.rollback()
            print(f"Failed: {len(failures)}")
            for failure in failures:
                print(f" - {failure}")
            return 1

        connection.commit()
        print(f"Migrated external resources: {migrated}")
        print(f"Generated thumbnails: {thumbnails_created}")
        return 0
    finally:
        connection.close()


if __name__ == "__main__":
    raise SystemExit(main())

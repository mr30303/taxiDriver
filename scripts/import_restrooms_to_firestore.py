#!/usr/bin/env python3
"""
Build a normalized restroom master dataset from CSV files and optionally upload it to Firestore.

Usage examples:
  python scripts/import_restrooms_to_firestore.py --dry-run
  python scripts/import_restrooms_to_firestore.py --export-jsonl doc/restroom/restrooms_master.jsonl
  python scripts/import_restrooms_to_firestore.py --upload \
      --service-account path/to/service-account.json \
      --collection restrooms_master
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Tuple


ROOT_DIR = Path(__file__).resolve().parents[1]
RESTROOM_DIR = ROOT_DIR / "doc" / "restroom"
GOOGLE_SERVICES_PATH = ROOT_DIR / "app" / "google-services.json"


@dataclass(frozen=True)
class SourceConfig:
    file_name: str
    region: str
    name_keys: Tuple[str, ...]
    road_address_keys: Tuple[str, ...]
    lot_address_keys: Tuple[str, ...]
    latitude_keys: Tuple[str, ...]
    longitude_keys: Tuple[str, ...]
    phone_keys: Tuple[str, ...]
    type_keys: Tuple[str, ...]
    open_hour_keys: Tuple[str, ...]
    ownership_keys: Tuple[str, ...]
    district_keys: Tuple[str, ...]
    source_row_id_keys: Tuple[str, ...]


SOURCE_CONFIGS: List[SourceConfig] = [
    SourceConfig(
        file_name="seoul.csv",
        region="seoul",
        name_keys=("건물명", "비고"),
        road_address_keys=("도로명주소",),
        lot_address_keys=("지번주소",),
        latitude_keys=("y 좌표",),
        longitude_keys=("x 좌표",),
        phone_keys=("전화번호",),
        type_keys=("유형", "소재지 용도"),
        open_hour_keys=("개방시간",),
        ownership_keys=("소재지 용도",),
        district_keys=("구 명칭",),
        source_row_id_keys=("연번",),
    ),
    SourceConfig(
        file_name="gyeonggi.csv",
        region="gyeonggi",
        name_keys=("화장실명",),
        road_address_keys=("소재지도로명주소",),
        lot_address_keys=("소재지지번주소",),
        latitude_keys=("위도", "WGS84위도"),
        longitude_keys=("경도", "WGS84경도"),
        phone_keys=("전화번호",),
        type_keys=("구분",),
        open_hour_keys=("개방시간", "개방시간상세"),
        ownership_keys=("화장실소유구분", "화장실소유구분명"),
        district_keys=("관리기관명",),
        source_row_id_keys=("데이터기준일자",),
    ),
    SourceConfig(
        file_name="korea.csv",
        region="korea",
        name_keys=("화장실명",),
        road_address_keys=("소재지도로명주소",),
        lot_address_keys=("소재지지번주소",),
        latitude_keys=("WGS84위도", "위도"),
        longitude_keys=("WGS84경도", "경도"),
        phone_keys=("전화번호",),
        type_keys=("구분명", "구분"),
        open_hour_keys=("개방시간", "개방시간상세"),
        ownership_keys=("화장실소유구분명", "화장실소유구분"),
        district_keys=("관리기관명",),
        source_row_id_keys=(),
    ),
]


def clean_header(value: str) -> str:
    return value.replace("\ufeff", "").strip().strip('"')


def normalize_text(value: str) -> str:
    text = value.strip().lower()
    text = re.sub(r"\s+", " ", text)
    text = text.replace("|", " ")
    return text


def normalize_key_text(value: str) -> str:
    text = normalize_text(value)
    text = re.sub(r"[^0-9a-zA-Z\u3131-\u318E\uAC00-\uD7A3]+", "", text)
    return text


def first_non_empty(row: Dict[str, str], keys: Iterable[str]) -> str:
    for key in keys:
        value = row.get(key, "").strip()
        if value:
            return value
    return ""


def parse_float(raw: str) -> Optional[float]:
    if not raw:
        return None
    cleaned = raw.strip().replace(",", "")
    try:
        return float(cleaned)
    except ValueError:
        return None


def map_type(category: str, ownership: str) -> str:
    text = f"{category} {ownership}"
    text = normalize_text(text)
    if "민간" in text or "private" in text:
        return "private"
    if "공중" in text or "공공" in text or "public" in text:
        return "public"
    return "open"


def compose_open_hours(row: Dict[str, str], keys: Iterable[str]) -> str:
    values: List[str] = []
    for key in keys:
        value = row.get(key, "").strip().strip("|")
        if value and value not in values:
            values.append(value)
    return " / ".join(values)


def build_description(name: str, address: str, open_hours: str) -> str:
    parts: List[str] = []
    if name:
        parts.append(name)
    if address:
        parts.append(address)
    if open_hours:
        parts.append(f"open: {open_hours}")
    if parts:
        return " | ".join(parts)
    return "restroom"


def build_dedupe_key(name: str, address: str, lat: float, lng: float) -> str:
    name_key = normalize_key_text(name)
    address_key = normalize_key_text(address)
    lat_key = f"{lat:.5f}"
    lng_key = f"{lng:.5f}"
    if address_key:
        return f"coord_addr:{lat_key}:{lng_key}:{address_key}"
    if name_key:
        return f"coord_name:{lat_key}:{lng_key}:{name_key}"
    return f"coord_only:{lat_key}:{lng_key}"


def build_doc_id(dedupe_key: str) -> str:
    return hashlib.sha1(dedupe_key.encode("utf-8")).hexdigest()[:24]


def decode_csv_text(csv_path: Path) -> str:
    raw = csv_path.read_bytes()
    encodings = ("utf-8-sig", "cp949", "euc-kr")
    last_error: Optional[Exception] = None
    for encoding in encodings:
        try:
            return raw.decode(encoding)
        except UnicodeDecodeError as exc:
            last_error = exc
    if last_error:
        raise last_error
    raise RuntimeError(f"Unable to decode CSV: {csv_path}")


def load_csv_rows(csv_path: Path) -> Iterable[Dict[str, str]]:
    text = decode_csv_text(csv_path)
    handle = io.StringIO(text)
    reader = csv.DictReader(handle)
    if reader.fieldnames is None:
        return
    reader.fieldnames = [clean_header(name) for name in reader.fieldnames]
    for raw_row in reader:
        row: Dict[str, str] = {}
        for key, value in raw_row.items():
            if key is None:
                continue
            row[clean_header(key)] = (value or "").strip().strip('"')
        yield row


def normalize_row(config: SourceConfig, row: Dict[str, str], row_number: int) -> Optional[Dict[str, object]]:
    name = first_non_empty(row, config.name_keys)
    road_address = first_non_empty(row, config.road_address_keys)
    lot_address = first_non_empty(row, config.lot_address_keys)
    address = road_address or lot_address
    latitude = parse_float(first_non_empty(row, config.latitude_keys))
    longitude = parse_float(first_non_empty(row, config.longitude_keys))
    if latitude is None or longitude is None:
        return None
    if latitude < -90.0 or latitude > 90.0 or longitude < -180.0 or longitude > 180.0:
        return None

    phone = first_non_empty(row, config.phone_keys)
    category = first_non_empty(row, config.type_keys)
    ownership = first_non_empty(row, config.ownership_keys)
    district = first_non_empty(row, config.district_keys)
    source_row_id = first_non_empty(row, config.source_row_id_keys) or str(row_number)
    open_hours = compose_open_hours(row, config.open_hour_keys)
    dedupe_key = build_dedupe_key(name=name, address=address, lat=latitude, lng=longitude)
    type_value = map_type(category=category, ownership=ownership)
    description = build_description(name=name, address=address, open_hours=open_hours)
    doc_id = build_doc_id(dedupe_key)

    return {
        "id": doc_id,
        "lat": latitude,
        "lng": longitude,
        "type": type_value,
        "description": description,
        "createdBy": "master-data",
        "likeCount": 0,
        "dislikeCount": 0,
        "likedUserIds": [],
        "dislikedUserIds": [],
        "name": name,
        "roadAddress": road_address,
        "lotAddress": lot_address,
        "address": address,
        "district": district,
        "phone": phone,
        "category": category,
        "ownership": ownership,
        "openHours": open_hours,
        "sourceRegion": config.region,
        "sourceFile": config.file_name,
        "sourceRowId": source_row_id,
        "dedupeKey": dedupe_key,
    }


def build_dataset() -> Tuple[List[Dict[str, object]], Dict[str, int]]:
    deduped: Dict[str, Dict[str, object]] = {}
    stats = {
        "rows_total": 0,
        "rows_invalid_coord": 0,
        "rows_unique": 0,
        "rows_duplicate": 0,
    }

    for config in SOURCE_CONFIGS:
        csv_path = RESTROOM_DIR / config.file_name
        if not csv_path.exists():
            raise FileNotFoundError(f"Missing source CSV: {csv_path}")

        for row_number, row in enumerate(load_csv_rows(csv_path), start=2):
            stats["rows_total"] += 1
            normalized = normalize_row(config, row, row_number)
            if normalized is None:
                stats["rows_invalid_coord"] += 1
                continue

            key = str(normalized["dedupeKey"])
            current = deduped.get(key)
            if current is None:
                normalized["sourceFiles"] = [normalized["sourceFile"]]
                normalized["sourceRowIds"] = [normalized["sourceRowId"]]
                normalized["duplicateCount"] = 0
                deduped[key] = normalized
                stats["rows_unique"] += 1
            else:
                stats["rows_duplicate"] += 1
                current["duplicateCount"] = int(current.get("duplicateCount", 0)) + 1
                source_files = set(current.get("sourceFiles", []))
                source_files.add(normalized["sourceFile"])
                current["sourceFiles"] = sorted(source_files)
                source_rows = list(current.get("sourceRowIds", []))
                source_rows.append(normalized["sourceRowId"])
                current["sourceRowIds"] = source_rows
                if not str(current.get("name", "")).strip() and str(normalized.get("name", "")).strip():
                    current["name"] = normalized["name"]
                if not str(current.get("roadAddress", "")).strip() and str(normalized.get("roadAddress", "")).strip():
                    current["roadAddress"] = normalized["roadAddress"]
                if not str(current.get("lotAddress", "")).strip() and str(normalized.get("lotAddress", "")).strip():
                    current["lotAddress"] = normalized["lotAddress"]
                if not str(current.get("openHours", "")).strip() and str(normalized.get("openHours", "")).strip():
                    current["openHours"] = normalized["openHours"]

    documents = list(deduped.values())
    documents.sort(key=lambda item: str(item.get("id", "")))
    return documents, stats


def write_jsonl(path: Path, documents: List[Dict[str, object]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        for doc in documents:
            handle.write(json.dumps(doc, ensure_ascii=False) + "\n")


def detect_project_id() -> Optional[str]:
    if not GOOGLE_SERVICES_PATH.exists():
        return None
    try:
        with GOOGLE_SERVICES_PATH.open("r", encoding="utf-8") as handle:
            data = json.load(handle)
        return data.get("project_info", {}).get("project_id")
    except Exception:
        return None


def upload_to_firestore(
    documents: List[Dict[str, object]],
    service_account_path: Path,
    collection: str,
    project_id: Optional[str],
    batch_size: int = 400,
) -> None:
    try:
        import firebase_admin  # type: ignore
        from firebase_admin import credentials, firestore  # type: ignore
    except ImportError as exc:
        raise RuntimeError(
            "firebase-admin is not installed. Run: pip install firebase-admin"
        ) from exc

    if not service_account_path.exists():
        raise FileNotFoundError(f"Service account file not found: {service_account_path}")

    app_kwargs = {}
    if project_id:
        app_kwargs["projectId"] = project_id

    cred = credentials.Certificate(str(service_account_path))
    firebase_admin.initialize_app(cred, app_kwargs or None)
    db = firestore.client()

    total = len(documents)
    if total == 0:
        print("No documents to upload.")
        return

    print(f"Uploading {total} docs to Firestore collection '{collection}'...")
    committed = 0
    batch = db.batch()
    pending = 0
    for index, doc in enumerate(documents, start=1):
        doc_id = str(doc["id"])
        ref = db.collection(collection).document(doc_id)
        batch.set(ref, doc)
        pending += 1
        if pending >= batch_size:
            batch.commit()
            committed += pending
            print(f"  committed {committed}/{total}")
            batch = db.batch()
            pending = 0

    if pending > 0:
        batch.commit()
        committed += pending
        print(f"  committed {committed}/{total}")

    print("Upload complete.")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Import restroom CSV files.")
    parser.add_argument("--dry-run", action="store_true", help="Build dataset without uploading.")
    parser.add_argument(
        "--export-jsonl",
        type=Path,
        default=RESTROOM_DIR / "restrooms_master.jsonl",
        help="Write normalized deduped records to JSONL.",
    )
    parser.add_argument(
        "--upload",
        action="store_true",
        help="Upload deduped records to Firestore.",
    )
    parser.add_argument(
        "--service-account",
        type=Path,
        help="Path to Firebase service account JSON.",
    )
    parser.add_argument(
        "--collection",
        type=str,
        default="restrooms_master",
        help="Target Firestore collection name.",
    )
    parser.add_argument(
        "--project-id",
        type=str,
        default="",
        help="Override Firestore project id. If omitted, reads app/google-services.json.",
    )
    parser.add_argument(
        "--batch-size",
        type=int,
        default=400,
        help="Firestore batch size (max 500).",
    )
    parser.add_argument(
        "--start-index",
        type=int,
        default=0,
        help="Start index in sorted deduped docs for partial/resume uploads.",
    )
    parser.add_argument(
        "--max-docs",
        type=int,
        default=0,
        help="Maximum docs to upload from start index. 0 means all remaining docs.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    documents, stats = build_dataset()
    write_jsonl(args.export_jsonl, documents)
    total_docs = len(documents)
    start_index = max(0, min(args.start_index, total_docs))
    if args.max_docs > 0:
        end_index = min(total_docs, start_index + args.max_docs)
    else:
        end_index = total_docs
    selected_documents = documents[start_index:end_index]

    print("=== Restroom import summary ===")
    print(f"rows_total        : {stats['rows_total']}")
    print(f"rows_invalid_coord: {stats['rows_invalid_coord']}")
    print(f"rows_unique       : {stats['rows_unique']}")
    print(f"rows_duplicate    : {stats['rows_duplicate']}")
    print(f"export_jsonl      : {args.export_jsonl}")
    print(f"upload_range      : [{start_index}:{end_index}] ({len(selected_documents)} docs)")

    if args.upload:
        if args.service_account is None:
            print("--upload requires --service-account <path>.", file=sys.stderr)
            return 2
        project_id = args.project_id.strip() or detect_project_id()
        if not project_id:
            print("Project id not found. Pass --project-id explicitly.", file=sys.stderr)
            return 2
        upload_to_firestore(
            documents=selected_documents,
            service_account_path=args.service_account,
            collection=args.collection,
            project_id=project_id,
            batch_size=max(1, min(args.batch_size, 500)),
        )
    elif args.dry_run:
        print("Dry-run mode: upload skipped.")
    else:
        print("Upload skipped. Use --upload to push to Firestore.")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

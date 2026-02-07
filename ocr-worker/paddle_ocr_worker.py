import json
import os
import sys
from typing import Any, Dict, List


def _load_payload() -> Dict[str, Any]:
    raw = sys.stdin.read()
    if not raw:
        return {"images": []}
    return json.loads(raw)


def _ensure_env_dir(key: str) -> None:
    path = os.environ.get(key)
    if path:
        os.makedirs(path, exist_ok=True)


def _normalize_result(ocr_result: Any) -> List[str]:
    lines: List[str] = []

    if ocr_result is None:
        return lines

    # PaddleOCR returns a list where each element is a list of detections for an image.
    # Each detection is [box, (text, confidence)].
    pages = ocr_result
    if isinstance(pages, list) and len(pages) == 1 and isinstance(pages[0], list):
        pages = pages

    if isinstance(pages, list):
        for page in pages:
            if not isinstance(page, list):
                continue
            for det in page:
                try:
                    text = det[1][0]
                except Exception:
                    continue
                if text:
                    lines.append(str(text))

    return lines


def main() -> None:
    _ensure_env_dir("PADDLEOCR_HOME")
    _ensure_env_dir("PADDLE_DOWNLOAD_HOME")
    _ensure_env_dir("MPLCONFIGDIR")

    try:
        from paddleocr import PaddleOCR
        import cv2
    except Exception as exc:
        sys.stderr.write(f"PaddleOCR import failed: {exc}\n")
        sys.exit(2)

    payload = _load_payload()
    images = payload.get("images", [])
    max_side = payload.get("max_side", 2000)

    ocr = PaddleOCR(use_angle_cls=True, lang="en", show_log=False)

    results: List[Dict[str, Any]] = []
    for item in images:
        image_id = item.get("id")
        path = item.get("path")
        if not path or not os.path.exists(path):
            results.append({"id": image_id, "lines": [], "error": "Image path not found"})
            continue

        try:
            img = cv2.imread(path)
            if img is None:
                raise RuntimeError("Failed to load image")
            if isinstance(max_side, int) and max_side > 0:
                h, w = img.shape[:2]
                longest = max(h, w)
                if longest > max_side:
                    scale = max_side / float(longest)
                    img = cv2.resize(img, (int(w * scale), int(h * scale)))
            ocr_result = ocr.ocr(img, cls=True)
            lines = _normalize_result(ocr_result)
            results.append({"id": image_id, "lines": lines, "error": ""})
        except Exception as exc:
            results.append({"id": image_id, "lines": [], "error": str(exc)})

    json.dump({"results": results}, sys.stdout, ensure_ascii=False)


if __name__ == "__main__":
    main()

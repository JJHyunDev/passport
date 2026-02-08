import argparse
import os
import inspect
from pathlib import Path

def _init_ocr(lang: str):
    from paddleocr import PaddleOCR

    # PaddleOCR v2/v3 have slightly different init kwargs. Only pass what is supported.
    init_params = set(inspect.signature(PaddleOCR.__init__).parameters.keys())
    kwargs = {"lang": lang}
    if "use_textline_orientation" in init_params:
        kwargs["use_textline_orientation"] = True
    elif "use_angle_cls" in init_params:
        kwargs["use_angle_cls"] = True
    # NOTE: 'show_log' is not accepted in PaddleOCR v3.
    return PaddleOCR(**kwargs)

def main() -> None:
    parser = argparse.ArgumentParser(description="Download PaddleOCR models for offline use")
    parser.add_argument("--model-dir", default="ocr-models", help="Directory to store OCR models")
    parser.add_argument("--lang", default="en", help="OCR language (default: en)")
    parser.add_argument(
        "--disable-source-check",
        action="store_true",
        help="Skip connectivity check for model hosters (still downloads models).",
    )
    args = parser.parse_args()

    model_dir = Path(args.model_dir).resolve()
    model_dir.mkdir(parents=True, exist_ok=True)

    env_dir = model_dir / "paddle"
    env_dir.mkdir(parents=True, exist_ok=True)

    os.environ["PADDLEOCR_HOME"] = str(env_dir)
    os.environ["PADDLE_DOWNLOAD_HOME"] = str(env_dir)
    os.environ["MPLCONFIGDIR"] = str(env_dir / "mpl")
    if args.disable_source_check:
        os.environ["DISABLE_MODEL_SOURCE_CHECK"] = "True"

    _init_ocr(args.lang)
    print(f"Models downloaded to: {env_dir}")


if __name__ == "__main__":
    main()

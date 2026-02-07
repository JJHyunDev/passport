import argparse
import os
from pathlib import Path

def main() -> None:
    parser = argparse.ArgumentParser(description="Download PaddleOCR models for offline use")
    parser.add_argument("--model-dir", default="ocr-models", help="Directory to store OCR models")
    parser.add_argument("--lang", default="en", help="OCR language (default: en)")
    args = parser.parse_args()

    model_dir = Path(args.model_dir).resolve()
    model_dir.mkdir(parents=True, exist_ok=True)

    env_dir = model_dir / "paddle"
    env_dir.mkdir(parents=True, exist_ok=True)

    os.environ["PADDLEOCR_HOME"] = str(env_dir)
    os.environ["PADDLE_DOWNLOAD_HOME"] = str(env_dir)
    os.environ["MPLCONFIGDIR"] = str(env_dir / "mpl")

    from paddleocr import PaddleOCR

    PaddleOCR(use_angle_cls=True, lang=args.lang, show_log=True)
    print(f"Models downloaded to: {env_dir}")


if __name__ == "__main__":
    main()

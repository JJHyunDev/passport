import json
import os
import subprocess
import sys
import tempfile

import cv2
import numpy as np

MRZ_LINE1 = "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<"
MRZ_LINE2 = "L898902C36UTO7408122F1204159ZE184226B<<<<<10"


def make_mrz_image(path: str) -> None:
    img = np.ones((300, 1200, 3), dtype=np.uint8) * 255
    font = cv2.FONT_HERSHEY_SIMPLEX
    cv2.putText(img, MRZ_LINE1, (20, 120), font, 0.8, (0, 0, 0), 2, cv2.LINE_AA)
    cv2.putText(img, MRZ_LINE2, (20, 220), font, 0.8, (0, 0, 0), 2, cv2.LINE_AA)
    cv2.imwrite(path, img)


def main() -> None:
    with tempfile.TemporaryDirectory() as tmp:
        img_path = os.path.join(tmp, "mrz.png")
        make_mrz_image(img_path)

        payload = {"images": [{"id": 0, "path": img_path}], "max_side": 2000}
        env = dict(os.environ)
        env["PADDLEOCR_HOME"] = os.path.join(tmp, "paddleocr")
        env["PADDLE_DOWNLOAD_HOME"] = os.path.join(tmp, "paddleocr")
        env["MPLCONFIGDIR"] = os.path.join(tmp, "mpl")
        proc = subprocess.run(
            [sys.executable, "paddle_ocr_worker.py"],
            input=json.dumps(payload),
            text=True,
            capture_output=True,
            cwd=os.path.dirname(__file__),
            env=env,
        )

        print("STDOUT:\n", proc.stdout)
        print("STDERR:\n", proc.stderr)
        if proc.returncode != 0:
            raise SystemExit(proc.returncode)


if __name__ == "__main__":
    main()

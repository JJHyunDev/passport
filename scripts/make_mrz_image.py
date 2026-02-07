import os
import sys
import cv2
import numpy as np

MRZ_LINE1 = "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<"
MRZ_LINE2 = "L898902C36UTO7408122F1204159ZE184226B<<<<<10"


def main() -> None:
    out_path = sys.argv[1] if len(sys.argv) > 1 else "/tmp/mrz.png"
    img = np.ones((300, 1200, 3), dtype=np.uint8) * 255
    font = cv2.FONT_HERSHEY_SIMPLEX
    cv2.putText(img, MRZ_LINE1, (20, 120), font, 0.8, (0, 0, 0), 2, cv2.LINE_AA)
    cv2.putText(img, MRZ_LINE2, (20, 220), font, 0.8, (0, 0, 0), 2, cv2.LINE_AA)
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    cv2.imwrite(out_path, img)
    print(out_path)


if __name__ == "__main__":
    main()

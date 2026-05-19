#!/usr/bin/env python3
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "source"
OUTPUTS = ROOT / "outputs"
ASSETS = {
    "reservation-flow.html": "reservation-flow.png",
    "tenant-security-boundary.html": "tenant-security-boundary.png",
    "verification-summary.html": "verification-summary.png",
}


def main() -> None:
    OUTPUTS.mkdir(parents=True, exist_ok=True)
    for html_name, png_name in ASSETS.items():
        html = SOURCE / html_name
        png = OUTPUTS / png_name
        subprocess.run(
            [
                "playwright",
                "screenshot",
                "--viewport-size=1600,900",
                f"file://{html}",
                str(png),
            ],
            check=True,
        )
        print(f"rendered {png}")


if __name__ == "__main__":
    main()

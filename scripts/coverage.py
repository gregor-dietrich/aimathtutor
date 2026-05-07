#!/usr/bin/env python3
"""Generate a markdown coverage report from a JaCoCo CSV file."""

import csv
import sys
from typing import Dict, List, Tuple


def main() -> None:
    csv_path: str = sys.argv[1]
    report_path: str = sys.argv[2]

    with open(csv_path, "r") as file:
        rows: List[Dict[str, str]] = list(csv.DictReader(file))

    pkg_data: Dict[str, Dict[str, int]] = {}
    for r in rows:
        pkg = r["PACKAGE"]
        if pkg not in pkg_data:
            pkg_data[pkg] = {"lm": 0, "lc": 0, "bm": 0, "bc": 0}
        pkg_data[pkg]["lm"] += int(r["LINE_MISSED"])
        pkg_data[pkg]["lc"] += int(r["LINE_COVERED"])
        pkg_data[pkg]["bm"] += int(r["BRANCH_MISSED"])
        pkg_data[pkg]["bc"] += int(r["BRANCH_COVERED"])

    pkg_sorted: List[Tuple[str, Dict[str, int]]] = sorted(
        pkg_data.items(),
        key=lambda x: (
            x[1]["lc"] / (x[1]["lm"] + x[1]["lc"])
            if (x[1]["lm"] + x[1]["lc"]) > 0
            else 0
        ),
    )

    md: str = "# Coverage Report\n\n"
    md += "## By Package (sorted by line coverage)\n\n"
    md += "| Package | Lines Missed | Lines Covered | Line % | Branch % |\n"
    md += "|---------|-------------:|--------------:|-------:|---------:|\n"

    total_lm: int = 0
    total_lc: int = 0
    total_bm: int = 0
    total_bc: int = 0
    for pkg, d in pkg_sorted:
        total_lm += d["lm"]
        total_lc += d["lc"]
        total_bm += d["bm"]
        total_bc += d["bc"]
        lt: int = d["lm"] + d["lc"]
        bt: int = d["bm"] + d["bc"]
        lp = f"{d['lc'] / lt * 100:.1f}" if lt > 0 else "100.0"
        bp = f"{d['bc'] / bt * 100:.1f}" if bt > 0 else "100.0"
        md += f"| {pkg} | {d['lm']} | {d['lc']} | {lp}% | {bp}% |\n"

    tl: int = total_lm + total_lc
    tb: int = total_bm + total_bc
    md += f"| **Total** | **{total_lm}** | **{total_lc}** | **{total_lc / tl * 100:.1f}%** | **{total_bc / tb * 100:.1f}%** |\n"

    rows_sorted = sorted(
        rows,
        key=lambda r: (
            (int(r["LINE_COVERED"]) / (int(r["LINE_MISSED"]) + int(r["LINE_COVERED"])))
            if (int(r["LINE_MISSED"]) + int(r["LINE_COVERED"])) > 0
            else 0
        ),
    )

    md += "\n## By File (sorted by line coverage, worst first)\n\n"
    md += "| Package | Class | Lines Missed | Lines Covered | Line % | Branch % |\n"
    md += "|---------|-------|-------------:|--------------:|-------:|---------:|\n"
    for r in rows_sorted:
        pkg, cls = r["PACKAGE"], r["CLASS"]
        lm, lc = int(r["LINE_MISSED"]), int(r["LINE_COVERED"])
        bm, bc = int(r["BRANCH_MISSED"]), int(r["BRANCH_COVERED"])
        lt, bt = lm + lc, bm + bc
        lp = f"{lc / lt * 100:.1f}" if lt > 0 else "100.0"
        bp = f"{bc / bt * 100:.1f}" if bt > 0 else "100.0"
        md += f"| {pkg} | {cls} | {lm} | {lc} | {lp}% | {bp}% |\n"

    with open(report_path, "w") as file:
        file.write(md)

    print(f"Written to {report_path}")
    print(
        f"Overall: {total_lc / tl * 100:.1f}% lines, {total_bc / tb * 100:.1f}% branches"
    )


if __name__ == "__main__":
    main()

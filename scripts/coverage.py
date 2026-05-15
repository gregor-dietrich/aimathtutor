#!/usr/bin/env python3
"""Generate a markdown coverage report from a JaCoCo CSV file."""

import csv
import sys
from typing import Dict, List, Tuple


def calculate_coverage(rows_list: List[Dict[str, str]]) -> Tuple[float, float]:
    """Calculate line and branch coverage percentages."""
    total_lm = sum(int(r["LINE_MISSED"]) for r in rows_list)
    total_lc = sum(int(r["LINE_COVERED"]) for r in rows_list)
    total_bm = sum(int(r["BRANCH_MISSED"]) for r in rows_list)
    total_bc = sum(int(r["BRANCH_COVERED"]) for r in rows_list)

    tl = total_lm + total_lc
    tb = total_bm + total_bc

    line_pct = (total_lc / tl * 100) if tl > 0 else 100.0
    branch_pct = (total_bc / tb * 100) if tb > 0 else 100.0

    return line_pct, branch_pct


def render_table(headers: List[str], alignments: List[str], data_rows: List[List[str]]) -> str:
    """Render a padded markdown table compatible with common linting rules."""
    widths = [len(h) for h in headers]
    for row in data_rows:
        for i, cell in enumerate(row):
            widths[i] = max(widths[i], len(str(cell)))

    # Ensure at least 3 chars for separator compatibility
    widths = [max(w, 3) for w in widths]

    header_parts: List[str] = []
    for i, h in enumerate(headers):
        if alignments[i] == "right":
            header_parts.append(h.rjust(widths[i]))
        elif alignments[i] == "center":
            header_parts.append(h.center(widths[i]))
        else:
            header_parts.append(h.ljust(widths[i]))
    header_line = "| " + " | ".join(header_parts) + " |"

    sep_parts: List[str] = []
    for i, align in enumerate(alignments):
        w = widths[i]
        if align == "right":
            sep_parts.append("-" * (w - 1) + ":")
        elif align == "center":
            sep_parts.append(":" + "-" * (w - 2) + ":")
        else:
            sep_parts.append(":" + "-" * (w - 1))
    sep_line = "| " + " | ".join(sep_parts) + " |"

    data_lines: List[str] = []
    for row in data_rows:
        row_parts: List[str] = []
        for i, cell in enumerate(row):
            val = str(cell)
            if alignments[i] == "right":
                row_parts.append(val.rjust(widths[i]))
            elif alignments[i] == "center":
                row_parts.append(val.center(widths[i]))
            else:
                row_parts.append(val.ljust(widths[i]))
        data_lines.append("| " + " | ".join(row_parts) + " |")

    return "\n".join([header_line, sep_line] + data_lines) + "\n"


def main() -> None:
    if len(sys.argv) < 3:
        print("Usage: coverage.py <report_path> <csv_path1> [csv_path2 ...]")
        sys.exit(1)

    report_path: str = sys.argv[1]
    csv_paths: List[str] = sys.argv[2:]

    rows: List[Dict[str, str]] = []

    for csv_path in csv_paths:
        try:
            with open(csv_path, "r") as file:
                csv_data = list(csv.DictReader(file))
                rows.extend(csv_data)
        except FileNotFoundError:
            print(f"Warning: CSV file not found: {csv_path}")

    if not rows:
        print("Error: No coverage data found.")
        sys.exit(1)

    md: str = "# Coverage Report\n\n"

    # 1. By Package
    pkg_data: Dict[str, Dict[str, int]] = {}
    for r in rows:
        pkg = r["PACKAGE"]
        if pkg not in pkg_data:
            pkg_data[pkg] = {"lm": 0, "lc": 0, "bm": 0, "bc": 0}
        pkg_data[pkg]["lm"] += int(r["LINE_MISSED"])
        pkg_data[pkg]["lc"] += int(r["LINE_COVERED"])
        pkg_data[pkg]["bm"] += int(r["BRANCH_MISSED"])
        pkg_data[pkg]["bc"] += int(r["BRANCH_COVERED"])

    pkg_sorted = sorted(
        pkg_data.items(),
        key=lambda x: (
            x[1]["lc"] / (x[1]["lm"] + x[1]["lc"])
            if (x[1]["lm"] + x[1]["lc"]) > 0
            else 1.0
        ),
    )

    md += "## By Package (sorted by line coverage)\n\n"
    pkg_headers = ["Package", "Lines Missed",
                   "Lines Covered", "Line %", "Branch %"]
    pkg_alignments = ["left", "right", "right", "right", "right"]
    pkg_table_data: List[List[str]] = []

    total_lm = 0
    total_lc = 0
    total_bm = 0
    total_bc = 0
    for pkg, d in pkg_sorted:
        total_lm += d["lm"]
        total_lc += d["lc"]
        total_bm += d["bm"]
        total_bc += d["bc"]
        lt = d["lm"] + d["lc"]
        bt = d["bm"] + d["bc"]
        lp = f"{d['lc'] / lt * 100:.1f}%" if lt > 0 else "100.0%"
        bp = f"{d['bc'] / bt * 100:.1f}%" if bt > 0 else "100.0%"
        pkg_table_data.append([pkg, str(d["lm"]), str(d["lc"]), lp, bp])

    tl = total_lm + total_lc
    tb = total_bm + total_bc
    if tl > 0:
        pkg_table_data.append([
            "**Total**",
            f"**{total_lm}**",
            f"**{total_lc}**",
            f"**{total_lc / tl * 100:.1f}%**",
            f"**{total_bc / tb * 100:.1f}%**"
        ])
    else:
        pkg_table_data.append(["**Total**", "0", "0", "100.0%", "100.0%"])

    md += render_table(pkg_headers, pkg_alignments, pkg_table_data)
    md += "\n"

    # 2. By File
    rows_sorted = sorted(
        rows,
        key=lambda r: (
            (int(r["LINE_COVERED"]) /
             (int(r["LINE_MISSED"]) + int(r["LINE_COVERED"])))
            if (int(r["LINE_MISSED"]) + int(r["LINE_COVERED"])) > 0
            else 1.0
        ),
    )

    md += "## By File (sorted by line coverage)\n\n"
    md += "> **Note:** Files with full line _and_ branch coverage have been omitted.\n\n"
    file_headers = ["Package", "Class", "Lines Missed",
                    "Lines Covered", "Line %", "Branch %"]
    file_alignments = ["left", "left", "right", "right", "right", "right"]
    file_table_data: List[List[str]] = []

    for r in rows_sorted:
        lm, lc = int(r["LINE_MISSED"]), int(r["LINE_COVERED"])
        bm, bc = int(r["BRANCH_MISSED"]), int(r["BRANCH_COVERED"])

        # Omit files with 100% line AND branch coverage
        if lm == 0 and bm == 0:
            continue

        pkg, cls = r["PACKAGE"], r["CLASS"]
        lt, bt = lm + lc, bm + bc
        lp = f"{lc / lt * 100:.1f}%" if lt > 0 else "100.0%"
        bp = f"{bc / bt * 100:.1f}%" if bt > 0 else "100.0%"
        file_table_data.append([pkg, cls, str(lm), str(lc), lp, bp])

    md += render_table(file_headers, file_alignments, file_table_data)

    with open(report_path, "w") as file:
        file.write(md)

    print(f"Written to {report_path}")
    if tl > 0:
        print(
            f"Overall: {total_lc / tl * 100:.1f}% lines, {total_bc / tb * 100:.1f}% branches")
    else:
        print("Overall: 100.0% lines, 100.0% branches (no data)")


if __name__ == "__main__":
    main()

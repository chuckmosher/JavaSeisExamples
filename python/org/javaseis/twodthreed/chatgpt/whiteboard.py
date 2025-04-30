import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from shapely.geometry import LineString, Point
from itertools import combinations

# Assume transect_data is a list of (transect_x, transect_y, transect_z, trcno) tuples
def analyze_misties(transect_data, line_names, output_csv="/home/chuck/mistie_report.csv"):
    report_rows = []
    intersection_id = 0

    for (i, data_a), (j, data_b) in combinations(enumerate(transect_data), 2):
        x_a, y_a, z_a, trc_a = data_a
        x_b, y_b, z_b, trc_b = data_b

        line_a = LineString(zip(x_a, y_a))
        line_b = LineString(zip(x_b, y_b))

        intersection = line_a.intersection(line_b)

        if intersection.is_empty or not isinstance(intersection, Point):
            continue

        px, py = intersection.x, intersection.y

        def nearest_info(x, y, z, trc):
            dists = np.sqrt((x - px)**2 + (y - py)**2)
            idx = np.argmin(dists)
            return z[idx], trc[idx], idx

        za, trcno_a, idx_a = nearest_info(x_a, y_a, z_a, trc_a)
        zb, trcno_b, idx_b = nearest_info(x_b, y_b, z_b, trc_b)
        dz = za - zb

        report_rows.append({
            "Intersection": intersection_id,
            "LineA": line_names[i],
            "LineB": line_names[j],
            "X": px,
            "Y": py,
            "Z_A": za,
            "Z_B": zb,
            "Delta_Z": dz,
            "TrcNo_A": trcno_a,
            "TrcNo_B": trcno_b,
            "IndexA": i,
            "IndexB": j,
            "PointA": idx_a,
            "PointB": idx_b
        })

        intersection_id += 1

    df_report = pd.DataFrame(report_rows)
    df_report.to_csv(output_csv, index=False)
    print(f"Mis-tie report saved to {output_csv} with {len(df_report)} intersections.")
    return df_report

def apply_mistie_corrections(transect_data, df_report):
    corrected = []

    for i, (x, y, z, trc) in enumerate(transect_data):
        dz_total = np.zeros_like(z)
        weight_total = np.zeros_like(z)

        for _, row in df_report.iterrows():
            if row['IndexA'] == i or row['IndexB'] == i:
                idx = row['PointA'] if row['IndexA'] == i else row['PointB']
                sign = -0.5 if row['IndexA'] == i else 0.5
                dz_local = sign * row['Delta_Z']

                distances = np.sqrt((x - x[idx])**2 + (y - y[idx])**2)

                max_dist = 1e-6 + np.min([
                    np.max(np.abs(idx - row['PointA'])) if row['IndexA'] == i else np.inf,
                    np.max(np.abs(idx - row['PointB'])) if row['IndexB'] == i else np.inf
                ])
                taper = np.clip(1 - distances / max_dist, 0, 1)

                dz_total += dz_local * taper
                weight_total += taper

        z_corr = z.copy()
        mask = weight_total > 0
        z_corr[mask] += dz_total[mask] / weight_total[mask]
        corrected.append((x.copy(), y.copy(), z_corr, trc.copy()))

    return corrected

def write_corrected_to_opendtect_format(corrected_data, original_df, output_file):
    corrected_rows = []
    for (x, y, z, trc), (line_name, group) in zip(corrected_data, original_df.groupby("LineName")):
        for i in range(len(x)):
            world_x = x[i] * GRID_SPACING + GRID_ORIGIN[0]
            world_y = y[i] * GRID_SPACING + GRID_ORIGIN[1]
            elevation = z[i]
            corrected_rows.append(f"{world_x:12.2f}{world_y:12.2f}{elevation:12.2f}\n")

    with open(output_file, "w") as f:
        f.writelines(corrected_rows)
    print(f"Corrected OpendTect horizon written to {output_file}")

if __name__ == "__main__":
    import os

    file_path = "/home/chuck/Niobrara2dV4Bulk.txt"
    COLUMN_SPECS = [(0, 17), (17, 35), (35, 47), (47, 59), (59, 69), (69, 79), (79, 89)]
    COLUMN_NAMES = ["HorizonName", "LineName", "X", "Y", "TrcNo", "ShotPt", "Z"]
    DATA_TYPES = {"X": float, "Y": float, "Z": float, "TrcNo": int}
    GRID_ORIGIN = (681900, 383900)
    GRID_UPPER_RIGHT = (695100, 401170)
    GRID_SPACING = 55

    df = pd.read_fwf(file_path, colspecs=COLUMN_SPECS, names=COLUMN_NAMES, dtype=DATA_TYPES, skiprows=3)

    transect_data = []
    line_names = []

    for line_name, group in df.groupby("LineName"):
        x = (group["X"].values - GRID_ORIGIN[0]) / GRID_SPACING
        y = (group["Y"].values - GRID_ORIGIN[1]) / GRID_SPACING
        z = group["Z"].values
        trcno = group["TrcNo"].values
        transect_data.append((x, y, z, trcno))
        line_names.append(line_name)

    df_report_before = analyze_misties(transect_data, line_names)
    transect_corrected = apply_mistie_corrections(transect_data, df_report_before)
    df_report_after = analyze_misties(transect_corrected, line_names, output_csv="/home/chuck/mistie_report_corrected.csv")

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(16, 8))

    for (x, y, _, _), name in zip(transect_data, line_names):
        ax1.plot(x, y, label=name)
    sc1 = ax1.scatter(df_report_before["X"], df_report_before["Y"],
                      c=np.abs(df_report_before["Delta_Z"]),
                      s=50 + 200 * np.abs(df_report_before["Delta_Z"]) / np.max(np.abs(df_report_before["Delta_Z"])),
                      cmap="coolwarm", edgecolor="black")
    ax1.set_title("Before Correction")
    ax1.set_xlabel("X")
    ax1.set_ylabel("Y")
    plt.colorbar(sc1, ax=ax1, label="|ΔZ| (m)")

    for (x, y, _, _), name in zip(transect_corrected, line_names):
        ax2.plot(x, y, label=name)
    sc2 = ax2.scatter(df_report_after["X"], df_report_after["Y"],
                      c=np.abs(df_report_after["Delta_Z"]),
                      s=50 + 200 * np.abs(df_report_after["Delta_Z"]) / np.max(np.abs(df_report_after["Delta_Z"])),
                      cmap="coolwarm", edgecolor="black")
    ax2.set_title("After Correction")
    ax2.set_xlabel("X")
    ax2.set_ylabel("Y")
    plt.colorbar(sc2, ax=ax2, label="|ΔZ| (m)")

    plt.tight_layout()
    plt.show()

    # Write OpendTect formatted corrected output
    output_ot_path = "/home/chuck/Niobrara2dV4Bulk_corrected.txt"
    write_corrected_to_opendtect_format(transect_corrected, df, output_ot_path)

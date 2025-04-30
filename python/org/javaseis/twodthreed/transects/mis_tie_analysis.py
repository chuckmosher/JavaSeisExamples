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

        # Function to find nearest index and interpolate value
        def nearest_info(x, y, z, trc):
            dists = np.sqrt((x - px)**2 + (y - py)**2)
            idx = np.argmin(dists)
            return z[idx], trc[idx]

        za, trcno_a = nearest_info(x_a, y_a, z_a, trc_a)
        zb, trcno_b = nearest_info(x_b, y_b, z_b, trc_b)
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
            "TrcNo_B": trcno_b
        })

        intersection_id += 1

    df_report = pd.DataFrame(report_rows)
    df_report.to_csv(output_csv, index=False)
    print(f"Mis-tie report saved to {output_csv} with {len(df_report)} intersections.")
    return df_report

if __name__ == "__main__":
    import os

    # Define file path and read data
    file_path = "/home/chuck/Niobrara2dV4Bulk.txt"
    COLUMN_SPECS = [(0, 17), (17, 35), (35, 47), (47, 59), (59, 69), (69, 79), (79, 89)]
    COLUMN_NAMES = ["HorizonName", "LineName", "X", "Y", "TrcNo", "ShotPt", "Z"]
    DATA_TYPES = {"X": float, "Y": float, "Z": float, "TrcNo": int}
    GRID_ORIGIN = (681900, 383900)  # Lower-left corner (X, Y)
    GRID_UPPER_RIGHT = (695100, 401170)  # Upper-right corner (X, Y)
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

    # Analyze misties and get DataFrame
    df_report = analyze_misties(transect_data, line_names)

    # Visualization
    fig, ax = plt.subplots(figsize=(10, 8))

    # Plot transects
    for (x, y, _, _), name in zip(transect_data, line_names):
        ax.plot(x, y, label=name)

    # Plot mis-tie intersections with color/size by Delta_Z
    sc = ax.scatter(df_report["X"], df_report["Y"], 
                    c=np.abs(df_report["Delta_Z"]), 
                    s=50 + 200 * np.abs(df_report["Delta_Z"]) / np.max(np.abs(df_report["Delta_Z"])),
                    cmap="coolwarm", edgecolor="black", label="Mis-ties")
    cbar = plt.colorbar(sc, ax=ax)
    cbar.set_label("|ΔZ| (m)")

    ax.set_title("Transects and Mis-tie Intersections")
    ax.set_xlabel("X (grid index)")
    ax.set_ylabel("Y (grid index)")
    ax.legend(loc="upper right")
    plt.tight_layout()
    plt.show()

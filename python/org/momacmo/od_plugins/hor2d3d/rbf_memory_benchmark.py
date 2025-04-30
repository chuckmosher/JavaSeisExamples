import numpy as np
import tracemalloc
import psutil
import os
import time
import resource
import pandas as pd
import matplotlib.pyplot as plt

from org.momacmo.od_plugins.hor2d3d.interpolation import RBFInterpolation

def benchmark_rbf_memory():
    print("\n📊 Starting RBF memory benchmark...\n")
    sizes = [500, 1000, 2000, 4000, 8000]
    grid_sizes = [(100, 100), (200, 200), (400, 400), (600, 600)]

    results = []

    for N in sizes:
        gx = np.random.uniform(0, 1, N)
        gy = np.random.uniform(0, 1, N)
        z = np.sin(gx) + np.cos(gy)

        for NX, NY in grid_sizes:
            rbf_matrix_bytes = N * N * 8
            grid_eval_bytes = NX * NY * 8
            est_mb = 3 * (rbf_matrix_bytes + grid_eval_bytes) / 1024**2

            print(f"N={N:5}, NX×NY={NX}×{NY}, est_mem={est_mb:8.1f} MB", end=" → ")

            try:
                interp = RBFInterpolation(function='multiquadric', epsilon=10, smooth=1.0)
                start = time.perf_counter()
                gx_axis = np.arange(NX)
                gy_axis = np.arange(NY)
                zgrid = interp.interpolate(gx, gy, z, NX, NY)
                elapsed = time.perf_counter() - start

                mem_mb = resource.getrusage(resource.RUSAGE_SELF).ru_maxrss / 1024  # in MB
                print(f"peak_mem={mem_mb:8.1f} MB, time={elapsed:5.2f} sec")
                results.append((N, NX, NY, est_mb, mem_mb, elapsed))

            except Exception as e:
                print(f"💥 failed: {e}")

    # Fit using NumPy least squares: Peak_MB ≈ a*N^2 + b*(NX*NY) + c
    df = pd.DataFrame(results, columns=["N", "NX", "NY", "Est_MB", "Peak_MB", "Time_sec"])
    df["N2"] = df["N"] ** 2
    df["Grid"] = df["NX"] * df["NY"]
    X = np.column_stack([df["N2"], df["Grid"], np.ones(len(df))])
    y = df["Peak_MB"].values
    coeffs, _, _, _ = np.linalg.lstsq(X, y, rcond=None)
    a, b, c = coeffs
    print("\n🧠 Empirical memory model:")
    print(f"Estimated_MB ≈ {a:.6f} * N^2 + {b:.6f} * (NX * NY) + {c:.1f}")

    # Plotting results: Actual vs Predicted memory usage
    df["Predicted_MB"] = a * df["N2"] + b * df["Grid"] + c

    plt.figure(figsize=(8, 6))
    plt.scatter(df["Peak_MB"], df["Predicted_MB"], c="blue", label="Samples")
    plt.plot([df["Peak_MB"].min(), df["Peak_MB"].max()],
             [df["Peak_MB"].min(), df["Peak_MB"].max()],
             'r--', label="Ideal Fit")
    plt.xlabel("Actual Peak Memory (MB)")
    plt.ylabel("Predicted Memory (MB)")
    plt.title("RBF Memory Prediction Model Fit")
    plt.legend()
    plt.grid(True)
    plt.tight_layout()
    plt.show()

    return results

if __name__ == "__main__":
    benchmark_rbf_memory()

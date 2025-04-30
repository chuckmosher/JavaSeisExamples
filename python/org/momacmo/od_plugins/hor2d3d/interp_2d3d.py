import numpy as np
import matplotlib.pyplot as plt
import tracemalloc
import psutil
import os
import xarray as xr
import argparse

from odbind.survey import Survey
from odbind.horizon2d import Horizon2D
from odbind.horizon3d import Horizon3D
from org.momacmo.od_plugins.hor2d3d.interpolation import get_interpolator, perform_interpolation
from org.javaseis.pygis.bingrid import BinGrid

def get_transect_data(survey: Survey, bingrid: BinGrid, horizon_name: str):
    horizon = Horizon2D(survey, horizon_name)
    gx_list, gy_list, z_list = [], [], []

    for lineid in horizon.lineids():
        x, y, _ = horizon.getxy(lineid)
        z = horizon.getz(lineid)

        for xi, yi, zi in zip(x, y, z):
            if not np.isnan(zi):
                gx, gy = bingrid.world_to_grid(xi, yi)
                gx_list.append(gx)
                gy_list.append(gy)
                z_list.append(zi)

    return np.array(gx_list), np.array(gy_list), np.array(z_list)

def write_horizon3d(survey, horizon3d_name, gx_grid, gy_grid, zgrid, xl0, yl0, xln, yln):
    try:
        NY, NX = zgrid.shape

        inl_rg = [int(yl0), int(yln), 1]
        crl_rg = [int(xl0), int(xln), 1]

        if horizon3d_name in survey.get_object_names("Horizon3D"):
            print(f"⚠️ 3D Horizon '{horizon3d_name}' already exists. Skipping creation.")
            horizon_3d = Horizon3D(survey, horizon3d_name)
        else:
            horizon_3d = Horizon3D.create(survey, horizon3d_name, inl_rg, crl_rg)

        if Horizon3D.use_xarray:
            iline = np.arange(int(yl0), int(yln) + 1)
            xline = np.arange(int(xl0), int(xln) + 1)

            if gx_grid.shape != (NY, NX) or gy_grid.shape != (NY, NX):
                raise ValueError(f"gx and gy grids must match zgrid shape {zgrid.shape}")

            z_data_xr = xr.DataArray(
                zgrid.astype(np.float32),
                dims=['iline', 'xline'],
                coords={
                    'iline': iline,
                    'xline': xline,
                    'x': (['iline', 'xline'], gx_grid),
                    'y': (['iline', 'xline'], gy_grid),
                }
            )

            xr_data = xr.Dataset({'z': z_data_xr})

            horizon_3d.putdata(xr_data)

        else:
            horizon_3d.put_z(zgrid.astype(np.float32), inl_rg, crl_rg)

        print(f"✅ 3D Horizon '{horizon3d_name}' written and saved.")

    except Exception as e:
        print(f"❌ Error writing Horizon3D: {e}")

def parse_arguments():
    parser = argparse.ArgumentParser(description="Create Horizon3D from Horizon2D using interpolation.")
    parser.add_argument("--data_root", required=True, help="Path to OpendTect data root.")
    parser.add_argument("--survey", required=True, help="Name of OpendTect survey.")
    parser.add_argument("--horizon2d", required=True, help="Name of input 2D horizon.")
    parser.add_argument("--horizon3d", required=True, help="Name of output 3D horizon.")
    parser.add_argument("--method", default="linear", choices=["linear", "nearest", "rbf"], help="Interpolation method.")
    parser.add_argument("--xl0", type=float, required=True, help="Starting crossline number.")
    parser.add_argument("--xln", type=float, required=True, help="Ending crossline number.")
    parser.add_argument("--il0", type=float, required=True, help="Starting inline number.")
    parser.add_argument("--iln", type=float, required=True, help="Ending inline number.")
    parser.add_argument("--smooth", type=float, default=50.0, help="Smoothing factor for RBF interpolation (default 50.0).")
    parser.add_argument("--epsilon", type=float, default=10.0, help="Epsilon parameter for RBF basis functions (default 10.0).")
    return parser.parse_args()


def main():

    args = parse_arguments()

    data_root = "/data2/home/data/ODData"
    survey_name = "Penobscot"

    bg = BinGrid(
        nx=482,
        ny=601,
        x0=731982.0,
        y0=4890109.0,
        dx=25.0,
        dy=12.5,
        lx0=1000, ly0=1000,
        ldx=1, ldy=1,
        angle=90.0-62.39
    )

    survey = Survey(survey_name, data_root)

    gx, gy, z = get_transect_data(survey, bg, args.horizon2d)

    NX = int(args.xln - args.xl0) + 1
    NY = int(args.iln - args.il0) + 1

    interpolation_method = get_interpolator(
        args.method,
        function='linear',   # only meaningful for RBF, safe default
        epsilon=args.epsilon,
        smooth=args.smooth
    )

    gy_axis, gx_axis, zgrid = perform_interpolation(
        interpolation_method,
        gx, gy, z,
        NX, NY,
        0.0, bg.dx,
        0.0, bg.dy
    )

    gx_grid, gy_grid = np.meshgrid(
        np.linspace(args.xl0, args.xln, NX),
        np.linspace(args.il0, args.iln, NY)
    )

    write_horizon3d(
        survey,
        args.horizon3d,
        gx_grid,
        gy_grid,
        zgrid,
        args.xl0,
        args.il0,
        args.xln,
        args.iln
    )

if __name__ == "__main__":
    main()

import numpy as np
import matplotlib.pyplot as plt
import tracemalloc
import psutil
import os
import xarray as xr

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

def export_zmap(grid: BinGrid, zgrid: np.ndarray, output_file: str):
    if zgrid.shape != (grid.ny, grid.nx):
        raise ValueError(f"Shape mismatch: zgrid shape {zgrid.shape} vs expected ({grid.ny},{grid.nx})")

    if os.path.exists(output_file):
        raise FileExistsError(f"Output file {output_file} already exists. Refusing to overwrite.")

    zmin = np.nanmin(zgrid)
    zmax = np.nanmax(zgrid)

    with open(output_file, 'w') as f:
        f.write("! ZMAP FORMATTED GRID FILE\n")
        f.write(f"{grid.nx} {grid.ny}\n")

        corners = [
            grid.index_to_world(0, 0),
            grid.index_to_world(grid.nx-1, 0),
            grid.index_to_world(0, grid.ny-1),
            grid.index_to_world(grid.nx-1, grid.ny-1)
        ]
        xs = [c[0] for c in corners]
        ys = [c[1] for c in corners]
        xmin, xmax = min(xs), max(xs)
        ymin, ymax = min(ys), max(ys)

        f.write(f"{xmin:.2f} {xmax:.2f} {ymin:.2f} {ymax:.2f}\n")
        f.write(f"{zmin:.3f} {zmax:.3f}\n")

        for iy in range(grid.ny):
            for ix in range(grid.nx):
                wx, wy = grid.index_to_world(ix, iy)
                zval = zgrid[iy, ix]
                f.write(f"{wx:.2f} {wy:.2f} {zval:.3f}\n")
                
def write_horizon3d(survey: Survey, horizon3d_name: str, bingrid: BinGrid, zgrid: np.ndarray):
    """
    Write interpolated grid (zgrid) to an OpendTect 3D Horizon using BinGrid for coordinates.
    
    Parameters
    ----------
    survey : Survey
        OpendTect survey object
    horizon3d_name : str
        Name of the 3D horizon to create
    bingrid : BinGrid
        BinGrid object describing the grid geometry
    zgrid : np.ndarray
        2D grid of interpolated Z values, shape (ny, nx)
    """
    try:
        # Verify shape matches grid
        if zgrid.shape != (bingrid.ny, bingrid.nx):
            raise ValueError(f"zgrid shape {zgrid.shape} does not match BinGrid ({bingrid.ny},{bingrid.nx})")

        # Horizon inline and crossline ranges
        inl_rg = [bingrid.ly0, bingrid.ly0 + (bingrid.ny - 1) * bingrid.ldy, bingrid.ldy]
        crl_rg = [bingrid.lx0, bingrid.lx0 + (bingrid.nx - 1) * bingrid.ldx, bingrid.ldx]

        # Create or open Horizon3D
        if horizon3d_name in survey.get_object_names("Horizon3D"):
            print(f"⚠️ Horizon '{horizon3d_name}' already exists, opening existing horizon.")
            horizon = Horizon3D(survey, horizon3d_name)
        else:
            print(f"➕ Creating new Horizon '{horizon3d_name}'.")
            horizon = Horizon3D.create(survey, horizon3d_name, inl_rg, crl_rg)

        # Build coordinates for xarray
        ilines = np.arange(inl_rg[0], inl_rg[1] + inl_rg[2], inl_rg[2], dtype=np.int32)
        xlines = np.arange(crl_rg[0], crl_rg[1] + crl_rg[2], crl_rg[2], dtype=np.int32)

        # Generate physical (world) coordinates
        gx = np.zeros((bingrid.ny, bingrid.nx), dtype=np.float64)
        gy = np.zeros((bingrid.ny, bingrid.nx), dtype=np.float64)

        for iy in range(bingrid.ny):
            for ix in range(bingrid.nx):
                gx[iy, ix], gy[iy, ix] = bingrid.index_to_world(ix, iy)

        # Build xarray Dataset
        z_data = xr.DataArray(
            zgrid.astype(np.float32),
            dims=('iline', 'xline'),
            coords={
                'iline': ilines,
                'xline': xlines,
                'x': (('iline', 'xline'), gx),
                'y': (('iline', 'xline'), gy),
            },
            name="z"
        )

        ds = xr.Dataset({'z': z_data})

        # Write to horizon
        if Horizon3D.use_xarray:
            horizon.putdata(ds)
            print(f"✅ 3D Horizon '{horizon3d_name}' successfully written to OpendTect.")

    except Exception as e:
        print(f"❌ Error writing Horizon3D: {e}")


def main():
    data_root = "/data2/home/data/ODData"
    zmap_root = "/home/chuck/ODData/Penobscot/Rawdata/zmap/"
    survey_name = "Penobscot"
    horizon2d_name = "Base-O"
    horizon3d_name = "Base-O_3D"

    bg = BinGrid(
        nx=482,
        ny=601,
        x0=731982.0,
        y0=4890109.0,
        dx=25.0,
        dy=12.5,
        lx0=1000, ly0=1000,
        ldx=1, ldy=1,
        angle= 90 - 62.39
    )

    survey = Survey(survey_name, data_root)

    gx, gy, z = get_transect_data(survey, bg, horizon2d_name)

    method = "linear"
    interpolation_method = get_interpolator(method)

    gy_axis, gx_axis, zgrid = perform_interpolation(
        interpolation_method,
        gx, gy, z,
        bg.nx, bg.ny, 
        0.0, bg.dx,
        0.0, bg.dy
    )

    # Plot interpolated zgrid immediately (grid space)
    plt.figure(figsize=(8,6))
    plt.contourf(gx_axis, gy_axis, zgrid, levels=40, cmap='terrain')
    plt.title("Interpolated ZGrid (Grid Space)")
    plt.xlabel("Grid X")
    plt.ylabel("Grid Y")
    plt.colorbar(label="Z")
    plt.gca().set_aspect('equal')
    plt.show()

    output_zmap_path = zmap_root + horizon3d_name + ".zmap"
    export_zmap(bg, zgrid, output_zmap_path)
    print(f"✅ ZMAP exported to {output_zmap_path}")

    # Read ZMAP back
    world_x = []
    world_y = []
    z_read = []
    with open(output_zmap_path, 'r') as f:
        lines = f.readlines()
        data_lines = lines[5:]  # Skip headers
        for line in data_lines:
            parts = line.strip().split()
            if len(parts) == 3:
                world_x.append(float(parts[0]))
                world_y.append(float(parts[1]))
                z_read.append(float(parts[2]))

    world_x = np.array(world_x)
    world_y = np.array(world_y)
    z_read = np.array(z_read)

    plt.figure(figsize=(8,6))
    plt.tricontourf(world_x, world_y, z_read, levels=40, cmap='terrain')
    plt.title("ZGrid Read Back from ZMAP (World Coordinates)")
    plt.xlabel("World X")
    plt.ylabel("World Y")
    plt.colorbar(label="Z")
    plt.gca().set_aspect('equal')
    plt.show()

if __name__ == "__main__":
    main()

import numpy as np
import matplotlib.pyplot as plt
from matplotlib.patches import Circle
from org.javaseis.pygis.bingrid import BinGrid  # assuming your BinGrid class is in BinGrid.py
734338.86
# Initialize a BinGrid
grid = BinGrid(nx=481, ny=601, x0=734338.86, y0=4893837.87, dx=25.0, dy=12.5, lx0=1000, ly0=1000, ldx=1, ldy=1, angle=90-62.39)

# Create the grid points
gx = np.arange(grid.nx) * grid.dx
gy = np.arange(grid.ny) * grid.dy
GX, GY = np.meshgrid(gx, gy)

# Map to world coordinates
WX, WY = grid.grid_to_world(GX, GY)

# Create the figure and axes
fig, (ax_grid, ax_world) = plt.subplots(1, 2, figsize=(12, 6))

# Plot grid coordinates
ax_grid.set_title("Grid Coordinates (gx, gy)")
ax_grid.plot(GX.flatten(), GY.flatten(), 'k.', markersize=2)
ax_grid.set_xlabel('gx')
ax_grid.set_ylabel('gy')
ax_grid.set_aspect('equal')

# Plot world coordinates
ax_world.set_title("World Coordinates (wx, wy)")
ax_world.plot(WX.flatten(), WY.flatten(), 'b.', markersize=2)
ax_world.set_xlabel('wx')
ax_world.set_ylabel('wy')
ax_world.set_aspect('equal')

# Add a marker that will move
grid_marker, = ax_grid.plot([], [], 'ro', markersize=8)
world_marker, = ax_world.plot([], [], 'go', markersize=8)

# Add dynamic text for coordinates
coord_text = fig.text(0.5, 0.95, '', ha='center', va='top', fontsize=12)

def on_mouse_move(event):
    if event.inaxes == ax_grid and event.xdata is not None and event.ydata is not None:
        # Mouse is inside the grid axes
        gx, gy = event.xdata, event.ydata
        # Update grid marker
        grid_marker.set_data([gx], [gy])

        # Convert to world coordinates
        wx, wy = grid.grid_to_world(gx, gy)
        # Update world marker
        world_marker.set_data([wx], [wy])

        # Update coordinate text
        coord_text.set_text(f"Grid (gx={gx:.2f}, gy={gy:.2f})  →  World (wx={wx:.2f}, wy={wy:.2f})")

        fig.canvas.draw_idle()

# Connect the motion event
fig.canvas.mpl_connect('motion_notify_event', on_mouse_move)

plt.tight_layout()
plt.show()

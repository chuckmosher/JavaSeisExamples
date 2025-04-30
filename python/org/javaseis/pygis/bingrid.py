import numpy as np

class BinGrid:
    def __init__(self, nx, ny, x0=0.0, y0=0.0, dx=1.0, dy=1.0, lx0=1, ly0=1, ldx=1, ldy=1, angle=0.0):
        self.nx = nx
        self.ny = ny
        self.x0 = x0
        self.y0 = y0
        self.dx = dx
        self.dy = dy
        self.angle = angle
        self.lx0 = lx0  # logical x origin
        self.ly0 = ly0  # logical y origin
        self.ldx = ldx  # logical x increment (typically 1)
        self.ldy = ldy  # logical y increment (typically 1)

        theta = np.deg2rad(angle)
        self.cos_theta = np.cos(theta)
        self.sin_theta = np.sin(theta)

    def world_to_grid(self, wx, wy):
        dx = wx - self.x0
        dy = wy - self.y0
        gx = (dx * self.cos_theta + dy * self.sin_theta)
        gy = (-dx * self.sin_theta + dy * self.cos_theta)
        return gx, gy

    def grid_to_world(self, gx, gy):
        wx = self.x0 + (gx * self.cos_theta - gy * self.sin_theta)
        wy = self.y0 + (gx * self.sin_theta + gy * self.cos_theta)
        return wx, wy

    def grid_to_logical(self, gx, gy):
        ix,iy = self.grid_to_index( gx, gy )
        return self.index_to_logical(ix,iy)

    def logical_to_grid(self, lx, ly):
        ix,iy = self.logical_to_index( lx, ly)
        return self.index_to_grid( ix, iy )

    def world_to_index(self, wx, wy):
        gx, gy = self.world_to_grid(wx, wy)
        return self.grid_to_index(gx, gy)

    def index_to_world(self, ix, iy):
        gx, gy = self.index_to_grid( ix, iy )
        return self.grid_to_world(gx, gy)

    def grid_to_index(self, gx, gy):
        return int(round(gx/self.dx)), int(round(gy/self.dy))

    def index_to_grid(self, ix, iy):
        return self.dx*ix, self.dy*iy

    def logical_to_index(self, lx, ly):
        return (lx-self.lx0)/self.ldx, (ly-self.ly0)/self.ldy

    def index_to_logical(self, ix, iy):
        return self.lx0 + ix*self.ldx, self.ly0 + iy*self.ldy 

    def to_dict(self):
        return {
            'nx': self.nx,
            'ny': self.ny,
            'x0': self.x0,
            'y0': self.y0,
            'dx': self.dx,
            'dy': self.dy,
            'lx0': self.lx0,
            'ly0': self.ly0,
            'ldx': self.ldx,
            'ldy': self.ldy,
            'angle': self.angle
        }

    @classmethod
    def from_dict(cls, d):
        return cls(
            d['nx'], d['ny'],
            d['x0'], d['y0'],
            d['dx'], d['dy'],
            d.get('lx0', 1), d.get('ly0', 1),
            d.get('ldx', 1), d.get('ldy', 1),
            d.get('angle', 0.0)
        )

if __name__ == "__main__":
    grid = BinGrid(nx=100, ny=100, x0=500000, y0=6000000, dx=25.0, dy=25.0, angle=30)

    gx0, gy0 = (1250.0, 1750.0)
    wx, wy = grid.grid_to_world(1250, 1750)
    print(f"Grid ({gx0:.2f}, {gy0:.2f}) -> World ({wx:.2f}, {wy:.2f})")

    gx, gy = grid.world_to_grid(wx, wy)
    print(f"World ({wx:.2f}, {wy:.2f}) -> Grid ({gx:.2f}, {gy:.2f})")

    lx, ly = grid.grid_to_logical(gx, gy)
    print(f"Grid ({gx:.2f},{gy:.2f}) -> Logical ({lx:.2f},{ly:.2f})")

    ix, iy = grid.grid_to_index(gx, gy)
    print(f"Grid ({gx:.2f},{gy:.2f}) -> Index ({ix},{iy})")

    wx2, wy2 = grid.index_to_world(ix, iy)
    print(f"Index ({ix},{iy}) -> World ({wx2:.2f},{wy2:.2f})")

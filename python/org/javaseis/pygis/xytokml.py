from pyproj import Transformer
import simplekml

def xytokml(filename, x1, y1, x2, y2):
    """
    Write a KML file showing a bounding box defined in NAD27 Wyoming East Central (EPSG:32042).
    Args:
        filename (str): Path to output KML file
        x1, y1 (float): Lower-left corner (X, Y) in NAD27 Wyoming East Central
        x2, y2 (float): Upper-right corner (X, Y) in NAD27 Wyoming East Central
    """
    # Set up transformer from NAD27 Wyoming East Central to WGS84
    transformer = Transformer.from_crs("EPSG:32042", "EPSG:4326", always_xy=True)

    # Define corners (closing the loop at the end)
    nad27_corners = [
        (x1, y1),  # Lower left
        (x2, y1),  # Lower right
        (x2, y2),  # Upper right
        (x1, y2),  # Upper left
        (x1, y1)   # Close the loop
    ]

    # Transform to lon/lat
    lonlat_corners = [transformer.transform(x, y) for (x, y) in nad27_corners]

    # Create KML polygon
    kml = simplekml.Kml()
    pol = kml.newpolygon(name="Grid Box", outerboundaryis=lonlat_corners)
    pol.style.polystyle.color = "7dff0000"  # Red with some transparency
    pol.style.linestyle.width = 2

    # Save file
    kml.save(filename)
    print(f"KML file saved to {filename}")

# Example usage
if __name__ == "__main__":
    xytokml("/home/chuck/Dropbox/Vector Minerals/tables/3D_Seismic.kml", x1=681900, y1=383900, x2=695100, y2=401170)

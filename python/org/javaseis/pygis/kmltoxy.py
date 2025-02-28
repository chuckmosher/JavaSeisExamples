import xml.etree.ElementTree as ET
import pandas as pd
from pyproj import Transformer

# Define the transformation from WGS84 (EPSG:4326) to NAD27 Wyoming East Central (EPSG:32042)
transformer = Transformer.from_crs("EPSG:4326", "EPSG:32056", always_xy=True)

def convert_latlon_to_nad27_xy(lon, lat):
    """Convert WGS84 (lat, lon) to NAD27 Wyoming East Central (X, Y)."""
    x, y = transformer.transform(lon, lat)
    return x, y

def extract_tracks_from_kml(kml_file):
    """Extract tracks (latitude, longitude) from a KML file and convert them to NAD27 X, Y."""
    tree = ET.parse(kml_file)
    root = tree.getroot()

    # Namespace for parsing KML
    ns = {"kml": "http://www.opengis.net/kml/2.2"}

    tracks = []
    for placemark in root.findall(".//{http://www.opengis.net/kml/2.2}Placemark"):
        name = placemark.find("{http://www.opengis.net/kml/2.2}name")
        name = name.text if name is not None else "Unnamed"

        coordinates = placemark.find(".//{http://www.opengis.net/kml/2.2}coordinates")
        if coordinates is not None:
            coord_text = coordinates.text.strip()
            for i, coord in enumerate(coord_text.split()):
                lon, lat, *_ = map(float, coord.split(","))
                x, y = convert_latlon_to_nad27_xy(lon, lat)
                tracks.append((name, i + 1, x, y))

    return tracks

# Load KML file
kml_file = "/home/chuck/Dropbox/Vector Minerals/tables/WellTracks.kml"  # Replace with your actual KML file path

# Extract and convert tracks
tracks_data = extract_tracks_from_kml(kml_file)

# Convert to DataFrame and save
df_tracks = pd.DataFrame(tracks_data, columns=["Track", "Point", "X", "Y"])
df_tracks.to_csv("converted_tracks.csv", index=False)

print("Conversion complete! Saved as 'converted_tracks.csv'")

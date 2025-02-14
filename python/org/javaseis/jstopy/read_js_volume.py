import numpy as np
import os
import xml.etree.ElementTree as ET

class JsVolumeToPy:
    '''
    A class to represent a 3D volume and its associated metadata (read from XML).
    This class can read the binary volume data and metadata, and store them for further use.
    '''
    
    def __init__(self, data_filename, dtype=np.float32):
        '''
        Initializes a JsVolumeToPy object by reading a binary file and associated XML metadata.
        
        Args:
            data_filename (str): Path to the binary data file (.bin).
            dtype (numpy.dtype, optional): The data type of the array (default: np.float32).
        '''
        self.data_filename = data_filename
        self.dtype = dtype
        
        # Read metadata from XML
        self.metadata = self.read_metadata_from_xml()

        # Read binary volume data and store as numpy array
        self.array = self.read_volume_data()

    def read_metadata_from_xml(self):
        '''
        Reads metadata from the XML file based on the binary file name.
        
        Returns:
            dict: Metadata dictionary containing shape, coordinate frame, etc.
        '''
        xml_filename = os.path.splitext(self.data_filename)[0] + ".xml"
        if not os.path.exists(xml_filename):
            raise FileNotFoundError(f"Metadata XML file not found: {xml_filename}")

        tree = ET.parse(xml_filename)
        root = tree.getroot()
        
        # Extracting shape and coordinate frame from XML
        shape_element = root.find("shape")
        if shape_element is None:
            raise ValueError("Missing <shape> element in XML metadata.")
        
        shape = (
            int(shape_element.get("depth")),
            int(shape_element.get("height")),
            int(shape_element.get("width")),
        )

        coord_frame_element = root.find("coordinate_frame")
        coordinate_frame = coord_frame_element.text if coord_frame_element is not None else "Unknown"

        metadata = {
            "shape": shape,
            "coordinate_frame": coordinate_frame,
            "xml_filename": os.path.basename(xml_filename),
        }

        return metadata

    def read_volume_data(self):
        '''
        Reads binary data from the file and reshapes it based on the extracted metadata shape.
        
        Returns:
            numpy.ndarray: The 3D array representing the volume.
        '''
        shape = self.metadata["shape"]
        expected_size = np.prod(shape) * np.dtype(self.dtype).itemsize

        with open(self.data_filename, "rb") as f:
            data = f.read()

        if len(data) != expected_size:
            raise ValueError(f"File size {len(data)} bytes does not match expected {expected_size} bytes for shape {shape}.")

        return np.frombuffer(data, dtype=self.dtype).reshape(shape)

    def __repr__(self):
        '''Representation of the JsVolumeToPy object for easier debugging.'''
        return f"JsVolumeToPy(shape={self.metadata['shape']}, dtype={self.dtype}, coordinate_frame={self.metadata['coordinate_frame']})"

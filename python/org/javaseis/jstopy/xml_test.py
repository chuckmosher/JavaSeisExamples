import os
import xml.etree.ElementTree as ET


def main():
    print("Hello, Python!")
    xml_filename = "/Users/chuck/Projects/SEG-ACTI/SegActiShotNo1.js/FileProperties.xml"
    tree = ET.parse(xml_filename)
    root = tree.getroot()
    print(root)   
    # Extracting shape and coordinate frame from XML
    shape_element = root.find("AxisLengths")
    
if __name__ == "__main__":
    main()
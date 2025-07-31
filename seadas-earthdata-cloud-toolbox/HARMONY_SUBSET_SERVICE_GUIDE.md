# Harmony Subset Service Guide

## Overview
The Harmony Subset Service allows you to request custom data subsets from NASA's Earthdata Cloud through the Harmony API. This feature enables you to extract specific spatial regions, temporal ranges, and variables from large satellite data files without downloading the entire dataset.

## How to Use

### Method 1: From Search Results (Recommended)
1. **Search for Data**: Use the existing search functionality to find data files
2. **Select a File**: Check the checkbox next to the file you want to subset
3. **Click "Subset"**: Click the "Subset" button in the pagination panel
4. **Configure Parameters**: The Harmony Subset Service dialog will open with the file URL pre-populated

### Method 2: Direct URL Input
1. **Open Subset Service**: Access the Harmony Subset Service from the menu
2. **Enter URL**: Manually enter the URL of the data file you want to subset
3. **Validate**: Click "Validate URL" to ensure the file is accessible
4. **Configure Parameters**: Set your subset parameters

## Subset Parameters

### Input Tab
- **Data File URL**: The URL of the source data file (auto-populated when using search results)
- **Validate URL**: Check if the URL is accessible and contains valid data

### Subset Parameters Tab
- **Spatial Bounds**: Define the geographic region
  - Lat Min/Max: Latitude range (e.g., 30.0 to 45.0)
  - Lon Min/Max: Longitude range (e.g., -80.0 to -60.0)
  - *Note: When accessed from search results, these fields are automatically pre-filled with the current search bounds but remain fully editable*
- **Temporal Bounds**: Define the time period
  - Start Date: Beginning of the time range
  - End Date: End of the time range
- **Variables**: Select specific data variables to include
  - Variables are automatically extracted from the selected file's metadata
  - Common variables: chlor_a, aot_869, Rrs_443, Rrs_555, Rrs_670
  - *Note: The variable list is populated based on the actual variables available in the selected file*

### Output Options Tab
- **Output Format**: Choose the file format
  - application/x-netcdf4 (NetCDF-4)
  - image/tiff (GeoTIFF)
  - application/x-hdf5 (HDF5)
- **Coordinate System**: Select the coordinate reference system
  - EPSG:4326 (WGS84)
  - EPSG:3857 (Web Mercator)
  - EPSG:32632/32633 (UTM zones)

## Workflow

1. **Search for Data**: Use the search functionality with your desired spatial bounds
2. **Select a File**: Check the checkbox next to the file you want to subset
3. **Click "Subset"**: Click the "Subset" button in the pagination panel
4. **Review Spatial Bounds**: The subset dialog opens with spatial bounds automatically pre-filled from your search
5. **Wait for Metadata**: The dialog automatically fetches file metadata to populate available variables
6. **Configure Parameters**: Adjust subset parameters as needed (spatial bounds remain editable)
7. **Request Subset**: Click "Request Subset" to submit your request
8. **Processing**: Harmony will process your request (this may take several minutes)
9. **Job Monitoring**: The dialog shows progress and job status
10. **Download Results**: Once complete, the subset files will be automatically downloaded
11. **File Location**: Files are saved to your selected download directory

## Supported Data Collections

The subset service works with various NASA ocean color data collections:
- **PACE OCI**: Plankton, Aerosol, Cloud, ocean Ecosystem data
- **MODIS**: Moderate Resolution Imaging Spectroradiometer data
- **VIIRS**: Visible Infrared Imaging Radiometer Suite data
- **SeaWiFS**: Sea-viewing Wide Field-of-view Sensor data

## Tips for Best Results

1. **Start Small**: Begin with small spatial regions to test the service
2. **Check Variables**: Ensure the variables you select are available in the source data
3. **Time Ranges**: Use reasonable time ranges to avoid very large requests
4. **Format Selection**: Choose NetCDF-4 for scientific analysis, GeoTIFF for GIS applications
5. **Coordinate Systems**: Use EPSG:4326 for global datasets, UTM for regional analysis

## Troubleshooting

### Common Issues
- **Authentication Errors**: Ensure your Earthdata credentials are properly configured
- **Invalid URLs**: Use the search functionality to get valid file URLs
- **Timeout Errors**: Large requests may take longer; be patient
- **Variable Not Found**: Check the source data metadata for available variables

### Error Messages
- **"No files selected"**: Select a file from the search results before clicking Subset
- **"Could not find URL"**: The file may not be available or accessible
- **"Job failed"**: Check the error message for specific details about the failure

## Technical Details

### API Endpoints
- **Harmony Base URL**: https://harmony.earthdata.nasa.gov/
- **OGC API Coverages**: Used for subset requests
- **Job Status**: Polled for asynchronous processing

### Authentication
- Uses Earthdata Cloud authentication (JWT tokens)
- Requires valid ~/.netrc credentials

### File Formats
- **Input**: HDF5, NetCDF, GeoTIFF
- **Output**: NetCDF-4, GeoTIFF, HDF5

## Future Enhancements

Planned improvements include:
- Batch subsetting of multiple files
- Advanced spatial subsetting (polygons, points)
- Custom variable expressions
- Result preview and validation
- Integration with SNAP data processing workflows 
import requests
import json
import re
import os
import sys
from pathlib import Path
from datetime import datetime

sys.stdout.reconfigure(encoding='utf-8')

# Define the output directory inside the SeaDAS project structure
BASE_DIR = os.path.join(
    os.getcwd(),  # Current working directory
    "seadas-toolbox", "seadas-earthdata-cloud-toolbox",
    "src", "main", "resources", "json-files"
)

# Ensure the output directory exists
os.makedirs(BASE_DIR, exist_ok=True)

def get_user_home():
    """Retrieve the correct home directory across different OS environments."""
    detected_os = sys.platform
    print(f"Detected OS: {detected_os}")

    if detected_os.startswith("win"):  # Windows
        user_profile = os.getenv("USERPROFILE")
        if user_profile:
            return os.path.join(user_profile, "Documents")
        else:
            return "C:\\Users\\Default\\Documents"
    else:  # macOS/Linux
        return str(Path.home() / "Documents")

def fetch_ob_cloud_collections(start_date=None, end_date=None):
    base_url = "https://cmr.earthdata.nasa.gov/search/collections.json?provider=OB_CLOUD&page_size=2000"

    # Add temporal constraints if provided
    if start_date and end_date:
        temporal_filter = f"&temporal={start_date},{end_date}"
        url = base_url + temporal_filter
    else:
        url = base_url

    response = requests.get(url)
    if response.status_code != 200:
        print("Failed to retrieve data.")
        return []

    data = response.json()
    collections = data.get('feed', {}).get('entry', [])
    print(f"Fetched {len(collections)} collections.")  # Debugging output
    return collections

def categorize_short_names(collections):
    categorized_data = {}
    pattern = re.compile(r"^(.*?)_(L\d+[a-zA-Z]*)(?:_(.*))?$")

    print("\nDebugging Short Names:")

    for collection in collections:
        short_name = collection.get('short_name', '')
        print(f"Found short name: {short_name}")  # Debugging output

        match = pattern.match(short_name)
        if match:
            satellite_instr, data_level, product_name = match.groups()
            product_name = product_name if product_name else "General"  # Default if no product

            if satellite_instr not in categorized_data:
                categorized_data[satellite_instr] = {}

            if data_level not in categorized_data[satellite_instr]:
                categorized_data[satellite_instr][data_level] = []

            entry = {"product_name": product_name, "short_name": short_name}
            if entry not in categorized_data[satellite_instr][data_level]:
                categorized_data[satellite_instr][data_level].append(entry)

            print(f"Categorized: {short_name} -> {satellite_instr} -> {data_level} -> {product_name}")  # Debugging output
        else:
            print(f"Skipping {short_name} (No match)")  # Debugging output

    print(f"\nTotal categorized short names: {sum(len(v) for level in categorized_data.values() for v in level.values())}")
    return categorized_data

def save_to_json(categorized_data):
    user_home = get_user_home()
    if user_home is None:
        print("Running in a browser, saving JSON as downloadable files.")
        for satellite, data in categorized_data.items():
            json_data = json.dumps(data, indent=4)
            print(f"Download JSON for: {satellite}.json")  # Simulate download
        return

    output_dir = os.path.join(user_home, "json-files")
    os.makedirs(output_dir, exist_ok=True)

    print(f"Saving files in: {output_dir}")  # Confirm correct directory

    for satellite, data in categorized_data.items():
        filename = os.path.join(output_dir, f"{satellite}.json")
        with open(filename, "w") as f:
            json.dump(data, f, indent=4)
        print(f"Saved {filename}")

def save_metadata(metadata):
    """
    Saves extracted metadata as JSON files in the specified directory.
    """
    for satellite, levels in metadata.items():
        output_path = os.path.join(BASE_DIR, f"{satellite}.json")

        try:
            with open(output_path, "w", encoding="utf-8") as json_file:
                json.dump(levels, json_file, indent=4)
            print(f"✅ Saved: {output_path}")
        except Exception as e:
            print(f"Error saving {output_path}: {e}")

def main():
    start_date, end_date = "", ""

    # Ensure Python output uses UTF-8 (Windows fix)
    sys.stdout.reconfigure(encoding='utf-8')

    # Remove Unicode characters if necessary
    print("Running metadata extraction (Start Date: None, End Date: None)")

    if start_date and end_date:
        try:
            start_date = datetime.strptime(start_date, "%Y-%m-%d").strftime("%Y-%m-%dT00:00:00Z")
            end_date = datetime.strptime(end_date, "%Y-%m-%d").strftime("%Y-%m-%dT23:59:59Z")
        except ValueError:
            print("Invalid date format. Please use YYYY-MM-DD.")
            return
    else:
        start_date = end_date = None

    print("Fetching OB_CLOUD collections...")
    collections = fetch_ob_cloud_collections(start_date, end_date)
    if not collections:
        print("No collections found.")
        return

    print("Categorizing short names...")
    categorized_data = categorize_short_names(collections)

    print("Saving categorized data...")
    #save_to_json(categorized_data)
    save_metadata(categorized_data)
    print("Process completed!")

if __name__ == "__main__":
    main()
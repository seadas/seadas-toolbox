import requests
import json
import re
import os
import sys
from pathlib import Path

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

def fetch_ob_cloud_collections():
    url = "https://cmr.earthdata.nasa.gov/search/collections.json?provider=OB_CLOUD&page_size=2000"
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

def main():
    print("Fetching OB_CLOUD collections...")
    collections = fetch_ob_cloud_collections()
    if not collections:
        print("No collections found.")
        return

    print("Categorizing short names...")
    categorized_data = categorize_short_names(collections)

    print("Saving categorized data...")
    save_to_json(categorized_data)
    print("Process completed!")

if __name__ == "__main__":
    main()

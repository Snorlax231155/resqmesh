import osmnx as ox
import json
import uuid
import math

def extract_graph():
    print("Downloading Lower Manhattan road network (this may take a minute)...")
    # Define a bounding box for Lower Manhattan (approx south of 14th street)
    # bbox=(left, bottom, right, top) i.e. (west, south, east, north)
    west, south, east, north = -74.0200, 40.7000, -73.9700, 40.7350
    
    # Download the drivable network within this bounding box
    G = ox.graph_from_bbox(bbox=(west, south, east, north), network_type='drive')
    print(f"Downloaded graph with {len(G.nodes)} nodes and {len(G.edges)} edges.")
    
    nodes_list = []
    node_mapping = {} # old_id -> new UUID
    
    print("Processing nodes...")
    for n, data in G.nodes(data=True):
        new_id = str(uuid.uuid4())
        node_mapping[n] = new_id
        nodes_list.append({
            "id": new_id,
            "name": f"Node {n}",
            "latitude": round(data['y'], 6),
            "longitude": round(data['x'], 6)
        })
        
    edges_list = []
    print("Processing edges...")
    for u, v, key, data in G.edges(keys=True, data=True):
        source = node_mapping.get(u)
        target = node_mapping.get(v)
        if not source or not target:
            continue
            
        # Get length in meters, convert to km
        length_m = data.get('length', 100.0)
        distance_km = length_m / 1000.0
        
        # Get speed limit or default to 40 km/h (approx 25 mph, NYC speed limit)
        speed_kph = 40.0
        
        # Calculate travel time in minutes
        time_hours = distance_km / speed_kph
        time_minutes = max(1, int(math.ceil(time_hours * 60)))
        
        edges_list.append({
            "id": str(uuid.uuid4()),
            "sourceNodeId": source,
            "destinationNodeId": target,
            "travelTimeMinutes": time_minutes,
            "distanceKm": round(distance_km, 3),
            "blocked": False,
            "bidirectional": not data.get('oneway', False)
        })
        
    output = {
        "nodes": nodes_list,
        "edges": edges_list
    }
    
    out_path = "src/main/resources/manhattan_graph.json"
    with open(out_path, "w") as f:
        json.dump(output, f)
        
    print(f"Successfully saved to {out_path}")

if __name__ == "__main__":
    extract_graph()

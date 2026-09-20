import json
import random

with open('src/main/resources/manhattan_graph.json', 'r') as f:
    graph = json.load(f)

nodes = [n['id'] for n in graph['nodes']]
edges = [e['id'] for e in graph['edges']]

scenario = {
    "name": "Urban Flood",
    "graphSource": "manhattan_graph.json",
    "agents": [],
    "events": []
}

for i in range(1, 7):
    agent_code = f"UNIT-0{i}"
    scenario['agents'].append({
        "agentCode": agent_code,
        "name": f"Rescue Unit {i}",
        "currentNodeId": random.choice(nodes),
        "capacity": 1
    })
    scenario['events'].append({
        "tOffsetSeconds": 5,
        "type": "AGENT_ONLINE",
        "payload": {
            "agentCode": agent_code
        }
    })

priorities = ["CRITICAL", "HIGH", "MEDIUM", "LOW"]
for i in range(1, 41):
    scenario['events'].append({
        "tOffsetSeconds": 10 + i * 15,
        "type": "MISSION_CREATED",
        "payload": {
            "missionCode": f"MED-{i:03d}",
            "pickupNodeId": random.choice(nodes),
            "destinationNodeId": random.choice(nodes),
            "priority": random.choice(priorities)
        }
    })

for i in range(1, 15):
    scenario['events'].append({
        "tOffsetSeconds": 30 + i * 30,
        "type": "EDGE_CLOSED",
        "payload": {
            "type": "ROAD_CLOSURE",
            "affectedRoadId": random.choice(edges),
            "description": "Flooded street"
        }
    })

with open('src/main/resources/scenarios/urban_flood.json', 'w') as f:
    json.dump(scenario, f, indent=2)

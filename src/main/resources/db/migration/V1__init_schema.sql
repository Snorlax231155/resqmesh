CREATE TABLE road_nodes (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255),
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7)
);

CREATE TABLE road_edges (
    id VARCHAR(64) PRIMARY KEY,
    source_node_id VARCHAR(64) NOT NULL REFERENCES road_nodes(id),
    destination_node_id VARCHAR(64) NOT NULL REFERENCES road_nodes(id),
    travel_time_minutes INTEGER NOT NULL,
    distance_km NUMERIC(8, 3),
    blocked BOOLEAN NOT NULL DEFAULT FALSE,
    bidirectional BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE agents (
    id UUID PRIMARY KEY,
    agent_code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    current_node_id VARCHAR(64) NOT NULL REFERENCES road_nodes(id),
    status VARCHAR(32) NOT NULL,
    capacity INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE missions (
    id UUID PRIMARY KEY,
    mission_code VARCHAR(64) NOT NULL UNIQUE,
    pickup_node_id VARCHAR(64) NOT NULL REFERENCES road_nodes(id),
    destination_node_id VARCHAR(64) NOT NULL REFERENCES road_nodes(id),
    priority VARCHAR(32) NOT NULL,
    deadline TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL,
    assigned_agent_id UUID REFERENCES agents(id),
    risk_status VARCHAR(32) NOT NULL DEFAULT 'ON_TRACK',
    estimated_arrival_minutes INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE disruptions (
    id UUID PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    affected_road_id VARCHAR(64) REFERENCES road_edges(id),
    affected_agent_id UUID REFERENCES agents(id),
    description VARCHAR(512) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE assignment_histories (
    id UUID PRIMARY KEY,
    mission_id UUID NOT NULL REFERENCES missions(id),
    previous_agent_id UUID REFERENCES agents(id),
    new_agent_id UUID REFERENCES agents(id),
    reason VARCHAR(512) NOT NULL,
    disruption_id UUID REFERENCES disruptions(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_missions_status ON missions(status);
CREATE INDEX idx_missions_priority ON missions(priority);
CREATE INDEX idx_agents_status ON agents(status);
CREATE INDEX idx_disruptions_active ON disruptions(active);
CREATE INDEX idx_road_edges_source_dest ON road_edges(source_node_id, destination_node_id);

DROP TABLE IF EXISTS children;
DROP TABLE IF EXISTS nodes;

-- Table Definition ----------------------------------------------

CREATE TABLE IF NOT EXISTS nodes (
    id varchar(5) PRIMARY KEY,
    parent varchar(5) REFERENCES nodes(id),
    root varchar(5) REFERENCES nodes(id)
);

-- Indices -------------------------------------------------------

CREATE UNIQUE INDEX IF NOT EXISTS nodes_pkey ON nodes(id varchar_pattern_ops);

-- Table Definition ----------------------------------------------

CREATE TABLE IF NOT EXISTS children (
    ancestor varchar(5) REFERENCES nodes(id),
    descendant varchar(5) REFERENCES nodes(id),
    depth integer,
    parent varchar(5) REFERENCES nodes(id),
    root varchar(5) REFERENCES nodes(id)
);

-- Indices -------------------------------------------------------

CREATE UNIQUE INDEX IF NOT EXISTS cp_unique_idx ON children(ancestor varchar_pattern_ops,descendant varchar_pattern_ops);
CREATE INDEX IF NOT EXISTS pdc_idx ON children(ancestor varchar_pattern_ops,depth int4_ops,descendant varchar_pattern_ops);
CREATE INDEX IF NOT EXISTS cpd_idx ON children(descendant varchar_pattern_ops,ancestor varchar_pattern_ops,depth int4_ops);
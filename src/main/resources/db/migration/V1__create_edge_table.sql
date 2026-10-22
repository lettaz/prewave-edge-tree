CREATE TABLE edge (
    from_id INTEGER NOT NULL,
    to_id INTEGER NOT NULL,

    CONSTRAINT edge_pkey PRIMARY KEY (from_id, to_id)
);

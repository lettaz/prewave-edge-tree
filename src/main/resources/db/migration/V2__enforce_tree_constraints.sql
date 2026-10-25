-- Adding some design constraints that make creating a tree reliable, although wasnt in the brief

ALTER TABLE edge
    ADD CONSTRAINT edge_to_id_unique UNIQUE (to_id);

ALTER TABLE edge
    ADD CONSTRAINT edge_no_self_link CHECK (from_id <> to_id);

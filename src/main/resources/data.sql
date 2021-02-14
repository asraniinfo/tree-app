DELETE
FROM children CASCADE;
DELETE
FROM nodes CASCADE;

INSERT INTO "public"."nodes"("id", "parent", "root")
VALUES ('root', NULL, 'root'),
       ('a', 'root', 'root'),
       ('b', 'root', 'root'),
       ('c', 'a', 'root'),
       ('d', 'a', 'root'),
       ('e', 'c', 'root'),
       ('f', 'c', 'root'),
       ('g', 'c', 'root') ON CONFLICT DO NOTHING;


INSERT INTO "public"."children"("ancestor", "descendant", "depth", "parent", "root")
VALUES ('root', 'root', 0, NULL, 'root'),
       ('root', 'a', 1, 'root', 'root'),
       ('root', 'b', 1, 'root', 'root'),
       ('root', 'c', 2, 'a', 'root'),
       ('root', 'd', 2, 'a', 'root'),
       ('root', 'e', 3, 'c', 'root'),
       ('root', 'f', 3, 'c', 'root'),
       ('root', 'g', 3, 'c', 'root'),
       ('a', 'a', 0, 'root', 'root'),
       ('a', 'c', 1, 'a', 'root'),
       ('a', 'd', 1, 'a', 'root'),
       ('a', 'e', 2, 'c', 'root'),
       ('a', 'f', 2, 'c', 'root'),
       ('a', 'g', 2, 'c', 'root'),
       ('b', 'b', 0, 'root', 'root'),
       ('c', 'c', 0, 'a', 'root'),
       ('c', 'e', 1, 'c', 'root'),
       ('c', 'f', 1, 'c', 'root'),
       ('c', 'g', 1, 'c', 'root'),
       ('d', 'd', 0, 'a', 'root'),
       ('e', 'e', 0, 'c', 'root'),
       ('f', 'f', 0, 'c', 'root'),
       ('g', 'g', 0, 'c', 'root') ON CONFLICT DO NOTHING;
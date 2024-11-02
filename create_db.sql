set client_encoding to 'UTF8';

create schema if not exists bis;

create extension if not exists postgis;

CREATE TABLE bis.test (
	id uuid NOT NULL,
	geometry public.geometry(geometry, 4326) NULL,
	CONSTRAINT test_pkey PRIMARY KEY (id)
);

insert into bis.test
(id, geometry)
values
('0192eca6-d729-7582-d41d-3c0d38b44f24', ST_GeomFromText('POLYGON ((39.661648 47.215334, 39.661943 47.215328, 39.66194 47.215244, 39.661649 47.215248, 39.661648 47.215334))', 4326)),
('0192eca7-5bf7-778d-d41d-29318724447a', ST_GeomFromText('POLYGON ((39.661648 47.215334, 39.661943 47.215328, 39.66194 47.215244, 39.661649 47.215248, 39.661648 47.215334))', 4326))
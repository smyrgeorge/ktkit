create table if not exists test
(
    id         serial    not null primary key,
    created_at timestamp not null default now(),
    created_by uuid      not null,
    updated_at timestamp not null default now(),
    updated_by uuid      not null,
    test       text      not null
);
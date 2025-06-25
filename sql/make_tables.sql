-- create tables

create table users (
    user_id serial primary key,
    user_name varchar(255) unique not null,
    password_hash varchar(255) not null,
    created_at timestamp default current_timestamp not null
);

create table group_data (
    group_id serial primary key,
    group_name varchar(255) not null,
    join_key varchar(255) unique not null,
    created_by_user_id bigint unsigned not null references users(user_id) on delete set null,
    created_at timestamp default current_timestamp not null
);

create table group_membership (
    membership_id serial primary key,
    group_id bigint unsigned not null references group_data(group_id) on delete cascade,
    user_id bigint unsigned not null references users(user_id) on delete cascade,
    user_role varchar(50),
    joined_at timestamp default current_timestamp not null
);

create table file_data (
    actual_data_id serial primary key,
    content blob,
    checksum varchar(64),
    created_at timestamp default current_timestamp not null
);

create table pages (
    page_id serial primary key,
    group_id bigint unsigned not null references group_data(group_id) on delete cascade,
    page_name varchar(255) not null,
    created_at timestamp default current_timestamp not null,
    latest_revision_id bigint unsigned -- ALTER TABLE에서 FK 추가 예정
);

create table comments (
	comment_id serial primary key,
    page_id bigint unsigned not null references pages(page_id) on delete cascade,
    commented_by_user_id bigint unsigned not null references users(user_id) on delete set null,
    commented_by_user_name varchar(255) not null references users(user_name) on delete set null,
    comment_data text,
    created_at timestamp default current_timestamp not null
);

create table file_revisions (
    revision_id serial primary key,
    page_id bigint unsigned not null, -- ALTER TABLE에서 FK 추가 예정
    actual_data_id bigint unsigned not null references file_data(actual_data_id) on delete cascade,
    committed_by_user_id bigint unsigned not null references users(user_id) on delete set null,
    parent_revision_id bigint unsigned, -- ALTER TABLE에서 FK 추가 예정
    commit_message text,
    created_at timestamp default current_timestamp not null
);

-- alter tables

-- pages 테이블에 latest_revision_id 외래 키 추가
alter table pages
add constraint fk_pages_latest_revision
foreign key (latest_revision_id) references file_revisions (revision_id) on delete set null;

-- file_revisions 테이블에 page_id 외래 키 추가
alter table file_revisions
add constraint fk_revisions_page
foreign key (page_id) references pages (page_id) on delete cascade;

-- file_revisions 테이블에 parent_revision_id (자체 참조) 외래 키 추가
alter table file_revisions
add constraint fk_revisions_parent_revision
foreign key (parent_revision_id) references file_revisions (revision_id) on delete set null;
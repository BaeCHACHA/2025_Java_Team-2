set foreign_key_checks = 0;

-- 역순으로 DROP (순서가 중요하지 않지만 이렇게 하는 것이 일반적)
DROP TABLE IF EXISTS file_revisions;
DROP TABLE IF EXISTS pages;
DROP TABLE IF EXISTS group_membership;
DROP TABLE IF EXISTS file_data;
DROP TABLE IF EXISTS group_data;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS comments;

set foreign_key_checks = 1;
-- Role 초기 데이터 삽입 (중복 방지)
INSERT IGNORE INTO role (role_type) VALUES ('USER');
INSERT IGNORE INTO role (role_type) VALUES ('SELLER');
INSERT IGNORE INTO role (role_type) VALUES ('ADMIN');
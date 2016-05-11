CREATE DATABASE TurtleTV;

CREATE TABLE tb_user(id INTEGER PRIMARY KEY AUTO_INCREMENT, 
					name TEXT, password TEXT, 
					canPublish BOOLEAN, 
					portrait BLOB, 
					likes TEXT);
CREATE TABLE tb_category(id INTEGER PRIMARY KEY AUTO_INCREMENT, 
					name TEXT, 
					coverPath TEXT);
CREATE TABLE tb_room(id INTEGER PRIMARY KEY AUTO_INCREMENT, 
					title TEXT, 
					categoryId INTEGER, 
					publisherId INTEGER, 
					isLiving BOOLEAN);

INSERT INTO tb_user VALUES(NULL, 'aaa', 'aaa', 1, NULL, NULL);
INSERT INTO tb_user VALUES(NULL, 'bbb', 'bbb', 1, NULL, NULL);
INSERT INTO tb_user VALUES(NULL, 'ccc', 'ccc', 1, NULL, NULL);
INSERT INTO tb_user VALUES(NULL, 'ddd', 'ddd', 0, NULL, NULL);

INSERT INTO tb_category VALUES(NULL, 'Other', NULL);
INSERT INTO tb_category VALUES(NULL, 'LOL', NULL);
INSERT INTO tb_category VALUES(NULL, 'StarCrate II', NULL);

INSERT INTO tb_room VALUES(NULL, '五五开开车车上上UU', 2, 1, 0);
INSERT INTO tb_room VALUES(NULL, '干死黄旭东', 3, 2, 0);
INSERT INTO tb_room VALUES(NULL, '房间1', 1, 3, 0);
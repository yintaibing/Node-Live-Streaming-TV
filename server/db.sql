CREATE DATABASE TurtleTV;

CREATE TABLE tb_user(id INTEGER PRIMARY KEY AUTO_INCREMENT, 
					name TEXT, password TEXT, 
					canPublish BOOLEAN, 
					portrait BLOB, 
					likes TEXT);
CREATE TABLE tb_room(id INTEGER PRIMARY KEY AUTO_INCREMENT, 
					title TEXT, 
					categoryId INTEGER, 
					publisherId INTEGER, 
					isLiving BOOLEAN);
CREATE TABLE tb_category(id INTEGER PRIMARY KEY AUTO_INCREMENT, 
					name TEXT, 
					coverPath TEXT);
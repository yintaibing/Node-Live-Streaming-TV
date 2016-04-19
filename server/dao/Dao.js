/*
 * @File:   Dao.js
 * @Author: yintaibing, SCUT Software Engineering, Grade 2012, Class 6.
 * @Date:   2016/04/16
 */

// why don't I use some popular ORM modules? because I just want it to be as simple as possible.

var fs = require('fs');
var mysql = require('mysql');
var User = require('../bean/User.js');
var Room = require('../bean/Room.js');
var Category = require('../bean/Category.js');

function Dao() {
	this.config = null;
	this.conn = null;
}

Dao.prototype.init = function() {
	var self = this;
	var reader = fs.createReadStream('../mysql.json');
	var json = '';
	reader.on('data', function(chunk) {
		json += chunk;
	});
	reader.on('end', function() {
		console.log('mysql config loaded');
		self.config = JSON.parse(json);
		self.connect();
	});
	reader.on('error', function(err) {
		console.error(err);
	});
};

Dao.prototype.connect = function() {
	this.conn = mysql.createConnection(this.config);
	this.conn.connect(function(err) {
		if (err) {
			console.error(err);
			return;
		}

		console.log('mysql connected');
	});

	// sometimes mysql would auto-disconnect after 8h, so we need auto-reconnect.
	var self = this;
	this.conn.on('error', function(err) {
		if (err) {
			console.log('mysql error:\n' + err);

			if (err.code === 'PROTOCOL_CONNECTION_LOST') {
				console.log('reconnecting mysql...');
				self.connect();
			} else {
				console.error(err);
			}
		}
	});
};

Dao.prototype.create = function(service, obj) {
	var sql = null;

	if (obj instanceof User) {
		sql = mysql.format('INSERT INTO ?? (??, ??, ??, ??, ??) VALUES (?, ?, ?, ?, ?)', 
			['tb_user', 'name', 'password', 'canPublish', 'portrait', 'likes',
			obj.getName(), obj.getPassword(), obj.getCanPublish(), obj.getPortrait(), 
			JSON.stringify(obj.geLikes())]);
	} else if (obj instanceof Room) {
		sql = mysql.format('INSERT INTO ?? (??, ??, ??, ??) VALUES (?, ?, ?, ?)',
			['tb_room', 'title', 'publisherId', 'categoryId', 'isLiving',
			obj.getTitle(), obj.getPublisherId(), obj.getCategoryId(), 
			obj.getIsLiving()]);
	} else if (obj instanceof Category) {
		sql = mysql.format('INSERT INTO ?? (??, ??) VALUES (?, ?)',
			['tb_category', 'name', 'coverPath', obj.getName(), obj.getCoverPath()]);
	} else {
		throw 'the obj is not a BaseBean!';
	}

	this.doTransaction(service, sql);
};

Dao.prototype.update = function(service, obj) {
	var sql = null;

	if (obj instanceof User) {
		sql = mysql.format('UPDATE ?? SET ?? = ?, ?? = ?, ?? = ?, ?? = ?, ?? = ?',
			['tb_user', 'name', obj.getName(), 'password', obj.getPassword(), 
			canPublish, obj.getCanPublish(), 'portrait', obj.getPortrait(), 
			'likes', JSON.stringify(obj.geLikes())]);
	} else if (obj instanceof Room) {
		sql = mysql.format('UPDATE ?? SET ?? = ?, ?? = ?, ?? = ?, ?? = ?',
			['tb_room', 'title', obj.getTitle(), 'publisherId', obj.getPublisherId(), 
			'categoryId', obj.getCategoryId(), 'isLiving', obj.getIsLiving()]);
	} else if (obj instanceof Category) {
		sql = mysql.format('UPDATE ?? SET ?? = ?, ?? = ?',
			['tb_category', 'name', obj.getName(), 'coverPath', obj.getCoverPath()]);
	} else {
		throw 'the obj is not a BaseBean!';
	}

	this.doTransaction(service, sql);
};

Dao.prototype.read = function(service, obj) {
	if (!obj) {
		throw 'the query object is undefined';
	}

	var self = this;
	var sql = 'SELECT * FROM ';
	var conditions = 0;
	if (obj instanceof User) {
		sql += 'tb_user ';
		if (obj.getId()) {
			sql += 'WHERE id = ' + obj.getId() + ' ';
			conditions++;
		}
		if (obj.getName()) {
			sql += conditions ? 'AND ' : 'WHERE ';
			sql += 'name = \'' + obj.getName() + '\' ';
			conditions++;
		}
		if (obj.getPassword()) {
			sql += conditions ? 'AND ' : 'WHERE ';
			sql += 'password = \'' + obj.getPassword() + '\' ';
			conditions++;
		}
	} else if (obj instanceof Room) {
		sql += 'tb_room ';
		if (obj.getId()) {
			sql += 'WHERE id = ' + obj.getId() + ' ';
			conditions++;
		}
		if (obj.getTitle()) {
			sql += conditions ? 'AND ' : 'WHERE ';
			sql += 'title = \'' + obj.getTitle() + '\' ';
			conditions++;
		}
		if (obj.getPublisherId()) {
			sql += conditions ? 'AND ' : 'WHERE ';
			sql += 'publisherId = ' + obj.getPublisherId() + ' ';
			conditions++;
		}
		if (obj.getCategoryId()) {
			sql += conditions ? 'AND ' : 'WHERE ';
			sql += 'categoryId = ' + obj.getCategoryId() + ' ';
			conditions++;
		}
	} else if (obj instanceof Category) {
		sql += 'tb_category ';
		if (obj.getId()) {
			sql += 'WHERE id = ' + obj.getId() + ' ';
			conditions++;
		}
		if (obj.getName()) {
			sql += conditions ? 'AND ' : 'WHERE ';
			sql += 'name = \'' + obj.getName() + '\' ';
			conditions++;
		}
	} else {
		throw 'the obj is not a BaseBean';
	}

	this.conn.query(sql, function(err, rows, fields) {
		if (err) {
			console.error(err);
			return;
		}

		var ary = self.rowsToBean(rows, obj);
		// notify the service that the operation is ended
		self.emitServiceEvent(service, 'mysql_done', ary);
	});
};

Dao.prototype.remove = function(service, obj) {
	if (!obj) {
		throw 'the obj is undefined';
	}

	var self = this;
	var sql = 'DELETE FROM ';
	if (obj instanceof User) {
		sql += 'tb_user ';
	} else if (obj instanceof Room) {
		sql += 'tb_room ';
	} else if (obj instanceof Category) {
		sql += 'tb_category ';
	} else {
		throw 'the obj is not a BaseBean';
	}

	sql += 'WHERE id = ' + obj.getId();
	this.doTransaction(service, sql);
};

Dao.prototype.doTransaction = function(service, sql) {
	var self = this;
	this.conn.beginTransaction(function(err) {
		if (err) {
			throw err;
		}

		connection.query(sql, function(err, result) {
			if (err) {
				console.error('mysql rollback...');
				return connection.rollback(function() {
					throw err;
				});
			}

			connection.commit(function(err) {
				if (err) {
					return connection.rollback(function() {
						throw err;
					});
				}

				// notify the service that the operation is ended
				self.emitServiceEvent(service, 'mysql_done');
				console.log('mysql query ok');
			});
		});
	});
};

Dao.prototype.rowsToBean = function(rows, model) {
	if (!rows || rows.length === 0) {
		return null;
	}

	var ary = [];
	var item, obj;
	if (model instanceof User) {
		for (item in rows) {
			obj = new User(item.id, item.name, item.password);
			obj.setCanPublish(item.canPublish);
			obj.setPortrait(item.portrait);
			obj.setLikes(JSON.parse(item.likes));
			ary.push(obj);
		}
	} else if (model instanceof Room) {
		for (item in rows) {
			obj = new Room(item.id, item.title);
			obj.setPublisherId(item.publisherId);
			obj.setCategoryId(item.categoryId);
			obj.setIsLiving(item.isLiving);
			ary.push(obj);
		}
	} else if (model instanceof Category) {
		for (item in rows) {
			obj = new Category(item.id, item.name, item.coverPath);
			ary.push(obj);
		}
	} else {
		throw 'the model is not a BaseBean';
	}

	return ary;
};

// emit the service event
Dao.prototype.emitServiceEvent = function(service, event, result) {
	service.emit(event, result);
};
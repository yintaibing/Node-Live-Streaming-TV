/*
 * @File:   UserService.js
 * @Author: yintaibing, SCUT Software Engineering, Grade 2012, Class 6.
 * @Date:   2016/04/19
 */

var Util = require('util');
var BaseService = require('./BaseService.js');
var User = require('../bean/User.js');
var Constants = require('../myutil/Constants.js');

function UserService(dao) {
	BaseService.call(this, dao);

	this.events = {
		addUserOk: 'addUserOk',
		getUserOk: 'getUserOk',
		updateUserOk: 'updateUserOk'
	};
	this.userData = {};
}

Util.inherits(UserService, BaseService);

UserService.prototype.addUser = function() {
	var user = new User(null, this.userData.name, this.userData.password);
	this.dao.create(this, this.events.addUserOk, user);
};

UserService.prototype.getUser = function() {
	var user = new User(this.userData.id, this.userData.name, this.userData.password);
	user.setCanPublish(this.userData.canPublish);
	this.dao.read(this, this.events.getUserOk, user);
};

UserService.prototype.updateUser = function(user) {
	this.dao.update(this, this.events.updateUserOk, user);
};

UserService.prototype.register = function(socketHandler, userData, userManager) {
	var self = this;
	self.userData = {};
	self.userData.id = userData.id;
	self.userData.name = userData.name;
	self.userData.password = userData.password;

	// if the user name is unique or not
	self.setCallback(self.events.getUserOk, function(result) {
		if (!result) {
			// user name is unique, when new user has been added, then read it from database
			self.setCallback(self.events.addUserOk, function() {
				// reset self callback to read the new user from database after create
				self.setCallback(self.events.getUserOk, function(result) {
					var res = {};
					res[Constants.KEY_OP] = Constants.OP_REGISTR;
					if (!result) {
						res[Constants.KEY_STATUS] = Constants.STATUS_ERROR;
						res[Constants.KEY_MSG] = '注册成功，但登录失败';
					} else {
						// add user to UserManager
						userManager[result[0].getId()] = result[0];
						res[Constants.KEY_STATUS] = Constants.STATUS_OK;
						res[Constants.KEY_JSON] = result[0];
					}
					socketHandler.response(res);
				});
				self.getUser();

			});
			self.addUser();

		} else {
			// user name is not unique
			var res = {};
			res[Constants.KEY_OP] = Constants.OP_REGISTR;
			res[Constants.KEY_STATUS] = Constants.STATUS_ERROR;
			res[Constants.KEY_MSG] = '用户名已被使用';
			socketHandler.response(res);
		}
	});
	self.getUser();
};

UserService.prototype.login = function(socketHandler, userData, userManager) {
	var self = this;
	self.userData = {};
	self.userData.name = userData.name;
	self.userData.password = userData.password;

	self.setCallback(self.events.getUserOk, function(result) {
		var res = {};
		res[Constants.KEY_OP] = Constants.OP_LOGIN;

		if (result !== null && 
			result[0].getName() === self.userData.name && 
			result[0].getPassword() === self.userData.password) {
			res[Constants.KEY_STATUS] = Constants.STATUS_OK;
			res[Constants.KEY_JSON] = result[0];

			userManager[result[0].getId()] = result[0];
		} else {
			res[Constants.KEY_STATUS] = Constants.STATUS_ERROR;
			res[Constants.KEY_MSG] = '用户名或密码错误';
		}

		socketHandler.response(res);
	});
	self.getUser();
};

UserService.prototype.logout = function(socketHandler, userId, userManager) {
	delete userManager[userId];
};

UserService.prototype.applyPublish = function(socketHandler, json, userManager) {
	var self = this;
	self.userData = {};
	self.userData.id = json[Constants.KEY_USER_ID];

	self.setCallback(self.events.getUserOk, function(result) {
		if (!result) {
			// user do not exist
			res[Constants.KEY_OP] = Constants.OP_APPLY_PUHLISH;
			res[Constants.KEY_STATUS] = Constants.STATUS_ERROR;
			res[Constants.KEY_MSG] = '不存在此用户';

			socketHandler.response(res);
		} else {
			// user exist, update it
			result[0].setCanPublish(true);
			userManager[result[0].getId()] = result[0];
			self.setCallback(self.events.updateUserOk, function() {
				socketHandler.rs.applyPublish(socketHandler, json, userManager, socketHandler.rm);
			});
			self.updateUser(result[0]);
		}
	});
	self.getUser();
};

module.exports = UserService;
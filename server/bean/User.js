/*
 * @File:   user.js
 * @Author: yintaibing, SCUT Software Engineering, Grade 2012, Class 6.
 * @Date:   2016/04/15
 */

'use strict';

var BaseBean = require('./BaseBean.js');
var Util = require('../util/Util.js');

function User(id, name, password) {
	BaseBean.call(this, id);

	this.name = name;
	this.password = password;
	this.portrait = null;
	this.canPublish = false;
	this.likes = null;
}

Util.inherit(User, BaseBean);

User.prototype.getName = function() {
	return this.name;
};

User.prototype.setName = function(name) {
	this.name = name;
};

User.prototype.getPassword = function() {
	return this.password;
};

User.prototype.setPassword = function(password) {
	this.password = password;
};

User.prototype.getPortrait = function() {
	return this.portrait;
};

User.prototype.setPortrait = function(portrait) {
	this.portrait = portrait;
};

User.prototype.getCanPublish = function() {
	return this.canPublish;
};

User.prototype.setCanPublish = function(canPublish) {
	this.canPublish = canPublish;
};

User.prototype.getLikes = function() {
	return this.likes;
};

User.prototype.setLikes = function(likes) {
	this.likes = likes;
};

module.exports = User;
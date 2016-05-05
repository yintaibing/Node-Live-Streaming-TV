/*
 * @File:   Room.js
 * @Author: yintaibing, SCUT Software Engineering, Grade 2012, Class 6.
 * @Date:   2016/04/15
 */

var Util = require('util');
var BaseBean = require('./BaseBean.js');

function Room(id, title, publisherId) {
	BaseBean.call(this, id);

	this.title = title;
	this.publisherId = publisherId;
	this.categoryId = null;
	this.isLiving = false;
}

Util.inherits(Room, BaseBean);

Room.prototype.getTitle = function() {
	return this.title;
};

Room.prototype.setTitle = function(title) {
	this.title = title;
};

Room.prototype.getPublisherId = function() {
	return this.publisherId;
};

Room.prototype.setPublisherId = function(publisherId) {
	this.publisherId = publisherId;
};

Room.prototype.getCategoryId = function() {
	return this.categoryId;
};

Room.prototype.setCategoryId = function(categoryId) {
	this.categoryId = categoryId;
};

Room.prototype.getIsLiving = function() {
	return this.isLiving;
};

Room.prototype.setIsLiving = function(isLiving) {
	this.isLiving = isLiving;
};

module.exports = Room;
/*
 * @File:   room.js
 * @Author: yintaibing, SCUT Software Engineering, Grade 2012, Class 6.
 * @Date:   2016/04/15
 */

'use strict';

var BaseBean = require('./BaseBean.js');
var Util = require('../util/Util.js');

function Room(id, title) {
	BaseBean.call(this, id);

	this.title = title;
	this.isLiving = false;
	this.publisher = null;
	this.audiences = null;
}

Util.inherit(Room, BaseBean);

Room.prototype.getTitle = function() {
	return this.title;
};

Room.prototype.setTitle = function(title) {
	this.title = title;
};

Room.prototype.getIsLiving = function() {
	return this.title;
};

Room.prototype.setIsLiving = function(isLiving) {
	this.isLiving = isLiving;
};

Room.prototype.getPublisher = function() {
	return this.publisher;
};

Room.prototype.setPublisher = function(publisher) {
	this.publisher = publisher;
};

Room.prototype.getAudiences = function() {
	return this.audiences;
};

Room.prototype.setAudiences = function(audiences) {
	this.audiences = audiences;
};

module.exports = Room;
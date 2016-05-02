/*
 * @File:   BaseService.js
 * @Author: yintaibing, SCUT Software Engineering, Grade 2012, Class 6.
 * @Date:   2016/04/27
 */

var EventEmitter = require('events').EventEmitter;
var Util = require('util');
var Constants = require('../myutil/Constants.js');

function BaseService(dao) {
	this.dao = dao;
}

Util.inherits(BaseService, EventEmitter);

BaseService.prototype.setCallback = function(event, callback) {
	this.once(event, callback);
};

module.exports = BaseService;
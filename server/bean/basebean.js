/*
 * @File:   basebean.js
 * @Author: yintaibing, SCUT Software Engineering, Grade 2012, Class 6.
 * @Date:   2016/04/15
 */

'use strict';

function BaseBean(id) {
	this.id = id;
}

BaseBean.prototype.getId = function() {
	return this.name;
};

BaseBean.prototype.setId = function(id) {
	this.id = id;
};

module.exports = BaseBean;
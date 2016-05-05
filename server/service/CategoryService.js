/*
 * @File:   Category.js
 * @Author: yintaibing, SCUT Software Engineering, Grade 2012, Class 6.
 * @Date:   2016/04/15
 */

var Util = require('util');
var BaseService = require('./BaseService.js');
var Category = require('../bean/Category.js');
var Constants = require('../myutil/Constants.js');

function CategoryService(dao) {
	BaseService.call(this, dao);

	this.events = {
		getCategoryListOk: 'getCategoryListOk'
	};
	this.categoryData = {};
}

Util.inherits(CategoryService, BaseService);

CategoryService.prototype.getCategoryList = function() {
	var category = new Category(this.categoryData.id, this.categoryData.name, this.categoryData.coverPath);
	this.dao.read(this, this.events.getCategoryListOk, category);
};

CategoryService.prototype.queryCategoryList = function(socketHandler, categoryData) {
	this.categoryData = {};
	if (categoryData) {
		this.categoryData.id = categoryData.id;
		this.categoryData.name = categoryData.name;
		this.categoryData.coverPath = categoryData.coverPath;
	}

	this.setCallback(this.events.getCategoryListOk, function(result) {
		var res = {};
		res[Constants.KEY_OP] = Constants.OP_GET_CATEGORY_LIST;

		if (!result) {
			res[Constants.KEY_STATUS] = Constants.STATUS_ERROR;
			res[Constants.KEY_MSG] = '没有查询到相关分类';
		} else {
			res[Constants.KEY_STATUS] = Constants.STATUS_OK;
			res[Constants.KEY_JSON] = result;
		}

		socketHandler.response(res);
	});
	this.getCategoryList();
};

module.exports = CategoryService;
/*
 * @File:   util.js
 * @Author: yintaibing, SCUT Software Engineering, Grade 2012, Class 6.
 * @Date:   2016/04/15
 */

 'use strict';

 // inherit prototype
exports.inherit = function (subType, superType) {
 	var prototype = object(superType.prototype);
 	prototype.constructor = subType;
 	subType.prototype = prototype;
}
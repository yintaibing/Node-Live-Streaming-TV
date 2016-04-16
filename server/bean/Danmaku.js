/*
 * @File:   Danmaku.js
 * @Author: yintaibing, SCUT Software Engineering, Grade 2012, Class 6.
 * @Date:   2016/04/15
 */

'use strict';

// The Danmaku type is not necessary to be persisted in DB, so it doesn't need 'id' property;
function Danmaku(text) {
	this.text = text;
}

Danmaku.prototype.getText = function() {
	return this.text;
};

Danmaku.prototype.setText = function(text) {
	this.text = text;
}

module.exports = Danmaku;
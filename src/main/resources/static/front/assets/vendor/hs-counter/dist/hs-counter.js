(function webpackUniversalModuleDefinition(root, factory) {
	if(typeof exports === 'object' && typeof module === 'object')
		module.exports = factory();
	else if(typeof define === 'function' && define.amd)
		define([], factory);
	else if(typeof exports === 'object')
		exports["HSCounter"] = factory();
	else
		root["HSCounter"] = factory();
})(window, function() {
return /******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};
/******/
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/
/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId]) {
/******/ 			return installedModules[moduleId].exports;
/******/ 		}
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			i: moduleId,
/******/ 			l: false,
/******/ 			exports: {}
/******/ 		};
/******/
/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);
/******/
/******/ 		// Flag the module as loaded
/******/ 		module.l = true;
/******/
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/
/******/
/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;
/******/
/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;
/******/
/******/ 	// define getter function for harmony exports
/******/ 	__webpack_require__.d = function(exports, name, getter) {
/******/ 		if(!__webpack_require__.o(exports, name)) {
/******/ 			Object.defineProperty(exports, name, { enumerable: true, get: getter });
/******/ 		}
/******/ 	};
/******/
/******/ 	// define __esModule on exports
/******/ 	__webpack_require__.r = function(exports) {
/******/ 		if(typeof Symbol !== 'undefined' && Symbol.toStringTag) {
/******/ 			Object.defineProperty(exports, Symbol.toStringTag, { value: 'Module' });
/******/ 		}
/******/ 		Object.defineProperty(exports, '__esModule', { value: true });
/******/ 	};
/******/
/******/ 	// create a fake namespace object
/******/ 	// mode & 1: value is a module id, require it
/******/ 	// mode & 2: merge all properties of value into the ns
/******/ 	// mode & 4: return value when already ns object
/******/ 	// mode & 8|1: behave like require
/******/ 	__webpack_require__.t = function(value, mode) {
/******/ 		if(mode & 1) value = __webpack_require__(value);
/******/ 		if(mode & 8) return value;
/******/ 		if((mode & 4) && typeof value === 'object' && value && value.__esModule) return value;
/******/ 		var ns = Object.create(null);
/******/ 		__webpack_require__.r(ns);
/******/ 		Object.defineProperty(ns, 'default', { enumerable: true, value: value });
/******/ 		if(mode & 2 && typeof value != 'string') for(var key in value) __webpack_require__.d(ns, key, function(key) { return value[key]; }.bind(null, key));
/******/ 		return ns;
/******/ 	};
/******/
/******/ 	// getDefaultExport function for compatibility with non-harmony modules
/******/ 	__webpack_require__.n = function(module) {
/******/ 		var getter = module && module.__esModule ?
/******/ 			function getDefault() { return module['default']; } :
/******/ 			function getModuleExports() { return module; };
/******/ 		__webpack_require__.d(getter, 'a', getter);
/******/ 		return getter;
/******/ 	};
/******/
/******/ 	// Object.prototype.hasOwnProperty.call
/******/ 	__webpack_require__.o = function(object, property) { return Object.prototype.hasOwnProperty.call(object, property); };
/******/
/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";
/******/
/******/
/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(__webpack_require__.s = "./src/js/hs-counter.js");
/******/ })
/************************************************************************/
/******/ ({

/***/ "./src/js/hs-counter.js":
/*!******************************!*\
  !*** ./src/js/hs-counter.js ***!
  \******************************/
/*! exports provided: default */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
eval("__webpack_require__.r(__webpack_exports__);\n/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, \"default\", function() { return HSCounter; });\nfunction _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError(\"Cannot call a class as a function\"); } }\n\nfunction _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if (\"value\" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }\n\nfunction _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }\n\n/*\r\n* HSCounter Plugin\r\n* @version: 2.0.0 (Mon, 25 Nov 2019)\r\n* @requires: jQuery v3.0 or later, appear.js v1.0.3\r\n* @author: HtmlStream\r\n* @event-namespace: .HSCounter\r\n* @license: Htmlstream Libraries (https://htmlstream.com/)\r\n* Copyright 2019 Htmlstream\r\n*/\nvar HSCounter = /*#__PURE__*/function () {\n  function HSCounter(el, settings) {\n    _classCallCheck(this, HSCounter);\n\n    this.$el = typeof el === \"string\" ? document.querySelector(el) : el;\n    this.defaults = {\n      bounds: -100,\n      debounce: 10,\n      time: 2000,\n      fps: 60,\n      isCommaSeparated: false,\n      isReduceThousandsTo: false,\n      intervalId: null\n    };\n    this.dataSettings = this.$el.hasAttribute('data-hs-counter-options') ? JSON.parse(this.$el.getAttribute('data-hs-counter-options')) : {};\n    this.settings = Object.assign({}, this.defaults, this.dataSettings, settings);\n  }\n\n  _createClass(HSCounter, [{\n    key: \"init\",\n    value: function init() {\n      var _this = this;\n\n      var appearSettings = {\n        init: function init() {\n          var value = parseInt(_this.$el.textContent, 10);\n          _this.$el.innerHTML = '0';\n\n          _this.$el.setAttribute('data-value', value);\n        },\n        elements: function elements() {\n          return [_this.$el];\n        },\n        appear: function appear(innerEl) {\n          var $item = innerEl,\n              counter = 1,\n              endValue = $item.getAttribute('data-value'),\n              iterationValue = parseInt(endValue / (_this.settings.time / _this.settings.fps), 10);\n\n          if (iterationValue === 0) {\n            iterationValue = 1;\n          }\n\n          $item.data = {\n            intervalId: setInterval(function () {\n              if (_this.settings.isCommaSeparated) {\n                $item.innerHTML = _this._getCommaSeparatedValue(counter += iterationValue);\n              } else if (_this.settings.isReduceThousandsTo) {\n                $item.innerHTML = _this._getCommaReducedValue(counter += iterationValue, _this.settings.isReduceThousandsTo);\n              } else {\n                $item.innerHTML = counter += iterationValue;\n              }\n\n              if (counter > endValue) {\n                clearInterval($item.data.intervalId);\n\n                if (_this.settings.isCommaSeparated) {\n                  $item.innerHTML = _this._getCommaSeparatedValue(endValue);\n                } else if (_this.settings.isReduceThousandsTo) {\n                  $item.innerHTML = _this._getCommaReducedValue(endValue, _this.settings.isReduceThousandsTo);\n                } else {\n                  $item.innerHTML = endValue;\n                }\n              }\n            }, _this.settings.time / _this.settings.fps)\n          };\n        }\n      };\n      var options = Object.assign({}, this.settings, appearSettings);\n      appear(options);\n    }\n  }, {\n    key: \"_getCommaReducedValue\",\n    value: function _getCommaReducedValue(value, additionalText) {\n      return parseInt(value / 1000, 10) + additionalText;\n    }\n  }, {\n    key: \"_getCommaSeparatedValue\",\n    value: function _getCommaSeparatedValue(value) {\n      value = value.toString();\n\n      switch (value.length) {\n        case 4:\n          return \"\".concat(value.substr(0, 1), \",\").concat(value.substr(1));\n          break;\n\n        case 5:\n          return \"\".concat(value.substr(0, 2), \",\").concat(value.substr(2));\n          break;\n\n        case 6:\n          return \"\".concat(value.substr(0, 3), \",\").concat(value.substr(3));\n          break;\n\n        case 7:\n          value = \"\".concat(value.substr(0, 1), \",\").concat(value.substr(1));\n          return \"\".concat(value.substr(0, 5), \",\").concat(value.substr(5));\n          break;\n\n        case 8:\n          value = \"\".concat(value.substr(0, 2), \",\").concat(value.substr(2));\n          return \"\".concat(value.substr(0, 6), \",\").concat(value.substr(6));\n          break;\n\n        case 9:\n          value = \"\".concat(value.substr(0, 3), \",\").concat(value.substr(3));\n          return \"\".concat(value.substr(0, 7), \",\").concat(value.substr(7));\n          break;\n\n        case 10:\n          value = \"\".concat(value.substr(0, 1), \",\").concat(value.substr(1));\n          value = \"\".concat(value.substr(0, 5), \",\").concat(value.substr(5));\n          return \"\".concat(value.substr(0, 9), \",\").concat(value.substr(9));\n          break;\n\n        default:\n          return value;\n      }\n    }\n  }]);\n\n  return HSCounter;\n}();\n\n\n\n//# sourceURL=webpack://HSCounter/./src/js/hs-counter.js?");

/***/ })

/******/ })["default"];
});
(function webpackUniversalModuleDefinition(root, factory) {
	if(typeof exports === 'object' && typeof module === 'object')
		module.exports = factory();
	else if(typeof define === 'function' && define.amd)
		define([], factory);
	else if(typeof exports === 'object')
		exports["HSFullscreen"] = factory();
	else
		root["HSFullscreen"] = factory();
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
/******/ 	return __webpack_require__(__webpack_require__.s = "./src/js/hs-fullscreen.js");
/******/ })
/************************************************************************/
/******/ ({

/***/ "./src/js/hs-fullscreen.js":
/*!*********************************!*\
  !*** ./src/js/hs-fullscreen.js ***!
  \*********************************/
/*! exports provided: default */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
eval("__webpack_require__.r(__webpack_exports__);\n/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, \"default\", function() { return HSFullscreen; });\n/* harmony import */ var _methods_remove_class__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! ./methods/remove-class */ \"./src/js/methods/remove-class.js\");\n/* harmony import */ var _methods_toggle_class__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! ./methods/toggle-class */ \"./src/js/methods/toggle-class.js\");\nfunction _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError(\"Cannot call a class as a function\"); } }\n\nfunction _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if (\"value\" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }\n\nfunction _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }\n\n/*\n* HSFullscreen Plugin\n* @version: 2.0.0 (Sun, 1 Aug 2021)\n* @author: HtmlStream\n* @event-namespace: .HSFullscreen\n* @license: Htmlstream Libraries (https://htmlstream.com/)\n* Copyright 2021 Htmlstream\n*/\n\n\nvar dataAttributeName = 'data-hs-fullscreen-options';\nvar defaults = {\n  targetEl: null,\n  mainContainerSelector: 'body',\n  toggleClassName: '.hs-fullscreen',\n  preventScrollClassName: '.hs-fullscreen-on'\n};\n\nvar HSFullscreen = /*#__PURE__*/function () {\n  function HSFullscreen(el, options, id) {\n    _classCallCheck(this, HSFullscreen);\n\n    this.collection = [];\n    var that = this;\n    var elems;\n\n    if (el instanceof HTMLElement) {\n      elems = [el];\n    } else if (el instanceof Object) {\n      elems = el;\n    } else {\n      elems = document.querySelectorAll(el);\n    }\n\n    for (var i = 0; i < elems.length; i += 1) {\n      that.addToCollection(elems[i], options, id || elems[i].id);\n    }\n\n    if (!that.collection.length) {\n      return false;\n    } // initialization calls\n\n\n    that._init();\n\n    return this;\n  }\n\n  _createClass(HSFullscreen, [{\n    key: \"_init\",\n    value: function _init() {\n      var that = this;\n\n      var _loop = function _loop(i) {\n        var _$el = void 0;\n\n        var _options = void 0;\n\n        if (that.collection[i].hasOwnProperty('$initializedEl')) {\n          return \"continue\";\n        }\n\n        _$el = that.collection[i].$el;\n        _options = that.collection[i].options;\n        _options.targetEl = document.querySelector(_options.targetEl);\n\n        _$el.addEventListener('click', function () {\n          Object(_methods_toggle_class__WEBPACK_IMPORTED_MODULE_1__[\"default\"])(_options.targetEl, _options);\n        });\n\n        document.addEventListener('keydown', function (e) {\n          if (!_options.targetEl.classList.contains(_options.toggleClassName.slice(1))) return;\n\n          if (e.keyCode === 27) {\n            Object(_methods_remove_class__WEBPACK_IMPORTED_MODULE_0__[\"default\"])(_options.targetEl, _options);\n          }\n        });\n        that.collection[i].$initializedEl = _options;\n      };\n\n      for (var i = 0; i < that.collection.length; i += 1) {\n        var _ret = _loop(i);\n\n        if (_ret === \"continue\") continue;\n      }\n    }\n  }, {\n    key: \"addToCollection\",\n    value: function addToCollection(item, options, id) {\n      this.collection.push({\n        $el: item,\n        id: id || null,\n        options: Object.assign({}, defaults, item.hasAttribute(dataAttributeName) ? JSON.parse(item.getAttribute(dataAttributeName)) : {}, options)\n      });\n    }\n  }, {\n    key: \"getItems\",\n    value: function getItems() {\n      var that = this;\n      var newCollection = [];\n\n      for (var i = 0; i < that.collection.length; i += 1) {\n        newCollection.push(that.collection[i].$initializedEl);\n      }\n\n      return newCollection;\n    }\n  }, {\n    key: \"getItem\",\n    value: function getItem(ind) {\n      return this.collection[ind].$initializedEl;\n    }\n  }]);\n\n  return HSFullscreen;\n}();\n\n\n\n//# sourceURL=webpack://HSFullscreen/./src/js/hs-fullscreen.js?");

/***/ }),

/***/ "./src/js/methods/add-class.js":
/*!*************************************!*\
  !*** ./src/js/methods/add-class.js ***!
  \*************************************/
/*! exports provided: default */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
eval("__webpack_require__.r(__webpack_exports__);\n/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, \"default\", function() { return fullscreenAddClass; });\nfunction fullscreenAddClass(el, settings) {\n  el.classList.add(settings.toggleClassName.slice(1));\n  document.querySelector(settings.mainContainerSelector).classList.add(settings.preventScrollClassName.slice(1));\n}\n\n//# sourceURL=webpack://HSFullscreen/./src/js/methods/add-class.js?");

/***/ }),

/***/ "./src/js/methods/remove-class.js":
/*!****************************************!*\
  !*** ./src/js/methods/remove-class.js ***!
  \****************************************/
/*! exports provided: default */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
eval("__webpack_require__.r(__webpack_exports__);\n/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, \"default\", function() { return fullscreenRemoveClass; });\nfunction fullscreenRemoveClass(el, settings) {\n  el.classList.remove(settings.toggleClassName.slice(1));\n  document.querySelector(settings.mainContainerSelector).classList.remove(settings.preventScrollClassName.slice(1));\n}\n\n//# sourceURL=webpack://HSFullscreen/./src/js/methods/remove-class.js?");

/***/ }),

/***/ "./src/js/methods/toggle-class.js":
/*!****************************************!*\
  !*** ./src/js/methods/toggle-class.js ***!
  \****************************************/
/*! exports provided: default */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
eval("__webpack_require__.r(__webpack_exports__);\n/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, \"default\", function() { return fullscreenToggleClass; });\n/* harmony import */ var _add_class__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! ./add-class */ \"./src/js/methods/add-class.js\");\n/* harmony import */ var _remove_class__WEBPACK_IMPORTED_MODULE_1__ = __webpack_require__(/*! ./remove-class */ \"./src/js/methods/remove-class.js\");\n\n\nfunction fullscreenToggleClass(el, settings) {\n  if (!el.classList.contains(settings.toggleClassName.slice(1))) {\n    Object(_add_class__WEBPACK_IMPORTED_MODULE_0__[\"default\"])(el, settings);\n  } else {\n    Object(_remove_class__WEBPACK_IMPORTED_MODULE_1__[\"default\"])(el, settings);\n  }\n}\n\n//# sourceURL=webpack://HSFullscreen/./src/js/methods/toggle-class.js?");

/***/ })

/******/ })["default"];
});
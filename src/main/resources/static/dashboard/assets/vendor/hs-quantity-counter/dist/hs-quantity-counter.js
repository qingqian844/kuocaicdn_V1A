(function webpackUniversalModuleDefinition(root, factory) {
	if(typeof exports === 'object' && typeof module === 'object')
		module.exports = factory();
	else if(typeof define === 'function' && define.amd)
		define([], factory);
	else if(typeof exports === 'object')
		exports["HSQuantityCounter"] = factory();
	else
		root["HSQuantityCounter"] = factory();
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
/******/ 	return __webpack_require__(__webpack_require__.s = "./src/js/hs-quantity-counter.js");
/******/ })
/************************************************************************/
/******/ ({

/***/ "./src/js/hs-quantity-counter.js":
/*!***************************************!*\
  !*** ./src/js/hs-quantity-counter.js ***!
  \***************************************/
/*! exports provided: default */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
eval("__webpack_require__.r(__webpack_exports__);\n/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, \"default\", function() { return HSQuantityCounter; });\nfunction _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError(\"Cannot call a class as a function\"); } }\n\nfunction _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if (\"value\" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }\n\nfunction _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }\n\nvar dataAttributeName = 'data-hs-quantity-counter-options';\nvar defaults = {\n  classMap: {\n    plus: '.js-plus',\n    minus: '.js-minus',\n    result: '.js-result'\n  },\n  resultVal: null\n};\n\nvar HSQuantityCounter = /*#__PURE__*/function () {\n  function HSQuantityCounter(el, options, id) {\n    _classCallCheck(this, HSQuantityCounter);\n\n    this.collection = [];\n    var that = this;\n    var elems;\n\n    if (el instanceof HTMLElement) {\n      elems = [el];\n    } else if (el instanceof Object) {\n      elems = el;\n    } else {\n      elems = document.querySelectorAll(el);\n    }\n\n    for (var i = 0; i < elems.length; i += 1) {\n      that.addToCollection(elems[i], options, id || elems[i].id);\n    }\n\n    if (!that.collection.length) {\n      return false;\n    } // initialization calls\n\n\n    that._init();\n\n    return this;\n  }\n\n  _createClass(HSQuantityCounter, [{\n    key: \"_init\",\n    value: function _init() {\n      var that = this;\n\n      var _loop = function _loop(i) {\n        var _$el = void 0;\n\n        var _options = void 0;\n\n        if (that.collection[i].hasOwnProperty('$initializedEl')) {\n          return \"continue\";\n        }\n\n        _$el = that.collection[i].$el;\n        _options = that.collection[i].options; // Change Default Values\n\n        _options.resultVal = parseInt(_$el.querySelector(_options.classMap.result).value); // Plus Click Events\n\n        _$el.querySelector(_options.classMap.plus).addEventListener('click', function () {\n          that._plusClickEvents(_$el, _options);\n        }); // Minus Click Events\n\n\n        _$el.querySelector(_options.classMap.minus).addEventListener('click', function () {\n          that._minusClickEvents(_$el, _options);\n        });\n      };\n\n      for (var i = 0; i < that.collection.length; i += 1) {\n        var _ret = _loop(i);\n\n        if (_ret === \"continue\") continue;\n      }\n    }\n  }, {\n    key: \"_plusClickEvents\",\n    value: function _plusClickEvents($el, settings) {\n      settings.resultVal += 1;\n      $el.querySelector(settings.classMap.result).value++;\n    }\n  }, {\n    key: \"_minusClickEvents\",\n    value: function _minusClickEvents($el, settings) {\n      if (settings.resultVal >= 1) {\n        settings.resultVal -= 1;\n        $el.querySelector(settings.classMap.result).value--;\n      } else {\n        return false;\n      }\n    }\n  }, {\n    key: \"addToCollection\",\n    value: function addToCollection(item, options, id) {\n      this.collection.push({\n        $el: item,\n        id: id || null,\n        options: Object.assign({}, defaults, item.hasAttribute(dataAttributeName) ? JSON.parse(item.getAttribute(dataAttributeName)) : {}, options)\n      });\n    }\n  }, {\n    key: \"getItem\",\n    value: function getItem(item) {\n      if (typeof item === 'number') {\n        return this.collection[item].$initializedEl;\n      } else {\n        return this.collection.find(function (el) {\n          return el.id === item;\n        }).$initializedEl;\n      }\n    }\n  }]);\n\n  return HSQuantityCounter;\n}();\n\n\n\n//# sourceURL=webpack://HSQuantityCounter/./src/js/hs-quantity-counter.js?");

/***/ })

/******/ })["default"];
});
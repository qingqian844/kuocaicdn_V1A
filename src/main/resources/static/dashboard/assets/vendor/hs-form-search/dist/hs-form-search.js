(function webpackUniversalModuleDefinition(root, factory) {
	if(typeof exports === 'object' && typeof module === 'object')
		module.exports = factory();
	else if(typeof define === 'function' && define.amd)
		define([], factory);
	else if(typeof exports === 'object')
		exports["HSFormSearch"] = factory();
	else
		root["HSFormSearch"] = factory();
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
/******/ 	return __webpack_require__(__webpack_require__.s = "./src/js/hs-form-search.js");
/******/ })
/************************************************************************/
/******/ ({

/***/ "./src/js/hs-form-search.js":
/*!**********************************!*\
  !*** ./src/js/hs-form-search.js ***!
  \**********************************/
/*! exports provided: default */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
eval("__webpack_require__.r(__webpack_exports__);\n/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, \"default\", function() { return HSFormSearch; });\n/* harmony import */ var _utils__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! ./utils */ \"./src/js/utils/index.js\");\nfunction ownKeys(object, enumerableOnly) { var keys = Object.keys(object); if (Object.getOwnPropertySymbols) { var symbols = Object.getOwnPropertySymbols(object); if (enumerableOnly) symbols = symbols.filter(function (sym) { return Object.getOwnPropertyDescriptor(object, sym).enumerable; }); keys.push.apply(keys, symbols); } return keys; }\n\nfunction _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? arguments[i] : {}; if (i % 2) { ownKeys(Object(source), true).forEach(function (key) { _defineProperty(target, key, source[key]); }); } else if (Object.getOwnPropertyDescriptors) { Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)); } else { ownKeys(Object(source)).forEach(function (key) { Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key)); }); } } return target; }\n\nfunction _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }\n\nfunction _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError(\"Cannot call a class as a function\"); } }\n\nfunction _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if (\"value\" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }\n\nfunction _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }\n\n\nvar dataAttributeName = 'data-hs-form-search-options';\nvar defaults = {\n  clearIcon: null,\n  defaultIcon: null,\n  delay: 300,\n  isLoading: false,\n  dropMenuOffset: 0,\n  dropMenuDuration: 300,\n  toggleIconOnFocus: false,\n  activeClass: null,\n  handlers: {}\n};\n\nvar HSFormSearch = /*#__PURE__*/function () {\n  function HSFormSearch(el, options, id) {\n    _classCallCheck(this, HSFormSearch);\n\n    this.collection = [];\n    var that = this;\n    var elems;\n\n    if (el instanceof HTMLElement) {\n      elems = [el];\n    } else if (el instanceof Object) {\n      elems = el;\n    } else {\n      elems = document.querySelectorAll(el);\n    }\n\n    for (var i = 0; i < elems.length; i += 1) {\n      that.addToCollection(elems[i], options, id || elems[i].id);\n    }\n\n    if (!that.collection.length) {\n      return false;\n    } // initialization calls\n\n\n    that._init();\n\n    return this;\n  }\n\n  _createClass(HSFormSearch, [{\n    key: \"_init\",\n    value: function _init() {\n      var _this = this;\n\n      var that = this;\n\n      var _loop = function _loop(i) {\n        var _$el = void 0;\n\n        var _options = void 0;\n\n        if (that.collection[i].hasOwnProperty('$initializedEl')) {\n          return \"continue\";\n        }\n\n        _$el = that.collection[i].$el;\n        _options = that.collection[i].options;\n        _options.$loadingIcon = document.querySelector(_options.loadingIcon);\n        _options.$clearIcon = document.querySelector(_options.clearIcon);\n        _options.$defaultIcon = document.querySelector(_options.defaultIcon);\n        _options.$dropMenuElement = document.querySelector(_options.dropMenuElement);\n\n        _this.toggleIcon(_$el.value.length > 0, _options, _$el);\n\n        _options.$clearIcon.addEventListener('click', function () {\n          _$el.value = '';\n          that.toggleIcon(false, _options, _$el);\n\n          if (Object.prototype.hasOwnProperty.call(that.collection[i].$initializedEl.events, 'clear')) {\n            that.collection[i].$initializedEl.events.clear();\n          }\n        });\n\n        if (_options.toggleIconOnFocus) {\n          _$el.addEventListener('click', function (e) {\n            that.toggleIcon(true, _options, _$el);\n          });\n        } else {\n          _$el.addEventListener('input', function (e) {\n            that.toggleIcon(e.target.value.length > 0, _options, _$el);\n          });\n        }\n\n        if (_options.$dropMenuElement) {\n          _options.$dropMenuElement.classList.add('animated', 'hs-form-search-menu-hidden', 'hs-form-search-menu-initialized');\n\n          document.addEventListener('click', function (e) {\n            if (e.target.closest('input')) return;\n\n            if (_$el !== e.target && e.target.closest('a') || _$el !== e.target && window.getComputedStyle(_options.$dropMenuElement).display === 'block' && !e.target.closest(_options.dropMenuElement)) {\n              _options.$dropMenuElement.classList.remove('slideInUp');\n\n              _options.$dropMenuElement.classList.add('fadeOut');\n\n              if (Object.prototype.hasOwnProperty.call(that.collection[i].$initializedEl.events, 'close')) {\n                that.collection[i].$initializedEl.events.close(_options.$dropMenuElement);\n              }\n\n              if (_options.toggleIconOnFocus) {\n                that.toggleIcon(_$el.value.length > 0, _options, _$el);\n              }\n\n              setTimeout(function () {\n                _options.$dropMenuElement.classList.add('hs-form-search-menu-hidden');\n              }, _options.dropMenuDuration);\n            }\n          });\n\n          _$el.addEventListener('click', function () {\n            if (_options.$dropMenuElement.style.display !== 'block') {\n              setTimeout(function () {\n                _options.$dropMenuElement.style.top = 100 + _options.dropMenuOffset + '%';\n                _options.$dropMenuElement.style.width = '100%';\n                _options.$dropMenuElement.style.animationDuration = _options.dropMenuDuration + 'ms';\n\n                _options.$dropMenuElement.classList.remove('hs-form-search-menu-hidden', 'fadeOut');\n\n                _options.$dropMenuElement.classList.add('slideInUp');\n              }, 1);\n            }\n          });\n        }\n\n        that.collection[i].$initializedEl = _objectSpread(_objectSpread({}, that.collection[i]), {}, {\n          events: {},\n          loading: function loading() {\n            var isLoading = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : true;\n            var input = _$el.value.length > 0;\n\n            if (isLoading) {\n              _options.isLoading = true;\n              Object(_utils__WEBPACK_IMPORTED_MODULE_0__[\"fadeOut\"])(_options.$defaultIcon, 0);\n              Object(_utils__WEBPACK_IMPORTED_MODULE_0__[\"fadeOut\"])(_options.$clearIcon, 0);\n              _options.$loadingIcon.style.display = 'block';\n            } else {\n              _options.isLoading = false;\n              that.toggleIcon(input, _options, _$el);\n            }\n          },\n          on: function on(event, callback) {\n            that.collection[i].$initializedEl.events[event] = callback;\n          }\n        });\n      };\n\n      for (var i = 0; i < that.collection.length; i += 1) {\n        var _ret = _loop(i);\n\n        if (_ret === \"continue\") continue;\n      }\n    }\n  }, {\n    key: \"toggleIcon\",\n    value: function toggleIcon(input, settings, $el) {\n      var that = this;\n\n      if (!settings.isLoading) {\n        Object(_utils__WEBPACK_IMPORTED_MODULE_0__[\"fadeOut\"])(settings.$loadingIcon, 0);\n\n        if (!settings.$defaultIcon) {\n          if (input) {\n            Object(_utils__WEBPACK_IMPORTED_MODULE_0__[\"fadeIn\"])(settings.$clearIcon, settings.$loadingIcon ? 10 : settings.delay);\n            $el.classList.add(settings.activeClass);\n          } else {\n            Object(_utils__WEBPACK_IMPORTED_MODULE_0__[\"fadeOut\"])(settings.$clearIcon, 0);\n            $el.classList.remove(settings.activeClass);\n          }\n        } else {\n          if (input) {\n            Object(_utils__WEBPACK_IMPORTED_MODULE_0__[\"fadeOut\"])(settings.$defaultIcon, 0);\n            Object(_utils__WEBPACK_IMPORTED_MODULE_0__[\"fadeIn\"])(settings.$clearIcon, settings.$loadingIcon ? 10 : settings.delay);\n            $el.classList.add(settings.activeClass);\n          } else {\n            Object(_utils__WEBPACK_IMPORTED_MODULE_0__[\"fadeOut\"])(settings.$clearIcon, 0);\n            Object(_utils__WEBPACK_IMPORTED_MODULE_0__[\"fadeIn\"])(settings.$defaultIcon, settings.$loadingIcon ? 10 : settings.delay);\n            $el.classList.remove(settings.activeClass);\n          }\n        }\n      }\n    }\n  }, {\n    key: \"loading\",\n    value: function loading() {\n      var isLoading = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : true;\n      var input = this.$el.value.length > 0;\n\n      if (isLoading) {\n        this.settings.isLoading = true;\n        Object(_utils__WEBPACK_IMPORTED_MODULE_0__[\"fadeOut\"])(this.defaultIcon, 0);\n        Object(_utils__WEBPACK_IMPORTED_MODULE_0__[\"fadeOut\"])(this.clearIcon, 0);\n        this.loadingIcon.style.display = 'block';\n      } else {\n        this.settings.isLoading = false;\n        this.toggleIcon(input, this.settings);\n      }\n    }\n  }, {\n    key: \"addToCollection\",\n    value: function addToCollection(item, options, id) {\n      this.collection.push({\n        $el: item,\n        id: id || null,\n        options: Object.assign({}, defaults, item.hasAttribute(dataAttributeName) ? JSON.parse(item.getAttribute(dataAttributeName)) : {}, options)\n      });\n    }\n  }, {\n    key: \"getItem\",\n    value: function getItem(item) {\n      if (typeof item === 'number') {\n        return this.collection[item].$initializedEl;\n      } else {\n        return this.collection.find(function (el) {\n          return el.id === item;\n        }).$initializedEl;\n      }\n    }\n  }]);\n\n  return HSFormSearch;\n}();\n\n\n\n//# sourceURL=webpack://HSFormSearch/./src/js/hs-form-search.js?");

/***/ }),

/***/ "./src/js/utils/index.js":
/*!*******************************!*\
  !*** ./src/js/utils/index.js ***!
  \*******************************/
/*! exports provided: fadeIn, fadeOut */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
eval("__webpack_require__.r(__webpack_exports__);\n/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, \"fadeIn\", function() { return fadeIn; });\n/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, \"fadeOut\", function() { return fadeOut; });\nfunction fadeIn(el, time) {\n  if (!el || el.offsetParent !== null) return el;\n  el.style.opacity = 0;\n  el.style.display = 'block';\n  var last = +new Date();\n\n  var tick = function tick() {\n    el.style.opacity = +el.style.opacity + (new Date() - last) / time;\n    last = +new Date();\n\n    if (+el.style.opacity < 1) {\n      window.requestAnimationFrame && requestAnimationFrame(tick) || setTimeout(tick, 16);\n    }\n  };\n\n  tick();\n}\nfunction fadeOut(el, time) {\n  if (!el || el.offsetParent === null) return el;\n\n  if (!time) {\n    return el.style.display = 'none';\n  }\n\n  var intervalID = setInterval(function () {\n    if (!el.style.opacity) {\n      el.style.opacity = 1;\n    }\n\n    if (el.style.opacity > 0) {\n      el.style.opacity -= 0.1;\n    } else {\n      clearInterval(intervalID);\n      el.style.display = 'none';\n    }\n  }, time / 10);\n}\n\n//# sourceURL=webpack://HSFormSearch/./src/js/utils/index.js?");

/***/ })

/******/ })["default"];
});
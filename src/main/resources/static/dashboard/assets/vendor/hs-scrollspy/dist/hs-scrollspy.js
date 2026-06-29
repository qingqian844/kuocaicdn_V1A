(function webpackUniversalModuleDefinition(root, factory) {
	if(typeof exports === 'object' && typeof module === 'object')
		module.exports = factory();
	else if(typeof define === 'function' && define.amd)
		define([], factory);
	else if(typeof exports === 'object')
		exports["HSScrollspy"] = factory();
	else
		root["HSScrollspy"] = factory();
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
/******/ 	return __webpack_require__(__webpack_require__.s = "./src/js/hs-scrollspy.js");
/******/ })
/************************************************************************/
/******/ ({

/***/ "./node_modules/smoothscroll-polyfill/dist/smoothscroll.js":
/*!*****************************************************************!*\
  !*** ./node_modules/smoothscroll-polyfill/dist/smoothscroll.js ***!
  \*****************************************************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

eval("/* smoothscroll v0.4.4 - 2019 - Dustan Kasten, Jeremias Menichelli - MIT License */\n(function () {\n  'use strict';\n\n  // polyfill\n  function polyfill() {\n    // aliases\n    var w = window;\n    var d = document;\n\n    // return if scroll behavior is supported and polyfill is not forced\n    if (\n      'scrollBehavior' in d.documentElement.style &&\n      w.__forceSmoothScrollPolyfill__ !== true\n    ) {\n      return;\n    }\n\n    // globals\n    var Element = w.HTMLElement || w.Element;\n    var SCROLL_TIME = 468;\n\n    // object gathering original scroll methods\n    var original = {\n      scroll: w.scroll || w.scrollTo,\n      scrollBy: w.scrollBy,\n      elementScroll: Element.prototype.scroll || scrollElement,\n      scrollIntoView: Element.prototype.scrollIntoView\n    };\n\n    // define timing method\n    var now =\n      w.performance && w.performance.now\n        ? w.performance.now.bind(w.performance)\n        : Date.now;\n\n    /**\n     * indicates if a the current browser is made by Microsoft\n     * @method isMicrosoftBrowser\n     * @param {String} userAgent\n     * @returns {Boolean}\n     */\n    function isMicrosoftBrowser(userAgent) {\n      var userAgentPatterns = ['MSIE ', 'Trident/', 'Edge/'];\n\n      return new RegExp(userAgentPatterns.join('|')).test(userAgent);\n    }\n\n    /*\n     * IE has rounding bug rounding down clientHeight and clientWidth and\n     * rounding up scrollHeight and scrollWidth causing false positives\n     * on hasScrollableSpace\n     */\n    var ROUNDING_TOLERANCE = isMicrosoftBrowser(w.navigator.userAgent) ? 1 : 0;\n\n    /**\n     * changes scroll position inside an element\n     * @method scrollElement\n     * @param {Number} x\n     * @param {Number} y\n     * @returns {undefined}\n     */\n    function scrollElement(x, y) {\n      this.scrollLeft = x;\n      this.scrollTop = y;\n    }\n\n    /**\n     * returns result of applying ease math function to a number\n     * @method ease\n     * @param {Number} k\n     * @returns {Number}\n     */\n    function ease(k) {\n      return 0.5 * (1 - Math.cos(Math.PI * k));\n    }\n\n    /**\n     * indicates if a smooth behavior should be applied\n     * @method shouldBailOut\n     * @param {Number|Object} firstArg\n     * @returns {Boolean}\n     */\n    function shouldBailOut(firstArg) {\n      if (\n        firstArg === null ||\n        typeof firstArg !== 'object' ||\n        firstArg.behavior === undefined ||\n        firstArg.behavior === 'auto' ||\n        firstArg.behavior === 'instant'\n      ) {\n        // first argument is not an object/null\n        // or behavior is auto, instant or undefined\n        return true;\n      }\n\n      if (typeof firstArg === 'object' && firstArg.behavior === 'smooth') {\n        // first argument is an object and behavior is smooth\n        return false;\n      }\n\n      // throw error when behavior is not supported\n      throw new TypeError(\n        'behavior member of ScrollOptions ' +\n          firstArg.behavior +\n          ' is not a valid value for enumeration ScrollBehavior.'\n      );\n    }\n\n    /**\n     * indicates if an element has scrollable space in the provided axis\n     * @method hasScrollableSpace\n     * @param {Node} el\n     * @param {String} axis\n     * @returns {Boolean}\n     */\n    function hasScrollableSpace(el, axis) {\n      if (axis === 'Y') {\n        return el.clientHeight + ROUNDING_TOLERANCE < el.scrollHeight;\n      }\n\n      if (axis === 'X') {\n        return el.clientWidth + ROUNDING_TOLERANCE < el.scrollWidth;\n      }\n    }\n\n    /**\n     * indicates if an element has a scrollable overflow property in the axis\n     * @method canOverflow\n     * @param {Node} el\n     * @param {String} axis\n     * @returns {Boolean}\n     */\n    function canOverflow(el, axis) {\n      var overflowValue = w.getComputedStyle(el, null)['overflow' + axis];\n\n      return overflowValue === 'auto' || overflowValue === 'scroll';\n    }\n\n    /**\n     * indicates if an element can be scrolled in either axis\n     * @method isScrollable\n     * @param {Node} el\n     * @param {String} axis\n     * @returns {Boolean}\n     */\n    function isScrollable(el) {\n      var isScrollableY = hasScrollableSpace(el, 'Y') && canOverflow(el, 'Y');\n      var isScrollableX = hasScrollableSpace(el, 'X') && canOverflow(el, 'X');\n\n      return isScrollableY || isScrollableX;\n    }\n\n    /**\n     * finds scrollable parent of an element\n     * @method findScrollableParent\n     * @param {Node} el\n     * @returns {Node} el\n     */\n    function findScrollableParent(el) {\n      while (el !== d.body && isScrollable(el) === false) {\n        el = el.parentNode || el.host;\n      }\n\n      return el;\n    }\n\n    /**\n     * self invoked function that, given a context, steps through scrolling\n     * @method step\n     * @param {Object} context\n     * @returns {undefined}\n     */\n    function step(context) {\n      var time = now();\n      var value;\n      var currentX;\n      var currentY;\n      var elapsed = (time - context.startTime) / SCROLL_TIME;\n\n      // avoid elapsed times higher than one\n      elapsed = elapsed > 1 ? 1 : elapsed;\n\n      // apply easing to elapsed time\n      value = ease(elapsed);\n\n      currentX = context.startX + (context.x - context.startX) * value;\n      currentY = context.startY + (context.y - context.startY) * value;\n\n      context.method.call(context.scrollable, currentX, currentY);\n\n      // scroll more if we have not reached our destination\n      if (currentX !== context.x || currentY !== context.y) {\n        w.requestAnimationFrame(step.bind(w, context));\n      }\n    }\n\n    /**\n     * scrolls window or element with a smooth behavior\n     * @method smoothScroll\n     * @param {Object|Node} el\n     * @param {Number} x\n     * @param {Number} y\n     * @returns {undefined}\n     */\n    function smoothScroll(el, x, y) {\n      var scrollable;\n      var startX;\n      var startY;\n      var method;\n      var startTime = now();\n\n      // define scroll context\n      if (el === d.body) {\n        scrollable = w;\n        startX = w.scrollX || w.pageXOffset;\n        startY = w.scrollY || w.pageYOffset;\n        method = original.scroll;\n      } else {\n        scrollable = el;\n        startX = el.scrollLeft;\n        startY = el.scrollTop;\n        method = scrollElement;\n      }\n\n      // scroll looping over a frame\n      step({\n        scrollable: scrollable,\n        method: method,\n        startTime: startTime,\n        startX: startX,\n        startY: startY,\n        x: x,\n        y: y\n      });\n    }\n\n    // ORIGINAL METHODS OVERRIDES\n    // w.scroll and w.scrollTo\n    w.scroll = w.scrollTo = function() {\n      // avoid action when no arguments are passed\n      if (arguments[0] === undefined) {\n        return;\n      }\n\n      // avoid smooth behavior if not required\n      if (shouldBailOut(arguments[0]) === true) {\n        original.scroll.call(\n          w,\n          arguments[0].left !== undefined\n            ? arguments[0].left\n            : typeof arguments[0] !== 'object'\n              ? arguments[0]\n              : w.scrollX || w.pageXOffset,\n          // use top prop, second argument if present or fallback to scrollY\n          arguments[0].top !== undefined\n            ? arguments[0].top\n            : arguments[1] !== undefined\n              ? arguments[1]\n              : w.scrollY || w.pageYOffset\n        );\n\n        return;\n      }\n\n      // LET THE SMOOTHNESS BEGIN!\n      smoothScroll.call(\n        w,\n        d.body,\n        arguments[0].left !== undefined\n          ? ~~arguments[0].left\n          : w.scrollX || w.pageXOffset,\n        arguments[0].top !== undefined\n          ? ~~arguments[0].top\n          : w.scrollY || w.pageYOffset\n      );\n    };\n\n    // w.scrollBy\n    w.scrollBy = function() {\n      // avoid action when no arguments are passed\n      if (arguments[0] === undefined) {\n        return;\n      }\n\n      // avoid smooth behavior if not required\n      if (shouldBailOut(arguments[0])) {\n        original.scrollBy.call(\n          w,\n          arguments[0].left !== undefined\n            ? arguments[0].left\n            : typeof arguments[0] !== 'object' ? arguments[0] : 0,\n          arguments[0].top !== undefined\n            ? arguments[0].top\n            : arguments[1] !== undefined ? arguments[1] : 0\n        );\n\n        return;\n      }\n\n      // LET THE SMOOTHNESS BEGIN!\n      smoothScroll.call(\n        w,\n        d.body,\n        ~~arguments[0].left + (w.scrollX || w.pageXOffset),\n        ~~arguments[0].top + (w.scrollY || w.pageYOffset)\n      );\n    };\n\n    // Element.prototype.scroll and Element.prototype.scrollTo\n    Element.prototype.scroll = Element.prototype.scrollTo = function() {\n      // avoid action when no arguments are passed\n      if (arguments[0] === undefined) {\n        return;\n      }\n\n      // avoid smooth behavior if not required\n      if (shouldBailOut(arguments[0]) === true) {\n        // if one number is passed, throw error to match Firefox implementation\n        if (typeof arguments[0] === 'number' && arguments[1] === undefined) {\n          throw new SyntaxError('Value could not be converted');\n        }\n\n        original.elementScroll.call(\n          this,\n          // use left prop, first number argument or fallback to scrollLeft\n          arguments[0].left !== undefined\n            ? ~~arguments[0].left\n            : typeof arguments[0] !== 'object' ? ~~arguments[0] : this.scrollLeft,\n          // use top prop, second argument or fallback to scrollTop\n          arguments[0].top !== undefined\n            ? ~~arguments[0].top\n            : arguments[1] !== undefined ? ~~arguments[1] : this.scrollTop\n        );\n\n        return;\n      }\n\n      var left = arguments[0].left;\n      var top = arguments[0].top;\n\n      // LET THE SMOOTHNESS BEGIN!\n      smoothScroll.call(\n        this,\n        this,\n        typeof left === 'undefined' ? this.scrollLeft : ~~left,\n        typeof top === 'undefined' ? this.scrollTop : ~~top\n      );\n    };\n\n    // Element.prototype.scrollBy\n    Element.prototype.scrollBy = function() {\n      // avoid action when no arguments are passed\n      if (arguments[0] === undefined) {\n        return;\n      }\n\n      // avoid smooth behavior if not required\n      if (shouldBailOut(arguments[0]) === true) {\n        original.elementScroll.call(\n          this,\n          arguments[0].left !== undefined\n            ? ~~arguments[0].left + this.scrollLeft\n            : ~~arguments[0] + this.scrollLeft,\n          arguments[0].top !== undefined\n            ? ~~arguments[0].top + this.scrollTop\n            : ~~arguments[1] + this.scrollTop\n        );\n\n        return;\n      }\n\n      this.scroll({\n        left: ~~arguments[0].left + this.scrollLeft,\n        top: ~~arguments[0].top + this.scrollTop,\n        behavior: arguments[0].behavior\n      });\n    };\n\n    // Element.prototype.scrollIntoView\n    Element.prototype.scrollIntoView = function() {\n      // avoid smooth behavior if not required\n      if (shouldBailOut(arguments[0]) === true) {\n        original.scrollIntoView.call(\n          this,\n          arguments[0] === undefined ? true : arguments[0]\n        );\n\n        return;\n      }\n\n      // LET THE SMOOTHNESS BEGIN!\n      var scrollableParent = findScrollableParent(this);\n      var parentRects = scrollableParent.getBoundingClientRect();\n      var clientRects = this.getBoundingClientRect();\n\n      if (scrollableParent !== d.body) {\n        // reveal element inside parent\n        smoothScroll.call(\n          this,\n          scrollableParent,\n          scrollableParent.scrollLeft + clientRects.left - parentRects.left,\n          scrollableParent.scrollTop + clientRects.top - parentRects.top\n        );\n\n        // reveal parent in viewport unless is fixed\n        if (w.getComputedStyle(scrollableParent).position !== 'fixed') {\n          w.scrollBy({\n            left: parentRects.left,\n            top: parentRects.top,\n            behavior: 'smooth'\n          });\n        }\n      } else {\n        // reveal element in viewport\n        w.scrollBy({\n          left: clientRects.left,\n          top: clientRects.top,\n          behavior: 'smooth'\n        });\n      }\n    };\n  }\n\n  if (true) {\n    // commonjs\n    module.exports = { polyfill: polyfill };\n  } else {}\n\n}());\n\n\n//# sourceURL=webpack://HSScrollspy/./node_modules/smoothscroll-polyfill/dist/smoothscroll.js?");

/***/ }),

/***/ "./src/js/hs-scrollspy.js":
/*!********************************!*\
  !*** ./src/js/hs-scrollspy.js ***!
  \********************************/
/*! exports provided: default */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
eval("__webpack_require__.r(__webpack_exports__);\n/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, \"default\", function() { return HSScrollspy; });\n/* harmony import */ var smoothscroll_polyfill__WEBPACK_IMPORTED_MODULE_0__ = __webpack_require__(/*! smoothscroll-polyfill */ \"./node_modules/smoothscroll-polyfill/dist/smoothscroll.js\");\n/* harmony import */ var smoothscroll_polyfill__WEBPACK_IMPORTED_MODULE_0___default = /*#__PURE__*/__webpack_require__.n(smoothscroll_polyfill__WEBPACK_IMPORTED_MODULE_0__);\nfunction _typeof(obj) { \"@babel/helpers - typeof\"; if (typeof Symbol === \"function\" && typeof Symbol.iterator === \"symbol\") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === \"function\" && obj.constructor === Symbol && obj !== Symbol.prototype ? \"symbol\" : typeof obj; }; } return _typeof(obj); }\n\nfunction _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError(\"Cannot call a class as a function\"); } }\n\nfunction _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if (\"value\" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }\n\nfunction _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }\n\n/*\r\n* HSScrollspy Plugin\r\n* @version: 1.0.0 (Wed, 24 Nov 2021)\r\n* @author: HtmlStream\r\n* @event-namespace: .HSScrollspy\r\n* @license: Htmlstream Libraries (https://htmlstream.com/)\r\n* Copyright 2021 Htmlstream\r\n*/\n // kick off the polyfill!\n\nsmoothscroll_polyfill__WEBPACK_IMPORTED_MODULE_0___default.a.polyfill();\n\nvar HSScrollspy = /*#__PURE__*/function () {\n  function HSScrollspy(elem, settings) {\n    _classCallCheck(this, HSScrollspy);\n\n    this.$el = typeof elem === \"string\" ? document.querySelector(elem) : elem;\n    this.defaults = {\n      disableCollapse: null,\n      scrollOffset: 0,\n      collapsibleNav: null,\n      resolutionsList: {\n        xs: 0,\n        sm: 576,\n        md: 768,\n        lg: 992,\n        xl: 1200\n      },\n      resetOffset: null,\n      breakpoint: 'lg',\n      scrollspyContainer: document.body\n    };\n    this.dataSettings = this.$el.hasAttribute('data-hs-scrollspy-options') ? JSON.parse(this.$el.getAttribute('data-hs-scrollspy-options')) : {}, this.settings = Object.assign({}, this.defaults, this.dataSettings, settings);\n    this.init();\n  }\n\n  _createClass(HSScrollspy, [{\n    key: \"init\",\n    value: function init() {\n      var _this = this;\n\n      this.scrollSpyInstance = bootstrap.ScrollSpy.getInstance(this.settings.scrollspyContainer);\n      var nav = _typeof(this.scrollSpyInstance._config.target) === \"object\" ? this.scrollSpyInstance._config.target : document.querySelector(this.scrollSpyInstance._config.target);\n\n      if (this.settings.disableCollapse === null && this.$el.classList.contains('collapse')) {\n        this.settings.disableCollapse = false;\n      }\n\n      nav.addEventListener('click', function (e) {\n        if (!e.target.closest('a:not([href=\"#\"]):not([href=\"#0\"])')) return;\n        e.preventDefault();\n\n        if (_this.settings.disableCollapse === false && window.innerWidth < _this.settings.resolutionsList[_this.settings.breakpoint]) {\n          new bootstrap.Collapse(_this.$el).hide();\n          return _this.$el.addEventListener('hidden.bs.collapse', function () {\n            _this.smoothScroll(e);\n          });\n        } else {\n          _this.smoothScroll(e);\n        }\n      });\n    }\n  }, {\n    key: \"smoothScroll\",\n    value: function smoothScroll(e) {\n      var observableSection = this.scrollSpyInstance._observableSections.get(e.target.hash);\n\n      var offset = this.settings.resetOffset && window.innerWidth < this.settings.resolutionsList[this.settings.resetOffset] ? 0 : this.scrollSpyInstance._config.offset;\n      window.scroll({\n        top: observableSection.offsetTop - offset - this.settings.scrollOffset,\n        left: 0,\n        behavior: 'smooth'\n      });\n    }\n  }]);\n\n  return HSScrollspy;\n}();\n\n\n\n//# sourceURL=webpack://HSScrollspy/./src/js/hs-scrollspy.js?");

/***/ })

/******/ })["default"];
});
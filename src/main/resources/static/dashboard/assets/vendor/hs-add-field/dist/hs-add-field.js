(function webpackUniversalModuleDefinition(root, factory) {
	if(typeof exports === 'object' && typeof module === 'object')
		module.exports = factory();
	else if(typeof define === 'function' && define.amd)
		define([], factory);
	else if(typeof exports === 'object')
		exports["HSAddField"] = factory();
	else
		root["HSAddField"] = factory();
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
/******/ 	return __webpack_require__(__webpack_require__.s = "./src/js/hs-add-field.js");
/******/ })
/************************************************************************/
/******/ ({

/***/ "./src/js/hs-add-field.js":
/*!********************************!*\
  !*** ./src/js/hs-add-field.js ***!
  \********************************/
/*! exports provided: default */
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
eval("__webpack_require__.r(__webpack_exports__);\n/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, \"default\", function() { return HSAddField; });\nfunction _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError(\"Cannot call a class as a function\"); } }\n\nfunction _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if (\"value\" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }\n\nfunction _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }\n\n/*\n* HSAddField Plugin\n* @version: 2.0.1 (Jul, 31 Nov 2021)\n* @author: HtmlStream\n* @event-namespace: .HSAddField\n* @license: Htmlstream Libraries (https://htmlstream.com/)\n* Copyright 2021 Htmlstream\n*/\nvar dataAttributeName = 'data-hs-add-field-options';\nvar defaults = {\n  createTrigger: '.js-create-field',\n  deleteTrigger: '.js-delete-field',\n  limit: 5,\n  defaultCreated: 1,\n  nameSeparator: '_',\n  addedField: function addedField() {},\n  deletedField: function deletedField() {}\n};\n\nvar HSAddField = /*#__PURE__*/function () {\n  function HSAddField(el, options, id) {\n    _classCallCheck(this, HSAddField);\n\n    this.collection = [];\n    var that = this;\n    var elems;\n\n    if (el instanceof HTMLElement) {\n      elems = [el];\n    } else if (el instanceof Object) {\n      elems = el;\n    } else {\n      elems = document.querySelectorAll(el);\n    }\n\n    for (var i = 0; i < elems.length; i += 1) {\n      that.addToCollection(elems[i], options, id || elems[i].id);\n    }\n\n    if (!that.collection.length) {\n      return false;\n    } // initialization calls\n\n\n    that._init();\n\n    return this;\n  }\n\n  _createClass(HSAddField, [{\n    key: \"_init\",\n    value: function _init() {\n      var that = this;\n\n      var _loop = function _loop(i) {\n        var _$el = void 0;\n\n        var _options = void 0;\n\n        if (that.collection[i].hasOwnProperty('$initializedEl')) {\n          return \"continue\";\n        }\n\n        _$el = that.collection[i].$el;\n        _options = that.collection[i].options;\n        _options.flags = {\n          name: 'data-name',\n          \"delete\": 'data-hs-add-field-delete'\n        };\n        _options.fieldsCount = 0;\n        _options.fieldsCount = _options.defaultCreated;\n        _options.tempalte = document.querySelector(_options.template);\n        _options.contaienr = _$el.querySelector(_options.container);\n\n        for (key = 0; key < _options.defaultCreated; key++) {\n          that.addField(_$el, _options);\n        }\n\n        _$el.addEventListener('click', function (e) {\n          if (e.target.closest(_options.createTrigger)) {\n            that.addField(_$el, _options);\n          } else if (e.target.closest(_options.deleteTrigger)) {\n            that.deleteField(_$el, _options, e.target.closest(_options.deleteTrigger).getAttribute(_options.flags[\"delete\"]));\n          }\n        });\n      };\n\n      for (var i = 0; i < that.collection.length; i += 1) {\n        var key;\n\n        var _ret = _loop(i);\n\n        if (_ret === \"continue\") continue;\n      }\n    }\n  }, {\n    key: \"addField\",\n    value: function addField($el, settings) {\n      var that = this;\n\n      if (settings.fieldsCount < settings.limit) {\n        var field = settings.tempalte.cloneNode(true);\n        field.removeAttribute('id');\n        field.style.display = null;\n        settings.contaienr.appendChild(field);\n        that.updateFieldsCount($el, settings);\n        that.renderName($el, settings);\n        that.renderKeys($el, settings);\n        that.toggleCreateButton($el, settings);\n        settings.addedField(field);\n      }\n    }\n  }, {\n    key: \"deleteField\",\n    value: function deleteField($el, settings, index) {\n      var that = this;\n\n      if (settings.fieldsCount > 0) {\n        settings.contaienr.childNodes[index].parentNode.removeChild(settings.contaienr.childNodes[index]);\n        that.updateFieldsCount($el, settings);\n        that.renderName($el, settings);\n        that.renderKeys($el, settings);\n        that.toggleCreateButton($el, settings);\n        settings.deletedField();\n      }\n    }\n  }, {\n    key: \"renderName\",\n    value: function renderName($el, settings) {\n      settings.contaienr.childNodes.forEach(function (el, key) {\n        if (el.nodeName === '#text') return;\n        var field = el.querySelector(\"[\".concat(settings.flags.name, \"]\"));\n        if (!field) return;\n        field.setAttribute('name', \"\".concat(field.getAttribute('data-name')).concat(settings.nameSeparator).concat(key));\n      });\n    }\n  }, {\n    key: \"renderKeys\",\n    value: function renderKeys($el, settings) {\n      settings.contaienr.childNodes.forEach(function (el, key) {\n        if (el.nodeName === '#text') return;\n        var deleteTrigger = el.querySelector(settings.deleteTrigger);\n        deleteTrigger ? deleteTrigger.setAttribute(settings.flags[\"delete\"], key) : null;\n      });\n    }\n  }, {\n    key: \"updateFieldsCount\",\n    value: function updateFieldsCount($el, settings) {\n      settings.fieldsCount = settings.contaienr.childNodes.length;\n    }\n  }, {\n    key: \"toggleCreateButton\",\n    value: function toggleCreateButton($el, settings) {\n      var createTrigger = $el.querySelector(settings.createTrigger);\n\n      if (settings.fieldsCount === settings.limit) {\n        createTrigger.style.display = 'none';\n      } else {\n        createTrigger.style.display = null;\n      }\n    }\n  }, {\n    key: \"addToCollection\",\n    value: function addToCollection(item, options, id) {\n      this.collection.push({\n        $el: item,\n        id: id || null,\n        options: Object.assign({}, defaults, item.hasAttribute(dataAttributeName) ? JSON.parse(item.getAttribute(dataAttributeName)) : {}, options)\n      });\n    }\n  }, {\n    key: \"getItem\",\n    value: function getItem(item) {\n      if (typeof item === 'number') {\n        return this.collection[item].$initializedEl;\n      } else {\n        return this.collection.find(function (el) {\n          return el.id === item;\n        }).$initializedEl;\n      }\n    }\n  }]);\n\n  return HSAddField;\n}();\n\n\n\n//# sourceURL=webpack://HSAddField/./src/js/hs-add-field.js?");

/***/ })

/******/ })["default"];
});
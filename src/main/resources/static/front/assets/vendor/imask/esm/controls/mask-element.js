import { _ as _createClass, a as _classCallCheck } from '../_rollupPluginBabelHelpers-b054ecd2.js';
import IMask from '../core/holder.js';

/**
  Generic element API to use with mask
  @interface
*/
var MaskElement = /*#__PURE__*/function () {
  function MaskElement() {
    _classCallCheck(this, MaskElement);
  }

  _createClass(MaskElement, [{
    key: "selectionStart",
    get:
    /** */

    /** */

    /** */

    /** Safely returns selection start */
    function get() {
      var start;

      try {
        start = this._unsafeSelectionStart;
      } catch (e) {}

      return start != null ? start : this.value.length;
    }
    /** Safely returns selection end */

  }, {
    key: "selectionEnd",
    get: function get() {
      var end;

      try {
        end = this._unsafeSelectionEnd;
      } catch (e) {}

      return end != null ? end : this.value.length;
    }
    /** Safely sets element selection */

  }, {
    key: "select",
    value: function select(start, end) {
      if (start == null || end == null || start === this.selectionStart && end === this.selectionEnd) return;

      try {
        this._unsafeSelect(start, end);
      } catch (e) {}
    }
    /** Should be overriden in subclasses */

  }, {
    key: "_unsafeSelect",
    value: function _unsafeSelect(start, end) {}
    /** Should be overriden in subclasses */

  }, {
    key: "isActive",
    get: function get() {
      return false;
    }
    /** Should be overriden in subclasses */

  }, {
    key: "bindEvents",
    value: function bindEvents(handlers) {}
    /** Should be overriden in subclasses */

  }, {
    key: "unbindEvents",
    value: function unbindEvents() {}
  }]);

  return MaskElement;
}();
IMask.MaskElement = MaskElement;

export { MaskElement as default };

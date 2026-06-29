import { d as _inherits, e as _createSuper, _ as _createClass, a as _classCallCheck, g as _get, h as _getPrototypeOf } from '../_rollupPluginBabelHelpers-b054ecd2.js';
import Masked from './base.js';
import IMask from '../core/holder.js';
import '../core/change-details.js';
import '../core/continuous-tail-details.js';
import '../core/utils.js';

/** Masking by RegExp */

var MaskedRegExp = /*#__PURE__*/function (_Masked) {
  _inherits(MaskedRegExp, _Masked);

  var _super = _createSuper(MaskedRegExp);

  function MaskedRegExp() {
    _classCallCheck(this, MaskedRegExp);

    return _super.apply(this, arguments);
  }

  _createClass(MaskedRegExp, [{
    key: "_update",
    value:
    /**
      @override
      @param {Object} opts
    */
    function _update(opts) {
      if (opts.mask) opts.validate = function (value) {
        return value.search(opts.mask) >= 0;
      };

      _get(_getPrototypeOf(MaskedRegExp.prototype), "_update", this).call(this, opts);
    }
  }]);

  return MaskedRegExp;
}(Masked);
IMask.MaskedRegExp = MaskedRegExp;

export { MaskedRegExp as default };

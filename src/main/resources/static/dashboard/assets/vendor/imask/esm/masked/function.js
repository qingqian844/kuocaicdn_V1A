import { d as _inherits, e as _createSuper, a as _classCallCheck, _ as _createClass, g as _get, h as _getPrototypeOf } from '../_rollupPluginBabelHelpers-b054ecd2.js';
import Masked from './base.js';
import IMask from '../core/holder.js';
import '../core/change-details.js';
import '../core/continuous-tail-details.js';
import '../core/utils.js';

/** Masking by custom Function */

var MaskedFunction = /*#__PURE__*/function (_Masked) {
  _inherits(MaskedFunction, _Masked);

  var _super = _createSuper(MaskedFunction);

  function MaskedFunction() {
    _classCallCheck(this, MaskedFunction);

    return _super.apply(this, arguments);
  }

  _createClass(MaskedFunction, [{
    key: "_update",
    value:
    /**
      @override
      @param {Object} opts
    */
    function _update(opts) {
      if (opts.mask) opts.validate = opts.mask;

      _get(_getPrototypeOf(MaskedFunction.prototype), "_update", this).call(this, opts);
    }
  }]);

  return MaskedFunction;
}(Masked);
IMask.MaskedFunction = MaskedFunction;

export { MaskedFunction as default };

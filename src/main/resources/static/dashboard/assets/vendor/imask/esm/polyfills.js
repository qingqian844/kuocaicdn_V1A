var $$3 = require('../internals/export');

var assign = require('../internals/object-assign'); // `Object.assign` method
// https://tc39.es/ecma262/#sec-object.assign
// eslint-disable-next-line es/no-object-assign -- required for testing


$$3({
  target: 'Object',
  stat: true,
  forced: Object.assign !== assign
}, {
  assign: assign
});

var $$2 = require('../internals/export');

var repeat = require('../internals/string-repeat'); // `String.prototype.repeat` method
// https://tc39.es/ecma262/#sec-string.prototype.repeat


$$2({
  target: 'String',
  proto: true
}, {
  repeat: repeat
});

var $$1 = require('../internals/export');

var $padStart = require('../internals/string-pad').start;

var WEBKIT_BUG$1 = require('../internals/string-pad-webkit-bug'); // `String.prototype.padStart` method
// https://tc39.es/ecma262/#sec-string.prototype.padstart


$$1({
  target: 'String',
  proto: true,
  forced: WEBKIT_BUG$1
}, {
  padStart: function padStart(maxLength
  /* , fillString = ' ' */
  ) {
    return $padStart(this, maxLength, arguments.length > 1 ? arguments[1] : undefined);
  }
});

var $ = require('../internals/export');

var $padEnd = require('../internals/string-pad').end;

var WEBKIT_BUG = require('../internals/string-pad-webkit-bug'); // `String.prototype.padEnd` method
// https://tc39.es/ecma262/#sec-string.prototype.padend


$({
  target: 'String',
  proto: true,
  forced: WEBKIT_BUG
}, {
  padEnd: function padEnd(maxLength
  /* , fillString = ' ' */
  ) {
    return $padEnd(this, maxLength, arguments.length > 1 ? arguments[1] : undefined);
  }
});

// TODO: Remove from `core-js@4`
require('../modules/es.global-this');

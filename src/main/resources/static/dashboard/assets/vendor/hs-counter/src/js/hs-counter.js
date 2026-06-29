/*
* HSCounter Plugin
* @version: 2.0.1 (Sun, 0 Aug 2021)
* @requires: appear.js v1.0.3
* @author: HtmlStream
* @event-namespace: .HSCounter
* @license: Htmlstream Libraries (https://htmlstream.com/)
* Copyright 2021 Htmlstream
*/

const dataAttributeName = 'data-hs-counter-options'
const defaults = {
  bounds: -100,
  debounce: 10,
  time: 2000,
  fps: 60,
  isCommaSeparated: false,
  isReduceThousandsTo: false,
  intervalId: null
}

export default class HSCounter {
  constructor(el, options, id) {
    this.collection = []
    const that = this
    let elems

    if (el instanceof HTMLElement) {
      elems = [el]
    } else if (el instanceof Object) {
      elems = el
    } else {
      elems = document.querySelectorAll(el)
    }

    for (let i = 0; i < elems.length; i += 1) {
      that.addToCollection(elems[i], options, id || elems[i].id)
    }

    if (!that.collection.length) {
      return false
    }

    // initialization calls
    that._init()

    return this
  }

  _init() {
    const that = this;

    for (let i = 0; i < that.collection.length; i += 1) {
      let _$el
      let _options

      if (that.collection[i].hasOwnProperty('$initializedEl')) {
        continue
      }

      _$el = that.collection[i].$el;
      _options = that.collection[i].options

      const appearSettings = Object.assign({}, this.settings, {
        init: () => {
          var value = parseInt(_$el.textContent, 10)

          _$el.innerHTML = '0'
          _$el.setAttribute('data-value', value)
        },
        elements: () => {
          return [_$el]
        },
        appear: (innerEl) => {
          var $item = innerEl,
            counter = 1,
            endValue = $item.getAttribute('data-value'),
            iterationValue = parseInt(endValue / (_options.time / _options.fps), 10)

          if (iterationValue === 0) {
            iterationValue = 1
          }

          $item.data = {
            intervalId: setInterval(() => {
              if (_options.isCommaSeparated) {
                $item.innerHTML = this._getCommaSeparatedValue(counter += iterationValue)
              } else if (_options.isReduceThousandsTo) {
                $item.innerHTML = this._getCommaReducedValue(counter += iterationValue, _options.isReduceThousandsTo)
              } else {
                $item.innerHTML = counter += iterationValue
              }

              if (counter > endValue) {
                clearInterval($item.data.intervalId)

                if (_options.isCommaSeparated) {
                  $item.innerHTML = this._getCommaSeparatedValue(endValue)
                } else if (_options.isReduceThousandsTo) {
                  $item.innerHTML = this._getCommaReducedValue(endValue, _options.isReduceThousandsTo)
                } else {
                  $item.innerHTML = endValue
                }
              }
            }, _options.time / _options.fps)
          }
        }
      })
      appear(appearSettings)

      that.collection[i].$initializedEl = _options
    }
  }

  _getCommaReducedValue(value, additionalText) {
    return parseInt(value / 1000, 10) + additionalText
  }

  _getCommaSeparatedValue(value) {
    value = value.toString()

    switch (value.length) {
      case 4:
        return `${value.substr(0, 1)},${value.substr(1)}`
        break
      case 5:
        return `${value.substr(0, 2)},${value.substr(2)}`
        break
      case 6:
        return `${value.substr(0, 3)},${value.substr(3)}`
        break
      case 7:
        value = `${value.substr(0, 1)},${value.substr(1)}`
        return `${value.substr(0, 5)},${value.substr(5)}`
        break
      case 8:
        value = `${value.substr(0, 2)},${value.substr(2)}`
        return `${value.substr(0, 6)},${value.substr(6)}`
        break
      case 9:
        value = `${value.substr(0, 3)},${value.substr(3)}`
        return `${value.substr(0, 7)},${value.substr(7)}`
        break
      case 10:
        value = `${value.substr(0, 1)},${value.substr(1)}`
        value = `${value.substr(0, 5)},${value.substr(5)}`
        return `${value.substr(0, 9)},${value.substr(9)}`
        break
      default:
        return value
    }
  }

  addToCollection (item, options, id) {
    this.collection.push({
      $el: item,
      id: id || null,
      options: Object.assign(
        {},
        defaults,
        item.hasAttribute(dataAttributeName)
          ? JSON.parse(item.getAttribute(dataAttributeName))
          : {},
        options,
      ),
    })
  }

  getItem (item) {
    if (typeof item === 'number') {
      return this.collection[item].$initializedEl;
    } else {
      return this.collection.find(el => {
        return el.id === item;
      }).$initializedEl;
    }
  }
}

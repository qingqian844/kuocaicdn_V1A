/*
* HSCountCharacters Plugin
* @version: 1.0.1 (Sun, 1 Aug 2021)
* @author: HtmlStream
* @event-namespace: .HSCountCharacters
* @license: Htmlstream Libraries (https://htmlstream.com/)
* Copyright 2021 Htmlstream
*/

const dataAttributeName = 'data-hs-count-characters-options'
const defaults = {}

export default class HSCountCharacters {
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

      _options.output = document.querySelector(_options.output)
      _options.maxLength = _$el.hasAttribute('maxlength') ? '/ ' + _$el.getAttribute('maxlength') : ''

      that._updateOutput(_$el, _options)

      _$el.addEventListener('input', () => {
        that._updateOutput(_$el, _options)
      })

      that.collection[i].$initializedEl = _options
    }
  }

  _updateOutput($el, settings) {
    settings.output.innerHTML = `${$el.value.length} ${settings.maxLength}`
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

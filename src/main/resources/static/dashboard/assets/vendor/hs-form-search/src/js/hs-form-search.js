import {fadeIn, fadeOut} from "./utils";

const dataAttributeName = 'data-hs-form-search-options'
const defaults = {
  clearIcon: null,
  defaultIcon: null,
  delay: 300,
  isLoading: false,
  dropMenuOffset: 0,
  dropMenuDuration: 300,
  toggleIconOnFocus: false,
  activeClass: null,
  handlers: {}
}

export default class HSFormSearch {
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
    const that = this

    for (let i = 0; i < that.collection.length; i += 1) {
      let _$el
      let _options

      if (that.collection[i].hasOwnProperty('$initializedEl')) {
        continue
      }

      _$el = that.collection[i].$el;
      _options = that.collection[i].options


      _options.$loadingIcon = document.querySelector(_options.loadingIcon)
      _options.$clearIcon = document.querySelector(_options.clearIcon)
      _options.$defaultIcon = document.querySelector(_options.defaultIcon)
      _options.$dropMenuElement = document.querySelector(_options.dropMenuElement)
      this.toggleIcon(_$el.value.length > 0, _options, _$el)

      _options.$clearIcon.addEventListener('click', () => {
        _$el.value = ''
        that.toggleIcon(false, _options, _$el)

        if (Object.prototype.hasOwnProperty.call(that.collection[i].$initializedEl.events, 'clear')) {
          that.collection[i].$initializedEl.events.clear()
        }
      });

      if (_options.toggleIconOnFocus) {
        _$el.addEventListener('click', e => {
          that.toggleIcon(true, _options, _$el)
        })
      } else {
        _$el.addEventListener('input', e => {
          that.toggleIcon(e.target.value.length > 0, _options, _$el)
        })
      }

      if (_options.$dropMenuElement) {
        _options.$dropMenuElement.classList.add('animated', 'hs-form-search-menu-hidden', 'hs-form-search-menu-initialized');

        document.addEventListener('click', e => {
          if (e.target.closest('input')) return

          if (_$el !== e.target && e.target.closest('a') || _$el !== e.target && window.getComputedStyle(_options.$dropMenuElement).display === 'block' && !e.target.closest(_options.dropMenuElement)) {
            _options.$dropMenuElement.classList.remove('slideInUp')
            _options.$dropMenuElement.classList.add('fadeOut')
            if (Object.prototype.hasOwnProperty.call(that.collection[i].$initializedEl.events, 'close')) {
              that.collection[i].$initializedEl.events.close(_options.$dropMenuElement);
            }

            if (_options.toggleIconOnFocus) {
              that.toggleIcon(_$el.value.length > 0, _options, _$el)
            }

            setTimeout(() => {
              _options.$dropMenuElement.classList.add('hs-form-search-menu-hidden')
            }, _options.dropMenuDuration);
          }
        });

        _$el.addEventListener('click', () => {
          if (_options.$dropMenuElement.style.display !== 'block') {
            setTimeout(() => {
              _options.$dropMenuElement.style.top = 100 + _options.dropMenuOffset + '%'
              _options.$dropMenuElement.style.width = '100%'
              _options.$dropMenuElement.style.animationDuration = _options.dropMenuDuration + 'ms'

              _options.$dropMenuElement.classList.remove('hs-form-search-menu-hidden', 'fadeOut')
              _options.$dropMenuElement.classList.add('slideInUp')
            }, 1)
          }
        });
      }

      that.collection[i].$initializedEl = {
        ...that.collection[i],
        events: {},
        loading: function (isLoading = true) {
          const input = _$el.value.length > 0

          if (isLoading) {
            _options.isLoading = true;

            fadeOut(_options.$defaultIcon, 0);
            fadeOut(_options.$clearIcon, 0);

            _options.$loadingIcon.style.display = 'block'
          } else {
            _options.isLoading = false;

            that.toggleIcon(input, _options, _$el);
          }
        },
        on: function (event, callback) {
          that.collection[i].$initializedEl.events[event] = callback
        }
      }
    }
  }

  toggleIcon(input, settings, $el) {
    const that = this

    if (!settings.isLoading) {
      fadeOut(settings.$loadingIcon, 0);

      if (!settings.$defaultIcon) {
        if (input) {
          fadeIn(settings.$clearIcon, settings.$loadingIcon ? 10 : settings.delay);
          $el.classList.add(settings.activeClass)
        } else {
          fadeOut(settings.$clearIcon, 0);
          $el.classList.remove(settings.activeClass)
        }
      } else {
        if (input) {
          fadeOut(settings.$defaultIcon, 0)
          fadeIn(settings.$clearIcon, settings.$loadingIcon ? 10 : settings.delay);
          $el.classList.add(settings.activeClass)
        } else {
          fadeOut(settings.$clearIcon, 0);
          fadeIn(settings.$defaultIcon, settings.$loadingIcon ? 10 : settings.delay);
          $el.classList.remove(settings.activeClass)
        }
      }
    }
  }

  loading(isLoading = true) {
    const input = this.$el.value.length > 0

    if (isLoading) {
      this.settings.isLoading = true;

      fadeOut(this.defaultIcon, 0);
      fadeOut(this.clearIcon, 0);

      this.loadingIcon.style.display = 'block'
    } else {
      this.settings.isLoading = false;

      this.toggleIcon(input, this.settings);
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

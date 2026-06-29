
/*
* HSStepForm Plugin
* @version: 3.0.1 (Sun, 1 Aug 2021)
* @author: HtmlStream
* @event-namespace: .HSStepForm
* @license: Htmlstream Libraries (https://htmlstream.com/)
* Copyright 2021 Htmlstream
*/

const dataAttributeName = 'data-hs-step-form-options'
const defaults = {
  progressSelector: null,
  progressItems: null,

  stepsSelector: null,
  stepsItems: null,
  stepsActiveItem: null,

  nextSelector: '[data-hs-step-form-next-options]',
  prevSelector: '[data-hs-step-form-prev-options]',
  endSelector: null,

  isValidate: false,

  classMap: {
    active: 'active',
    checked: 'is-valid',
    error: 'is-invalid',
    required: 'js-step-required',
    focus: 'focus'
  },

  finish: () => {
  },

  preventNextStep: () => {
    return new Promise((resolve, reject) => {
      resolve()
    })
  },

  onNextStep: () => {
  },

  onPrevStep: () => {
  }
}

export default class HSStepForm {
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

      _options.progressItems = _$el.querySelector(_options.progressSelector).children
      _options.stepsItems = _$el.querySelector(_options.stepsSelector).children
      _options.stepsActiveItem = _$el.querySelector(_options.stepsSelector).querySelector(`.${_options.classMap.active}`)

      that._prepareObject(_$el, _options)

      _$el.querySelectorAll(_options.nextSelector).forEach(item => {
        item.addEventListener('click', () => {
          that._nextClickEvents(_$el, _options, item)
        })
      })

      _$el.querySelectorAll(_options.prevSelector).forEach(item => {
        item.addEventListener('click', () => {
          that._prevClickEvents(_$el, _options, item)
        })
      })

      _$el.querySelectorAll(_options.endSelector).forEach(item => {
        item.addEventListener('click', () => {
          that._endClickEvents(_$el, _options)
        })
      })

      that.collection[i].$initializedEl = _options
    }
  }

  _prepareObject($el, settings) {
    $el.querySelector(settings.stepsSelector).querySelectorAll(`:scope > :not(.${settings.classMap.active})`).forEach(item => {
      item.style.display = 'none'
    })

    settings.progressItems[[...settings.stepsActiveItem.parentNode.children].indexOf(settings.stepsActiveItem)].classList.add(settings.classMap.active, settings.classMap.focus)
  }

  _endClickEvents($el, settings) {
    let isValid = true

    if (settings.isValidate) {
      $el.classList.remove('was-validated')
      settings.progressItems[settings.progressItems.length - 1].classList.remove(settings.classMap.error)

      Array.from($el.elements)
        .forEach(item => {
          if (item.offsetParent !== null && !item.checkValidity()) {
            isValid = false

            settings.progressItems[settings.progressItems.length - 1].classList.add(settings.classMap.error)

            if (settings.validator) {
              settings.validator.updateFieldStete(item)
              $el.classList.add('was-validated')
            }
          }
        })
    }

    if (isValid) {
      return settings.finish($el, settings)
    }
  }

  _nextClickEvents($el, settings, nextEl) {
    const nextDataSettings = nextEl.hasAttribute('data-hs-step-form-next-options') ? JSON.parse(nextEl.getAttribute('data-hs-step-form-next-options')) : {}
    let nextItemDefaults = {
        targetSelector: null
      },
      nextItemOptions = Object.assign({}, nextItemDefaults, nextDataSettings)

    const targetSelector = $el.querySelector(nextItemOptions.targetSelector)
    const targetIndex = [...targetSelector.parentNode.children].indexOf(targetSelector)

    for (let i = 0; i < settings.progressItems.length; i++) {
      if (settings.isValidate) {
        if (settings.validator) {
          $el.classList.remove('was-validated')
        }

        if (targetIndex > i) {
          settings.progressItems[i].classList.add(settings.classMap.error)

          let requiredSelector = settings.progressItems[i].querySelector(settings.nextSelector).getAttribute('data-hs-step-form-next-options')

          for (let item of settings.stepsItems) {
            item.classList.remove(settings.classMap.active)
            item.style.display = 'none'
          }

          const newTargetSelector = $el.querySelector(JSON.parse(requiredSelector).targetSelector)

          newTargetSelector.classList.add(settings.classMap.active)
          newTargetSelector.style.display = 'block'

          let isValid = true

          Array.from($el.elements)
            .forEach(item => {
              if (item.offsetParent !== null && !item.checkValidity()) {
                isValid = false

                if (settings.validator) {
                  settings.validator.updateFieldStete(item)
                  $el.classList.add('was-validated')
                }
              }
            })

          if (!isValid) {
            settings.progressItems[i].classList.remove(settings.classMap.checked)
            return false
          } else {
            settings.progressItems[i].classList.remove(settings.classMap.error)
          }
        }

        if (targetIndex > i && settings.isValidate) {
          settings.progressItems[i].classList.add(settings.classMap.checked)
        }
      } else {
        if (targetIndex > i && settings.isValidate) {
          settings.progressItems[i].classList.add(settings.classMap.checked)
        }

        if (targetIndex > i && !settings.isValidate) {
          settings.progressItems[i].classList.add(settings.classMap.active)
        }
      }
    }

    settings.preventNextStep($el)
      .then(() => {
        for (let item of settings.progressItems) {
          item.classList.remove(settings.classMap.active, settings.classMap.focus)
        }

        settings.progressItems[targetIndex].classList.add(settings.classMap.active, settings.classMap.focus)

        for (let item of settings.stepsItems) {
          item.classList.remove(settings.classMap.active)
          item.style.display = 'none'
        }

        targetSelector.classList.add(settings.classMap.active)
        this.fadeIn(targetSelector, 400)

        return settings.onNextStep()
      })
  }

  _prevClickEvents($el, settings, prevEl) {
    const prevDataSettings = prevEl.hasAttribute('data-hs-step-form-prev-options') ? JSON.parse(prevEl.getAttribute('data-hs-step-form-prev-options')) : {}
    let prevItemDefaults = {
        targetSelector: null
      },
      prevItemOptions = Object.assign({}, prevItemDefaults, prevDataSettings)

    const targetSelector = $el.querySelector(prevItemOptions.targetSelector)
    const targetIndex = [...targetSelector.parentNode.children].indexOf(targetSelector)

    for (let i = 0; i < settings.progressItems.length; i++) {
      if (settings.isValidate) {
        if (targetIndex > i) {
          settings.progressItems[i].classList.add(settings.classMap.error)

          let requiredSelector = settings.progressItems[i].querySelector(settings.nextSelector).getAttribute('data-hs-step-form-next-options')

          for (let item of settings.stepsItems) {
            item.classList.remove(settings.classMap.active)
            item.style.display = 'none'
          }

          const newTargetSelector = $el.querySelector(JSON.parse(requiredSelector).targetSelector)

          newTargetSelector.classList.add(settings.classMap.active)
          newTargetSelector.style.display = 'block'


          let isValid = true

          Array.from($el.elements)
            .forEach(item => {
              if (item.offsetParent !== null && !item.checkValidity()) {
                isValid = false
              }
            })

          if (!isValid) {
            settings.progressItems[i].classList.remove(settings.classMap.checked)
            return false
          } else {
            settings.progressItems[i].classList.remove(settings.classMap.error)
          }
        }

        if (targetIndex > i && settings.isValidate) {
          settings.progressItems[i].classList.add(settings.classMap.checked)
        }
      } else {
        if (targetIndex > i && settings.isValidate) {
          settings.progressItems[i].classList.add(settings.classMap.checked)
        }

        if (targetIndex > i && !settings.isValidate) {
          settings.progressItems[i].classList.add(settings.classMap.active)
        }
      }
    }

    for (let item of settings.progressItems) {
      item.classList.remove(settings.classMap.active, settings.classMap.focus)
    }

    settings.progressItems[targetIndex].classList.add(settings.classMap.active, settings.classMap.focus)

    for (let item of settings.stepsItems) {
      item.classList.remove(settings.classMap.active)
      item.style.display = 'none'
    }

    targetSelector.classList.add(settings.classMap.active)
    this.fadeIn(targetSelector, 400)

    return settings.onPrevStep()
  }

  fadeIn(el, time) {
    el.style.opacity = 0
    el.style.display = 'block'

    var last = +new Date()
    var tick = function() {
      el.style.opacity = +el.style.opacity + (new Date() - last) / time
      last = +new Date()

      if (+el.style.opacity < 1) {
        (window.requestAnimationFrame && requestAnimationFrame(tick)) || setTimeout(tick, 16)
      }
    }

    tick()
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

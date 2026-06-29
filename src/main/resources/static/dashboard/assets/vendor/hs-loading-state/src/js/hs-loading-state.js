/*
* HSLoadingState Plugin
* @version: 2.0.0 (Sun, 1 Aug 2021)
* @author: HtmlStream
* @event-namespace: .HSLoadingState
* @license: Htmlstream Libraries (https://htmlstream.com/)
* Copyright 2021 Htmlstream
*/


import {fadeOut, createElementFromHTML} from "../utils";
import fullscreenToggleClass from "../../../hs-fullscreen/src/js/methods/toggle-class";
import fullscreenRemoveClass from "../../../hs-fullscreen/src/js/methods/remove-class";

const dataAttributeName = 'data-hs-loading-state-options'
const defaults = {
  targetEl: null,
  targetElStyles: {
    position: ''
  },
  targetElCustomStyles: {
    position: 'relative'
  },

  eventType: 'click',
  loaderMode: 'simple',
  loaderText: 'Loading...',
  removeLoaderDelay: 1500,

  loaderContainerClassNames: 'hs-loader-wrapper',
  loaderContainerExtendedClassNames: '',

  loaderClassNames: 'hs-loader',
  loaderExtendedClassNames: '',
  loaderWithTextClassNames: 'hs-loader-with-text',

  loaderIconClassNames: 'spinner-border spinner-border-sm text-primary',
  loaderIconExtendedClassNames: '',

  loaderTextClassNames: 'hs-loader-text',
  loaderTextExtendedClassNames: '',

  beforeLoading: null,
  afterLoading: null
}

export default class HSLoadingState {
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

      _$el = that.collection[i].$el
      _options = that.collection[i].options

      this._loading(_$el, _options)

      that.collection[i].$initializedEl = _options
    }
  }

  _loading($el, settings) {
    const that = this
    $el.addEventListener(settings.eventType,  () => {
      const $loader = createElementFromHTML(that._selectTemplate(settings))

      if (typeof settings.beforeLoading === 'function') {
        let before = settings.beforeLoading($el, settings);

        if (before === false) return;
      }

      const $target = document.querySelector(settings.targetEl)

      $target.style = settings.targetElCustomStyles
      $target.appendChild($loader)

      $loader.style.display = 'block'

      setTimeout(() => {
        fadeOut($loader, 400, () => {
          document.querySelector(settings.targetEl).style = settings.targetElStyles

          $loader.parentNode.removeChild($loader)

          if (typeof settings.afterLoading === 'function') {
            settings.afterLoading($el, settings);
          }
        });
      }, settings.removeLoaderDelay);
    });
  }

  _selectTemplate(settings) {
    if (settings.loaderMode === 'with-text') {
      return `<div class="${settings.loaderContainerClassNames} ${settings.loaderContainerExtendedClassNames}">
				<div class="${settings.loaderClassNames} ${settings.loaderExtendedClassNames} ${settings.loaderWithTextClassNames}">
					<span class="${settings.loaderTextClassNames} ${settings.loaderTextExtendedClassNames}">${settings.loaderText}</span>
					<span class="${settings.loaderIconClassNames} ${settings.loaderIconExtendedClassNames}"></span>
				</div>
      </div>`;
    } else {
      return `<div class="${settings.loaderContainerClassNames} ${settings.loaderContainerExtendedClassNames}">
				<div class="${settings.loaderClassNames} ${settings.loaderExtendedClassNames}">
					<span class="${settings.loaderIconClassNames} ${settings.loaderIconExtendedClassNames}"></span>
				</div>
      </div>`;
    }
  }

  addToCollection(item, options, id) {
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
    });
  }

  getItems() {
    const that = this;
    let newCollection = [];

    for (let i = 0; i < that.collection.length; i += 1) {
      newCollection.push(that.collection[i].$initializedEl);
    }

    return newCollection;
  }

  getItem(ind) {
    return this.collection[ind].$initializedEl;
  }
}

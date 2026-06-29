/*
* HSFullscreen Plugin
* @version: 2.0.0 (Sun, 1 Aug 2021)
* @author: HtmlStream
* @event-namespace: .HSFullscreen
* @license: Htmlstream Libraries (https://htmlstream.com/)
* Copyright 2021 Htmlstream
*/


import fullscreenRemoveClass from "./methods/remove-class"
import fullscreenToggleClass from "./methods/toggle-class"

const dataAttributeName = 'data-hs-fullscreen-options'
const defaults = {
	targetEl: null,
	mainContainerSelector: 'body',
	toggleClassName: '.hs-fullscreen',
	preventScrollClassName: '.hs-fullscreen-on'
}

export default class HSFullscreen {
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

			_options.targetEl = document.querySelector(_options.targetEl)

			_$el.addEventListener('click', () => {
				fullscreenToggleClass(_options.targetEl, _options)
			})

			document.addEventListener('keydown', e => {
				if (!_options.targetEl.classList.contains(_options.toggleClassName.slice(1))) return

				if (e.keyCode === 27) {
					fullscreenRemoveClass(_options.targetEl, _options)
				}
			})

			that.collection[i].$initializedEl = _options
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

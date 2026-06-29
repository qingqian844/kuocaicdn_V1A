/*
* HSRemoveElement Plugin
* @version: 2.0.0 (Sun, 1 Aug 2021)
* @author: HtmlStream
* @event-namespace: .HSRemoveElement
* @license: Htmlstream Libraries (https://htmlstream.com/)
* Copyright 2021 Htmlstream
*/

const dataAttributeName = 'data-hs-remove-element-options'
const defaults = {
	targetEl: null,

	beforeDelete: null,
	afterDelete: null,
}

export default class HSRemoveElement {
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
			this.removeElement(_$el, _options)

			that.collection[i].$initializedEl = _options
		}
	}

	removeElement($el, settings) {
		$el.addEventListener('click', () => {
			if (typeof settings.beforeDelete === 'function') {
				let before = settings.beforeDelete($el, settings)

				if (before === false) return
			}

			settings.targetEl.parentNode.removeChild(settings.targetEl)

			if (typeof settings.afterDelete === 'function') {
				settings.afterDelete($el, settings)
			}
		})
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


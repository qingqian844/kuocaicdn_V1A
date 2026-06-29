/*
* HSToggleState Plugin
* @version: 3.0.1 (Sun, 01 Aug 2021)
* @author: HtmlStream
* @event-namespace: .HSToggleState
* @license: Htmlstream Libraries (https://htmlstream.com/)
* Copyright 2021 Htmlstream
*/

const dataAttributeName = 'data-hs-toggle-state-options'
const defaults = {
	targetSelector: null,
	slaveSelector: null,

	classMap: {
		toggle: 'toggled'
	}
}

export default class HSToggleState {

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

			that.prepareObject(_$el, _options)
			const $slaveSelector = document.querySelector(_options.slaveSelector)

			_$el.addEventListener('click', () => {
				_$el.classList.toggle(_options.classMap.toggle)

				if (_options.slaveSelector) {
					if (_$el.classList.contains(_options.classMap.toggle)) {
						$slaveSelector.classList.add(_options.classMap.toggle)
					} else {
						$slaveSelector.classList.remove(_options.classMap.toggle)
					}
				}

				that.checkState(_$el, _options)
			});

			if ($slaveSelector) {
				$slaveSelector.addEventListener('click', () => {
					document.querySelector(`[data-hs-toggle-state-slave="${_options.slaveSelector}"]`).classList.remove(_options.classMap.toggle)
				})
			}

			that.collection[i].$initializedEl = _options
		}
	}

	prepareObject($el, settings) {
		$el.setAttribute('data-hs-toggle-state-slave', settings.slaveSelector)
	}

	checkState($el, settings) {
		const $targetSelectors = Array.from(document.querySelectorAll(settings.targetSelector))
		if ($el.classList.contains(settings.classMap.toggle)) {
			$targetSelectors.forEach($target => $target.checked = true)
		} else {
			$targetSelectors.forEach($target => $target.checked = false)
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

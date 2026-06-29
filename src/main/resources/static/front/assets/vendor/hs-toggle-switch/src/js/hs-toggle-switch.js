/*
* HSToggleSwitch Plugin
* @version: 1.0.0 (Mon, 12 Dec 2019)
* @requires: countup.js v2.0.4
* @author: HtmlStream
* @event-namespace: .HSToggleSwitch
* @license: Htmlstream Libraries (https://htmlstream.com/)
* Copyright 2019 Htmlstream
*/

import {CountUp} from 'countup.js'

const dataAttributeName = 'data-hs-toggle-switch-options'
const dataAttributeItemName = 'data-hs-toggle-switch-item-options'
const defaults = {
	mode: 'toggle-count',
	targetSelector: undefined,
	isChecked: false,
	eventType: 'change'
}

export default class HSToggleSwitch {
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

			_options.isChecked = _$el.checked
			_options.$targets = document.querySelectorAll(_options.targetSelector)

			if (_options.mode === 'toggle-count') {
				if (_options.isChecked) {
					_options.isChecked = true

					_options.$targets.forEach($target => {
						const	currentDataSettings = $target.hasAttribute(dataAttributeItemName) ? JSON.parse($target.getAttribute(dataAttributeItemName)) : {}
						$target.innerHTML = currentDataSettings.max
					})
				}

				_$el.addEventListener(_options.eventType, () => that._toggleCount(_options))
			}
		}
	}

	// Toggle Count
	_toggleCount(settings) {
		if (settings.isChecked) {
			this._countDownEach(settings)
		} else {
			this._countUpEach(settings)
		}
	}

	_countUpEach(settings) {
		settings.isChecked = true

		settings.$targets.forEach($target => {
			const currentDataSettings = $target.hasAttribute(dataAttributeItemName) ? JSON.parse($target.getAttribute(dataAttributeItemName)) : {}

			let currentDefaults = {
					duration: .5,
					useEasing: false
				},
				currentOptions = {}
			currentOptions = Object.assign({}, currentDefaults, currentDataSettings)

			this._countUp($target, currentOptions)
		})
	}

	_countDownEach(settings) {
		settings.isChecked = false

		settings.$targets.forEach($target => {
			const currentDataSettings = $target.hasAttribute(dataAttributeItemName) ? JSON.parse($target.getAttribute(dataAttributeItemName)) : {}

			let currentDefaults = {
					duration: .5,
					useEasing: false
				},
				currentOptions = {}
			currentOptions = Object.assign({}, currentDefaults, currentDataSettings)

			this._countDown($target, currentOptions)
		})
	}

	_countUp(el, data) {
		const defaults = {
			startVal: data.min
		}
		let options = Object.assign({}, defaults, data)

		const countUp = new CountUp(el, data.max, options)

		countUp.start()
	}

	_countDown(el, data) {
		const defaults = {
			startVal: data.max
		}
		let options = Object.assign({}, defaults, data)

		const countUp = new CountUp(el, data.min, options)

		countUp.start()
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

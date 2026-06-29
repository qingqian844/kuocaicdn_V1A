/*
* HSCounter Plugin
* @version: 2.0.0 (Mon, 25 Nov 2019)
* @requires: jQuery v3.0 or later, appear.js v1.0.3
* @author: HtmlStream
* @event-namespace: .HSCounter
* @license: Htmlstream Libraries (https://htmlstream.com/)
* Copyright 2019 Htmlstream
*/

export default class HSCounter {
	constructor(el, settings) {
		this.$el = typeof el === "string" ? document.querySelector(el) : el
		this.defaults = {
			bounds: -100,
			debounce: 10,
			time: 2000,
			fps: 60,
			isCommaSeparated: false,
			isReduceThousandsTo: false,
			intervalId: null
		}
		this.dataSettings = this.$el.hasAttribute('data-hs-counter-options') ? JSON.parse(this.$el.getAttribute('data-hs-counter-options')) : {}
		this.settings = Object.assign({}, this.defaults, this.dataSettings, settings)
	}

	init() {
		const appearSettings = {
			init: () => {
				var value = parseInt(this.$el.textContent, 10)

				this.$el.innerHTML = '0'
				this.$el.setAttribute('data-value', value)
			},
			elements: () => {
				return [this.$el]
			},
			appear: (innerEl) => {
				var $item = innerEl,
					counter = 1,
					endValue = $item.getAttribute('data-value'),
					iterationValue = parseInt(endValue / (this.settings.time / this.settings.fps), 10)

				if (iterationValue === 0) {
					iterationValue = 1
				}

				$item.data = {intervalId: setInterval(() => {
						if (this.settings.isCommaSeparated) {
							$item.innerHTML = this._getCommaSeparatedValue(counter += iterationValue)
						} else if (this.settings.isReduceThousandsTo) {
							$item.innerHTML = this._getCommaReducedValue(counter += iterationValue, this.settings.isReduceThousandsTo)
						} else {
							$item.innerHTML = counter += iterationValue
						}

						if (counter > endValue) {
							clearInterval($item.data.intervalId)

							if (this.settings.isCommaSeparated) {
								$item.innerHTML = this._getCommaSeparatedValue(endValue)
							} else if (this.settings.isReduceThousandsTo) {
								$item.innerHTML = this._getCommaReducedValue(endValue, this.settings.isReduceThousandsTo)
							} else {
								$item.innerHTML = endValue
							}
						}
					}, this.settings.time / this.settings.fps)}
			}
		}

		const options = Object.assign({}, this.settings, appearSettings)

		appear(options)
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
}

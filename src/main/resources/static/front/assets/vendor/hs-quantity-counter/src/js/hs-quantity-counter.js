const dataAttributeName = 'data-hs-quantity-counter-options'
const defaults = {
	classMap: {
		plus: '.js-plus',
		minus: '.js-minus',
		result: '.js-result'
	},
	resultVal: null
}

export default class HSQuantityCounter {
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

	_init () {
		const that = this;

		for (let i = 0; i < that.collection.length; i += 1) {
			let _$el
			let _options

			if (that.collection[i].hasOwnProperty('$initializedEl')) {
				continue
			}

			_$el = that.collection[i].$el;
			_options = that.collection[i].options

			// Change Default Values
			_options.resultVal = parseInt(_$el.querySelector(_options.classMap.result).value)

			// Plus Click Events
			_$el.querySelector(_options.classMap.plus).addEventListener('click', () => {
				that._plusClickEvents(_$el, _options)
			})

			// Minus Click Events
			_$el.querySelector(_options.classMap.minus).addEventListener('click', () => {
				that._minusClickEvents(_$el, _options)
			})
		}
	}

	_plusClickEvents($el, settings) {
		settings.resultVal += 1

		$el.querySelector(settings.classMap.result).value++
	}

	_minusClickEvents($el, settings) {
		if (settings.resultVal >= 1) {
			settings.resultVal -= 1

			$el.querySelector(settings.classMap.result).value--
		} else {
			return false
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

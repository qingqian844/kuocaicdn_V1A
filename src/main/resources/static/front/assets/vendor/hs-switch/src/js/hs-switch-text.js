const dataAttributeName = 'data-hs-switch-text-options'
const defaults = {
	target: null,
	eventType: 'change',
	afterChange: null,
	startUpdateOnChange: false
}

export default class HSSwitchText {
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

			_$el.addEventListener(_options.eventType,() => {
				for (let i = 0; i < _options.target.length; i++) {
					const $el = document.querySelector(_options.target[i].selector)
					$el.innerHTML =_options.target[i].text
				}

				if (typeof _options.afterChange === "function") {
					_options.afterChange();
				}
			})
			
			that.collection[i].$initializedEl = _options
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

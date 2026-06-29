/*
* HSAddField Plugin
* @version: 2.0.1 (Jul, 31 Nov 2021)
* @author: HtmlStream
* @event-namespace: .HSAddField
* @license: Htmlstream Libraries (https://htmlstream.com/)
* Copyright 2021 Htmlstream
*/

const dataAttributeName = 'data-hs-add-field-options'
const defaults = {
    createTrigger: '.js-create-field',
    deleteTrigger: '.js-delete-field',
    limit: 5,
    defaultCreated: 1,
    nameSeparator: '_',

    addedField: function () {
    },
    deletedField: function () {
    }
}

export default class HSAddField {
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

            _options.flags = {
                name: 'data-name',
                delete: 'data-hs-add-field-delete'
            }
            _options.fieldsCount = 0
            _options.fieldsCount = _options.defaultCreated;
            _options.tempalte = document.querySelector(_options.template)
            _options.contaienr = _$el.querySelector(_options.container)

            for (var key = 0; key < _options.defaultCreated; key++) {
                that.addField(_$el, _options)
            }

            _$el.addEventListener('click', (e) => {
                if (e.target.closest(_options.createTrigger)) {
                    that.addField(_$el, _options)
                } else if (e.target.closest(_options.deleteTrigger)) {
                    that.deleteField(_$el, _options, e.target.closest(_options.deleteTrigger).getAttribute(_options.flags.delete))
                }
            })
        }
    }

    addField($el, settings) {
        const that = this

        if (settings.fieldsCount < settings.limit) {
            const field = settings.tempalte.cloneNode(true)
            field.removeAttribute('id')
            field.style.display = null

            settings.contaienr.appendChild(field)

            that.updateFieldsCount($el, settings);
            that.renderName($el, settings);
            that.renderKeys($el, settings);
            that.toggleCreateButton($el, settings);

            settings.addedField(field);
        }
    }

    deleteField($el, settings, index) {
        const that = this
        if (settings.fieldsCount > 0) {
            settings.contaienr.childNodes[index].parentNode.removeChild(settings.contaienr.childNodes[index])

            that.updateFieldsCount($el, settings);
            that.renderName($el, settings);
            that.renderKeys($el, settings);
            that.toggleCreateButton($el, settings);

            settings.deletedField()
        }
    }

    renderName($el, settings) {        
        settings.contaienr.childNodes.forEach((el, key) => {
            if (el.nodeName === '#text') return        
            const field = el.querySelector(`[${settings.flags.name}]`)
            if (!field) return
            field.setAttribute('name', `${field.getAttribute('data-name')}${settings.nameSeparator}${key}`)
        })
    }

    renderKeys($el, settings) {
        settings.contaienr.childNodes.forEach((el, key) => {
            if (el.nodeName === '#text') return        
            const deleteTrigger = el.querySelector(settings.deleteTrigger)
            deleteTrigger ? deleteTrigger.setAttribute(settings.flags.delete, key) : null
        })
    }

    updateFieldsCount($el, settings) {
        settings.fieldsCount = settings.contaienr.childNodes.length
    }

    toggleCreateButton($el, settings) {
        const createTrigger = $el.querySelector(settings.createTrigger)

        if (settings.fieldsCount === settings.limit) {
            createTrigger.style.display = 'none'
        } else {
            createTrigger.style.display = null
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
        })
    }

    getItem(item) {
        if (typeof item === 'number') {
            return this.collection[item].$initializedEl;
        } else {
            return this.collection.find(el => {
                return el.id === item;
            }).$initializedEl;
        }
    }
}

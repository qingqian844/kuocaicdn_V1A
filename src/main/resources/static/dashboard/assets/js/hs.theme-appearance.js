const HSThemeAppearance = {
  currentThemeAttributeName: 'data-hs-current-theme',
  visablilityAttributeName: 'data-hs-theme-appearance',

  init() {
    const defaultTheme = window.hs_config.themeAppearance.layoutSkin
    let theme = localStorage.getItem('hs_theme') || defaultTheme

    if (theme === 'auto') {
      theme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'default'
    }

    const $appearances = document.querySelectorAll(`[data-hs-appearance="${theme}"]`)

    this._linkElementsAction(theme, $appearances, () => {
      document.querySelector('[data-hs-appearance-onload-styles]').remove()
    })

    this._setVisablilityStyles(theme)
  },

  setAppearance(theme, saveInStore = true) {
    const scrollTop = window.pageYOffset

    if (saveInStore) {
      localStorage.setItem('hs_theme', theme)
    }

    if (theme === 'auto') {
      theme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'default'
    }

    const $appearances = document.querySelectorAll(`[data-hs-appearance="${theme}"]`)
    if (!$appearances.length) {
      return console.error(`Theme '${theme}' not found.`)
    }

    const $resetStyles = this._resetStylesOnLoad()

    this._linkElementsAction(theme, $appearances, () => {
      window.scrollTo({top: scrollTop})
      this._removeOldStyleElements()
      this._setVisablilityStyles(theme)
      $resetStyles.remove()
    })

    window.dispatchEvent(new CustomEvent('on-hs-appearance-change', {detail: theme}))
  },

  _resetStylesOnLoad() {
    const $resetStyles = document.createElement('style')
    $resetStyles.innerText = `*{transition: unset !important;}body{opacity: 0 !important;}`
    $resetStyles.setAttribute('data-hs-appearance-onload-styles', '')
    document.head.appendChild($resetStyles)
    return $resetStyles
  },

  _setVisablilityStyles(theme) {
    const attrubuteName = 'data-hs-appearance-visability-styles'
    let $style = document.querySelector(`[${attrubuteName}]`)

    if (!$style) {
      $style = document.createElement('style')
      $style.setAttribute(attrubuteName, '')
      document.head.append($style)
    }

    $style.textContent = `[${this.visablilityAttributeName}]:not([${this.visablilityAttributeName}='${theme}']){display:none!important;}`
  },

  _linkElementsAction(theme, appearances, callback = null) {
    this._setOldAppearanceElements()
    const linkElements = []

    appearances.forEach($appearanceNode => {
      const $link = document.createElement('link')
      $link.setAttribute('rel', 'stylesheet')
      $link.setAttribute('href', $appearanceNode.getAttribute('href'))
      $link.setAttribute(this.currentThemeAttributeName, 'stylesheet')
      document.head.insertAdjacentElement('afterEnd', $link)
      linkElements.push($link)
    })

    if (callback) {
      let loaded = 0

      linkElements.forEach($styleNode => {
        $styleNode.addEventListener('load', function () {
          loaded++
          if (loaded === linkElements.length) {
            return callback()
          }
        })
      })
    }
  },

  _setOldAppearanceElements() {
    const $currentAppearanceElements = document.querySelectorAll(`[${this.currentThemeAttributeName}]`)
    if ($currentAppearanceElements.length) {
      $currentAppearanceElements.forEach($appearnce => $appearnce.setAttribute('data-hs-appearnce-old', true))
    }
  },

  _removeOldStyleElements() {
    const $oldAppearanceElements = document.querySelectorAll(`[${'data-hs-appearnce-old'}]`)
    if ($oldAppearanceElements.length) {
      $oldAppearanceElements.forEach($appearnce => $appearnce.remove())
    }
  },

  getAppearance() {
    let theme = this.getOriginalAppearance()

    if (theme === 'auto') {
      theme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'default'
    }

    return theme
  },

  getOriginalAppearance() {
    const defaultTheme = window.hs_config.themeAppearance.layoutSkin
    return localStorage.getItem('hs_theme') || defaultTheme
  }
}

HSThemeAppearance.init()

window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', e => {
  if (HSThemeAppearance.getOriginalAppearance() === 'auto') {
    HSThemeAppearance.setAppearance('auto', false)
  }
})

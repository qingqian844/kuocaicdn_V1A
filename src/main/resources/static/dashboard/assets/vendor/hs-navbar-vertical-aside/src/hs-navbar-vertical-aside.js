import slideUp from "./utils/slideUp"
import slideDown from "./utils/slideDown"
import getParents from "./utils/getParents"

export default class HSSideNav {
  constructor(el, settings) {
    this.$el = typeof el === "string" ? document.querySelector(el) : el
    if (!this.$el) return
    this.defaults = {
      defaultWidth: 0,

      mainContainer: 'body',
      autoscrollToActive: true,

      compactClass: '.navbar-vertical-aside-compact-mode',
      compactMinClass: '.navbar-vertical-aside-compact-mini-mode',
      minClass: '.navbar-vertical-aside-mini-mode',
      closedClass: '.navbar-vertical-aside-closed-mode',
      navbarVertical: '.navbar-vertical-content',

      transitionOnClassName: 'navbar-vertical-aside-transition-on',

      mobileOverlayClass: '.navbar-vertical-aside-mobile-overlay',
      toggleInvokerClass: '.js-navbar-vertical-aside-toggle-invoker',

      subMenuClass: '.js-navbar-vertical-aside-submenu',
      subMenuInvokerClass: '.js-navbar-vertical-aside-menu-link',
      subMenuInvokerActiveClass: '.show',
      hasSubMenuClass: '.navbar-vertical-aside-has-menu',

      subMenuAnimationSpeed: 200,
      subMenuOpenEvent: 'hover',

      showClassNames: {
        xs: 'navbar-vertical-aside-show-xs',
        sm: 'navbar-vertical-aside-show-sm',
        md: 'navbar-vertical-aside-show-md',
        lg: 'navbar-vertical-aside-show-lg',
        xl: 'navbar-vertical-aside-show-xl'
      },

      $showedMenu: null,

      onMini: function () {
      },
      onFull: function () {
      },
      onInitialized: function () {
      }
    }

    this.dataSettings = this.$el.hasAttribute('data-hs-navbar-vertical-aside') ? JSON.parse(this.$el.getAttribute('data-hs-navbar-vertical-aside')) : {}
    this.settings = Object.assign({}, this.defaults, this.dataSettings, settings)

    this.openedMenus = []
    this.items = this.$el.querySelectorAll(this.settings.hasSubMenuClass)
    // this.topLevels = document.querySelector(this.settings.hasSubMenuClass).parentNode.closest(':not(' + this.settings.subMenuClass + ')').querySelectorAll(`:scope > ${this.settings.hasSubMenuClass}`)


    this.$container = document.querySelector(this.settings.mainContainer)
    this.isMini = this.$container.classList.contains(this.settings.minClass.slice(1))
    this.isCompact = this.$container.classList.contains(this.settings.compactClass.slice(1))

    this.initializedClass = '.navbar-vertical-aside-initialized'
  }

  init() {
    if (!this.$el) return

    this.setState()

    if (this.settings.autoscrollToActive) {
      const $active = this.$el.querySelector('.active')
      if ($active) {
        if ($active.getBoundingClientRect().y > document.querySelector(this.settings.navbarVertical).getBoundingClientRect().height) {
          setTimeout(() => {
            $active.scrollIntoView({behavior: 'smooth'})
          }, 100)
        }
      }
    }

    // Click events
    document.addEventListener('click', e => {
      // Toggle aside menu
      if (e.target.closest(this.settings.toggleInvokerClass)) {
        this.toggleSidebar()
      }
    })

    // Rebuild states for aside menu on resizing
    window.addEventListener('resize', () => {
      if (window.innerWidth !== this.defaultWidth) {
        this.setState()
      }
    })

    var collapseElementList = [].slice.call(document.querySelectorAll('.nav-collapse'))
    this.collapseList = collapseElementList.map((collapseEl) => {
      return new bootstrap.Collapse(collapseEl, {
        toggle: false
      })
    })
    const $mainContainer = document.querySelector(this.settings.mainContainer)

    this.topLevelElements = collapseElementList.filter(collapseEl => getParents(collapseEl, '.nav-collapse').length === 1)

    // Toggle sub menus on hover
    var timeOut = null

    if (this.settings.subMenuOpenEvent === 'hover') {
      this.collapseList.forEach(collapse => {
        Array.from([collapse._element, collapse._element.previousElementSibling]).forEach($el => {
          $el.addEventListener('mouseenter', e => {
            if (!$mainContainer.classList.contains(this.settings.minClass.slice(1)) && !this.isCompact) return
            clearTimeout(timeOut)

            if (this.topLevelElements.includes(collapse._element)) {
              collapse.show()
            }
          })

          $el.addEventListener('mouseleave', e => {
            if (!$mainContainer.classList.contains(this.settings.minClass.slice(1)) && !this.isCompact) return
            if (this.topLevelElements.includes($el.parentElement.querySelector('.nav-collapse'))) {
              timeOut = setTimeout(() => {
                collapse.hide()
              }, 200)
            }
          })
        })
      })
    }

    function prepareParentsTargetID($menu) {
      let id = $menu.getAttribute('id')
      $menu.querySelectorAll('.nav-collapse').forEach($subMenu => {
        if (id && !$subMenu.hasAttribute('hs-parent-area')) {
          $subMenu.setAttribute('hs-parent-area', `#${id}`)
          prepareParentsTargetID($subMenu)
        }
      })
    }

    prepareParentsTargetID(document.querySelector('#navbarVerticalMenu'))

    this.collapseList.forEach(collapse => {
      collapse._element.addEventListener('show.bs.collapse', (e) => {
        const trigeredEl = e.target,
          parentEl = e.target.hasAttribute('hs-parent-area') ? document.querySelector(e.target.getAttribute('hs-parent-area')) : null

        trigeredEl.previousElementSibling.setAttribute('aria-expanded', true)

        // Remove animation on mobile
        if (($mainContainer.classList.contains(this.settings.minClass.slice(1)) || this.isCompact) && this.topLevelElements.includes(trigeredEl)) {
          e.preventDefault()
          this.setPosition(trigeredEl, trigeredEl.previousElementSibling)
          trigeredEl.style.height = 'auto'
          trigeredEl.classList.add('show')
        }

        // Check if menu is outside of the screen
        setTimeout(() => {
          if (($mainContainer.classList.contains(this.settings.minClass.slice(1)) || this.isCompact) && parentEl && parentEl.offsetHeight + parentEl.offsetTop > window.innerHeight) {
            const distance = parentEl.offsetHeight + parentEl.offsetTop - window.innerHeight

            parentEl.style.top = parentEl.offsetTop - distance + 'px'
            parentEl.style.transition = '.4s'

            setTimeout(() => {
              parentEl.style.transition = 'unset'
            }, 400)
          }
        }, 500)

        // Close others submenu
        this.collapseList.forEach(function (collapse) {
          let collapseEl = collapse._element
          if (collapseEl === trigeredEl) return
          let triggeredArea = trigeredEl.getAttribute('hs-parent-area'),
            collapseArea = collapseEl.getAttribute('hs-parent-area')
          if ((collapseEl && triggeredArea) ? collapseArea === triggeredArea : false) {
            collapse.hide()
            collapseEl.classList.remove('nav-collapse-action-mobile')
            collapseEl.previousElementSibling.setAttribute('aria-expanded', false)
          }
        })
      })

      collapse._element.addEventListener('hide.bs.collapse', (e) => {
        const trigeredEl = e.target
        trigeredEl.classList.remove('nav-collapse-action-mobile')
        trigeredEl.previousElementSibling.setAttribute('aria-expanded', false)

        // Remove animation on mobile
        if (($mainContainer.classList.contains(this.settings.minClass.slice(1)) || this.isCompact) && this.topLevelElements.includes(trigeredEl)) {
          trigeredEl.style.opacity = 0
          setTimeout(() => {
            trigeredEl.style.opacity = 1
          }, 400)
        }

        // Collapse all sub menus
        trigeredEl.querySelectorAll('.nav-collapse').forEach($menu => {
          let collapse = this.collapseList.find(collapse => collapse._element === $menu)
          if (collapse) collapse.hide(false)
        })
      })
    })

    // Add overlay for mobile
    const $sideNavOverlay = document.createElement('div')
    $sideNavOverlay.classList.add(this.settings.toggleInvokerClass.slice(1), this.settings.mobileOverlayClass.slice(1))
    document.body.appendChild($sideNavOverlay)

    // Add transition state
    this.$el.addEventListener('transitionend', () => {
      document.querySelector(this.settings.mainContainer).classList.remove(this.settings.transitionOnClassName)
    })

    // Done initializing
    this.$el.classList.add(this.initializedClass.slice(1))

    document.querySelectorAll(this.settings.toggleInvokerClass).forEach(el => el.style.opacity = 1)

    setTimeout(() => {
      this.settings.onInitialized()
    })
  }

  toggleOnHover(e, menu) {
   let collapse = this.collapseList.find(collapse => collapse._element.previousElementSibling === e.target && collapse._element === menu)

   if (collapse) {
     collapse.toggle()
   }
  }

  setState() {
    this.defaultWidth = window.innerWidth

    const isClosed = this.showResolutionChecking(),
      mini = this.isMini || this.isCompact ? true : false

    if (isClosed) {
      this.sidebarToggleClass = this.settings.closedClass

      this.$container.classList.add(this.settings.closedClass.slice(1))

      if (!mini) {
        this.$container.classList.remove(this.settings.minClass.slice(1))
      }
    } else {
      this.sidebarToggleClass = this.settings.minClass

      this.$container.classList.remove(this.settings.closedClass.slice(1))
    }

    // If mini mode, add save active item and remove show class to hide it
    if (mini) {
      this.settings.$showedMenu = document.querySelector('.nav-collapse.show')
      if (this.settings.$showedMenu) {
        this.settings.$showedMenu.classList.remove('show')
      }
    }
  }

  showResolutionChecking() {
    if (this.$container.classList.contains(this.settings.showClassNames.xs) && window.innerWidth <= 0) {

      return true

    } else if (this.$container.classList.contains(this.settings.showClassNames.sm) && window.innerWidth <= 576) {

      return true

    } else if (this.$container.classList.contains(this.settings.showClassNames.md) && window.innerWidth <= 768) {

      return true

    } else if (this.$container.classList.contains(this.settings.showClassNames.lg) && window.innerWidth <= 992) {

      return true

    } else if (this.$container.classList.contains(this.settings.showClassNames.xl) && window.innerWidth <= 1200) {

      return true

    } else {

      return false

    }
  }

  toggleSubMenu($invoker) {
    if (!$invoker) return null

    // Prepare variables
    let collapseOthers = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : true,
      $menu = $invoker.querySelector(this.settings.subMenuClass),
      $mainContainer = document.querySelector(this.settings.mainContainer),
      allExcludeTarget = [...$invoker.parentNode.querySelectorAll(`:scope > ${this.settings.hasSubMenuClass}`)].filter($item => $item !== $invoker),
      onAction = $mainContainer.classList.contains(this.settings.transitionOnClassName),
      topLevel = !$invoker.parentNode.classList.contains(this.settings.subMenuClass.slice(1)),
      mini = $mainContainer.classList.contains(this.settings.minClass.slice(1))
      || $mainContainer.classList.contains(this.settings.compactMinClass.slice(1))
        ? true : false,
      parentMenu = $invoker

    // Close excluded targets
    if (collapseOthers && onAction || collapseOthers && topLevel && mini) {
      allExcludeTarget.reduce((acc, $item) => {
        return acc = [...acc, ...$item.querySelectorAll(this.settings.subMenuClass)]
      }, [])
        .forEach($item => {
          $item.style.display = 'none'
          $item.parentNode.classList.remove(this.settings.subMenuInvokerActiveClass.slice(1))
        })
    } else if (collapseOthers) {
      allExcludeTarget.reduce((acc, $item) => {
        return acc = [...acc, ...$item.querySelectorAll(this.settings.subMenuClass)]
      }, [])
        .forEach($item => {
          slideUp($item, this.settings.subMenuAnimationSpeed)
            .parentNode.classList.remove(this.settings.subMenuInvokerActiveClass.slice(1))
        })
    }

    // Close sub menu immediately
    if (onAction || topLevel && mini) {
      $menu.style.transition = 'unset'

      if (window.getComputedStyle($menu).display === 'none') {
        $menu.style.display = 'block'
      } else {
        $menu.style.display = 'none'
      }
    }

    // Close sub menu with animation
    else {
      while (parentMenu.parentNode.classList.contains(this.settings.subMenuClass.slice(1))) {
        parentMenu = parentMenu.parentNode
      }

      if (window.getComputedStyle($menu).display === 'none') {
        slideDown($menu, this.settings.subMenuAnimationSpeed)
      } else {
        slideUp($menu, this.settings.subMenuAnimationSpeed)
      }

      if (mini) {
        setTimeout(() => {
          if (parentMenu.offsetHeight + parentMenu.offsetTop > window.innerHeight) {
            const distance = parentMenu.offsetHeight + parentMenu.offsetTop - window.innerHeight
            parentMenu.style.top = parentMenu.offsetTop - distance + 'px'
            parentMenu.style.transition = '.4s'
          }
        }, this.settings.subMenuAnimationSpeed)
      }
    }

    // Toggle Class
    $invoker.classList.contains(this.settings.subMenuInvokerActiveClass.slice(1))
      ? $invoker.classList.remove(this.settings.subMenuInvokerActiveClass.slice(1))
      : $invoker.classList.add(this.settings.subMenuInvokerActiveClass.slice(1))

    // Smart position
    if ($menu.offsetParent) {
      this.setPosition($menu, $invoker)
      document.querySelector('.navbar-vertical-container').addEventListener('scroll', () => {
        this.setPosition($menu, $invoker)
      }, 1000)
    }

    return $invoker
  }

  toggleSidebar() {
    console.log(123)
    // Get opened menus
    const notHidden = els => [...els].filter($el => window.getComputedStyle($el).display !== 'none')

    let $mainContainer = document.querySelector(this.settings.mainContainer)

    $mainContainer.classList.add(this.settings.transitionOnClassName)

    // Toggle class
    $mainContainer.classList.contains(this.sidebarToggleClass.slice(1))
      ? $mainContainer.classList.remove(this.sidebarToggleClass.slice(1))
      : $mainContainer.classList.add(this.sidebarToggleClass.slice(1))

    // Toggle aside
    if ($mainContainer.classList.contains(this.sidebarToggleClass.slice(1))) {
      $mainContainer.classList.add(this.settings.minClass.slice(1))
    } else {
      $mainContainer.classList.remove(this.settings.minClass.slice(1))
    }

    // Additional for plugin
    if (!this.showResolutionChecking() && $mainContainer.classList.contains(this.settings.minClass.slice(1)) || this.showResolutionChecking() && $mainContainer.classList.contains(this.settings.closedClass.slice(1))) {
      this.settings.onMini()
      window.localStorage.setItem('hs-navbar-vertical-aside-mini', false)
    } else {
      this.settings.onFull()
      window.localStorage.removeItem('hs-navbar-vertical-aside-mini')
    }

    // Close/Open sub menus
    if ($mainContainer.classList.contains(this.settings.minClass.slice(1)) || this.isCompact) {
      const $menu = document.querySelector('.nav-collapse.show')
      if (!$menu) return

      $menu.classList.remove('show')
      $menu.classList.add('nav-collapse-action-mobile')

      const collapse = this.collapseList.find(collapse => collapse._element === $menu)
      collapse.hide()
    } else {

      // If the mini mod is enabled, when expand the sidebar, a menu will open with an active item
      if (this.settings.$showedMenu) {
        this.settings.$showedMenu.classList.add('show')
        this.settings.$showedMenu = null
      }

      document.querySelectorAll('.nav-collapse-action-mobile').forEach($item => {
        $item.classList.remove('nav-collapse-action-mobile')
        $item.classList.add('show')

        document.querySelectorAll('.nav-collapse.show').forEach($menu => {
          $menu.classList.add('show')
        })
      })

      document.querySelectorAll('.nav-collapse').forEach($item => {
        $item.style.top = 0
      })
    }
  }

  setPosition($menu, $invoker) {
    $menu.classList.add('nav-collapse-action-mobile')
    $menu.style.top = $invoker.getBoundingClientRect().top + 'px'

    setTimeout(() => {
      if ($menu.offsetHeight + $menu.offsetTop > window.innerHeight) {
        const distance = $menu.offsetHeight + $menu.offsetTop - window.innerHeight

        $menu.style.top = $invoker.offsetTop - distance + 'px'
      }
    })
  }
}

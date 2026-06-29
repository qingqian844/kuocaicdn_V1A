import { ChangeDetectionStrategy, Component, ContentChildren, EventEmitter, HostBinding, Inject, Input, Output, PLATFORM_ID, ViewChild, ViewEncapsulation, } from '@angular/core';
// @ts-ignore
import Swiper from 'swiper';
import { of, Subject } from 'rxjs';
import { getParams } from './utils/get-params';
import { SwiperSlideDirective } from './swiper-slide.directive';
import { extend, isObject, setProperty, ignoreNgOnChanges, coerceBooleanProperty, isShowEl, isEnabled, } from './utils/utils';
import { isPlatformBrowser } from '@angular/common';
import * as i0 from "@angular/core";
import * as i1 from "@angular/common";
export class SwiperComponent {
    constructor(_ngZone, elementRef, _changeDetectorRef, _platformId) {
        this._ngZone = _ngZone;
        this.elementRef = elementRef;
        this._changeDetectorRef = _changeDetectorRef;
        this._platformId = _platformId;
        this.slideClass = 'swiper-slide';
        this.wrapperClass = 'swiper-wrapper';
        this.showNavigation = true;
        this.showPagination = true;
        this.showScrollbar = true;
        this.s__beforeBreakpoint = new EventEmitter();
        this.s__containerClasses = new EventEmitter();
        this.s__slideClass = new EventEmitter();
        this.s__swiper = new EventEmitter();
        this.s_activeIndexChange = new EventEmitter();
        this.s_afterInit = new EventEmitter();
        this.s_autoplay = new EventEmitter();
        this.s_autoplayStart = new EventEmitter();
        this.s_autoplayStop = new EventEmitter();
        this.s_autoplayPause = new EventEmitter();
        this.s_autoplayResume = new EventEmitter();
        this.s_beforeDestroy = new EventEmitter();
        this.s_beforeInit = new EventEmitter();
        this.s_beforeLoopFix = new EventEmitter();
        this.s_beforeResize = new EventEmitter();
        this.s_beforeSlideChangeStart = new EventEmitter();
        this.s_beforeTransitionStart = new EventEmitter();
        this.s_breakpoint = new EventEmitter();
        this.s_changeDirection = new EventEmitter();
        this.s_click = new EventEmitter();
        this.s_doubleTap = new EventEmitter();
        this.s_doubleClick = new EventEmitter();
        this.s_destroy = new EventEmitter();
        this.s_fromEdge = new EventEmitter();
        this.s_hashChange = new EventEmitter();
        this.s_hashSet = new EventEmitter();
        this.s_imagesReady = new EventEmitter();
        this.s_init = new EventEmitter();
        this.s_keyPress = new EventEmitter();
        this.s_lazyImageLoad = new EventEmitter();
        this.s_lazyImageReady = new EventEmitter();
        this.s_loopFix = new EventEmitter();
        this.s_momentumBounce = new EventEmitter();
        this.s_navigationHide = new EventEmitter();
        this.s_navigationShow = new EventEmitter();
        this.s_navigationPrev = new EventEmitter();
        this.s_navigationNext = new EventEmitter();
        this.s_observerUpdate = new EventEmitter();
        this.s_orientationchange = new EventEmitter();
        this.s_paginationHide = new EventEmitter();
        this.s_paginationRender = new EventEmitter();
        this.s_paginationShow = new EventEmitter();
        this.s_paginationUpdate = new EventEmitter();
        this.s_progress = new EventEmitter();
        this.s_reachBeginning = new EventEmitter();
        this.s_reachEnd = new EventEmitter();
        this.s_realIndexChange = new EventEmitter();
        this.s_resize = new EventEmitter();
        this.s_scroll = new EventEmitter();
        this.s_scrollbarDragEnd = new EventEmitter();
        this.s_scrollbarDragMove = new EventEmitter();
        this.s_scrollbarDragStart = new EventEmitter();
        this.s_setTransition = new EventEmitter();
        this.s_setTranslate = new EventEmitter();
        this.s_slideChange = new EventEmitter();
        this.s_slideChangeTransitionEnd = new EventEmitter();
        this.s_slideChangeTransitionStart = new EventEmitter();
        this.s_slideNextTransitionEnd = new EventEmitter();
        this.s_slideNextTransitionStart = new EventEmitter();
        this.s_slidePrevTransitionEnd = new EventEmitter();
        this.s_slidePrevTransitionStart = new EventEmitter();
        this.s_slideResetTransitionStart = new EventEmitter();
        this.s_slideResetTransitionEnd = new EventEmitter();
        this.s_sliderMove = new EventEmitter();
        this.s_sliderFirstMove = new EventEmitter();
        this.s_slidesLengthChange = new EventEmitter();
        this.s_slidesGridLengthChange = new EventEmitter();
        this.s_snapGridLengthChange = new EventEmitter();
        this.s_snapIndexChange = new EventEmitter();
        this.s_tap = new EventEmitter();
        this.s_toEdge = new EventEmitter();
        this.s_touchEnd = new EventEmitter();
        this.s_touchMove = new EventEmitter();
        this.s_touchMoveOpposite = new EventEmitter();
        this.s_touchStart = new EventEmitter();
        this.s_transitionEnd = new EventEmitter();
        this.s_transitionStart = new EventEmitter();
        this.s_update = new EventEmitter();
        this.s_zoomChange = new EventEmitter();
        this.s_swiper = new EventEmitter();
        this.s_lock = new EventEmitter();
        this.s_unlock = new EventEmitter();
        this._activeSlides = new Subject();
        this.containerClasses = 'swiper';
        this.slidesChanges = (val) => {
            this.slides = val.map((slide, index) => {
                slide.slideIndex = index;
                slide.classNames = this.slideClass || '';
                return slide;
            });
            if (this.loop && !this.loopedSlides) {
                this.calcLoopedSlides();
            }
            if (!this.virtual) {
                if (this.loopedSlides) {
                    this.prependSlides = of(this.slides.slice(this.slides.length - this.loopedSlides));
                    this.appendSlides = of(this.slides.slice(0, this.loopedSlides));
                }
            }
            else if (this.swiperRef && this.swiperRef.virtual) {
                this._ngZone.runOutsideAngular(() => {
                    this.swiperRef.virtual.slides = this.slides;
                    this.swiperRef.virtual.update(true);
                });
            }
            this._changeDetectorRef.detectChanges();
        };
        this.style = null;
        this.updateVirtualSlides = (virtualData) => {
            // TODO: type virtualData
            if (!this.swiperRef ||
                (this.currentVirtualData &&
                    this.currentVirtualData.from === virtualData.from &&
                    this.currentVirtualData.to === virtualData.to &&
                    this.currentVirtualData.offset === virtualData.offset)) {
                return;
            }
            this.style = this.swiperRef.isHorizontal()
                ? {
                    [this.swiperRef.rtlTranslate ? 'right' : 'left']: `${virtualData.offset}px`,
                }
                : {
                    top: `${virtualData.offset}px`,
                };
            this.currentVirtualData = virtualData;
            this._activeSlides.next(virtualData.slides);
            this._ngZone.run(() => {
                this._changeDetectorRef.detectChanges();
            });
            this._ngZone.runOutsideAngular(() => {
                this.swiperRef.updateSlides();
                this.swiperRef.updateProgress();
                this.swiperRef.updateSlidesClasses();
                if (isEnabled(this.swiperRef.params.lazy)) {
                    this.swiperRef.lazy.load();
                }
                this.swiperRef.virtual.update(true);
            });
            return;
        };
    }
    set navigation(val) {
        const currentNext = typeof this._navigation !== 'boolean' && this._navigation !== ''
            ? this._navigation?.nextEl
            : null;
        const currentPrev = typeof this._navigation !== 'boolean' && this._navigation !== ''
            ? this._navigation?.prevEl
            : null;
        this._navigation = setProperty(val, {
            nextEl: currentNext || null,
            prevEl: currentPrev || null,
        });
        this.showNavigation = !(coerceBooleanProperty(val) !== true ||
            (this._navigation &&
                typeof this._navigation !== 'boolean' &&
                this._navigation.prevEl !== this._prevElRef?.nativeElement &&
                (this._navigation.prevEl !== null || this._navigation.nextEl !== null) &&
                (typeof this._navigation.nextEl === 'string' ||
                    typeof this._navigation.prevEl === 'string' ||
                    typeof this._navigation.nextEl === 'object' ||
                    typeof this._navigation.prevEl === 'object')));
    }
    get navigation() {
        return this._navigation;
    }
    set pagination(val) {
        const current = typeof this._pagination !== 'boolean' && this._pagination !== ''
            ? this._pagination?.el
            : null;
        this._pagination = setProperty(val, {
            el: current || null,
        });
        this.showPagination = isShowEl(val, this._pagination, this._paginationElRef);
    }
    get pagination() {
        return this._pagination;
    }
    set scrollbar(val) {
        const current = typeof this._scrollbar !== 'boolean' && this._scrollbar !== '' ? this._scrollbar?.el : null;
        this._scrollbar = setProperty(val, {
            el: current || null,
        });
        this.showScrollbar = isShowEl(val, this._scrollbar, this._scrollbarElRef);
    }
    get scrollbar() {
        return this._scrollbar;
    }
    set virtual(val) {
        this._virtual = setProperty(val);
    }
    get virtual() {
        return this._virtual;
    }
    set config(val) {
        this.updateSwiper(val);
        const { params } = getParams(val);
        Object.assign(this, params);
    }
    set prevElRef(el) {
        this._prevElRef = el;
        this._setElement(el, this.navigation, 'navigation', 'prevEl');
    }
    set nextElRef(el) {
        this._nextElRef = el;
        this._setElement(el, this.navigation, 'navigation', 'nextEl');
    }
    set scrollbarElRef(el) {
        this._scrollbarElRef = el;
        this._setElement(el, this.scrollbar, 'scrollbar');
    }
    set paginationElRef(el) {
        this._paginationElRef = el;
        this._setElement(el, this.pagination, 'pagination');
    }
    get activeSlides() {
        if (this.virtual) {
            return this._activeSlides;
        }
        return of(this.slides);
    }
    get zoomContainerClass() {
        return this.zoom && typeof this.zoom !== 'boolean'
            ? this.zoom.containerClass
            : 'swiper-zoom-container';
    }
    _setElement(el, ref, update, key = 'el') {
        if (!ref || !el)
            return;
        if (el.nativeElement) {
            if (ref[key] === el.nativeElement) {
                return;
            }
            ref[key] = el.nativeElement;
        }
        const updateObj = {};
        updateObj[update] = true;
        this.updateInitSwiper(updateObj);
    }
    ngOnInit() {
        const { params } = getParams(this);
        Object.assign(this, params);
    }
    ngAfterViewInit() {
        this.childrenSlidesInit();
        this.initSwiper();
        this._changeDetectorRef.detectChanges();
        setTimeout(() => {
            this.s_swiper.emit(this.swiperRef);
        });
    }
    childrenSlidesInit() {
        this.slidesChanges(this.slidesEl);
        this.slidesEl.changes.subscribe(this.slidesChanges);
    }
    get isSwiperActive() {
        return this.swiperRef && !this.swiperRef.destroyed;
    }
    initSwiper() {
        const { params: swiperParams, passedParams } = getParams(this);
        Object.assign(this, swiperParams);
        this._ngZone.runOutsideAngular(() => {
            swiperParams.init = false;
            if (!swiperParams.virtual) {
                swiperParams.observer = true;
            }
            swiperParams.onAny = (eventName, ...args) => {
                const emitter = this[('s_' + eventName)];
                if (emitter) {
                    emitter.emit([...args]);
                }
            };
            const _slideClasses = (_, updated) => {
                updated.forEach(({ slideEl, classNames }, index) => {
                    const dataIndex = slideEl.getAttribute('data-swiper-slide-index');
                    const slideIndex = dataIndex ? parseInt(dataIndex) : index;
                    if (this.virtual) {
                        const virtualSlide = this.slides.find((item) => {
                            return item.virtualIndex && item.virtualIndex === slideIndex;
                        });
                        if (virtualSlide) {
                            virtualSlide.classNames = classNames;
                            return;
                        }
                    }
                    if (this.slides[slideIndex]) {
                        this.slides[slideIndex].classNames = classNames;
                    }
                });
                this._changeDetectorRef.detectChanges();
            };
            const _containerClasses = (_, classes) => {
                setTimeout(() => {
                    this.containerClasses = classes;
                });
            };
            Object.assign(swiperParams.on, {
                _containerClasses,
                _slideClasses,
            });
            const swiperRef = new Swiper(swiperParams);
            swiperRef.loopCreate = () => { };
            swiperRef.loopDestroy = () => { };
            if (swiperParams.loop) {
                swiperRef.loopedSlides = this.loopedSlides;
            }
            const isVirtualEnabled = isEnabled(swiperRef.params.virtual);
            if (swiperRef.virtual && isVirtualEnabled) {
                swiperRef.virtual.slides = this.slides;
                const extendWith = {
                    cache: false,
                    slides: this.slides,
                    renderExternal: this.updateVirtualSlides,
                    renderExternalUpdate: false,
                };
                extend(swiperRef.params.virtual, extendWith);
                extend(swiperRef.originalParams.virtual, extendWith);
            }
            if (isPlatformBrowser(this._platformId)) {
                this.swiperRef = swiperRef.init(this.elementRef.nativeElement);
                const isVirtualEnabled = isEnabled(this.swiperRef.params.virtual);
                if (this.swiperRef.virtual && isVirtualEnabled) {
                    this.swiperRef.virtual.update(true);
                }
                this._changeDetectorRef.detectChanges();
            }
        });
    }
    ngOnChanges(changedParams) {
        this.updateSwiper(changedParams);
        this._changeDetectorRef.detectChanges();
    }
    updateInitSwiper(changedParams) {
        if (!(changedParams && this.swiperRef && !this.swiperRef.destroyed)) {
            return;
        }
        this._ngZone.runOutsideAngular(() => {
            const { params: currentParams, pagination, navigation, scrollbar, virtual, thumbs, } = this.swiperRef;
            if (changedParams.pagination) {
                if (this.pagination &&
                    typeof this.pagination !== 'boolean' &&
                    this.pagination.el &&
                    pagination &&
                    !pagination.el) {
                    this.updateParameter('pagination', this.pagination);
                    pagination.init();
                    pagination.render();
                    pagination.update();
                }
                else {
                    pagination.destroy();
                    pagination.el = null;
                }
            }
            if (changedParams.scrollbar) {
                if (this.scrollbar &&
                    typeof this.scrollbar !== 'boolean' &&
                    this.scrollbar.el &&
                    scrollbar &&
                    !scrollbar.el) {
                    this.updateParameter('scrollbar', this.scrollbar);
                    scrollbar.init();
                    scrollbar.updateSize();
                    scrollbar.setTranslate();
                }
                else {
                    scrollbar.destroy();
                    scrollbar.el = null;
                }
            }
            if (changedParams.navigation) {
                if (this.navigation &&
                    typeof this.navigation !== 'boolean' &&
                    this.navigation.prevEl &&
                    this.navigation.nextEl &&
                    navigation &&
                    !navigation.prevEl &&
                    !navigation.nextEl) {
                    this.updateParameter('navigation', this.navigation);
                    navigation.init();
                    navigation.update();
                }
                else if (navigation.prevEl && navigation.nextEl) {
                    navigation.destroy();
                    navigation.nextEl = null;
                    navigation.prevEl = null;
                }
            }
            if (changedParams.thumbs && this.thumbs && this.thumbs.swiper) {
                this.updateParameter('thumbs', this.thumbs);
                const initialized = thumbs.init();
                if (initialized)
                    thumbs.update(true);
            }
            if (changedParams.controller && this.controller && this.controller.control) {
                this.swiperRef.controller.control = this.controller.control;
            }
            this.swiperRef.update();
        });
    }
    updateSwiper(changedParams) {
        this._ngZone.runOutsideAngular(() => {
            if (changedParams.config) {
                return;
            }
            if (!(changedParams && this.swiperRef && !this.swiperRef.destroyed)) {
                return;
            }
            for (const key in changedParams) {
                if (ignoreNgOnChanges.indexOf(key) >= 0) {
                    continue;
                }
                const newValue = changedParams[key]?.currentValue ?? changedParams[key];
                this.updateParameter(key, newValue);
            }
            if (changedParams.allowSlideNext) {
                this.swiperRef.allowSlideNext = this.allowSlideNext;
            }
            if (changedParams.allowSlidePrev) {
                this.swiperRef.allowSlidePrev = this.allowSlidePrev;
            }
            if (changedParams.direction) {
                this.swiperRef.changeDirection(this.direction, false);
            }
            if (changedParams.breakpoints) {
                if (this.loop && !this.loopedSlides) {
                    this.calcLoopedSlides();
                }
                this.swiperRef.currentBreakpoint = null;
                this.swiperRef.setBreakpoint();
            }
            if (changedParams.thumbs || changedParams.controller) {
                this.updateInitSwiper(changedParams);
            }
            this.swiperRef.update();
        });
    }
    calcLoopedSlides() {
        if (!this.loop) {
            return false;
        }
        let slidesPerViewParams = this.slidesPerView;
        if (this.breakpoints) {
            const breakpoint = Swiper.prototype.getBreakpoint(this.breakpoints);
            const breakpointOnlyParams = breakpoint in this.breakpoints ? this.breakpoints[breakpoint] : undefined;
            if (breakpointOnlyParams && breakpointOnlyParams.slidesPerView) {
                slidesPerViewParams = breakpointOnlyParams.slidesPerView;
            }
        }
        if (slidesPerViewParams === 'auto') {
            this.loopedSlides = this.slides.length;
            return this.slides.length;
        }
        let loopedSlides = this.loopedSlides || slidesPerViewParams;
        if (!loopedSlides) {
            // ?
            return false;
        }
        if (this.loopAdditionalSlides) {
            loopedSlides += this.loopAdditionalSlides;
        }
        if (loopedSlides > this.slides.length) {
            loopedSlides = this.slides.length;
        }
        this.loopedSlides = loopedSlides;
        return true;
    }
    updateParameter(key, value) {
        if (!(this.swiperRef && !this.swiperRef.destroyed)) {
            return;
        }
        const _key = key.replace(/^_/, '');
        const isCurrentParamObj = isObject(this.swiperRef.params[_key]);
        if (_key === 'enabled') {
            if (value === true) {
                this.swiperRef.enable();
            }
            else if (value === false) {
                this.swiperRef.disable();
            }
            return;
        }
        if (isCurrentParamObj && isObject(value)) {
            extend(this.swiperRef.params[_key], value);
        }
        else {
            this.swiperRef.params[_key] = value;
        }
    }
    ngOnDestroy() {
        this._ngZone.runOutsideAngular(() => {
            this.swiperRef?.destroy(true, false);
        });
    }
}
SwiperComponent.ɵfac = i0.ɵɵngDeclareFactory({ minVersion: "12.0.0", version: "13.3.11", ngImport: i0, type: SwiperComponent, deps: [{ token: i0.NgZone }, { token: i0.ElementRef }, { token: i0.ChangeDetectorRef }, { token: PLATFORM_ID }], target: i0.ɵɵFactoryTarget.Component });
SwiperComponent.ɵcmp = i0.ɵɵngDeclareComponent({ minVersion: "12.0.0", version: "13.3.11", type: SwiperComponent, selector: "swiper, [swiper]", inputs: { enabled: "enabled", on: "on", direction: "direction", touchEventsTarget: "touchEventsTarget", initialSlide: "initialSlide", speed: "speed", cssMode: "cssMode", updateOnWindowResize: "updateOnWindowResize", resizeObserver: "resizeObserver", nested: "nested", focusableElements: "focusableElements", width: "width", height: "height", preventInteractionOnTransition: "preventInteractionOnTransition", userAgent: "userAgent", url: "url", edgeSwipeDetection: "edgeSwipeDetection", edgeSwipeThreshold: "edgeSwipeThreshold", freeMode: "freeMode", autoHeight: "autoHeight", setWrapperSize: "setWrapperSize", virtualTranslate: "virtualTranslate", effect: "effect", breakpoints: "breakpoints", spaceBetween: "spaceBetween", slidesPerView: "slidesPerView", maxBackfaceHiddenSlides: "maxBackfaceHiddenSlides", grid: "grid", slidesPerGroup: "slidesPerGroup", slidesPerGroupSkip: "slidesPerGroupSkip", centeredSlides: "centeredSlides", centeredSlidesBounds: "centeredSlidesBounds", slidesOffsetBefore: "slidesOffsetBefore", slidesOffsetAfter: "slidesOffsetAfter", normalizeSlideIndex: "normalizeSlideIndex", centerInsufficientSlides: "centerInsufficientSlides", watchOverflow: "watchOverflow", roundLengths: "roundLengths", touchRatio: "touchRatio", touchAngle: "touchAngle", simulateTouch: "simulateTouch", shortSwipes: "shortSwipes", longSwipes: "longSwipes", longSwipesRatio: "longSwipesRatio", longSwipesMs: "longSwipesMs", followFinger: "followFinger", allowTouchMove: "allowTouchMove", threshold: "threshold", touchMoveStopPropagation: "touchMoveStopPropagation", touchStartPreventDefault: "touchStartPreventDefault", touchStartForcePreventDefault: "touchStartForcePreventDefault", touchReleaseOnEdges: "touchReleaseOnEdges", uniqueNavElements: "uniqueNavElements", resistance: "resistance", resistanceRatio: "resistanceRatio", watchSlidesProgress: "watchSlidesProgress", grabCursor: "grabCursor", preventClicks: "preventClicks", preventClicksPropagation: "preventClicksPropagation", slideToClickedSlide: "slideToClickedSlide", preloadImages: "preloadImages", updateOnImagesReady: "updateOnImagesReady", loop: "loop", loopAdditionalSlides: "loopAdditionalSlides", loopedSlides: "loopedSlides", loopFillGroupWithBlank: "loopFillGroupWithBlank", loopPreventsSlide: "loopPreventsSlide", rewind: "rewind", allowSlidePrev: "allowSlidePrev", allowSlideNext: "allowSlideNext", swipeHandler: "swipeHandler", noSwiping: "noSwiping", noSwipingClass: "noSwipingClass", noSwipingSelector: "noSwipingSelector", passiveListeners: "passiveListeners", containerModifierClass: "containerModifierClass", slideClass: "slideClass", slideBlankClass: "slideBlankClass", slideActiveClass: "slideActiveClass", slideDuplicateActiveClass: "slideDuplicateActiveClass", slideVisibleClass: "slideVisibleClass", slideDuplicateClass: "slideDuplicateClass", slideNextClass: "slideNextClass", slideDuplicateNextClass: "slideDuplicateNextClass", slidePrevClass: "slidePrevClass", slideDuplicatePrevClass: "slideDuplicatePrevClass", wrapperClass: "wrapperClass", runCallbacksOnInit: "runCallbacksOnInit", observeParents: "observeParents", observeSlideChildren: "observeSlideChildren", a11y: "a11y", autoplay: "autoplay", controller: "controller", coverflowEffect: "coverflowEffect", cubeEffect: "cubeEffect", fadeEffect: "fadeEffect", flipEffect: "flipEffect", creativeEffect: "creativeEffect", cardsEffect: "cardsEffect", hashNavigation: "hashNavigation", history: "history", keyboard: "keyboard", lazy: "lazy", mousewheel: "mousewheel", parallax: "parallax", thumbs: "thumbs", zoom: "zoom", class: "class", id: "id", navigation: "navigation", pagination: "pagination", scrollbar: "scrollbar", virtual: "virtual", config: "config" }, outputs: { s__beforeBreakpoint: "_beforeBreakpoint", s__containerClasses: "_containerClasses", s__slideClass: "_slideClass", s__swiper: "_swiper", s_activeIndexChange: "activeIndexChange", s_afterInit: "afterInit", s_autoplay: "autoplay", s_autoplayStart: "autoplayStart", s_autoplayStop: "autoplayStop", s_autoplayPause: "autoplayPause", s_autoplayResume: "autoplayResume", s_beforeDestroy: "beforeDestroy", s_beforeInit: "beforeInit", s_beforeLoopFix: "beforeLoopFix", s_beforeResize: "beforeResize", s_beforeSlideChangeStart: "beforeSlideChangeStart", s_beforeTransitionStart: "beforeTransitionStart", s_breakpoint: "breakpoint", s_changeDirection: "changeDirection", s_click: "click", s_doubleTap: "doubleTap", s_doubleClick: "doubleClick", s_destroy: "destroy", s_fromEdge: "fromEdge", s_hashChange: "hashChange", s_hashSet: "hashSet", s_imagesReady: "imagesReady", s_init: "init", s_keyPress: "keyPress", s_lazyImageLoad: "lazyImageLoad", s_lazyImageReady: "lazyImageReady", s_loopFix: "loopFix", s_momentumBounce: "momentumBounce", s_navigationHide: "navigationHide", s_navigationShow: "navigationShow", s_navigationPrev: "navigationPrev", s_navigationNext: "navigationNext", s_observerUpdate: "observerUpdate", s_orientationchange: "orientationchange", s_paginationHide: "paginationHide", s_paginationRender: "paginationRender", s_paginationShow: "paginationShow", s_paginationUpdate: "paginationUpdate", s_progress: "progress", s_reachBeginning: "reachBeginning", s_reachEnd: "reachEnd", s_realIndexChange: "realIndexChange", s_resize: "resize", s_scroll: "scroll", s_scrollbarDragEnd: "scrollbarDragEnd", s_scrollbarDragMove: "scrollbarDragMove", s_scrollbarDragStart: "scrollbarDragStart", s_setTransition: "setTransition", s_setTranslate: "setTranslate", s_slideChange: "slideChange", s_slideChangeTransitionEnd: "slideChangeTransitionEnd", s_slideChangeTransitionStart: "slideChangeTransitionStart", s_slideNextTransitionEnd: "slideNextTransitionEnd", s_slideNextTransitionStart: "slideNextTransitionStart", s_slidePrevTransitionEnd: "slidePrevTransitionEnd", s_slidePrevTransitionStart: "slidePrevTransitionStart", s_slideResetTransitionStart: "slideResetTransitionStart", s_slideResetTransitionEnd: "slideResetTransitionEnd", s_sliderMove: "sliderMove", s_sliderFirstMove: "sliderFirstMove", s_slidesLengthChange: "slidesLengthChange", s_slidesGridLengthChange: "slidesGridLengthChange", s_snapGridLengthChange: "snapGridLengthChange", s_snapIndexChange: "snapIndexChange", s_tap: "tap", s_toEdge: "toEdge", s_touchEnd: "touchEnd", s_touchMove: "touchMove", s_touchMoveOpposite: "touchMoveOpposite", s_touchStart: "touchStart", s_transitionEnd: "transitionEnd", s_transitionStart: "transitionStart", s_update: "update", s_zoomChange: "zoomChange", s_swiper: "swiper", s_lock: "lock", s_unlock: "unlock" }, host: { properties: { "class": "this.containerClasses" } }, queries: [{ propertyName: "slidesEl", predicate: SwiperSlideDirective }], viewQueries: [{ propertyName: "prevElRef", first: true, predicate: ["prevElRef"], descendants: true }, { propertyName: "nextElRef", first: true, predicate: ["nextElRef"], descendants: true }, { propertyName: "scrollbarElRef", first: true, predicate: ["scrollbarElRef"], descendants: true }, { propertyName: "paginationElRef", first: true, predicate: ["paginationElRef"], descendants: true }], usesOnChanges: true, ngImport: i0, template: "<ng-content select=\"[slot=container-start]\"></ng-content>\r\n<ng-container *ngIf=\"navigation && showNavigation\">\r\n  <div class=\"swiper-button-prev\" #prevElRef></div>\r\n  <div class=\"swiper-button-next\" #nextElRef></div>\r\n</ng-container>\r\n<div *ngIf=\"scrollbar && showScrollbar\" class=\"swiper-scrollbar\" #scrollbarElRef></div>\r\n<div *ngIf=\"pagination && showPagination\" class=\"swiper-pagination\" #paginationElRef></div>\r\n<div [ngClass]=\"wrapperClass\" [attr.id]=\"id\">\r\n  <ng-content select=\"[slot=wrapper-start]\"></ng-content>\r\n  <ng-template\r\n    *ngTemplateOutlet=\"\r\n      slidesTemplate;\r\n      context: {\r\n        loopSlides: prependSlides,\r\n        key: 'prepend'\r\n      }\r\n    \"\r\n  ></ng-template>\r\n  <ng-template\r\n    *ngTemplateOutlet=\"\r\n      slidesTemplate;\r\n      context: {\r\n        loopSlides: activeSlides,\r\n        key: ''\r\n      }\r\n    \"\r\n  ></ng-template>\r\n  <ng-template\r\n    *ngTemplateOutlet=\"\r\n      slidesTemplate;\r\n      context: {\r\n        loopSlides: appendSlides,\r\n        key: 'append'\r\n      }\r\n    \"\r\n  ></ng-template>\r\n  <ng-content select=\"[slot=wrapper-end]\"></ng-content>\r\n</div>\r\n<ng-content select=\"[slot=container-end]\"></ng-content>\r\n\r\n<ng-template #slidesTemplate let-loopSlides=\"loopSlides\" let-slideKey=\"key\">\r\n  <div\r\n    *ngFor=\"let slide of loopSlides | async\"\r\n    [ngClass]=\"\r\n      (slide.class ? slide.class + ' ' : '') +\r\n      slideClass +\r\n      (slideKey !== '' ? ' ' + slideDuplicateClass : '')\r\n    \"\r\n    [attr.data-swiper-slide-index]=\"slide.virtualIndex ? slide.virtualIndex : slide.slideIndex\"\r\n    [attr.data-swiper-autoplay]=\"slide.autoplayDelay\"\r\n    [style]=\"style\"\r\n    [ngSwitch]=\"slide.zoom\"\r\n  >\r\n    <div *ngSwitchCase=\"true\" [ngClass]=\"zoomContainerClass\">\r\n      <ng-template\r\n        [ngTemplateOutlet]=\"slide.template\"\r\n        [ngTemplateOutletContext]=\"{\r\n          $implicit: slide.slideData\r\n        }\"\r\n      ></ng-template>\r\n    </div>\r\n    <ng-container *ngSwitchDefault>\r\n      <ng-template\r\n        [ngTemplateOutlet]=\"slide.template\"\r\n        [ngTemplateOutletContext]=\"{\r\n          $implicit: slide.slideData\r\n        }\"\r\n      ></ng-template>\r\n    </ng-container>\r\n  </div>\r\n</ng-template>\r\n", styles: ["swiper{display:block}\n"], directives: [{ type: i1.NgIf, selector: "[ngIf]", inputs: ["ngIf", "ngIfThen", "ngIfElse"] }, { type: i1.NgClass, selector: "[ngClass]", inputs: ["class", "ngClass"] }, { type: i1.NgTemplateOutlet, selector: "[ngTemplateOutlet]", inputs: ["ngTemplateOutletContext", "ngTemplateOutlet"] }, { type: i1.NgForOf, selector: "[ngFor][ngForOf]", inputs: ["ngForOf", "ngForTrackBy", "ngForTemplate"] }, { type: i1.NgSwitch, selector: "[ngSwitch]", inputs: ["ngSwitch"] }, { type: i1.NgSwitchCase, selector: "[ngSwitchCase]", inputs: ["ngSwitchCase"] }, { type: i1.NgSwitchDefault, selector: "[ngSwitchDefault]" }], pipes: { "async": i1.AsyncPipe }, changeDetection: i0.ChangeDetectionStrategy.OnPush, encapsulation: i0.ViewEncapsulation.None });
i0.ɵɵngDeclareClassMetadata({ minVersion: "12.0.0", version: "13.3.11", ngImport: i0, type: SwiperComponent, decorators: [{
            type: Component,
            args: [{ selector: 'swiper, [swiper]', changeDetection: ChangeDetectionStrategy.OnPush, encapsulation: ViewEncapsulation.None, styles: [
                        `
      swiper {
        display: block;
      }
    `,
                    ], template: "<ng-content select=\"[slot=container-start]\"></ng-content>\r\n<ng-container *ngIf=\"navigation && showNavigation\">\r\n  <div class=\"swiper-button-prev\" #prevElRef></div>\r\n  <div class=\"swiper-button-next\" #nextElRef></div>\r\n</ng-container>\r\n<div *ngIf=\"scrollbar && showScrollbar\" class=\"swiper-scrollbar\" #scrollbarElRef></div>\r\n<div *ngIf=\"pagination && showPagination\" class=\"swiper-pagination\" #paginationElRef></div>\r\n<div [ngClass]=\"wrapperClass\" [attr.id]=\"id\">\r\n  <ng-content select=\"[slot=wrapper-start]\"></ng-content>\r\n  <ng-template\r\n    *ngTemplateOutlet=\"\r\n      slidesTemplate;\r\n      context: {\r\n        loopSlides: prependSlides,\r\n        key: 'prepend'\r\n      }\r\n    \"\r\n  ></ng-template>\r\n  <ng-template\r\n    *ngTemplateOutlet=\"\r\n      slidesTemplate;\r\n      context: {\r\n        loopSlides: activeSlides,\r\n        key: ''\r\n      }\r\n    \"\r\n  ></ng-template>\r\n  <ng-template\r\n    *ngTemplateOutlet=\"\r\n      slidesTemplate;\r\n      context: {\r\n        loopSlides: appendSlides,\r\n        key: 'append'\r\n      }\r\n    \"\r\n  ></ng-template>\r\n  <ng-content select=\"[slot=wrapper-end]\"></ng-content>\r\n</div>\r\n<ng-content select=\"[slot=container-end]\"></ng-content>\r\n\r\n<ng-template #slidesTemplate let-loopSlides=\"loopSlides\" let-slideKey=\"key\">\r\n  <div\r\n    *ngFor=\"let slide of loopSlides | async\"\r\n    [ngClass]=\"\r\n      (slide.class ? slide.class + ' ' : '') +\r\n      slideClass +\r\n      (slideKey !== '' ? ' ' + slideDuplicateClass : '')\r\n    \"\r\n    [attr.data-swiper-slide-index]=\"slide.virtualIndex ? slide.virtualIndex : slide.slideIndex\"\r\n    [attr.data-swiper-autoplay]=\"slide.autoplayDelay\"\r\n    [style]=\"style\"\r\n    [ngSwitch]=\"slide.zoom\"\r\n  >\r\n    <div *ngSwitchCase=\"true\" [ngClass]=\"zoomContainerClass\">\r\n      <ng-template\r\n        [ngTemplateOutlet]=\"slide.template\"\r\n        [ngTemplateOutletContext]=\"{\r\n          $implicit: slide.slideData\r\n        }\"\r\n      ></ng-template>\r\n    </div>\r\n    <ng-container *ngSwitchDefault>\r\n      <ng-template\r\n        [ngTemplateOutlet]=\"slide.template\"\r\n        [ngTemplateOutletContext]=\"{\r\n          $implicit: slide.slideData\r\n        }\"\r\n      ></ng-template>\r\n    </ng-container>\r\n  </div>\r\n</ng-template>\r\n" }]
        }], ctorParameters: function () { return [{ type: i0.NgZone }, { type: i0.ElementRef }, { type: i0.ChangeDetectorRef }, { type: Object, decorators: [{
                    type: Inject,
                    args: [PLATFORM_ID]
                }] }]; }, propDecorators: { enabled: [{
                type: Input
            }], on: [{
                type: Input
            }], direction: [{
                type: Input
            }], touchEventsTarget: [{
                type: Input
            }], initialSlide: [{
                type: Input
            }], speed: [{
                type: Input
            }], cssMode: [{
                type: Input
            }], updateOnWindowResize: [{
                type: Input
            }], resizeObserver: [{
                type: Input
            }], nested: [{
                type: Input
            }], focusableElements: [{
                type: Input
            }], width: [{
                type: Input
            }], height: [{
                type: Input
            }], preventInteractionOnTransition: [{
                type: Input
            }], userAgent: [{
                type: Input
            }], url: [{
                type: Input
            }], edgeSwipeDetection: [{
                type: Input
            }], edgeSwipeThreshold: [{
                type: Input
            }], freeMode: [{
                type: Input
            }], autoHeight: [{
                type: Input
            }], setWrapperSize: [{
                type: Input
            }], virtualTranslate: [{
                type: Input
            }], effect: [{
                type: Input
            }], breakpoints: [{
                type: Input
            }], spaceBetween: [{
                type: Input
            }], slidesPerView: [{
                type: Input
            }], maxBackfaceHiddenSlides: [{
                type: Input
            }], grid: [{
                type: Input
            }], slidesPerGroup: [{
                type: Input
            }], slidesPerGroupSkip: [{
                type: Input
            }], centeredSlides: [{
                type: Input
            }], centeredSlidesBounds: [{
                type: Input
            }], slidesOffsetBefore: [{
                type: Input
            }], slidesOffsetAfter: [{
                type: Input
            }], normalizeSlideIndex: [{
                type: Input
            }], centerInsufficientSlides: [{
                type: Input
            }], watchOverflow: [{
                type: Input
            }], roundLengths: [{
                type: Input
            }], touchRatio: [{
                type: Input
            }], touchAngle: [{
                type: Input
            }], simulateTouch: [{
                type: Input
            }], shortSwipes: [{
                type: Input
            }], longSwipes: [{
                type: Input
            }], longSwipesRatio: [{
                type: Input
            }], longSwipesMs: [{
                type: Input
            }], followFinger: [{
                type: Input
            }], allowTouchMove: [{
                type: Input
            }], threshold: [{
                type: Input
            }], touchMoveStopPropagation: [{
                type: Input
            }], touchStartPreventDefault: [{
                type: Input
            }], touchStartForcePreventDefault: [{
                type: Input
            }], touchReleaseOnEdges: [{
                type: Input
            }], uniqueNavElements: [{
                type: Input
            }], resistance: [{
                type: Input
            }], resistanceRatio: [{
                type: Input
            }], watchSlidesProgress: [{
                type: Input
            }], grabCursor: [{
                type: Input
            }], preventClicks: [{
                type: Input
            }], preventClicksPropagation: [{
                type: Input
            }], slideToClickedSlide: [{
                type: Input
            }], preloadImages: [{
                type: Input
            }], updateOnImagesReady: [{
                type: Input
            }], loop: [{
                type: Input
            }], loopAdditionalSlides: [{
                type: Input
            }], loopedSlides: [{
                type: Input
            }], loopFillGroupWithBlank: [{
                type: Input
            }], loopPreventsSlide: [{
                type: Input
            }], rewind: [{
                type: Input
            }], allowSlidePrev: [{
                type: Input
            }], allowSlideNext: [{
                type: Input
            }], swipeHandler: [{
                type: Input
            }], noSwiping: [{
                type: Input
            }], noSwipingClass: [{
                type: Input
            }], noSwipingSelector: [{
                type: Input
            }], passiveListeners: [{
                type: Input
            }], containerModifierClass: [{
                type: Input
            }], slideClass: [{
                type: Input
            }], slideBlankClass: [{
                type: Input
            }], slideActiveClass: [{
                type: Input
            }], slideDuplicateActiveClass: [{
                type: Input
            }], slideVisibleClass: [{
                type: Input
            }], slideDuplicateClass: [{
                type: Input
            }], slideNextClass: [{
                type: Input
            }], slideDuplicateNextClass: [{
                type: Input
            }], slidePrevClass: [{
                type: Input
            }], slideDuplicatePrevClass: [{
                type: Input
            }], wrapperClass: [{
                type: Input
            }], runCallbacksOnInit: [{
                type: Input
            }], observeParents: [{
                type: Input
            }], observeSlideChildren: [{
                type: Input
            }], a11y: [{
                type: Input
            }], autoplay: [{
                type: Input
            }], controller: [{
                type: Input
            }], coverflowEffect: [{
                type: Input
            }], cubeEffect: [{
                type: Input
            }], fadeEffect: [{
                type: Input
            }], flipEffect: [{
                type: Input
            }], creativeEffect: [{
                type: Input
            }], cardsEffect: [{
                type: Input
            }], hashNavigation: [{
                type: Input
            }], history: [{
                type: Input
            }], keyboard: [{
                type: Input
            }], lazy: [{
                type: Input
            }], mousewheel: [{
                type: Input
            }], parallax: [{
                type: Input
            }], thumbs: [{
                type: Input
            }], zoom: [{
                type: Input
            }], class: [{
                type: Input
            }], id: [{
                type: Input
            }], navigation: [{
                type: Input
            }], pagination: [{
                type: Input
            }], scrollbar: [{
                type: Input
            }], virtual: [{
                type: Input
            }], config: [{
                type: Input
            }], s__beforeBreakpoint: [{
                type: Output,
                args: ['_beforeBreakpoint']
            }], s__containerClasses: [{
                type: Output,
                args: ['_containerClasses']
            }], s__slideClass: [{
                type: Output,
                args: ['_slideClass']
            }], s__swiper: [{
                type: Output,
                args: ['_swiper']
            }], s_activeIndexChange: [{
                type: Output,
                args: ['activeIndexChange']
            }], s_afterInit: [{
                type: Output,
                args: ['afterInit']
            }], s_autoplay: [{
                type: Output,
                args: ['autoplay']
            }], s_autoplayStart: [{
                type: Output,
                args: ['autoplayStart']
            }], s_autoplayStop: [{
                type: Output,
                args: ['autoplayStop']
            }], s_autoplayPause: [{
                type: Output,
                args: ['autoplayPause']
            }], s_autoplayResume: [{
                type: Output,
                args: ['autoplayResume']
            }], s_beforeDestroy: [{
                type: Output,
                args: ['beforeDestroy']
            }], s_beforeInit: [{
                type: Output,
                args: ['beforeInit']
            }], s_beforeLoopFix: [{
                type: Output,
                args: ['beforeLoopFix']
            }], s_beforeResize: [{
                type: Output,
                args: ['beforeResize']
            }], s_beforeSlideChangeStart: [{
                type: Output,
                args: ['beforeSlideChangeStart']
            }], s_beforeTransitionStart: [{
                type: Output,
                args: ['beforeTransitionStart']
            }], s_breakpoint: [{
                type: Output,
                args: ['breakpoint']
            }], s_changeDirection: [{
                type: Output,
                args: ['changeDirection']
            }], s_click: [{
                type: Output,
                args: ['click']
            }], s_doubleTap: [{
                type: Output,
                args: ['doubleTap']
            }], s_doubleClick: [{
                type: Output,
                args: ['doubleClick']
            }], s_destroy: [{
                type: Output,
                args: ['destroy']
            }], s_fromEdge: [{
                type: Output,
                args: ['fromEdge']
            }], s_hashChange: [{
                type: Output,
                args: ['hashChange']
            }], s_hashSet: [{
                type: Output,
                args: ['hashSet']
            }], s_imagesReady: [{
                type: Output,
                args: ['imagesReady']
            }], s_init: [{
                type: Output,
                args: ['init']
            }], s_keyPress: [{
                type: Output,
                args: ['keyPress']
            }], s_lazyImageLoad: [{
                type: Output,
                args: ['lazyImageLoad']
            }], s_lazyImageReady: [{
                type: Output,
                args: ['lazyImageReady']
            }], s_loopFix: [{
                type: Output,
                args: ['loopFix']
            }], s_momentumBounce: [{
                type: Output,
                args: ['momentumBounce']
            }], s_navigationHide: [{
                type: Output,
                args: ['navigationHide']
            }], s_navigationShow: [{
                type: Output,
                args: ['navigationShow']
            }], s_navigationPrev: [{
                type: Output,
                args: ['navigationPrev']
            }], s_navigationNext: [{
                type: Output,
                args: ['navigationNext']
            }], s_observerUpdate: [{
                type: Output,
                args: ['observerUpdate']
            }], s_orientationchange: [{
                type: Output,
                args: ['orientationchange']
            }], s_paginationHide: [{
                type: Output,
                args: ['paginationHide']
            }], s_paginationRender: [{
                type: Output,
                args: ['paginationRender']
            }], s_paginationShow: [{
                type: Output,
                args: ['paginationShow']
            }], s_paginationUpdate: [{
                type: Output,
                args: ['paginationUpdate']
            }], s_progress: [{
                type: Output,
                args: ['progress']
            }], s_reachBeginning: [{
                type: Output,
                args: ['reachBeginning']
            }], s_reachEnd: [{
                type: Output,
                args: ['reachEnd']
            }], s_realIndexChange: [{
                type: Output,
                args: ['realIndexChange']
            }], s_resize: [{
                type: Output,
                args: ['resize']
            }], s_scroll: [{
                type: Output,
                args: ['scroll']
            }], s_scrollbarDragEnd: [{
                type: Output,
                args: ['scrollbarDragEnd']
            }], s_scrollbarDragMove: [{
                type: Output,
                args: ['scrollbarDragMove']
            }], s_scrollbarDragStart: [{
                type: Output,
                args: ['scrollbarDragStart']
            }], s_setTransition: [{
                type: Output,
                args: ['setTransition']
            }], s_setTranslate: [{
                type: Output,
                args: ['setTranslate']
            }], s_slideChange: [{
                type: Output,
                args: ['slideChange']
            }], s_slideChangeTransitionEnd: [{
                type: Output,
                args: ['slideChangeTransitionEnd']
            }], s_slideChangeTransitionStart: [{
                type: Output,
                args: ['slideChangeTransitionStart']
            }], s_slideNextTransitionEnd: [{
                type: Output,
                args: ['slideNextTransitionEnd']
            }], s_slideNextTransitionStart: [{
                type: Output,
                args: ['slideNextTransitionStart']
            }], s_slidePrevTransitionEnd: [{
                type: Output,
                args: ['slidePrevTransitionEnd']
            }], s_slidePrevTransitionStart: [{
                type: Output,
                args: ['slidePrevTransitionStart']
            }], s_slideResetTransitionStart: [{
                type: Output,
                args: ['slideResetTransitionStart']
            }], s_slideResetTransitionEnd: [{
                type: Output,
                args: ['slideResetTransitionEnd']
            }], s_sliderMove: [{
                type: Output,
                args: ['sliderMove']
            }], s_sliderFirstMove: [{
                type: Output,
                args: ['sliderFirstMove']
            }], s_slidesLengthChange: [{
                type: Output,
                args: ['slidesLengthChange']
            }], s_slidesGridLengthChange: [{
                type: Output,
                args: ['slidesGridLengthChange']
            }], s_snapGridLengthChange: [{
                type: Output,
                args: ['snapGridLengthChange']
            }], s_snapIndexChange: [{
                type: Output,
                args: ['snapIndexChange']
            }], s_tap: [{
                type: Output,
                args: ['tap']
            }], s_toEdge: [{
                type: Output,
                args: ['toEdge']
            }], s_touchEnd: [{
                type: Output,
                args: ['touchEnd']
            }], s_touchMove: [{
                type: Output,
                args: ['touchMove']
            }], s_touchMoveOpposite: [{
                type: Output,
                args: ['touchMoveOpposite']
            }], s_touchStart: [{
                type: Output,
                args: ['touchStart']
            }], s_transitionEnd: [{
                type: Output,
                args: ['transitionEnd']
            }], s_transitionStart: [{
                type: Output,
                args: ['transitionStart']
            }], s_update: [{
                type: Output,
                args: ['update']
            }], s_zoomChange: [{
                type: Output,
                args: ['zoomChange']
            }], s_swiper: [{
                type: Output,
                args: ['swiper']
            }], s_lock: [{
                type: Output,
                args: ['lock']
            }], s_unlock: [{
                type: Output,
                args: ['unlock']
            }], prevElRef: [{
                type: ViewChild,
                args: ['prevElRef', { static: false }]
            }], nextElRef: [{
                type: ViewChild,
                args: ['nextElRef', { static: false }]
            }], scrollbarElRef: [{
                type: ViewChild,
                args: ['scrollbarElRef', { static: false }]
            }], paginationElRef: [{
                type: ViewChild,
                args: ['paginationElRef', { static: false }]
            }], slidesEl: [{
                type: ContentChildren,
                args: [SwiperSlideDirective, { descendants: false, emitDistinctChangesOnly: true }]
            }], containerClasses: [{
                type: HostBinding,
                args: ['class']
            }] } });
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoic3dpcGVyLmNvbXBvbmVudC5qcyIsInNvdXJjZVJvb3QiOiIiLCJzb3VyY2VzIjpbIi4uLy4uLy4uLy4uLy4uL3NyYy9hbmd1bGFyL3NyYy9zd2lwZXIuY29tcG9uZW50LnRzIiwiLi4vLi4vLi4vLi4vLi4vc3JjL2FuZ3VsYXIvc3JjL3N3aXBlci5jb21wb25lbnQuaHRtbCJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiQUFBQSxPQUFPLEVBQ0wsdUJBQXVCLEVBRXZCLFNBQVMsRUFDVCxlQUFlLEVBRWYsWUFBWSxFQUNaLFdBQVcsRUFDWCxNQUFNLEVBQ04sS0FBSyxFQUdMLE1BQU0sRUFDTixXQUFXLEVBR1gsU0FBUyxFQUNULGlCQUFpQixHQUNsQixNQUFNLGVBQWUsQ0FBQztBQUN2QixhQUFhO0FBQ2IsT0FBTyxNQUFNLE1BQU0sUUFBUSxDQUFDO0FBQzVCLE9BQU8sRUFBYyxFQUFFLEVBQUUsT0FBTyxFQUFFLE1BQU0sTUFBTSxDQUFDO0FBQy9DLE9BQU8sRUFBRSxTQUFTLEVBQUUsTUFBTSxvQkFBb0IsQ0FBQztBQUMvQyxPQUFPLEVBQUUsb0JBQW9CLEVBQUUsTUFBTSwwQkFBMEIsQ0FBQztBQUVoRSxPQUFPLEVBQ0wsTUFBTSxFQUNOLFFBQVEsRUFDUixXQUFXLEVBQ1gsaUJBQWlCLEVBQ2pCLHFCQUFxQixFQUNyQixRQUFRLEVBQ1IsU0FBUyxHQUNWLE1BQU0sZUFBZSxDQUFDO0FBU3ZCLE9BQU8sRUFBRSxpQkFBaUIsRUFBRSxNQUFNLGlCQUFpQixDQUFDOzs7QUFjcEQsTUFBTSxPQUFPLGVBQWU7SUF5YzFCLFlBQ1UsT0FBZSxFQUNmLFVBQXNCLEVBQ3RCLGtCQUFxQyxFQUNoQixXQUFtQjtRQUh4QyxZQUFPLEdBQVAsT0FBTyxDQUFRO1FBQ2YsZUFBVSxHQUFWLFVBQVUsQ0FBWTtRQUN0Qix1QkFBa0IsR0FBbEIsa0JBQWtCLENBQW1CO1FBQ2hCLGdCQUFXLEdBQVgsV0FBVyxDQUFRO1FBaFl6QyxlQUFVLEdBQWdDLGNBQWMsQ0FBQztRQVV6RCxp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O3VCQUFDLGlCQUFpQixFQUFFLEVBQUUsTUFBTSxFQUFFLEtBQUssRUFBRTtnQkFPL0MsUUFBUTtzQkFEUCxlQUFlO3VCQUFDLG9CQUFvQixFQUFFLEVBQUUsV0FBVyxFQUFFLEtBQUssRUFBRSx1QkFBdUIsRUFBRSxJQUFJLEVBQUU7Z0JBdUJ0RSxnQkFBZ0I7c0JBQXJDLFdBQVc7dUJBQUMsT0FBTyIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCB7XHJcbiAgQ2hhbmdlRGV0ZWN0aW9uU3RyYXRlZ3ksXHJcbiAgQ2hhbmdlRGV0ZWN0b3JSZWYsXHJcbiAgQ29tcG9uZW50LFxyXG4gIENvbnRlbnRDaGlsZHJlbixcclxuICBFbGVtZW50UmVmLFxyXG4gIEV2ZW50RW1pdHRlcixcclxuICBIb3N0QmluZGluZyxcclxuICBJbmplY3QsXHJcbiAgSW5wdXQsXHJcbiAgTmdab25lLFxyXG4gIE9uSW5pdCxcclxuICBPdXRwdXQsXHJcbiAgUExBVEZPUk1fSUQsXHJcbiAgUXVlcnlMaXN0LFxyXG4gIFNpbXBsZUNoYW5nZXMsXHJcbiAgVmlld0NoaWxkLFxyXG4gIFZpZXdFbmNhcHN1bGF0aW9uLFxyXG59IGZyb20gJ0Bhbmd1bGFyL2NvcmUnO1xyXG4vLyBAdHMtaWdub3JlXHJcbmltcG9ydCBTd2lwZXIgZnJvbSAnc3dpcGVyJztcclxuaW1wb3J0IHsgT2JzZXJ2YWJsZSwgb2YsIFN1YmplY3QgfSBmcm9tICdyeGpzJztcclxuaW1wb3J0IHsgZ2V0UGFyYW1zIH0gZnJvbSAnLi91dGlscy9nZXQtcGFyYW1zJztcclxuaW1wb3J0IHsgU3dpcGVyU2xpZGVEaXJlY3RpdmUgfSBmcm9tICcuL3N3aXBlci1zbGlkZS5kaXJlY3RpdmUnO1xyXG5pbXBvcnQgeyBFdmVudHNQYXJhbXMgfSBmcm9tICcuL3N3aXBlci1ldmVudHMnO1xyXG5pbXBvcnQge1xyXG4gIGV4dGVuZCxcclxuICBpc09iamVjdCxcclxuICBzZXRQcm9wZXJ0eSxcclxuICBpZ25vcmVOZ09uQ2hhbmdlcyxcclxuICBjb2VyY2VCb29sZWFuUHJvcGVydHksXHJcbiAgaXNTaG93RWwsXHJcbiAgaXNFbmFibGVkLFxyXG59IGZyb20gJy4vdXRpbHMvdXRpbHMnO1xyXG5pbXBvcnQge1xyXG4gIFN3aXBlck9wdGlvbnMsXHJcbiAgU3dpcGVyRXZlbnRzLFxyXG4gIE5hdmlnYXRpb25PcHRpb25zLFxyXG4gIFBhZ2luYXRpb25PcHRpb25zLFxyXG4gIFNjcm9sbGJhck9wdGlvbnMsXHJcbiAgVmlydHVhbE9wdGlvbnMsXHJcbn0gZnJvbSAnc3dpcGVyL3R5cGVzJztcclxuaW1wb3J0IHsgaXNQbGF0Zm9ybUJyb3dzZXIgfSBmcm9tICdAYW5ndWxhci9jb21tb24nO1xyXG5AQ29tcG9uZW50KHtcclxuICBzZWxlY3RvcjogJ3N3aXBlciwgW3N3aXBlcl0nLFxyXG4gIHRlbXBsYXRlVXJsOiAnLi9zd2lwZXIuY29tcG9uZW50Lmh0bWwnLFxyXG4gIGNoYW5nZURldGVjdGlvbjogQ2hhbmdlRGV0ZWN0aW9uU3RyYXRlZ3kuT25QdXNoLFxyXG4gIGVuY2Fwc3VsYXRpb246IFZpZXdFbmNhcHN1bGF0aW9uLk5vbmUsXHJcbiAgc3R5bGVzOiBbXHJcbiAgICBgXHJcbiAgICAgIHN3aXBlciB7XHJcbiAgICAgICAgZGlzcGxheTogYmxvY2s7XHJcbiAgICAgIH1cclxuICAgIGAsXHJcbiAgXSxcclxufSlcclxuZXhwb3J0IGNsYXNzIFN3aXBlckNvbXBvbmVudCBpbXBsZW1lbnRzIE9uSW5pdCB7XHJcbiAgQElucHV0KCkgZW5hYmxlZDogU3dpcGVyT3B0aW9uc1snZW5hYmxlZCddO1xyXG4gIEBJbnB1dCgpIG9uOiBTd2lwZXJPcHRpb25zWydvbiddO1xyXG4gIEBJbnB1dCgpIGRpcmVjdGlvbjogU3dpcGVyT3B0aW9uc1snZGlyZWN0aW9uJ107XHJcbiAgQElucHV0KCkgdG91Y2hFdmVudHNUYXJnZXQ6IFN3aXBlck9wdGlvbnNbJ3RvdWNoRXZlbnRzVGFyZ2V0J107XHJcbiAgQElucHV0KCkgaW5pdGlhbFNsaWRlOiBTd2lwZXJPcHRpb25zWydpbml0aWFsU2xpZGUnXTtcclxuICBASW5wdXQoKSBzcGVlZDogU3dpcGVyT3B0aW9uc1snc3BlZWQnXTtcclxuICBASW5wdXQoKSBjc3NNb2RlOiBTd2lwZXJPcHRpb25zWydjc3NNb2RlJ107XHJcbiAgQElucHV0KCkgdXBkYXRlT25XaW5kb3dSZXNpemU6IFN3aXBlck9wdGlvbnNbJ3VwZGF0ZU9uV2luZG93UmVzaXplJ107XHJcbiAgQElucHV0KCkgcmVzaXplT2JzZXJ2ZXI6IFN3aXBlck9wdGlvbnNbJ3Jlc2l6ZU9ic2VydmVyJ107XHJcbiAgQElucHV0KCkgbmVzdGVkOiBTd2lwZXJPcHRpb25zWyduZXN0ZWQnXTtcclxuICBASW5wdXQoKSBmb2N1c2FibGVFbGVtZW50czogU3dpcGVyT3B0aW9uc1snZm9jdXNhYmxlRWxlbWVudHMnXTtcclxuICBASW5wdXQoKSB3aWR0aDogU3dpcGVyT3B0aW9uc1snd2lkdGgnXTtcclxuICBASW5wdXQoKSBoZWlnaHQ6IFN3aXBlck9wdGlvbnNbJ2hlaWdodCddO1xyXG4gIEBJbnB1dCgpIHByZXZlbnRJbnRlcmFjdGlvbk9uVHJhbnNpdGlvbjogU3dpcGVyT3B0aW9uc1sncHJldmVudEludGVyYWN0aW9uT25UcmFuc2l0aW9uJ107XHJcbiAgQElucHV0KCkgdXNlckFnZW50OiBTd2lwZXJPcHRpb25zWyd1c2VyQWdlbnQnXTtcclxuICBASW5wdXQoKSB1cmw6IFN3aXBlck9wdGlvbnNbJ3VybCddO1xyXG4gIEBJbnB1dCgpIGVkZ2VTd2lwZURldGVjdGlvbjogYm9vbGVhbiB8IHN0cmluZztcclxuICBASW5wdXQoKSBlZGdlU3dpcGVUaHJlc2hvbGQ6IG51bWJlcjtcclxuICBASW5wdXQoKSBmcmVlTW9kZTogU3dpcGVyT3B0aW9uc1snZnJlZU1vZGUnXTtcclxuICBASW5wdXQoKSBhdXRvSGVpZ2h0OiBTd2lwZXJPcHRpb25zWydhdXRvSGVpZ2h0J107XHJcbiAgQElucHV0KCkgc2V0V3JhcHBlclNpemU6IFN3aXBlck9wdGlvbnNbJ3NldFdyYXBwZXJTaXplJ107XHJcbiAgQElucHV0KCkgdmlydHVhbFRyYW5zbGF0ZTogU3dpcGVyT3B0aW9uc1sndmlydHVhbFRyYW5zbGF0ZSddO1xyXG4gIEBJbnB1dCgpIGVmZmVjdDogU3dpcGVyT3B0aW9uc1snZWZmZWN0J107XHJcbiAgQElucHV0KCkgYnJlYWtwb2ludHM6IFN3aXBlck9wdGlvbnNbJ2JyZWFrcG9pbnRzJ107XHJcbiAgQElucHV0KCkgc3BhY2VCZXR3ZWVuOiBTd2lwZXJPcHRpb25zWydzcGFjZUJldHdlZW4nXTtcclxuICBASW5wdXQoKSBzbGlkZXNQZXJWaWV3OiBTd2lwZXJPcHRpb25zWydzbGlkZXNQZXJWaWV3J107XHJcbiAgQElucHV0KCkgbWF4QmFja2ZhY2VIaWRkZW5TbGlkZXM6IFN3aXBlck9wdGlvbnNbJ21heEJhY2tmYWNlSGlkZGVuU2xpZGVzJ107XHJcbiAgQElucHV0KCkgZ3JpZDogU3dpcGVyT3B0aW9uc1snZ3JpZCddO1xyXG4gIEBJbnB1dCgpIHNsaWRlc1Blckdyb3VwOiBTd2lwZXJPcHRpb25zWydzbGlkZXNQZXJHcm91cCddO1xyXG4gIEBJbnB1dCgpIHNsaWRlc1Blckdyb3VwU2tpcDogU3dpcGVyT3B0aW9uc1snc2xpZGVzUGVyR3JvdXBTa2lwJ107XHJcbiAgQElucHV0KCkgY2VudGVyZWRTbGlkZXM6IFN3aXBlck9wdGlvbnNbJ2NlbnRlcmVkU2xpZGVzJ107XHJcbiAgQElucHV0KCkgY2VudGVyZWRTbGlkZXNCb3VuZHM6IFN3aXBlck9wdGlvbnNbJ2NlbnRlcmVkU2xpZGVzQm91bmRzJ107XHJcbiAgQElucHV0KCkgc2xpZGVzT2Zmc2V0QmVmb3JlOiBTd2lwZXJPcHRpb25zWydzbGlkZXNPZmZzZXRCZWZvcmUnXTtcclxuICBASW5wdXQoKSBzbGlkZXNPZmZzZXRBZnRlcjogU3dpcGVyT3B0aW9uc1snc2xpZGVzT2Zmc2V0QWZ0ZXInXTtcclxuICBASW5wdXQoKSBub3JtYWxpemVTbGlkZUluZGV4OiBTd2lwZXJPcHRpb25zWydub3JtYWxpemVTbGlkZUluZGV4J107XHJcbiAgQElucHV0KCkgY2VudGVySW5zdWZmaWNpZW50U2xpZGVzOiBTd2lwZXJPcHRpb25zWydjZW50ZXJJbnN1ZmZpY2llbnRTbGlkZXMnXTtcclxuICBASW5wdXQoKSB3YXRjaE92ZXJmbG93OiBTd2lwZXJPcHRpb25zWyd3YXRjaE92ZXJmbG93J107XHJcbiAgQElucHV0KCkgcm91bmRMZW5ndGhzOiBTd2lwZXJPcHRpb25zWydyb3VuZExlbmd0aHMnXTtcclxuICBASW5wdXQoKSB0b3VjaFJhdGlvOiBTd2lwZXJPcHRpb25zWyd0b3VjaFJhdGlvJ107XHJcbiAgQElucHV0KCkgdG91Y2hBbmdsZTogU3dpcGVyT3B0aW9uc1sndG91Y2hBbmdsZSddO1xyXG4gIEBJbnB1dCgpIHNpbXVsYXRlVG91Y2g6IFN3aXBlck9wdGlvbnNbJ3NpbXVsYXRlVG91Y2gnXTtcclxuICBASW5wdXQoKSBzaG9ydFN3aXBlczogU3dpcGVyT3B0aW9uc1snc2hvcnRTd2lwZXMnXTtcclxuICBASW5wdXQoKSBsb25nU3dpcGVzOiBTd2lwZXJPcHRpb25zWydsb25nU3dpcGVzJ107XHJcbiAgQElucHV0KCkgbG9uZ1N3aXBlc1JhdGlvOiBTd2lwZXJPcHRpb25zWydsb25nU3dpcGVzUmF0aW8nXTtcclxuICBASW5wdXQoKSBsb25nU3dpcGVzTXM6IFN3aXBlck9wdGlvbnNbJ2xvbmdTd2lwZXNNcyddO1xyXG4gIEBJbnB1dCgpIGZvbGxvd0ZpbmdlcjogU3dpcGVyT3B0aW9uc1snZm9sbG93RmluZ2VyJ107XHJcbiAgQElucHV0KCkgYWxsb3dUb3VjaE1vdmU6IFN3aXBlck9wdGlvbnNbJ2FsbG93VG91Y2hNb3ZlJ107XHJcbiAgQElucHV0KCkgdGhyZXNob2xkOiBTd2lwZXJPcHRpb25zWyd0aHJlc2hvbGQnXTtcclxuICBASW5wdXQoKSB0b3VjaE1vdmVTdG9wUHJvcGFnYXRpb246IFN3aXBlck9wdGlvbnNbJ3RvdWNoTW92ZVN0b3BQcm9wYWdhdGlvbiddO1xyXG4gIEBJbnB1dCgpIHRvdWNoU3RhcnRQcmV2ZW50RGVmYXVsdDogU3dpcGVyT3B0aW9uc1sndG91Y2hTdGFydFByZXZlbnREZWZhdWx0J107XHJcbiAgQElucHV0KCkgdG91Y2hTdGFydEZvcmNlUHJldmVudERlZmF1bHQ6IFN3aXBlck9wdGlvbnNbJ3RvdWNoU3RhcnRGb3JjZVByZXZlbnREZWZhdWx0J107XHJcbiAgQElucHV0KCkgdG91Y2hSZWxlYXNlT25FZGdlczogU3dpcGVyT3B0aW9uc1sndG91Y2hSZWxlYXNlT25FZGdlcyddO1xyXG4gIEBJbnB1dCgpIHVuaXF1ZU5hdkVsZW1lbnRzOiBTd2lwZXJPcHRpb25zWyd1bmlxdWVOYXZFbGVtZW50cyddO1xyXG4gIEBJbnB1dCgpIHJlc2lzdGFuY2U6IFN3aXBlck9wdGlvbnNbJ3Jlc2lzdGFuY2UnXTtcclxuICBASW5wdXQoKSByZXNpc3RhbmNlUmF0aW86IFN3aXBlck9wdGlvbnNbJ3Jlc2lzdGFuY2VSYXRpbyddO1xyXG4gIEBJbnB1dCgpIHdhdGNoU2xpZGVzUHJvZ3Jlc3M6IFN3aXBlck9wdGlvbnNbJ3dhdGNoU2xpZGVzUHJvZ3Jlc3MnXTtcclxuICBASW5wdXQoKSBncmFiQ3Vyc29yOiBTd2lwZXJPcHRpb25zWydncmFiQ3Vyc29yJ107XHJcbiAgQElucHV0KCkgcHJldmVudENsaWNrczogU3dpcGVyT3B0aW9uc1sncHJldmVudENsaWNrcyddO1xyXG4gIEBJbnB1dCgpIHByZXZlbnRDbGlja3NQcm9wYWdhdGlvbjogU3dpcGVyT3B0aW9uc1sncHJldmVudENsaWNrc1Byb3BhZ2F0aW9uJ107XHJcbiAgQElucHV0KCkgc2xpZGVUb0NsaWNrZWRTbGlkZTogU3dpcGVyT3B0aW9uc1snc2xpZGVUb0NsaWNrZWRTbGlkZSddO1xyXG4gIEBJbnB1dCgpIHByZWxvYWRJbWFnZXM6IFN3aXBlck9wdGlvbnNbJ3ByZWxvYWRJbWFnZXMnXTtcclxuICBASW5wdXQoKSB1cGRhdGVPbkltYWdlc1JlYWR5OiBTd2lwZXJPcHRpb25zWyd1cGRhdGVPbkltYWdlc1JlYWR5J107XHJcbiAgQElucHV0KCkgbG9vcDogU3dpcGVyT3B0aW9uc1snbG9vcCddO1xyXG4gIEBJbnB1dCgpIGxvb3BBZGRpdGlvbmFsU2xpZGVzOiBTd2lwZXJPcHRpb25zWydsb29wQWRkaXRpb25hbFNsaWRlcyddO1xyXG4gIEBJbnB1dCgpIGxvb3BlZFNsaWRlczogU3dpcGVyT3B0aW9uc1snbG9vcGVkU2xpZGVzJ107XHJcbiAgQElucHV0KCkgbG9vcEZpbGxHcm91cFdpdGhCbGFuazogU3dpcGVyT3B0aW9uc1snbG9vcEZpbGxHcm91cFdpdGhCbGFuayddO1xyXG4gIEBJbnB1dCgpIGxvb3BQcmV2ZW50c1NsaWRlOiBTd2lwZXJPcHRpb25zWydsb29wUHJldmVudHNTbGlkZSddO1xyXG4gIEBJbnB1dCgpIHJld2luZDogU3dpcGVyT3B0aW9uc1sncmV3aW5kJ107XHJcbiAgQElucHV0KCkgYWxsb3dTbGlkZVByZXY6IFN3aXBlck9wdGlvbnNbJ2FsbG93U2xpZGVQcmV2J107XHJcbiAgQElucHV0KCkgYWxsb3dTbGlkZU5leHQ6IFN3aXBlck9wdGlvbnNbJ2FsbG93U2xpZGVOZXh0J107XHJcbiAgQElucHV0KCkgc3dpcGVIYW5kbGVyOiBTd2lwZXJPcHRpb25zWydzd2lwZUhhbmRsZXInXTtcclxuICBASW5wdXQoKSBub1N3aXBpbmc6IFN3aXBlck9wdGlvbnNbJ25vU3dpcGluZyddO1xyXG4gIEBJbnB1dCgpIG5vU3dpcGluZ0NsYXNzOiBTd2lwZXJPcHRpb25zWydub1N3aXBpbmdDbGFzcyddO1xyXG4gIEBJbnB1dCgpIG5vU3dpcGluZ1NlbGVjdG9yOiBTd2lwZXJPcHRpb25zWydub1N3aXBpbmdTZWxlY3RvciddO1xyXG4gIEBJbnB1dCgpIHBhc3NpdmVMaXN0ZW5lcnM6IFN3aXBlck9wdGlvbnNbJ3Bhc3NpdmVMaXN0ZW5lcnMnXTtcclxuICBASW5wdXQoKSBjb250YWluZXJNb2RpZmllckNsYXNzOiBTd2lwZXJPcHRpb25zWydjb250YWluZXJNb2RpZmllckNsYXNzJ107XHJcbiAgQElucHV0KCkgc2xpZGVDbGFzczogU3dpcGVyT3B0aW9uc1snc2xpZGVDbGFzcyddID0gJ3N3aXBlci1zbGlkZSc7XHJcbiAgQElucHV0KCkgc2xpZGVCbGFua0NsYXNzOiBTd2lwZXJPcHRpb25zWydzbGlkZUJsYW5rQ2xhc3MnXTtcclxuICBASW5wdXQoKSBzbGlkZUFjdGl2ZUNsYXNzOiBTd2lwZXJPcHRpb25zWydzbGlkZUFjdGl2ZUNsYXNzJ107XHJcbiAgQElucHV0KCkgc2xpZGVEdXBsaWNhdGVBY3RpdmVDbGFzczogU3dpcGVyT3B0aW9uc1snc2xpZGVEdXBsaWNhdGVBY3RpdmVDbGFzcyddO1xyXG4gIEBJbnB1dCgpIHNsaWRlVmlzaWJsZUNsYXNzOiBTd2lwZXJPcHRpb25zWydzbGlkZVZpc2libGVDbGFzcyddO1xyXG4gIEBJbnB1dCgpIHNsaWRlRHVwbGljYXRlQ2xhc3M6IFN3aXBlck9wdGlvbnNbJ3NsaWRlRHVwbGljYXRlQ2xhc3MnXTtcclxuICBASW5wdXQoKSBzbGlkZU5leHRDbGFzczogU3dpcGVyT3B0aW9uc1snc2xpZGVOZXh0Q2xhc3MnXTtcclxuICBASW5wdXQoKSBzbGlkZUR1cGxpY2F0ZU5leHRDbGFzczogU3dpcGVyT3B0aW9uc1snc2xpZGVEdXBsaWNhdGVOZXh0Q2xhc3MnXTtcclxuICBASW5wdXQoKSBzbGlkZVByZXZDbGFzczogU3dpcGVyT3B0aW9uc1snc2xpZGVQcmV2Q2xhc3MnXTtcclxuICBASW5wdXQoKSBzbGlkZUR1cGxpY2F0ZVByZXZDbGFzczogU3dpcGVyT3B0aW9uc1snc2xpZGVEdXBsaWNhdGVQcmV2Q2xhc3MnXTtcclxuICBASW5wdXQoKSB3cmFwcGVyQ2xhc3M6IFN3aXBlck9wdGlvbnNbJ3dyYXBwZXJDbGFzcyddID0gJ3N3aXBlci13cmFwcGVyJztcclxuICBASW5wdXQoKSBydW5DYWxsYmFja3NPbkluaXQ6IFN3aXBlck9wdGlvbnNbJ3J1bkNhbGxiYWNrc09uSW5pdCddO1xyXG4gIEBJbnB1dCgpIG9ic2VydmVQYXJlbnRzOiBTd2lwZXJPcHRpb25zWydvYnNlcnZlUGFyZW50cyddO1xyXG4gIEBJbnB1dCgpIG9ic2VydmVTbGlkZUNoaWxkcmVuOiBTd2lwZXJPcHRpb25zWydvYnNlcnZlU2xpZGVDaGlsZHJlbiddO1xyXG4gIEBJbnB1dCgpIGExMXk6IFN3aXBlck9wdGlvbnNbJ2ExMXknXTtcclxuICBASW5wdXQoKSBhdXRvcGxheTogU3dpcGVyT3B0aW9uc1snYXV0b3BsYXknXTtcclxuICBASW5wdXQoKSBjb250cm9sbGVyOiBTd2lwZXJPcHRpb25zWydjb250cm9sbGVyJ107XHJcbiAgQElucHV0KCkgY292ZXJmbG93RWZmZWN0OiBTd2lwZXJPcHRpb25zWydjb3ZlcmZsb3dFZmZlY3QnXTtcclxuICBASW5wdXQoKSBjdWJlRWZmZWN0OiBTd2lwZXJPcHRpb25zWydjdWJlRWZmZWN0J107XHJcbiAgQElucHV0KCkgZmFkZUVmZmVjdDogU3dpcGVyT3B0aW9uc1snZmFkZUVmZmVjdCddO1xyXG4gIEBJbnB1dCgpIGZsaXBFZmZlY3Q6IFN3aXBlck9wdGlvbnNbJ2ZsaXBFZmZlY3QnXTtcclxuICBASW5wdXQoKSBjcmVhdGl2ZUVmZmVjdDogU3dpcGVyT3B0aW9uc1snY3JlYXRpdmVFZmZlY3QnXTtcclxuICBASW5wdXQoKSBjYXJkc0VmZmVjdDogU3dpcGVyT3B0aW9uc1snY2FyZHNFZmZlY3QnXTtcclxuICBASW5wdXQoKSBoYXNoTmF2aWdhdGlvbjogU3dpcGVyT3B0aW9uc1snaGFzaE5hdmlnYXRpb24nXTtcclxuICBASW5wdXQoKSBoaXN0b3J5OiBTd2lwZXJPcHRpb25zWydoaXN0b3J5J107XHJcbiAgQElucHV0KCkga2V5Ym9hcmQ6IFN3aXBlck9wdGlvbnNbJ2tleWJvYXJkJ107XHJcbiAgQElucHV0KCkgbGF6eTogU3dpcGVyT3B0aW9uc1snbGF6eSddO1xyXG4gIEBJbnB1dCgpIG1vdXNld2hlZWw6IFN3aXBlck9wdGlvbnNbJ21vdXNld2hlZWwnXTtcclxuICBASW5wdXQoKSBwYXJhbGxheDogU3dpcGVyT3B0aW9uc1sncGFyYWxsYXgnXTtcclxuICBASW5wdXQoKSB0aHVtYnM6IFN3aXBlck9wdGlvbnNbJ3RodW1icyddO1xyXG4gIEBJbnB1dCgpIHpvb206IFN3aXBlck9wdGlvbnNbJ3pvb20nXTtcclxuICBASW5wdXQoKSBjbGFzczogc3RyaW5nO1xyXG4gIEBJbnB1dCgpIGlkOiBzdHJpbmc7XHJcbiAgQElucHV0KClcclxuICBzZXQgbmF2aWdhdGlvbih2YWwpIHtcclxuICAgIGNvbnN0IGN1cnJlbnROZXh0ID1cclxuICAgICAgdHlwZW9mIHRoaXMuX25hdmlnYXRpb24gIT09ICdib29sZWFuJyAmJiB0aGlzLl9uYXZpZ2F0aW9uICE9PSAnJ1xyXG4gICAgICAgID8gdGhpcy5fbmF2aWdhdGlvbj8ubmV4dEVsXHJcbiAgICAgICAgOiBudWxsO1xyXG4gICAgY29uc3QgY3VycmVudFByZXYgPVxyXG4gICAgICB0eXBlb2YgdGhpcy5fbmF2aWdhdGlvbiAhPT0gJ2Jvb2xlYW4nICYmIHRoaXMuX25hdmlnYXRpb24gIT09ICcnXHJcbiAgICAgICAgPyB0aGlzLl9uYXZpZ2F0aW9uPy5wcmV2RWxcclxuICAgICAgICA6IG51bGw7XHJcbiAgICB0aGlzLl9uYXZpZ2F0aW9uID0gc2V0UHJvcGVydHkodmFsLCB7XHJcbiAgICAgIG5leHRFbDogY3VycmVudE5leHQgfHwgbnVsbCxcclxuICAgICAgcHJldkVsOiBjdXJyZW50UHJldiB8fCBudWxsLFxyXG4gICAgfSk7XHJcbiAgICB0aGlzLnNob3dOYXZpZ2F0aW9uID0gIShcclxuICAgICAgY29lcmNlQm9vbGVhblByb3BlcnR5KHZhbCkgIT09IHRydWUgfHxcclxuICAgICAgKHRoaXMuX25hdmlnYXRpb24gJiZcclxuICAgICAgICB0eXBlb2YgdGhpcy5fbmF2aWdhdGlvbiAhPT0gJ2Jvb2xlYW4nICYmXHJcbiAgICAgICAgdGhpcy5fbmF2aWdhdGlvbi5wcmV2RWwgIT09IHRoaXMuX3ByZXZFbFJlZj8ubmF0aXZlRWxlbWVudCAmJlxyXG4gICAgICAgICh0aGlzLl9uYXZpZ2F0aW9uLnByZXZFbCAhPT0gbnVsbCB8fCB0aGlzLl9uYXZpZ2F0aW9uLm5leHRFbCAhPT0gbnVsbCkgJiZcclxuICAgICAgICAodHlwZW9mIHRoaXMuX25hdmlnYXRpb24ubmV4dEVsID09PSAnc3RyaW5nJyB8fFxyXG4gICAgICAgICAgdHlwZW9mIHRoaXMuX25hdmlnYXRpb24ucHJldkVsID09PSAnc3RyaW5nJyB8fFxyXG4gICAgICAgICAgdHlwZW9mIHRoaXMuX25hdmlnYXRpb24ubmV4dEVsID09PSAnb2JqZWN0JyB8fFxyXG4gICAgICAgICAgdHlwZW9mIHRoaXMuX25hdmlnYXRpb24ucHJldkVsID09PSAnb2JqZWN0JykpXHJcbiAgICApO1xyXG4gIH1cclxuICBnZXQgbmF2aWdhdGlvbigpIHtcclxuICAgIHJldHVybiB0aGlzLl9uYXZpZ2F0aW9uO1xyXG4gIH1cclxuICBwcml2YXRlIF9uYXZpZ2F0aW9uOiBOYXZpZ2F0aW9uT3B0aW9ucyB8IGJvb2xlYW4gfCAnJztcclxuICBzaG93TmF2aWdhdGlvbjogYm9vbGVhbiA9IHRydWU7XHJcblxyXG4gIEBJbnB1dCgpXHJcbiAgc2V0IHBhZ2luYXRpb24odmFsKSB7XHJcbiAgICBjb25zdCBjdXJyZW50ID1cclxuICAgICAgdHlwZW9mIHRoaXMuX3BhZ2luYXRpb24gIT09ICdib29sZWFuJyAmJiB0aGlzLl9wYWdpbmF0aW9uICE9PSAnJ1xyXG4gICAgICAgID8gdGhpcy5fcGFnaW5hdGlvbj8uZWxcclxuICAgICAgICA6IG51bGw7XHJcbiAgICB0aGlzLl9wYWdpbmF0aW9uID0gc2V0UHJvcGVydHkodmFsLCB7XHJcbiAgICAgIGVsOiBjdXJyZW50IHx8IG51bGwsXHJcbiAgICB9KTtcclxuICAgIHRoaXMuc2hvd1BhZ2luYXRpb24gPSBpc1Nob3dFbCh2YWwsIHRoaXMuX3BhZ2luYXRpb24sIHRoaXMuX3BhZ2luYXRpb25FbFJlZik7XHJcbiAgfVxyXG4gIGdldCBwYWdpbmF0aW9uKCkge1xyXG4gICAgcmV0dXJuIHRoaXMuX3BhZ2luYXRpb247XHJcbiAgfVxyXG4gIHByaXZhdGUgX3BhZ2luYXRpb246IFBhZ2luYXRpb25PcHRpb25zIHwgYm9vbGVhbiB8ICcnO1xyXG4gIHNob3dQYWdpbmF0aW9uOiBib29sZWFuID0gdHJ1ZTtcclxuXHJcbiAgQElucHV0KClcclxuICBzZXQgc2Nyb2xsYmFyKHZhbCkge1xyXG4gICAgY29uc3QgY3VycmVudCA9XHJcbiAgICAgIHR5cGVvZiB0aGlzLl9zY3JvbGxiYXIgIT09ICdib29sZWFuJyAmJiB0aGlzLl9zY3JvbGxiYXIgIT09ICcnID8gdGhpcy5fc2Nyb2xsYmFyPy5lbCA6IG51bGw7XHJcbiAgICB0aGlzLl9zY3JvbGxiYXIgPSBzZXRQcm9wZXJ0eSh2YWwsIHtcclxuICAgICAgZWw6IGN1cnJlbnQgfHwgbnVsbCxcclxuICAgIH0pO1xyXG4gICAgdGhpcy5zaG93U2Nyb2xsYmFyID0gaXNTaG93RWwodmFsLCB0aGlzLl9zY3JvbGxiYXIsIHRoaXMuX3Njcm9sbGJhckVsUmVmKTtcclxuICB9XHJcbiAgZ2V0IHNjcm9sbGJhcigpIHtcclxuICAgIHJldHVybiB0aGlzLl9zY3JvbGxiYXI7XHJcbiAgfVxyXG4gIHByaXZhdGUgX3Njcm9sbGJhcjogU2Nyb2xsYmFyT3B0aW9ucyB8IGJvb2xlYW4gfCAnJztcclxuICBzaG93U2Nyb2xsYmFyOiBib29sZWFuID0gdHJ1ZTtcclxuXHJcbiAgQElucHV0KClcclxuICBzZXQgdmlydHVhbCh2YWwpIHtcclxuICAgIHRoaXMuX3ZpcnR1YWwgPSBzZXRQcm9wZXJ0eSh2YWwpO1xyXG4gIH1cclxuICBnZXQgdmlydHVhbCgpIHtcclxuICAgIHJldHVybiB0aGlzLl92aXJ0dWFsO1xyXG4gIH1cclxuICBwcml2YXRlIF92aXJ0dWFsOiBWaXJ0dWFsT3B0aW9ucyB8IGJvb2xlYW4gfCAnJztcclxuXHJcbiAgQElucHV0KClcclxuICBzZXQgY29uZmlnKHZhbDogU3dpcGVyT3B0aW9ucykge1xyXG4gICAgdGhpcy51cGRhdGVTd2lwZXIodmFsKTtcclxuICAgIGNvbnN0IHsgcGFyYW1zIH0gPSBnZXRQYXJhbXModmFsKTtcclxuICAgIE9iamVjdC5hc3NpZ24odGhpcywgcGFyYW1zKTtcclxuICB9XHJcbiAgQE91dHB1dCgnX2JlZm9yZUJyZWFrcG9pbnQnKSBzX19iZWZvcmVCcmVha3BvaW50ID0gbmV3IEV2ZW50RW1pdHRlcjxcclxuICAgIEV2ZW50c1BhcmFtc1snX2JlZm9yZUJyZWFrcG9pbnQnXVxyXG4gID4oKTtcclxuXHJcbiAgQE91dHB1dCgnX2NvbnRhaW5lckNsYXNzZXMnKSBzX19jb250YWluZXJDbGFzc2VzID0gbmV3IEV2ZW50RW1pdHRlcjxcclxuICAgIEV2ZW50c1BhcmFtc1snX2NvbnRhaW5lckNsYXNzZXMnXVxyXG4gID4oKTtcclxuXHJcbiAgQE91dHB1dCgnX3NsaWRlQ2xhc3MnKSBzX19zbGlkZUNsYXNzID0gbmV3IEV2ZW50RW1pdHRlcjxFdmVudHNQYXJhbXNbJ19zbGlkZUNsYXNzJ10+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ19zd2lwZXInKSBzX19zd2lwZXIgPSBuZXcgRXZlbnRFbWl0dGVyPEV2ZW50c1BhcmFtc1snX3N3aXBlciddPigpO1xyXG5cclxuICBAT3V0cHV0KCdhY3RpdmVJbmRleENoYW5nZScpIHNfYWN0aXZlSW5kZXhDaGFuZ2UgPSBuZXcgRXZlbnRFbWl0dGVyPFxyXG4gICAgRXZlbnRzUGFyYW1zWydhY3RpdmVJbmRleENoYW5nZSddXHJcbiAgPigpO1xyXG5cclxuICBAT3V0cHV0KCdhZnRlckluaXQnKSBzX2FmdGVySW5pdCA9IG5ldyBFdmVudEVtaXR0ZXI8RXZlbnRzUGFyYW1zWydhZnRlckluaXQnXT4oKTtcclxuXHJcbiAgQE91dHB1dCgnYXV0b3BsYXknKSBzX2F1dG9wbGF5ID0gbmV3IEV2ZW50RW1pdHRlcjxFdmVudHNQYXJhbXNbJ2F1dG9wbGF5J10+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ2F1dG9wbGF5U3RhcnQnKSBzX2F1dG9wbGF5U3RhcnQgPSBuZXcgRXZlbnRFbWl0dGVyPEV2ZW50c1BhcmFtc1snYXV0b3BsYXlTdGFydCddPigpO1xyXG5cclxuICBAT3V0cHV0KCdhdXRvcGxheVN0b3AnKSBzX2F1dG9wbGF5U3RvcCA9IG5ldyBFdmVudEVtaXR0ZXI8RXZlbnRzUGFyYW1zWydhdXRvcGxheVN0b3AnXT4oKTtcclxuXHJcbiAgQE91dHB1dCgnYXV0b3BsYXlQYXVzZScpIHNfYXV0b3BsYXlQYXVzZSA9IG5ldyBFdmVudEVtaXR0ZXI8RXZlbnRzUGFyYW1zWydhdXRvcGxheVBhdXNlJ10+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ2F1dG9wbGF5UmVzdW1lJykgc19hdXRvcGxheVJlc3VtZSA9IG5ldyBFdmVudEVtaXR0ZXI8RXZlbnRzUGFyYW1zWydhdXRvcGxheVJlc3VtZSddPigpO1xyXG5cclxuICBAT3V0cHV0KCdiZWZvcmVEZXN0cm95Jykgc19iZWZvcmVEZXN0cm95ID0gbmV3IEV2ZW50RW1pdHRlcjxFdmVudHNQYXJhbXNbJ2JlZm9yZURlc3Ryb3knXT4oKTtcclxuXHJcbiAgQE91dHB1dCgnYmVmb3JlSW5pdCcpIHNfYmVmb3JlSW5pdCA9IG5ldyBFdmVudEVtaXR0ZXI8RXZlbnRzUGFyYW1zWydiZWZvcmVJbml0J10+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ2JlZm9yZUxvb3BGaXgnKSBzX2JlZm9yZUxvb3BGaXggPSBuZXcgRXZlbnRFbWl0dGVyPEV2ZW50c1BhcmFtc1snYmVmb3JlTG9vcEZpeCddPigpO1xyXG5cclxuICBAT3V0cHV0KCdiZWZvcmVSZXNpemUnKSBzX2JlZm9yZVJlc2l6ZSA9IG5ldyBFdmVudEVtaXR0ZXI8RXZlbnRzUGFyYW1zWydiZWZvcmVSZXNpemUnXT4oKTtcclxuXHJcbiAgQE91dHB1dCgnYmVmb3JlU2xpZGVDaGFuZ2VTdGFydCcpIHNfYmVmb3JlU2xpZGVDaGFuZ2VTdGFydCA9IG5ldyBFdmVudEVtaXR0ZXI8XHJcbiAgICBFdmVudHNQYXJhbXNbJ2JlZm9yZVNsaWRlQ2hhbmdlU3RhcnQnXVxyXG4gID4oKTtcclxuXHJcbiAgQE91dHB1dCgnYmVmb3JlVHJhbnNpdGlvblN0YXJ0Jykgc19iZWZvcmVUcmFuc2l0aW9uU3RhcnQgPSBuZXcgRXZlbnRFbWl0dGVyPFxyXG4gICAgRXZlbnRzUGFyYW1zWydiZWZvcmVUcmFuc2l0aW9uU3RhcnQnXVxyXG4gID4oKTtcclxuXHJcbiAgQE91dHB1dCgnYnJlYWtwb2ludCcpIHNfYnJlYWtwb2ludCA9IG5ldyBFdmVudEVtaXR0ZXI8RXZlbnRzUGFyYW1zWydicmVha3BvaW50J10+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ2NoYW5nZURpcmVjdGlvbicpIHNfY2hhbmdlRGlyZWN0aW9uID0gbmV3IEV2ZW50RW1pdHRlcjxcclxuICAgIEV2ZW50c1BhcmFtc1snY2hhbmdlRGlyZWN0aW9uJ11cclxuICA+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ2NsaWNrJykgc19jbGljayA9IG5ldyBFdmVudEVtaXR0ZXI8RXZlbnRzUGFyYW1zWydjbGljayddPigpO1xyXG5cclxuICBAT3V0cHV0KCdkb3VibGVUYXAnKSBzX2RvdWJsZVRhcCA9IG5ldyBFdmVudEVtaXR0ZXI8RXZlbnRzUGFyYW1zWydkb3VibGVUYXAnXT4oKTtcclxuXHJcbiAgQE91dHB1dCgnZG91YmxlQ2xpY2snKSBzX2RvdWJsZUNsaWNrID0gbmV3IEV2ZW50RW1pdHRlcjxFdmVudHNQYXJhbXNbJ2RvdWJsZUNsaWNrJ10+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ2Rlc3Ryb3knKSBzX2Rlc3Ryb3kgPSBuZXcgRXZlbnRFbWl0dGVyPEV2ZW50c1BhcmFtc1snZGVzdHJveSddPigpO1xyXG5cclxuICBAT3V0cHV0KCdmcm9tRWRnZScpIHNfZnJvbUVkZ2UgPSBuZXcgRXZlbnRFbWl0dGVyPEV2ZW50c1BhcmFtc1snZnJvbUVkZ2UnXT4oKTtcclxuXHJcbiAgQE91dHB1dCgnaGFzaENoYW5nZScpIHNfaGFzaENoYW5nZSA9IG5ldyBFdmVudEVtaXR0ZXI8RXZlbnRzUGFyYW1zWydoYXNoQ2hhbmdlJ10+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ2hhc2hTZXQnKSBzX2hhc2hTZXQgPSBuZXcgRXZlbnRFbWl0dGVyPEV2ZW50c1BhcmFtc1snaGFzaFNldCddPigpO1xyXG5cclxuICBAT3V0cHV0KCdpbWFnZXNSZWFkeScpIHNfaW1hZ2VzUmVhZHkgPSBuZXcgRXZlbnRFbWl0dGVyPEV2ZW50c1BhcmFtc1snaW1hZ2VzUmVhZHknXT4oKTtcclxuXHJcbiAgQE91dHB1dCgnaW5pdCcpIHNfaW5pdCA9IG5ldyBFdmVudEVtaXR0ZXI8RXZlbnRzUGFyYW1zWydpbml0J10+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ2tleVByZXNzJykgc19rZXlQcmVzcyA9IG5ldyBFdmVudEVtaXR0ZXI8RXZlbnRzUGFyYW1zWydrZXlQcmVzcyddPigpO1xyXG5cclxuICBAT3V0cHV0KCdsYXp5SW1hZ2VMb2FkJykgc19sYXp5SW1hZ2VMb2FkID0gbmV3IEV2ZW50RW1pdHRlcjxFdmVudHNQYXJhbXNbJ2xhenlJbWFnZUxvYWQnXT4oKTtcclxuXHJcbiAgQE91dHB1dCgnbGF6eUltYWdlUmVhZHknKSBzX2xhenlJbWFnZVJlYWR5ID0gbmV3IEV2ZW50RW1pdHRlcjxFdmVudHNQYXJhbXNbJ2xhenlJbWFnZVJlYWR5J10+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ2xvb3BGaXgnKSBzX2xvb3BGaXggPSBuZXcgRXZlbnRFbWl0dGVyPEV2ZW50c1BhcmFtc1snbG9vcEZpeCddPigpO1xyXG5cclxuICBAT3V0cHV0KCdtb21lbnR1bUJvdW5jZScpIHNfbW9tZW50dW1Cb3VuY2UgPSBuZXcgRXZlbnRFbWl0dGVyPEV2ZW50c1BhcmFtc1snbW9tZW50dW1Cb3VuY2UnXT4oKTtcclxuXHJcbiAgQE91dHB1dCgnbmF2aWdhdGlvbkhpZGUnKSBzX25hdmlnYXRpb25IaWRlID0gbmV3IEV2ZW50RW1pdHRlcjxFdmVudHNQYXJhbXNbJ25hdmlnYXRpb25IaWRlJ10+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ25hdmlnYXRpb25TaG93Jykgc19uYXZpZ2F0aW9uU2hvdyA9IG5ldyBFdmVudEVtaXR0ZXI8RXZlbnRzUGFyYW1zWyduYXZpZ2F0aW9uU2hvdyddPigpO1xyXG5cclxuICBAT3V0cHV0KCduYXZpZ2F0aW9uUHJldicpIHNfbmF2aWdhdGlvblByZXYgPSBuZXcgRXZlbnRFbWl0dGVyPEV2ZW50c1BhcmFtc1snbmF2aWdhdGlvblByZXYnXT4oKTtcclxuXHJcbiAgQE91dHB1dCgnbmF2aWdhdGlvbk5leHQnKSBzX25hdmlnYXRpb25OZXh0ID0gbmV3IEV2ZW50RW1pdHRlcjxFdmVudHNQYXJhbXNbJ25hdmlnYXRpb25OZXh0J10+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ29ic2VydmVyVXBkYXRlJykgc19vYnNlcnZlclVwZGF0ZSA9IG5ldyBFdmVudEVtaXR0ZXI8RXZlbnRzUGFyYW1zWydvYnNlcnZlclVwZGF0ZSddPigpO1xyXG5cclxuICBAT3V0cHV0KCdvcmllbnRhdGlvbmNoYW5nZScpIHNfb3JpZW50YXRpb25jaGFuZ2UgPSBuZXcgRXZlbnRFbWl0dGVyPFxyXG4gICAgRXZlbnRzUGFyYW1zWydvcmllbnRhdGlvbmNoYW5nZSddXHJcbiAgPigpO1xyXG5cclxuICBAT3V0cHV0KCdwYWdpbmF0aW9uSGlkZScpIHNfcGFnaW5hdGlvbkhpZGUgPSBuZXcgRXZlbnRFbWl0dGVyPEV2ZW50c1BhcmFtc1sncGFnaW5hdGlvbkhpZGUnXT4oKTtcclxuXHJcbiAgQE91dHB1dCgncGFnaW5hdGlvblJlbmRlcicpIHNfcGFnaW5hdGlvblJlbmRlciA9IG5ldyBFdmVudEVtaXR0ZXI8XHJcbiAgICBFdmVudHNQYXJhbXNbJ3BhZ2luYXRpb25SZW5kZXInXVxyXG4gID4oKTtcclxuXHJcbiAgQE91dHB1dCgncGFnaW5hdGlvblNob3cnKSBzX3BhZ2luYXRpb25TaG93ID0gbmV3IEV2ZW50RW1pdHRlcjxFdmVudHNQYXJhbXNbJ3BhZ2luYXRpb25TaG93J10+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ3BhZ2luYXRpb25VcGRhdGUnKSBzX3BhZ2luYXRpb25VcGRhdGUgPSBuZXcgRXZlbnRFbWl0dGVyPFxyXG4gICAgRXZlbnRzUGFyYW1zWydwYWdpbmF0aW9uVXBkYXRlJ11cclxuICA+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ3Byb2dyZXNzJykgc19wcm9ncmVzcyA9IG5ldyBFdmVudEVtaXR0ZXI8RXZlbnRzUGFyYW1zWydwcm9ncmVzcyddPigpO1xyXG5cclxuICBAT3V0cHV0KCdyZWFjaEJlZ2lubmluZycpIHNfcmVhY2hCZWdpbm5pbmcgPSBuZXcgRXZlbnRFbWl0dGVyPEV2ZW50c1BhcmFtc1sncmVhY2hCZWdpbm5pbmcnXT4oKTtcclxuXHJcbiAgQE91dHB1dCgncmVhY2hFbmQnKSBzX3JlYWNoRW5kID0gbmV3IEV2ZW50RW1pdHRlcjxFdmVudHNQYXJhbXNbJ3JlYWNoRW5kJ10+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ3JlYWxJbmRleENoYW5nZScpIHNfcmVhbEluZGV4Q2hhbmdlID0gbmV3IEV2ZW50RW1pdHRlcjxcclxuICAgIEV2ZW50c1BhcmFtc1sncmVhbEluZGV4Q2hhbmdlJ11cclxuICA+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ3Jlc2l6ZScpIHNfcmVzaXplID0gbmV3IEV2ZW50RW1pdHRlcjxFdmVudHNQYXJhbXNbJ3Jlc2l6ZSddPigpO1xyXG5cclxuICBAT3V0cHV0KCdzY3JvbGwnKSBzX3Njcm9sbCA9IG5ldyBFdmVudEVtaXR0ZXI8RXZlbnRzUGFyYW1zWydzY3JvbGwnXT4oKTtcclxuXHJcbiAgQE91dHB1dCgnc2Nyb2xsYmFyRHJhZ0VuZCcpIHNfc2Nyb2xsYmFyRHJhZ0VuZCA9IG5ldyBFdmVudEVtaXR0ZXI8XHJcbiAgICBFdmVudHNQYXJhbXNbJ3Njcm9sbGJhckRyYWdFbmQnXVxyXG4gID4oKTtcclxuXHJcbiAgQE91dHB1dCgnc2Nyb2xsYmFyRHJhZ01vdmUnKSBzX3Njcm9sbGJhckRyYWdNb3ZlID0gbmV3IEV2ZW50RW1pdHRlcjxcclxuICAgIEV2ZW50c1BhcmFtc1snc2Nyb2xsYmFyRHJhZ01vdmUnXVxyXG4gID4oKTtcclxuXHJcbiAgQE91dHB1dCgnc2Nyb2xsYmFyRHJhZ1N0YXJ0Jykgc19zY3JvbGxiYXJEcmFnU3RhcnQgPSBuZXcgRXZlbnRFbWl0dGVyPFxyXG4gICAgRXZlbnRzUGFyYW1zWydzY3JvbGxiYXJEcmFnU3RhcnQnXVxyXG4gID4oKTtcclxuXHJcbiAgQE91dHB1dCgnc2V0VHJhbnNpdGlvbicpIHNfc2V0VHJhbnNpdGlvbiA9IG5ldyBFdmVudEVtaXR0ZXI8RXZlbnRzUGFyYW1zWydzZXRUcmFuc2l0aW9uJ10+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ3NldFRyYW5zbGF0ZScpIHNfc2V0VHJhbnNsYXRlID0gbmV3IEV2ZW50RW1pdHRlcjxFdmVudHNQYXJhbXNbJ3NldFRyYW5zbGF0ZSddPigpO1xyXG5cclxuICBAT3V0cHV0KCdzbGlkZUNoYW5nZScpIHNfc2xpZGVDaGFuZ2UgPSBuZXcgRXZlbnRFbWl0dGVyPEV2ZW50c1BhcmFtc1snc2xpZGVDaGFuZ2UnXT4oKTtcclxuXHJcbiAgQE91dHB1dCgnc2xpZGVDaGFuZ2VUcmFuc2l0aW9uRW5kJykgc19zbGlkZUNoYW5nZVRyYW5zaXRpb25FbmQgPSBuZXcgRXZlbnRFbWl0dGVyPFxyXG4gICAgRXZlbnRzUGFyYW1zWydzbGlkZUNoYW5nZVRyYW5zaXRpb25FbmQnXVxyXG4gID4oKTtcclxuXHJcbiAgQE91dHB1dCgnc2xpZGVDaGFuZ2VUcmFuc2l0aW9uU3RhcnQnKSBzX3NsaWRlQ2hhbmdlVHJhbnNpdGlvblN0YXJ0ID0gbmV3IEV2ZW50RW1pdHRlcjxcclxuICAgIEV2ZW50c1BhcmFtc1snc2xpZGVDaGFuZ2VUcmFuc2l0aW9uU3RhcnQnXVxyXG4gID4oKTtcclxuXHJcbiAgQE91dHB1dCgnc2xpZGVOZXh0VHJhbnNpdGlvbkVuZCcpIHNfc2xpZGVOZXh0VHJhbnNpdGlvbkVuZCA9IG5ldyBFdmVudEVtaXR0ZXI8XHJcbiAgICBFdmVudHNQYXJhbXNbJ3NsaWRlTmV4dFRyYW5zaXRpb25FbmQnXVxyXG4gID4oKTtcclxuXHJcbiAgQE91dHB1dCgnc2xpZGVOZXh0VHJhbnNpdGlvblN0YXJ0Jykgc19zbGlkZU5leHRUcmFuc2l0aW9uU3RhcnQgPSBuZXcgRXZlbnRFbWl0dGVyPFxyXG4gICAgRXZlbnRzUGFyYW1zWydzbGlkZU5leHRUcmFuc2l0aW9uU3RhcnQnXVxyXG4gID4oKTtcclxuXHJcbiAgQE91dHB1dCgnc2xpZGVQcmV2VHJhbnNpdGlvbkVuZCcpIHNfc2xpZGVQcmV2VHJhbnNpdGlvbkVuZCA9IG5ldyBFdmVudEVtaXR0ZXI8XHJcbiAgICBFdmVudHNQYXJhbXNbJ3NsaWRlUHJldlRyYW5zaXRpb25FbmQnXVxyXG4gID4oKTtcclxuXHJcbiAgQE91dHB1dCgnc2xpZGVQcmV2VHJhbnNpdGlvblN0YXJ0Jykgc19zbGlkZVByZXZUcmFuc2l0aW9uU3RhcnQgPSBuZXcgRXZlbnRFbWl0dGVyPFxyXG4gICAgRXZlbnRzUGFyYW1zWydzbGlkZVByZXZUcmFuc2l0aW9uU3RhcnQnXVxyXG4gID4oKTtcclxuXHJcbiAgQE91dHB1dCgnc2xpZGVSZXNldFRyYW5zaXRpb25TdGFydCcpIHNfc2xpZGVSZXNldFRyYW5zaXRpb25TdGFydCA9IG5ldyBFdmVudEVtaXR0ZXI8XHJcbiAgICBFdmVudHNQYXJhbXNbJ3NsaWRlUmVzZXRUcmFuc2l0aW9uU3RhcnQnXVxyXG4gID4oKTtcclxuXHJcbiAgQE91dHB1dCgnc2xpZGVSZXNldFRyYW5zaXRpb25FbmQnKSBzX3NsaWRlUmVzZXRUcmFuc2l0aW9uRW5kID0gbmV3IEV2ZW50RW1pdHRlcjxcclxuICAgIEV2ZW50c1BhcmFtc1snc2xpZGVSZXNldFRyYW5zaXRpb25FbmQnXVxyXG4gID4oKTtcclxuXHJcbiAgQE91dHB1dCgnc2xpZGVyTW92ZScpIHNfc2xpZGVyTW92ZSA9IG5ldyBFdmVudEVtaXR0ZXI8RXZlbnRzUGFyYW1zWydzbGlkZXJNb3ZlJ10+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ3NsaWRlckZpcnN0TW92ZScpIHNfc2xpZGVyRmlyc3RNb3ZlID0gbmV3IEV2ZW50RW1pdHRlcjxcclxuICAgIEV2ZW50c1BhcmFtc1snc2xpZGVyRmlyc3RNb3ZlJ11cclxuICA+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ3NsaWRlc0xlbmd0aENoYW5nZScpIHNfc2xpZGVzTGVuZ3RoQ2hhbmdlID0gbmV3IEV2ZW50RW1pdHRlcjxcclxuICAgIEV2ZW50c1BhcmFtc1snc2xpZGVzTGVuZ3RoQ2hhbmdlJ11cclxuICA+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ3NsaWRlc0dyaWRMZW5ndGhDaGFuZ2UnKSBzX3NsaWRlc0dyaWRMZW5ndGhDaGFuZ2UgPSBuZXcgRXZlbnRFbWl0dGVyPFxyXG4gICAgRXZlbnRzUGFyYW1zWydzbGlkZXNHcmlkTGVuZ3RoQ2hhbmdlJ11cclxuICA+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ3NuYXBHcmlkTGVuZ3RoQ2hhbmdlJykgc19zbmFwR3JpZExlbmd0aENoYW5nZSA9IG5ldyBFdmVudEVtaXR0ZXI8XHJcbiAgICBFdmVudHNQYXJhbXNbJ3NuYXBHcmlkTGVuZ3RoQ2hhbmdlJ11cclxuICA+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ3NuYXBJbmRleENoYW5nZScpIHNfc25hcEluZGV4Q2hhbmdlID0gbmV3IEV2ZW50RW1pdHRlcjxcclxuICAgIEV2ZW50c1BhcmFtc1snc25hcEluZGV4Q2hhbmdlJ11cclxuICA+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ3RhcCcpIHNfdGFwID0gbmV3IEV2ZW50RW1pdHRlcjxFdmVudHNQYXJhbXNbJ3RhcCddPigpO1xyXG5cclxuICBAT3V0cHV0KCd0b0VkZ2UnKSBzX3RvRWRnZSA9IG5ldyBFdmVudEVtaXR0ZXI8RXZlbnRzUGFyYW1zWyd0b0VkZ2UnXT4oKTtcclxuXHJcbiAgQE91dHB1dCgndG91Y2hFbmQnKSBzX3RvdWNoRW5kID0gbmV3IEV2ZW50RW1pdHRlcjxFdmVudHNQYXJhbXNbJ3RvdWNoRW5kJ10+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ3RvdWNoTW92ZScpIHNfdG91Y2hNb3ZlID0gbmV3IEV2ZW50RW1pdHRlcjxFdmVudHNQYXJhbXNbJ3RvdWNoTW92ZSddPigpO1xyXG5cclxuICBAT3V0cHV0KCd0b3VjaE1vdmVPcHBvc2l0ZScpIHNfdG91Y2hNb3ZlT3Bwb3NpdGUgPSBuZXcgRXZlbnRFbWl0dGVyPFxyXG4gICAgRXZlbnRzUGFyYW1zWyd0b3VjaE1vdmVPcHBvc2l0ZSddXHJcbiAgPigpO1xyXG5cclxuICBAT3V0cHV0KCd0b3VjaFN0YXJ0Jykgc190b3VjaFN0YXJ0ID0gbmV3IEV2ZW50RW1pdHRlcjxFdmVudHNQYXJhbXNbJ3RvdWNoU3RhcnQnXT4oKTtcclxuXHJcbiAgQE91dHB1dCgndHJhbnNpdGlvbkVuZCcpIHNfdHJhbnNpdGlvbkVuZCA9IG5ldyBFdmVudEVtaXR0ZXI8RXZlbnRzUGFyYW1zWyd0cmFuc2l0aW9uRW5kJ10+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ3RyYW5zaXRpb25TdGFydCcpIHNfdHJhbnNpdGlvblN0YXJ0ID0gbmV3IEV2ZW50RW1pdHRlcjxcclxuICAgIEV2ZW50c1BhcmFtc1sndHJhbnNpdGlvblN0YXJ0J11cclxuICA+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ3VwZGF0ZScpIHNfdXBkYXRlID0gbmV3IEV2ZW50RW1pdHRlcjxFdmVudHNQYXJhbXNbJ3VwZGF0ZSddPigpO1xyXG5cclxuICBAT3V0cHV0KCd6b29tQ2hhbmdlJykgc196b29tQ2hhbmdlID0gbmV3IEV2ZW50RW1pdHRlcjxFdmVudHNQYXJhbXNbJ3pvb21DaGFuZ2UnXT4oKTtcclxuXHJcbiAgQE91dHB1dCgnc3dpcGVyJykgc19zd2lwZXIgPSBuZXcgRXZlbnRFbWl0dGVyPGFueT4oKTtcclxuXHJcbiAgQE91dHB1dCgnbG9jaycpIHNfbG9jayA9IG5ldyBFdmVudEVtaXR0ZXI8RXZlbnRzUGFyYW1zWydsb2NrJ10+KCk7XHJcblxyXG4gIEBPdXRwdXQoJ3VubG9jaycpIHNfdW5sb2NrID0gbmV3IEV2ZW50RW1pdHRlcjxFdmVudHNQYXJhbXNbJ3VubG9jayddPigpO1xyXG5cclxuICBAVmlld0NoaWxkKCdwcmV2RWxSZWYnLCB7IHN0YXRpYzogZmFsc2UgfSlcclxuICBzZXQgcHJldkVsUmVmKGVsOiBFbGVtZW50UmVmKSB7XHJcbiAgICB0aGlzLl9wcmV2RWxSZWYgPSBlbDtcclxuICAgIHRoaXMuX3NldEVsZW1lbnQoZWwsIHRoaXMubmF2aWdhdGlvbiwgJ25hdmlnYXRpb24nLCAncHJldkVsJyk7XHJcbiAgfVxyXG4gIF9wcmV2RWxSZWY6IEVsZW1lbnRSZWY7XHJcbiAgQFZpZXdDaGlsZCgnbmV4dEVsUmVmJywgeyBzdGF0aWM6IGZhbHNlIH0pXHJcbiAgc2V0IG5leHRFbFJlZihlbDogRWxlbWVudFJlZikge1xyXG4gICAgdGhpcy5fbmV4dEVsUmVmID0gZWw7XHJcbiAgICB0aGlzLl9zZXRFbGVtZW50KGVsLCB0aGlzLm5hdmlnYXRpb24sICduYXZpZ2F0aW9uJywgJ25leHRFbCcpO1xyXG4gIH1cclxuICBfbmV4dEVsUmVmOiBFbGVtZW50UmVmO1xyXG4gIEBWaWV3Q2hpbGQoJ3Njcm9sbGJhckVsUmVmJywgeyBzdGF0aWM6IGZhbHNlIH0pXHJcbiAgc2V0IHNjcm9sbGJhckVsUmVmKGVsOiBFbGVtZW50UmVmKSB7XHJcbiAgICB0aGlzLl9zY3JvbGxiYXJFbFJlZiA9IGVsO1xyXG4gICAgdGhpcy5fc2V0RWxlbWVudChlbCwgdGhpcy5zY3JvbGxiYXIsICdzY3JvbGxiYXInKTtcclxuICB9XHJcbiAgX3Njcm9sbGJhckVsUmVmOiBFbGVtZW50UmVmO1xyXG4gIEBWaWV3Q2hpbGQoJ3BhZ2luYXRpb25FbFJlZicsIHsgc3RhdGljOiBmYWxzZSB9KVxyXG4gIHNldCBwYWdpbmF0aW9uRWxSZWYoZWw6IEVsZW1lbnRSZWYpIHtcclxuICAgIHRoaXMuX3BhZ2luYXRpb25FbFJlZiA9IGVsO1xyXG4gICAgdGhpcy5fc2V0RWxlbWVudChlbCwgdGhpcy5wYWdpbmF0aW9uLCAncGFnaW5hdGlvbicpO1xyXG4gIH1cclxuICBfcGFnaW5hdGlvbkVsUmVmOiBFbGVtZW50UmVmO1xyXG4gIEBDb250ZW50Q2hpbGRyZW4oU3dpcGVyU2xpZGVEaXJlY3RpdmUsIHsgZGVzY2VuZGFudHM6IGZhbHNlLCBlbWl0RGlzdGluY3RDaGFuZ2VzT25seTogdHJ1ZSB9KVxyXG4gIHNsaWRlc0VsOiBRdWVyeUxpc3Q8U3dpcGVyU2xpZGVEaXJlY3RpdmU+O1xyXG4gIHByaXZhdGUgc2xpZGVzOiBTd2lwZXJTbGlkZURpcmVjdGl2ZVtdO1xyXG5cclxuICBwcmVwZW5kU2xpZGVzOiBPYnNlcnZhYmxlPFN3aXBlclNsaWRlRGlyZWN0aXZlW10+O1xyXG4gIGFwcGVuZFNsaWRlczogT2JzZXJ2YWJsZTxTd2lwZXJTbGlkZURpcmVjdGl2ZVtdPjtcclxuXHJcbiAgc3dpcGVyUmVmOiBTd2lwZXI7XHJcbiAgcmVhZG9ubHkgX2FjdGl2ZVNsaWRlcyA9IG5ldyBTdWJqZWN0PFN3aXBlclNsaWRlRGlyZWN0aXZlW10+KCk7XHJcblxyXG4gIGdldCBhY3RpdmVTbGlkZXMoKSB7XHJcbiAgICBpZiAodGhpcy52aXJ0dWFsKSB7XHJcbiAgICAgIHJldHVybiB0aGlzLl9hY3RpdmVTbGlkZXM7XHJcbiAgICB9XHJcbiAgICByZXR1cm4gb2YodGhpcy5zbGlkZXMpO1xyXG4gIH1cclxuXHJcbiAgZ2V0IHpvb21Db250YWluZXJDbGFzcygpIHtcclxuICAgIHJldHVybiB0aGlzLnpvb20gJiYgdHlwZW9mIHRoaXMuem9vbSAhPT0gJ2Jvb2xlYW4nXHJcbiAgICAgID8gdGhpcy56b29tLmNvbnRhaW5lckNsYXNzXHJcbiAgICAgIDogJ3N3aXBlci16b29tLWNvbnRhaW5lcic7XHJcbiAgfVxyXG5cclxuICBASG9zdEJpbmRpbmcoJ2NsYXNzJykgY29udGFpbmVyQ2xhc3Nlczogc3RyaW5nID0gJ3N3aXBlcic7XHJcbiAgY29uc3RydWN0b3IoXHJcbiAgICBwcml2YXRlIF9uZ1pvbmU6IE5nWm9uZSxcclxuICAgIHByaXZhdGUgZWxlbWVudFJlZjogRWxlbWVudFJlZixcclxuICAgIHByaXZhdGUgX2NoYW5nZURldGVjdG9yUmVmOiBDaGFuZ2VEZXRlY3RvclJlZixcclxuICAgIEBJbmplY3QoUExBVEZPUk1fSUQpIHByaXZhdGUgX3BsYXRmb3JtSWQ6IE9iamVjdCxcclxuICApIHt9XHJcblxyXG4gIHByaXZhdGUgX3NldEVsZW1lbnQoZWw6IEVsZW1lbnRSZWYsIHJlZjogYW55LCB1cGRhdGU6IHN0cmluZywga2V5ID0gJ2VsJykge1xyXG4gICAgaWYgKCFyZWYgfHwgIWVsKSByZXR1cm47XHJcbiAgICBpZiAoZWwubmF0aXZlRWxlbWVudCkge1xyXG4gICAgICBpZiAocmVmW2tleV0gPT09IGVsLm5hdGl2ZUVsZW1lbnQpIHtcclxuICAgICAgICByZXR1cm47XHJcbiAgICAgIH1cclxuICAgICAgcmVmW2tleV0gPSBlbC5uYXRpdmVFbGVtZW50O1xyXG4gICAgfVxyXG4gICAgY29uc3QgdXBkYXRlT2JqOiB7IFtrZXk6IHN0cmluZ106IGJvb2xlYW4gfSA9IHt9O1xyXG4gICAgdXBkYXRlT2JqW3VwZGF0ZV0gPSB0cnVlO1xyXG4gICAgdGhpcy51cGRhdGVJbml0U3dpcGVyKHVwZGF0ZU9iaik7XHJcbiAgfVxyXG4gIG5nT25Jbml0KCk6IHZvaWQge1xyXG4gICAgY29uc3QgeyBwYXJhbXMgfSA9IGdldFBhcmFtcyh0aGlzKTtcclxuICAgIE9iamVjdC5hc3NpZ24odGhpcywgcGFyYW1zKTtcclxuICB9XHJcbiAgbmdBZnRlclZpZXdJbml0KCkge1xyXG4gICAgdGhpcy5jaGlsZHJlblNsaWRlc0luaXQoKTtcclxuICAgIHRoaXMuaW5pdFN3aXBlcigpO1xyXG4gICAgdGhpcy5fY2hhbmdlRGV0ZWN0b3JSZWYuZGV0ZWN0Q2hhbmdlcygpO1xyXG4gICAgc2V0VGltZW91dCgoKSA9PiB7XHJcbiAgICAgIHRoaXMuc19zd2lwZXIuZW1pdCh0aGlzLnN3aXBlclJlZik7XHJcbiAgICB9KTtcclxuICB9XHJcblxyXG4gIHByaXZhdGUgY2hpbGRyZW5TbGlkZXNJbml0KCkge1xyXG4gICAgdGhpcy5zbGlkZXNDaGFuZ2VzKHRoaXMuc2xpZGVzRWwpO1xyXG4gICAgdGhpcy5zbGlkZXNFbC5jaGFuZ2VzLnN1YnNjcmliZSh0aGlzLnNsaWRlc0NoYW5nZXMpO1xyXG4gIH1cclxuXHJcbiAgcHJpdmF0ZSBzbGlkZXNDaGFuZ2VzID0gKHZhbDogUXVlcnlMaXN0PFN3aXBlclNsaWRlRGlyZWN0aXZlPikgPT4ge1xyXG4gICAgdGhpcy5zbGlkZXMgPSB2YWwubWFwKChzbGlkZTogU3dpcGVyU2xpZGVEaXJlY3RpdmUsIGluZGV4OiBudW1iZXIpID0+IHtcclxuICAgICAgc2xpZGUuc2xpZGVJbmRleCA9IGluZGV4O1xyXG4gICAgICBzbGlkZS5jbGFzc05hbWVzID0gdGhpcy5zbGlkZUNsYXNzIHx8ICcnO1xyXG4gICAgICByZXR1cm4gc2xpZGU7XHJcbiAgICB9KTtcclxuICAgIGlmICh0aGlzLmxvb3AgJiYgIXRoaXMubG9vcGVkU2xpZGVzKSB7XHJcbiAgICAgIHRoaXMuY2FsY0xvb3BlZFNsaWRlcygpO1xyXG4gICAgfVxyXG4gICAgaWYgKCF0aGlzLnZpcnR1YWwpIHtcclxuICAgICAgaWYgKHRoaXMubG9vcGVkU2xpZGVzKSB7XHJcbiAgICAgICAgdGhpcy5wcmVwZW5kU2xpZGVzID0gb2YodGhpcy5zbGlkZXMuc2xpY2UodGhpcy5zbGlkZXMubGVuZ3RoIC0gdGhpcy5sb29wZWRTbGlkZXMpKTtcclxuICAgICAgICB0aGlzLmFwcGVuZFNsaWRlcyA9IG9mKHRoaXMuc2xpZGVzLnNsaWNlKDAsIHRoaXMubG9vcGVkU2xpZGVzKSk7XHJcbiAgICAgIH1cclxuICAgIH0gZWxzZSBpZiAodGhpcy5zd2lwZXJSZWYgJiYgdGhpcy5zd2lwZXJSZWYudmlydHVhbCkge1xyXG4gICAgICB0aGlzLl9uZ1pvbmUucnVuT3V0c2lkZUFuZ3VsYXIoKCkgPT4ge1xyXG4gICAgICAgIHRoaXMuc3dpcGVyUmVmLnZpcnR1YWwuc2xpZGVzID0gdGhpcy5zbGlkZXM7XHJcbiAgICAgICAgdGhpcy5zd2lwZXJSZWYudmlydHVhbC51cGRhdGUodHJ1ZSk7XHJcbiAgICAgIH0pO1xyXG4gICAgfVxyXG4gICAgdGhpcy5fY2hhbmdlRGV0ZWN0b3JSZWYuZGV0ZWN0Q2hhbmdlcygpO1xyXG4gIH07XHJcblxyXG4gIGdldCBpc1N3aXBlckFjdGl2ZSgpIHtcclxuICAgIHJldHVybiB0aGlzLnN3aXBlclJlZiAmJiAhdGhpcy5zd2lwZXJSZWYuZGVzdHJveWVkO1xyXG4gIH1cclxuXHJcbiAgaW5pdFN3aXBlcigpIHtcclxuICAgIGNvbnN0IHsgcGFyYW1zOiBzd2lwZXJQYXJhbXMsIHBhc3NlZFBhcmFtcyB9ID0gZ2V0UGFyYW1zKHRoaXMpO1xyXG4gICAgT2JqZWN0LmFzc2lnbih0aGlzLCBzd2lwZXJQYXJhbXMpO1xyXG4gICAgdGhpcy5fbmdab25lLnJ1bk91dHNpZGVBbmd1bGFyKCgpID0+IHtcclxuICAgICAgc3dpcGVyUGFyYW1zLmluaXQgPSBmYWxzZTtcclxuICAgICAgaWYgKCFzd2lwZXJQYXJhbXMudmlydHVhbCkge1xyXG4gICAgICAgIHN3aXBlclBhcmFtcy5vYnNlcnZlciA9IHRydWU7XHJcbiAgICAgIH1cclxuXHJcbiAgICAgIHN3aXBlclBhcmFtcy5vbkFueSA9IChldmVudE5hbWU6IGtleW9mIFN3aXBlckNvbXBvbmVudCwgLi4uYXJnczogYW55W10pID0+IHtcclxuICAgICAgICBjb25zdCBlbWl0dGVyID0gdGhpc1soJ3NfJyArIGV2ZW50TmFtZSkgYXMga2V5b2YgU3dpcGVyQ29tcG9uZW50XSBhcyBFdmVudEVtaXR0ZXI8YW55PjtcclxuICAgICAgICBpZiAoZW1pdHRlcikge1xyXG4gICAgICAgICAgZW1pdHRlci5lbWl0KFsuLi5hcmdzXSk7XHJcbiAgICAgICAgfVxyXG4gICAgICB9O1xyXG4gICAgICBjb25zdCBfc2xpZGVDbGFzc2VzOiBTd2lwZXJFdmVudHNbJ19zbGlkZUNsYXNzZXMnXSA9IChfLCB1cGRhdGVkKSA9PiB7XHJcbiAgICAgICAgdXBkYXRlZC5mb3JFYWNoKCh7IHNsaWRlRWwsIGNsYXNzTmFtZXMgfSwgaW5kZXgpID0+IHtcclxuICAgICAgICAgIGNvbnN0IGRhdGFJbmRleCA9IHNsaWRlRWwuZ2V0QXR0cmlidXRlKCdkYXRhLXN3aXBlci1zbGlkZS1pbmRleCcpO1xyXG4gICAgICAgICAgY29uc3Qgc2xpZGVJbmRleCA9IGRhdGFJbmRleCA/IHBhcnNlSW50KGRhdGFJbmRleCkgOiBpbmRleDtcclxuICAgICAgICAgIGlmICh0aGlzLnZpcnR1YWwpIHtcclxuICAgICAgICAgICAgY29uc3QgdmlydHVhbFNsaWRlID0gdGhpcy5zbGlkZXMuZmluZCgoaXRlbSkgPT4ge1xyXG4gICAgICAgICAgICAgIHJldHVybiBpdGVtLnZpcnR1YWxJbmRleCAmJiBpdGVtLnZpcnR1YWxJbmRleCA9PT0gc2xpZGVJbmRleDtcclxuICAgICAgICAgICAgfSk7XHJcbiAgICAgICAgICAgIGlmICh2aXJ0dWFsU2xpZGUpIHtcclxuICAgICAgICAgICAgICB2aXJ0dWFsU2xpZGUuY2xhc3NOYW1lcyA9IGNsYXNzTmFtZXM7XHJcbiAgICAgICAgICAgICAgcmV0dXJuO1xyXG4gICAgICAgICAgICB9XHJcbiAgICAgICAgICB9XHJcblxyXG4gICAgICAgICAgaWYgKHRoaXMuc2xpZGVzW3NsaWRlSW5kZXhdKSB7XHJcbiAgICAgICAgICAgIHRoaXMuc2xpZGVzW3NsaWRlSW5kZXhdLmNsYXNzTmFtZXMgPSBjbGFzc05hbWVzO1xyXG4gICAgICAgICAgfVxyXG4gICAgICAgIH0pO1xyXG4gICAgICAgIHRoaXMuX2NoYW5nZURldGVjdG9yUmVmLmRldGVjdENoYW5nZXMoKTtcclxuICAgICAgfTtcclxuICAgICAgY29uc3QgX2NvbnRhaW5lckNsYXNzZXM6IFN3aXBlckV2ZW50c1snX2NvbnRhaW5lckNsYXNzZXMnXSA9IChfLCBjbGFzc2VzKSA9PiB7XHJcbiAgICAgICAgc2V0VGltZW91dCgoKSA9PiB7XHJcbiAgICAgICAgICB0aGlzLmNvbnRhaW5lckNsYXNzZXMgPSBjbGFzc2VzO1xyXG4gICAgICAgIH0pO1xyXG4gICAgICB9O1xyXG4gICAgICBPYmplY3QuYXNzaWduKHN3aXBlclBhcmFtcy5vbiwge1xyXG4gICAgICAgIF9jb250YWluZXJDbGFzc2VzLFxyXG4gICAgICAgIF9zbGlkZUNsYXNzZXMsXHJcbiAgICAgIH0pO1xyXG4gICAgICBjb25zdCBzd2lwZXJSZWYgPSBuZXcgU3dpcGVyKHN3aXBlclBhcmFtcyk7XHJcbiAgICAgIHN3aXBlclJlZi5sb29wQ3JlYXRlID0gKCkgPT4ge307XHJcbiAgICAgIHN3aXBlclJlZi5sb29wRGVzdHJveSA9ICgpID0+IHt9O1xyXG4gICAgICBpZiAoc3dpcGVyUGFyYW1zLmxvb3ApIHtcclxuICAgICAgICBzd2lwZXJSZWYubG9vcGVkU2xpZGVzID0gdGhpcy5sb29wZWRTbGlkZXM7XHJcbiAgICAgIH1cclxuICAgICAgY29uc3QgaXNWaXJ0dWFsRW5hYmxlZCA9IGlzRW5hYmxlZChzd2lwZXJSZWYucGFyYW1zLnZpcnR1YWwpO1xyXG4gICAgICBpZiAoc3dpcGVyUmVmLnZpcnR1YWwgJiYgaXNWaXJ0dWFsRW5hYmxlZCkge1xyXG4gICAgICAgIHN3aXBlclJlZi52aXJ0dWFsLnNsaWRlcyA9IHRoaXMuc2xpZGVzO1xyXG4gICAgICAgIGNvbnN0IGV4dGVuZFdpdGggPSB7XHJcbiAgICAgICAgICBjYWNoZTogZmFsc2UsXHJcbiAgICAgICAgICBzbGlkZXM6IHRoaXMuc2xpZGVzLFxyXG4gICAgICAgICAgcmVuZGVyRXh0ZXJuYWw6IHRoaXMudXBkYXRlVmlydHVhbFNsaWRlcyxcclxuICAgICAgICAgIHJlbmRlckV4dGVybmFsVXBkYXRlOiBmYWxzZSxcclxuICAgICAgICB9O1xyXG4gICAgICAgIGV4dGVuZChzd2lwZXJSZWYucGFyYW1zLnZpcnR1YWwsIGV4dGVuZFdpdGgpO1xyXG4gICAgICAgIGV4dGVuZChzd2lwZXJSZWYub3JpZ2luYWxQYXJhbXMudmlydHVhbCwgZXh0ZW5kV2l0aCk7XHJcbiAgICAgIH1cclxuXHJcbiAgICAgIGlmIChpc1BsYXRmb3JtQnJvd3Nlcih0aGlzLl9wbGF0Zm9ybUlkKSkge1xyXG4gICAgICAgIHRoaXMuc3dpcGVyUmVmID0gc3dpcGVyUmVmLmluaXQodGhpcy5lbGVtZW50UmVmLm5hdGl2ZUVsZW1lbnQpO1xyXG4gICAgICAgIGNvbnN0IGlzVmlydHVhbEVuYWJsZWQgPSBpc0VuYWJsZWQodGhpcy5zd2lwZXJSZWYucGFyYW1zLnZpcnR1YWwpO1xyXG4gICAgICAgIGlmICh0aGlzLnN3aXBlclJlZi52aXJ0dWFsICYmIGlzVmlydHVhbEVuYWJsZWQpIHtcclxuICAgICAgICAgIHRoaXMuc3dpcGVyUmVmLnZpcnR1YWwudXBkYXRlKHRydWUpO1xyXG4gICAgICAgIH1cclxuICAgICAgICB0aGlzLl9jaGFuZ2VEZXRlY3RvclJlZi5kZXRlY3RDaGFuZ2VzKCk7XHJcbiAgICAgIH1cclxuICAgIH0pO1xyXG4gIH1cclxuXHJcbiAgc3R5bGU6IGFueSA9IG51bGw7XHJcbiAgY3VycmVudFZpcnR1YWxEYXRhOiBhbnk7IC8vIFRPRE86IHR5cGUgdmlydHVhbERhdGE7XHJcbiAgcHJpdmF0ZSB1cGRhdGVWaXJ0dWFsU2xpZGVzID0gKHZpcnR1YWxEYXRhOiBhbnkpID0+IHtcclxuICAgIC8vIFRPRE86IHR5cGUgdmlydHVhbERhdGFcclxuICAgIGlmIChcclxuICAgICAgIXRoaXMuc3dpcGVyUmVmIHx8XHJcbiAgICAgICh0aGlzLmN1cnJlbnRWaXJ0dWFsRGF0YSAmJlxyXG4gICAgICAgIHRoaXMuY3VycmVudFZpcnR1YWxEYXRhLmZyb20gPT09IHZpcnR1YWxEYXRhLmZyb20gJiZcclxuICAgICAgICB0aGlzLmN1cnJlbnRWaXJ0dWFsRGF0YS50byA9PT0gdmlydHVhbERhdGEudG8gJiZcclxuICAgICAgICB0aGlzLmN1cnJlbnRWaXJ0dWFsRGF0YS5vZmZzZXQgPT09IHZpcnR1YWxEYXRhLm9mZnNldClcclxuICAgICkge1xyXG4gICAgICByZXR1cm47XHJcbiAgICB9XHJcbiAgICB0aGlzLnN0eWxlID0gdGhpcy5zd2lwZXJSZWYuaXNIb3Jpem9udGFsKClcclxuICAgICAgPyB7XHJcbiAgICAgICAgICBbdGhpcy5zd2lwZXJSZWYucnRsVHJhbnNsYXRlID8gJ3JpZ2h0JyA6ICdsZWZ0J106IGAke3ZpcnR1YWxEYXRhLm9mZnNldH1weGAsXHJcbiAgICAgICAgfVxyXG4gICAgICA6IHtcclxuICAgICAgICAgIHRvcDogYCR7dmlydHVhbERhdGEub2Zmc2V0fXB4YCxcclxuICAgICAgICB9O1xyXG4gICAgdGhpcy5jdXJyZW50VmlydHVhbERhdGEgPSB2aXJ0dWFsRGF0YTtcclxuICAgIHRoaXMuX2FjdGl2ZVNsaWRlcy5uZXh0KHZpcnR1YWxEYXRhLnNsaWRlcyk7XHJcbiAgICB0aGlzLl9uZ1pvbmUucnVuKCgpID0+IHtcclxuICAgICAgdGhpcy5fY2hhbmdlRGV0ZWN0b3JSZWYuZGV0ZWN0Q2hhbmdlcygpO1xyXG4gICAgfSk7XHJcbiAgICB0aGlzLl9uZ1pvbmUucnVuT3V0c2lkZUFuZ3VsYXIoKCkgPT4ge1xyXG4gICAgICB0aGlzLnN3aXBlclJlZi51cGRhdGVTbGlkZXMoKTtcclxuICAgICAgdGhpcy5zd2lwZXJSZWYudXBkYXRlUHJvZ3Jlc3MoKTtcclxuICAgICAgdGhpcy5zd2lwZXJSZWYudXBkYXRlU2xpZGVzQ2xhc3NlcygpO1xyXG4gICAgICBpZiAoaXNFbmFibGVkKHRoaXMuc3dpcGVyUmVmLnBhcmFtcy5sYXp5KSkge1xyXG4gICAgICAgIHRoaXMuc3dpcGVyUmVmLmxhenkubG9hZCgpO1xyXG4gICAgICB9XHJcbiAgICAgIHRoaXMuc3dpcGVyUmVmLnZpcnR1YWwudXBkYXRlKHRydWUpO1xyXG4gICAgfSk7XHJcbiAgICByZXR1cm47XHJcbiAgfTtcclxuXHJcbiAgbmdPbkNoYW5nZXMoY2hhbmdlZFBhcmFtczogU2ltcGxlQ2hhbmdlcykge1xyXG4gICAgdGhpcy51cGRhdGVTd2lwZXIoY2hhbmdlZFBhcmFtcyk7XHJcbiAgICB0aGlzLl9jaGFuZ2VEZXRlY3RvclJlZi5kZXRlY3RDaGFuZ2VzKCk7XHJcbiAgfVxyXG5cclxuICB1cGRhdGVJbml0U3dpcGVyKGNoYW5nZWRQYXJhbXM6IGFueSkge1xyXG4gICAgaWYgKCEoY2hhbmdlZFBhcmFtcyAmJiB0aGlzLnN3aXBlclJlZiAmJiAhdGhpcy5zd2lwZXJSZWYuZGVzdHJveWVkKSkge1xyXG4gICAgICByZXR1cm47XHJcbiAgICB9XHJcblxyXG4gICAgdGhpcy5fbmdab25lLnJ1bk91dHNpZGVBbmd1bGFyKCgpID0+IHtcclxuICAgICAgY29uc3Qge1xyXG4gICAgICAgIHBhcmFtczogY3VycmVudFBhcmFtcyxcclxuICAgICAgICBwYWdpbmF0aW9uLFxyXG4gICAgICAgIG5hdmlnYXRpb24sXHJcbiAgICAgICAgc2Nyb2xsYmFyLFxyXG4gICAgICAgIHZpcnR1YWwsXHJcbiAgICAgICAgdGh1bWJzLFxyXG4gICAgICB9ID0gdGhpcy5zd2lwZXJSZWY7XHJcblxyXG4gICAgICBpZiAoY2hhbmdlZFBhcmFtcy5wYWdpbmF0aW9uKSB7XHJcbiAgICAgICAgaWYgKFxyXG4gICAgICAgICAgdGhpcy5wYWdpbmF0aW9uICYmXHJcbiAgICAgICAgICB0eXBlb2YgdGhpcy5wYWdpbmF0aW9uICE9PSAnYm9vbGVhbicgJiZcclxuICAgICAgICAgIHRoaXMucGFnaW5hdGlvbi5lbCAmJlxyXG4gICAgICAgICAgcGFnaW5hdGlvbiAmJlxyXG4gICAgICAgICAgIXBhZ2luYXRpb24uZWxcclxuICAgICAgICApIHtcclxuICAgICAgICAgIHRoaXMudXBkYXRlUGFyYW1ldGVyKCdwYWdpbmF0aW9uJywgdGhpcy5wYWdpbmF0aW9uKTtcclxuICAgICAgICAgIHBhZ2luYXRpb24uaW5pdCgpO1xyXG4gICAgICAgICAgcGFnaW5hdGlvbi5yZW5kZXIoKTtcclxuICAgICAgICAgIHBhZ2luYXRpb24udXBkYXRlKCk7XHJcbiAgICAgICAgfSBlbHNlIHtcclxuICAgICAgICAgIHBhZ2luYXRpb24uZGVzdHJveSgpO1xyXG4gICAgICAgICAgcGFnaW5hdGlvbi5lbCA9IG51bGw7XHJcbiAgICAgICAgfVxyXG4gICAgICB9XHJcblxyXG4gICAgICBpZiAoY2hhbmdlZFBhcmFtcy5zY3JvbGxiYXIpIHtcclxuICAgICAgICBpZiAoXHJcbiAgICAgICAgICB0aGlzLnNjcm9sbGJhciAmJlxyXG4gICAgICAgICAgdHlwZW9mIHRoaXMuc2Nyb2xsYmFyICE9PSAnYm9vbGVhbicgJiZcclxuICAgICAgICAgIHRoaXMuc2Nyb2xsYmFyLmVsICYmXHJcbiAgICAgICAgICBzY3JvbGxiYXIgJiZcclxuICAgICAgICAgICFzY3JvbGxiYXIuZWxcclxuICAgICAgICApIHtcclxuICAgICAgICAgIHRoaXMudXBkYXRlUGFyYW1ldGVyKCdzY3JvbGxiYXInLCB0aGlzLnNjcm9sbGJhcik7XHJcbiAgICAgICAgICBzY3JvbGxiYXIuaW5pdCgpO1xyXG4gICAgICAgICAgc2Nyb2xsYmFyLnVwZGF0ZVNpemUoKTtcclxuICAgICAgICAgIHNjcm9sbGJhci5zZXRUcmFuc2xhdGUoKTtcclxuICAgICAgICB9IGVsc2Uge1xyXG4gICAgICAgICAgc2Nyb2xsYmFyLmRlc3Ryb3koKTtcclxuICAgICAgICAgIHNjcm9sbGJhci5lbCA9IG51bGw7XHJcbiAgICAgICAgfVxyXG4gICAgICB9XHJcblxyXG4gICAgICBpZiAoY2hhbmdlZFBhcmFtcy5uYXZpZ2F0aW9uKSB7XHJcbiAgICAgICAgaWYgKFxyXG4gICAgICAgICAgdGhpcy5uYXZpZ2F0aW9uICYmXHJcbiAgICAgICAgICB0eXBlb2YgdGhpcy5uYXZpZ2F0aW9uICE9PSAnYm9vbGVhbicgJiZcclxuICAgICAgICAgIHRoaXMubmF2aWdhdGlvbi5wcmV2RWwgJiZcclxuICAgICAgICAgIHRoaXMubmF2aWdhdGlvbi5uZXh0RWwgJiZcclxuICAgICAgICAgIG5hdmlnYXRpb24gJiZcclxuICAgICAgICAgICFuYXZpZ2F0aW9uLnByZXZFbCAmJlxyXG4gICAgICAgICAgIW5hdmlnYXRpb24ubmV4dEVsXHJcbiAgICAgICAgKSB7XHJcbiAgICAgICAgICB0aGlzLnVwZGF0ZVBhcmFtZXRlcignbmF2aWdhdGlvbicsIHRoaXMubmF2aWdhdGlvbik7XHJcbiAgICAgICAgICBuYXZpZ2F0aW9uLmluaXQoKTtcclxuICAgICAgICAgIG5hdmlnYXRpb24udXBkYXRlKCk7XHJcbiAgICAgICAgfSBlbHNlIGlmIChuYXZpZ2F0aW9uLnByZXZFbCAmJiBuYXZpZ2F0aW9uLm5leHRFbCkge1xyXG4gICAgICAgICAgbmF2aWdhdGlvbi5kZXN0cm95KCk7XHJcbiAgICAgICAgICBuYXZpZ2F0aW9uLm5leHRFbCA9IG51bGw7XHJcbiAgICAgICAgICBuYXZpZ2F0aW9uLnByZXZFbCA9IG51bGw7XHJcbiAgICAgICAgfVxyXG4gICAgICB9XHJcbiAgICAgIGlmIChjaGFuZ2VkUGFyYW1zLnRodW1icyAmJiB0aGlzLnRodW1icyAmJiB0aGlzLnRodW1icy5zd2lwZXIpIHtcclxuICAgICAgICB0aGlzLnVwZGF0ZVBhcmFtZXRlcigndGh1bWJzJywgdGhpcy50aHVtYnMpO1xyXG4gICAgICAgIGNvbnN0IGluaXRpYWxpemVkID0gdGh1bWJzLmluaXQoKTtcclxuICAgICAgICBpZiAoaW5pdGlhbGl6ZWQpIHRodW1icy51cGRhdGUodHJ1ZSk7XHJcbiAgICAgIH1cclxuXHJcbiAgICAgIGlmIChjaGFuZ2VkUGFyYW1zLmNvbnRyb2xsZXIgJiYgdGhpcy5jb250cm9sbGVyICYmIHRoaXMuY29udHJvbGxlci5jb250cm9sKSB7XHJcbiAgICAgICAgdGhpcy5zd2lwZXJSZWYuY29udHJvbGxlci5jb250cm9sID0gdGhpcy5jb250cm9sbGVyLmNvbnRyb2w7XHJcbiAgICAgIH1cclxuXHJcbiAgICAgIHRoaXMuc3dpcGVyUmVmLnVwZGF0ZSgpO1xyXG4gICAgfSk7XHJcbiAgfVxyXG5cclxuICB1cGRhdGVTd2lwZXIoY2hhbmdlZFBhcmFtczogU2ltcGxlQ2hhbmdlcyB8IGFueSkge1xyXG4gICAgdGhpcy5fbmdab25lLnJ1bk91dHNpZGVBbmd1bGFyKCgpID0+IHtcclxuICAgICAgaWYgKGNoYW5nZWRQYXJhbXMuY29uZmlnKSB7XHJcbiAgICAgICAgcmV0dXJuO1xyXG4gICAgICB9XHJcbiAgICAgIGlmICghKGNoYW5nZWRQYXJhbXMgJiYgdGhpcy5zd2lwZXJSZWYgJiYgIXRoaXMuc3dpcGVyUmVmLmRlc3Ryb3llZCkpIHtcclxuICAgICAgICByZXR1cm47XHJcbiAgICAgIH1cclxuICAgICAgZm9yIChjb25zdCBrZXkgaW4gY2hhbmdlZFBhcmFtcykge1xyXG4gICAgICAgIGlmIChpZ25vcmVOZ09uQ2hhbmdlcy5pbmRleE9mKGtleSkgPj0gMCkge1xyXG4gICAgICAgICAgY29udGludWU7XHJcbiAgICAgICAgfVxyXG4gICAgICAgIGNvbnN0IG5ld1ZhbHVlID0gY2hhbmdlZFBhcmFtc1trZXldPy5jdXJyZW50VmFsdWUgPz8gY2hhbmdlZFBhcmFtc1trZXldO1xyXG4gICAgICAgIHRoaXMudXBkYXRlUGFyYW1ldGVyKGtleSwgbmV3VmFsdWUpO1xyXG4gICAgICB9XHJcblxyXG4gICAgICBpZiAoY2hhbmdlZFBhcmFtcy5hbGxvd1NsaWRlTmV4dCkge1xyXG4gICAgICAgIHRoaXMuc3dpcGVyUmVmLmFsbG93U2xpZGVOZXh0ID0gdGhpcy5hbGxvd1NsaWRlTmV4dDtcclxuICAgICAgfVxyXG4gICAgICBpZiAoY2hhbmdlZFBhcmFtcy5hbGxvd1NsaWRlUHJldikge1xyXG4gICAgICAgIHRoaXMuc3dpcGVyUmVmLmFsbG93U2xpZGVQcmV2ID0gdGhpcy5hbGxvd1NsaWRlUHJldjtcclxuICAgICAgfVxyXG4gICAgICBpZiAoY2hhbmdlZFBhcmFtcy5kaXJlY3Rpb24pIHtcclxuICAgICAgICB0aGlzLnN3aXBlclJlZi5jaGFuZ2VEaXJlY3Rpb24odGhpcy5kaXJlY3Rpb24sIGZhbHNlKTtcclxuICAgICAgfVxyXG4gICAgICBpZiAoY2hhbmdlZFBhcmFtcy5icmVha3BvaW50cykge1xyXG4gICAgICAgIGlmICh0aGlzLmxvb3AgJiYgIXRoaXMubG9vcGVkU2xpZGVzKSB7XHJcbiAgICAgICAgICB0aGlzLmNhbGNMb29wZWRTbGlkZXMoKTtcclxuICAgICAgICB9XHJcbiAgICAgICAgdGhpcy5zd2lwZXJSZWYuY3VycmVudEJyZWFrcG9pbnQgPSBudWxsO1xyXG4gICAgICAgIHRoaXMuc3dpcGVyUmVmLnNldEJyZWFrcG9pbnQoKTtcclxuICAgICAgfVxyXG5cclxuICAgICAgaWYgKGNoYW5nZWRQYXJhbXMudGh1bWJzIHx8IGNoYW5nZWRQYXJhbXMuY29udHJvbGxlcikge1xyXG4gICAgICAgIHRoaXMudXBkYXRlSW5pdFN3aXBlcihjaGFuZ2VkUGFyYW1zKTtcclxuICAgICAgfVxyXG4gICAgICB0aGlzLnN3aXBlclJlZi51cGRhdGUoKTtcclxuICAgIH0pO1xyXG4gIH1cclxuXHJcbiAgY2FsY0xvb3BlZFNsaWRlcygpIHtcclxuICAgIGlmICghdGhpcy5sb29wKSB7XHJcbiAgICAgIHJldHVybiBmYWxzZTtcclxuICAgIH1cclxuICAgIGxldCBzbGlkZXNQZXJWaWV3UGFyYW1zID0gdGhpcy5zbGlkZXNQZXJWaWV3O1xyXG4gICAgaWYgKHRoaXMuYnJlYWtwb2ludHMpIHtcclxuICAgICAgY29uc3QgYnJlYWtwb2ludCA9IFN3aXBlci5wcm90b3R5cGUuZ2V0QnJlYWtwb2ludCh0aGlzLmJyZWFrcG9pbnRzKTtcclxuICAgICAgY29uc3QgYnJlYWtwb2ludE9ubHlQYXJhbXMgPVxyXG4gICAgICAgIGJyZWFrcG9pbnQgaW4gdGhpcy5icmVha3BvaW50cyA/IHRoaXMuYnJlYWtwb2ludHNbYnJlYWtwb2ludF0gOiB1bmRlZmluZWQ7XHJcbiAgICAgIGlmIChicmVha3BvaW50T25seVBhcmFtcyAmJiBicmVha3BvaW50T25seVBhcmFtcy5zbGlkZXNQZXJWaWV3KSB7XHJcbiAgICAgICAgc2xpZGVzUGVyVmlld1BhcmFtcyA9IGJyZWFrcG9pbnRPbmx5UGFyYW1zLnNsaWRlc1BlclZpZXc7XHJcbiAgICAgIH1cclxuICAgIH1cclxuICAgIGlmIChzbGlkZXNQZXJWaWV3UGFyYW1zID09PSAnYXV0bycpIHtcclxuICAgICAgdGhpcy5sb29wZWRTbGlkZXMgPSB0aGlzLnNsaWRlcy5sZW5ndGg7XHJcbiAgICAgIHJldHVybiB0aGlzLnNsaWRlcy5sZW5ndGg7XHJcbiAgICB9XHJcbiAgICBsZXQgbG9vcGVkU2xpZGVzID0gdGhpcy5sb29wZWRTbGlkZXMgfHwgc2xpZGVzUGVyVmlld1BhcmFtcztcclxuICAgIGlmICghbG9vcGVkU2xpZGVzKSB7XHJcbiAgICAgIC8vID9cclxuICAgICAgcmV0dXJuIGZhbHNlO1xyXG4gICAgfVxyXG5cclxuICAgIGlmICh0aGlzLmxvb3BBZGRpdGlvbmFsU2xpZGVzKSB7XHJcbiAgICAgIGxvb3BlZFNsaWRlcyArPSB0aGlzLmxvb3BBZGRpdGlvbmFsU2xpZGVzO1xyXG4gICAgfVxyXG4gICAgaWYgKGxvb3BlZFNsaWRlcyA+IHRoaXMuc2xpZGVzLmxlbmd0aCkge1xyXG4gICAgICBsb29wZWRTbGlkZXMgPSB0aGlzLnNsaWRlcy5sZW5ndGg7XHJcbiAgICB9XHJcbiAgICB0aGlzLmxvb3BlZFNsaWRlcyA9IGxvb3BlZFNsaWRlcztcclxuICAgIHJldHVybiB0cnVlO1xyXG4gIH1cclxuXHJcbiAgdXBkYXRlUGFyYW1ldGVyKGtleTogc3RyaW5nLCB2YWx1ZTogYW55KSB7XHJcbiAgICBpZiAoISh0aGlzLnN3aXBlclJlZiAmJiAhdGhpcy5zd2lwZXJSZWYuZGVzdHJveWVkKSkge1xyXG4gICAgICByZXR1cm47XHJcbiAgICB9XHJcbiAgICBjb25zdCBfa2V5ID0ga2V5LnJlcGxhY2UoL15fLywgJycpIGFzIGtleW9mIFN3aXBlck9wdGlvbnM7XHJcbiAgICBjb25zdCBpc0N1cnJlbnRQYXJhbU9iaiA9IGlzT2JqZWN0KHRoaXMuc3dpcGVyUmVmLnBhcmFtc1tfa2V5XSk7XHJcblxyXG4gICAgaWYgKF9rZXkgPT09ICdlbmFibGVkJykge1xyXG4gICAgICBpZiAodmFsdWUgPT09IHRydWUpIHtcclxuICAgICAgICB0aGlzLnN3aXBlclJlZi5lbmFibGUoKTtcclxuICAgICAgfSBlbHNlIGlmICh2YWx1ZSA9PT0gZmFsc2UpIHtcclxuICAgICAgICB0aGlzLnN3aXBlclJlZi5kaXNhYmxlKCk7XHJcbiAgICAgIH1cclxuICAgICAgcmV0dXJuO1xyXG4gICAgfVxyXG4gICAgaWYgKGlzQ3VycmVudFBhcmFtT2JqICYmIGlzT2JqZWN0KHZhbHVlKSkge1xyXG4gICAgICBleHRlbmQodGhpcy5zd2lwZXJSZWYucGFyYW1zW19rZXldLCB2YWx1ZSk7XHJcbiAgICB9IGVsc2Uge1xyXG4gICAgICAodGhpcy5zd2lwZXJSZWYucGFyYW1zW19rZXldIGFzIGFueSkgPSB2YWx1ZTtcclxuICAgIH1cclxuICB9XHJcblxyXG4gIG5nT25EZXN0cm95KCkge1xyXG4gICAgdGhpcy5fbmdab25lLnJ1bk91dHNpZGVBbmd1bGFyKCgpID0+IHtcclxuICAgICAgdGhpcy5zd2lwZXJSZWY/LmRlc3Ryb3kodHJ1ZSwgZmFsc2UpO1xyXG4gICAgfSk7XHJcbiAgfVxyXG59XHJcbiIsIjxuZy1jb250ZW50IHNlbGVjdD1cIltzbG90PWNvbnRhaW5lci1zdGFydF1cIj48L25nLWNvbnRlbnQ+XHJcbjxuZy1jb250YWluZXIgKm5nSWY9XCJuYXZpZ2F0aW9uICYmIHNob3dOYXZpZ2F0aW9uXCI+XHJcbiAgPGRpdiBjbGFzcz1cInN3aXBlci1idXR0b24tcHJldlwiICNwcmV2RWxSZWY+PC9kaXY+XHJcbiAgPGRpdiBjbGFzcz1cInN3aXBlci1idXR0b24tbmV4dFwiICNuZXh0RWxSZWY+PC9kaXY+XHJcbjwvbmctY29udGFpbmVyPlxyXG48ZGl2ICpuZ0lmPVwic2Nyb2xsYmFyICYmIHNob3dTY3JvbGxiYXJcIiBjbGFzcz1cInN3aXBlci1zY3JvbGxiYXJcIiAjc2Nyb2xsYmFyRWxSZWY+PC9kaXY+XHJcbjxkaXYgKm5nSWY9XCJwYWdpbmF0aW9uICYmIHNob3dQYWdpbmF0aW9uXCIgY2xhc3M9XCJzd2lwZXItcGFnaW5hdGlvblwiICNwYWdpbmF0aW9uRWxSZWY+PC9kaXY+XHJcbjxkaXYgW25nQ2xhc3NdPVwid3JhcHBlckNsYXNzXCIgW2F0dHIuaWRdPVwiaWRcIj5cclxuICA8bmctY29udGVudCBzZWxlY3Q9XCJbc2xvdD13cmFwcGVyLXN0YXJ0XVwiPjwvbmctY29udGVudD5cclxuICA8bmctdGVtcGxhdGVcclxuICAgICpuZ1RlbXBsYXRlT3V0bGV0PVwiXHJcbiAgICAgIHNsaWRlc1RlbXBsYXRlO1xyXG4gICAgICBjb250ZXh0OiB7XHJcbiAgICAgICAgbG9vcFNsaWRlczogcHJlcGVuZFNsaWRlcyxcclxuICAgICAgICBrZXk6ICdwcmVwZW5kJ1xyXG4gICAgICB9XHJcbiAgICBcIlxyXG4gID48L25nLXRlbXBsYXRlPlxyXG4gIDxuZy10ZW1wbGF0ZVxyXG4gICAgKm5nVGVtcGxhdGVPdXRsZXQ9XCJcclxuICAgICAgc2xpZGVzVGVtcGxhdGU7XHJcbiAgICAgIGNvbnRleHQ6IHtcclxuICAgICAgICBsb29wU2xpZGVzOiBhY3RpdmVTbGlkZXMsXHJcbiAgICAgICAga2V5OiAnJ1xyXG4gICAgICB9XHJcbiAgICBcIlxyXG4gID48L25nLXRlbXBsYXRlPlxyXG4gIDxuZy10ZW1wbGF0ZVxyXG4gICAgKm5nVGVtcGxhdGVPdXRsZXQ9XCJcclxuICAgICAgc2xpZGVzVGVtcGxhdGU7XHJcbiAgICAgIGNvbnRleHQ6IHtcclxuICAgICAgICBsb29wU2xpZGVzOiBhcHBlbmRTbGlkZXMsXHJcbiAgICAgICAga2V5OiAnYXBwZW5kJ1xyXG4gICAgICB9XHJcbiAgICBcIlxyXG4gID48L25nLXRlbXBsYXRlPlxyXG4gIDxuZy1jb250ZW50IHNlbGVjdD1cIltzbG90PXdyYXBwZXItZW5kXVwiPjwvbmctY29udGVudD5cclxuPC9kaXY+XHJcbjxuZy1jb250ZW50IHNlbGVjdD1cIltzbG90PWNvbnRhaW5lci1lbmRdXCI+PC9uZy1jb250ZW50PlxyXG5cclxuPG5nLXRlbXBsYXRlICNzbGlkZXNUZW1wbGF0ZSBsZXQtbG9vcFNsaWRlcz1cImxvb3BTbGlkZXNcIiBsZXQtc2xpZGVLZXk9XCJrZXlcIj5cclxuICA8ZGl2XHJcbiAgICAqbmdGb3I9XCJsZXQgc2xpZGUgb2YgbG9vcFNsaWRlcyB8IGFzeW5jXCJcclxuICAgIFtuZ0NsYXNzXT1cIlxyXG4gICAgICAoc2xpZGUuY2xhc3MgPyBzbGlkZS5jbGFzcyArICcgJyA6ICcnKSArXHJcbiAgICAgIHNsaWRlQ2xhc3MgK1xyXG4gICAgICAoc2xpZGVLZXkgIT09ICcnID8gJyAnICsgc2xpZGVEdXBsaWNhdGVDbGFzcyA6ICcnKVxyXG4gICAgXCJcclxuICAgIFthdHRyLmRhdGEtc3dpcGVyLXNsaWRlLWluZGV4XT1cInNsaWRlLnZpcnR1YWxJbmRleCA/IHNsaWRlLnZpcnR1YWxJbmRleCA6IHNsaWRlLnNsaWRlSW5kZXhcIlxyXG4gICAgW2F0dHIuZGF0YS1zd2lwZXItYXV0b3BsYXldPVwic2xpZGUuYXV0b3BsYXlEZWxheVwiXHJcbiAgICBbc3R5bGVdPVwic3R5bGVcIlxyXG4gICAgW25nU3dpdGNoXT1cInNsaWRlLnpvb21cIlxyXG4gID5cclxuICAgIDxkaXYgKm5nU3dpdGNoQ2FzZT1cInRydWVcIiBbbmdDbGFzc109XCJ6b29tQ29udGFpbmVyQ2xhc3NcIj5cclxuICAgICAgPG5nLXRlbXBsYXRlXHJcbiAgICAgICAgW25nVGVtcGxhdGVPdXRsZXRdPVwic2xpZGUudGVtcGxhdGVcIlxyXG4gICAgICAgIFtuZ1RlbXBsYXRlT3V0bGV0Q29udGV4dF09XCJ7XHJcbiAgICAgICAgICAkaW1wbGljaXQ6IHNsaWRlLnNsaWRlRGF0YVxyXG4gICAgICAgIH1cIlxyXG4gICAgICA+PC9uZy10ZW1wbGF0ZT5cclxuICAgIDwvZGl2PlxyXG4gICAgPG5nLWNvbnRhaW5lciAqbmdTd2l0Y2hEZWZhdWx0PlxyXG4gICAgICA8bmctdGVtcGxhdGVcclxuICAgICAgICBbbmdUZW1wbGF0ZU91dGxldF09XCJzbGlkZS50ZW1wbGF0ZVwiXHJcbiAgICAgICAgW25nVGVtcGxhdGVPdXRsZXRDb250ZXh0XT1cIntcclxuICAgICAgICAgICRpbXBsaWNpdDogc2xpZGUuc2xpZGVEYXRhXHJcbiAgICAgICAgfVwiXHJcbiAgICAgID48L25nLXRlbXBsYXRlPlxyXG4gICAgPC9uZy1jb250YWluZXI+XHJcbiAgPC9kaXY+XHJcbjwvbmctdGVtcGxhdGU+XHJcbiJdfQ==
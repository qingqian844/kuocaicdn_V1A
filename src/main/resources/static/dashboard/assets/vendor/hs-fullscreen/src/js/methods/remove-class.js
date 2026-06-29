export default function fullscreenRemoveClass(el, settings) {
	el.classList.remove(settings.toggleClassName.slice(1))
	
	document.querySelector(settings.mainContainerSelector).classList.remove(settings.preventScrollClassName.slice(1))
}

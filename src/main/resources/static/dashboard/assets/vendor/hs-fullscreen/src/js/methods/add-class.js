export default function fullscreenAddClass(el, settings) {
	el.classList.add(settings.toggleClassName.slice(1))
	
	document.querySelector(settings.mainContainerSelector).classList.add(settings.preventScrollClassName.slice(1))
}

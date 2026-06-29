import fullscreenAddClass from "./add-class"
import fullscreenRemoveClass from "./remove-class"

export default function fullscreenToggleClass(el, settings) {
	if (!el.classList.contains(settings.toggleClassName.slice(1))) {
		fullscreenAddClass(el, settings)
	} else {
		fullscreenRemoveClass(el, settings)
	}
}

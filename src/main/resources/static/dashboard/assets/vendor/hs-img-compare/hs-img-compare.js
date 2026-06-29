function hsImgCompare() {
  var x, i;
  let loadedImages = 0
  /*find all elements with an "overlay" class:*/
  x = document.getElementsByClassName('js-img-comp-wrapper');
  for (i = 0; i < x.length; i++) {
    /*once for each "overlay" element:
    pass the "overlay" element as a parameter when executing the compareImages function:*/
    compareImages(x[i]);
  }
  function compareImages(img) {
    let slider, clicked = 0, w, h, deviceHeaderEl, loader, images;
    deviceHeaderEl = document.querySelector('.js-img-comp')
    loader = document.querySelector('.js-img-comp-loader')
    images = document.querySelectorAll('.js-img-comp-container .hs-img-comp')
    const dispose = () => {
      if (!slider) return
      slider.removeEventListener("mousedown", slideReady)
      window.removeEventListener("mouseup", slideFinish)
      slider.removeEventListener("touchstart", slideReady)
      window.removeEventListener("touchend", slideFinish)
      window.removeEventListener("mousemove", slideMove)
      window.removeEventListener("touchmove", slideMove)
    }

    const init = () => {
      w = deviceHeaderEl.offsetWidth
      h = img.offsetHeight

      img.style.width = (w / 2) + "px"

      if (slider) {
        slider.remove()
      }

      slider = document.createElement("DIV")
      slider.innerHTML = `<i class="bi bi-chevron-left text-primary" /><i class="bi bi-chevron-right text-primary" />`
      slider.setAttribute('class', 'hs-img-comp-slider')
      img.parentElement.insertBefore(slider, img)
      slider.style.top = (h / 2) - (slider.offsetHeight / 2) + 'px'
      slider.style.right = (w / 2) - (slider.offsetWidth / 2) + 'px'

      window.addEventListener('resize', resize)
      slider.addEventListener("mousedown", slideReady)
      window.addEventListener("mouseup", slideFinish)
      slider.addEventListener("touchstart", slideReady)
      window.addEventListener("touchend", slideFinish)

      loader.remove()
    }

    const initilizeImage = (images) => {
      loadedImages++
      if (loadedImages === images.length) {
        resize()
      }
    }

    [].forEach.call(images, img => {
      if (img.complete) {
        setTimeout(() => {
          initilizeImage(images)
        }, 100)
      } else {
        img.addEventListener( 'load', () => {
          initilizeImage(images)
        }, false)
      }
    })

    function slideReady(e) {
      e.preventDefault()
      clicked = 1;
      window.addEventListener("mousemove", slideMove)
      window.addEventListener("touchmove", slideMove)
    }
    function slideFinish() {
      clicked = 0
    }
    function slideMove(e) {
      var pos
      if (clicked == 0) return false
      pos = getCursorPos(e)

      if (pos < 0) pos = 0
      if (pos > w) pos = w

      slide(pos)
    }
    function getCursorPos(e) {
      var a, x = 0
      e = (e.changedTouches) ? e.changedTouches[0] : e
      a = img.getBoundingClientRect()
      x = a.right - e.pageX
      x = x - window.pageXOffset
      return x
    }
    function slide(x) {
      img.style.width = x + 'px'
      slider.style.right = img.offsetWidth - (slider.offsetWidth / 2) + 'px'
    }
    function resize(e) {
      dispose()
      images.forEach(img => img.style.width = `${deviceHeaderEl.offsetWidth}px`)
      init()
    }
  }
}

hsImgCompare()

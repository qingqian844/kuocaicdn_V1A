export function fadeOut(el, time, callback) {
  var intervalID = setInterval(function () {

    if (!el.style.opacity) {
      el.style.opacity = 1;
    }


    if (el.style.opacity > 0) {
      el.style.opacity -= 0.1;
    } else {
      clearInterval(intervalID);
      el.style.display = 'none'
      callback()
    }

  }, time / 10);
}

export function createElementFromHTML(htmlString) {
  var div = document.createElement('div');
  div.innerHTML = htmlString.trim();

  return div.firstChild;
}


/* istanbul ignore next */
export const on = (function() {
    if (document.addEventListener) {
        return function(element, event, handler, useCapture = false) {
            if (element && event && handler) {
                element.addEventListener(event, handler, useCapture);
            }
        };
    } else {
        return function(element, event, handler) {
            if (element && event && handler) {
                element.attachEvent('on' + event, handler);
            }
        };
    }
})();

/* istanbul ignore next */
export const off = (function() {
    if (document.removeEventListener) {
        return function(element, event, handler, useCapture = false) {
            if (element && event) {
                element.removeEventListener(event, handler, useCapture);
            }
        };
    } else {
        return function(element, event, handler) {
            if (element && event) {
                element.detachEvent('on' + event, handler);
            }
        };
    }
})();

export function oneOf (value, validList) {
    for (let i = 0; i < validList.length; i++) {
        if (value === validList[i]) {
            return true;
        }
    }
    return false;
}
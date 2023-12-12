import Split from "./index.vue";
import SplitWindow from "./window.vue";

// 获取父节点并且包含指定class
export function getParentElByClname(element, className) {
	var el = element;
	if (el.nodeType === 1 && [...el?.classList].includes(className)) {
		return el;
	}
	while ((el = el.parentNode)) {
		if (el && el.nodeType === 1 && [...el?.classList].includes(className)) {
			return el;
		}
	}
	return null;
}

// 格式化css像素值
export function formatInsetCss(style) {
	const _style = {...style};
	Object.keys(_style).forEach(key => {
		_style[key] = _style[key] + "px";
	});
	return _style;
}

export const dropPositionMap = {
	top: "top",
	bottom: "bottom",
	left: "left",
	right: "right",
};
export const splitDirectionMap = {
	horizontal: "horizontal",
	vertical: "vertical",
};

export const eventMap = {};

export {Split, SplitWindow};

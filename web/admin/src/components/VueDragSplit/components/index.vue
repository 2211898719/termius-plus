<template>
    <div
        :style="{
            userSelect: resizing ? 'none' : '',
        }"
        class="split_container clearfix"
        @mouseup="onMouseUp"
        @mousemove="onMouseMove"
        ref="splitContainerRef"
        :split="split"
    >
        <Pane
            class="split_pane split_paneL"
            :split="split"
            :style="{ [type]: percent + '%' }"
        >
            <slot name="paneL"></slot>
        </Pane>

        <resizer
            v-if="!layout"
            :style="{ [resizeType]: percent + '%' }"
            :split="split"
            @mousedown="onMouseDown"
        ></resizer>

        <Pane
            v-if="!layout"
            class="split_pane split_paneR"
            :split="split"
            :style="{ [type]: 100 - percent + '%' }"
        >
            <slot name="paneR"></slot>
        </Pane>
    </div>
</template>
<script setup>
import Resizer from "./resizer.vue"
import Pane from "./pane.vue";
import { ref, watch } from "vue";

const props = defineProps({
    layout: {
        type: Boolean,
        default: false,
    },
    minPercent: {
        type: Number,
        default: 10,
    },
    defaultPercent: {
        type: Number,
        default: 50,
    },
    split: {
        validator(value) {
            return ["horizontal", "vertical"].indexOf(value) >= 0;
        },
        required: true,
    },
});
// const { layout, minPercent, defaultPercent, split } = toRefs(props);

const type = ref("width");
const resizeType = ref("left");
const resizing = ref(false);
const percent = ref(50);

watch(
    ()=>props.split,
    (nv) => {
        type.value = nv === "horizontal" ? "width" : "height";
        resizeType.value = nv === "horizontal" ? "left" : "top";
    },
    { immediate: true }
);
watch(
    ()=>props.defaultPercent,
    (nv) => {
        percent.value = nv;
    },
    { immediate: true }
);

const emits = defineEmits(["resize"]);
function debounce() {
    let timer = null;
    return (fun) => {
        if (timer) {
            return;
        }
        timer = setTimeout(() => {
            fun();
            clearTimeout(timer);
            timer = null;
        }, 100);
    };
}
const debounceIns = new debounce();
function onMouseDown() {
    resizing.value = true;
}
function onMouseUp() {
    resizing.value = false;
}
function onMouseMove(e) {
    if (e.buttons === 0 || e.which === 0) {
        return;
    }
    if (!resizing.value) return;
    let offset = 0;
    let target = e.currentTarget;
    if (props.split === "horizontal") {
        while (target) {
            offset += target.offsetLeft;
            target = target.offsetParent;
        }
    } else {
        while (target) {
            offset += target.offsetTop;
            target = target.offsetParent;
        }
    }
    const currentPage = props.split === "horizontal" ? e.pageX : e.pageY;
    const targetOffset =
        props.split === "horizontal"
            ? e.currentTarget.offsetWidth
            : e.currentTarget.offsetHeight;
    const _percent =
        Math.floor(((currentPage - offset) / targetOffset) * 10000) / 100;
    if (_percent > props.minPercent && _percent < 100 - props.minPercent) {
        percent.value = _percent;
    }

    debounceIns(() => {
        emits("resize", e);
    });
}
</script>
<script>
export default {
    name: "Split",
};
</script>


<style lang="less">
#split_window {
    .clearfix:after {
        visibility: hidden;
        display: block;
        font-size: 0;
        content: " ";
        clear: both;
        height: 0;
    }

    .split_container {
        height: 100%;
        position: relative;
    }

    .split_container:not(:has(.Resizer)) {
        .split_pane {
            padding: 0 !important;
        }
    }
}
</style>

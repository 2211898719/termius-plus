<template>
    <div class="Resizer" :class="split"></div>
</template>
<script setup>
const { split } = defineProps(["split"]);

</script>
<script>
export default {};
</script>

<style lang="less">
#split_window {
    .Resizer {
        -moz-box-sizing: border-box;
        -webkit-box-sizing: border-box;
        box-sizing: border-box;
        position: absolute;
        -moz-background-clip: padding;
        -webkit-background-clip: padding;
        background-clip: padding-box;
        z-index: 1000;
        background: #858585;
        transition: background 0.25s ease-out, border 0.25s ease-out;
    }
    .Resizer:hover {
        background: rgb(243, 243, 243);
        border-color: darkgrey;
    }

    .Resizer:after {
        content: "";
        display: block;
        position: absolute;
        background: #fff;
    }

    .Resizer:hover:after {
        background: darkgrey;
    }

    .Resizer.vertical:after {
        top: 0;
        left: 50%;
        width: 24px;
        height: 2px;
    }

    .Resizer.horizontal:after {
        top: 50%;
        left: 0;
        width: 2px;
        height: 24px;
    }

    .Resizer.vertical {
        cursor: row-resize;
        width: 100%;
        height: 4px;
        border: 1px #858585 solid;
        margin-top: -2px;
    }

    .Resizer.horizontal {
        height: 100%;
        cursor: col-resize;
        width: 4px;
        border: 1px #858585 solid;
        margin-left: -2px;
    }
}
</style>

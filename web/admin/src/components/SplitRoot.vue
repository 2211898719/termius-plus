<template>
  <VueDragSplit
      class="drag-root"
      :generateWindowConfig="generateWindowConfig"
      v-model:windowListSync="windowList"
      v-model:activeTabKeySync="activeTabKey"
  >
    <template #Tab="win">
            <p style="color: white; font-size: 12px">{{ win.label }}</p>
      <p></p>
    </template>
    <template #CloseBtn>
      <span></span>
    </template>
    <template #AddBtn>
      <span></span>
    </template>
    <!--    <template #TabActions>-->
    <!--      <span></span>-->
    <!--    </template>-->
    <!--    <template #placeHolder>-->
    <!--      <span></span>-->
    <!--    </template>-->
    <template #TabView="win">
      <span>
      <slot name="content" :win="win"></slot>
      </span>
    </template>
  </VueDragSplit>
</template>

<script setup>
import {ref} from "vue";

// 引入样式文件
import "vue-drag-split/dist/style.css";
// 引入组件
import {VueDragSplit} from "vue-drag-split";

const activeTabKey = ref("");
const windowList = ref([
  {
    key: "1",
    label: "标签一",
  },
]);
function generateWindowConfig(params) {
  return {
    key: Date.now(),
    label: "标签" + Date.now(),
  };
}
</script>
<script>
export default {};
</script>

<style lang="less">
.header_item{
  max-width: none !important;
}

#split_window .split_view .split_content_wrapper .split_view_label_wrapper .split_view_label_box .header_item p, #split_window .split_view .split_content_wrapper .split_view_label_wrapper .split_view_label_box .header_item span{
  margin: 0;
  text-align: center;
}

.drag-root {
  height: calc(@height - 100px) !important;
}

.split_view_content {
  background-color: #000 !important;
}

#split_window .drag_modal_wrapper {
  background-color: #1daa6c !important;
  //透明度
  opacity: 0.6 !important;
  //毛玻璃
  backdrop-filter: blur(30px) !important;
}

#split_window .split_view .split_content_wrapper .split_view_label_wrapper .split_view_label_box .header_item.label_active {
  background-color: #27664C !important;
}

</style>

<template>
  <VueDragSplit
      style="height: 650px"
      :generateWindowConfig="generateWindowConfig"
      v-model:windowListSync="windowList"
      v-model:activeTabKeySync="activeTabKey"
  >
    <template #Tab="win">
      <p style="color: white; font-size: 12px">{{ win.label }}</p>
    </template>
    <template #CloseBtn>
      <span></span>
    </template>
<!--    <template #AddBtn>-->
<!--      <span></span>-->
<!--    </template>-->
    <template #TabActions>
      <span></span>
    </template>
    <template #placeHolder>
      <span></span>
    </template>
    <template #TabView="win">
      <keep-alive>
      <slot name="content" :win="win"></slot>
      </keep-alive>
    </template>
  </VueDragSplit>
</template>

<script setup>
import {ref} from "vue";

// 引入样式文件
import "vue-drag-split/dist/style.css";
// 引入组件
import { VueDragSplit } from "vue-drag-split";

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

<style>
.header_item{
  max-width: none !important;
}

#split_window .split_view .split_content_wrapper .split_view_label_wrapper .split_view_label_box .header_item p, #split_window .split_view .split_content_wrapper .split_view_label_wrapper .split_view_label_box .header_item span{
  margin: 0;
  text-align: center;
}
</style>

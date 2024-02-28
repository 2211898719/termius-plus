<script setup>
import {defineEmits, defineProps} from "vue";

let props = defineProps({
  tree: {
    type: [Object], required: true,
  }
})

let emit = defineEmits(['dragContentOver'])

const handle = _.throttle((event, tree) => {
      emit('dragContentOver', event, tree)
    },
    200,
    {leading: true, trailing: false}
)

const submitTop = (event, tree) => {
  emit('dragContentOver', event, tree)
}
</script>

<template>
  <div draggable="true" @dragover.prevent="handle($event,tree)" v-if="!Array.isArray(tree.children)">
    <div style="height: 100px;width: 100px">
      {{ tree.content }}
    </div>
  </div>
  <split-box :class="tree.layout" v-else>
    <p-split-box-tree @dragContentOver="submitTop" v-for="item in tree.children" :key="item.name" :tree="item"></p-split-box-tree>
  </split-box>
</template>

<style scoped lang="less">

//拖拽分屏预览大小蒙层
.drag-mask-left:after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 50%;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 999;
}

.drag-mask-right:after {
  content: '';
  position: absolute;
  top: 0;
  left: 50%;
  right: 0;
  bottom: 0;
  width: 50%;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 999;
}

.drag-mask-top:after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 50%;
  height: 50%;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 999;
}

.drag-mask-bottom:after {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  bottom: 0;
  height: 50%;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 999;
}
</style>

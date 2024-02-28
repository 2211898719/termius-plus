<script setup>
import {ref} from "vue";
import PSplitBoxTree from "@/components/p-split-box-tree.vue";


let tabSortAnimationTime = ref(200);

let tabData = ref([
  {
    name: 'first1',
    content: 'first1',
    children: [
      {
        name: 'first1-1',
        content: 'first1-1'
      },
      {
        name: 'first1-2',
        content: 'first1-2'
      }
    ]
  },
  {
    name: 'first2',
    content: 'first2'
  },
  {
    name: 'first3',
    content: 'first3'
  },
  {
    name: 'first4',
    content: 'first4'
  }
]);

let activeName = ref('first1');

const handleChangeActiveName = (val) => {
  activeName.value = val;
}


//~~~~~~~~~~~~~~~~~~~~~~~~~~~~tab拖拽~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
let currentDragIndex = ref(null);

const dragStart = (item, index) => {
  currentDragIndex.value = index;
  console.log('dragStart', item);
}


const dragOver = _.throttle(
    (item, index) => {
      if (currentDragIndex.value === null) return;
      const dragItem = tabData.value[currentDragIndex.value];
      tabData.value.splice(currentDragIndex.value, 1);
      tabData.value.splice(index, 0, dragItem);
      currentDragIndex.value = index;
    },
    tabSortAnimationTime.value,
    {leading: true, trailing: false}
);

let dragContentOverSet = new Set();
let endTargetItem = null;


const dragContentOver = _.throttle((e, item) => {
      console.log('dragContentOver', item, e.target);
      //根据在元素上的位置上下左右分屏添加一个.drag-mask-xxx样式 中间20%的区域区分上下
      const {clientX, clientY} = e;
      const {left, top, width, height} = e.target.getBoundingClientRect();
      const x = clientX - left;
      const y = clientY - top;
      const xPercent = x / width;
      const yPercent = y / height;
      e.target.classList.remove('drag-mask-left', 'drag-mask-right', 'drag-mask-top', 'drag-mask-bottom');
      if (xPercent < 0.3) {
        e.target.classList.add('drag-mask-left');
        item.dragFlag = 'left';
      } else if (xPercent > 0.7) {
        e.target.classList.add('drag-mask-right');
        item.dragFlag = 'right';
      } else if (yPercent < 0.3) {
        e.target.classList.add('drag-mask-top');
        item.dragFlag = 'top';
      } else if (yPercent > 0.7) {
        e.target.classList.add('drag-mask-bottom');
        item.dragFlag = 'bottom';
      }
      dragContentOverSet.add(e.target);
      endTargetItem = item;
    },
    tabSortAnimationTime.value,
    {leading: true, trailing: false}
);

const dragEnd = () => {
  //没有拖拽的目标，不做任何操作
  if (!endTargetItem) {
    return;
  }

  const dragItem = tabData.value[currentDragIndex.value];
  //如果拖拽的目标是自己，不做任何操作
  if (endTargetItem === dragItem) {
    return;
  }

  //根据endTargetItem 和 dragItem 的位置关系，进行分屏操作
  if (endTargetItem.children) {
    endTargetItem.children.push(dragItem);
  } else {
    //根据dragFlag计算layout和children中的顺序
    let newEndItem = JSON.parse(JSON.stringify(endTargetItem));
    if (endTargetItem.dragFlag === 'left') {
      endTargetItem.layout = 'flex-row';
      endTargetItem.children = [dragItem, newEndItem];
    } else if (endTargetItem.dragFlag === 'right') {
      endTargetItem.layout = 'flex-row';
      endTargetItem.children = [newEndItem, dragItem];
    } else if (endTargetItem.dragFlag === 'top') {
      endTargetItem.layout = 'flex-col';
      endTargetItem.children = [dragItem, newEndItem];
    } else if (endTargetItem.dragFlag === 'bottom') {
      endTargetItem.layout = 'flex-col';
      endTargetItem.children = [newEndItem, dragItem];
    }

    endTargetItem.name = endTargetItem.name + '-'+dragItem.name;
  }

  console.log('dragEnd', tabData.value);

  endTargetItem.dragFlag = null;
  //善后工作
  endTargetItem = null;
  currentDragIndex.value = null;
  dragContentOverSet.forEach((item) => {
    item.classList.remove('drag-mask-left', 'drag-mask-right', 'drag-mask-top', 'drag-mask-bottom');
  });
}

/**
 * 嵌套的split-box组件可以视为一个n叉树，每个节点都是一个split-box组件,拖拽操作就是在这个树上进行的
 */

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
</script>

<template>
  <div class="split">
    <div class="navigation" v-auto-animate="{ duration: tabSortAnimationTime }">
      <div draggable="true"
           v-for="(item, index) in tabData"
           :key="item.name"
           @dragstart="dragStart(item,index)"
           @dragover.prevent="dragOver(item,index)"
           @dragend="dragEnd()"
           :class="{item:true,active:activeName===item.name}"
           @click="handleChangeActiveName(item.name)">
        {{ item.name }}
      </div>
    </div>
    <div class="content">
      <div v-for="item in tabData" :key="item.name"
           :class="{'content-item':true,'content-item-active':activeName===item.name}">
        <p-split-box-tree @dragContentOver="dragContentOver" :tree="item"></p-split-box-tree>
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
.split {
  display: flex;


  .navigation {
    .item {
      background-color: #f50000;
      padding: 10px;
      margin: 10px;
      cursor: pointer;
    }

    .active {
      background-color: #3fff02;
    }
  }

  .content {
    background-color: #1daa6c;
    position: relative;
    pointer-events: auto;

    .title {
      background-color: #f50000;
      padding: 10px;
      margin: 10px;
      //抓手
      cursor: grab;
    }

    &-item {
      height: 0;
      overflow: hidden;

      &-active {
        height: auto;
      }
    }

  }


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
}
</style>

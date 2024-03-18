<script setup>

</script>

<template>
  <div>
    1231312312311111111111
  </div>
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

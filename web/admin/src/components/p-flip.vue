<template>
  <div class="flip-container">
    <div :class="{flipped: flipped}" class="flipper">
      <div class="front">
        <slot name="front">

        </slot>
      </div>
      <div class="back">
        <slot name="back"></slot>
      </div>
    </div>
  </div>
</template>

<script setup>

import {defineExpose, defineProps, ref} from "vue";

const props = defineProps({
  operationId: {
    type: [String], default: undefined,
  },
});

let flipped = ref(false)

const flip = () => {
  flipped.value = !flipped.value
}

defineExpose({
  flip,
  operationId: props.operationId,
})

</script>
<style lang="less">


.flip-container {
  perspective: 1000px;
  height: 100%;
}

.flipper {
  width: 100%;
  height: 100%;
  overflow: hidden;
  position: relative;
  transform-style: preserve-3d;
  transition: all 0.9s ease-in-out;
}

.front, .back {
  position: absolute;
  top: 0;
  left: 0;
  backface-visibility: hidden;
}

.front {
  width: 100%;
  height: 100%;
  overflow: hidden;
  transform: rotateY(0deg);
  transform-origin: calc(50%) calc(50%);
  transition: all 0.9s ease-in-out;
}

.back {
  width: 100%;
  height: 100%;
  //overflow: hidden;
  transform: rotateY(-180deg);
  transform-origin: calc(50%) calc(50%);
  transition: all 0.9s ease-in-out;

  .ant-card {
    overflow: hidden;
  }
}

.flipped .front {
  transform: rotateY(180deg);

}

.flipped .back {
  transform: rotateY(0deg);
  overflow: auto;

  .ant-card-body {
    overflow: scroll;
  }
}
</style>

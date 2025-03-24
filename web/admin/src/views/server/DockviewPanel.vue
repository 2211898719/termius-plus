<script setup>
import {onMounted, ref} from "vue";

let props = defineProps({
  params: {
    type: Object
  },
});

let container = ref(null);

onMounted(() => {
  if (props.params.params.el) {
    container.value.appendChild(props.params.params.el)
  }
})

</script>

<template>
  <div style="height: 100%" ref="container">
  </div>
</template>

<style scoped lang="less">

</style>
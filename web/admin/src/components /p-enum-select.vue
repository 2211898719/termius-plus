<template>
  <a-select  v-model:value="currentValue" v-if="module" :bordered="true"
            @change="handleChange">
    <template v-for="item in Object.getOwnPropertyNames(module)" :key="module[item]?.value">
      <a-select-option :value="module[item]?.value" v-if="module[item] instanceof module">
        {{ module[item]?.name }}
      </a-select-option>
    </template>
  </a-select>
</template>

<script setup>

import {ref, watch, defineProps, defineEmits} from "vue";

const props = defineProps({
  value: {
    type: [String,Number], default: '',
  },
  module: {
    type: null, default: null,
  },
})

let currentValue = ref(props.value)
watch(() => props.value, (val) => {
  currentValue.value = val;
})

const emit = defineEmits(['update:value', 'change'])

const handleChange = (e) => {
  emit("update:value", e);
  emit("change", e);
}

</script>

<style scoped>

</style>

<template>
  <a-cascader
      v-model:value="currentValue"
      :defaultValue="currentValue"
      :options="options"
      :fieldNames="fieldNames"
      @change="handleChange"
  ></a-cascader>
</template>

<script setup>
import {reactive, ref, watch} from 'vue';
import {findNodePath} from "@/utils/treeUtil";

const props = defineProps({
  value: {
    type: [Number,String], default: null,
  },
  module: {
    type: null, default: null,
  },
  fieldNames: {
    type: Object,
    default: () => {
      return {
        children: "children",
        value: "id",
        label: "name",
      };
    },
  },
  api: {
    type: Function, default: async () => {
    }
  }
})

const options = ref([]);

let currentValue = reactive([])

const loadCurrentValue = () => {
  if (props.value && options.value.length) {
    currentValue = findNodePath(options.value, props.value);
  }
}

loadCurrentValue();

const getOptionsList = async () => {
  options.value = await props.api()

  loadCurrentValue()
}

getOptionsList()


watch(() => props.value, (val) => {
  if (!val) {
    currentValue = []
    return
  }

  loadCurrentValue()
})

const emit = defineEmits(['update:value', 'change'])

const handleChange = (e, selectedOptions) => {
  if (!e) {
    emit("update:value", null);
    emit("change", null)
    return
  }

  emit("update:value", e[e.length - 1]);
  emit("change", e[e.length - 1])
}


</script>

<style scoped>

</style>

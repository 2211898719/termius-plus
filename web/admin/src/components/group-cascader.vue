<template>
  <a-cascader
      v-model:value="currentValue"
      :defaultValue="currentValue"
      :options="options"
      :fieldNames="fieldNames"
      @change="handleChange"
      change-on-select
  />
</template>

<script setup>
import {defineEmits, defineProps, reactive, ref, watch} from 'vue';
import {findNodePath, walk} from "@/utils/treeUtil";
import {serverApi} from "@/api/server";

const props = defineProps({
  value: {
    type: [Number, String], default: null,
  },
  module: {
    type: null, default: null,
  },
  disabled: {
    type: [Number, String], default: null,
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
    type: Function,
    default: serverApi.groupList
  }
})

const options = ref([]);

let currentValue = reactive([])

const loadCurrentValue = () => {
  if (props.value != null && options.value.length) {
    currentValue = findNodePath(options.value, props.value);
    if (props.disabled != null) {
      walk(options.value, (node) => {
        if (node.id === props.disabled) node.disabled = true
      });
    }
  }
}

loadCurrentValue();

const getOptionsList = async () => {
  options.value = await props.api()

  loadCurrentValue()
}

getOptionsList()


watch(() => props.value, (val) => {
  if (val == null) {
    currentValue = []
    return
  }

  loadCurrentValue()
})

const emit = defineEmits(['update:value', 'change'])

const handleChange = (e) => {
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

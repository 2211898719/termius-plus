<template>
  <a-select style="min-width: 200px" v-model:value="currentValue" :placeholder="placeholder" :bordered="true"
            @change="handleChange" :disabled="disabled" :mode="mode" :max-tag-count="maxTagCount" show-search allow-clear>
    <template v-for="item in data" :key="item[optionKey]">
      <a-select-option :value="item[optionKey]"> {{
          item[label]
        }}
        <template v-if="subLabel">
          ({{ item[subLabel] }})
        </template>
      </a-select-option>
    </template>
  </a-select>
</template>

<script setup>
import {ref, watch, defineProps, defineEmits, defineExpose} from "vue";
import _ from "lodash";

const props = defineProps({
  value: {
    type: [String, Number, Array], default: undefined,
  },
  api: {
    type: Function, default: async () => {
    },
  },
  label: {
    type: String, default: "name"
  },
  subLabel: {
    type: String, default: ""
  },
  optionKey: {
    type: String, default: "id"
  },
  placeholder: {
    type: String, default: ""
  },
  autoGetData: {
    type: Boolean, default: true
  },
  disabled: {
    type: Boolean, default: false,
  },
  mode: {
    type: String, default: null
  },
  maxTagCount: {
    type: Number, default: null
  }
})

let data = ref([])

let idMap = ref([])
const getData = async (params = {}) => {
  data.value = await props.api(params)
  idMap.value = _.keyBy(data.value, 'id');
}
if (props.autoGetData) {
  getData()
}

watch(() => props.api, () => {
  if (props.autoGetData) {
    getData()
  }
},)

let currentValue = ref();
if (props.value){
  currentValue.value = props.value
}

watch(() => props.value, (val) => {
  if (_.isNil(val)) {
    console.log(11111)
    currentValue.value = undefined
    return
  }

  currentValue.value = val;
})

const emit = defineEmits(['update:value', 'change'])


const handleChange = (e) => {
  emit("update:value", e);
  emit("change", e, idMap.value[e])
}

defineExpose({
  getData,
  data
})
</script>

<style>

</style>

<script setup>

import {message} from "ant-design-vue";
import {getCurrentInstance, ref} from "vue";
import {useStorage} from "@vueuse/core";

let frontColor = useStorage("frontColor", "#ffffff")
let backColor = useStorage("backColor", "#000000")

let channel = new BroadcastChannel("theme")

channel.onmessage = (e) => {
  frontColor.value = e.data.frontColor
  backColor.value = e.data.backColor
}

//把样式保存到本地
const saveStyle = () => {
  channel.postMessage({frontColor: frontColor.value, backColor: backColor.value})
  message.success("保存成功");
}

let themes = ref([
  {name: '默认', frontColor: '#ffffff', backColor: '#000000'},
  {
    name: 'Solarized Dark',
    frontColor: '#93a1a1',
    backColor: '#002b36'
  },
  {
    name: 'Monokai',
    frontColor: '#f8f8f2',
    backColor: '#272822'
  },
  {
    name: 'Dracula',
    frontColor: '#f8f8f2',
    backColor: '#282a36'
  }, {
    name: 'Nord',
    frontColor: '#d8dee9',
    backColor: '#2e3440'
  },
  {
    name: 'One Dark',
    frontColor: '#abb2bf',
    backColor: '#282c34'
  }, {
    name: 'Material ',
    frontColor: '#c5c8c6',
    backColor: '#1d1f21'
  }, {
    name: 'Tomorrow ',
    frontColor: '#c5c8c6',
    backColor: '#1d1f21'
  }, {
    name: 'Zenburn',
    frontColor: '#dcdccc',
    backColor: '#3f3f3f'
  },
  {
    name: 'Termius',
    frontColor: '#00CC74',
    backColor: '#141729'
  }
])

const selectThemes = (theme) => {
  frontColor.value = theme.frontColor
  backColor.value = theme.backColor
  saveStyle()
}

const {proxy} = getCurrentInstance()
let fonts = ref(proxy.$allFonts)
console.log(fonts.value)

</script>

<template>
  <a-space direction="vertical" size="middle" style="width: 100%;">
    <a-card style="width: 100%;">
      <a-card title="主题色">
        <a-card-grid @click="selectThemes(item)"
                     :style="`width: 15%; text-align: center; background-color: ${item.backColor}; color: ${item.frontColor}`"
                     v-for="item in themes" :key="item.backColor" :title="item.name">
          {{ item.name }}

        </a-card-grid>

      </a-card>
      <a-card title="字体"  class="font-card">
        <a-card-grid @click="selectThemes(item)"
                     :style="`font-family: '${item.name}', Arial, sans-serif`"
                     class="font-item"
                     v-for="item in fonts" :key="item.name" :title="item.name">
          {{ item.name }}
        </a-card-grid>
      </a-card>
      <a-form :label-col="{ span: 4 }"
              :wrapper-col="{ span: 20 }"
              style="margin-top: 30px"
              autocomplete="off">
        <a-form-item label="字体颜色">
          <input type="color" v-model="frontColor"/>
        </a-form-item>
        <a-form-item label="背景颜色">
          <input type="color" v-model="backColor"/>
        </a-form-item>
        <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
          <a-button type="primary" @click.prevent="saveStyle">保存</a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </a-space>
</template>

<style scoped lang="less">
:deep(.font-card){
  max-width: 100%;
  .ant-card-body{
    display: flex;
    flex-wrap: wrap;

    .font-item{
      width: 15%;
      height: 100px;
      padding: 8px !important;
      text-overflow: ellipsis;
      text-align: center;
      overflow: hidden;
    }
  }

}

</style>

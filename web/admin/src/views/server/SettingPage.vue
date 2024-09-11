<script setup>

import {message} from "ant-design-vue";
import {getCurrentInstance, ref} from "vue";
import {useStorage} from "@vueuse/core";
import PTermLog from "@/components/p-term-log.vue";

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
let currentFont = useStorage("currentFont", "JetBrainsMono-Bold")

let fontChannel = new BroadcastChannel("font")

const selectFont = (font) => {
  currentFont.value = font.name
  frontFamilyView.value = true
  frontFamilyViewFront.value = font.name
  fontChannel.postMessage(font.name)
  message.success("保存成功");
}

fontChannel.onmessage = (e) => {
  currentFont.value = e.data
}


let frontFamilyView = ref(false)
let frontFamilyViewFront = ref('')

import viewData from "@/assets/view.json";

let aiSystem = useStorage("aiSystem", "你是一个专业的linux命令生成器，以markDown形式回答命令生成结果。")

let aiMessagePrefix = useStorage("aiMessagePrefix", "{{commandLog}} \n写一条linux命令，")

let aiSettingChannel = new BroadcastChannel("aiSetting")

const saveAI = () => {
  aiSettingChannel.postMessage({aiSystem: aiSystem.value, aiMessagePrefix: aiMessagePrefix.value})
  message.success("保存成功");
}

aiSettingChannel.onmessage = (e) => {
  aiSystem.value = e.data.aiSystem
  aiMessagePrefix.value = e.data.aiMessagePrefix
}

</script>

<template>
  <a-space class="content-wrapper" direction="vertical" size="middle" style="width: 100%;">
    <a-card style="width: 100%;">
      <a-card title="主题色">
        <a-card-grid @click="selectThemes(item)"
                     :style="`width: 15%; text-align: center; background-color: ${item.backColor}; color: ${item.frontColor}`"
                     v-for="item in themes" :key="item.backColor" :title="item.name">
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
      <a-card title="字体" class="font-card">
        <a-card-grid @click="selectFont(item)"
                     :style="`font-family: '${item.name}', Arial, sans-serif`"
                     :class="{'font-item':true, 'font-selected': item.name === currentFont}"
                     v-for="item in fonts" :key="item.name" :title="item.name">
          {{ item.name }}
        </a-card-grid>
      </a-card>
      <a-card title="AI提示词" class="font-card">
        <a-form :label-col="{ span: 4 }"
                :wrapper-col="{ span: 20 }"
                style="margin-top: 30px"
                autocomplete="off">
          <a-form-item label="AI系统提示词">
            <a-textarea auto-size v-model:value="aiSystem"/>
          </a-form-item>
          <a-form-item label="AI消息前缀">
            <a-textarea auto-size v-model:value="aiMessagePrefix"/>
          </a-form-item>
          <a-form-item label="说明">
            <div v-pre>
              {{commandLog}}用于插入终端上下文信息
            </div>
          </a-form-item>

          <a-form-item :wrapper-col="{ span: 14, offset: 4 }">
            <a-button type="primary" @click.prevent="saveAI">保存</a-button>
          </a-form-item>
        </a-form>
      </a-card>
    </a-card>
    <a-drawer title="字体预览" width="80%" v-model:visible="frontFamilyView" placement="right">
      <p-term-log v-if="frontFamilyView" :command-data="viewData.log" :font-family="frontFamilyViewFront"></p-term-log>
    </a-drawer>
  </a-space>
</template>

<style scoped lang="less">
.content-wrapper {
  height: @height;
  overflow: scroll;
}

:deep(.font-card) {
  margin-top: 20px;

  .ant-card-body {
    max-width: 100%;
    //栅格布局
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    grid-gap: 8px;

    &:before {
      display: none;
    }

    .font-item {
      width: 200px;
      height: 100px;
      //padding: 8px !important;

    }

    .font-selected {
      border: 1px solid #1890ff;
    }
  }

}

</style>

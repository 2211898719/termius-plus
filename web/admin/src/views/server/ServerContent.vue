<script setup>
import PFlip from "@/components/p-flip.vue";
import PSftp from "@/components/p-sftp.vue";
import {message} from "ant-design-vue";
import {defineExpose, defineProps, nextTick, ref} from "vue";
import {commandApi} from "@/api/command";
import PTerm from "@/components/p-term.vue";
import "@/components/VueDragSplit/style.css";
import VueDragSplit from "@/components/VueDragSplit/index.vue";
import linuxDoc from "@/assets/linux-doc.json";
import {useStorage} from "@vueuse/core";

let searchLinuxDoc = ref(JSON.parse(JSON.stringify(linuxDoc)))

const activeTabKey = ref("");
const windowList = ref([
  {
    key: "1",
    label: "标签一",
  },
]);

function generateWindowConfig(params) {
  console.log(params)
  return {
    key: Date.now(),
    label: "标签" + Date.now(),
  };
}

const props = defineProps({
  server: {
    type: [Object], default: () => {
    },
  }
})


const remarkStatus = ref(false)

let flipStatus = ref(false)

// let fullscreenContent = ref(null)
//
// let frontColor = useStorage('frontColor', "#ffffff")
// let backColor = useStorage('backColor', "#000000")


let sftpEnable = ref(false)

let flip = ref(null)

// let link = ref(true)


let sftpEl = ref([])

const reloadSftp = () => {
  sftpEl.value.init()
}

const changeDir = (dir) => {
  if (sftpEnable.value) {
    sftpEl.value.changeDir(dir)
  }
}

let PTermRef = ref(null)
const reloadServer = () => {
  PTermRef.value.reload()
}

let fullscreenRef = ref(null)

const handleRequestFullscreen = () => {
  fullscreenRef.value.$el.requestFullscreen()
}

const handleExecCommand = (command) => {
  // iframeRef.value.contentWindow.postMessage({command: command}, '*')
  PTermRef.value.execCommand(command)
}

const changeSftpEnable = () => {
  flipStatus.value = !flipStatus.value
  if (!sftpEnable.value) {
    sftpEnable.value = true;
    message.success("开启sftp成功");
  }

  nextTick(() => {
    flip.value.flip()
  })
}

let rightTabKey = ref('remark')

const commandData = ref([])

const getCommandData = async () => {
  commandData.value = await commandApi.list()
}

let activeKey = ref("")

getCommandData()

defineExpose({
  handleExecCommand,
  changeDir,
  server: props.server,
  focus: () => {
    PTermRef.value.focus()
  },
  close: () => {
    PTermRef.value.close()
  },
})

let backColor = useStorage('backColor', "#000000")

let linuxDocSearch = ref("")
//高亮搜索
const handleChangeSearch = (e) => {
  linuxDocSearch.value = e.target.value
  let search = e.target.value
  searchLinuxDoc.value = JSON.parse(JSON.stringify(linuxDoc))

  searchLinuxDoc.value = searchLinuxDoc.value.filter(item => {
    return item.title.includes(search) || item.des.includes(search)
  })

  //标题和描述按照关键字相似度排序
  searchLinuxDoc.value.sort((a, b) => {
    return (a.title+a.des).indexOf(search) - (b.title+b.des).indexOf(search)
  })

   searchLinuxDoc.value.forEach(item => {
    item.title = item.title.replace(search, `<span style="color: #1890ff">${search}</span>`)
    item.des = item.des.replace(search, `<span style="color: #1890ff">${search}</span>`)
  })

}


</script>

<template>
  <div class="split-box">
    <p-flip ref="flip" :operation-id="server.operationId">
      <template #back>
        <div v-if="sftpEnable">
          <div class="sftp-content">
            <a-card title="sftp">
              <template #extra>
                <a-button type="link" style="display: flex;justify-content: center;align-items: center"
                          :class="{green:sftpEnable}" @click="changeSftpEnable">
                  <template v-slot:icon>
                    <svg t="1696435355552" class="tags" viewBox="0 0 1024 1024" version="1.1"
                         xmlns="http://www.w3.org/2000/svg" p-id="19507" width="200" height="200">
                      <path
                          d="M972.8 249.856h-14.336l-0.512-108.032c0-25.6-20.992-45.568-46.08-45.568l-413.184 2.56h-3.584l-23.552-25.088c-9.728-10.24-23.04-16.384-37.376-16.384l-381.952-0.512C24.064 56.832 1.536 79.36 1.024 107.52L0 914.432c0 13.824 5.12 26.624 14.848 36.352 9.728 9.728 22.528 14.848 36.352 15.36l921.088 1.024c28.16 0 51.2-22.528 51.2-51.2l0.512-614.912c0-28.16-23.04-50.688-51.2-51.2z m-105.984-61.44l0.512 61.44-232.96-0.512-55.296-59.392 287.744-1.536zM921.088 865.28L102.4 864.256 108.032 158.72l303.616 0.512 162.816 176.128c9.728 10.24 23.04 16.384 37.376 16.384l310.272 0.512-1.024 513.024z"
                          p-id="19508"></path>
                      <path
                          d="M531.968 441.344c-9.216 1.536-17.408 7.168-22.528 15.36-9.728 15.872-6.144 36.864 8.192 48.128l28.16 22.016-183.808 1.024c-18.432 0-33.28 16.384-33.28 35.84s15.36 35.328 33.792 35.328l284.16-1.536c18.432 0 33.28-16.384 33.28-35.84 0-9.728-4.096-18.944-10.752-25.6-1.536-2.048-3.584-4.096-5.632-5.632l-106.496-82.944c-7.168-5.632-15.872-7.68-25.088-6.144zM647.168 639.488l-283.648 2.048c-18.432 0-33.28 16.384-33.28 35.84 0 9.728 4.096 18.944 10.752 25.6 1.536 2.048 3.584 4.096 5.632 5.632l106.496 82.944c5.632 4.608 12.8 6.656 19.968 6.656 11.264 0 21.504-6.144 27.648-15.872 4.608-7.68 6.656-16.896 5.12-25.6-1.536-9.216-6.144-17.408-13.312-22.528l-28.16-22.016 183.808-1.024c18.432 0 33.28-16.384 33.28-35.84-1.024-19.968-15.872-35.84-34.304-35.84z"
                          p-id="19509"></path>
                    </svg>
                  </template>
                </a-button>
                <div class="center-name">{{ server.name }}</div>

                <a-button type="link" @click="reloadSftp">
                  <template v-slot:icon>
                    <reload-outlined/>
                  </template>
                </a-button>

              </template>
              <p-sftp class="sftp" ref="sftpEl" :operation-id="server.operationId"
                      :server-id="server.id"></p-sftp>
            </a-card>
          </div>
        </div>
      </template>
      <template #front>
        <div :class="{'ssh-content':true}">
          <a-card title="终端">
            <template #extra>
              <a-button type="link" style="display: flex;justify-content: center;align-items: center"
                        :class="{green:sftpEnable}" @click="changeSftpEnable">
                <template v-slot:icon>
                  <svg t="1696435355552" class="tags" viewBox="0 0 1024 1024" version="1.1"
                       xmlns="http://www.w3.org/2000/svg" p-id="19507" width="200" height="200">
                    <path
                        d="M972.8 249.856h-14.336l-0.512-108.032c0-25.6-20.992-45.568-46.08-45.568l-413.184 2.56h-3.584l-23.552-25.088c-9.728-10.24-23.04-16.384-37.376-16.384l-381.952-0.512C24.064 56.832 1.536 79.36 1.024 107.52L0 914.432c0 13.824 5.12 26.624 14.848 36.352 9.728 9.728 22.528 14.848 36.352 15.36l921.088 1.024c28.16 0 51.2-22.528 51.2-51.2l0.512-614.912c0-28.16-23.04-50.688-51.2-51.2z m-105.984-61.44l0.512 61.44-232.96-0.512-55.296-59.392 287.744-1.536zM921.088 865.28L102.4 864.256 108.032 158.72l303.616 0.512 162.816 176.128c9.728 10.24 23.04 16.384 37.376 16.384l310.272 0.512-1.024 513.024z"
                        p-id="19508"></path>
                    <path
                        d="M531.968 441.344c-9.216 1.536-17.408 7.168-22.528 15.36-9.728 15.872-6.144 36.864 8.192 48.128l28.16 22.016-183.808 1.024c-18.432 0-33.28 16.384-33.28 35.84s15.36 35.328 33.792 35.328l284.16-1.536c18.432 0 33.28-16.384 33.28-35.84 0-9.728-4.096-18.944-10.752-25.6-1.536-2.048-3.584-4.096-5.632-5.632l-106.496-82.944c-7.168-5.632-15.872-7.68-25.088-6.144zM647.168 639.488l-283.648 2.048c-18.432 0-33.28 16.384-33.28 35.84 0 9.728 4.096 18.944 10.752 25.6 1.536 2.048 3.584 4.096 5.632 5.632l106.496 82.944c5.632 4.608 12.8 6.656 19.968 6.656 11.264 0 21.504-6.144 27.648-15.872 4.608-7.68 6.656-16.896 5.12-25.6-1.536-9.216-6.144-17.408-13.312-22.528l-28.16-22.016 183.808-1.024c18.432 0 33.28-16.384 33.28-35.84-1.024-19.968-15.872-35.84-34.304-35.84z"
                        p-id="19509"></path>
                  </svg>
                </template>
              </a-button>
              <div class="center-name">{{ server.name }}</div>
              <a-button type="link" @click="reloadServer">
                <template v-slot:icon>
                  <reload-outlined/>
                </template>
              </a-button>
            </template>

            <div style="display: flex">
              <div style="width: 100%;position: relative;">
                <VueDragSplit
                    ref="fullscreenRef"
                    class="drag-root"
                    :generateWindowConfig="generateWindowConfig"
                    v-model:windowListSync="windowList"
                    v-model:activeTabKeySync="activeTabKey"
                >
                  <template #Tab="win">
                    <p style="color: white; font-size: 12px">{{ win.label }}</p>
                    <p></p>
                  </template>
                  <template #CloseBtn>
                    <span></span>
                  </template>
                  <template #AddBtn>
                    <span></span>
                  </template>
                  <template #TabActions>
                    <span></span>
                  </template>
                  <template #placeHolder>
                    <span></span>
                  </template>
                  <template #TabView="win">
                    <span>
                      <p-term class="ssh" :server="server" ref="PTermRef"></p-term>
                    </span>
                  </template>
                </VueDragSplit>

                <div style="position: absolute;right: 16px;top: calc(50% - 1em / 2);color: aliceblue;z-index: 100"
                     @click="remarkStatus=!remarkStatus">
                  <left-outlined :class="{'button-action':remarkStatus,'left':true}"/>
                </div>
                <div style="position: absolute;right: 16px;top: 16px;color: aliceblue;z-index: 100" class="left"
                     @click="handleRequestFullscreen">
                  <fullscreen-outlined/>
                </div>
              </div>
              <div :class="{remark:true,'remark-enter':remarkStatus}" class="card-container">

                <a-tabs v-model:activeKey="rightTabKey" style="margin-left: 8px" type="card">
                  <a-tab-pane key="remark" tab="备注">
                    <div class="w-e-text-container">
                      <div data-slate-editor v-html="server.remark">

                      </div>
                    </div>
                  </a-tab-pane>
                  <a-tab-pane key="command" tab="命令" force-render>
                    <a-list style="padding: 8px" item-layout="horizontal" :data-source="commandData">
                      <template #renderItem="{ item }">
                        <a-list-item>
                          <a-list-item-meta>
                            <template #title>
                              {{ item.name }}
                            </template>
                            <template #avatar>

                              <mac-command-outlined style="color: #F6C445;"/>
                            </template>
                            <template #description>
                              <a-collapse v-model:activeKey="item.activeKey">
                                <a-collapse-panel key="1" :header="item.command">
                                  <div class="w-e-text-container">
                                    <div data-slate-editor v-html="item.remark">

                                    </div>
                                  </div>
                                  <template #extra>
                                    <a-popconfirm
                                        title="确定执行吗?"
                                        ok-text="Yes"
                                        cancel-text="No"
                                        @click.stop
                                        @confirm="handleExecCommand(item.command)"
                                    >
                                    <a-button type="link">执行</a-button>
                                    </a-popconfirm>
                                  </template>
                                </a-collapse-panel>
                              </a-collapse>
                            </template>
                          </a-list-item-meta>
                        </a-list-item>
                      </template>
                    </a-list>
                  </a-tab-pane>
                  <a-tab-pane key="linux-doc" tab="Linux文档">
                    <div class="linux-doc">
                      <a-input-search class="search" @change="handleChangeSearch" allow-clear></a-input-search>
                      <a-collapse v-model:activeKey="activeKey" accordion>
                        <a-collapse-panel v-for="item in searchLinuxDoc" :key="item.title">
                          <template #header>
                            <div >
                              <span class="title" v-html="item.title"></span>
                              <span class="des" v-html="item.des"></span>
                            </div>
                          </template>
                          <transition>
                            <div v-html="item.body" v-if="activeKey===item.title">
                            </div>
                          </transition>
                        </a-collapse-panel>
                      </a-collapse>
                    </div>
                  </a-tab-pane>
                </a-tabs>
              </div>
            </div>
          </a-card>
        </div>
      </template>
    </p-flip>
  </div>
</template>

<style scoped lang="less">

.split-box {
  min-height: auto;

  .ssh-content {
    .ssh {
      //width: 100%;
      //height: calc(@height - 120px);
      height: 100%;
      border: none;
      //resize: vertical; /* 可以调整宽度和高度 */
    }

    .remark {
      transition: all 0.3s;
      overflow: scroll;
      height: calc(@height - 100px);
      width: 0;
    }

    .remark-enter {
      width: 70%;
    }

    .left {
      transition: all 0.9s;
      mix-blend-mode: difference; /* 使用difference混合模式实现反色效果 */
      color: white; /* 设置图标的颜色为白色 */
      cursor: pointer; /* 设置鼠标样式为手型 */
    }

    .button-action {
      transform: rotateY(180deg);
    }
  }

  .sftp-content {

    .sftp {
      width: 100%;
      //height: 500px;
      border: none;
      resize: both; /* 可以调整宽度和高度 */
    }
  }
}


:deep(.w-e-text-container) {
  table {
    table-layout: fixed;
    width: 100% !important;
  }
}

:deep(.header_item) {
  max-width: none !important;
}

:deep(#split_window .split_view .split_content_wrapper .split_view_label_wrapper){
  display: none;
}

:deep(#split_window .split_view .split_content_wrapper .split_view_label_wrapper .split_view_label_box .header_item p, #split_window .split_view .split_content_wrapper .split_view_label_wrapper .split_view_label_box .header_item span) {
  margin: 0;
  text-align: center;
}

:deep(.drag-root) {
  height: calc(@height - 100px) !important;
}

:deep(#split_window .drag_modal_wrapper) {
  background-color: #1daa6c !important;
  //透明度
  opacity: 0.6 !important;
  //毛玻璃
  backdrop-filter: blur(30px) !important;
}

:deep(#split_window .split_view .split_content_wrapper .split_view_label_wrapper .split_view_label_box .header_item.label_active) {
  background-color: #27664C !important;
}

:deep(.linux-doc) {
  .search {
    margin-bottom: 16px;
  }

  .title {

  }

  .des {
    margin-left: 16px;
    color: #00000040;
  }

  h1, h2, h3, h4, h5, h6, p {
    margin-top: 8px;
  }

  .blockquote {

  }

  pre {
    background: rgb(242, 242, 242);
    color: rgb(102, 102, 102);
    padding: 20px;
  }

  ul, ol {
    list-style-position: inside;
  }

  pre code.hljs {
    display: block;
    overflow-x: auto;
    padding: 1em
  }

  code.hljs {
    padding: 3px 5px
  }

  .hljs {
    background: #f3f3f3;
    color: #444
  }

  .hljs-comment {
    color: #697070
  }

  .hljs-punctuation, .hljs-tag {
    color: #444a
  }

  .hljs-tag .hljs-attr, .hljs-tag .hljs-name {
    color: #444
  }

  .hljs-attribute, .hljs-doctag, .hljs-keyword, .hljs-meta .hljs-keyword, .hljs-name, .hljs-selector-tag {
    font-weight: 700
  }

  .hljs-deletion, .hljs-number, .hljs-quote, .hljs-selector-class, .hljs-selector-id, .hljs-string, .hljs-template-tag, .hljs-type {
    color: #800
  }

  .hljs-section, .hljs-title {
    color: #800;
    font-weight: 700
  }

  .hljs-link, .hljs-operator, .hljs-regexp, .hljs-selector-attr, .hljs-selector-pseudo, .hljs-symbol, .hljs-template-variable, .hljs-variable {
    color: #ab5656
  }

  .hljs-literal {
    color: #695
  }

  .hljs-addition, .hljs-built_in, .hljs-bullet, .hljs-code {
    color: #397300
  }

  .hljs-meta {
    color: #1f7199
  }

  .hljs-meta .hljs-string {
    color: #38a
  }

  .hljs-emphasis {
    font-style: italic
  }

  .hljs-strong {
    font-weight: 700
  }
}

:deep(#split_window .split_view .split_content_wrapper .split_view_content_wrapper .split_view_content) {
  background-color: v-bind(backColor);
}

</style>

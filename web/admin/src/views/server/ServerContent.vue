<script setup>
import PFlip from "@/components/p-flip.vue";
import PSftp from "@/components/p-sftp.vue";
import {message} from "ant-design-vue";
import {defineExpose, defineProps, nextTick, onMounted, ref} from "vue";
import {commandApi} from "@/api/command";
import PTerm from "@/components/p-term.vue";
import "@/components/VueDragSplit/style.css";
import linuxDoc from "@/assets/linux-doc.json";
import {useStorage} from "@vueuse/core";
import {useShepherd} from 'vue-shepherd'
import OsEnum from "@/enums/OsEnum";
import PRdp from "@/components/p-rdp.vue";

const emit = defineEmits(['hot'])

const onHot = (server) => {
  emit('hot', server)
}

let searchLinuxDoc = ref(JSON.parse(JSON.stringify(linuxDoc)))

const activeTabKey = ref("");
const windowList = ref([
  {
    key: "1",
    label: "标签一",
  },
]);

function generateWindowConfig(params) {
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
let pTermLoading = ref(false)

const reloadServer = () => {
  PTermRef.value.reload()
  inputTerm.value = false
}

let fullscreenRef = ref(null)

const handleRequestFullscreen = () => {
  fullscreenRef.value.$el.requestFullscreen()
}

const handleExecCommand = (command) => {
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

let pRdpEl = ref(null)

getCommandData()

defineExpose({
  handleExecCommand,
  changeDir,
  server: props.server,
  focus: () => {
    if (props.server.os === OsEnum.LINUX.value) {
      PTermRef.value.focus()
    }
  },
  close: () => {
    if (props.server.os === OsEnum.LINUX.value) {
      PTermRef.value.close()
    } else if (props.server.os === OsEnum.WINDOWS.value) {
      pRdpEl.value.close()
    }
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
    return (a.title + a.des).indexOf(search) - (b.title + b.des).indexOf(search)
  })

  searchLinuxDoc.value.forEach(item => {
    item.title = item.title.replace(search, `<span style="color: #1890ff">${search}</span>`)
    item.des = item.des.replace(search, `<span style="color: #1890ff">${search}</span>`)
  })

}

let inputTerm = ref(false)
const handleRequestInputTerm = () => {
  if (!inputTerm.value) {
    PTermRef.value.requestAuthEditSession()
  }
}

let autoComp = useStorage('autoComp', false)

const handleChangeComp = () => {
  autoComp.value = !autoComp.value
  PTermRef.value.setAutoComplete(autoComp.value)
}

let subSessionUsername = ref([])

const defaultConfig = {
  // 是否显示黑色遮罩层
  useModalOverlay: true,
  // 键盘按钮控制步骤
  keyboardNavigation: true,
  defaultStepOptions: {
    // 显示关闭按钮
    cancelIcon: {
      enabled: true
    },
    scrollTo: {behavior: 'smooth', block: 'center'},
    // 高亮元素四周要填充的空白像素
    modalOverlayOpeningPadding: 4,
    // 空白像素的圆角
    modalOverlayOpeningRadius: 4,
    buttons: [{
      action() {
        return this.back()
      },
      text: '上一步'
    }, {
      action() {
        return this.next()
      },
      text: '下一步'
    }]
  }
}

const tour = useShepherd({
  useModalOverlay: true,
  ...defaultConfig
});

let CompChangeEl = ref(null)
let SftpChangeEl = ref(null)
let reloadEl = ref(null)
let openPopover = ref(null)
let fullscreenEl = ref(null)

let hasSeenTour = useStorage('hasSeenTour', false)

onMounted(() => {
  if (hasSeenTour.value) {
    return
  }

  tour.addSteps([
    {
      attachTo: {element: document.getElementsByClassName("ant-tabs-tab-active")[0], on: 'right'},
      text: '服务器操作标签，可以拖拽排序，右键可以进行复制等操作',
    },
    {
      attachTo: {element: CompChangeEl.value, on: 'bottom'},
      text: '开启后可根据history提示最接近的命令,使用ctrl+w补全命令',
    },
    {
      attachTo: {element: SftpChangeEl.value, on: 'bottom'},
      text: '开启sftp,图形化管理文件',
    },
    {
      attachTo: {element: reloadEl.value, on: 'bottom'},
      text: '重新加载终端',
    },
    {
      attachTo: {element: openPopover.value, on: 'bottom'},
      text: '展开额外信息面板，包括备注、命令、Linux文档',
    },
    {
      attachTo: {element: fullscreenEl.value, on: 'bottom'},
      text: '全屏显示终端',
      buttons: [
        {
          action() {
            return this.back()
          },
          text: '上一步'
        },
        {
          action() {
            hasSeenTour.value = true
            return this.cancel()
          },
          text: '结束'
        }
      ]
    },
  ])

  tour.start();
});



</script>

<template>
  <div class="split-box">
    <div v-if="server.os===OsEnum.WINDOWS.value">
      <p-rdp ref="pRdpEl" :server="server"></p-rdp>
    </div>
    <p-flip v-else-if="server.os===OsEnum.LINUX.value" ref="flip" :operation-id="server.operationId">
      <template #back>
        <div v-if="sftpEnable">
          <div class="sftp-content">
            <a-card title="sftp">
              <template #extra>
                <div class="center-name">{{ server.name }}</div>

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
        <a-spin :spinning="pTermLoading" style="height: 100%">
          <div :class="{'ssh-content':true}">
<!--            <a-card :title="server.masterUserInfo?('观察'+server.masterUserInfo.username)+'的终端':'终端'" :body-style="{background:backColor}"  style="border:none">-->
<!--              <template #extra>-->
<!--                <div>-->
<!--                  <a-avatar-group :max-count="2" :max-style="{ color: '#f56a00', backgroundColor: '#fde3cf' }">-->
<!--                    <a-avatar :title="username" v-for="username in subSessionUsername" :key="username"-->
<!--                              style="background-color: #1890ff">-->
<!--                      {{ getSurname(username) }}-->
<!--                    </a-avatar>-->
<!--                  </a-avatar-group>-->
<!--                </div>-->
<!--                <div ref="CompChangeEl">-->
<!--                  <a-popover title="提示">-->
<!--                    <template #content>-->
<!--                      <p>根据history提示最接近的命令</p>-->
<!--                      <p>ctrl+w补全命令</p>-->
<!--                    </template>-->
<!--                    <a-button type="link" @click="handleChangeComp" :class="{green:autoComp,center:true}">-->
<!--                      <template v-slot:icon>-->
<!--                        <svg class="tags"-->
<!--                             style="vertical-align: middle;fill: currentColor;overflow: hidden;"-->
<!--                             viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="1494">-->
<!--                          <path d="M512 512m-512 0a512 512 0 1 0 1024 0 512 512 0 1 0-1024 0Z"-->
<!--                                p-id="1495"></path>-->
<!--                          <path d="M651.2 672.7h-548l269.6-321.4h548z" fill="#FFFFFF" p-id="1496"></path>-->
<!--                          <path-->
<!--                              d="M662.4 696.7H51.7l309.9-369.3h610.7L662.4 696.7z m-507.8-48H640l229.4-273.3H384L154.6 648.7z"-->
<!--                              p-id="1497"></path>-->
<!--                          <path d="M512 512m-48.2 0a48.2 48.2 0 1 0 96.4 0 48.2 48.2 0 1 0-96.4 0Z"-->
<!--                                p-id="1498"></path>-->
<!--                          <path-->
<!--                              d="M512 584.2c-39.8 0-72.2-32.4-72.2-72.2s32.4-72.2 72.2-72.2 72.2 32.4 72.2 72.2-32.4 72.2-72.2 72.2z m0-96.4c-13.4 0-24.2 10.9-24.2 24.2 0 13.4 10.9 24.2 24.2 24.2 13.4 0 24.2-10.9 24.2-24.2 0-13.4-10.8-24.2-24.2-24.2z"-->
<!--                              p-id="1499"></path>-->
<!--                        </svg>-->
<!--                      </template>-->
<!--                    </a-button>-->
<!--                  </a-popover>-->
<!--                </div>-->

<!--                <div class="center-name">{{ server.name }}</div>-->
<!--                <div ref="reloadEl">-->
<!--                  <a-button type="link" @click="reloadServer">-->
<!--                    <template v-slot:icon>-->
<!--                      <reload-outlined/>-->
<!--                    </template>-->
<!--                  </a-button>-->
<!--                </div>-->
<!--              </template>-->

              <div class="p-term-root">
                <div style="width: 100%;position: relative;overflow: hidden">
                      <p-term v-model:loading="pTermLoading" class="ssh" :server="server"
                              :master-session-id="server.masterSessionId"
                              ref="PTermRef"
                              @hot="onHot"
                              v-model:inputTerminal="inputTerm"
                              v-model:sub-session-username="subSessionUsername"></p-term>
                  <div style="position: absolute;right: 16px;top: calc(50% - 1em / 2);color: aliceblue;z-index: 100"
                       ref="openPopover"
                       @click="remarkStatus=!remarkStatus">
                    <left-outlined :class="{'button-action':remarkStatus,'left':true}"/>
                  </div>
                  <div ref="reloadEl"
                       @click="reloadServer"
                       style="position: absolute;right: 16px;top: 17px;color: aliceblue;z-index: 99999;fill: aliceblue"
                       class="left">
                    <reload-outlined class="tags"/>
                  </div>
                  <div :class="{green:sftpEnable,center:true}" @click="changeSftpEnable"
                       style="position: absolute;right:45px;top: 17px;color: aliceblue;z-index: 99999;fill: aliceblue"
                       class="left" ref="SftpChangeEl">

                        <svg t="1696435355552" class="tags" viewBox="0 0 1024 1024" version="1.1"
                             xmlns="http://www.w3.org/2000/svg" p-id="19507" >
                          <path
                              d="M972.8 249.856h-14.336l-0.512-108.032c0-25.6-20.992-45.568-46.08-45.568l-413.184 2.56h-3.584l-23.552-25.088c-9.728-10.24-23.04-16.384-37.376-16.384l-381.952-0.512C24.064 56.832 1.536 79.36 1.024 107.52L0 914.432c0 13.824 5.12 26.624 14.848 36.352 9.728 9.728 22.528 14.848 36.352 15.36l921.088 1.024c28.16 0 51.2-22.528 51.2-51.2l0.512-614.912c0-28.16-23.04-50.688-51.2-51.2z m-105.984-61.44l0.512 61.44-232.96-0.512-55.296-59.392 287.744-1.536zM921.088 865.28L102.4 864.256 108.032 158.72l303.616 0.512 162.816 176.128c9.728 10.24 23.04 16.384 37.376 16.384l310.272 0.512-1.024 513.024z"
                              p-id="19508"></path>
                          <path
                              d="M531.968 441.344c-9.216 1.536-17.408 7.168-22.528 15.36-9.728 15.872-6.144 36.864 8.192 48.128l28.16 22.016-183.808 1.024c-18.432 0-33.28 16.384-33.28 35.84s15.36 35.328 33.792 35.328l284.16-1.536c18.432 0 33.28-16.384 33.28-35.84 0-9.728-4.096-18.944-10.752-25.6-1.536-2.048-3.584-4.096-5.632-5.632l-106.496-82.944c-7.168-5.632-15.872-7.68-25.088-6.144zM647.168 639.488l-283.648 2.048c-18.432 0-33.28 16.384-33.28 35.84 0 9.728 4.096 18.944 10.752 25.6 1.536 2.048 3.584 4.096 5.632 5.632l106.496 82.944c5.632 4.608 12.8 6.656 19.968 6.656 11.264 0 21.504-6.144 27.648-15.872 4.608-7.68 6.656-16.896 5.12-25.6-1.536-9.216-6.144-17.408-13.312-22.528l-28.16-22.016 183.808-1.024c18.432 0 33.28-16.384 33.28-35.84-1.024-19.968-15.872-35.84-34.304-35.84z"
                              p-id="19509"></path>
                        </svg>
                  </div>
                  <div v-if="server.masterSessionId"
                       style="position: absolute;right: 16px;top: 60px;color: aliceblue;z-index: 100"
                       class="left enable-line"
                       :class="{'disable-line':!inputTerm,'enable-line':sftpEnable}"
                       @click="handleRequestInputTerm">
                    <edit-outlined style="text-decoration: line-through;"/>
                  </div>
                </div>

                <div :class="{remark:true,'remark-enter':remarkStatus}" class="card-container">
                  <a-tabs v-model:activeKey="rightTabKey" style="margin: 8px" type="card">
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
                              <div>
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
<!--            </a-card>-->
          </div>
        </a-spin>
      </template>
    </p-flip>
  </div>
</template>

<style scoped lang="less">

.split-box {
  height: 100%;
  min-height: auto;

  .ssh-content {
    height: 100%;

    :deep(.ant-card) {
      height: 100%;

      .ant-card-body {
        height: 100%;
      }
    }

    .p-term-root {
      display: flex;
      height: 100%
    }


    /deep/ .ant-card-head {

      .ant-card-extra {
        padding: 0;

        .tags {
          color: #000000;
        }
      }

      .ant-card-head-title {
        padding: 16px 0;
      }
    }

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
      //height: calc(@height - 100px);
      width: 0;
      background: rgba(255, 255, 255, 0.6); /* 设置元素的背景颜色和透明度 */
      backdrop-filter: blur(20px); /* 应用模糊效果，值可以调整模糊的程度 */
      border-radius: 8px; /* 设置元素的圆角 */
    }

    .ssh-enter {
      width: 80% !important;
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

:deep(#split_window .split_view .split_content_wrapper .split_view_label_wrapper) {
  display: none;
}

:deep(#split_window .split_view .split_content_wrapper .split_view_label_wrapper .split_view_label_box .header_item p, #split_window .split_view .split_content_wrapper .split_view_label_wrapper .split_view_label_box .header_item span) {
  margin: 0;
  text-align: center;
}

:deep(.drag-root) {
  //height: calc(@height - 100px) !important;
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

.center {
  display: flex;
  justify-content: center;
  align-items: center;
}

:deep(.ant-spin-nested-loading) {
  height: 100%;
}

:deep(.ant-spin-container) {
  height: 100%;
}

</style>


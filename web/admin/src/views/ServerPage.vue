<script setup>
import {computed, createVNode, nextTick, onMounted, ref} from "vue";
import Sortable from 'sortablejs';
import _ from "lodash";
import {Input, Modal} from "ant-design-vue";
import {ExclamationCircleOutlined} from "@ant-design/icons-vue";
import {v4} from 'uuid'
import ServerListPage from "@/views/ServerListPage.vue";
import ProxyListPage from "@/views/ProxyListPage.vue";
import SettingPage from "@/views/SettingPage.vue";
import ServerContent from "@/views/ServerContent.vue";
import SnippetListPage from "@/views/SnippetListPage.vue";

let spinning = ref(false)

let serverContentList = ref(null)

let currentServerContent = computed(() =>
    serverContentList.value.find(item => item.server.operationId === tagActiveKey.value)
)


const handleOpenServer = (item) => {
  spinning.value = true;
  let uuid = v4();
  while (serverList.value.findIndex(e => e.operationId === uuid) !== -1) {
    uuid = v4();
  }

  let copyItem = JSON.parse(JSON.stringify(item));
  copyItem.operationId = uuid;
  serverList.value.push(copyItem)
  tagActiveKey.value = copyItem.operationId
}


//监听iframe加载完成，执行sudo等初始化命令
window.onmessage = (e) => {
  if (e.data.event === 'connected') {
    spinning.value = false
    let server = serverList.value.find(item => item.operationId === tagActiveKey.value)
    if (!server) {
      return;
    }
    if (server.autoSudo && server.username !== 'root') {
      nextTick(() => {
        currentServerContent.value.handleExecCommand("echo \"" + server.password + "\" | sudo -S ls && sudo -i")
      })
    }

    if (server.firstCommand) {
      nextTick(() => {
        currentServerContent.value.handleExecCommand(server.firstCommand)
      })
    }

    nextTick(() => {
      //可以用来实现拖拽分屏
      // createSortEl(document.getElementsByClassName('split-box')[0])
    })
  }

  let rule = /^\/[^\0]+$/;
  if (e.data.event === 'command') {
    // 判断e.data.data是不是一个标准的linux路径 比如 /root , /home ，/var/www，如果是就保存下来留着sftp用
    let command = e.data.data.replaceAll('\r\n', '')
    if (rule.test(command)) {
      console.log("监测到命令", command)
      currentServerContent.value.changeDir(command)
    }
    // let data = e.data.data.split('\r\n')
    // for (let i = 0; i < data.length; i++) {
    //   if (rule.test(data[i])) {
    //     // console.log("监测到命令",data[i])
    //     currentServerContent.value.changeDir(data[i])
    //     break
    //   }
    // }

  }
}

onMounted(() => {
  return Sortable.create((document.getElementsByClassName('ant-tabs-nav-list')[0]), {
    group: {
      name: 'shared',
      pull: 'clone',
      put: 'true' // Do not allow items to be put into this list
    },
    scroll: true,
    dataIdAttr: 'id',
    sortElement: '.sortEl',
    dragClass: "sortable-drag",
    animation: 500
  });
})

const onCloseServer = (item) => {
  _.remove(serverList.value, i => i.operationId === item)
  if (item === tagActiveKey.value) {
    tagActiveKey.value = 'server'
  }
}

let tagActiveKey = ref()

let serverList = ref([])

let serverListRef = ref()
let proxyListRef = ref()

const handleCopy = (server) => {
  handleOpenServer(server)
}

const handleRename = (server) => {
  server.rename = server.name
  Modal.confirm({
    title: '重命名',
    icon: createVNode(ExclamationCircleOutlined),
    content: createVNode(Input, {
      onChange: (e) => {
        server.rename = e.target.value
      }
    }),
    onOk: async () => {
      server.name = server.rename
    },
    onCancel() {
      server.rename = ''
    },
  });
}

const handleAddTags = (server, tag) => {
  if (server.tag === tag) {
    server.tag = ''
  } else {
    server.tag = tag
  }
}

let tags = ref([
  `<svg t="1696430949034" class="tags" viewBox="0 0 1024 1024" version="1.1"
        xmlns="http://www.w3.org/2000/svg" p-id="8012" width="200" height="200">
     <path
         d="M301.3 496.7c-23.8 0-40.2-10.5-41.6-26.9H205c0.9 43.4 36.9 70.3 93.9 70.3 59.1 0 95-28.4 95-75.5 0-35.8-20-55.9-64.5-64.5l-29.1-5.6c-23.8-4.7-33.8-11.9-33.8-24.2 0-15 13.3-24.5 33.4-24.5 20.1 0 35.3 11.1 36.6 27h53c-0.9-41.7-37.5-70.3-90.3-70.3-54.4 0-89.7 28.9-89.7 73 0 35.5 21.2 58 62.5 65.8l29.7 5.9c25.8 5.2 35.6 11.9 35.6 24.4 0.1 14.7-14.5 25.1-36 25.1z"
         p-id="8013"></path>
     <path
         d="M928 140H96c-17.7 0-32 14.3-32 32v496c0 17.7 14.3 32 32 32h380v112H304c-8.8 0-16 7.2-16 16v48c0 4.4 3.6 8 8 8h432c4.4 0 8-3.6 8-8v-48c0-8.8-7.2-16-16-16H548V700h380c17.7 0 32-14.3 32-32V172c0-17.7-14.3-32-32-32z m-40 488H136V212h752v416z"
         p-id="8014"></path>
     <path
         d="M828.5 486.7h-95.8V308.5h-57.4V534h153.2zM529.9 540.1c14.1 0 27.2-2 39.1-5.8l13.3 20.3h53.3L607.9 511c21.1-20 33-51.1 33-89.8 0-73.3-43.3-118.8-110.9-118.8s-111.2 45.3-111.2 118.8c-0.1 73.7 43 118.9 111.1 118.9z m0-190c31.6 0 52.7 27.7 52.7 71.1 0 16.7-3.6 30.6-10 40.5l-5.2-6.9h-48.8L542 491c-3.9 0.9-8 1.4-12.2 1.4-31.7 0-52.8-27.5-52.8-71.2 0.1-43.6 21.2-71.1 52.9-71.1z"
         p-id="8015"></path>
   </svg>`
  ,
  `<svg t="1696431138103" class="tags" viewBox="0 0 1024 1024" version="1.1"
        xmlns="http://www.w3.org/2000/svg" p-id="16226" width="200" height="200">
  <path
      d="M752 321.84l132.7-132.69c63.79 89.55 55.75 214.52-24.57 294.85-60 60-144.9 79.57-221.37 59.2L292.59 889.38a114.66 114.66 0 0 1-162.16 0 114.65 114.65 0 0 1 0-162.15l346.18-346.18c-20.37-76.47-0.78-161.38 59.21-221.36 80.32-80.33 205.3-88.37 294.85-24.6L698 267.79z m98.58 9.53l-45.2 45.2a75.48 75.48 0 0 1-106.75 0l-55.41-55.4a75.48 75.48 0 0 1 0-106.75l45.2-45.2a152.56 152.56 0 0 0-109.79 57c-31.67 39.2-39.46 92.71-26.48 141.41l9.67 36.29-377.34 377.36a38.23 38.23 0 0 0 0 54.05 38.21 38.21 0 0 0 54.05 0L615.89 458l36.29 9.67c48.7 13 102.22 5.2 141.42-26.48a152.61 152.61 0 0 0 57-109.82z"
      fill="#949DA6" p-id="16227"></path>
  <path
      d="M656.23 487.31L893 724.12a116.5 116.5 0 0 1 0 164.73 116.48 116.48 0 0 1-164.73 0L491.5 652z m-37.74 72.08l-54.91 54.91 219.64 219.64A38.83 38.83 0 0 0 838.13 779zM261.578 312.297l54.907-54.907L501.71 442.617l-54.906 54.907z"
      fill="#949DA6" p-id="16228"></path>
  <path
      d="M316.49 312.3l-77.22 15.44a75.48 75.48 0 0 1-84.18-44.28L96.85 147.57l54.91-54.91 135.89 58.24a75.48 75.48 0 0 1 44.28 84.18z m-61.05-90.7l-51.94-22.28 22.28 51.93 24.72-4.93z"
      fill="#949DA6" p-id="16229"></path>
</svg>`
  ,
  `  <svg t="1696430996543" class="tags" viewBox="0 0 1024 1024" version="1.1"
          xmlns="http://www.w3.org/2000/svg" p-id="9844" width="200" height="200">
  <path
      d="M707.2 79.9H238.6c-11.6 0-21 9.4-21 21V923c0 11.6 9.4 21 21 21h704.9V316.3L707.2 79.9"
      fill="#FFFFFF" p-id="9845"></path>
  <path
      d="M943.6 960h-705c-20.4 0-37-16.6-37-37V100.9c0-20.4 16.6-37 37-37h468.6c4.2 0 8.3 1.7 11.3 4.7L954.9 305c3 3 4.7 7.1 4.7 11.3V944c0 8.9-7.2 16-16 16z m-705-864.1c-2.7 0-5 2.2-5 5V923c0 2.7 2.2 5 5 5h689V323l-227-227h-462z"
      fill="#00365B" p-id="9846"></path>
  <path d="M943.6 316.3H707.2V79.9z" fill="#FFFFFF" p-id="9847"></path>
  <path
      d="M943.6 332.3H707.2c-8.8 0-16-7.2-16-16V79.9c0-6.5 3.9-12.3 9.9-14.8s12.9-1.1 17.4 3.5L954.9 305c4.6 4.6 5.9 11.5 3.5 17.4-2.5 6-8.3 9.9-14.8 9.9z m-220.4-32H905L723.2 118.6v181.7z"
      fill="#00365B" p-id="9848"></path>
  <path
      d="M104.7 414.2h698.8c22.4 0 40.6 19.8 40.6 44.3v301.4c0 24.5-18.2 44.3-40.6 44.3H104.7c-22.4 0-40.6-19.8-40.6-44.3V458.5c-0.1-24.5 18.1-44.3 40.6-44.3z"
      fill="#B56FE1" p-id="9849"></path>
  <path
      d="M241.2 736c4.5 0 8.9-1.6 13.2-4.9 4.3-3.3 6.4-8.9 6.4-17 0-7.8-2-13.4-6-16.8s-8.6-5.1-13.6-5.1h-74V488.3c0-6-2.3-10.8-6.8-14.2-4.5-3.4-10.4-5.1-17.7-5.1-6.8 0-12.6 1.7-17.4 5.1-4.8 3.4-7.2 8.1-7.2 14.2v220.5c0 7.6 2.4 14 7.2 19.3 4.8 5.3 11.3 7.9 19.6 7.9h96.3z m162.8 7.5c20.4 0 38.1-3.6 53-10.8 15-7.2 27.4-17 37.2-29.5 9.8-12.5 17.1-27.2 21.9-44.2 4.8-17 7.2-35.3 7.2-54.9s-2.4-37.9-7.2-54.7c-4.8-16.9-12.1-31.5-21.9-44C484.4 493 472 483.2 457 476c-15-7.2-32.7-10.8-53-10.8-21.1 0-39.2 3.6-54.2 10.8-15 7.2-27.3 17-37 29.5s-16.8 27.2-21.3 44.2c-4.5 17-6.8 35.2-6.8 54.6 0 20.1 2.5 38.8 7.6 55.9 5 17.1 12.6 31.8 22.7 44.2 10.1 12.3 22.5 22 37.4 28.9s31.9 10.2 51.6 10.2z m0-43c-13.3 0-24.5-2.8-33.4-8.3-8.9-5.5-16.2-12.8-21.7-21.9-5.5-9.1-9.4-19.4-11.7-31-2.3-11.6-3.4-23.3-3.4-35.1 0-13.1 1.3-25.4 3.8-37 2.5-11.6 6.5-21.7 12.1-30.4 5.5-8.7 12.8-15.6 21.7-20.8 8.9-5.2 19.8-7.7 32.7-7.7s23.7 2.5 32.7 7.6c8.9 5 16.2 12 21.7 20.8s9.6 19.1 12.1 31c2.5 11.8 3.8 24.4 3.8 37.8 0 13.3-1.1 25.8-3.4 37.4-2.3 11.6-6.2 21.6-11.7 30.2-5.5 8.6-12.8 15.3-21.7 20.2-9.2 4.7-20.3 7.2-33.6 7.2z m278.2 43c20.4 0 37.6-3.2 51.7-9.6 14.1-6.4 25.6-15.1 34.5-26.1 8.9-10.9 15.4-23.6 19.3-37.9 3.9-14.3 5.9-29.5 5.9-45.3 0-13.1-1.8-22.3-5.3-27.6s-10.4-7.9-20.8-7.9h-63.1c-8.8 0-15.4 1.8-19.6 5.3-4.3 3.5-6.4 9.3-6.4 17.4 0 8.1 2 13.7 6 16.8 4 3.1 10.6 4.7 19.6 4.7h40.4c0 9.8-1.6 18.8-4.9 27-3.3 8.2-7.7 15.2-13.4 21.1-5.7 5.9-12.2 10.6-19.6 14s-15.4 5.1-24 5.1c-11.8 0-22.7-2.3-32.5-6.8s-18.2-10.9-25.3-19.3c-7-8.3-12.5-18.4-16.4-30.2-3.9-11.8-5.9-25.2-5.9-40 0-14.6 2-27.8 6-39.6s9.4-21.9 16.2-30.2c6.8-8.3 14.9-14.7 24.2-19.3 9.3-4.5 19.3-6.8 29.8-6.8 12.1 0 22.6 2 31.5 5.9 8.9 3.9 18.4 11.3 28.5 22.1 3 3 6.3 5 9.8 6 3.5 1 7 1.4 10.6 1.1 3.5-0.3 6.9-1.1 10-2.6 3.1-1.5 5.9-3.3 8.1-5.3 4.5-3.3 6.6-7.2 6.2-11.9-0.4-4.7-2.3-9.4-5.9-14.3-3.5-4.9-8.2-9.8-14-14.7-5.8-4.9-12.1-9.5-18.9-13.8-11.3-6.5-22-10.8-32.1-12.6-10.1-1.9-20.9-2.8-32.5-2.8-20.1 0-38.1 3.6-53.8 10.9-15.7 7.3-29 17.2-39.8 29.8-10.8 12.6-19.1 27.4-24.7 44.6-5.7 17.1-8.5 35.6-8.5 55.5 0 20.9 3 39.8 8.9 56.6 5.9 16.9 14.5 31.3 25.7 43.2 11.2 12 24.7 21.2 40.6 27.8 16.2 6.5 34 9.7 53.9 9.7z"
      fill="#FFFFFF" p-id="9850"></path>
</svg>`,
  `  <svg t="1696431206829" class="tags" viewBox="0 0 1024 1024" version="1.1"
       xmlns="http://www.w3.org/2000/svg" p-id="17235" width="200" height="200">
    <path
        d="M855.04 385.024q19.456 2.048 38.912 10.24t33.792 23.04 21.504 37.376 2.048 54.272q-2.048 8.192-8.192 40.448t-14.336 74.24-18.432 86.528-19.456 76.288q-5.12 18.432-14.848 37.888t-25.088 35.328-36.864 26.112-51.2 10.24l-567.296 0q-21.504 0-44.544-9.216t-42.496-26.112-31.744-40.96-12.288-53.76l0-439.296q0-62.464 33.792-97.792t95.232-35.328l503.808 0q22.528 0 46.592 8.704t43.52 24.064 31.744 35.84 12.288 44.032l0 11.264-53.248 0q-40.96 0-95.744-0.512t-116.736-0.512-115.712-0.512-92.672-0.512l-47.104 0q-26.624 0-41.472 16.896t-23.04 44.544q-8.192 29.696-18.432 62.976t-18.432 61.952q-10.24 33.792-20.48 65.536-2.048 8.192-2.048 13.312 0 17.408 11.776 29.184t29.184 11.776q31.744 0 43.008-39.936l54.272-198.656q133.12 1.024 243.712 1.024l286.72 0z"
        p-id="17236"></path>
  </svg>`
])

const handleProxyCreateSuccess = async (res) => {
  if (serverListRef.value) {
    await serverListRef.value.getProxyData()
    serverListRef.value.setProxyId(res.id)
  }
}

const proxyCreation = () => {
  proxyListRef.value.proxyCreation()
}


</script>

<template>
  <div class="server-root">
    <a-spin :spinning="spinning" tip="等待中...">
      <a-tabs style="width: 100%;"
              class="server-content-tabs"
              type="editable-card"
              :tabBarGutter="8"
              :hideAdd="true"
              v-model:activeKey="tagActiveKey"
              :tab-position="'left'">

        <a-tab-pane  tab="服务器" class="server-pane" key="server" :closable="false">
          <ServerListPage ref="serverListRef" @openServer="handleOpenServer"
                          @proxyCreation="proxyCreation"></ServerListPage>
        </a-tab-pane>
        <a-tab-pane class="proxy-pane" tab="代理" key="proxy" :closable="false" :forceRender="true">
          <proxy-list-page ref="proxyListRef" @createSuccess="handleProxyCreateSuccess"></proxy-list-page>
        </a-tab-pane>
        <a-tab-pane tab="命令" key="snippet" :closable="false" :forceRender="true">
          <snippet-list-page ref="snippetListRef" @createSuccess="handleProxyCreateSuccess"></snippet-list-page>
        </a-tab-pane>
        <a-tab-pane class="setting-pane" tab="设置" key="setting" :closable="false" :forceRender="true">
          <setting-page></setting-page>
        </a-tab-pane>
        <template v-for="server in serverList" :key="server.operationId">
          <a-tab-pane>
            <template v-slot:tab>
              <a-dropdown :trigger="['contextmenu']" class="dropdown">
                <div style="display: flex;justify-content: center;align-items: center;">
                  <div style="line-height: 1px;" v-html="server.tag"></div>
                  <div class="ml5">{{ server.name }}</div>
                </div>
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="4" @click="handleCopy(server)">
                      <template #icon>
                        <CopyOutlined/>
                      </template>
                      复制
                    </a-menu-item>
                    <a-menu-item key="5" @click="handleRename(server)">
                      <a-sub-menu key="7">
                        <template #icon>
                          <tag-outlined/>
                        </template>
                        <template #title>标签</template>
                        <a-menu-item @click="handleAddTags(server,item)" v-for="item in tags" :key="item.length">
                          <template #icon>
                            <div v-html="item"></div>
                          </template>
                        </a-menu-item>

                      </a-sub-menu>
                    </a-menu-item>
                    <a-menu-item key="3" @click="handleRename(server)">
                      <template #icon>
                        <edit-outlined/>
                      </template>
                      重命名
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </template>
            <template v-slot:closeIcon>
              <close-outlined @click="onCloseServer(server.operationId)"/>
            </template>
            <server-content ref="serverContentList" :server="server"></server-content>
          </a-tab-pane>
        </template>
      </a-tabs>
    </a-spin>

  </div>
</template>

<style scoped lang="less">

:deep(.ant-card-extra) {
  display: flex;
}


:deep(.tags) {
  width: 20px;
  height: 20px;
  font-size: 20px;
}


:deep(.ant-dropdown-menu-submenu-title) {
  padding-left: 0px;
}

.ml5 {
  margin-left: 5px;
}

:deep(.green) {
  color: #1daa6c;
  //设置svg颜色
  svg {
    fill: #1daa6c;
  }
}

.server-content-tabs{
  height: @height;
}

:deep(.ant-tabs-tab-with-remove){
  display: flex;
  justify-content: space-between;
}

</style>

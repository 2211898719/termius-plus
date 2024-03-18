<script setup>
import {computed, createVNode, getCurrentInstance, h, nextTick, onMounted, ref, render, watch} from "vue";
import Sortable from 'sortablejs';
import _ from "lodash";
import {Input, Modal} from "ant-design-vue";
import {ExclamationCircleOutlined} from "@ant-design/icons-vue";
import {v4} from 'uuid'
import ServerListPage from "@/views/server/ServerListPage.vue";
import ProxyListPage from "@/views/server/ProxyListPage.vue";
import SettingPage from "@/views/server/SettingPage.vue";
import ServerContent from "@/views/server/ServerContent.vue";
import SnippetListPage from "@/views/server/SnippetListPage.vue";
import PortForwarderPage from "@/views/server/PortForwarderPage.vue";
import CronJobPage from "@/views/server/CronJobPage.vue";
import OsEnum from "@/enums/OsEnum";
import {useStorage} from "@vueuse/core";
import {ComponentItem, GoldenLayout} from "golden-layout";

let spinning = ref(false)

let serverContentList = ref(null)

let tagActiveKey = ref('server')

let serverList = ref([])

let serverListRef = ref()
let proxyListRef = ref()

const handleOpenServer = (item, masterSessionId = 0) => {
  // spinning.value = true;
  let uuid = v4();
  while (serverList.value.findIndex(e => e.operationId === uuid) !== -1) {
    uuid = v4();
  }

  let copyItem = JSON.parse(JSON.stringify(item));
  copyItem.operationId = uuid;
  copyItem.masterSessionId = masterSessionId;
  if (Array.isArray(copyItem.onlyConnect)) {
    let connect = copyItem.onlyConnect.find(e => e.masterSessionId === masterSessionId);
    if (connect) {
      copyItem.masterUserInfo = connect.user;
    }
  }

  serverList.value.push(copyItem)
  tagActiveKey.value = copyItem.operationId

  nextTick(() => {
    renderLayout(copyItem)
  })
}

onMounted(() => {
  return Sortable.create((document.getElementsByClassName('sortable')[0]), {
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
  let index = serverList.value.findIndex(e => e.operationId === item)
  if (index === -1) {
    return
  }

  nextTick(() => {
    goldenLayoutArr[item].destroy()
    delete goldenLayoutArr[item]
  })

  _.remove(serverList.value, i => i.operationId === item)
  if (item === tagActiveKey.value) {
    tagActiveKey.value = 'server'
  }
}

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
      fill="" p-id="16227"></path>
  <path
      d="M656.23 487.31L893 724.12a116.5 116.5 0 0 1 0 164.73 116.48 116.48 0 0 1-164.73 0L491.5 652z m-37.74 72.08l-54.91 54.91 219.64 219.64A38.83 38.83 0 0 0 838.13 779zM261.578 312.297l54.907-54.907L501.71 442.617l-54.906 54.907z"
      fill="" p-id="16228"></path>
  <path
      d="M316.49 312.3l-77.22 15.44a75.48 75.48 0 0 1-84.18-44.28L96.85 147.57l54.91-54.91 135.89 58.24a75.48 75.48 0 0 1 44.28 84.18z m-61.05-90.7l-51.94-22.28 22.28 51.93 24.72-4.93z"
      fill="" p-id="16229"></path>
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


const changeTab = (item) => {
  tagActiveKey.value = item
}

const onServerHot = (server) => {
  if (server.operationId === tagActiveKey.value) {
    return
  }

  let index = serverList.value.findIndex(e => e.operationId === server.operationId)
  if (index === -1) {
    return
  }

  serverList.value[index].hot = true
}

let backColor = useStorage('backColor', "#000000")
let frontColor = useStorage('frontColor', "#ffffff")

let backColorRadialGradient = computed(() => {
  return `radial-gradient(10px at 0px 0px, transparent 15px, ${backColor.value} 0px)`
})


let layoutContainer = ref([])
let tabBarRef = ref([])

const componentIns = getCurrentInstance();

class termComponent {
  rootElement;
  pTerm;

  constructor(container, state) {
    this.rootElement = container.element;
    this.pTerm = h(ServerContent, {...state, onHot: onServerHot})
    this.pTerm.appContext = componentIns.appContext
    this.rootElement.appendChild(this.vNode2dom(this.pTerm));
  }

  close() {
    this.pTerm.component.exposed.close()
  }

  vNode2dom(vNode) {
    let domEl = document.createElement("div");
    render(vNode, domEl);
    return domEl.firstElementChild;
  }
}

let goldenLayoutArr = {}

const resizeObserver = new ResizeObserver(() => {
  Object.keys(goldenLayoutArr).map(e => goldenLayoutArr[e]).forEach(e => {
    e.updateRootSize()
  });
});

resizeObserver.observe(document.body);

const renderLayout = (server) => {
  let el = layoutContainer.value[layoutContainer.value.length - 1]
  const goldenLayout = new GoldenLayout(el);

  goldenLayout.registerComponentConstructor('termComponent', termComponent);
  goldenLayout.loadLayout({
    header: {
      show: true,
      popout: false,
      maximise: false,
      close: "关闭",
    },

    root: {
      type: 'row',
      content: [
        {
          title: server.name,
          type: 'component',
          componentType: 'termComponent',
          componentState: {
            server: server
          }
        }
      ]
    }
  });

  goldenLayout.on("itemDestroyed", (item) => {
    //是我吗自己的组件,需要调用组件的close方法
    if (item.target instanceof ComponentItem) {
      item.target.component.close()
    }

    nextTick(() => {
      //没有rootItem就关闭标签
      if (!goldenLayout.rootItem) {
        onCloseServer(server.operationId)
      }
    })
  })

  // goldenLayout.on( 'stateChanged', function( stack ){
  //  stack.target.header.controlsContainerElement.prepend(
  //
  //  )
  // })


  goldenLayoutArr[server.operationId] = goldenLayout
}


watch(() => tagActiveKey.value, (val) => {
  let index = serverList.value.findIndex(e => e.operationId === val)
  if (index === -1) {
    return
  }

  nextTick(() => {
    let current = goldenLayoutArr[tagActiveKey.value];

    //清除掉其他面板的拖拽源
    Object.keys(goldenLayoutArr).forEach(k => {
      let e = goldenLayoutArr[k];
        if (Array.isArray(e.dragSources)) {
          e.dragSources.forEach(dragSource => {
            e.removeDragSource(dragSource)
          })

          e.dragSources = []
        }
    })

    current.dragSources = []

    for (let i = 0; i < tabBarRef.value.length; i++) {
      if (i !== index) {
        current.dragSources.push(
            current.newDragSource(tabBarRef.value[i], () => {
              return {
                title: serverList.value[i].name,
                type: 'component',
                componentType: 'termComponent',
                componentState: {
                  server: {
                    ...serverList.value[i],
                    operationId: val
                  }
                }
              }
            })
        );
      }
    }


    current.updateRootSize()

    // serverContentList.value[index].focus()
    serverList.value[index].hot = false
  })

})

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

        <a-tab-pane tab="服务器" class="server-pane" key="server" :closable="false">
          <ServerListPage ref="serverListRef" @openServer="handleOpenServer"
                          @proxyCreation="proxyCreation"></ServerListPage>
        </a-tab-pane>
        <a-tab-pane class="proxy-pane" tab="代理" key="proxy" :closable="false" :forceRender="true">
          <proxy-list-page ref="proxyListRef" @createSuccess="handleProxyCreateSuccess"></proxy-list-page>
        </a-tab-pane>
        <a-tab-pane tab="端口转发" key="portForwarder" :closable="false" :forceRender="true">
          <port-forwarder-page></port-forwarder-page>
        </a-tab-pane>
        <a-tab-pane tab="定时任务" key="cronJob" :closable="false" :forceRender="true">
          <cron-job-page></cron-job-page>
        </a-tab-pane>
        <a-tab-pane tab="命令片段" key="snippet" :closable="false" :forceRender="true">
          <snippet-list-page ref="snippetListRef" @createSuccess="handleProxyCreateSuccess"></snippet-list-page>
        </a-tab-pane>
        <a-tab-pane class="setting-pane" tab="设置" key="setting" :closable="false" :forceRender="true">
          <setting-page></setting-page>
        </a-tab-pane>

        <template v-slot:renderTabBar>
          <div class="tab-bar-group">
            <div class="tab-bar" :class="{'tab-active-normal':tagActiveKey==='server'}" @click="changeTab('server')">
              <div class="left">
                <div class="tab-icon">
                  <cloud-server-outlined/>
                </div>
                <div class="tab-title">
                  服务器
                </div>
              </div>
              <div class="right"></div>
            </div>
            <div class="tab-bar" :class="{'tab-active-normal':tagActiveKey==='proxy'}" @click="changeTab('proxy')">
              <div class="left">
                <div class="tab-icon">
                  <svg class="icon"
                       style="vertical-align: middle;fill: currentColor;overflow: hidden;"
                       viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="1877">
                    <path
                        d="M894.424364 729.008452c-53.43705-151.726633-194.117093-258.544451-354.425171-269.737374v-64.9912c87.777159-13.472849 155.219177-89.505523 155.219177-180.992167 0-100.968599-82.144882-183.113481-183.113481-183.113481s-183.113481 82.144882-183.113481 183.113481c0 91.487667 67.442018 167.518295 155.219177 180.992167v64.9912c-160.309102 11.1919-300.993239 118.007671-354.429265 269.728165-71.986521 2.539846-129.764436 61.880349-129.764435 134.475737 0 74.204026 60.365856 134.569882 134.569881 134.569882s134.569882-60.365856 134.569882-134.569882c0-56.175417-34.600029-104.409978-83.604117-124.537388 47.802726-125.075647 165.031661-212.908064 298.657031-223.73362v216.59299c-60.916395 12.8384-106.784048 66.997903-106.784048 131.679041 0 74.204026 60.365856 134.569882 134.569881 134.569882s134.569882-60.365856 134.569882-134.569882c0-64.603367-45.759183-118.709659-106.567108-131.630946v-216.640062c133.584438 10.82351 250.799046 98.622158 298.622238 223.657896-49.101301 20.082384-83.787288 68.36811-83.787288 124.613112 0 74.204026 60.365856 134.569882 134.569882 134.569882s134.569882-60.365856 134.569881-134.569882c0.001023-72.523757-57.662281-131.818211-129.548518-134.467551z m-681.057347 134.466528c0 43.435255-35.344996 78.780251-78.780251 78.780251s-78.780251-35.344996-78.780251-78.780251 35.344996-78.780251 78.780251-78.780251c3.151783 0 6.255471 0.206708 9.312087 0.569982 1.287319 0.277316 2.577708 0.459464 3.859911 0.553608 37.184901 6.288217 65.608254 38.71065 65.608253 77.656661z m171.412998-650.186246c0-70.214155 57.110719-127.32385 127.323851-127.32385s127.32385 57.110719 127.32385 127.32385c0 70.214155-57.110719 127.32385-127.32385 127.323851s-127.32385-57.109696-127.323851-127.323851z m205.996654 650.186246c0 43.435255-35.344996 78.780251-78.780251 78.780251-43.435255 0-78.780251-35.344996-78.780251-78.780251s35.344996-78.780251 78.780251-78.780251c43.434232 0 78.780251 35.343973 78.780251 78.780251z m298.627355 78.781274c-43.435255 0-78.780251-35.344996-78.780251-78.780251 0-39.041178 28.561499-71.529103 65.880453-77.706803a28.15627 28.15627 0 0 0 3.627621-0.510629 79.109756 79.109756 0 0 1 9.272177-0.563842c43.435255 0 78.780251 35.344996 78.780251 78.780251 0.002047 43.435255-35.343973 78.781274-78.780251 78.781274z"
                        fill="" p-id="1878"></path>
                  </svg>
                </div>
                <div class="tab-title">
                  代理
                </div>
              </div>
              <div class="right"></div>
            </div>
            <div class="tab-bar" :class="{'tab-active-normal':tagActiveKey==='portForwarder'}"
                 @click="changeTab('portForwarder')">
              <div class="left">
                <div class="tab-icon">
                  <svg class="icon" style=";vertical-align: middle;fill: currentColor;overflow: hidden;"
                       viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="2316">
                    <path
                        d="M865.922728 583.211878c-16.276708 0-29.580712 13.246699-29.667693 29.727045l0 215.125569c0 17.992793-14.58723 32.637328-32.520671 32.637328L181.762717 860.70182c-17.935488 0-32.520671-14.645558-32.520671-32.637328L149.242046 292.155966c0-17.992793 14.586207-32.637328 32.520671-32.637328l291.230897 0c16.304338-0.029676 29.580712-13.363356 29.580712-29.724998 0-16.392342-13.276375-29.727045-29.610388-29.727045l-295.336402 0c-48.358381 0-87.721901 39.450501-87.721901 87.925538l0 544.205493c0 48.475038 39.36352 87.925538 87.721901 87.925538l630.239961 0c48.358381 0 87.720877-39.450501 87.720877-87.925538L895.588375 612.762915C895.501394 596.458577 882.19739 583.211878 865.922728 583.211878z"
                        fill="" p-id="2317"></path>
                    <path
                        d="M930.818761 338.183256l0-0.318248L727.07645 133.511783l-6.435573-6.259564-0.814552 0.844228c-4.511757-2.532683-9.606799-3.873214-14.876826-3.873214-16.974603 0-30.774911 13.829983-30.774911 30.832216 0 5.298679 1.338485 10.393721 3.873214 14.907525l-0.903579 0.931209 141.845589 142.224212-145.573493 0.057305C436.396091 342.726735 378.197598 489.375723 361.049033 717.050096c0 17.004279 13.800307 30.832216 30.772864 30.832216 13.858636 0 25.620517-9.229199 29.464055-21.893636l1.397836-8.181333c18.022469-215.329207 60.470233-321.567833 251.839749-342.937536l144.466276 0L683.433464 510.804778l-5.502317 7.744381c-1.951445 4.104481-2.969635 9.112542-2.969635 13.654998 0 17.002232 13.799284 30.832216 30.772864 30.832216 4.832052 0 10.160407-1.164522 14.439874-3.37691L929.954067 350.740246c1.860371-1.305739 4.140297-4.52506 4.140297-6.970762C934.093341 341.323782 932.679132 339.488994 930.818761 338.183256z"
                        fill="" p-id="2318"></path>
                  </svg>
                </div>
                <div class="tab-title">
                  端口转发
                </div>
              </div>
              <div class="right"></div>
            </div>
            <div class="tab-bar" :class="{'tab-active-normal':tagActiveKey==='cronJob'}" @click="changeTab('cronJob')">
              <div class="left">
                <div class="tab-icon">
                  <svg class="icon"
                       style="vertical-align: middle;fill: currentColor;overflow: hidden;"
                       viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="3172">
                    <path
                        d="M553 186.4v-84h204.8c22.5 0 41-18.4 41-41 0-22.5-18.4-41-41-41H266.2c-22.5 0-41 18.4-41 41 0 22.5 18.4 41 41 41H471v84C264 207 102.4 381.5 102.4 593.9c0 226.2 183.4 409.6 409.6 409.6s409.6-183.4 409.6-409.6c0-212.4-161.7-387-368.6-407.5z m-41 735.2c-180.7 0-327.7-147-327.7-327.7s147-327.7 327.7-327.7 327.7 147 327.7 327.7-147 327.7-327.7 327.7zM532.5 556.5V368.6c0-22.5-18.4-41-41-41s-41 18.4-41 41v204.8c0 0.3 0.1 0.5 0.1 0.7 0 2.4 0.3 4.9 0.7 7.3 0.2 0.9 0.5 1.7 0.8 2.6 0.5 1.7 0.9 3.5 1.6 5.2 0.2 0.5 0.5 1 0.8 1.5 2 4.2 4.5 8.2 8 11.6l173.8 173.8c15.9 15.9 42 15.9 57.9 0 15.9-15.9 15.9-42 0-57.9L532.5 556.5z"
                        p-id="3173"></path>
                  </svg>
                </div>
                <div class="tab-title">
                  定时任务
                </div>
              </div>
              <div class="right"></div>
            </div>
            <div class="tab-bar" :class="{'tab-active-normal':tagActiveKey==='snippet'}" @click="changeTab('snippet')">
              <div class="left">
                <div class="tab-icon">
                  <svg class="icon"
                       style="vertical-align: middle;fill: currentColor;overflow: hidden;"
                       viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="4057">
                    <path
                        d="M896 346.8c-0.1 0 0 0 0 0 0-0.9-0.1-1.7-0.2-2.5 0-0.1 0-0.3-0.1-0.4-0.2-1.7-0.5-3.3-1-4.9-0.5-1.7-1-3.3-1.7-4.9v-0.1c-0.3-0.8-0.7-1.5-1.1-2.3v-0.1c-0.4-0.7-0.8-1.5-1.2-2.2-0.1-0.1-0.1-0.2-0.2-0.3-0.4-0.6-0.8-1.2-1.3-1.9-0.1-0.1-0.1-0.2-0.2-0.2-0.5-0.6-1-1.3-1.5-1.9-0.1-0.1-0.2-0.3-0.4-0.4-0.5-0.6-1-1.2-1.6-1.7l-0.1-0.1L637.3 74.5c-0.6-0.6-1.2-1.1-1.8-1.6-0.1-0.1-0.3-0.2-0.4-0.4-0.6-0.5-1.2-1-1.9-1.5-0.1 0-0.1-0.1-0.2-0.1-0.6-0.5-1.3-0.9-1.9-1.3-0.1-0.1-0.2-0.1-0.3-0.2-0.7-0.4-1.4-0.9-2.2-1.3-0.8-0.4-1.5-0.8-2.3-1.1h-0.1c-1.6-0.7-3.2-1.3-4.9-1.7-1.6-0.4-3.2-0.8-4.9-1-0.1 0-0.3 0-0.4-0.1-1.4-0.2-2.8-0.3-4.3-0.3H164c-19.9 0-36 16.1-36 36v823.3c0 19.9 16.1 36 36 36h696c19.9 0 36-16.1 36-36V348.6v-1.8zM647.8 186.9l125.4 125.6H647.8V186.9zM200 887.2V135.9h375.8v212.7c0 19.9 16.1 36 36 36H824v502.7H200z"
                        fill="" p-id="4058"></path>
                    <path
                        d="M378.8 603.9l59.7-65.8c13.4-14.7 12.2-37.5-2.5-50.9-14.7-13.4-37.5-12.2-50.9 2.5l-80.8 89.1c-12.1 13.3-12.5 33.5-0.9 47.3l80.9 96.5c7.1 8.5 17.3 12.9 27.6 12.9 8.2 0 16.4-2.8 23.1-8.4 15.2-12.8 17.2-35.5 4.5-50.7l-60.7-72.5zM639 489.7c-13.4-14.7-36.1-15.8-50.9-2.5-14.7 13.4-15.8 36.1-2.5 50.9l59.7 65.8-60.7 72.4c-12.8 15.2-10.8 37.9 4.5 50.7 6.7 5.7 14.9 8.4 23.1 8.4 10.3 0 20.5-4.4 27.6-12.9l80.9-96.5c11.6-13.8 11.2-34-0.9-47.3l-80.8-89z"
                        fill="" p-id="4059"></path>
                  </svg>
                </div>
                <div class="tab-title">
                  命令片段
                </div>
              </div>

              <div class="right"></div>
            </div>
            <div class="tab-bar" :class="{'tab-active-normal':tagActiveKey==='setting'}" @click="changeTab('setting')">
              <div class="left">
                <div class="tab-icon">
                  <setting-outlined/>
                </div>
                <div class="tab-title">
                  设置
                </div>
              </div>

              <div class="right"></div>
            </div>
            <div class="sortable">
              <a-dropdown v-for="server in serverList" :key="server.operationId" :trigger="['contextmenu']"
                          class="dropdown">

                <div ref="tabBarRef" class="tab-bar" :class="{'tab-active':tagActiveKey===server.operationId}"
                     @click="changeTab(server.operationId)">
                  <div class="left">
                    <a-badge :dot="server.hot" :offset="[-9,-2]">
                      <div class="tab-icon">
                        <template v-if="server.tag">
                          <div v-html="server.tag">

                          </div>
                        </template>
                        <template v-else>
                          <ApartmentOutlined v-if="server.isGroup" class="icon-server"/>
                          <hdd-outlined v-else-if="server.os===OsEnum.LINUX.value" class="icon-server"
                                        style="color: #E45F2B;"/>
                          <windows-outlined v-else-if="server.os===OsEnum.WINDOWS.value" class="icon-server"
                                            style="color: #E45F2B;"/>
                        </template>

                      </div>
                    </a-badge>
                    <div class="tab-title" :title="server.name">
                      {{ server.name }}
                    </div>
                  </div>
                  <div class="right tab-close">
                    <close-outlined @click.stop="onCloseServer(server.operationId)"/>
                  </div>
                </div>
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="4" @click="handleCopy(server)">
                      <template #icon>
                        <CopyOutlined/>
                      </template>
                      复制
                    </a-menu-item>
                    <a-menu-item key="5" >
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

            </div>
          </div>
        </template>

        <template v-for="server in serverList" :key="server.operationId">
          <a-tab-pane class="ant-tabs-tab-dot">
            <template v-slot:closeIcon>
              <close-outlined @click="onCloseServer(server.operationId)"/>
            </template>
            <div ref="layoutContainer" class="layoutContainer">

            </div>
          </a-tab-pane>
        </template>
      </a-tabs>
    </a-spin>

  </div>
</template>

<style lang="less">
@import 'golden-layout/dist/less/goldenlayout-base.less';
@import 'golden-layout/dist/less/themes/goldenlayout-dark-theme.less';

</style>

<style scoped lang="less">


:deep(.ant-card-extra) {
  display: flex;
}


:deep(.tags) {
  width: 18px;
  height: 18px;
  font-size: 16px;
}


:deep(.ant-dropdown-menu-submenu-title) {
  padding-left: 0px;
}

.ml5 {
  margin-left: 5px;
}

:deep(.green) {
  color: #1daa6c !important;
  //设置svg颜色
  svg {
    fill: #1daa6c !important;
  }
}

.server-content-tabs {
  height: @height;
}


:deep(.ant-tabs-left > .ant-tabs-content-holder > .ant-tabs-content > .ant-tabs-tabpane, .ant-tabs-left > div > .ant-tabs-content-holder > .ant-tabs-content > .ant-tabs-tabpane) {
  padding: 0;
}

:deep(.ant-tabs-content-holder) {
  border: none;
  //z-index: 100;
}

.tab-bar-group {
  background-color: #282B3B;
  padding: 10px;
  overflow: auto;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE 10+ */
  //设置滚动条隐藏
  ::-webkit-scrollbar {
    display: none;
  }


  .sortable {
    display: flex;
    flex-direction: column;
  }


  .tab-bar {
    margin: 4px 0;
    padding: 8px 16px;
    cursor: pointer;
    color: #fff;
    text-align: left;
    width: 180px;
    display: flex;
    border-radius: 8px;
    justify-content: space-between;

    .left {
      width: 83%;
      display: flex;

      .tab-icon {
        margin-right: 8px;
        font-size: 18px;
        display: flex;
        justify-content: center;
        align-items: center;
        color: #fff;
        fill: #fff;
        width: 20px;
        height: 100%;

        svg {
          width: 18px;
          height: 18px;
          display: flex;
          justify-content: center;
          align-items: center;
        }

        .tags {
          width: 18px;
          height: 18px;
          font-size: 17px;
          display: flex;
          justify-content: center;
          align-items: center;
        }
      }

      .tab-title {
        //单行省略
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;

      }
    }

    .right {

    }


    //悬停时出现关闭按钮
    &:hover .tab-close {
      display: flex !important;
    }

    .tab-close {
      font-size: 12px;
      display: none;
      justify-content: center;
      align-items: center;
      padding: 0 4px;
      border-radius: 4px;

      &:hover {
        background-color: #404460;
      }
    }

    &:hover {
      color: #1890ff;
      background-color: #32364A;
      border-radius: 8px;
    }
  }

  //普通的
  .tab-active-normal {
    color: #1890ff;
    background-color: #32364A;
    position: relative;
    z-index: 100;
  }

  .tab-active {
    color: v-bind(frontColor) !important;
    background-color: v-bind(backColor) !important;
    position: relative;
    z-index: 100;
  }

  .tab-active:after {
    content: "";
    display: block;
    height: 103%;
    width: 30px;
    position: absolute;
    right: -24px;
    top: -15px;
    background: v-bind(backColorRadialGradient);
  }

  .tab-active:before {
    content: "";
    display: block;
    height: 103%;
    width: 30px;
    position: absolute;
    right: -24px;
    top: 14px;
    background: v-bind(backColorRadialGradient);
    transform: rotateX(180deg);
  }

}

:deep(.ant-tabs-tab-with-remove) {
  display: flex;
  justify-content: space-between;
}

.layoutContainer {
  height: @height;
}

:deep(.lm_header .lm_tab .lm_title){
  white-space: nowrap;
  max-width: 100px;
}

</style>

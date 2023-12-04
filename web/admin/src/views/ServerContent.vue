<script setup>
import PFlip from "@/components/p-flip.vue";
import PSftp from "@/components/p-sftp.vue";
import {message} from "ant-design-vue";
import {defineExpose, defineProps, nextTick, ref} from "vue";
import {useStorage} from "@vueuse/core";

const props = defineProps({
  server: {
    type: [Object], default: () => {
    },
  }
})


const remarkStatus = ref(false)

let flipStatus = ref(false)

// let fullscreenContent = ref(null)

let frontColor = useStorage('frontColor', "#ffffff")
let backColor = useStorage('backColor', "#000000")

const hexToRgb = (hex) => {
  let rgb = [];
  hex = hex.substr(1);//去除前缀 # 号

  if (hex.length === 3) { // 处理 "#abc" 成 "#aabbcc"
    hex = hex.replace(/(.)/g, '$1$1');
  }

  hex.replace(/../g, function (color) {
    rgb.push(parseInt(color, 0x10));//按16进制将字符串转换为数字
  });

  return `rgb(${rgb.join(", ")})`;
}

let sftpEnable = ref(false)

let flip = ref(null)

// let link = ref(true)


let sftpEl = ref([])

let iframeRef = ref(null)


const reloadSftp = () => {
  sftpEl.value.init()
}

const changeDir = (dir) => {
  sftpEl.value.changeDir(dir)
}

const reloadServer = () => {
  // spinning.value = true;
  let server = props.server
  let reloadIndex = server.url.indexOf("&reload=1");
  if (reloadIndex !== -1) {
    server.url = server.url.slice(0, reloadIndex)
    return
  }
  server.url = server.url + "&reload=1"
}

const handleRequestFullscreen = () => {
  iframeRef.value.requestFullscreen()
}

const handleExecCommand = (command) => {
  iframeRef.value.contentWindow.postMessage({command: command}, '*')
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

defineExpose({
  handleExecCommand,
  changeDir,
  server: props.server
})


</script>

<template>
  <div class="split-box" >
    <p-flip ref="flip" :operation-id="server.operationId">
      <template #back>
        <div v-show="sftpEnable">
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

              <a-button type="link" @click="reloadServer">
                <template v-slot:icon>
                  <reload-outlined/>
                </template>
              </a-button>
            </template>

            <div style="display: flex" >
              <div style="width: 100%;position: relative;">
                <iframe class="ssh"
                        title="ssh"
                        ref="iframeRef"
                        :id="server.operationId"
                        :src="server.url+'&bgcolor='+hexToRgb(backColor)+'&fontcolor='+hexToRgb(frontColor)">
                </iframe>
                <div style="position: absolute;right: 16px;top: calc(50% - 1em / 2);color: aliceblue"
                     @click="remarkStatus=!remarkStatus" v-if="server.remark">
                  <left-outlined :class="{'button-action':remarkStatus,'left':true}"/>
                </div>
                <div style="position: absolute;right: 16px;top: 16px;color: aliceblue" class="left" @click="handleRequestFullscreen">
                  <fullscreen-outlined/>
                </div>
              </div>
              <div :class="{remark:true,'remark-enter':remarkStatus}">
                <a-card title="备注" style="margin-left: 8px">
                  <div class="w-e-text-container">
                    <div data-slate-editor v-html="server.remark">

                    </div>
                  </div>
                </a-card>
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
      width: 100%;
      height: 500px;
      border: none;
      //resize: vertical; /* 可以调整宽度和高度 */
    }

    .remark {
      transition: all 0.3s;
      overflow: scroll;
      max-height: 500px;
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
    margin-top: 12px;

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
</style>

<script setup>

import {formatSecondsMax} from "@/components/process";
import {onMounted, ref} from "vue";
import {applicationApi} from "@/api/application";
import {serverApi} from "@/api/server";
import autoAnimate from "@formkit/auto-animate";
import * as echarts from 'echarts';

let applicationErrorRankData = ref([])
const getApplicationErrorRank = async () => {
  let data = await applicationApi.getApplicationErrorRank()
  let i = 1;
  data.forEach(item => {
    item.rank = i++
  })
  applicationErrorRankData.value = data
}

getApplicationErrorRank()


let columns = [
  {
    title: '排名',
    dataIndex: 'rank',
    key: 'rank',
    width: '60px'
  },
  {
    title: '应用',
    dataIndex: 'applicationContent',
    key: 'applicationContent',
  },
  {
    title: '宕机时间',
    key: 'downTime',
    dataIndex: 'downTime',
    width: '100px',
  },
  {
    title: '年稳定性',
    key: 'yearStability',
  },
];

let maxDistNum = ref(3);
let serverRunInfo = ref([])
const getServerRunInfo = async () => {
  serverRunInfo.value = await serverApi.getAllServerRunInfo()
  serverRunInfo.value.forEach(item => {
    if (!item.infoStatus) {
      return
    }
    item.diskUsages = JSON.parse(item.diskUsages)
    item.networkUsages = JSON.parse(item.networkUsages)
    item.cpuUsage = JSON.parse(item.cpuUsage)
    maxDistNum = Math.max(maxDistNum, item.diskUsages.length)
    item.diskUsages.forEach(d => {
      d.useNum = parseFloat(d.use.replace('%', ''))
    })

    if (item.detail){
      item.detail.forEach(d => {
        d.diskUsages = JSON.parse(d.diskUsages)
        d.networkUsages = JSON.parse(d.networkUsages)
        d.cpuUsage = JSON.parse(d.cpuUsage)
      })
    }
  })

  //硬盘空间有一个大于80的置顶,有多个大于80的按使用率排序
  serverRunInfo.value.sort((a, b) => {
    if (!a.infoStatus && !b.infoStatus) {
      return 0
    }
    if (!b.infoStatus && a.infoStatus) {
      return -1
    }
    if (!a.infoStatus && b.infoStatus) {
      return 1
    }
    let aMax = Math.max(...a.diskUsages.map(d => d.useNum))
    let bMax = Math.max(...b.diskUsages.map(d => d.useNum))
    if (aMax > 80 && bMax > 80) {
      return bMax - aMax
    } else if (aMax > 80) {
      return -1
    } else if (bMax > 80) {
      return 1
    } else {
      return 0
    }
  })
}

getServerRunInfo();

let height = ref(window.innerHeight*0.8)
let width = ref(window.innerWidth*0.6)

const openDetail = async (item, index) => {
  let i = serverRunInfo.value.findIndex(i => i.active)

  serverRunInfo.value.forEach(i => {
    i.active = false;
    i.transform = ''
  })

  if (index !== i) {
    item.active = true;
  }

//根据浏览器窗口居中
  let me = serverItem.value[index]
  let meWidth = width.value
  let meHeight = height.value
  let mePos = me.getBoundingClientRect()
  let windowWidth = window.innerWidth
  let windowHeight = window.innerHeight
  //视口大小
  let left = windowWidth / 2 - meWidth / 2 - mePos.left
  let top = windowHeight / 2 - meHeight / 2 - mePos.top

  //y轴反转180度，放大到2 倍
  if (item.active) {
    item.transform = `translateX(${left}px) translateY(${top}px)  rotate3d(0,1,0,180deg) `
  } else {
    item.transform = ''
  }

  updateChart(item, index)
}

const updateChart = (item, index) => {
  let myChart = echarts.init(chartCpu.value[index]);
  let option;
  option = {
    title: {
      text: 'cpu使用率'
    },
    tooltip: {
      trigger: 'axis',
      formatter: function (params) {
        params = params[0];
        var date = new Date(params.name);
        console.log(params)
        return (
            date.getDate() +
            '/' +
            (date.getMonth() + 1) +
            '/' +
            date.getFullYear() +
            ' : ' +
            params.value[1]
        );
      },
      axisPointer: {
        animation: false
      }
    },
    xAxis: {
      type: 'time',
      splitLine: {
        show: false
      }
    },
    yAxis: {
      type: 'value',
      boundaryGap: [0, '100%'],
      splitLine: {
        show: false
      }
    },
    series: [
      {
        name: '日期',
        type: 'line',
        showSymbol: false,
        data: item.detail.map(d => ({name:new Date(d.date).toString(), value:[new Date(d.date).getTime(),d.cpuUsage.us]}))
      }
    ]
  };
  myChart.setOption(option);


  let chartDiskChart = echarts.init(chartDisk.value[index]);
  let chartDiskOption;
  chartDiskOption = {
    title: {
      text: '硬盘使用率'
    },
    tooltip: {
      trigger: 'axis',
      formatter: function (params) {
        params = params[0];
        var date = new Date(params.name);
        console.log(params)
        return (
            date.getDate() +
            '/' +
            (date.getMonth() + 1) +
            '/' +
            date.getFullYear() +
            ' : ' +
            params.value[1]
        );
      },
      axisPointer: {
        animation: false
      }
    },
    xAxis: {
      type: 'time',
      splitLine: {
        show: false
      }
    },
    yAxis: {
      type: 'value',
      boundaryGap: [0, '100%'],
      splitLine: {
        show: false
      }
    },
    series: [
      {
        name: '日期',
        type: 'line',
        showSymbol: false,
        data: item.detail.map(d => ({name:new Date(d.date).toString(), value:[new Date(d.date).getTime(),d.diskUsages[0].use.replace('%','')]}))
      }
    ]
  };
  chartDiskChart.setOption(chartDiskOption);

  let memoryChart = echarts.init(chartMemory.value[index]);
  let memoryOption;
  memoryOption = {
    title: {
      text: '内存使用率'
    },
    tooltip: {
      trigger: 'axis',
      formatter: function (params) {
        params = params[0];
        var date = new Date(params.name);
        console.log(params)
        return (
            date.getDate() +
            '/' +
            (date.getMonth() + 1) +
            '/' +
            date.getFullYear() +
            ' : ' +
            params.value[1]
        );
      },
      axisPointer: {
        animation: false
      }
    },
    xAxis: {
      type: 'time',
      splitLine: {
        show: false
      }
    },
    yAxis: {
      type: 'value',
      boundaryGap: [0, '100%'],
      splitLine: {
        show: false
      }
    },
    series: [
      {
        name: '日期',
        type: 'line',
        showSymbol: false,
        data: item.detail.map(d => ({name:new Date(d.date).toString(), value:[new Date(d.date).getTime(),d.diskUsages[0].use.replace('%','')]}))
      }
    ]
  };
  memoryChart.setOption(memoryOption);
}

let chartCpu = ref([])
let chartDisk = ref([])
let chartMemory = ref([])
let chartNetwork = ref([])
let animateRef = ref()
let serverItem = ref([])
onMounted(() => {
  autoAnimate(animateRef.value, {duration: 300})
})


</script>

<template>
  <div class="dashboard-page">
    <div class="root" ref="animateRef">
      <div v-for="(item,index) in serverRunInfo"
           :key="item.serverId"
           ref="serverItem"
           :class="{'server-item':true, 'server-item-active': item.active}"
           style="position: relative;cursor: pointer;transform-style: preserve-3d;"
           :style="{'grid-row': `span ${(item.diskUsages?item.diskUsages.length * 1:0)+4}`,'transform': item.transform,'z-index': item.active? 1001 : 1}">
        <div class="front">
          <div class="server-name">{{ item.serverName }}</div>
          <div>
            <span v-if="item.infoStatus">     <a-badge status="success" text="运行中"/></span>
            <span v-else>   <a-badge status="error" text="离线"/></span>
          </div>
          <div>{{ item.serverIp }}:{{ item.serverPort }}</div>
          <div class="disk-usages">
            <div class="disk-usage" v-for="(d,index) in item.diskUsages" :key="d.filesystem">
              <div class="disk-name">
                <span v-if="d.mountedOn === '/'">系统硬盘： </span>
                <span v-else>数据硬盘{{ index }}：</span>
                <span>
            <a-progress :percent="d.useNum" status="normal" :strokeColor="d.useNum >= 80 ? '#f5222d' : '#1890ff'"/>
              </span>
              </div>
            </div>
          </div>
        </div>
        <div class="back">
          <div class="detail">
            <div class="title">{{ item.serverName }}</div>
            <div style="height: 100%;width: 100%;overflow: hidden;">
              <div ref="chartCpu"  style="display: inline-block" :style="{width: width/2-10+'px',height: height/2-10+'px'}"></div>
              <div ref="chartDisk" style="display: inline-block" :style="{width: width/2-10+'px',height: height/2-10+'px'}"></div>
              <div ref="chartMemory" style="display: inline-block" :style="{width: width/2-10+'px',height: height/2-10+'px'}"></div>
              <div ref="chartNetwork" style="display: inline-block" :style="{width: width/2-10+'px',height: height/2-10+'px'}"></div>
            </div>

          </div>
        </div>
      </div>
    </div>
    <div style="padding: 20px;width: 50%;">
      <div
          style=" background-color: #E45F2B;color: #fff;padding: 10px;font-size: 18px;font-weight: bold;text-align: center;">
        {{ new Date().getFullYear() }}年宕机时间排行榜
      </div>
      <div style="color: #fff;margin-top: 10px;font-size: 16px">
        <a-table :columns="columns" :data-source="applicationErrorRankData" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'downTime'">
              {{ formatSecondsMax(record.errorSeconds) }}
            </template>
            <template v-if="column.key === 'yearStability'">
              {{ ((365 * 24 * 60 * 60 - record.errorSeconds) / (365 * 24 * 60 * 60) * 100).toFixed(2) }}%
            </template>
          </template>
        </a-table>
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
.dashboard-page {
  height: @height;
  overflow-y: scroll;

  .root {
    width: calc(100vw - 180px);

    padding: 8px;
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    grid-auto-rows: 10px;
    column-gap: 20px;
    row-gap: 20px;

    .server-item {
      //overflow: scroll;
      transition: all 1s ease-in-out;
      //backface-visibility: hidden;
      cursor: pointer;
      perspective: 1000px; /* 添加透视效果 */
      transform-style: preserve-3d;
      border-radius: 10px;
      max-width: 220px;
      max-height: 220px;
      box-shadow: 0 3px 6px -4px #0000001f, 0 6px 16px #00000014, 0 9px 28px 8px #0000000d;

      .front {
        position: absolute;
        left: 0;
        top: 0;
        backface-visibility: hidden;
        display: flex;
        flex-direction: column;
        align-items: center;
        width: 220px;
        background-color: #94cefa;
        border-radius: 10px;
        //margin-left: 8px;
        //margin-top: 16px;
        padding: 8px;
        box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
      }

      .back {
        position: absolute;
        left: 0;
        top: 0;
        transform: rotateY(180deg);
        z-index: 1000;
        backface-visibility: hidden;
        //width: calc(v-bind(width) * 1px);
        //height: calc(v-bind(height) * 1px);
        pointer-events: auto;

        .detail {
          padding: 8px;
          width: 100%;
          height: 100%;

          .title {
            text-align: center;
            font-size: 16px;
            font-weight: bold;
            margin-bottom: 8px;
            color: green;
          }
        }

      }

      &-active {
        //移动到中间位置
        //transform: translate3d(-50%, -50%, 0);
        max-width: calc(v-bind(width) * 1px);
        max-height: calc(v-bind(height) * 1px);
        width: calc(v-bind(width) * 1px);
        height: calc(v-bind(height) * 1px);
        background-color: #fff;

      }

      .server-name {
        color: green;
        font-size: 18px;
        font-weight: bold;
        width: 100%;
        text-align: center;
        //溢出隐藏
        white-space: nowrap;
        text-overflow: ellipsis;
        overflow: hidden;
        margin-bottom: 4px;
      }


      .disk-usages {
        width: 100%;


        .disk-usage {
          margin-top: 2px;

          .disk-name {
            display: flex;

            span:nth-of-type(1) {
              width: 40%;
            }

            span:nth-of-type(2) {
              width: 60%;
            }
          }
        }
      }
    }


  }
}

</style>
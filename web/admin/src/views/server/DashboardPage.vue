<script setup>

import {formatSecondsMax} from "@/components/process";
import {defineEmits, nextTick, onMounted, ref} from "vue";
import {applicationApi} from "@/api/application";
import {serverApi} from "@/api/server";
import autoAnimate from "@formkit/auto-animate";
import echarts from '@/boot/bootEcharts'
import {computedFileSize} from "@/utils/File";
import {useStorage} from "@vueuse/core";

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
    item.diskUsages.forEach(d => {
      d.useNum = parseFloat(d.use.replace('%', ''))
    })

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
    return  bMax - aMax
  })
}

getServerRunInfo();

// let interval = setInterval(() => {
//   getServerRunInfo()
// }, 1000 * 120)
//
// onBeforeUnmount(() => {
//   clearInterval(interval)
// })

const emit = defineEmits(['openServer','findServer'])

let currentServer = ref(null)
let detailLoading = ref(false)
const openDetail = async (item) => {
  detailLoading.value = true
  detailVisible.value = true
  if (!item.infoStatus) {
    emit('findServer', item.server)
    return
  }
  item.detail = await serverApi.getServerDetail(item.serverId)
  if (item.detail) {
    item.detail.forEach(d => {
      d.diskUsages = JSON.parse(d.diskUsages)
      d.networkUsages = JSON.parse(d.networkUsages)
      d.cpuUsage = JSON.parse(d.cpuUsage)
    })
  }

  currentServer.value = item
  await nextTick()
  updateChart(item)
  detailLoading.value = false
}

let allChart = [];
const updateChart = (item) => {
  let myChart = echarts.init(chartCpu.value);
  allChart.push(myChart);
  myChart.clear()
  let option;
  option = {
    title: {
      text: 'cpu使用率',
      left: 'center',
      bottom: 0
    },
    tooltip: {
      trigger: 'axis',
      formatter: function (params) {
        params = params[0];
        var date = new Date(params.name);
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
      max: 100,
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
        data: item.detail.map(d => ({
          name: new Date(d.date).toString(),
          value: [new Date(d.date).getTime(), d.cpuUsage.us]
        }))
      }
    ]
  };
  myChart.setOption(option);


  let chartDiskChart = echarts.init(chartDisk.value);
  allChart.push(chartDiskChart)
  chartDiskChart.clear()
  let chartDiskOption;
  let diskRes = {}
  item.detail.forEach(e => {
    e.diskUsages.forEach(d => {
      if (!diskRes[d.filesystem]) {
        diskRes[d.filesystem] = []
      }
      diskRes[d.filesystem].push({date: e.date, data: d})
    })
  })
  chartDiskOption = {
    title: {
      text: '硬盘使用率',
      left: 'center',
      bottom: 0
    },
    legend: {
      data: Object.keys(diskRes)
    },
    tooltip: {
      trigger: 'axis',
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
      max: 100,
      boundaryGap: [0, '100%'],
      splitLine: {
        show: false
      }
    },
    series: Object.keys(diskRes).map(k => ({
      name: k,
      type: 'line',
      showSymbol: false,
      data: diskRes[k].map(d => ({
        name: new Date(d.date).toString(),
        value: [new Date(d.date).getTime(), d.data.use.replace('%', '')]
      }))
    }))
  };

  chartDiskChart.setOption(chartDiskOption);


  let networkRes = {}
  item.detail.forEach(e => {
    e.networkUsages.forEach(d => {
      if (!networkRes[d.interfaceName]) {
        networkRes[d.interfaceName] = []
      }
      networkRes[d.interfaceName].push({date: e.date, data: d})
    })
  })

  let chartNetworkTransmitChart = echarts.init(chartNetworkTransmit.value);
  chartNetworkTransmitChart.clear()
  allChart.push(chartNetworkTransmitChart)
  let chartNetworkTransmitOption;
  chartNetworkTransmitOption = {
    title: {
      text: '上行网络使用率',
      left: 'center',
      bottom: 0
    },
    tooltip: {
      trigger: 'axis',
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
      },
      axisLabel: {
        formatter: function (value) {
          return computedFileSize(value)
        }
      }
    },
    series: Object.keys(networkRes).map(k => ({
      name: k,
      type: 'line',
      showSymbol: false,
      data: networkRes[k].map(d => ({
        name: new Date(d.date).toString(),
        value: [new Date(d.date).getTime(), d.data.transmitBytes]
      }))
    }))
  };
  chartNetworkTransmitChart.setOption(chartNetworkTransmitOption);

  let chartNetworkReceiveChart = echarts.init(chartNetworkReceive.value);
  chartNetworkReceiveChart.clear()
  let chartNetworkReceiveOption;
  chartNetworkReceiveOption = {
    title: {
      text: '下行网络使用率',
      left: 'center',
      bottom: 0
    },
    tooltip: {
      trigger: 'axis',
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
      },
      axisLabel: {
        formatter: function (value) {
          return computedFileSize(value)
        }
      }
    },
    series: Object.keys(networkRes).map(k => ({
      name: k,
      type: 'line',
      showSymbol: false,
      data: networkRes[k].map(d => ({
        name: new Date(d.date).toString(),
        value: [new Date(d.date).getTime(), d.data.receiveBytes]
      }))
    }))
  };
  chartNetworkReceiveChart.setOption(chartNetworkReceiveOption);

  const resizeObserver = new ResizeObserver(() => {
    myChart.resize();
    chartDiskChart.resize();
    chartNetworkTransmitChart.resize();
    chartNetworkReceiveChart.resize();
  });

  resizeObserver.observe(window.document.body);
}

let chartCpu = ref()
let chartDisk = ref()
let chartNetworkTransmit = ref()
let chartNetworkReceive = ref()
let animateRef = ref()
let serverItem = ref([])
onMounted(() => {
  autoAnimate(animateRef.value, {duration: 600})
})

let detailVisible = ref(false)
const handleDetailOk = () => {
  detailVisible.value = false
  emit('openServer', {
    ...currentServer.value.server
  }, 0)
}

let miniTabBar = useStorage('miniTabBar', false)

const handleDetailClose = () => {
  detailVisible.value = false
}

</script>

<template>
  <div class="dashboard-page">
    <div class="root" ref="animateRef">
      <div v-for="(item,index) in serverRunInfo"
           :key="item.serverId"
           ref="serverItem"
           :class="{'server-item':true, 'server-item-active': item.active}"
           @click="openDetail(item)"
           :style="{'grid-row': `span ${(item.diskUsages?Math.round(item.diskUsages.length * 0.8):0)+4}`}">
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
      </div>
    </div>
    <a-modal v-model:visible="detailVisible" :title="currentServer?.serverName" @ok="handleDetailOk"
             @close="handleDetailClose" width="100%"
             wrap-class-name="full-modal" ok-text="进入服务器" cancel-text="关闭">
      <a-spin :spinning="detailLoading">
      <div class="dashboard-page-detail">
        <div class="dashboard-page-detail-chart" ref="chartCpu"></div>
        <div class="dashboard-page-detail-chart" ref="chartDisk"></div>
        <div class="dashboard-page-detail-chart" ref="chartNetworkTransmit"></div>
        <div class="dashboard-page-detail-chart" ref="chartNetworkReceive"></div>
      </div>
      </a-spin>
    </a-modal>
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

<style lang="less">
.full-modal {
  .ant-modal {
    max-width: 100%;
    top: 0;
    padding-bottom: 0;
    margin: 0;
  }

  .ant-modal-content {
    display: flex;
    flex-direction: column;
    height: calc(100vh);
  }

  .ant-modal-body {
    flex: 1;
  }
}

.dashboard-page-detail {
  padding: 8px;
  width: 100%;
  height: calc(100vh - 160px);
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: space-between;

  .dashboard-page-detail-chart {
    width: 46%;
    height: 49%;
  }
}
</style>
<style scoped lang="less">


.dashboard-page {
  height: @height;
  overflow-y: scroll;

  .root {
    width: calc(100vw - v-bind(miniTabBar) * 180px);

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
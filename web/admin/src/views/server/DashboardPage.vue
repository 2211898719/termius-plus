<script setup>

import {formatSecondsMax} from "@/components/process";
import {ref} from "vue";
import {applicationApi} from "@/api/application";
import {serverApi} from "@/api/server";


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
    item.diskUsages = JSON.parse(item.diskUsages)
    item.networkUsages = JSON.parse(item.networkUsages)
    item.cpuUsage = JSON.parse(item.cpuUsage)
    maxDistNum = Math.max(maxDistNum, item.diskUsages.length)
    item.diskUsages.forEach(d => {
      d.useNum = d.use.replace('%', '')
    })
  })
}

getServerRunInfo();

</script>

<template>
  <div class="root">
    <div v-for="item in serverRunInfo"
         :key="item.serverId"
         class="server-item">
      <div class="server-name">{{ item.serverName }}</div>
      <div>正常运行</div>
      <div>{{ item.serverIp }}:{{ item.serverPort }}</div>
      <div class="disk-usages">
        <div class="disk-usage" v-for="(d,index) in item.diskUsages" :key="d.filesystem">
          <div class="disk-name" v-if="d.mountedOn === '/'"><span>系统硬盘： </span>
            <span>
            <a-progress :percent="d.useNum" status="active"/>
            </span>
          </div>
          <div class="disk-name" v-else>
            <span>数据硬盘{{ index }}：</span>
            <span>
            <a-progress :percent="d.useNum" status="normal" :strokeColor="d.useNum >= 80 ? '#f5222d' : '#1890ff'"/>
              </span>
          </div>
        </div>
      </div>
    </div>
    <div style="padding: 20px;width: 30%;">
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
.root {
  width: calc(100vw - 180px);
  height: @height;
  overflow-y: scroll;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-evenly;
  padding: 8px;

  .server-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 248px;
    height: calc(v-bind(maxDistNum) * 34px + 120px);
    background-color: #94cefa;
    //margin-left: 8px;
    margin-top: 16px;
    padding: 8px;
    border-radius: 10px;
    box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
    overflow: scroll;
    .server-name{
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
</style>
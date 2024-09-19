<script setup>

import {formatSecondsMax} from "@/components/process";
import {ref} from "vue";
import {applicationApi} from "@/api/application";


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
</script>

<template>
  <div style="text-align: center;width: 200px;height: 200px;background-color: #1daa6c">
    <div>广西教务</div>
    <div>正常运行</div>
    <div>
      <div>内存：123</div>
      <div>硬盘：123</div>
      <div>cpu：123</div>
    </div>
  </div>
  <!--  <div style="padding: 20px;width: 30%;">-->
  <!--    <div-->
  <!--        style=" background-color: #E45F2B;color: #fff;padding: 10px;font-size: 18px;font-weight: bold;text-align: center;">-->
  <!--      {{ new Date().getFullYear() }}年宕机时间排行榜-->
  <!--    </div>-->
  <!--    <div style="color: #fff;margin-top: 10px;font-size: 16px">-->
  <!--      <a-table :columns="columns" :data-source="applicationErrorRankData" :pagination="false">-->
  <!--        <template #bodyCell="{ column, record }">-->
  <!--          <template v-if="column.key === 'downTime'">-->
  <!--            {{ formatSecondsMax(record.errorSeconds) }}-->
  <!--          </template>-->
  <!--          <template v-if="column.key === 'yearStability'">-->
  <!--            {{ ((365 * 24 * 60 * 60 - record.errorSeconds) / (365 * 24 * 60 * 60) * 100).toFixed(2) }}%-->
  <!--          </template>-->
  <!--        </template>-->
  <!--      </a-table>-->
  <!--    </div>-->
  <!--  </div>-->
</template>

<style scoped lang="less">

</style>
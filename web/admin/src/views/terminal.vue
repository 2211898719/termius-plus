<template>
  <a-button @click="add">加一个</a-button>
  <a-button @click="test">测试</a-button>
  <div class="ssh-list">
   <a-tabs style="width: 100%;"
           :tabBarGutter="5"
           v-model:activeKey="activeKey"
           type="editable-card"
           :tab-position="'left'"
           @edit="onEdit">
     <template v-slot:addIcon></template>
     <template v-for="item in ssh" :key="item.id">
        <a-tab-pane :tab="item.id">
            <iframe class="ssh"
                    title="ssh"
                    :src="item.url">
          </iframe>
        </a-tab-pane>
     </template>
  </a-tabs>
  </div>
</template>
<script setup>
  import {ref} from "vue";

  let activeKey = ref('1');

  let ssh = ref([
    {
      id: 1,
      url: 'http://localhost:8888/?hostname=121.36.246.56&username=root&password=OU1zITJCSG5CZm1FY05RNjJWSXI='/*+"&proxyType=2&proxyIp=192.168.100.82&proxyPort=1082&proxyRdns=true"*/
    },
  ]);


  const add = () => {
    ssh.value.push(
        {
          id: 2,
          url: 'http://localhost:8888/?hostname=121.36.246.56&username=root&password=OU1zITJCSG5CZm1FY05RNjJWSXI='/*+"&proxyType=2&proxyIp=192.168.100.82&proxyPort=1082&proxyRdns=true"*/
        },
    );
  };

  const onEdit = (e) => {
    ssh.value = ssh.value.filter(item => item.id !== e)
  }

  const test = () => {
    ssh.value[0].url += '&command=pwd';
  };
</script>
<style>
  .ssh-list {
    display: flex;
    flex-direction: row;
    margin-top: 10px;
    flex-wrap: wrap;
    width: 100%;
    justify-content: space-around;
  }

  .ssh {
    width: 100%;
    height: 500px;
    border: none;
    resize: both; /* 可以调整宽度和高度 */
    margin: 5px;
  }
</style>

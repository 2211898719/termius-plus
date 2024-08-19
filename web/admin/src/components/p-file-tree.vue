<script setup>

import {defineProps, ref, watch} from "vue";
import {sftpApi} from "@/api/sftp";
import {message} from "ant-design-vue";
import _ from "lodash";

const props = defineProps({
  sftpId: {
    type: String, default: ""
  },
  initialPath: {
    type: String, default: ""
  },
  treeData: {
    type: Array, default: null
  }
});

const emit = defineEmits(['openFileInCodeEditor'])

let treeData = ref([])

const sortFileByName = (files) => {
  //文件夹在前，文件在后，相同类型按字母顺序排序
  return _.sortBy(files, (file) => {
    if (file.attributes.type === 'DIRECTORY') {
      return '0' + file.name
    } else if (file.attributes.type === 'REGULAR') {
      return '1' + file.name
    } else {
      return '2' + file.name
    }
  })
}


const openCode = async () => {
  treeData.value = sortFileByName((await sftpApi.ls({id: props.sftpId, remotePath: props.initialPath})))
  treeData.value.forEach(item => {
    if (item.attributes.type === 'DIRECTORY') {
      item.children = []
    } else {
      item.children = false
    }
  })
}

watch(() => props.initialPath, async (e) => {
  if (e && !props.treeData) {
    await openCode()
  } else {
    treeData.value = props.treeData
  }
})

watch(()=>props.treeData, (e) => {
  treeData.value = e
})

if (props.initialPath && !props.treeData) {
  openCode()
} else {
  treeData.value = props.treeData
}

const openSubMenu = async (file) => {
  if (file.attributes.type === 'REGULAR') {
    emit('openFileInCodeEditor', file)
    return
  } else {
    file.spinning = true
    try {
      file.children = sortFileByName(await sftpApi.ls({id: props.sftpId, remotePath: file.path}))
    } catch (e) {
      console.error(e)
      message.error(e.message)
    }finally {
      file.spinning = false
    }
  }
  console.log(file)
}

const currentOpenFileInCodeEditor = (file) => {
  emit('openFileInCodeEditor', file)
}

</script>

<template>
  <a-menu
      mode="inline"
  >
    <template :key="item.path" v-for="item in treeData">
      <a-spin :spinning="!!item.spinning" v-if="item.attributes.type==='DIRECTORY' || item.attributes.type==='SYMLINK'">
        <a-sub-menu @titleClick="openSubMenu(item)" :key="item.path+'----subMenu'">
          <template #icon>
            <folder-outlined v-if="item.attributes.type==='DIRECTORY'"/>
            <link-outlined v-else-if="item.attributes.type==='SYMLINK'"/>
          </template>
          <template #title>{{ item.name }}</template>
          <p-file-tree v-if="item.children" :treeData="item.children" :sftpId="sftpId"
                       @openFileInCodeEditor="currentOpenFileInCodeEditor"/>
        </a-sub-menu>
      </a-spin>
      <a-menu-item v-else-if="item.attributes.type==='REGULAR'" @click="openSubMenu(item)"
                   :key="item.path+'----menuItem'">
        <template #icon>
          <file-outlined/>
        </template>
        {{ item.name }}
      </a-menu-item>

    </template>
  </a-menu>
</template>

<style scoped lang="less">

</style>

<script setup>

import {defineProps} from "vue";
import {sftpApi} from "@/api/sftp";
import {message} from "ant-design-vue";
import _ from "lodash";

const props = defineProps({
  treeData: {
    type: [Array], default: () => [],
  },
  sftpId: {
    type: String, default: ""
  }
});

const emit = defineEmits(['openFileInCodeEditor'])

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

const openSubMenu = async (file) => {
  if (file.attributes.type === 'REGULAR') {
    emit('openFileInCodeEditor', file)
    return
  } else {
    try {
      file.children = sortFileByName(await sftpApi.ls({id: props.sftpId, remotePath: file.path}))
    } catch (e) {
      console.error(e)
      message.error(e.message)
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
      <a-sub-menu v-if="item.attributes.type==='DIRECTORY' || item.attributes.type==='SYMLINK'"
                  @titleClick="openSubMenu(item)">
        <template #icon>
          <folder-outlined v-if="item.attributes.type==='DIRECTORY'"/>
          <link-outlined v-else-if="item.attributes.type==='SYMLINK'"/>
        </template>
        <template #title>{{ item.name }}</template>
        <p-file-tree v-if="item.children" :treeData="item.children" :sftpId="sftpId" @openFileInCodeEditor="currentOpenFileInCodeEditor"/>
      </a-sub-menu>
      <a-menu-item :key="item.path" v-else-if="item.attributes.type==='REGULAR'" @click="openSubMenu(item)">
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

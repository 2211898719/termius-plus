<script setup>
import {defineEmits, reactive, ref, watch} from "vue";
import {useForm} from "ant-design-vue/es/form";
import {quartzApi} from "@/api/quartz";
import {message} from "ant-design-vue";
import ServerListPage from "@/views/ServerListPage.vue";
import {Codemirror} from "vue-codemirror";
import {oneDark} from '@codemirror/theme-one-dark'
import {java} from "@codemirror/lang-java";
import {json} from "@codemirror/lang-json";

const creationCronJobType = ref('create');
const creationCronJobState = reactive({
  jobName: "",
  jobGroup: "",
  serverIds: [],
  serverDtos: [],
  params: [],
  cronExpression: "",
  mvelScript: "",
});

const creationCronJobRules = reactive({
  jobName: [
    {
      required: true,
      message: "请输入名称",
    }
  ],
  jobGroup: [
    {
      required: true,
      message: "请输入组",
    }
  ],
  mvelScript: [
    {
      required: true,
      message: "请输入脚本",
    }
  ],
  cronExpression: [
    {
      required: true,
      message: "请输入cron表达式",
    }
  ],
  serverId: [
    {
      required: true,
      message: "请选择服务器",
    }
  ],
});

const emit = defineEmits(['createSuccess', 'cronJobCreation'])

let {
  resetFields: cronJobResetCreationFields,
  validate: cronJobValidateCreation,
  validateInfos: cronJobCreationValidations
} = useForm(creationCronJobState, creationCronJobRules)

let cronJobCreationVisible = ref(false);

watch(cronJobCreationVisible, (visible) => {
  if (!visible) {
    cronJobResetCreationFields();
  }
});

const cronJobData = ref([])

const getCronJobData = async () => {
  cronJobData.value = await quartzApi.list()
}

[...document.getElementsByClassName('markdown-body')].map(body => {
  return {
    title: body.getElementsByTagName('h1')[0].innerHTML,
    des: body.getElementsByTagName('p')[0].innerHTML,
    body: body.innerHTML
  }
})

getCronJobData()

const handleCronJobCreate = async () => {
  try {
    await cronJobValidateCreation();
  } catch (error) {
    cronJobCreationVisible.value = true;
    return false;
  }

  let res;
  try {
    res = await quartzApi[creationCronJobType.value](creationCronJobState);
  } catch (e) {
    message.error(e.message)
    return
  }
  emit('createSuccess', res)
  await getCronJobData();

  cronJobCreationVisible.value = false;
  message.success("操作成功");
}


const handleEditCronJob = (row) => {
  cronJobCreationVisible.value = true;
  creationCronJobType.value = 'update'
  Object.assign(creationCronJobState, row)
}

const cronJobCreation = () => {
  cronJobCreationVisible.value = true;
  creationCronJobType.value = 'create'
}

let selectServerVisible = ref(false);

const handleSelectServer = (server) => {
  if (creationCronJobState.serverIds.includes(server.id)) {
    message.error("已经选择了该服务器");
    return
  }
  creationCronJobState.serverIds.push(server.id);
  creationCronJobState.serverDtos.push(server);
  selectServerVisible.value = false;
}


const handleCloseServer = (item) => {
  quartzApi.delete({jobName: item.jobName, jobGroup: item.jobGroup}).then(() => {
    message.success("操作成功");
    getCronJobData()
  }).catch(e => {
    message.error(e.message)
  })
}

const removeServer = (id) => {
  creationCronJobState.serverIds = creationCronJobState.serverIds.filter(item => item !== id);
  creationCronJobState.serverDtos = creationCronJobState.serverDtos.filter(item => item.id !== id);
}

const extensions = [java(), oneDark]

let editorOptions = ref({
  mode: 'text/x-java',
  theme: 'default',
  lineNumbers: true,
  tabSize: 4,
  autoCloseBrackets: true,
});

const extensionsJson = [json(), oneDark]

let editorOptionsJson = ref({
  mode: 'text/x-json',
  theme: 'default',
  lineNumbers: true,
  tabSize: 4,
  autoCloseBrackets: true,
});


let example = ref(`String script = "df -h | awk '$NF==\\"/\\"{printf \\"%s\\\\n\\", $5}' | cut -d'%' -f1";
res = Integer.parseInt(session.executeCommand(script));
if (res > 50) {
    dingerSender.send(
            MessageSubType.TEXT,
            DingerRequest.request(server.name+"服务器，硬盘空间不足百分之50")
    );
}`)

defineExpose({
  cronJobCreation
})


</script>

<template>
  <div class="server-root">
    <div class="server-pane">

      <a-space direction="vertical" size="middle" style="width: 100%;">
        <a-card>
          <div style="display: flex;justify-content: space-between">
            <div>

            </div>
            <div>
              <a-button @click="cronJobCreation" class="my-button">新增定时任务</a-button>
            </div>
          </div>
          <div class="mt30 server">
            <a-list :grid="{ gutter: 16, column: 4 }" :data-source="cronJobData" row-key="id">
              <template #renderItem="{ item }">
                <a-dropdown :trigger="['contextmenu']">
                  <a-list-item>
                    <template #actions>
                      <a key="list-loadmore-edit">
                        <edit-outlined @click="handleEditCronJob(item)"/>
                      </a>
                    </template>
                    <a-card>
                      <a-skeleton avatar :title="false" :loading="!!item.loading" active>
                        <a-list-item-meta
                            :description="item.jobGroup+'：'+item.serverDtos[0].name+'，'+item.cronExpression"
                        >
                          <template #title>
                            <span>{{ item.jobName }}</span>
                          </template>
                          <template #avatar>
                            <expand-alt-outlined class="icon-server" style="color: #E45F2B;"/>

                          </template>
                        </a-list-item-meta>
                      </a-skeleton>
                    </a-card>

                  </a-list-item>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item key="close" @click="handleCloseServer(item)">
                        <DeleteOutlined/>
                        关闭
                      </a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </template>
            </a-list>
          </div>
        </a-card>
        <a-drawer
            v-model:visible="cronJobCreationVisible"
            :title="creationCronJobType ==='create'?'新增定时任务':'修改定时任务'"
            placement="right"
            width="80%"
            size="large"
        >
          <template #extra>
            <a-space>
              <a-button @click="cronJobCreationVisible = false">取消</a-button>
              <a-button type="primary" @click="handleCronJobCreate">提交</a-button>
            </a-space>
          </template>

          <a-form
              :label-col="{ span: 3 }"
              :wrapper-col="{ span: 18 }"
              autocomplete="off"
          >
            <a-form-item label="任务名称：" v-bind="cronJobCreationValidations.jobName">
              <a-input v-model:value="creationCronJobState.jobName"/>
            </a-form-item>
            <a-form-item label="任务组：" v-bind="cronJobCreationValidations.jobGroup">
              <a-input v-model:value="creationCronJobState.jobGroup"/>
            </a-form-item>
            <a-form-item label="cron表达式：" v-bind="cronJobCreationValidations.cronExpression">
              <a-input v-model:value="creationCronJobState.cronExpression"/>
            </a-form-item>
            <a-form-item label="服务器：" v-bind="cronJobCreationValidations.serverId">
              <div>
              <span v-if="creationCronJobState.serverIds.length">
                <a-tag v-for="item in creationCronJobState.serverDtos" :key="item.id" closable
                       @close="removeServer(item.id)">{{ item.name }}</a-tag>
              </span>
                <a-button type="link" @click="selectServerVisible = true">添加服务器</a-button>
              </div>
            </a-form-item>
            <a-form-item label="参数集：" v-bind="cronJobCreationValidations.params">
              <div style="margin: 5px 0">填写json从脚本中可以param获取</div>
              <a-form-item :label="item.name" v-for="(item,index) in creationCronJobState.serverDtos" :key="item.id">
              <codemirror
                          v-model:model-value="creationCronJobState.params[index]" :extensions="extensionsJson"
                          :options="editorOptionsJson"></codemirror>
              </a-form-item>
            </a-form-item>
            <a-form-item label="脚本：" v-bind="cronJobCreationValidations.mvelScript">
              <codemirror v-model:model-value="creationCronJobState.mvelScript" :extensions="extensions"
                          :options="editorOptions"></codemirror>
              <div style="margin-top: 16px">
                <p>示例:
                  <codemirror v-model:model-value="example" :extensions="extensions"
                              :options="editorOptions"></codemirror>
                </p>

              </div>

            </a-form-item>
            <a-form-item label="常用cron表达式例子：">
              <div>
                <div>（1）0/2 * * * * ? 表示每2秒 执行任务</div>
                <div>（1）0 0/2 * * * ? 表示每2分钟 执行任务</div>
                <div>（1）0 0 2 1 * ? 表示在每月的1日的凌晨2点调整任务</div>
                <div>（2）0 15 10 ? * MON-FRI 表示周一到周五每天上午10:15执行作业</div>
                <div>（3）0 15 10 ? 6L 2002-2006 表示2002-2006年的每个月的最后一个星期五上午10:15执行作</div>
                <div>（4）0 0 10,14,16 * * ? 每天上午10点，下午2点，4点</div>
                <div>（5）0 0/30 9-17 * * ? 朝九晚五工作时间内每半小时</div>
                <div>（6）0 0 12 ? * WED 表示每个星期三中午12点</div>
                <div>（7）0 0 12 * * ? 每天中午12点触发</div>
                <div>（8）0 15 10 ? * * 每天上午10:15触发</div>
                <div>（9）0 15 10 * * ? 每天上午10:15触发</div>
                <div>（10）0 15 10 * * ? 每天上午10:15触发</div>
                <div>（11）0 15 10 * * ? 2005 2005年的每天上午10:15触发</div>
                <div>（12）0 * 14 * * ? 在每天下午2点到下午2:59期间的每1分钟触发</div>
                <div>（13）0 0/5 14 * * ? 在每天下午2点到下午2:55期间的每5分钟触发</div>
                <div>（14）0 0/5 14,18 * * ? 在每天下午2点到2:55期间和下午6点到6:55期间的每5分钟触发</div>
                <div>（15）0 0-5 14 * * ? 在每天下午2点到下午2:05期间的每1分钟触发</div>
                <div>（16）0 10,44 14 ? 3 WED 每年三月的星期三的下午2:10和2:44触发</div>
                <div>（17）0 15 10 ? * MON-FRI 周一至周五的上午10:15触发</div>
                <div>（18）0 15 10 15 * ? 每月15日上午10:15触发</div>
                <div>（19）0 15 10 L * ? 每月最后一日的上午10:15触发</div>
                <div>（20）0 15 10 ? * 6L 每月的最后一个星期五上午10:15触发</div>
                <div>（21）0 15 10 ? * 6L 2002-2005 2002年至2005年的每月的最后一个星期五上午10:15触发</div>
                <div>（22）0 15 10 ? * 6#3 每月的第三个星期五上午10:15触发</div>
              </div>
            </a-form-item>
          </a-form>
          <a-drawer
              v-model:visible="selectServerVisible"
              :title="'选择服务器'"
              placement="right"
              width="80%"
              size="large"
          >
            <server-list-page :column="4" @change="handleSelectServer" :select="true"></server-list-page>
          </a-drawer>
        </a-drawer>
      </a-space>
    </div>
  </div>
</template>

<style scoped lang="less">
@import url('./css/termius');

</style>

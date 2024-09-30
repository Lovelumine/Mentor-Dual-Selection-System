<script setup lang="ts">
import {onMounted, ref, watch} from "vue";
import {useUserInfoStore} from "@/stores/user/UserBasicInformation";
import {http} from "@/utils/http";
const userStore = useUserInfoStore();
import {useTeacherListStore} from "@/stores/TeacherListStore";
const teacherListStore = useTeacherListStore();

const isUtilButtons = ref(false);
const tableData = ref([]);

const handleEdit = (index: number, row) => {
  console.log(index, row)
}
const handleDelete = (index: number, row) => {
  console.log(index, row)
}
function handleIsUtilButtons(target: string) {
  isUtilButtons.value = 'ADMIN' === target;
}
onMounted(() => {
  if (userStore.userInfo) handleIsUtilButtons(userStore.userInfo.role);
  http({
    url: '/search/teachers',
    method: 'GET',
    headers: {
      Accept: '*/*',
      Authorization: 'Bearer ' + localStorage.getItem('token'),
    }
  }).then(res => {
    if (res.data.code === 200) {
      tableData.value = res.data.data;
      for (let i = 0; i < tableData.value.length; i++){
        tableData.value[i].uid
      }
    } else {
      alert(res.data.data.error);
    }
  }).catch(err => {
    alert(JSON.parse(err.requests.responseText).data.error);
  });
})

watch([
  () => userStore.userInfo,
  () => teacherListStore.teacherListSt
], ([newUserInfoVal, newTeacherListVal]) => {
  handleIsUtilButtons(newUserInfoVal.role);
  if (newTeacherListVal){
    console.log('ntlsv', newTeacherListVal);
    tableData.value = newTeacherListVal;
  } else {
    console.log('ntlsv', newTeacherListVal);
  }
})
</script>

<template>
  <div class="title">
    <span>列表内容</span>
  </div>
  <el-table :data="tableData" stripe class="table">
    <el-table-column prop="fullName" label="姓名"  />
    <el-table-column prop="username" label="工号"  />
    <el-table-column prop="email" label="邮箱"  />
    <el-table-column prop="professionalDirection" label="研究专业" show-overflow-tooltip />
    <el-table-column prop="research_direction" label="研究方向" />
    <el-table-column prop="resume" label="介绍" show-overflow-tooltip />
    <el-table-column prop="teacherposition" label="职称"  />
    <el-table-column label="操作" v-if="isUtilButtons">
      <template #default="scope">
        <button class="button" @click="handleEdit(scope.$index, scope.row)">修改</button>
        <button class="button" @click="handleDelete(scope.$index, scope.row)">删除</button>
      </template>
    </el-table-column>
  </el-table>
</template>

<style scoped lang="sass">
.title
  width: 100%
  height: 40px
  background-color: #e2e2e2
  line-height: 40px
  padding-left: 20px
  font-size: 16px
  margin-bottom: 20px
.table
  margin: 0 auto
  width: 98%
  border: 1px solid #005826
  border-radius: 5px
  transition: .3s ease
  .button
    background-color: #005826
    border: 1px solid #005826
    color: white
    width: 60px
    height: 32px
    border-radius: 5px
    font-size: 15px
    transition: .3s ease
  .button:hover
    cursor: pointer
    background-color: #0f7e3f
    border: 1px solid #0f7e3f
  .button:last-child
    margin-left: 10px
    border: 1px solid #bd0000
    background-color: #bd000000
    color: #bd0000
  .button:last-child:hover
    background-color: #bd0000
    color: white
.table:hover
  box-shadow: #005826 0 0 10px
</style>
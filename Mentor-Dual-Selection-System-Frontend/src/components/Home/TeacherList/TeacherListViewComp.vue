<script setup lang="ts">
import {onMounted, ref, watch} from "vue";
import {useUserInfoStore} from "@/stores/user/UserBasicInformation";
const userStore = useUserInfoStore();

const userPermi = ref(null);
const tableData = [
  {
    name: '测试一',
    id: 10001,
    email: 'test@test.com',
    research_direction: '生物信息学、肿瘤多组学',
    link: 'https://test.com',
  },
  {
    name: '测试二',
    id: 10002,
    email: 'test@test.com',
    research_direction: '进化基因组学、生物信息学、动物系统发育与基因组进化',
    link: 'https://test.com',
  },
  {
    name: '测试三',
    id: 10003,
    email: 'test@test.com',
    research_direction: '生态系统演化与修复',
    link: 'https://test.com',
  },
  {
    name: '测试四',
    id: 10004,
    email: 'test@test.com',
    research_direction: '微生物智造与生物防治',
    link: 'https://test.com',
  },
]

const handleEdit = (index: number, row: User) => {
  console.log(index, row)
}
const handleDelete = (index: number, row: User) => {
  console.log(index, row)
}
onMounted(() => {
  if (userStore.userInfo){
    userPermi.value = userStore.userInfo.role;
  }
})

watch(() => userStore.userInfo, (newValue) => {
  userPermi.value = newValue.role;
})
</script>

<template>
  <div class="title">
    <span>列表内容</span>
  </div>
  <el-table :data="tableData" stripe class="table">
    <el-table-column prop="name" label="姓名"  />
    <el-table-column prop="id" label="工号"  />
    <el-table-column prop="email" label="邮箱"  />
    <el-table-column prop="research_direction" label="研究方向" show-overflow-tooltip />
    <el-table-column prop="link" label="主页链接"  />
    <el-table-column label="操作" v-if="userPermi === 'ADMIN'">
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
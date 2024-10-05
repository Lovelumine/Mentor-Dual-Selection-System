<script setup lang="ts">
import {useStudentListStore} from "@/stores/StudentListStore";
import {ref} from "vue";
import {http} from "@/utils/http";

const studentNameValue = ref(null);
const studentIsSelectTeacher = ref(null);

function selectStudentClicked() {
  const studentListStore = useStudentListStore();
  studentListStore.updateIsSelectTeacher(studentIsSelectTeacher);
  if (studentNameValue.value === null) studentNameValue.value = "";
  http({
    url: '/search/students/search',
    method: "GET",
    headers: {
      Accept: '*/*',
      Authorization: 'Bearer ' + localStorage.getItem('token')
    },
    params: {
      name: studentNameValue.value,
    }
  }).then(res => {
    if (res.data.code === 200){
      studentListStore.updateStudentList(res.data.data);
    } else {
      alert(res.data.data.error);
    }
  }).catch(err => {
    alert(JSON.parse(err.requests.responseText).data.error);
  })
}

function resettingClicked(){
  window.location.reload();
}
</script>

<template>
  <div class="search_box">
    <span class="input_item">学生姓名：<input type="text" placeholder="请输入" v-model="studentNameValue"/></span>
<!--    <span class="input_item">学号：<input type="text" placeholder="请输入"/></span>-->
    <span class="input_item">
      <label for="is_select_teacher">选择导师状态：</label>
      <select id="is_select_teacher" v-model="studentIsSelectTeacher">
        <option :value="null">全部</option>
        <option value="no">未选择</option>
        <option value="accepted">已通过</option>
      </select>
    </span>
<!--    <span class="input_item">-->
<!--      <label for="is_teacher_score">导师评分状态：</label>-->
<!--      <select id="is_teacher_score">-->
<!--        <option value="all">全部</option>-->
<!--        <option value="no_search">未评分</option>-->
<!--        <option value="passed">已评分</option>-->
<!--      </select>-->
<!--    </span>-->
    <button class="button" @click="selectStudentClicked">查询</button>
    <button class="button" @click="resettingClicked">重置</button>
  </div>
</template>

<style scoped lang="sass">
.search_box
  margin: 20px auto
.input_item
  font-size: 16px
  margin-left: 20px
  input
    width: 220px
    height: 32px
    transition: .3s ease
    border: 1px solid #989898
    border-radius: 5px
  input:hover
    border: 1px solid #005826
  input:focus
    outline: none
    box-shadow: #005826 0 0 5px
    border: 1px #005826 solid
  select
    width: 220px
    height: 32px
  select
    width: 220px
    height: 32px
    transition: .3s ease
    border: 1px solid #989898
    border-radius: 5px
  select:hover
    border: 1px solid #005826
  select:focus
    outline: none
    box-shadow: #005826 0 0 5px
    border: 1px #005826 solid
.button
  border-radius: 5px
  width: 60px
  height: 32px
  background-color: #005826
  color: white
  margin-left: 20px
  border: 1px solid #005826
  font-size: 15px
  transition: .3s ease
.button:hover
  background-color: #0f7e3f
  border: 1px solid #0f7e3f
.button:last-child
  background-color: white
  border: 1px solid #bd0000
  color: #bd0000
.button:last-child:hover
  background-color: #bd0000
  color: white
</style>
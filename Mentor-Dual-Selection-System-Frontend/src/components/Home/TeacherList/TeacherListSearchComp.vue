<script setup lang="ts">
import {ref} from "vue";
import {http} from "@/utils/http";
import {useTeacherListStore} from "@/stores/TeacherListStore";
const teacherListStore = useTeacherListStore();

const selectTeacherValue = ref(null);

function selectTeacherClicked() {
  console.log(selectTeacherValue.value);
  if (selectTeacherValue.value === null || selectTeacherValue.value === "") {
    alert('请输入正确的内容');
  } else {
    http({
      url: '/search/teachers/search',
      method: 'GET',
      headers: {
        Accept: '*/*',
        Authorization: 'Bearer ' + localStorage.getItem('token')
      },
      params: {
        name: selectTeacherValue.value,
      }
    }).then(res => {
      console.log(res);
      if (res.data.code === 200){
        teacherListStore.updateTeacherList(res.data.data);
      } else {
        alert(res.data.data.error);
      }
    }).catch(err => {
      alert(JSON.parse(err.requests.responseText).data.error);
    })
  }
}

function resettingClicked() {
  window.location.reload();
}
</script>

<template>
  <div class="search_box">
    导师姓名模糊搜索：
    <input type="text" placeholder="请输入" v-model="selectTeacherValue">
    <button class="button" @click="selectTeacherClicked">查询</button>
    <button class="button" @click="resettingClicked">重置</button>
  </div>
</template>

<style scoped lang="sass">
.search_box
  font-size: 16px
  font-weight: bold
  margin: 0 auto 0 auto
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
  .button
    border-radius: 5px
    width: 60px
    height: 32px
    background-color: #005826
    color: white
    margin-left: 20px
    border: none
    font-size: 15px
    transition: .3s ease
  .button:hover
    background-color: #0f7e3f
  .button:last-child
    background-color: white
    color: #bd0000
  .button:last-child:hover
    background-color: #bd0000
    color: white
</style>
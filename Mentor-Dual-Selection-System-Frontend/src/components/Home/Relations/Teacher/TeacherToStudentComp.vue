<script setup lang="ts">
import {onMounted, ref} from "vue";
import {useRouter} from "vue-router";
import {http} from "@/utils/http";
const router = useRouter();

const studentList = ref([]);

onMounted(() => {
  if (!localStorage.getItem('token')){
    router.push('/');
  } else{
    http({
      url: '/application/mentor-student-relations',
      method: 'GET',
      headers: {
        'Authorization': 'Bearer ' + localStorage.getItem('token'),
        'accept': '*/*'
      }
    }).then(res => {
      if (res.data.code === 200){
        studentList.value = res.data.data[0].students;
        console.log('sts', studentList.value);
      } else {
        alert(res.data.data.error);
      }
    }).catch(err => {
      console.error(err);
      alert('获取信息出错');
      router.push('/');
    });
  }
})
</script>

<template>
  <div class="teacher_to_student_box">
    <h3>与您有过处理申请的学生</h3>
    <div class="item_box">
      <div class="item" v-for="(item, index) in studentList" :key="index">
        <div>学生姓名：{{item.fullName}}</div>
        <div>学生邮箱：{{item.email}}</div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="sass">
.teacher_to_student_box
  position: relative
  display: flex
  align-items: center
  justify-content: center
  margin: 50px auto 20px auto
  width: 50%
  h3
    position: absolute
    top: -40px
    font-weight: bold
  .item_box
    display: flex
    .item
      box-shadow: #005826 0 0 5px
      margin-right: 20px
      padding: 10px
      border-radius: 10px
    .item:last-child
      margin: 0
</style>
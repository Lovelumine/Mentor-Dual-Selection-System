<script setup lang="ts">
import {onMounted, ref} from "vue";
import {http} from "@/utils/http";
import {useRouter} from "vue-router";
const router = useRouter();

const allTeacher = ref([]);
const allStudent = ref([]);
const allPending = ref([]);

onMounted(() => {
  http({
    url: '/user/all',
    method: 'POST',
    headers: {
      'Accept': '*/*',
      'Authorization': 'Bearer ' + localStorage.getItem('token')
    }
  }).then(res => {
    if (res.data.code === 200) {
      for (let i = 0; i < res.data.data.length; i++) {
        if (res.data.data[i].role === 'TEACHER'){
          allTeacher.value.push(res.data.data[i]);
        } else if (res.data.data[i].role === 'STUDENT'){
          allStudent.value.push(res.data.data[i]);
        }
      }
      console.log('allTeacher', allTeacher.value);
      http({
        url: '/application/pending',
        method: 'GET',
        headers: {
          'Accept': '*/*',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        params: {
          'Authorization': localStorage.getItem('token'),
        }
      }).then(res => {
        if (res.data.code === 200) {
          allPending.value = res.data.data;
          for (let i = 0; i < allPending.value.length; i++) {
            switch (allPending.value[i].status) {
              case 'REJECTED':
                allPending.value[i].statusCN = '已拒绝';
                break;
              case 'PENDING':
                allPending.value[i].statusCN = '待审核';
                break;
              case 'ACCEPTED':
                allPending.value[i].statusCN = '已通过';
                break;
              default:
                break;
            }
          }
          for (let i = 0; i < allPending.value.length; i++){
            for (let j = 0; j < allStudent.value.length; j++){
              if (allPending.value[i].studentId === allStudent.value[j].uid){
                allPending.value[i].studentName = allStudent.value[j].fullName;
                break;
              }
            }
          }
        } else{
          alert('获取申请列表失败，请联系管理员修复系统！');
        }
      }).catch(err => {
        console.error(err);
        alert('出现错误，或请重新登录！');
      })
    } else{
      alert('身份验证出错！请重新登入');
      localStorage.removeItem('token');
      window.location.reload();
    }
  }).catch(err => {
    console.error(err);
    alert('请求信息出错！');
    router.back();
  })
})
</script>

<template>
  <div class="all_pending_box">
    <h3>所有处理过的师生关系</h3>
    <div v-for="(tItem, tIndex) in allTeacher" :key="tIndex" class="for_teacher_box">
      <div class="teacher_item">
        导师姓名：{{tItem.fullName}}
      </div>
      <div v-for="(sItem, sIndex) in allPending" :key="sIndex" class="for_student_box">
        <div  v-if="sItem.mentorId === tItem.uid" class="student_item">
          学生姓名：{{sItem.studentName}}（{{sItem.statusCN}}）
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="sass">
.all_pending_box
  position: relative
  margin: 50px auto 20px auto
  width: 50%
  h3
    position: absolute
    top: -40px
    left: 50%
    transform: translateX(-50%)
    font-weight: bold
  .for_teacher_box
    margin-bottom: 20px
    .teacher_item
      padding: 5px
      text-align: center
      box-shadow: #005826 0 0 5px
      border-radius: 10px
      margin: 0 auto 20px auto
      font-size: 17px
      font-weight: bold
    .for_student_box
      display: flex
      .student_item
        width: 50%
        padding: 3px
        text-align: center
        box-shadow: #005826 0 0 5px
        border-radius: 10px
        margin: 0 auto 20px auto
</style>
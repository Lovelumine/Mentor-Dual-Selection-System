<script setup lang="ts">
import {http} from "@/utils/http";
import {onMounted, ref, watch} from "vue";
import {useRouter} from "vue-router";
const router = useRouter();
import {useUserInfoStore} from "@/stores/user/UserBasicInformation";
const userStore = useUserInfoStore();

let allUser = [];
const allTeacher = ref([]);
const selectedTeacher = ref(null);
const selectReason = ref(null);

function toggleSelect(target: number){
  selectedTeacher.value = selectedTeacher.value === target ? null : target;
  console.log(target, selectedTeacher.value);
}
function submitApplication(){
  if (selectedTeacher.value === null || selectReason.value === null) {
    alert('请选择导师并填写选择理由');
  } else {
    console.log('tid', selectedTeacher.value, 'reason', selectReason.value);
    http({
      url: '/application/submit',
      method: 'POST',
      headers: {
        "Content-Type": "*/*",
        "Authorization": `Bearer ${localStorage.getItem("token")}`
      },
      params: {
        mentorId: selectedTeacher.value,
        reason: selectReason.value,
      }
    }).then(res => {
      if (res.data.code === 200) {
        alert('提交申请成功，请等待导师处理。');
      }
    }).catch((err) => {
      alert(JSON.parse(err.request.responseText));
    })
  }
}
onMounted(() => {
  if (!localStorage.getItem('token')){
    router.push('/');
  }
  http({
    url: '/user/all',
    method: 'POST',
    headers: {
      'Content-Type': '*/*',
      'Authorization': 'Bearer ' + localStorage.getItem('token')
    }
  }).then(res => {
    console.log(res);
    if (res.data.code === 200){
      allUser = res.data.data;
      for (let i = 0; i < allUser.length; i++){
        if (allUser[i].role == 'TEACHER'){
          allTeacher.value.push(allUser[i]);
        }
      }
    } else{
      alert('身份验证出错！请重新登入');
      localStorage.removeItem('token');
      window.location.reload();
    }
  }).catch(err => {
    console.error(err);
    alert('请求导师信息出错！');
    router.back();
  })
})
watch(() => userStore.userInfo, (newValue) => {
  if (newValue.role != 'STUDENT') {
    router.push('/');
  }
})
</script>

<template>
  <div class="title">
    <span>选择导师</span>
  </div>
  <div class="container">
    <div class="item_box">
      <div v-for="(item) in allTeacher" :key="item.uid" class="item" @click="toggleSelect(item.uid)">
        <div v-if="selectedTeacher === item.uid" class="select_bar">选择该导师</div>
        <img class="avatar" :src="item.avatarUrl" alt="avatar">
        <div>导师姓名：{{item.fullName}}</div>
        <div>导师邮箱：{{item.email}}</div>
      </div>
    </div>
    <span class="input_item">
      申请该导师的理由：
      <input type="text" placeholder="请精简你的理由（必填）" required v-model="selectReason"/>
      <button class="button" @click="submitApplication">提交申请</button>
    </span>
  </div>
</template>

<style scoped lang="sass">
.title
  width: 100%
  height: 60px
  background-color: #e2e2e2
  line-height: 60px
  padding-left: 20px
  font-size: 20px
.container
  .item_box
    margin: 20px auto 20px 20px
    display: flex
    .item
      margin-right: 20px
      width: 320px
      height: 140px
      box-shadow: #005826 0 0 5px
      border-radius: 10px
      position: relative
      overflow: hidden
      padding: 20px
      .avatar
        border-radius: 5px
        width: 48px
        height: 48px
      .select_bar
        width: 130px
        height: 30px
        position: absolute
        background-color: #005826
        right: 0
        color: white
        line-height: 30px
        padding-left: 10px
        letter-spacing: 2px
    .item:hover
      cursor: pointer

  .input_item
    font-size: 16px
    margin-left: 20px
    input
      width: 400px
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
      width: 100px
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
</style>
<script setup lang="ts">
import {Avatar, Menu} from "@element-plus/icons-vue";
import {ref} from "vue";
import {http} from "@/utils/http";
import {useRouter} from "vue-router";
const router = useRouter();

const signinForm = ref({
  username: '',
  password: ''
});

const forgetPwdClicked = () => {
  router.push('/forgetpwd');
}

function singinClicked(){
  console.log(signinForm.value);
  http({
    url: '/auth/login',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': '*/*',
    },
    data: signinForm.value
  }).then(res => {
    console.log(res.status);
    if (res.status === 200) {
      localStorage.setItem('token', res.data.data.token);
      window.location.reload();
    }
  }).catch((err) => {
    alert(JSON.parse(err.request.responseText).data.error);
  })
}

</script>

<template>
  <div class="container">
    <h3>
      账号登录(系统处于测试阶段，暂未开放)
    </h3>
    <form @submit.prevent="singinClicked">
      <div class="input_box">
        <el-icon size="25" color="#005826"><Avatar /></el-icon>
        <input placeholder="工号 / 学号" type="text" required v-model="signinForm.username" />
      </div>
      <div class="input_box">
        <el-icon size="25" color="#005826"><Menu /></el-icon>
        <input placeholder="密码" type="password" required v-model="signinForm.password" />
      </div>
      <button type="submit" class="register_button">登录</button>
    </form>
    <button type="button" class="forget_button" @click="forgetPwdClicked">忘记密码</button>
  </div>
</template>

<style scoped lang="sass">
.container
  width: 400px
  height: 300px
  position: relative
  margin: 15vh auto 0 auto
  h3
    color: #005826
    text-align: center
    font-weight: bold
  .input_box
    width: 100%
    margin: 30px 0 30px 0
    display: flex
    align-items: center
    border-bottom: 1px solid #005826
    input
      margin-left: 20px
      width: 80%
      border: none
    input:focus
      outline: none
      border: none
    input::placeholder
      font-size: 15px
  .register_button
    border-radius: 5px
    border: none
    background-color: #005826
    color: white
    width: 50%
    height: 35px
    font-size: 16px
    font-weight: bold
    letter-spacing: 5px
    margin-top: 20px
    position: absolute
    left: 50%
    transform: translate(-50%)
    transition: .3s ease
  .register_button:hover, .forget_button:hover
    background-color: #0f7e3f
  .forget_button
    border-radius: 5px
    border: none
    background-color: #005826
    color: white
    width: 50%
    height: 35px
    font-size: 16px
    font-weight: bold
    letter-spacing: 5px
    margin-top: 20px
    position: absolute
    left: 50%
    transform: translate(-50%)
    bottom: 0
    transition: .3s ease
</style>
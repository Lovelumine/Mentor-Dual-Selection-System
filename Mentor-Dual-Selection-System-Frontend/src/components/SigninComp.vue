<script setup lang="ts">
import {Avatar, Menu} from "@element-plus/icons-vue";
import {ref} from "vue";

const signinForm = ref({
  account: '',
  password: '',
  role: 'teacher'
});

function singinClicked(){
  console.log(signinForm.value);
}

</script>

<template>
  <div class="container">
    <h3>
      账号登录
    </h3>
    <form @submit.prevent="singinClicked">
      <div class="input_box">
        <el-icon size="25" color="#005826"><Avatar /></el-icon>
        <input placeholder="工号 / 学号" type="text" required v-model="signinForm.account" />
      </div>
      <div class="input_box">
        <el-icon size="25" color="#005826"><Menu /></el-icon>
        <input placeholder="密码" type="password" required v-model="signinForm.password" />
      </div>
      <button type="submit" class="login_button">登录</button>
    </form>
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
  .login_button
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
  .login_button:hover
    background-color: #0f7e3f
</style>
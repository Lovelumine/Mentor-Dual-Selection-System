<script setup lang="ts">

import {Lock, Message, User} from "@element-plus/icons-vue";
import {ref} from "vue";
import {http} from "@/utils/http";

const registerForm = ref({
  username: '',
  password: '',
  email: ''
});

function signupClicked(){
  http({
    url: '/auth/register',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': '*/*',
    },
    data: registerForm.value
  }).then(res => {
    console.log(res.status);
    if (res.status === 200){

    } else {
      alert('注册失败');
    }
  }).catch((err) => {
    console.error(err);
    alert('注册失败');
  })
}
</script>

<template>
  <div class="container">
    <h3>
      账号注册
    </h3>
    <form @submit.prevent="signupClicked">
      <div class="input_box">
        <el-icon size="25" color="var(--app-brand)"><User /></el-icon>
        <input placeholder="工号 / 学号" type="text" required v-model="registerForm.username" />
      </div>
      <div class="input_box">
        <el-icon size="25" color="var(--app-brand)"><Message /></el-icon>
        <input placeholder="电子邮箱" type="text" required v-model="registerForm.email" />
      </div>
      <div class="input_box">
        <el-icon size="25" color="var(--app-brand)"><Lock /></el-icon>
        <input placeholder="密码" type="password" required v-model="registerForm.password" />
      </div>
      <button type="submit" class="register_button">注册</button>
    </form>
  </div>
</template>

<style scoped lang="sass">
.container
  width: 500px
  height: 300px
  position: relative
  margin: 30px auto 0 0
  box-shadow: 0 10px 24px var(--app-shadow)
  padding: 20px
  border-radius: 10px
  background-color: var(--app-surface)
  color: var(--app-text-primary)
  h3
    color: var(--app-brand)
    text-align: center
    font-weight: bold
  .input_box
    width: 100%
    margin: 30px 0 30px 0
    display: flex
    align-items: center
    border-bottom: 1px solid var(--app-brand)
    input
      margin-left: 20px
      width: 80%
      border: none
      color: var(--app-text-primary)
      background: transparent
    input:focus
      outline: none
      border: none
    input::placeholder
      font-size: 15px
      color: var(--app-text-secondary)
  .register_button
    border-radius: 5px
    border: none
    background-color: var(--app-brand)
    color: var(--app-text-inverse)
    width: 50%
    height: 35px
    font-size: 16px
    font-weight: bold
    letter-spacing: 5px
    margin: 0 auto
    position: absolute
    left: 50%
    transform: translate(-50%)
    transition: .3s ease
  .register_button:hover
    background-color: var(--app-brand-hover)
</style>

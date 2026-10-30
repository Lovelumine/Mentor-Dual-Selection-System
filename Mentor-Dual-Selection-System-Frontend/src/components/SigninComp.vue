<script setup lang="ts">
import {Avatar, Menu} from "@element-plus/icons-vue";
import {ref} from "vue";
import {http} from "@/utils/http";
import {useRouter} from "vue-router";
import {ElMessage} from "element-plus";
import {useUserInfoStore} from "@/stores/user/UserBasicInformation";
const router = useRouter();
const userInfoStore = useUserInfoStore();

const signinForm = ref({
  username: '',
  password: ''
});

const forgetPwdClicked = () => {
  router.push('/forgetpwd');
}

function singinClicked(){
  http({
    url: '/auth/login',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': '*/*',
    },
    data: signinForm.value
  }).then(res => {
    if (res.status === 200) {
      localStorage.setItem('token', res.data.data.token);
      userInfoStore.fetchUserInfo().then((success) => {
        if (success) {
          ElMessage.success('登录成功');
          router.replace('/notice');
          return;
        }

        ElMessage.error('登录状态初始化失败，请重新登录');
      });
    }
  }).catch((err) => {
    ElMessage.error(err?.response?.data?.data?.error || '登录失败，请检查账号和密码');
  })
}

</script>

<template>
  <div class="container">
    <h3>
      账号登录
    </h3>
    <form @submit.prevent="singinClicked">
      <div class="input_box">
        <el-icon size="25" color="var(--app-brand)"><Avatar /></el-icon>
        <input placeholder="工号 / 学号" type="text" required v-model="signinForm.username" />
      </div>
      <div class="input_box">
        <el-icon size="25" color="var(--app-brand)"><Menu /></el-icon>
        <input placeholder="密码" type="password" required v-model="signinForm.password" />
      </div>
      <div class="action_box">
        <button type="submit" class="register_button">登录</button>
        <button type="button" class="forget_button" @click="forgetPwdClicked">忘记密码</button>
      </div>
    </form>
  </div>
</template>

<style scoped lang="sass">
.container
  width: 400px
  min-height: 340px
  position: relative
  margin: 15vh auto 0 auto
  color: var(--app-text-primary)
  h3
    color: var(--app-brand)
    text-align: center
    font-weight: bold
    margin: 0
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
  .action_box
    margin-top: 16px
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
    display: block
    transition: .3s ease
  .register_button:hover
    background-color: var(--app-brand-hover)
  .forget_button
    border-radius: 5px
    border: 1px solid var(--app-border-strong)
    background-color: var(--app-surface-muted)
    color: var(--app-text-primary)
    width: 50%
    height: 35px
    font-size: 16px
    font-weight: bold
    letter-spacing: 5px
    margin: 14px auto 0 auto
    display: block
    transition: .3s ease
  .forget_button:hover
    border-color: var(--app-brand)
    background-color: var(--app-brand-soft)
    color: var(--app-brand)

@media (max-width: 767px)
  .container
    width: min(100%, calc(100vw - 24px))
    margin: 8vh auto 0 auto
    padding: 24px 18px 20px
    box-sizing: border-box
    border: 1px solid var(--app-border)
    border-radius: 16px
    background: linear-gradient(180deg, var(--app-surface) 0%, var(--app-surface-elevated) 100%)
    box-shadow: 0 20px 40px var(--app-shadow)
    .input_box
      margin: 24px 0
      input
        width: calc(100% - 45px)
    .action_box
      display: flex
      flex-direction: column
      gap: 14px
    .register_button,
    .forget_button
      width: 100%
      min-height: 44px
      height: auto
      margin: 0
      font-size: 15px
      letter-spacing: 2px
</style>

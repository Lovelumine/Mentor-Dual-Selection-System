import { createRouter, createWebHistory } from 'vue-router'
import Home from "@/router/RouterLists/Home";
import Personal from "@/router/RouterLists/Personal";
import ForgetPwd from "@/router/RouterLists/ForgetPwd";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    Home,
    Personal,
    ForgetPwd,
  ]
})

export default router

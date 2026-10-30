<script setup lang="ts">
import { ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import { getSingleQueryValue } from "@/utils/routeQuery";

const route = useRoute();
const router = useRouter();
const selectTeacherValue = ref("");

function syncSearchInputFromRoute() {
  selectTeacherValue.value = getSingleQueryValue(route.query.name);
}

async function selectTeacherClicked() {
  const nextTeacherName = selectTeacherValue.value.trim();
  if (!nextTeacherName) {
    ElMessage.warning("请输入正确的内容");
    return;
  }

  if (getSingleQueryValue(route.query.name) === nextTeacherName) {
    return;
  }

  await router.replace({
    path: route.path,
    query: {
      ...route.query,
      name: nextTeacherName,
    },
  });
}

async function resettingClicked() {
  if (!selectTeacherValue.value && !getSingleQueryValue(route.query.name)) {
    return;
  }

  selectTeacherValue.value = "";
  const nextQuery = { ...route.query };
  delete nextQuery.name;

  await router.replace({
    path: route.path,
    query: nextQuery,
  });
}

watch(
  () => route.query.name,
  () => {
    syncSearchInputFromRoute();
  },
  { immediate: true },
);
</script>

<template>
  <div class="search_box">
    <label class="search_label" for="teacher-search-input">导师姓名模糊搜索</label>
    <input id="teacher-search-input" type="text" placeholder="请输入" v-model="selectTeacherValue">
    <div class="search_actions">
      <button class="button" @click="selectTeacherClicked">查询</button>
      <button class="button" @click="resettingClicked">重置</button>
    </div>
  </div>
</template>

<style scoped lang="sass">
.search_box
  display: flex
  align-items: center
  flex-wrap: wrap
  gap: 10px
  font-size: 16px
  font-weight: bold
  margin: 0 auto 0 auto
  min-width: 0
  .search_label
    white-space: nowrap
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
  .search_actions
    display: flex
    flex-wrap: wrap
    gap: 10px

@media (max-width: 767px)
  .search_box
    width: 100%
    .search_label
      width: 100%
    input
      width: 100%
    .search_actions
      width: 100%
    .button
      width: calc(50% - 5px)
      margin-left: 0
</style>

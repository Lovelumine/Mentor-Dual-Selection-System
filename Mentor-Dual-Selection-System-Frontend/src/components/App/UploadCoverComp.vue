<script setup lang="ts">
import { Loading } from "@element-plus/icons-vue";
import { useUploadCoverStore } from "@/stores/UploadCoverStore";
import { onMounted, ref, watch } from "vue";
const uploadCoverStore = useUploadCoverStore();

<<<<<<< HEAD
// 确定类型，避免null和undefined导致的类型问题

const allStatus = ref({
  numberOfPeopleComp: null as number | null,
  nowNumberOfPeopleComp: null as number | null,
  nowPeopleNameComp: null as string | null,
  updateFalStatusComp: null as number | null
=======
interface AllStatusImpl {
  numberOfPeopleComp: number;
  nowNumberOfPeopleComp: number;
  nowPeopleNameComp: string | null;
  updateFalStatusComp: number;
}
const numberOfPeopleComp = ref();
const nowNumberOfPeopleComp = ref();
const nowPeopleNameComp = ref();
const updateFalStatusComp = ref(0);
const allStatus = ref<AllStatusImpl>({
  numberOfPeopleComp: -1,
  nowNumberOfPeopleComp: -1,
  nowPeopleNameComp: '',
  updateFalStatusComp: 0
>>>>>>> fe643114aca173d2ceaf54b24572a62f9fcc100f
})

function finishClicked() {
  uploadCoverStore.triggerCoverShow(false);
<<<<<<< HEAD
  uploadCoverStore.statusStore.numberOfPeople = -1;
  uploadCoverStore.statusStore.nowNumberOfPeople = -2;
  uploadCoverStore.statusStore.nowPeopleName = ""; // 将undefined替换为空字符串
  uploadCoverStore.statusStore.updateFalStatus = 0;
=======
  uploadCoverStore.recordStatus(-1, -1, '', 0);
  // uploadCoverStore.statusStore.numberOfPeople = -1;
  // uploadCoverStore.statusStore.nowNumberOfPeople = -2;
  // uploadCoverStore.statusStore.nowPeopleName = null;
  // uploadCoverStore.statusStore.updateFalStatus = 0;
>>>>>>> fe643114aca173d2ceaf54b24572a62f9fcc100f
}

onMounted(() => {
  allStatus.value.numberOfPeopleComp = uploadCoverStore.statusStore.numberOfPeople;
  allStatus.value.nowNumberOfPeopleComp = uploadCoverStore.statusStore.nowNumberOfPeople;
  allStatus.value.nowPeopleNameComp = uploadCoverStore.statusStore.nowPeopleName || ""; // 处理空值
  allStatus.value.updateFalStatusComp = uploadCoverStore.statusStore.updateFalStatus;
})

watch(() => uploadCoverStore.statusStore, (newVal) => {
  allStatus.value.numberOfPeopleComp = newVal.numberOfPeople;
  allStatus.value.nowNumberOfPeopleComp = newVal.nowNumberOfPeople;
  allStatus.value.nowPeopleNameComp = newVal.nowPeopleName || ""; // 处理空值
  allStatus.value.updateFalStatusComp = newVal.updateFalStatus;
})
</script>

<template>
  <div class="cover_box" v-if="true">
    <img class="logo" src="@/assets/zhuzhanLOGOgreen.png" alt="logo"/>
    <div class="icon_text_box">
      <el-icon size="128" color="#005826"><Loading /></el-icon>
      <div>
<<<<<<< HEAD
        <span>{{ allStatus.numberOfPeopleComp !== null && allStatus.numberOfPeopleComp >= 0 ? `解析到您的Excel存有 ${allStatus.numberOfPeopleComp} 条信息` : '正在解析……' }}</span><br/>
        <span>{{ allStatus.nowNumberOfPeopleComp !== null && allStatus.nowNumberOfPeopleComp >= 0 ? `当前 ${allStatus.nowNumberOfPeopleComp} / 共有 ${allStatus.numberOfPeopleComp}` : '正在解析……' }}</span><br/>
        <span>{{ allStatus.nowPeopleNameComp !== '' && allStatus.nowPeopleNameComp !== null ? `姓名：${allStatus.nowPeopleNameComp}` : '正在解析……'}}</span><br/>
=======
        <span>{{ '期间禁止关闭网页，请保持网络畅通' }}</span><br/>
        <span>{{ allStatus?.numberOfPeopleComp >= 0? `解析到您的Excel存有 ${allStatus?.numberOfPeopleComp} 条信息`: '正在解析……' }}</span><br/>
        <span>{{ allStatus?.nowNumberOfPeopleComp >= 0? `当前 ${allStatus?.nowNumberOfPeopleComp} / 共有 ${allStatus?.numberOfPeopleComp}`: '正在解析……' }}</span><br/>
        <span>{{ allStatus?.nowPeopleNameComp !== '' && allStatus?.nowPeopleNameComp !== undefined? `姓名：${allStatus?.nowPeopleNameComp}`: '正在解析……'}}</span><br/>
>>>>>>> fe643114aca173d2ceaf54b24572a62f9fcc100f
      </div>
      <button :disabled="allStatus?.numberOfPeopleComp !== allStatus?.nowNumberOfPeopleComp" @click="finishClicked" class="button">完成</button>
    </div>
  </div>
</template>

<style scoped lang="sass">
.cover_box
  opacity: .9
  width: 100vw
  height: 100vh
  z-index: 99
  background-color: #e5efe8
  position: fixed
  text-align: center
  .logo
    position: absolute
    opacity: .1
    width: 700px
    bottom: -8%
    right: -1%
  .icon_text_box
    width: 50%
    margin: 200px auto
    button
      padding: 0 20px
      height: 32px
      width: 160px
      font-size: 14px
      border-radius: 5px
      border: none
      color: white
      background-color: #005826
      transition: .3s ease
    button:hover
      background-color: #0f7e3f
    button:disabled
      background-color: #55655b
  span:first-child
    font-weight: bold
    font-size: 24px
  span
    font-size: 18px
</style>

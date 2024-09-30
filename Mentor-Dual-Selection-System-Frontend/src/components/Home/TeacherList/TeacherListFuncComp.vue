<script setup lang="ts">
import {onMounted, ref} from "vue";
import * as XLSX from 'xlsx';
import {useUploadCoverStore} from "@/stores/UploadCoverStore";
import {http} from "@/utils/http";
const uploadCoverStore = useUploadCoverStore();
import {useRouter} from "vue-router";
const router = useRouter();

const fileInput = ref(null);
const excelData = ref([]);
const excelName = ref('');
const isUploadBoxShow = ref(false);

function addSingleTeacherClicked() {
  router.push('/add_single_teach');
}

function downloadTeacherTemplate () {
  window.location.href = 'https://minio.lumoxuan.cn/mentor-dual-selection-system/teacher0/导师表格模板.xlsx';
}

function checkUpload() {
  uploadCoverStore.triggerCoverShow(true);
  uploadCoverStore.recordNumberOfPeople(excelData.value.length - 1);
  let falCount = 0;
  for (let i = 1; i < excelData.value.length; i++) {
    uploadCoverStore.recordNowNumberOfPeople(i);
    uploadCoverStore.recordNowPeopleName(excelData.value[i][1]);
    http({
      url: '/user/update',
      method: 'POST',
      headers: {
        Accept: '*/*',
        Authorization: `Bearer ${localStorage.getItem("token")}`,
        'Content-Type': 'application/json',
      },
      data: {
        role: excelData.value[i][0],
        fullName: excelData.value[i][1],
        email: excelData.value[i][2],
        username: excelData.value[i][3]
      }
    }).then(res => {
      if (res.data.code !== 200){
        uploadCoverStore.recordUpdateFalStatus(++falCount);
      }
    }).catch((err) => {
      alert(`${JSON.parse(err.request.response).data.error}\n第${i}条数据：${excelData.value[i][3]}-${excelData.value[i][1]}\n${excelData.value[i][0]}`);
      uploadCoverStore.recordUpdateFalStatus(++falCount);
    })
  }
}

const triggerFileInput = () => {
  fileInput.value.click();
}

// 处理文件选择后的变化
function handleFileChange(event) {
  const files = event.target.files;
  console.log('files', files);
  if (files.length === 0) return;

  const file = files[0];
  console.log('file', file);
  excelName.value = file.name;
  if (!['xlsx', 'xls'].includes(file.name.toString().split('.')[file.name.toString().split('.').length - 1])){
    alert('你选择的不是Excel表格文档！');
    return;
  }
  const reader = new FileReader();
  reader.onload = (e) => {
    const arrayBuffer = e.target.result;
    const workbook = XLSX.read(arrayBuffer, { type: 'array' }); // 注意这里的 type 是 'array'

    // 处理第一个工作表
    const firstSheetName = workbook.SheetNames[0];
    console.log('firstSheetName', firstSheetName);
    const worksheet = workbook.Sheets[firstSheetName];

    // 将工作表的数据转换成JSON，注意header行需要与实际Excel文件的标题行相匹配
    // 更新excelData ref
    excelData.value = XLSX.utils.sheet_to_json(worksheet, {header: 1});
    console.log('excelData.value', excelData.value);
    if (excelData.value.length > 0) isUploadBoxShow.value = true;
  };
  reader.readAsArrayBuffer(file); // 使用 readAsArrayBuffer 而不是 readAsBinaryString
}
</script>

<template>
  <div class="button_box">
    <button @click="addSingleTeacherClicked">添加单条导师信息</button>
    <button>下载当前导师状态</button>
    <button @click="downloadTeacherTemplate">下载导师表格模板</button>
    <input type="file" @change="handleFileChange" ref="fileInput" style="display: none;" accept=".xlsx, .xls">
    <button @click="triggerFileInput">批量上传导师信息</button>
  </div>
  <div v-if="isUploadBoxShow" class="excel_status">
    <p>{{excelData.length > 0? `文件已准备就绪：${excelName}`: ''}}</p>
    <button @click="checkUpload">确认上传</button>
    <button @click="triggerFileInput">重新上传</button>
  </div>
</template>

<style scoped lang="sass">
.button_box
  width: 100%
  height: 80px
  display: flex
  align-items: center
  button
    padding: 0 20px
    height: 32px
    width: 160px
    font-size: 14px
    border-radius: 5px
    border: none
    color: white
    margin-left: 32px
    background-color: #005826
    transition: .3s ease
  button:hover
    background-color: #0f7e3f
.excel_status
  background-color: #005826
  padding-left: 20px
  width: 100%
  height: 60px
  display: flex
  align-items: center
  p
    color: white
  button
    padding: 0 20px
    height: 32px
    width: 120px
    font-size: 14px
    border-radius: 5px
    border: none
    color: #005826
    margin-left: 32px
    background-color: white
    transition: .3s ease
  button:hover
    background-color: #0f7e3f
    color: white
  button:last-child
    background-color: white
    border: 1px solid #bd0000
    color: #bd0000
  button:last-child:hover
    background-color: #bd0000
    color: white
</style>
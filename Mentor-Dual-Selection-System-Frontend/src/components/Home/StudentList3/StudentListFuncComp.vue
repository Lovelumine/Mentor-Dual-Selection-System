<script setup lang="ts">
import {onMounted, ref} from "vue";
import * as XLSX from 'xlsx';
import {useUploadCoverStore} from "@/stores/UploadCoverStore";
import {http} from "@/utils/http";
const uploadCoverStore = useUploadCoverStore();
import {useRouter} from "vue-router";
const router = useRouter();

const fileInput = ref<HTMLInputElement | null>(null);
const excelData = ref<any[]>([]);
const excelName = ref('');
const isUploadBoxShow = ref(false);

function addSingleStuClicked () {
  router.push('/add_single_stu');
}

function downloadStudentTemplate () {
  window.location.href = 'https://minio.lumoxuan.cn/mentor-dual-selection-system/teacher0/学生表格模板.xlsx';
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
        username: excelData.value[i][3],
        grade: excelData.value[i][4]
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
  if (fileInput.value) fileInput.value.click();
}

// 处理文件选择后的变化
function handleFileChange(event: Event) {
  const inputElement = event.target as HTMLInputElement | null;
  let file: any;
  if (inputElement && inputElement.files) {
    file = inputElement.files[0];
  }
  if (!file) return;
  console.log('file', file);
  excelName.value = file.name;
  if (!['xlsx', 'xls'].includes(file.name.toString().split('.').pop())){
    alert('您选择的不是Excel表格文档！');
    return;
  }
  const reader = new FileReader();
  reader.onload = (e: Event) => {
    const loadEvent = e as ProgressEvent<FileReader>;
    const arrayBuffer = loadEvent.target?.result as ArrayBuffer;
    const workbook = XLSX.read(arrayBuffer, { type: 'array' }); // 注意这里的 type 是 'array'
    if (!arrayBuffer) return;

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
    <button @click="addSingleStuClicked">添加单条学生信息</button>
<!--    <button>下载当前学生状态</button>-->
    <button @click="downloadStudentTemplate">下载学生表格模板</button>
    <input type="file" @change="handleFileChange" ref="fileInput" style="display: none;" accept=".xlsx, .xls">
    <button @click="triggerFileInput">批量上传学生信息</button>
<!--    <button>批量下载学生附件</button>-->
<!--    <button>下载列表</button>-->
  </div>
  <div v-if="isUploadBoxShow" class="excel_status">
    <p>{{excelData.length > 0? `文件已准备就绪：${excelName}`: ''}}</p>
    <button @click="checkUpload">确认上传</button>
    <button @click="triggerFileInput">重新上传</button>
  </div>
</template>

<style scoped lang="sass">
.button_box
  background-color: #e2e2e2
  width: 100%
  height: 60px
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
  background-color: white
  padding-left: 20px
  width: 100%
  height: 60px
  display: flex
  align-items: center
  button
    padding: 0 20px
    height: 32px
    width: 120px
    font-size: 14px
    border-radius: 5px
    border: none
    color: white
    margin-left: 32px
    background-color: #005826
    transition: .3s ease
  button:hover
    background-color: #0f7e3f
  button:last-child
    background-color: white
    border: 1px solid #bd0000
    color: #bd0000
  button:last-child:hover
    background-color: #bd0000
    color: white
</style>
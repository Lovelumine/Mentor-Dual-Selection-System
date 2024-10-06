<script setup lang="ts">
import { ref } from "vue";
import * as XLSX from 'xlsx';
import { http } from "@/utils/http";
import { useRouter } from "vue-router";
import axios from "axios";
import { ElProgress } from 'element-plus';
const router = useRouter();

const fileInput = ref(null);
const excelData = ref<any[]>([]);
const excelName = ref('');
const isUploadBoxShow = ref(false);
const progressPercentage = ref(0); // 进度百分比
const progressStatus = ref(''); // 当前正在处理的信息
const total = ref(0); // 总条数
const falCount = ref(0); // 失败的条数

function addSingleTeacherClicked() {
  router.push('/add_single_teach');
}

function downloadTeacherTemplate() {
  window.location.href = 'https://minio.lumoxuan.cn/mentor-dual-selection-system/teacher0/导师表格模板.xlsx';
}

// 批量上传基本信息
async function checkUpload() {
  if (excelData.value.length <= 1) {
    alert('没有可上传的数据');
    return;
  }
  falCount.value = 0;
  total.value = excelData.value.length - 1; // 总条数

  for (let i = 1; i < excelData.value.length; i++) {
    try {
      const currentName = excelData.value[i][1];
      progressStatus.value = `正在处理第 ${i} 条数据：${currentName}`;

      // 上传基本信息
      await uploadBasicInfo(excelData.value[i], i);

      // 获取教师 UID 并上传职位和研究方向
      await updateTeacherDetails(excelData.value[i], i);

    } catch (err) {
      console.error(`第 ${i} 条数据上传失败`, err);
      falCount.value++;
    } finally {
      // 更新进度
      progressPercentage.value = Math.round((i / total.value) * 100);
    }
  }

  progressStatus.value = `上传完成，成功 ${total.value - falCount.value} 条，失败 ${falCount.value} 条`;

  // 上传完成后刷新页面
  setTimeout(() => {
    window.location.reload();
  }, 2000); // 延迟2秒刷新，便于用户查看上传结果
}

async function uploadBasicInfo(rowData, index) {
  return http({
    url: '/user/update',
    method: 'POST',
    headers: {
      Accept: '*/*',
      Authorization: `Bearer ${localStorage.getItem("token")}`,
      'Content-Type': 'application/json',
    },
    data: {
      role: String(rowData[0]).trim(),        // '权限（导师请大写TEACHER）' 去掉多余空格
      fullName: String(rowData[1]).trim(),    // '姓名' 去掉多余空格
      email: String(rowData[2]).trim(),       // '电子邮箱' 去掉多余空格
      username: String(rowData[3]).trim(),    // '工号' 去掉多余空格
    }
  }).then(res => {
    if (res.data.code !== 200) {
      throw new Error(`第 ${index} 条数据上传失败`);
    }
  }).catch(err => {
    throw err;
  });
}

async function updateTeacherDetails(rowData, index) {
  try {
    // 获取教师列表
    const res = await http({
      url: '/search/teachers',
      method: 'GET',
      headers: {
        Accept: '*/*',
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
    });

    if (res.data.code === 200) {
      const allTeachers = res.data.data;
      const fullName = String(rowData[1]).trim();
      const username = String(rowData[3]).trim();

      const teacher = allTeachers.find(t => t.username === username && t.fullName === fullName);

      if (teacher && teacher.uid) {
        // 上传职位、研究方向
        await uploadAdditionalInfo(teacher.uid, rowData, index);
      } else {
        console.error(`未找到第 ${index} 条教师的 UID, 工号: ${username}, 姓名: ${fullName}`);
        throw new Error(`未找到第 ${index} 条教师的 UID`);
      }
    } else {
      throw new Error('教师信息获取失败');
    }
  } catch (err) {
    throw new Error(`获取教师 UID 失败: ${err}`);
  }
}

async function uploadAdditionalInfo(uid, rowData, index) {
  try {
    const res = await axios({
      url: `/admin/update/${uid}`,
      method: 'PUT',
      headers: {
        Accept: '*/*',
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
      data: {
        teacherPosition: rowData[4] ? String(rowData[4]).trim() : '',   // '职位'
        researchDirection: rowData[5] ? String(rowData[5]).trim() : '', // '研究方向'
        resume: '', // 简历可以留空
        studentGrade: '' // 必传的空年级信息
      }
    });

    if (res.data.code !== 200) {
      throw new Error(`第 ${index} 条详细信息上传失败`);
    }
  } catch (err) {
    console.error(`第 ${index} 条数据上传详细信息异常`, err);
    throw err;
  }
}

const triggerFileInput = () => {
  fileInput.value.click();
}

// 处理文件选择后的变化
function handleFileChange(event) {
  const files = event.target.files;
  if (files.length === 0) return;

  const file = files[0];
  excelName.value = file.name;
  if (!['xlsx', 'xls'].includes(file.name.toString().split('.').pop())) {
    alert('你选择的不是Excel表格文档！');
    return;
  }
  const reader = new FileReader();
  reader.onload = (e) => {
    const arrayBuffer = e.target.result;
    const workbook = XLSX.read(arrayBuffer, { type: 'array' });
    const firstSheetName = workbook.SheetNames[0];
    const worksheet = workbook.Sheets[firstSheetName];

    excelData.value = XLSX.utils.sheet_to_json(worksheet, { header: 1 });
    if (excelData.value.length > 0) isUploadBoxShow.value = true;
  };
  reader.readAsArrayBuffer(file);
}
</script>

<template>
  <div class="button_box">
    <button @click="addSingleTeacherClicked">添加单条导师信息</button>
    <button @click="downloadTeacherTemplate">下载导师表格模板</button>
    <input type="file" @change="handleFileChange" ref="fileInput" style="display: none;" accept=".xlsx, .xls">
    <button @click="triggerFileInput">批量上传导师信息</button>
  </div>

  <div v-if="isUploadBoxShow" class="excel_status">
    <p>{{excelData.length > 0 ? `文件已准备就绪：${excelName}` : ''}}</p>
    <button @click="checkUpload">确认上传</button>
    <button @click="triggerFileInput">重新上传</button>
  </div>

  <!-- 进度条 -->
  <div v-if="progressPercentage > 0" class="progress-container">
    <el-progress
      :percentage="progressPercentage"
      :text-inside="true"
      stroke-width="30"
      color="#7ED321"
    ></el-progress>
    <p>{{ progressStatus }}</p>
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

.progress-container
  margin-top: 20px
  padding: 0 20px
  p
    margin-top: 10px
</style>

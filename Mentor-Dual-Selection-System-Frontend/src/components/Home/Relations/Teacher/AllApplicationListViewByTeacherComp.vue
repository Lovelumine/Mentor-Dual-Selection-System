<script setup lang="ts">
import { onMounted, ref } from "vue";
import { http } from "@/utils/http";
import { useRouter } from "vue-router";
import * as echarts from 'echarts';
import { useUserInfoStore } from "@/stores/user/UserBasicInformation";
import type { EChartsOption } from 'echarts';

const router = useRouter();
const userStore = useUserInfoStore();
const totalSlots = 3;  // 总的可以招收的学生数
const studentCount = ref(0);  // 已招收的学生数

// 所有的申请数据
const acceptedCount = ref(0);
const rejectedCount = ref(0);
const pendingCount = ref(0);

interface Application {
  id: number;
  studentName: string;
  status: 'ACCEPTED' | 'REJECTED' | 'PENDING';
  // 其他字段可以根据你的数据结构定义
}

// 定义 Params 接口，包含 dataIndex 属性
interface Params {
  dataIndex: number;
  // 根据需要添加其他属性
}

const pendingApplications = ref<Application[]>([]);

const updateChartData = () => {
  acceptedCount.value = pendingApplications.value.filter(app => app.status === 'ACCEPTED').length;
  rejectedCount.value = pendingApplications.value.filter(app => app.status === 'REJECTED').length;
  pendingCount.value = pendingApplications.value.filter(app => app.status === 'PENDING').length;
}

const drawProgressPieChart = () => {
  const chartDom = document.getElementById('progressPieChart');
  if (chartDom) {
    const myChart = echarts.init(chartDom);
    const filledSlots = studentCount.value;
    const emptySlots = totalSlots - studentCount.value;
    const option: EChartsOption = {
      title: {
        text: '导师招生进度',
        left: 'center',
        top: '5%',
        textStyle: {
          fontSize: 18,
          fontWeight: 'bold'
        }
      },
      tooltip: {
        trigger: 'item'
      },
      series: [
        {
          name: '招生进度',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          label: {
            show: false // 不显示数字
          },
          emphasis: {
            label: {
              show: true,
              fontSize: '24',
              fontWeight: 'bold'
            }
          },
          labelLine: {
            show: false
          },
          data: [
            { value: filledSlots, name: '已招收', itemStyle: { color: '#67C23A' } },
            { value: emptySlots, name: '剩余名额', itemStyle: { color: '#E5E5E5' } }
          ]
        }
      ]
    };
    myChart.setOption(option);

    // 自适应图表大小
    window.addEventListener('resize', () => {
      myChart.resize();
    });
  }
};

const drawBarChart = () => {
  const chartDom = document.getElementById('barChart');
  if (chartDom) {
    const myChart = echarts.init(chartDom);
    const option: EChartsOption = {
      title: {
        text: '申请处理情况',
        left: 'center',
        top: '5%',
        textStyle: {
          fontSize: 18,
          fontWeight: 'bold'
        }
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {           
          type: 'shadow'       
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '5%',
        top: '20%',
        containLabel: true
      },
      xAxis: [
        {
          type: 'category',
          data: ['同意', '拒绝', '待处理'],
          axisTick: {
            alignWithLabel: true
          },
          axisLabel: {
            fontSize: 14
          }
        }
      ],
      yAxis: [
        {
          type: 'value',
          axisLabel: {
            fontSize: 14
          }
        }
      ],
      series: [
        {
          name: '申请数量',
          type: 'bar',
          barWidth: '60%',
          data: [acceptedCount.value, rejectedCount.value, pendingCount.value],
          itemStyle: {
            color: function(params: Params) {
              const colorList = ['#67C23A', '#F56C6C', '#E6A23C'];
              return colorList[params.dataIndex];
            }
          }
        }
      ]
    };
    myChart.setOption(option);

    // 自适应图表大小
    window.addEventListener('resize', () => {
      myChart.resize();
    });
  }
};

onMounted(() => {
  userStore.fetchUserInfo();
  if (!localStorage.getItem("token")) {
    router.push("/");
  } else {
    http({
      url: "/application/mentor-student-relations",
      method: "GET",
      headers: {
        Authorization: "Bearer " + localStorage.getItem("token"),
        Accept: "*/*",
      },
    }).then((res) => {
      if (res.data.code === 200) {
        studentCount.value = res.data.data[0].students.length;  // 更新已招收的学生数
        drawProgressPieChart(); // 绘制饼状图
      } else {
        alert("获取学生列表失败！");
      }
    }).catch(err => {
      console.error(err);
      alert("请求学生信息出错！");
    });

    http({
      url: '/application/pending',
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem("token")}`,
      }
    }).then((res) => {
      if (res.data.code === 200) {
        pendingApplications.value = res.data.data;
        updateChartData();
        drawBarChart();
      } else {
        alert("获取申请信息失败！");
      }
    }).catch(err => {
      console.error(err);
      alert("请求申请信息出错！");
    });
  }
});

</script>

<template>
  <div class="application_overview_box">
    <h3>申请概况</h3>
    <div class="content_box">
      <div class="progress_box box">
        <div id="progressPieChart" class="chart"></div>
        <p>已招收学生数：{{ studentCount }} / {{ totalSlots }}</p>
      </div>

      <div class="chart_box box">
        <div id="barChart" class="chart"></div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.application_overview_box {
  padding: 20px;

  h3 {
    text-align: center;
    margin-bottom: 30px;
    font-size: 24px;
  }

  .content_box {
    display: flex;
    justify-content: space-between;
    align-items: stretch; /* 使子元素等高 */

    @media (max-width: 768px) {
      flex-direction: column;
      align-items: center;
    }

    .box {
      width: 48%;
      background-color: #f9f9f9;
      padding: 20px;
      border-radius: 10px;
      transition: transform 0.3s, box-shadow 0.3s;
      display: flex;
      flex-direction: column;
      justify-content: space-between; /* 调整内容位置 */
      align-items: center; /* 居中对齐 */

      &:hover {
        transform: translateY(-5px);
        box-shadow: 0 10px 15px rgba(0, 0, 0, 0.1);
      }

      @media (max-width: 768px) {
        width: 100%;
        margin-bottom: 20px;
      }

      .chart {
        width: 100%;
        height: 300px;
        flex-grow: 1; /* 使图表占满剩余空间 */
      }

      p {
        margin-top: 10px;
        font-size: 16px;
        font-weight: bold;
        text-align: center;
      }
    }
  }
}
</style>

import HomeView from "@/views/HomeView.vue";
import TeacherListComp from "@/components/Home/TeacherList/TeacherListComp.vue";

export default {
    path: '/',
    name: 'home',
    component: HomeView,
    children: [
        {
            path: 'teach_list',
            name: 'teachList',
            component: TeacherListComp
        },
        {
            path: 'notice',
            name: 'notice',
            component: () => import('@/components/Home/Notice/NoticeComp.vue')
        },
        {
            path: 'student_list1',
            name: 'studentList1',
            component: () => import('@/components/Home/StudentList1/StudentList1Comp.vue')
        },
        {
            path: 'student_list2',
            name: 'studentList2',
            component: () => import('@/components/Home/StudentList2/StudentList2Comp.vue')
        },
        {
            path: 'student_list3',
            name: 'studentList3',
            component: () => import('@/components/Home/StudentList3/StudentList3Comp.vue')
        },
        {
            path: 'student_list4',
            name: 'studentList4',
            component: () => import('@/components/Home/StudentList4/StudentList4Comp.vue')
        },
        {
            path: 'select_teacher',
            name: 'selectTeacher',
            component: () => import('@/components/Home/SelectTeacherComp.vue')
        },
        {
            path: 'select_student',
            name: 'selectStudent',
            component: () => import('@/components/Home/SelectStudent/SelectStudentComp.vue')
        },
        {
            path: 'relations',
            name: 'relations',
            component: () => import('@/components/Home/Relations/RelationsComp.vue')
        },
        {
            path: 'add_single_stu',
            name: 'addSingleStu',
            component: () => import('@/components/Home/AddSingleStuInfoComp.vue')
        }
    ]
}
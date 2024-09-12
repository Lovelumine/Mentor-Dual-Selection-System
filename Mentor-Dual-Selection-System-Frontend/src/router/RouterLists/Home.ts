import HomeView from "@/views/HomeView.vue";

export default {
    path: '/',
    name: 'home',
    component: HomeView,
    children: [
        {
            path: 'teacher_list',
            name: 'teacherList',
            component: () => import('@/components/Home/TeacherList/TeacherListComp.vue')
        },
        {
            path: 'notice',
            name: 'notice',
            component: () => import('@/components/Home/Notice/NoticeComp.vue')
        },
        {
            path: 'student_list',
            name: 'studentList',
            component: () => import('@/components/Home/StudentList/StudentListComp.vue')
        }
    ]
}
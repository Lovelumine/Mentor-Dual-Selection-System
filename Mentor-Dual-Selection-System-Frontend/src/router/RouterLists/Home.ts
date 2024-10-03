import HomeView from "@/views/HomeView.vue";
import TeacherListComp from "@/components/Home/TeacherList/TeacherListComp.vue";

export default {
    path: '/',
    name: 'home',
    component: HomeView,
    redirect: '/notice',
    children: [
        {
            path: 'teach_list',
            name: 'teachList',
            component: TeacherListComp
        },
        {
            path: 'notice',
            name: 'notice',
            component: () => import('@/components/Home/Notice/NoticeComp.vue'),
            children: [
                {
                    path: '',
                    component: () => import('@/components/Home/Notice/NoticeCardComp.vue'),
                },
                {
                    path: 'release',
                    name: 'noticeRelease',
                    component: () => import('@/components/Home/Notice/NoticeReleaseComp.vue'),
                }
            ]
        },
        {
            path: 'stu_list1',
            name: 'stuList1',
            component: () => import('@/components/Home/StudentList1/StudentList1Comp.vue')
        },
        {
            path: 'stu_list2',
            name: 'stuList2',
            component: () => import('@/components/Home/StudentList2/StudentList2Comp.vue')
        },
        {
            path: 'stu_list3',
            name: 'stuList3',
            component: () => import('@/components/Home/StudentList3/StudentList3Comp.vue')
        },
        {
            path: 'stu_list4',
            name: 'stuList4',
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
        },
        {
            path: 'add_single_teach',
            name: 'addSingleTeach',
            component: () => import('@/components/Home/AddSingleTeachInfoComp.vue')
        }
    ]
}
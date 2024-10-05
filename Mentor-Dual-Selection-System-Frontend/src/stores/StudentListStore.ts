import {defineStore} from "pinia";

export const useStudentListStore = defineStore('StudentListStore', {
    state: () => ({
        isSelectTeacher: null,
        studentListSt: []
    }),
    actions: {
        updateIsSelectTeacher(target: string) {
            this.isSelectTeacher = target;
        },
        updateStudentList(target) {
            this.studentListSt = target;
        }
    }
})
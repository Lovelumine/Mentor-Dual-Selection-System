import { defineStore } from "pinia";

export const useStudentListStore = defineStore('StudentListStore', {
    state: () => ({
        isSelectTeacher: null as string | null,
        studentListSt: [] as any[]
    }),
    actions: {
        updateIsSelectTeacher(target: string) {
            this.isSelectTeacher = target;
        },
        updateStudentList(target: any[]) {
            this.studentListSt = target;
        }
    }
})

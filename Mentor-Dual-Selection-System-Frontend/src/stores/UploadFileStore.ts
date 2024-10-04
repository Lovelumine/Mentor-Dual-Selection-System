import {defineStore} from "pinia";

export const useUploadFileStore = defineStore("UploadFileStore", {
    state: () => ({
        isLoading: false,
    }),
    actions: {
        changeIsLoading(target: boolean) {
            if (this.isLoading !== target) this.isLoading = target;
        }
    }
})
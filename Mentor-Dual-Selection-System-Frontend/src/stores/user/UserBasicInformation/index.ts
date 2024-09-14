import {defineStore} from "pinia";
import {http} from "@/utils/http";

export const useUserInfoStore = defineStore('userInfoStore', {
    state: () => ({
        userInfo: null
    }),
    actions: {
        async fetchUserInfo() {
            http({
                url: '/user/me',
                method: "GET",
                headers: {
                    "Accept": "*/*",
                    "Authorization": `Bearer ${localStorage.getItem('token')}`
                }
            }).then(res => {
                console.log(res);
                if (res.data.code === 200) {
                    this.userInfo = res.data.data;
                }
            }).catch(err => {
                console.error(err);
                localStorage.removeItem('token');
                window.location.reload();
            })
        }
    }
})
import axios from 'axios';
import router from '@/router';

let isHandlingUnauthorized = false;

const redirectToSignin = async () => {
    if (isHandlingUnauthorized) return;
    isHandlingUnauthorized = true;

    localStorage.removeItem('token');
    window.dispatchEvent(new CustomEvent('mentor-auth-expired'));

    try {
        if (router.currentRoute.value.path !== '/') {
            await router.replace('/');
        }
    } finally {
        isHandlingUnauthorized = false;
    }
};

const withAuthHandling = <T>(instance: T & { interceptors: { response: { use: Function } } }) => {
    instance.interceptors.response.use(
        (response: unknown) => response,
        async (error: any) => {
            if (error?.response?.status === 401) {
                await redirectToSignin();
            }

            return Promise.reject(error);
        }
    );

    return instance;
};

export const http = withAuthHandling(axios.create({
    baseURL: '/api',
    timeout: 30000,
    withCredentials: true
}))

export const httpAdmin = withAuthHandling(axios.create({
    baseURL: '/admin',
    timeout: 30000,
    withCredentials: true
}))

export const httpTeacher = withAuthHandling(axios.create({
    baseURL: '/teacher',
    timeout: 30000,
    withCredentials: true
}))

export const httpStudent = withAuthHandling(axios.create({
    baseURL: '/student',
    timeout: 30000,
    withCredentials: true
}))

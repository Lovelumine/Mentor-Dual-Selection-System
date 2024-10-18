import axios from 'axios';

export const http = axios.create({
    baseURL: '/api',
    timeout: 30000,
    withCredentials: true
})

export const httpAdmin = axios.create({
    baseURL: '/admin',
    timeout: 30000,
    withCredentials: true
})

export const httpTeacher = axios.create({
    baseURL: '/teacher',
    timeout: 30000,
    withCredentials: true
})

export const httpStudent = axios.create({
    baseURL: '/student',
    timeout: 30000,
    withCredentials: true
})
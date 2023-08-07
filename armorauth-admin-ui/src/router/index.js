import {createRouter, createWebHistory} from "vue-router";
import MainChildren from "../router/main_children";

const routerHistory = createWebHistory();

const router = createRouter({
    history: routerHistory,
    routes: [
        {
            path: '/',
            redirect: '/login'
        },
        {
            path: '/login',
            name: 'Login',
            component: () => import('../views/Login.vue'),
            meta: {
                title: '用户登录'
            }
        },
        {
            path: '/main',
            name: 'Main',
            component: () => import('../views/Main.vue'),
            children: MainChildren,
            redirect: '/main/home',
        },
        {
            path: '/:pathMatch(.*)*',
            name: 'NotFound',
            component: () => import('../views/NotFound.vue'),
        }
    ]
})

export default router;
export default [
    {
        path: '/main/home',
        name: 'Home',
        component: () => import('../views/main/Home.vue'),
        meta: {
            title: '用户首页'
        }
    },
    {
        path: '/main/applications',
        name: 'Applications',
        component: () => import('../views/main/Application.vue'),
        meta: {
            title: '应用管理'
        }
    },
    {
        path: '/main/monitor',
        name: 'Monitor',
        component: () => import('../views/main/Monitor.vue'),
        meta: {
            title: '监控管理'
        }
    },
    {
        path: '/main/thirdPartLogin',
        name: 'ThirdPartLogin',
        component: () => import('../views/main/ThirdPartLogin.vue'),
        meta: {
            title: '第三方登陆'
        }
    },
    {
        path: '/main/settings',
        name: 'Settings',
        component: () => import('../views/main/Settings.vue'),
        meta: {
            title: '设置'
        }
    }
]
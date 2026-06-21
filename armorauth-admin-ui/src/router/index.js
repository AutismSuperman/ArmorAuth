import {createRouter, createWebHistory} from "vue-router";
import MainChildren from "../router/main_children";
import {clearAdminAuth, getAdminUser, isAdminAuthenticated, validateAdminSession} from "../api";

const routerHistory = createWebHistory();
const SESSION_VALIDATION_TTL_MS = 5 * 60 * 1000;
let sessionValidationPromise = null;

const safeMainRedirect = (redirect) => redirect.startsWith('/main') ? redirect : '/main/home';

const needsSessionValidation = () => {
    const verifiedAt = Date.parse(getAdminUser().verifiedAt || '');
    return !Number.isFinite(verifiedAt) || Date.now() - verifiedAt > SESSION_VALIDATION_TTL_MS;
};

const ensureAdminSession = async () => {
    if (!needsSessionValidation()) {
        return;
    }
    if (!sessionValidationPromise) {
        sessionValidationPromise = validateAdminSession().finally(() => {
            sessionValidationPromise = null;
        });
    }
    await sessionValidationPromise;
};

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
                title: '用户登录',
                public: true
            }
        },
        {
            path: '/main',
            name: 'Main',
            component: () => import('../views/Main.vue'),
            children: MainChildren,
            redirect: '/main/home',
            meta: {
                requiresAuth: true
            }
        },
        {
            path: '/:pathMatch(.*)*',
            name: 'NotFound',
            component: () => import('../views/NotFound.vue'),
        }
    ]
})

router.beforeEach(async (to) => {
    if (to.meta?.title) {
        document.title = `${to.meta.title} - ArmorAuth`;
    }

    const loggedIn = isAdminAuthenticated();
    if (to.meta?.public) {
        if (to.path === '/login' && loggedIn) {
            const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : '/main/home';
            try {
                await ensureAdminSession();
                return safeMainRedirect(redirect);
            } catch {
                clearAdminAuth();
                return true;
            }
        }
        return true;
    }

    if (to.path.startsWith('/main')) {
        if (!loggedIn) {
            return {
                path: '/login',
                query: { redirect: to.fullPath }
            };
        }
        try {
            await ensureAdminSession();
        } catch {
            clearAdminAuth();
            return {
                path: '/login',
                query: { redirect: to.fullPath }
            };
        }
    }

    return true;
});

export default router;

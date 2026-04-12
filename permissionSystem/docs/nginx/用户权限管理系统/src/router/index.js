import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Layout from '../views/layout/Layout.vue'
import UserManagement from '../views/user/UserManagement.vue'
import RoleManagement from '../views/role/RoleManagement.vue'
import Chatbot from '../views/chat/Chatbot.vue'
import SystemLog from '../views/system/SystemLog.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/users',
    children: [
      {
        path: 'users',
        name: 'UserManagement',
        component: UserManagement,
        meta: { title: '用户管理' }
      },
      {
        path: 'roles',
        name: 'RoleManagement',
        component: RoleManagement,
        meta: { title: '角色管理' }
      },
      {
        path: 'chatbot',
        name: 'Chatbot',
        component: Chatbot,
        meta: { title: '智能客服' }
      },
      {
        path: 'system-log',
        name: 'SystemLog',
        component: SystemLog,
        meta: { title: '系统日志' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  if (to.path === '/login') {
    // 如果已经登录，重定向到首页
    if (token) {
      next('/')
    } else {
      next()
    }
  } else {
    // 需要登录才能访问的页面
    if (token) {
      next()
    } else {
      next('/login')
    }
  }
})

export default router

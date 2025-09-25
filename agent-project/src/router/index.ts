import { createRouter, createWebHistory } from 'vue-router'
import home from '@/views/home.vue'
import ChatWithAi from '@/views/ChatWithAi.vue'
import Friends from '@/views/Friends.vue'
import Chat from '@/views/Chat.vue'
import Nothing from '@/views/Nothing.vue'
import Session from '@/views/Session.vue'
import Contact from '@/views/Contact.vue'
import Nothing2 from '@/views/Nothing2.vue'
import VoiceToAi from '@/views/VoiceToAi.vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: home,
    },
    {
      path: '/ChatWithAi',
      name: 'ChatWithAi',
      component: ChatWithAi,
      meta: { requiresAuth: true }
    },
    {
      path: '/Friends',
      name: 'Friends',
      component: Friends,
      meta: { requiresAuth: true},
      children: [
        {
          path: '/Friends/Session',
          name: 'Session',
          component: Session,
          children: [
            {
              path: '/Friends/Session/Chat',
              name: "Chat",
              component: Chat
            },
            {
              path: '/Friends/Session/Nothing',
              name: "Nothing",
              component: Nothing
            }
          ]
        },
        {
          path:"/Friends/Contact",
          name:"Contact",
          component: Contact,
          children: [
            {
              path: '/Friends/Contact/Nothing',
              name: "Nothing2",
              component: Nothing2
            }
          ]
        }
      ]
    },
    {
      path: '/VoiceToAi',
      component: VoiceToAi,
      name: 'VoiceToAi',
      meta: { requiresAuth: true }
    }
  ],
})

// 添加全局前置守卫
router.beforeEach(async (to, from, next) => {
  // 检查路由是否需要验证UUID
  if (to.matched.some(record => record.meta.requiresAuth)) {
    // 发送请求到后端验证UUID
    await axios({
      url: "http://127.0.0.1:8080/ai/nothing",
      method: 'GET',
      headers: {
        "id": localStorage.getItem("id"),
        "uuid": localStorage.getItem("uuid")
      }
    }).then(res=>{
      if(res.data === 'Not logged in') next({name: "home"});
      else next();
    }).catch(e=>{
      ElMessage.error(e);
      next({name: "home"});
    })
  } else next();
})

export default router

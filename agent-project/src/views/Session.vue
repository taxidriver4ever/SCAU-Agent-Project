<template>
    <ElAside
        style="display: flex;flex-direction: column;  width: 32vh; height: 100vh; background-color: rgb(40, 40, 40);overflow: hidden;">
        <ElHeader style="display: flex; height: 8vh; align-items: center; flex-direction: row;">
            <div style="display: flex;">
                <ElInput placeholder="search" class="chat-search-all" :prefix-icon="Search"></ElInput>
            </div>
        </ElHeader>
        <ElDivider style="margin: 0; border-color: rgb(60, 60, 60); padding: 0; ">
        </ElDivider>
        <ElMain style="display: flex; margin: 0; padding: 0; flex-direction: column;">
            <div class="chat-history" @click="BeginToChat(index)" v-for="(historySession, index) in historySessions"
                :key="index">
                <!-- <div style="display: flex; padding: 12px;">
                    <ElImage src="/src/images/050-lemonjuice.svg"></ElImage>
                </div> -->
                <div style="display: flex; flex-direction: row; justify-items: flex-end;
                align-items: center;  justify-content: flex-end; align-content: flex-end; margin-left: 20px;">
                    <div style="display: flex; font-weight: 600; font-size: 15px; color: white; margin-bottom: 5px;"
                        v-if="historySession.receiverName.length <= 7">{{
                            historySession.receiverName }}</div>
                    <div style="display: flex; font-weight: 600; font-size: 15px; color: white; margin-bottom: 5px;"
                        v-else>{{
                            historySession.receiverName.substring(0, 6) + "..." }}</div>
                </div>
                <!-- <div style="display: flex; font-size: 12px; color: white; margin-left: auto; margin-top: auto; margin-bottom: auto; margin-right: 10px;">
                    <ElButton type="danger" :circle="true" style="height: 10px; width: 10px;">1</ElButton>
                </div> -->
            </div>
        </ElMain>
    </ElAside>
    <RouterView></RouterView>
</template>

<script setup lang="ts">
import { ElAside, ElButton, ElDivider, ElHeader, ElIcon, ElInput, ElMain, ElImage, ElMessage } from 'element-plus';
import { Search, Plus } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router'
import axios from 'axios';
import { onMounted, ref, onUnmounted } from 'vue';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const historySessions = ref<HistorySession[]>([])
const stompClient = ref<Client | null>(null); // STOMP客户端
const isConnected = ref(false); // 连接状态
const websocketUrl = ref("http://localhost:8080/ws-chat")

class HistorySession {
    updatedTime: string;
    receiverId: string;
    avatar: string;
    receiverName: string;

    constructor(receiverName: string, updatedTime: string, receiverId: string, avatar: string) {
        this.updatedTime = updatedTime;
        this.receiverId = receiverId;
        this.avatar = avatar;
        this.receiverName = receiverName;
    }
}

async function BeginToChat(index: number) {
    localStorage.setItem("receiverId", historySessions.value[index].receiverId)
    localStorage.setItem("receiverName", historySessions.value[index].receiverName)
    window.location.href = "/Friends/Session/Chat"
}

// 连接到WebSocket服务器
const connect = () => {
    try {
        stompClient.value = new Client({
            webSocketFactory: () => new SockJS(websocketUrl.value),
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
            connectHeaders: {
                "id": localStorage.getItem("id") || "",
                "uuid": localStorage.getItem("uuid") || ""
            },
            onConnect: () => {
                console.log('STOMP连接已建立');
                // 订阅私聊消息
                stompClient.value?.subscribe(
                    `/user/queue/chat.private`,
                    (message) => {
                        const chatMessage = JSON.parse(message.body);
                        console.log(message);
                        handleIncomingMessage(chatMessage);
                    }
                );

                isConnected.value = true;
            },
            onStompError: (frame) => {
                console.error('STOMP协议错误:', frame);
                alert('STOMP协议错误: ' + frame.headers['message']);
            },
            onWebSocketError: (event) => {
                console.error('WebSocket错误:', event);
                alert('WebSocket连接错误，请检查URL和服务器状态');
            },
            onDisconnect: () => {
                console.log('STOMP连接已断开');
            }
        });

        // 激活客户端
        stompClient.value.activate();
    } catch (error) {
        console.error('连接失败:', error);
    }
};

// 处理收到的消息
const handleIncomingMessage = (chatMessage: any) => {
    
    if (localStorage.getItem("id") === chatMessage.receiverId && localStorage.getItem("receiverId") !== chatMessage.senderId) {
        ElMessage.info(`收到来自 ${chatMessage.userName} 的新消息`);
    }
};


async function SelectSessions() {
    await axios({
        url: "http://127.0.0.1:8080/chat/FindAllSessions",
        method: "POST",
        headers: {
            "id": localStorage.getItem("id"),
            "uuid": localStorage.getItem("uuid")
        },
        data: {
            userId: localStorage.getItem("id")
        }
    }).then(res => {
        if (res.data.code === 200) {
            historySessions.value = res.data.data.map((item: any) =>
                new HistorySession(
                    item.receiverName,
                    item.updatedTime,
                    item.receiverId,
                    item.avatar
                )
            );
        }
        else ElMessage.error(res.data.message)
    }).catch(e => {
        ElMessage.error(e)
    })
}

onMounted(() => {
    SelectSessions();
    connect();
})
onUnmounted(() => {
    localStorage.removeItem("receiverId")
    localStorage.removeItem("receiverName")
})
</script>

<style lang="css" scoped>
.chat-search-all {
    color: white;
    height: 27px;
    border: 0;
}

/* 输入框样式 */
:deep(.chat-search-all .el-input__wrapper) {
    color: white;
    background-color: rgb(55, 55, 55) !important;
    border: 0;
    box-shadow: 0 0 0 0 !important;
}

:deep(.chat-search-all .el-input__wrapper.is-focus) {
    color: white;
    background-color: rgb(55, 55, 55) !important;
    box-shadow: 0 0 0 1px white !important;
    border: 0;
    outline: none !important;
}

:deep(.chat-search-all .el-input__inner) {
    color: white;
    height: 24px;
    background-color: rgb(55, 55, 55) !important;
    border: 0;
}

.chat-history {
    flex-direction: row;
    display: flex;
    width: 32vh;
    height: 8vh;
    border-radius: 0;
    background-color: rgb(40, 40, 40);
    border: 0;
}

.chat-history:hover {
    display: flex;
    width: 32vh;
    height: 8vh;
    border-radius: 0;
    background-color: rgb(60, 60, 60);
    border: 0;
}
</style>
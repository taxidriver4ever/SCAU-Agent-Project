<template>
    <ElContainer style="display: flex; height: 100vh; background-color: rgb(25, 25, 25);overflow: hidden;">
        <ElHeader style="padding-left: 15px; height: 8vh;display:flex; align-items: center; color: white;">
            {{ receiverName }}
        </ElHeader>
        <ElDivider style="margin: 0; border-color: rgb(60, 60, 60);"></ElDivider>
        <ElMain style="padding:15px; display: flex; flex-direction: column; overflow-y: auto;" class="chat-container">
            <!-- 消息显示区域 -->
            <div v-for="(chatContent, index) in chatContents" :key="index"
                :class="['message-container', chatContent.senderId === currentUser ? 'message-self' : 'message-other']">
                <div class="message-bubble">
                    <div>{{ chatContent.content }}</div>
                    <div class="message-time">
                        {{ chatContent.getFormattedTime() }}
                    </div>
                </div>
            </div>
        </ElMain>
        <ElDivider style="margin: 0; border-color: rgb(60, 60, 60);"></ElDivider>
        <ElFooter style="margin: 0; padding-left: 10px; height: 18vh; display: flex; flex-direction: column;">
            <div style="display: flex; flex-direction: row; margin-top: 5px;">
                <ElButton style="width: 30px; height: 30px;" type="text" class="features-button">
                    <ElIcon size="20">
                        <PictureFilled />
                    </ElIcon>
                </ElButton>
            </div>
            <ElInput @keydown="handleKeyDown" resize="none" type="textarea" class="send-message-input"
                v-model="InputContent" :autosize="{ minRows: 2, maxRows: 2 }">
            </ElInput>
            <div style="margin-top: 10px; display: flex; justify-content: flex-end;">
                <ElButton @click="sendMessage" class="send-message-button" :disabled="ContentIsEmpty">Send
                </ElButton>
            </div>
        </ElFooter>
    </ElContainer>
</template>

<script setup lang="ts">
import { ElMessage, ElButton, ElContainer, ElDivider, ElFooter, ElHeader, ElIcon, ElInput, ElMain, ElImage } from 'element-plus';
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue';
import { PictureFilled } from '@element-plus/icons-vue';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axios from 'axios';

const chatContents = ref<ChatContent[]>([]); // 聊天内容列表
const currentUser = ref(localStorage.getItem("id")); // 当前用户（随机生成）
const InputContent = ref(""); // 输入框内容
const stompClient = ref<Client | null>(null); // STOMP客户端
const isConnected = ref(false); // 连接状态
const websocketUrl = ref("http://localhost:8080/ws-chat")
const receiverId = ref(localStorage.getItem("receiverId"))
const receiverName = ref(localStorage.getItem("receiverName"))
// 聊天内容类
class ChatContent {
    createTime: string;
    senderId: string;
    content: string;
    avatar: string;
    receiverId: string;
    userName: string;

    constructor(createTime: string, receiverId: string, senderId: string, content: string, avatar: string = "", userName: string) {
        this.createTime = createTime;
        this.receiverId = receiverId;
        this.content = content;
        this.avatar = avatar || "/src/images/048-gingerbread.svg";
        this.senderId = senderId;
        this.userName = userName;
    }

    // 格式化时间显示
    getFormattedTime(): string {
        if (!this.createTime) return '';
        try {
            const date = new Date(this.createTime);
            return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
        } catch (e) {
            return this.createTime;
        }
    }
}

async function GetChatHistory() {
    await axios({
        url: "http://127.0.0.1:8080/chat/GetChatHistory",
        method: "POST",
        headers: {
            "id": localStorage.getItem("id"),
            "uuid": localStorage.getItem("uuid")
        },
        data: {
            "senderId": localStorage.getItem("id"),
            "receiverId": localStorage.getItem("receiverId")
        }
    }).then(res=>{
        if(res.data.code === 200) {
            // console.log(res.data.data);
            chatContents.value = res.data.data.map((item: any)=>
                new ChatContent(
                    item.createTime,
                    item.receiverId,
                    item.senderId,
                    item.content,
                    "",
                    ""
                )
            )
        }
        else ElMessage.error("Get chat history error")
    }).catch(e=>{
        ElMessage.error(e);
        console.log(e);        
    })
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


    if (chatMessage.senderId === localStorage.getItem("receiverId") ||
        chatMessage.senderId === localStorage.getItem("id")) {
        const newChatContent = new ChatContent(
            chatMessage.createTime || new Date().toISOString(),
            chatMessage.receiverId,
            chatMessage.senderId,
            chatMessage.content,
            chatMessage.avatar,
            chatMessage.receiverName
        );

        chatContents.value.push(newChatContent);
        scrollToBottom();
    }

    // 如果消息不是来自当前选中的聊天对象，可以显示通知
    // if (chatMessage.from !== receiverId.value) {
    //     ElMessage.info(`收到来自 ${chatMessage.fromName} 的新消息`);
    // }
};

// 处理键盘事件（回车发送消息）
function handleKeyDown(event: KeyboardEvent | Event) {
    if ('key' in event && event instanceof KeyboardEvent) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            if (InputContent.value.trim()) sendMessage();
        }
    }
}

// 发送消息
async function sendMessage() {
    if (!InputContent.value.trim()) return;
    
    else if(InputContent.value.length >= 2048) {
        ElMessage.error("Message limit exceeded");
        return
    }

    if (stompClient.value && isConnected.value) {
        // 创建消息对象
        const chatMessage = {
            senderId: localStorage.getItem("id"),
            content: InputContent.value.trim(),
            type: 'CHAT',
            createTime: new Date().toISOString(),
            avatar: "/src/images/048-gingerbread.svg",
            receiverId: receiverId.value,
            receiverName: receiverName.value
        };

        // 发送消息到服务器
        stompClient.value.publish({
            destination: '/app/chat.private',
            body: JSON.stringify(chatMessage)
        });

        // 清空输入框
        InputContent.value = "";
    } else {
        ElMessage.error('未连接到聊天服务器');
    }
}
// 计算属性：检查输入内容是否为空
const ContentIsEmpty = computed(() => {
    return !InputContent.value.trim();
});

// 自动滚动到底部
const scrollToBottom = () => {
    nextTick(() => {
        const container = document.querySelector('.chat-container');
        if (container) {
            container.scrollTop = container.scrollHeight;
        }
    });
};

// 组件挂载时连接WebSocket
onMounted(() => {
    connect();
    GetChatHistory();
});

// 组件卸载时断开连接
onUnmounted(() => {
    if (stompClient.value) stompClient.value.deactivate();
});
</script>

<style lang="css" scoped>
/* 消息样式 */
.message-container {
    width: 100%;
    display: flex;
    margin: 10px 0;
}

.message-self {
    justify-content: flex-end;
}

.message-other {
    justify-content: flex-start;
}

.message-bubble {
    max-width: 70%;
    padding: 10px 15px;
    border-radius: 18px;
    position: relative;
}

.message-self .message-bubble {
    background-color: #0084ff;
    color: white;
    border-bottom-right-radius: 0;
}

.message-other .message-bubble {
    background-color: #282828;
    color: white;
    border-bottom-left-radius: 0;
}

.message-sender {
    font-size: 12px;
    margin-bottom: 4px;
    opacity: 0.8;
}

.message-time {
    font-size: 10px;
    margin-top: 4px;
    opacity: 0.6;
    text-align: right;
}

.features-button {
    width: 40px;
    height: 40px;
    color: white;
}

.features-button:hover {
    color: rgb(255, 255, 255) !important;
    background-color: rgb(50, 50, 50) !important;
    width: 40px;
    height: 40px;
}

:deep(.send-message-input .el-textarea__wrapper) {
    color: white;
    background-color: rgb(25, 25, 25) !important;
    border: 0;
    box-shadow: 0 0 0 0 !important;
}

:deep(.send-message-input .el-textarea__inner) {
    color: white;
    font-size: 15px;
    height: 24px;
    box-shadow: none !important;
    background-color: rgb(25, 25, 25) !important;
    scrollbar-color: rgb(25, 25, 25) !important;
    border: 0 solid wheat;
    scrollbar-color: rgb(100, 100, 100) rgb(25, 25, 25);
    scrollbar-width: thin;
}

.send-message-button:disabled {
    border: 0;
    background-color: rgb(35, 35, 35);
    color: rgb(80, 80, 80);
}

.send-message-button {
    background-color: white;
    color: black;
}

.send-message-button:hover {
    background-color: white;
    color: black;
}
</style>
<template>
    <div style="overflow: hidden;">
        <el-container style="overflow: hidden;">
            <el-aside width="300px" style="background-color: whitesmoke;height: 100vh;">
                <el-container>
                    <ElHeader style="height: 10vh; margin-bottom:5px">
                        <ElButton type="text"
                            style="height: 60px; font-size: 37px;font-weight: 550 ; color: black;font-family:'Gill Sans', 'Gill Sans MT', Calibri, 'Trebuchet MS', sans-serif;margin-top: 5px;"
                            @click="deepsuck">
                            华农求索</ElButton>
                        <br></br>
                        <ElButton @click="deepsuck" class="start-new-session" :icon="Plus">新对话</ElButton>
                        <div
                            style="font-weight: 700; font-size: 20px; font-family:Cambria, Cochin, Georgia, Times, 'Times New Roman', serif;">
                            对话历史</div>
                    </ElHeader>
                    <ElMain class="history-conversation">
                        <div style="margin-top: 5px;" v-for="(HistoryConversation, index) in HistoryConversations"
                            :key="index">
                            <ElButtonGroup class="history-button-group"
                                :class="{ 'selected': selectedIndex === index }">
                                <ElButton @click="GetHistoryMessage(index)" type="text" class="history-title">{{
                                    HistoryConversation }}
                                </ElButton>
                                <ElButton @click="DeleteHistoryMessage(index)" :icon="Delete" class="history-delete">
                                </ElButton>
                            </ElButtonGroup>
                        </div>
                    </ElMain>
                    <el-footer>
                        <ElButton class="back-to-home" type="text" :icon="Back" @click="BackToHome">返回到主页面
                        </ElButton>
                    </el-footer>
                </el-container>
            </el-aside>
            <el-container class="converstaion-container" v-if="!ShowDeepSuck">
                <ElHeader
                    style="display: flex; align-items: center; justify-content: center; font-weight: 700; margin: 0 auto; width: 80%;">
                    {{ AIWithUserChatContents[0]?.createTime || '当前会话' }}
                </ElHeader>
                <ElMain class="chat-container" style="display: flex;
                    flex-direction: column;
                    width: 80%;
                    margin: 0 auto;
                    padding: 10px 10px;
                    height: 70vh;
                    overflow-y: auto;">
                    <div v-for="(AIWithUserChatContent, index) in AIWithUserChatContents" :key="index"
                        class="message-container">
                        <div v-if="AIWithUserChatContent.status === 'user'" class="user-message">
                            <div class="message-bubble user-bubble">
                                {{ AIWithUserChatContent.content }}
                            </div>
                            <div class="avatar-container">
                                <ElAvatar style="background-color: white;" :src="AIWithUserChatContent.avatar"
                                    shape="square"></ElAvatar>
                            </div>
                        </div>
                        <div v-else class="ai-message">
                            <div class="avatar-container">
                                <ElAvatar style="background-color: white;" :src="AIWithUserChatContent.avatar"
                                    shape="square"></ElAvatar>
                            </div>
                            <div class="message-bubble ai-bubble">
                                <div class="markdown-content-wrapper">
                                    <div v-html="enhancedParseMarkdownContent(AIWithUserChatContent.content)"
                                        class="markdown-content"></div>
                                </div>
                                <span v-if="isStreaming && index === AIWithUserChatContents.length - 1"
                                    class="streaming-indicator">
                                    <span class="streaming-dot"></span>
                                    <span class="streaming-dot"></span>
                                    <span class="streaming-dot"></span>
                                </span>
                            </div>
                        </div>
                    </div>
                </ElMain>
                <el-footer class="deepsuck-container-in-history">
                    <div class="input-wrapper-in-history">
                        <ElInput placeholder="发送信息给华农求索" :rows="2"
                            :autosize="{ minRows: 2, maxRows: 4 }" type="textarea" resize="none" v-model="InputContent"
                            class="input-content no-border-input" @keydown="handleKeyDown">
                        </ElInput>
                        <div class="button-wrapper-in-history">
                            <ElButton type="danger" :circle="true" v-if="isStreaming" @click="disconnectFromAIStream">
                                <el-icon>
                                    <SwitchButton />
                                </el-icon>
                            </ElButton>
                            <ElButton :circle="true" @click="sendMessageInHistory"
                                :disabled="ContentIsEmpty || isStreaming || AIWithUserChatContents.length >= 20">
                                <ElIcon>
                                    <Promotion />
                                </ElIcon>
                            </ElButton>
                        </div>
                    </div>
                </el-footer>
            </el-container>
            <div class="deepsuck-container" v-if="ShowDeepSuck">
                <h2 style="margin-bottom: 20px;">华农求索</h2>
                <div style="margin-bottom: 20px;font-weight: 300;">为您的复杂问题获得即时、数据驱动的答案</div>
                <div class="input-wrapper">
                    <ElInput placeholder="发送信息给华农求索" :rows="2" :autosize="{ minRows: 2, maxRows: 4 }"
                        type="textarea" resize="none" v-model="InputContent" class="input-content no-border-input"
                        @keydown="handleKeyDown">
                    </ElInput>
                    <div class="button-wrapper">
                        <ElButton :circle="true" @click="SendMessage" :disabled="ContentIsEmpty || isStreaming">
                            <ElIcon>
                                <Promotion />
                            </ElIcon>
                        </ElButton>
                    </div>
                </div>
            </div>
        </el-container>
    </div>
</template>

<script setup lang="ts">
import { ElContainer, ElHeader, ElMain, ElAside, ElFooter, ElButton, ElInput, ElIcon, ElMessage, ElImage, ElAvatar, ElButtonGroup, ElMessageBox } from 'element-plus';
import { Delete, Promotion, Plus, Back, SwitchButton, Timer } from '@element-plus/icons-vue';
import { computed, onMounted, ref, onUnmounted, onUpdated, nextTick, watch } from 'vue';
import axios from 'axios';
import { parseMarkdown } from '@/utils/markdownParser';

const ShowDeepSuck = ref(true);
const InputContent = ref<string>("");
const AIWithUserChatContents = ref<ChatContent[]>([]);
const HistoryConversations = ref<string[]>([]);
const selectedIndex = ref<number>();
const sessionId = ref<string>("");
const now = ref(new Date());
const eventSource = ref<EventSource | null>(null);
const isStreaming = ref(false);
// const streamError = ref<string>('');

// 在组件中直接使用导入的函数
const parseMarkdownContent = (content: string): string => {
    return parseMarkdown(content);
};

// 图片路径解析函数
const parseImagePaths = (content: string): string => {
    // 匹配 [./path/to/image.jpg] 格式的图片路径
    const imageRegex = /\[(\.\/[^\]]+\.(?:jpg|jpeg|png|gif|bmp|webp|svg))\]/gi;

    return content.replace(imageRegex, (match, imagePath) => {
        // 将相对路径转换为绝对路径（根据实际项目结构调整）
        const absolutePath = imagePath.startsWith('./')
            ? `/src/images/${imagePath.substring(2)}`
            : `/src/images/${imagePath}`;
        return `<div class="image-container"><img src="${absolutePath}" alt="Image" style="max-width:100%" /></div>`;
    });
};

// 修改后的parseMarkdownContent函数，先解析图片路径，再解析Markdown
const enhancedParseMarkdownContent = (content: string): string => {
    const withImages = parseImagePaths(content);
    return parseMarkdown(withImages);
};

// 添加onMounted生命周期钩子
onMounted(async () => {
    await loadConversationHistory();
});

function BackToHome() { window.location.href = "/" }

// 新增方法：加载会话历史
async function loadConversationHistory() {
    await axios({
        url: "http://127.0.0.1:8080/ai/GetConversationHistory",
        method: "GET",
        headers: {
            "id": localStorage.getItem("id"),
            "uuid": localStorage.getItem("uuid")
        },
        params: {
            userId: localStorage.getItem("id")
        }
    }).then(res => {
        if (res.data.code === 200) {
            HistoryConversations.value = res.data.data;
        }
    }).catch(e => {
        ElMessage.error(e)
    });
}

class ChatContent {
    createTime: string;
    status: string;
    content: string;
    avatar: string;

    constructor(createTime: string, status: string, content: string, avatar: string = "") {
        this.createTime = createTime;
        this.status = status;
        this.content = content;
        this.avatar = avatar || "/src/images/048-gingerbread.svg";
    }

    isUserMessage(): boolean {
        return this.status === 'user';
    }

    isAIMessage(): boolean {
        return this.status === 'ai';
    }

    getFormattedTime(): string {
        if (!this.createTime) return '';
        try {
            const date = new Date(this.createTime);
            return date.toLocaleString('zh-CN');
        } catch (e) {
            return this.createTime;
        }
    }
}

const ContentIsEmpty = computed(() => {
    return !InputContent.value.trim();
});

function deepsuck() {
    ShowDeepSuck.value = true;
    selectedIndex.value = undefined;
    disconnectFromAIStream();
}

function handleKeyDown(event: KeyboardEvent | Event) {
    if ('key' in event && event instanceof KeyboardEvent) {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            if (!InputContent.value.trim()) {
                console.log("Please input your question");
            } else if (!isStreaming.value) {
                if (AIWithUserChatContents.value.length >= 20 && !ShowDeepSuck.value) {
                    ElMessage.warning("The maximum limit for sending messages has been exceeded. Please create a new conversation")
                }
                else if (ShowDeepSuck.value) {
                    SendMessage();
                } else {
                    sendMessageInHistory();
                }
            }
        }
    }
}

function padTo2Digits(num: number): string {
    return num.toString().padStart(2, '0');
}

function formatDateTime(date: Date): string {
    const year = date.getFullYear();
    const month = padTo2Digits(date.getMonth() + 1);
    const day = padTo2Digits(date.getDate());
    const hours = padTo2Digits(date.getHours());
    const minutes = padTo2Digits(date.getMinutes());
    const seconds = padTo2Digits(date.getSeconds());

    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

async function AddNewSession() {
    await axios({
        url: "http://127.0.0.1:8080/ai/AddNewSession",
        method: "POST",
        headers: {
            "id": localStorage.getItem("id"),
            "uuid": localStorage.getItem("uuid")
        },
        data: {
            'userId': localStorage.getItem("id"),
            'createTime': now.value,
            'sessionId': sessionId.value,
            'historyMessage': InputContent.value
        }
    }).then(res => {
        if (res.data.code === 200) {
            HistoryConversations.value.unshift(res.data.data.toString());
            selectedIndex.value = 0;
        } else {
            ElMessage.error("Add new session failed");
        }
    }).catch(e => {
        ElMessage.error(e);
    });
}

async function ShowHistory() {
    await axios({
        url: "http://127.0.0.1:8080/ai/ShowHistory",
        method: "GET",
        headers: {
            "id": localStorage.getItem("id"),
            "uuid": localStorage.getItem("uuid")
        },
        params: {
            id: sessionId.value
        }
    }).then(res => {
        if (res.data.code === 200) {
            InputContent.value = "";
            AIWithUserChatContents.value = [];
            AIWithUserChatContents.value = res.data.data.map((item: any) =>
                new ChatContent(
                    item.createTime,
                    item.status,
                    item.content,
                    item.avatar
                )
            );
            ShowDeepSuck.value = false;
            // 自动滚动到底部
            nextTick(() => {
                const container = document.querySelector('.chat-container');
                if (container) {
                    container.scrollTo({
                        top: container.scrollHeight,
                    });
                }
            });
        } else {
            ElMessage.error("Get history failed: " + res.data.message);
        }
    }).catch(e => {
        ElMessage.error(e);
    });
}

async function SendMessage() {
    now.value = new Date();
    const userMessageContent = InputContent.value;

    await axios({
        url: "http://127.0.0.1:8080/ai/SaveMessage",
        method: "POST",
        headers: {
            "id": localStorage.getItem("id"),
            "uuid": localStorage.getItem("uuid")
        },
        data: {
            'userId': localStorage.getItem("id"),
            'createTime': now.value,
            'historyMessage': userMessageContent,
            'status': 'user',
            'avatar': "/src/images/039-coconutdrink.svg"
        }
    }).then(async (res) => {
        if (res.data.code === 200) {
            sessionId.value = res.data.data;

            // 添加用户消息
            const userMessage = new ChatContent(
                formatDateTime(now.value),
                "user",
                userMessageContent,
                "/src/images/039-coconutdrink.svg"
            );
            AIWithUserChatContents.value.push(userMessage);

            // 添加新会话
            await AddNewSession();

            await ShowHistory();
            // 开始接收 AI 流式响应
            // 添加空的 AI 消息占位符
            const aiMessage = new ChatContent(
                formatDateTime(new Date()),
                "ai",
                "",
                "/src/images/ai-avatar.svg"
            );
            AIWithUserChatContents.value.push(aiMessage);
            // 自动滚动到底部
            nextTick(() => {
                const container = document.querySelector('.chat-container');
                if (container) {
                    container.scrollTo({
                        top: container.scrollHeight,
                        behavior: 'smooth'
                    });
                }
            });
            await connectToAIStream(userMessageContent);
        } else {
            ElMessage.error(res.data.message);
        }
    }).catch(e => {
        ElMessage.error(e);
    });
}

async function sendMessageInHistory() {
    now.value = new Date();
    const userMessageContent = InputContent.value;

    await axios({
        url: "http://127.0.0.1:8080/ai/SaveMessage",
        method: "POST",
        headers: {
            "id": localStorage.getItem("id"),
            "uuid": localStorage.getItem("uuid")
        },
        data: {
            'userId': localStorage.getItem("id"),
            'createTime': now.value,
            'historyMessage': userMessageContent,
            'status': 'user',
            'sessionId': sessionId.value,
            'avatar': "/src/images/039-coconutdrink.svg"
        }
    }).then(async (res) => {
        if (res.data.code === 200) {
            ShowDeepSuck.value = false;

            // 添加用户消息
            const userMessage = new ChatContent(
                formatDateTime(now.value),
                "user",
                userMessageContent,
                "/src/images/039-coconutdrink.svg"
            );
            AIWithUserChatContents.value.push(userMessage);

            // 自动滚动到底部
            nextTick(() => {
                const container = document.querySelector('.chat-container');
                if (container) {
                    container.scrollTo({
                        top: container.scrollHeight,
                        behavior: 'smooth'
                    });
                }
            });

            // 添加空的 AI 消息占位符
            const aiMessage = new ChatContent(
                formatDateTime(new Date()),
                "ai",
                "",
                "",
            );
            AIWithUserChatContents.value.push(aiMessage);

            InputContent.value = "";

            // 开始接收 AI 流式响应
            await connectToAIStream(userMessageContent);

        } else {
            ElMessage.error(res.data.message);
            setTimeout(() => {
                window.location.href = "/ChatWithAi"
            }, 1000)
        }
    }).catch(e => {
        ElMessage.error(e);
    });
}

async function connectToAIStream(message: string) {
    disconnectFromAIStream();

    isStreaming.value = true;

    try {
        const baseUrl = 'http://127.0.0.1:8080/ai/stream';
        const params = new URLSearchParams({
            message: message,
            sessionId: sessionId.value || 'new-session',
            userId: localStorage.getItem("id") || '',
            id: localStorage.getItem("id") || '',
            uuid: localStorage.getItem("uuid") || ''
        });

        const url = `${baseUrl}?${params}`;

        eventSource.value = new EventSource(url);

        eventSource.value.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data).delta;
                const JSONData = JSON.parse(event.data);
                // 检查结束标志（支持多种格式）
                const endMarkers = ['[DONE]', '[END]', '[FINISH]', 'DONE', 'END'];
                if (JSONData.type === "finished") {
                    console.log('流式响应正常结束');
                    isStreaming.value = false;
                    disconnectFromAIStream();

                    // 可选：保存完整的对话到历史
                    saveCompleteConversation();
                    return;
                } else if (JSONData.type === "intent") {
                    AIWithUserChatContents.value
                    [AIWithUserChatContents.value.length - 1].avatar = "/src/images/" + JSONData.avatar.replace(/\s/g, '');
                }

                // 查找最后一个 AI 消息
                let lastAIMessageIndex = -1;
                for (let i = AIWithUserChatContents.value.length - 1; i >= 0; i--) {
                    if (AIWithUserChatContents.value[i].status === 'ai') {
                        lastAIMessageIndex = i;
                        break;
                    }
                }

                if (lastAIMessageIndex !== -1) {
                    // 处理特殊格式（如 JSON）
                    if (data.startsWith('{') && data.endsWith('}')) {
                        try {
                            const jsonData = JSON.parse(data);
                            if (jsonData.content) {
                                AIWithUserChatContents.value[lastAIMessageIndex].content += jsonData.content;
                            } else if (jsonData.text) {
                                AIWithUserChatContents.value[lastAIMessageIndex].content += jsonData.text;
                            }
                        } catch {
                            // 如果不是有效的 JSON，直接追加
                            AIWithUserChatContents.value[lastAIMessageIndex].content += data;
                        }
                    } else {
                        AIWithUserChatContents.value[lastAIMessageIndex].content += data;
                    }

                    // 触发响应式更新
                    AIWithUserChatContents.value = [...AIWithUserChatContents.value];

                    // 自动滚动到底部
                    nextTick(() => {
                        const container = document.querySelector('.chat-container');
                        if (container) {
                            container.scrollTo({
                                top: container.scrollHeight,
                                behavior: 'smooth'
                            });
                        }
                    });
                }
            } catch (error) {
                console.error('处理流消息错误:', error);
            }
        };

        eventSource.value.onerror = (error) => {
            console.error('SSE连接错误:', error);
            AIWithUserChatContents.value.pop();
            AIWithUserChatContents.value.push(new ChatContent
                ("", "ai",
                    'The connection is broken, please try again',
                    "/src/images/048-gingerbread.svg"
                )
            )
            isStreaming.value = false;
            disconnectFromAIStream();
        };

        eventSource.value.onopen = () => {
            console.log('SSE连接已建立');
        };

        // 添加超时处理（可选）
        setTimeout(() => {
            if (isStreaming.value) {
                console.warn('流式响应超时，强制结束');
                AIWithUserChatContents.value.pop();
                AIWithUserChatContents.value.push(new ChatContent
                    ("", "ai",
                        'The response timed out, please try again',
                        "/src/images/048-gingerbread.svg"
                    )
                )
                isStreaming.value = false;
                disconnectFromAIStream();
            }
        }, 60000 * 5); // 5分钟超时

    } catch (error) {
        console.error('创建SSE连接失败:', error);
        AIWithUserChatContents.value.pop();
        AIWithUserChatContents.value.push(new ChatContent
            ("", "ai",
                'Connection failed',
                "/src/images/048-gingerbread.svg"
            )
        );
        isStreaming.value = false;
    }
}

// 添加保存完整对话的方法（可选）
async function saveCompleteConversation() {
    try {
        // 获取最后一个 AI 消息的完整内容
        let lastAIMessageIndex = -1;
        for (let i = AIWithUserChatContents.value.length - 1; i >= 0; i--) {
            if (AIWithUserChatContents.value[i].status === 'ai') {
                lastAIMessageIndex = i;
                break;
            }
        }

        if (lastAIMessageIndex !== -1) {
            const aiMessage = AIWithUserChatContents.value[lastAIMessageIndex];

            await axios({
                url: "http://127.0.0.1:8080/ai/SaveMessage",
                method: "POST",
                headers: {
                    "id": localStorage.getItem("id"),
                    "uuid": localStorage.getItem("uuid")
                },
                data: {
                    'userId': localStorage.getItem("id"),
                    'createTime': new Date(),
                    'historyMessage': aiMessage.content,
                    'status': 'ai',
                    'sessionId': sessionId.value,
                    'avatar': AIWithUserChatContents.value[AIWithUserChatContents.value.length - 1].avatar
                }
            });
        }
    } catch (error) {
        console.error('保存完整对话失败:', error);
    }
}

function disconnectFromAIStream() {
    if (eventSource.value) {
        eventSource.value.close();
        eventSource.value = null;
    }
    isStreaming.value = false;
}

async function GetHistoryMessage(index: number) {
    disconnectFromAIStream();

    await axios({
        url: "http://127.0.0.1:8080/ai/GetUUid",
        method: "GET",
        headers: {
            "id": localStorage.getItem("id"),
            "uuid": localStorage.getItem("uuid")
        },
        params: {
            userId: localStorage.getItem("id"),
            index: index
        }
    }).then(res => {
        if (res.data.code === 200) {
            sessionId.value = res.data.data;
            selectedIndex.value = index;
            ShowHistory();
        } else {
            ElMessage.error(res.data.message);
        }
    }).catch(e => {
        ElMessage.error(e);
    });
}

async function DeleteHistory(index: number, sessionUUID: string) {
    await axios({
        url: "http://127.0.0.1:8080/ai/DeleteHistory",
        method: "DELETE",
        headers: {
            "id": localStorage.getItem("id"),
            "uuid": localStorage.getItem("uuid")
        },
        params: {
            sessionId: sessionUUID,
            userId: localStorage.getItem("id"),
            index: index
        }
    }).then(res => {
        if (res.data.code === 200) {
            HistoryConversations.value.splice(index, 1);
            if (selectedIndex.value !== undefined) {
                if (selectedIndex.value > index) selectedIndex.value--;
                else if (selectedIndex.value === index) {
                    selectedIndex.value = undefined;
                    ShowDeepSuck.value = true;
                }
            }
            ElMessage.success("Delete successful");
        } else {
            ElMessage.error(res.data.message);
        }
    }).catch(e => {
        ElMessage.error(e);
    });
}

async function DeleteHistoryMessage(index: number) {
    ElMessageBox.confirm(
        'Are you sure you want to delete?',
        'Warning',
        {
            cancelButtonText: 'Cancel',
            confirmButtonText: 'OK',
            type: 'warning',
            customClass: 'custom-message-box'
        }
    ).then(async () => {
        await axios({
            url: "http://127.0.0.1:8080/ai/GetUUid",
            method: "GET",
            headers: {
                "id": localStorage.getItem("id"),
                "uuid": localStorage.getItem("uuid")
            },
            params: {
                userId: localStorage.getItem("id"),
                index: index.toString()
            }
        }).then(res => {
            if (res.data.code === 200) {
                DeleteHistory(index, res.data.data);
            } else {
                ElMessage.error(res.data.message);
            }
        }).catch(e => {
            ElMessage.error(e);
        });
    });
}

// 组件卸载时自动断开连接
onUnmounted(() => {
    disconnectFromAIStream();
});
</script>

<style lang="css" scoped>
.history-conversation {
    overflow: hidden;
    margin-top: 70px;
    height: 70vh;
}

/* Webkit浏览器 */
.history-conversation::-webkit-scrollbar {
    width: 12px;
}

.history-conversation::-webkit-scrollbar-track {
    background: #dfdfdf;
    border-radius: 6px;
}

.history-conversation::-webkit-scrollbar-thumb {
    background: #6b6b6b;
    border-radius: 6px;
    border: 3px solid #f0f0f0;
}

.history-conversation::-webkit-scrollbar-thumb:hover {
    background: #a8a8a8;
}

.converstaion-container {
    height: 100vh;
    overflow: hidden;
    display: flex;
}

.deepsuck-container {
    /* overflow: hidden; */
    display: flex;
    flex-direction: column;
    align-items: center;
    align-content: center;
    justify-content: center;
    justify-items: center;
    height: 100vh;
    width: 100%;
}

.deepsuck-container-in-history {
    /* overflow: hidden; */
    height: 15vh;
    display: flex;
    flex-direction: column;
    align-items: center;
    align-content: center;
    justify-content: center;
    justify-items: center;
    width: 100%;
    margin-top: 50px;
}

.input-wrapper {
    width: 80%;
    display: flex;
    flex-direction: column;
    border: 1px solid #dcdfe6;
    border-radius: 10px;
    padding: 10px;
    background-color: #fff;
    box-shadow: 5px 5px 5px 2px rgba(0, 0, 0, 0.1);
    margin-bottom: 150px;
}

.input-wrapper-in-history {
    height: 100vh;
    width: 80%;
    display: flex;
    flex-direction: column;
    border: 1px solid #dcdfe6;
    border-radius: 10px;
    padding: 10px;
    background-color: #fff;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    margin-bottom: 60px;
}

.input-content {
    font-size: 15px;
    width: 100%;
}

.no-border-input :deep(.el-textarea__inner) {
    border: none;
    box-shadow: none;
    padding: 2px 5px;
}

.no-border-input :deep(.el-textarea__inner:focus) {
    border: none;
    box-shadow: none;
    outline: none;
}

.button-wrapper {
    display: flex;
    justify-content: flex-end;
    width: 100%;
}

.button-wrapper-in-history {
    display: flex;
    justify-content: flex-end;
    width: 100%;
}

.message-container {
    margin-bottom: 16px;
}

.user-message {
    display: flex;
    align-items: flex-end;
    justify-content: flex-end;
    gap: 8px;
}

.ai-message {
    display: flex;
    align-items: flex-start;
    justify-content: flex-start;
    gap: 12px;
}

.message-bubble {
    max-width: 88%;
    padding: 12px 16px;
    border-radius: 18px;
    word-break: break-word;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
    position: relative;
}

.user-bubble {
    background-color: #409EFF;
    color: white;
    border-bottom-right-radius: 4px;
}

.ai-bubble {
    background-color: #f0f0f0;
    color: #333;
    border-bottom-left-radius: 4px;
}

.avatar-container {
    display: flex;
    align-items: flex-end;
}

.streaming-indicator {
    display: inline-block;
    margin-left: 8px;
}

.streaming-dot {
    display: inline-block;
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background-color: #409EFF;
    margin: 0 1px;
    animation: streaming 1.4s infinite ease-in-out both;
}

.streaming-dot:nth-child(1) {
    animation-delay: -0.32s;
}

.streaming-dot:nth-child(2) {
    animation-delay: -0.16s;
}

@keyframes streaming {

    0%,
    80%,
    100% {
        transform: scale(0.8);
        opacity: 0.5;
    }

    40% {
        transform: scale(1);
        opacity: 1;
    }
}

.history-button-group {
    height: 5vh;
}

.history-button-group:hover {
    .history-delete:hover {
        background-color: rgb(150, 150, 150);
        border: 0px;
        color: white;
    }

    .history-delete {
        background-color: #b9b9b9;
        color: white;
    }

    .history-title {
        background-color: #b9b9b9;
        color: white;
    }
}

.history-delete {
    height: 5vh;
    background-color: whitesmoke;
    border: 0px;
}

.history-title {
    height: 5vh;
    padding-left: 10px;
    display: flex;
    justify-content: flex-start;
    color: black;
    width: 24vh;
    overflow: hidden;
}

.history-button-group.selected {
    .history-delete {
        background-color: rgb(100, 100, 100) !important;
        color: white !important;
    }

    .history-delete:hover {
        background-color: rgb(80, 80, 80) !important;
        border: 0px !important;
        color: white !important;
    }

    .history-title {
        background-color: rgb(100, 100, 100) !important;
        color: white !important;
    }
}

.start-new-session {
    margin-top: 10px;
    height: 35px;
    font-size: 18px;
    border-radius: 30px;
    width: 200px;
    margin-bottom: 20px;
}

.start-new-session:hover {
    background-color: rgb(50, 50, 50);
    color: rgb(255, 255, 255);
    border: 0;
}

.back-to-home {
    margin-top: 15px;
    font-size: 15px;
    color: #333;
}

.back-to-home:hover {
    color: gray !important
}

/* 添加 Markdown 内容样式 */
.markdown-content {
    max-width: 100%;
    line-height: 1.6;
}

.markdown-content h1,
.markdown-content h2,
.markdown-content h3,
.markdown-content h4,
.markdown-content h5,
.markdown-content h6 {
    margin: 1em 0 0.5em 0;
    font-weight: 600;
}

.markdown-content h1 {
    font-size: 1.8em;
}

.markdown-content h2 {
    font-size: 1.6em;
}

.markdown-content h3 {
    font-size: 1.4em;
}

.markdown-content h4 {
    font-size: 1.2em;
}

.markdown-content h5 {
    font-size: 1.1em;
}

.markdown-content h6 {
    font-size: 1em;
}

.markdown-content p {
    margin: 0.5em 0;
}

.markdown-content ul,
.markdown-content ol {
    margin: 0.5em 0;
    padding-left: 2em;
}

.markdown-content li {
    margin: 0.25em 0;
}

.markdown-content blockquote {
    border-left: 4px solid #ddd;
    margin: 1em 0;
    padding-left: 1em;
    color: #666;
}

.markdown-content code {
    background-color: #f5f5f5;
    padding: 0.2em 0.4em;
    border-radius: 3px;
    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
    font-size: 0.9em;
}

.markdown-content pre {
    background-color: #f5f5f5;
    padding: 1em;
    border-radius: 5px;
    overflow-x: auto;
    margin: 1em 0;
}

.markdown-content pre code {
    background: none;
    padding: 0;
}

.markdown-content table {
    border-collapse: collapse;
    width: 100%;
    margin: 1em 0;
}

.markdown-content th,
.markdown-content td {
    border: 1px solid #ddd;
    padding: 0.5em;
    text-align: left;
}

.markdown-content th {
    background-color: #f5f5f5;
    font-weight: 600;
}

.markdown-content a {
    color: #409EFF;
    text-decoration: none;
}

.markdown-content a:hover {
    text-decoration: underline;
}

.markdown-content img {
    max-width: 100%;
    height: auto;
    border-radius: 4px;
    margin: 0.5em 0;
}

/* 图片容器样式 */
.markdown-content .image-container {
    margin: 1em 0;
    text-align: center;
}

.markdown-content .chat-image {
    max-width: 88%;
    height: auto;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    border: 1px solid #e0e0e0;
}

/* 确保消息气泡内的 markdown 内容样式正确 */
.ai-bubble .markdown-content {
    color: #333 !important;
}

.user-bubble .markdown-content {
    color: white !important;
}

/* 代码块语法高亮（可选） */
.markdown-content .hljs {
    border-radius: 4px;
}

/*1*/
/* 气泡基础样式 */
.message-bubble {
    max-width: 88%;
    padding: 12px 16px;
    border-radius: 18px;
    word-break: break-word;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
    position: relative;
}

.ai-bubble {
    background-color: #f0f0f0;
    color: #333;
    border-bottom-left-radius: 4px;
}

/* Markdown内容包装器 */
.markdown-content-wrapper {
    max-width: 100%;
    overflow: hidden;
}

/* Markdown内容样式 */
.markdown-content {
    max-width: 100%;
    overflow-wrap: break-word;
    word-wrap: break-word;
    hyphens: auto;
}

/* 处理表格溢出 */
.markdown-content table {
    max-width: 100%;
    display: block;
    overflow-x: auto;
    white-space: nowrap;
}

.markdown-content th,
.markdown-content td {
    white-space: nowrap;
    min-width: 50px;
}

/* 处理代码块 */
.markdown-content pre {
    max-width: 100%;
    overflow-x: auto;
    white-space: pre-wrap;
}

.markdown-content code {
    white-space: pre-wrap;
    word-break: break-all;
}

/* 处理图片 */
.markdown-content img {
    max-width: 100%;
    height: auto;
}

/* 处理长URL链接 */
.markdown-content a {
    word-break: break-all;
}

/* 处理列表 */
.markdown-content ul,
.markdown-content ol {
    max-width: 100%;
    overflow: hidden;
}

/* 响应式设计 */
@media (max-width: 768px) {
    .message-bubble {
        max-width: 95%;
        padding: 10px 12px;
    }

    .markdown-content {
        font-size: 14px;
    }
}

/* 防止数学公式溢出 */
.markdown-content :deep(.math) {
    max-width: 100%;
    overflow-x: auto;
}

/* 处理块引用 */
.markdown-content blockquote {
    max-width: 100%;
    overflow: hidden;
}
</style>
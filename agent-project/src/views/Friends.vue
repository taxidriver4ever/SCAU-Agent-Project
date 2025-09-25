<template>
    <div>
        <ElContainer>
            <ElAside
                style="width: 8vh; display: flex; flex-direction: column; align-items: center;background-color: rgb(20, 20, 20);height: 100vh;">
                <ElMain style="padding: 0;">
                    <div style="display: flex; height: 15px; width: 35px; align-items: center; margin-top: 40px;">
                        <ElPopover popper-style="border:0;"
                        width="170px" :title="userName" :content="userEmail" placement="right">
                            <template #reference>
                                <ElAvatar style="height: auto; width: auto;" shape="square" :src="avatar"></ElAvatar>
                            </template>
                        </ElPopover>
                    </div>
                    <div style="margin-top: 40px;">
                        <ElButton type="text" v-if="featureSelected === 'chat'"
                            style="color: white; width: 40px;height: 40px;">
                            <ElIcon size="27">
                                <ChatLineRound />
                            </ElIcon>
                        </ElButton>
                        <ElButton type="text" v-else class="features-button" @click="showChat">
                            <ElIcon size="27">
                                <ChatRound />
                            </ElIcon>
                        </ElButton>
                    </div>
                    <div style="margin-top: 20px;">
                        <ElButton type="text" v-if="featureSelected === 'friends'"
                            style="color: white; width: 40px;height: 40px;">
                            <ElIcon size="27">
                                <UserFilled />
                            </ElIcon>
                        </ElButton>
                        <ElButton type="text" v-else class="features-button" @click="showFriends">
                            <ElIcon size="27">
                                <User />
                            </ElIcon>
                        </ElButton>
                    </div>
                </ElMain>
                <ElFooter style="padding: 0;">
                    <ElButton type="text" class="exit-button" @click="BackToHome">
                        <ElIcon size="27">
                            <Back />
                        </ElIcon>
                    </ElButton>
                </ElFooter>
            </ElAside>
            <ElContainer>
                <RouterView></RouterView>
            </ElContainer>
        </ElContainer>
    </div>
</template>

<script setup lang="ts">
import { ElMessage, ElAside, ElAvatar, ElButton, ElContainer, ElDivider, ElFooter, ElHeader, ElIcon, ElInput, ElMain, ElImage, ElTooltip, ElPopper, ElPopover } from 'element-plus';
import { ChatRound, ChatLineRound, User, UserFilled, Back, Plus, Search, PictureFilled, Timer } from '@element-plus/icons-vue'
import { onMounted, ref, onUnmounted } from 'vue';
import axios from 'axios';

// 状态管理
const featureSelected = ref(localStorage.getItem("featureSelected")?.toString()); // 当前选中的功能模块
const userName = ref("")
const userEmail = ref("")
const avatar = ref("/src/images/039-coconutdrink.svg")

// 显示聊天界面
async function showChat() {
    window.location.href = "/Friends/Session/Nothing"
    localStorage.setItem("featureSelected", "chat")
}

// 显示好友界面
async function showFriends() {
    window.location.href = "/Friends/Contact/Nothing"
    localStorage.setItem("featureSelected", "friends")
}

// 返回首页
function BackToHome() {
    window.location.href = "/";
}

async function GetUserInformation() {
    await axios({
        url: "http://127.0.0.1:8080/chat/GetUserInformation",
        method: "POST",
        headers: {
            "id": localStorage.getItem("id"),
            "uuid": localStorage.getItem("uuid")
        },
        data: {
            "id": localStorage.getItem("id")
        }
    }).then(res => {
        userName.value = res.data.data.userName;
        if(userName.value.length >= 8)
            userName.value = userName.value.substring(0,6) + "...";
        userEmail.value = res.data.data.userEmail;
    }).catch(e => {
        ElMessage.error(e)
    })
}

onMounted(() => {
    GetUserInformation();
})
</script>

<style lang="css" scoped>
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

.exit-button {
    color: white;
}

.exit-button:hover {
    color: rgb(180, 180, 180) !important;
}

.el-popover {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
  border-radius: 8px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
  width: 250px;
}
</style>
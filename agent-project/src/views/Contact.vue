<template>
    <ElAside
        style="display: flex;flex-direction: column;  width: 32vh; height: 100vh; background-color: rgb(40, 40, 40);">
        <ElHeader style="display: flex; height: 8vh; align-items: center; flex-direction: row;">
            <div style="display: flex;">
                <ElInput placeholder="search" class="chat-search-all" :prefix-icon="Search"></ElInput>
                <div>&nbsp;&nbsp;</div>
                <ElButton class="add-all" @click="ToShowAddFriends">
                    <ElIcon>
                        <Plus />
                    </ElIcon>
                </ElButton>
            </div>
        </ElHeader>
        <ElDivider style="margin: 0; border-color: rgb(60, 60, 60); padding: 0; ">
        </ElDivider>
        <ElMain style="margin: 0; padding: 0;">
            <ElMenu class="friends-menu">
                <ElSubMenu index="1">
                    <template #title>
                        <div style="color: white;">
                            New friends
                        </div>
                    </template>
                    <ElMenuItem class="new-friends" v-for="(newFriend, index) in newFriends" :key="index">
                        <div v-if="newFriend.userName.length >= 8">
                            {{ newFriend.userName.substring(0, 6) + "..." }}
                        </div>
                        <div v-else>
                            {{ newFriend.userName }}
                        </div>
                        <div v-if="newFriend.status === '0'"
                            style="display: flex;">
                            <ElButton @click="AcceptRequest(index)" class="accept-button">Accept</ElButton>
                        </div>
                        <div v-else-if="newFriend.status === '1'" style="font-size: 10px;background-color: rgb(60, 60, 60);display: flex; margin-left: 50px; height: 60%; align-items: center;">
                            <h2 style="margin: 0 5px;">Accepted</h2>
                        </div>
                        <div v-else-if="newFriend.status === '2'" style="font-size: 10px;   background-color: rgb(60, 60, 60);display: flex; margin-left: 50px; height: 60%; align-items: center;">
                            <h2 style="margin: 0 10px;">Expired</h2>
                        </div>
                    </ElMenuItem>
                </ElSubMenu>
                <ElSubMenu index="2" class="address-book">
                    <template #title>
                        <div style="color: white;">
                            Address Book
                        </div>
                    </template>
                    <ElMenuItem class="chat-history" @click="ShowInformation(index)"
                        v-for="(historySession, index) in historySessions" :key="index">
                        <!-- <div style="display: flex; margin-right: 20px; width: 33px">
                            <ElImage src="/src/images/050-lemonjuice.svg"></ElImage>
                        </div> -->
                        <div style="display: flex; flex-direction: column; justify-items: center;
                align-items: center;  justify-content: center; align-content: center;">
                            <div style="display: flex; font-weight: 600; font-size: 15px; color: white; margin-bottom: 5px;"
                                v-if="historySession.receiverName.length >= 8">
                                {{ historySession.receiverName.substring(0, 6) + "..." }}</div>
                            <div style="display: flex; font-weight: 600; font-size: 15px; color: white; margin-bottom: 5px;"
                                v-else>
                                {{ historySession.receiverName}}</div>
                        </div>
                    </ElMenuItem>
                </ElSubMenu>
            </ElMenu>
        </ElMain>
        <ElDialog
            style="display: flex; flex-direction: column; width: 50vh; ;height: 60vh; color: white; align-items: center; background-color: rgb(30, 30, 30);"
            v-model="ShowAddFriends">
            <div style="display: flex; color: white; justify-content: center;">Add friends</div>
            <br></br>
            <div style="display: flex; flex-direction: row;align-items: center;">
                <ElInput style="width: 35vh; margin-right: 10px;" placeholder="Please enter your email address"
                    v-model="InputEmail"></ElInput>
                <ElButton @click="SelectUser" type="info">
                    <ElIcon size="18">
                        <Search />
                    </ElIcon>
                </ElButton>
            </div>
            <div v-if="userInformation.status !== '-1' && userInformation.status !== ''">
                <br></br>
                <br></br>
                <div
                    style="display: flex; align-items: center; justify-content: center; background-color: rgb(25,25,25); flex-direction: row;">
                    <div style=" display: flex;width: 50px; height: 50px; margin-right: 30px;margin-top: 50px; margin-bottom: 50px;"
                        v-if="userInformation.avatar !== ''">
                        <ElImage src="/src/images/007-gintonic.svg"></ElImage>
                        &nbsp;&nbsp;
                    </div>
                    <div style="display: flex; margin-top: 50px; margin-bottom: 50px;">
                        {{ userInformation.userName }}
                    </div>
                </div>
                <br></br>
                <div v-if="userInformation.status === '0'"
                    style="display: flex; align-items: center; justify-content: center;">
                    <ElButton type="info" @click="SendFriendRequest" :disabled="RequestIsClicked">Send a friend request</ElButton>
                </div>
            </div>
            <div v-else-if="userInformation.status === '-1'"
                style="display: flex; padding: auto; margin-top: 17vh; justify-content: center; font-size: 20px;">
                The search result is empty
            </div>
        </ElDialog>
    </ElAside>
    <RouterView></RouterView>
</template>

<script setup lang="ts">
import { ElAside, ElButton, ElDivider, ElHeader, ElIcon, ElInput, ElMain, ElImage, ElMessage, ElDialog, ElMenu, ElMenuItem, ElSubMenu } from 'element-plus';
import { Search, Plus, User } from '@element-plus/icons-vue';
import axios from 'axios';
import { onMounted, ref } from 'vue';

class UserInformation {
    userName: string;
    avatar: string;
    status: string;

    constructor(userName: string, status: string, avatar: string) {
        this.userName = userName;
        this.status = status;
        this.avatar = avatar;
    }
}

const historySessions = ref<HistorySession[]>([])
const userInformation = ref<UserInformation>(new UserInformation("", "", ""))
const ShowAddFriends = ref(false)
const InputEmail = ref("")
const newFriends = ref<UserInformation[]>([])
const RequestIsClicked = ref(false)

class HistorySession {
    updatedTime: string;
    receiverId: string;
    avatar: string;
    receiverName: string

    constructor(receiverName: string, updatedTime: string, receiverId: string, avatar: string) {
        this.updatedTime = updatedTime;
        this.receiverId = receiverId;
        this.avatar = avatar;
        this.receiverName = receiverName;
    }
}

async function ToShowAddFriends() {
    ShowAddFriends.value = true;
    userInformation.value = new UserInformation("", "", "");
    RequestIsClicked.value = false;
}

async function ShowInformation(index: number) {
    // localStorage.setItem("receiverId",historySessions.value[index].receiverId)
    // localStorage.setItem("receiverName",historySessions.value[index].receiverName)
    // window.location.href = "/Friends/Session/Chat"
}

async function SelectUser() {
    await axios({
        url: "http://127.0.0.1:8080/chat/SelectUser",
        method: "POST",
        headers: {
            'id': localStorage.getItem("id"),
            'uuid': localStorage.getItem("uuid")
        },
        data: {
            id: Number(localStorage.getItem("id")),
            userEmail: InputEmail.value
        }
    }).then(res => {
        if (res.data.code === 200) {
            userInformation.value = new UserInformation(
                res.data.data.userName, res.data.data.status, res.data.data.avatar);
        }
        else userInformation.value.status = '-1'
    }).catch(e => {
        ElMessage.error(e)
    })
}

async function SendFriendRequest() {
    await axios({
        url: "http://127.0.0.1:8080/chat/SendFriendRequest",
        method: "POST",
        headers: {
            'id': localStorage.getItem("id"),
            'uuid': localStorage.getItem("uuid")
        },
        data: {
            id: Number(localStorage.getItem("id")),
            userEmail: InputEmail.value
        }
    }).then(res => {
        if (res.data.code === 200) {
            RequestIsClicked.value = true;
            userInformation.value.status = '0'
        }
        else {
            ElMessage.error(res.data.message)
        }
    }).catch(e => {
        ElMessage.error(e)
    })
}

async function GetNewFriendsInformation() {
    await axios({
        url: "http://127.0.0.1:8080/chat/GetNewFriendsInformation",
        method: "POST",
        headers: {
            "id": localStorage.getItem("id"),
            "uuid": localStorage.getItem("uuid")
        },
        data: {
            id: localStorage.getItem("id")
        }
    }).then(res => {
        if (res.data.code === 200) {
            newFriends.value = res.data.data.map((item: any) =>
                new UserInformation(
                    item.userName,
                    item.status,
                    item.avatar
                )
            );
        }
        else ElMessage.error(res.data.message)
    }).catch(e => {
        ElMessage.error(e)
    })
}

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

async function AcceptRequest(index : number) {
    await axios({
        url: "http://127.0.0.1:8080/chat/AcceptRequest",
        method: "POST",
        headers: {
            "id": localStorage.getItem("id"),
            "uuid": localStorage.getItem("uuid")
        },
        data: {
            id: localStorage.getItem("id"),
            index: index
        }
    }).then(res => {
        if (res.data.code === 200) 
            newFriends.value[index].status = "1"
        else ElMessage.error(res.data.message)
    }).catch(e => {
        ElMessage.error(e)
    })
}

onMounted(() => {
    SelectSessions();
    GetNewFriendsInformation();
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

.add-all {
    height: 27px;
    width: 27px;
    background-color: rgb(55, 55, 55);
    color: rgb(90, 90, 90);
    border: 0;
}

.add-all:hover {
    background-color: rgb(65, 65, 65);
    color: rgb(90, 90, 90);
    height: 27px;
    width: 27px;
}

.chat-history {
    flex-direction: row;
    display: flex;
    width: 32vh;
    height: 6vh;
    border-radius: 0;
    background-color: rgb(40, 40, 40);
    border: 0;
}

.chat-history:hover {
    display: flex;
    width: 32vh;
    height: 6vh;
    border-radius: 0;
    background-color: rgb(60, 60, 60);
    border: 0;
}

.el-sub-menu {
    background-color: rgb(40, 40, 40);
    display: flex;
    flex-direction: column;
    border-color: #1890ff !important;
}


/* 使用 :deep() 来深度选择子组件内的元素 */
:deep(.el-sub-menu) .el-sub-menu__title:hover {
    background-color: rgb(40, 40, 40) !important;
}

:deep(.el-sub-menu) .el-sub-menu__title:hover .el-sub-menu__icon-arrow {
    color: white !important;
}

:deep(.el-sub-menu) .el-sub-menu__title .el-sub-menu__icon-arrow {
    color: rgb(40, 40, 40) !important;
}


.friends-menu {
    display: flex;
    flex-direction: column;
}

.new-friends {
    display: flex;
    justify-content: flex-end;
    color: white;
    background-color: rgb(40, 40, 40);
    flex-direction: row;
}

.new-friends:hover {
    color: white;
    background-color: rgb(60, 60, 60);
}

.accept-button {
    display: flex;
    margin-left: 50px;
    justify-content: flex-end;
    justify-items: flex-end;
    align-items: flex-end;
    align-content: flex-end;
    background-color: rgb(100, 100, 100);
    color: white;
    border: 0;
}

.accept-button:hover {
    display: flex;
    margin-left: 50px;
    background-color: white;
    color: black;
}
</style>
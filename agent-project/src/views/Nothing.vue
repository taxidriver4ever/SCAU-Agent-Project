<template>
    <div style="background-color: rgb(25,25,25); height: 100%; width: 100%;" >
    </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
onMounted(()=>{
    localStorage.removeItem("receiverId")
    localStorage.removeItem("receiverName")
})

</script>

<style lang="css" scoped>
</style>
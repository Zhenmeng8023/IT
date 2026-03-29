<template>
    <el-container class="app">
        <el-header>
            <el-page-header @back="goBack" content="">
            </el-page-header>
        </el-header>
        <el-container>
            <el-aside width="300px" class="history-aside">
                <h2>历史记录</h2><br>
                <!-- 个人介绍开始 -->
                <p>昵称：  {{username}}</p><br>
                <p>生日：  {{formatDate(userbrithday)}}</p><br>
                <p>邮箱：  {{useremail}}</p><br>
                <p>联系地址：  {{useraddress}}</p><br>
                <p>性别：  {{usersex}}</p><br>
                <p>标签：  {{userbog.join('、')}}</p><br>
                <p>签名：  {{usersign}}</p><br>
                <!-- 个人介绍结束 -->
            </el-aside>
            <el-main class="history-main">
                <el-table :data="historyList" style="width: 100%">
                    <el-table-column prop="zuozhe" label="作者" width="200">
                    </el-table-column>
                    <el-table-column prop="title" label="标题">
                    </el-table-column>
                    <el-table-column prop="pic"  width="100">
                        <template slot-scope="scope">
                            <el-image :src="scope.row.pic" fit="fill" :preview-src-list="[scope.row.pic]" />
                        </template>
                    </el-table-column>
                </el-table>
            </el-main>
        </el-container>
    </el-container>
    <!-- <el-empty description="暂无历史记录">先阅读一些博客吧</el-empty> -->
</template>

<script>
export default {
    layout: 'default',
    data() {
        return {
            historyList: [
                {
                    zuozhe: '历史记录1的博客作者',
                    title: '这是历史记录1博客的页面跳转',
                    pic:'/pic/choubi.jpg'
                },
                {
                    zuozhe: '历史记录2的博客作者',
                    title: '这是历史记录2博客的页面跳转',
                    pic:'/pic/choubi.jpg'
                }
            ],
            username:'',          //昵称
            userbrithday: '',     //生日
            useremail: '',        //邮箱
            useraddress: '',      //联系地址
            usersex: '',          //性别
            userbog:[],           //标签
            usersign: '',         //签名
        }
    },
    methods: {
        formatDate(date) {
        if (!date) return '';
        const d = new Date(date);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${year}年${month}月${day}日`;
        },
        goBack() {
            this.$router.push('/user');
        }
    },
    beforeRouteLeave(to, from, next) {
        // 在离开页面前恢复body默认样式
        document.body.style.overflow = '';
        next();
    },
    destroyed() {
        // 组件销毁时确保恢复body样式
        document.body.style.overflow = '';
    }
}
</script>

<style scoped>

</style>
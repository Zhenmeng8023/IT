<template>
    <el-container class="app">
        <el-header>
            <el-page-header @back="goBack" content="">
            </el-page-header>
        </el-header>
        <el-container>
            <el-aside width="300px">
                历史记录
                <!-- 个人介绍开始 -->
                <el-descriptions class="margin-top" title="" :column="4" direction="vertical">
                    <el-descriptions-item label="用户名">kooriookami</el-descriptions-item>
                    <el-descriptions-item label="手机号">18100000000</el-descriptions-item>
                    <el-descriptions-item label="居住地">苏州市</el-descriptions-item>
                    <el-descriptions-item label="联系地址">江苏省苏州市吴中区吴中大道 1188 号</el-descriptions-item>
                </el-descriptions>
                <!-- 个人介绍结束 -->
            </el-aside>
            <el-main>
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
            ]
        }
    },
    methods: {
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
/* 局部样式控制 */
.app {
    margin: 0;
    padding: 0;
    min-height: 100vh;
    overflow: hidden; /* 将overflow: hidden限制在当前组件内部 */
}

.app {
  height: 100vh;
  width: 100vw;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.el-header {
  background-color: #000000;
  color: #ffffff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  padding: 0 20px;
}

.el-container {
  flex: 1;
  height: calc(100vh - 60px);
}

.el-aside {
  background-color: #D3DCE6;
  color: #333;
  padding: 20px;
  overflow-y: auto;
}

.el-main {
  background-color: #E9EEF3;
  color: #333;
  padding: 20px;
  overflow-y: auto;
}

.el-table {
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.margin-top {
  margin-top: 20px;
}
</style>
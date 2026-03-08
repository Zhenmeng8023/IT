// <template>
  <el-container class="app">

  <el-header>Header</el-header>

    <el-container>
        <!-- 侧边栏导航 start-->
        <el-aside width="200px">
          <!-- 页面刷新时，根据当前路由路径设置默认激活项 -->
          <el-menu
            :default-active="$route.path"
            class="el-menu-vertical-demo"
            router
            background-color="#545c64"
            text-color="#fff"
            active-text-color="#ffd04b"> 
            <el-menu-item index="/">
              <i class="el-icon-menu"></i>
              <span slot="title">首页</span>
            </el-menu-item>
            <el-submenu index="/teacher">
              <template slot="title">
                <i class="el-icon-s-management"></i>
                <span>系统管理</span>
              </template>
                <el-menu-item index="/teacher/teacherlist">老师列表</el-menu-item>
                <el-menu-item index="/teacher/teacheradd">添加老师</el-menu-item>
            </el-submenu>
            <el-submenu index="/vod">
                <template slot="title">
                  <i class="el-icon-video-camera-solid"></i>
                  <span>管理</span>
                </template>
                  <el-menu-item index="/vod/vodlist">vod视频列表</el-menu-item>
                  <el-menu-item index="/vod/vodadd">添加vod视频</el-menu-item>
            </el-submenu>
          </el-menu>
        </el-aside>
    <el-container>
        <!-- 侧边栏导航 end -->
      <el-main>
        <!-- 路由视图 -->
        <nuxt/>
      </el-main>

      <el-footer>Footer</el-footer>

    </el-container>
  </el-container>
</el-container>
</template>

<script>
export default {

}
</script>

<style>
 html, body, #__nuxt, #__layout, .app {
    height: 100%;
    }

.el-header, .el-footer {
    background-color: #B3C0D1;
    color: #333;
    text-align: center;
    line-height: 60px;
  }
  
  .el-aside {
    background-color: #D3DCE6;
    color: #333;
    line-height: 200px;
  }
  
  .el-main {
    background-color: #E9EEF3;
    color: #333;
  }

</style>
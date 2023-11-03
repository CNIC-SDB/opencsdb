<template>
  <the-header/>
  <router-view/>
</template>

<style lang="scss">
  *{
    padding: 0;
    margin: 0;
    list-style: none;
  }
  html {
    // filter: progid:DXImageTransform.Microsoft.BasicImage(grayscale=1);
    // -webkit-filter: grayscale(100%);
  }
  body,html{
    width: 100%;
    height: 100%;
    margin: 0;
    padding: 0;
    font-family: Helvetica Neue, Helvetica, PingFang SC, Hiragino Sans GB, Microsoft YaHei, SimSun, sans-serif;
    -webkit-font-smoothing: antialiased;
    background: #F5F7F9;
  }
  #app {
    position: absolute;
    top: 0;
    bottom: 0;
    width: 100%;
    height: 100%;
    // overflow: hidden;
  }
  section{
    padding: 0 10rem;
  }
  .breadcrumb{
    background-color: #fff;
    height: 45px;
    font-size: 14px;
    box-shadow: 0px 2px 4px 0px rgba(0, 0, 0, 0.1);
    .el-breadcrumb{
      padding: 0 360px;
      line-height: 45px;
    }
  }
  .el-submenu [class^=fa] {
    vertical-align: baseline;
    margin-right: 0.5rem;
  }
  .el-menu-item [class^=fa] {
    vertical-align: baseline;
    margin-right: 0.5rem;
  }
  .toolbar {
    background: #f2f2f2;
    padding: 0.5rem;
    margin: 0.5rem 0;
    .el-form-item {
      margin-bottom: 0.5rem;
    }
  }
  .fade-enter-active, .fade-leave-active {
    transition: all .2s ease;
  }
  .fade-enter, .fade-leave-active {
    opacity: 0;
  }
  .header .el-menu--horizontal > .el-menu-item:hover,
  .header .el-menu--horizontal > .el-submenu.is-active .el-submenu__title,
  .header .el-menu--horizontal > .el-submenu:hover .el-submenu__title {
    border-bottom: 0.1rem solid #1d8ce0;
  }
  body .el-table th.gutter{
    display: table-cell!important;
  }
  body .el-table--boder th.gutter:last-of-type {
    display: block !important
  }
  .el-table {
    th, td {
      font-size: 14px;
    }
  }
  ::-webkit-scrollbar {/*滚动条整体样式*/
    width: 0.25rem;     /*高宽分别对应横竖滚动条的尺寸*/
    height: 0.05rem;
  }
  ::-webkit-scrollbar-thumb {/*滚动条里面小方块*/
    border-radius: 0.25rem;
    -webkit-box-shadow: inset 0 0 0.25rem rgba(0,0,0,0.2);
    background: #c0c4cc;
  }
  ::-webkit-scrollbar-track {/*滚动条里面轨道*/
    -webkit-box-shadow: inset 0 0 0.25rem rgba(0,0,0,0.2);
    border-radius: 0.25rem;
    background: #ebeef5;
  }
</style>
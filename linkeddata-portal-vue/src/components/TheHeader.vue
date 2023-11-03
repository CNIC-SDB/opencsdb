<template>
  <header>
    <h1 @click="goHome">SemWeb-Cloud</h1>
    <el-button @click="goResource" type="primary">Browse</el-button>
  </header>
</template>

<script>
export default {
  name: 'theHeader',
  methods: {
    goHome() {
      this.$router.push('/')
    },
    goResource() {
      this.$router.push('/resource')
    }
  }
}
</script>

<style lang="scss" scoped>
  header{
    height: 5rem;
    background-color: #3897F1;
    padding: 0 10rem;
    display: flex;
    align-items: center;
    justify-content: space-between;
    h1{
      cursor: pointer;
      user-select:none;
      color: #fff;
    }
    .el-button{
      margin-right: 20rem;
      font-size: 1.5rem;
      padding: 1.4rem 1.6rem;
      border-radius: .5rem;
    }
  }
</style>
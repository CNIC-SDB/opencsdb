<template>
  <footer>
    <div class="footer-body">
			Copyright @ 2015-2022      Computer Network Information Center, Chinese Academy of Sciences. All Rights Reserved
		</div>
  </footer>
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
  footer{
    background: url(../assets/background-img.png) repeat-x;
		background-size: 100% 100%;
    height: 5rem;
    color: #4a4747;
    .footer-body{
      line-height: 5rem;
      text-align: center;
      user-select:none
    }
  }
</style>
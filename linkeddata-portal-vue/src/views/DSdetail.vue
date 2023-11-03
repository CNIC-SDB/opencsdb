<template>
  <section>
    <main>
      <div class="content">
        <h1>{{detailData.title}}</h1>
        <!-- About this Dataset -->
        <div class="describe-box">
          <p class="describe">
            <span class="label">About this dataset</span>
          </p>
          <p class="message">{{description.en}}</p>
        </div>
        <!-- 搜索框 -->
        <div class="search">
          <el-input v-model="searchText" placeholder="Search data in the dataset ..." />
          <el-button type="primary" @click="SearchList()">Search</el-button>
        </div>
        <!--  -->
        <div class="list">
          <div class="list-item">
            <!-- keyword -->
            <div class="item-keyWord">
              <span class="item-label">Keyword</span>
              <p class="item-msg"><span class="key-word-key" v-for="(item, key) in detailData.keywords" :key="key">{{item}}</span></p>
            </div>
            <!-- ContactPerson -->
            <div class="item-keyWord">
              <span class="item-label">Contact Person</span>
              <p class="item-msg"><span>{{detailData.contactName}}</span></p>
            </div>
            <!-- keyword -->
            <div class="item-keyWord">
              <span class="item-label">Email</span>
              <p class="item-msg"><a :href="'mailto:' + detailData.contactEmail">{{detailData.contactEmail}}</a></p>
            </div>
            <!-- keyword -->
            <div class="item-keyWord">
              <span class="item-label">SPARQL Endpoints</span>
              <p class="item-msg"><a class="forbid" target="_blank" @click="goLink(detailData.sparql)" href="./yasgui.html">{{detailData.sparql}}</a></p>
            </div>
            <!-- keyword -->
            <div class="item-keyWord">
              <span class="item-label">Download metadata</span>
              <div class="item-msg load-body">
                <p class="load"><a target="downloadFile" :href="'download/' + detailData.identifier + '/' +detailData.identifier + '.jsonld'">JSON-LD</a></p>
                <p class="load"><a target="downloadFile" :href="'download/' + detailData.identifier + '/' +detailData.identifier + '.rdf'">RDF/XML</a></p>
                <p class="load"><a target="downloadFile" :href="'download/' + detailData.identifier + '/' + detailData.identifier + '.ttl'">Turtle</a></p>
                <p class="load"><a target="downloadFile" :href="'download/' + detailData.identifier + '/' + detailData.identifier + '.nt'">N-Triples</a></p>
              </div>
            </div>
            <!-- keyword -->
            <div class="item-keyWord">
              <span class="item-label">Download Data</span>
              <div class="item-msg load-body">
                <p class="load"><a target="downloadFile" :href="'download/' + detailData.identifier + '/' +detailData.identifier + '.jsonld'">download</a></p>
              </div>
            </div>
            <!-- keyword -->
            <div class="item-keyWord">
              <span class="item-label">Links</span>
              <el-table :data="detailData.links" stripe border style="width: 50%">
                <el-table-column prop="target"  align="center" label="Links to"/>
                <el-table-column prop="value"  align="center" label="Total size"/>
              </el-table>
            </div>
            <!-- 图表 -->
            <div class="item-keyWord">
              <span class="item-label">Graph</span>
              <div class="item-msg">
                <div ref="chart" class="chart" />
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="org-detail">
        <div class="org-imgbody">
          <img class="org-img" :src="detailData.image" alt="">
        </div>
        <div class="org-content">
          <p class="label">Institution</p>
          <p class="label-key"><a :href="detailData.website" target="_blank" rel="noopener noreferrer">{{detailData.unitName}}</a></p>
        </div>
        <div class="org-content">
          <p class="label">Publish date</p>
          <p class="label-key">{{$TOOL.dateFormat(detailData.publishTime)}}</p>
        </div>
      </div>
    </main>
  </section>
  <the-footer/>
</template>

<script>
import * as echarts from 'echarts';
export default {
  name: 'DSdetail',
  data() {
    return {
      identifier: this.$route.query.identifier,
      searchText: '',
      detailData: {},
      description: {}
    }
  },
  watch: {
    '$route' (to, from) { 
      if(to.query.identifier != from.query.identifier){
        this.identifier = to.query.identifier; // 把最新id赋值给定义在data中的id
        this.getList(); // 重新调用加载数据方法
        this.renderChart()
      }
    }
  },
  mounted() {
    this.getList()
    this.renderChart()
    const onWinResize = () => {
      this.chart && this.chart.resize()
    }
    window.addEventListener('resize', onWinResize)
    // const that = this
    this.chart.on('click', function (params) {
      window.open(params.data.url)
      // that.$router.push({ path: '/DS', query: {identifier: params.value} })
    });
  },
  methods: {
    async getList() {
      await this.$http(`/dataset/getDatasetByIdentifier?identifier=${this.identifier}`).then(res => {
        if (res.code == 200) {
          this.detailData = res.data
          this.description = res.data.description
        } else {
          this.$message.error(res.message)
        }
      })
    },
    goLink(item) {
      localStorage.setItem('link', item)
    },
    SearchList() {
      this.$router.push({ path: '/resource', query: { text: this.searchText, title: this.detailData.title, id: this.detailData.id } })
    },
    // 排序
    compare(property) {
      return function (a, b) {
        var value1 = a[property];
        var value2 = b[property];
        return value1 - value2;
      }
    },
    // 图表
    async renderChart () {
      if (!this.chart) {
        this.chart = echarts.init(this.$refs.chart)
      }
      this.chart.showLoading()
      try {
        let chartList = []
        await this.$http(`/dataset/listRelationDataset?identifier=${this.identifier}`).then(_ => {
          chartList = _
        })
        let data = chartList.nodes;
        let link = chartList.links;
        let categories = chartList.categories
        const sortList = data.sort(this.compare("triples"))
        const linkList = link.sort(this.compare("value"))
        sortList.forEach((node, i) => {
          if (linkList.length != 0) {
            if (node.title == linkList[0].source) {
              node.x = 100
              node.y = 100
            } else {
              node.x = 100 + Math.cos(Math.PI / 180 + Math.PI * i /( data.length / 2 ) ) * (50 + i % 2) + (i + 1 * 3)
              node.y = 100 - Math.sin(Math.PI / 180 + Math.PI * i /( data.length / 2 ) ) * (50 + i % 2) + (i + 1 * 3)
            }
          } else {
            node.x = 100
            node.y = 100
          }
          node.label = {
            show: true,
            formatter:function(params){
              let str = params.data.title
              return str
            }
          };
          node.name = node.title
          node.value = node.id
          if (sortList.length == 1) {
            node.symbolSize = 200
          } else {
            node.symbolSize = (i + 1) * 20 > 250 ? 250 : (i + 1) * 20
          }
          
        });
        this.chart.setOption({
          animationDuration: 1500,
          series: [
            {
              type: 'graph',
              layout: 'none',
              data: sortList,
              links: linkList,
              categories: categories,
              roam: false,
              zoom: .7,
            }
          ]
        })
      } catch (e) { console.log(e) }
      this.chart.hideLoading()
    }
  }
}
</script>

<style lang="scss" scope>
  main{
    margin-top: 3rem;
    display: flex;
    .content{
      flex:1;
      padding: 0 4rem;
      width: 100%;
      h1{
        text-align: center;
        color: #3897F1;
        line-height: 3rem;
        border-bottom: 1px solid #d0d0d0;
        margin-bottom: 1rem;
      }
      // 顶部
      .describe-box{
        padding: 1rem 0;
        .describe{
          display: flex;
          .label{
            font-size: 1.2rem;
            font-weight: 600;
            color: #169BD5;
            width: 15rem;
          }
          .link{
            flex: 1;
            color: #AAAAAA;
          }
        }
        .message{
          font-size: 1.2rem;
          padding-top: 1rem;
          line-height: 1.6;
          color: rgba(127, 127, 127);
        }
      }
      // 搜索框
      .search{
        padding: 1rem 0;
        border-bottom: 1px solid #d0d0d0;
        display: flex;
        .el-input{
          height: 2.6rem;
        }
        .el-button{
          height: 2.6rem;
          font-size: 1.2rem;
          margin-left: 2rem;
        }
      }
      .list-item{
        margin-bottom: 5rem;
        width: 100%;
        overflow: hidden;
        .item-keyWord{
          margin-top: 2rem;
          display: flex;
          .item-label{
            font-size: 1.2rem;
            font-weight: 600;
            width: 15rem;
            color: #169BD5;
          }
          .item-msg{
            font-size: 1.2rem;
            color: #169BD5;
            flex: 1;
            a {
              color: #169BD5;
              text-decoration: none;
            }
            a:hover{
              text-decoration: underline;
              font-weight: bold;
            }
            span{
              color: rgba(127, 127, 127);
              margin-right: .5rem;
            }
            .key-word-key{
              padding: .2rem .6rem;
              border-radius: 4rem;
              background-color: #777777;
              color: #fff;
            }
          }
          .load-body{
            display: flex;
            .load{
              border: 1px solid #169BD5;
              padding: .2rem .6rem;
              border-radius: .4rem;
              margin-right: 1rem;
              a {
              color: #169BD5;
                text-decoration: none;
              }
              a:hover{
                text-decoration: none;
                font-weight: normal;
              }
            }
          }
        }
      }
    }
    .org-detail {
      width: 20rem;
      height: 40rem;
      overflow: hidden;
      .org-imgbody{
        width: 100%;
        height: 15rem;
        overflow: hidden;
        .org-img{
          width: 100%;
          transition: all .5s;
        }
        .org-img:hover{
          transform: scale(1.2,1.2);
        }
      }
      .org-content{
        margin-top: 2rem;
        display: flex;
        align-items: center;
        .label{
          color: #3897F1;
          font-weight: 600;
          font-size: 1.2rem;
        }
        .label-key{
          font-size: 1.2rem;
          color: rgba(127, 127, 127);
          margin-left: 2rem;
          a{
            text-decoration: none;
            color: #169BD5;
          }
          a:hover{
            font-weight: bold;
            text-decoration: underline;
          }
        }
      }
    }
  }
  .chart {
    height: 600px;
  }
</style>
<style>
  .el-table thead tr th {
    background-color: #3897F1 !important;
    color: #fff;
  }
</style>
<template>
  <section>
    <div class="search">
      <el-input v-model="searchText" placeholder="Search data ..."  clearable />
      <el-button type="primary" @click="goResource">Search</el-button>
    </div>
    <div class="chart-img">
      <div ref="chart" class="chart" />
    </div>
    <div class="list-content">
      <div class="filtrate">
        <div class="filter-select">
          <img src="../assets/filter.png">
          <el-select v-model="filterValue" class="m-2" placeholder="Select" @change="filterChange" :teleported="false">
            <el-option v-for="item in options" :key="item" :label="item" :value="item"/>
          </el-select>
        </div>
        <el-checkbox-group v-model="checkList" @change="getList()">
          <el-checkbox v-for="item in checkOption" :key="item" :label="item" />
        </el-checkbox-group>
      </div>
      <div class="list" v-loading="loading">
        <div class="list-item" v-for="item in dataList" :key="item.title">
          <div class="item-img" @click="goDetail(item)"><img :src="item.image" alt=""></div>
          <div class="item-content">
            <p class="item-title" @click="goDetail(item)">{{item.title}}</p>
            <p class="item-message" @click="goDetail(item)">{{item.description.en}}</p>
            <div class="item-bottom">
              <p><img src="../assets/org-icon.png" alt=""><a :href="item.website" target="_blank" rel="noopener noreferrer">{{item.unitName}}</a></p>
              <span v-time.dateFormat="item.publishTime"></span>
            </div>
          </div>
        </div>
        <el-empty v-if="dataList.length == 0" description="No data was found" />
        <el-pagination
          v-model:currentPage="currentPage"
          v-model:page-size="pageSize"
          layout="prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
  </section>
  <the-footer/>
</template>

<script>
import * as echarts from 'echarts';
export default {
  name: 'index',
  data() {
    return {
      searchText: '',
      filterValue: 'filter by domain',
      options: ['filter by domain', 'filter by institution'],
      checkList: [],
      checkOption: [],
      dataList: [],
      pageSize: 5,
      pageNum: 1,
      total: 0,
      currentPage: 1,
      loading: false
    }
  },
  mounted() {
    // this.getList()
    this.filterChange()
    this.renderChart()
    const onWinResize = () => {
      this.chart && this.chart.resize()
    }
    window.addEventListener('resize', onWinResize)
    const that = this
    this.chart.on('click', function (params) {
      // window.open(params.data.url)
      that.$router.push({ path: '/DS', query: {identifier: params.value} })
    });
  },
  methods: {
    // 搜索数据集
    goResource() {
      this.$router.push({ path: '/resource', query: { text: this.searchText }})
    },
    // 获取列表
    async getList() {
      this.loading = true
      const data = {
        condition: this.searchText,
        domain: this.filterValue == 'filter by domain' ? this.checkList : [],
        institution: this.filterValue == 'filter by institution' ? this.checkList : [],
        pageNum: this.pageNum,
        pageSize: this.pageSize
      }
      await this.$http.post(`/dataset/getDatasetList`, data).then(res => {
        if (res.code == 200) {
          this.dataList = res.data ? res.data.pageList : []
          this.total = res.data ? res.data.countNum : 0
          this.loading = false
        } else {
          this.$message.error(res.message)
          this.loading = false
        }
      })
    },
    // 更换筛选条件
    filterChange() {
      if (this.filterValue == 'filter by domain') {
        this.$http('/dataset/listDomains').then(res => {
          this.checkOption = res
          this.checkList = this.checkOption
          this.getList()
        })
      } else {
        this.$http('/dataset/listInstitutions').then(res => {
          this.checkOption = res
          this.checkList = this.checkOption
          this.getList()
        })
      }
      
    },
    // 跳转
    goDetail(item) {
      this.$router.push({ path: '/DS', query: {identifier: item.identifier} })
    },
    // 页码改变
    handleCurrentChange(val) {
      this.pageNum = val
      this.getList()
    },
    handleSizeChange() {
      this.getList()
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
        let chartList = null
        await this.$http(`/dataset/listAllDatasetRelation`).then(_ => {
          chartList = _
        })
        let data = chartList.nodes;
        let link = chartList.links;
        let categories = chartList.categories
        const sortList = data.sort(this.compare("triples"))
        const linkList = link.sort(this.compare("value"))
        sortList.forEach((node, i) => {
          if (node.name == linkList[0].source) {
            node.x = 100
            node.y = 100
          } else {
            node.x = 100 + Math.cos(Math.PI / 180 + Math.PI * i /( data.length / 2 ) ) * (50 + i % 2) + (i + 1 * 3)
            node.y = 100 - Math.sin(Math.PI / 180 + Math.PI * i /( data.length / 2 ) ) * (50 + i % 2) + (i + 1 * 3)            
          }          
          node.label = {
            show: true,
            formatter:function(params){
              let str = params.data.name
              return str
            }
          };
          node.value = node.id
          node.symbolSize = (i + 1) * 20 > 250 ? 250 : (i + 1) * 20
        });
        this.chart.setOption({
          legend: [{
              data: categories.map(function (a) {
                return a.name;
              }),
              left: '0',
              orient: 'vertical',
              textStyle:{
                fontSize: 17
              } 
            }
          ],
          animationDuration: 1500,
          series: [
            {
              type: 'graph',
              layout: 'none',
              data: sortList,
              links: linkList,
              categories: categories,
              roam: false,
              zoom: .9,
            }
          ]
        })
      } catch (e) { console.log(e) }
      this.chart.hideLoading()
    }
  }
}
</script>

<style lang="scss" scoped>
  .search{
    padding-top: 2rem;
    display: flex;
    .el-input{
      height: 3rem;
    }
    .el-button{
      height: 3rem;
      font-size: 1.2rem;
      margin-left: 2rem;
    }
  }
  .chart-img{
    padding-top: 2rem;
    width: 100%;
    img{
      // width: 100%;
      height: auto;
      margin-left: 50%;
      transform: translateX(-50%)
    }
  }
  .list-content{
    padding-top: 2rem;
    display: flex;
    .filtrate{
      width: 17rem;
      flex-shrink: 0;
      margin-right: 3rem;
      .filter-select{
        display: flex;
        align-items: center;
        margin-bottom: 1rem;
        img{
          width: 2rem;
          height: 2rem;
          margin-right: 1rem;
        }
      }
      .el-checkbox-group{
        padding: 1rem;
      }
      .el-checkbox{
        display: block;
        margin-bottom: .6rem;
      }
    }
    .list{
      flex: 1;
      .list-item{
        display: flex;
        margin-bottom: 1.5rem;
        cursor: pointer;
        min-height: 10rem;
        .item-img{
          width: 13rem;
          max-height: 13rem;
          overflow: hidden;
          margin-right: 2rem;
          img{
            width: 100%;
            display: block;
            transition: all .5s;
          }
          img:hover{
            transform: scale(1.2,1.2);
          }
        }
        .item-content{
          flex: 1;
          position: relative;
          .item-title{
            font-size: 1.4rem;
            margin-bottom: 0.4rem;
            font-weight: 600;
            line-height: 2rem;
            color: #3897F1;
          }
          .item-title:hover{
            color: rgb(2,125,180);
            // color: rgba(127, 127, 127);
          }
          .item-message{
            font-size: 1.2rem;
            line-height: 1.7;
            display: -webkit-box;
            -webkit-box-orient: vertical;
            -webkit-line-clamp: 3;
            overflow: hidden;
          }
          .item-message:hover{
            color: rgba(127, 127, 127);
          }
          .item-bottom{
            width: 100%;
            position: absolute;
            bottom: 0;
            display: flex;
            align-items: center;
            justify-content: space-between;
            p{
              display: flex;
              color: #02A7F0;
              font-size: 1.1rem;
              img{
                margin-right: .4rem;
                width: 1.2rem;
                height: 1.2rem;
              }
              a{
                text-decoration: none;
                color: #169BD5;
              }
              a:hover{
                font-weight: bold;
                text-decoration: underline;
              }
            }
            span{
              color: rgba(127, 127, 127);
            }
          }
        }
      }
      .el-pagination{
        display: flex;
        justify-content: center;
        margin: 2rem 0;
      }
    }
  }
  .chart {
    height: 40rem;
  }
</style>
<style>
.el-checkbox__label{
  font-size: 1.1rem !important;
  width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
<template>
  <section>
    <main>
      <div class="content">
        <!-- 顶部标题 -->
        <div class="header">
          <h2>{{dataList.title}}</h2>
          <a>{{dataList.subjectShort}}</a>
        </div>
        <div class="placeholder-body"  v-loading="loading" v-show="itemList.length == 0"></div>
        <!-- label -->
          <div class="label-list" v-for="content in itemList" :key="content.sparql">
            <span class="set-name" v-if="content.dataSetName">{{content.dataSetName}}</span>
            <div v-for="(item, index) in content.detailLines" :key="index">
              <div  class="label-item" v-if="item.shortPre">
                <div class="label">
                  <p class="title"><a :href="item.predicate" target="_blank">{{item.shortPre}}</a></p>
                  <p class="title-det" v-if="item.preLabel" style="margin-left:.4rem;margin-right:2rem;">({{item.preLabel}})</p>
                </div>
                <div class="label-msg" v-for="msg in item.contents" :key="msg">
                  <p v-if="msg.label"><span style="color: #555;">{{msg.label}}</span> <span v-if="msg.language" style="margin-left:.4rem;margin-right:2rem;">({{msg.language}})</span></p>
                  <p v-if="msg.iriShort"><a :href="msg.iri" target="_blank" rel="noopener noreferrer">{{msg.iriShort}}</a><span style="margin-left:.4rem;margin-right:2rem;" v-if="msg.iriLabel">({{msg.iriLabel}})</span></p>
                  <div v-if="msg.objects">
                    <div class="label-objects" v-for="detail in msg.objects" :key="detail" :style="msg.iriShort ? 'margin-left: 3rem' : '' ">
                      <p>
                        <a style="margin-right: .5rem" :href="detail.objectPre" target="_blank" rel="noopener noreferrer">{{detail.objectPreShort}}</a>
                        <span v-if="detail.objectPreLabel" style="margin-left:.4rem;margin-right:2rem;">({{detail.objectPreLabel}})</span>
                      </p>
                      <p v-if="detail.objectLabel">
                        <span style="color: #555;">{{detail.objectLabel}} </span>
                        <span v-if="detail.objectLabelLang" style="margin-left:.4rem;margin-right:2rem;">({{detail.objectLabelLang}})</span>
                      </p>
                      <p v-if="detail.objectIsIri">
                        <a :href="detail.objectIsIri" target="_blank" rel="noopener noreferrer">{{detail.objectIsIriShort}}</a>
                        <span v-if="detail.objectIsIriLabel" style="margin-left:.4rem;margin-right:2rem;">({{detail.objectIsIriLabel}})</span>
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        

        <!-- 图表 -->
        <div class="label-item" style="margin-bottom:10rem;">
          <div class="label">
            <p class="title" style="font-weight: 700;font-size:1.2rem">Graph</p>
          </div>
          <div class="label-msg">
             <div ref="chart" class="chart" />
          </div>
        </div>
      </div>
      <!-- 所 -->
      <div class="org-detail">
        <h1 @click="goDS(dataList.identifier)">{{dataList.datasetName}}</h1>
        <div class="org-imgbody">
          <img class="org-img" :src="dataList.dataSetImage" alt="">
        </div>
        <div class="org-content">
          <p class="label">Institution</p>
          <p class="label-key"><a :href="dataList.website" target="_blank" rel="noopener noreferrer">{{dataList.unitName}}</a></p>
        </div>
        <div class="org-content">
          <p class="label">Publish date</p>
          <p class="label-key" v-if="dataList.publishDate">{{$TOOL.dateFormat(dataList.publishDate)}}</p>
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
      subject: this.$route.query.subject,
      dataList: {},
      itemList: [],
      loading: false
    }
  },
  mounted() {
    this.getList()
    this.renderChart()
    const onWinResize = () => {
      this.chart && this.chart.resize()
    }
    window.addEventListener('resize', onWinResize)
    this.chart.on('click', function (params) {
      if (params.data.id) {
        if (params.data.id.indexOf('http') == 0) {
          window.open(params.data.id, "_blank")
        }
      } else {
        window.open(params.data.predicate, "_blank")
      }
    });
  },
  methods: {
    // 获取数据
    getList () {
      this.loading = true
      this.$http.post(`/resource/detail`, {subject: this.subject}).then(_ => {
        if (_.code == 200) {
          this.dataList = _.data
          this.itemList = _.data.dataSets
          this.loading = false
        } else {
          this.$message.error(_.message)
        }
      })
    },
    // 
    goDS(item) {
      this.$router.push({ path: '/DS', query: {identifier: item} })
    },
    async renderChart () {
      if (!this.chart) {
        this.chart = echarts.init(this.$refs.chart)
      }
      this.chart.showLoading()
      try {
        const params = new URLSearchParams();
        params.append('iri', this.subject)
        let chartList = null
        await this.$http.post(`/resource/listRelationResource`, params, { headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },}).then(_ => {
          chartList = _
        })
        let data = chartList.data.nodes;
        let link = chartList.data.links;
        let categories = chartList.data.categories
        data.forEach((node, i) => {
            node.x = 100 + Math.cos(Math.PI / 20 + Math.PI * i /( data.length / 2 ) ) * (50 + i % 2 * 2)
            node.y = 100 - Math.sin(Math.PI / 20 + Math.PI * i /( data.length / 2 ) ) * (50 + i % 2 * 2)
            node.label = {
              show: true,
              formatter:function(params){
                let str = params.data.name
                return str
              }
            };
            node.symbolSize = 80
        });
        link.forEach( i => {
          i.label = {
            show : true,
            formatter:function(params){
                let str = params.data.shortPredicate
                return str
            }
          }
        })
        this.chart.setOption({
          legend: [{
              data: categories.map(function (a) {
                return a.name;
              }),
              textStyle:{
                fontSize: 17
              } 
            }
          ],
          animationDuration: 1500,
          animationEasingUpdate: 'quinticInOut',
          series: [
            {
              type: 'graph',
              layout: 'force',
              data: data,
              links: link,
              categories: categories,
              roam: true,
              lineStyle: {
                color: 'source',
                curveness: 0.3
              },
              zoom: .4,
              force: {
                repulsion: 80
              },
              emphasis: {
                focus: 'adjacency',
                lineStyle: {
                  width: 4
                }
              }
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
      .header{
        display: flex;
        justify-content: center;
        align-items: center;
        border-bottom: 1px solid #d0d0d0;
        margin-bottom: 1rem;
        h2{
          text-align: center;
          color: #3897F1;
          line-height: 3rem;
        }
        a{
          color: #3897F1;
          margin-left: 1rem;
          font-size: 1.2rem;
          text-decoration: none;
          line-height: 3rem;
          font-weight: bold;
        }
      }
      .placeholder-body{
        height: 60rem;
      }
      .label-list{
        margin-bottom: 2rem;
        .set-name{
          background-color: #169BD5;
          color: #fff;
          padding: .2rem .3rem;
          border-radius: .2rem;
          font-size: 1.4rem;
        }
        
      }
      .label-item{
          display: flex;
          margin-top: 1.6rem;
          .label{
            width: 16rem;
            .title{
              font-weight: bold;
              color: #3897F1;
              a{
                color: #3897F1;
                font-size: 1.2rem;
                text-decoration: none;
                // word-wrap: break-word;
                overflow: hidden;
                text-overflow:ellipsis;
                white-space: nowrap;
              }
            }
            .title-det{
              font-size: 1.1rem;
              margin-left: .8rem;
              color: #7F7F7F;
            }
          }
          .label-msg{
            flex: 1;
            font-size: 1.2rem;
            color: #7f7f7f;
            p{
              margin-bottom: .6rem;
              a{
                color: #169BD5;
                text-decoration: none;
              }
              a:hover{
                font-weight: bold;
                text-decoration: underline;
              }
            }
            .chart {
              height: 50rem;
            }
          }
          .label-objects{
            display:flex;
            align-items: center;
            line-height: 20px;
          }
        }
    }
    .org-detail {
      width: 20rem;
      height: 40rem;
      overflow: hidden;
      h1{
        cursor: pointer;
        color: #3897F1;
        line-height: 3rem;
        overflow: hidden;
        text-overflow:ellipsis;
        white-space: nowrap;
      }
      h1:hover{
        text-decoration: underline;
      }
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
        display: flex;
        align-items: center;
        margin-top: 2rem;
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
  a{
    color: #51a8fa;
  }
</style>
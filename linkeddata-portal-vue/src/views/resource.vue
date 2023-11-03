<template>
  <section>
    <div class="filter">
      <div class="filtrate">
        <div v-if="flag" style="line-height: 4;">
          <el-checkbox v-model="DSChecked" :label="DSName" size="large"  @change="getList" />
        </div>
        <div class="filter-select">
          <img src="../assets/filter.png">
          <el-select v-model="filterValue" class="m-2" placeholder="Select" @change="filterChange">
            <el-option v-for="item in options" :key="item" :label="item" :value="item"/>
          </el-select>
        </div>
        <el-checkbox-group v-model="checkList" @change="getList()">
          <el-checkbox v-for="item in checkOption" :key="item" :label="item" />
        </el-checkbox-group>
      </div>
    </div>
    <div class="content">
      <div class="search">
        <el-input v-model="searchText" placeholder="Search data ..." clearable @clear="getList"  />
        <el-button type="primary" @click="getList">Search</el-button>
      </div>
      <div class="list" v-loading="loading">
        <div class="list-item"  v-for="(item, index) in listData" :key="index" >
          <p class="item-title" @click="goDetail(item)"><span class="data-title" v-if="item.title!= null">{{item.title}}</span><span class="item-logogram">{{item.subjectShort}}</span></p>
          <!-- rdfs:label -->
          <div class="item-wrap"  v-if="item.type != null">
            <p class="lable"><a target="_blank" :href="item.type.typeLink">{{item.type.typeShort}}</a></p>
            <p class="msg"><span v-for="(value,key) in item.type.value" :key="key"><a target="_blank" v-for="(i, t) in value" :key="i" :href="t">{{i}}</a></span></p>
          </div>
          <!-- rdf:type -->
          <div class="item-wrap" v-if="item.label != null">
            <p class="lable" v-if="item.label.value.length != 0"><a target="_blank" :href="item.label.labelLink" >{{item.label.labelShort}}</a></p>
            <p class="rdf-label"><span v-for="(value,key) in item.label.value" :key="key"><b v-for="i in value" :key="i">{{i}}</b></span></p>
          </div>
          <div class="item-wrap" v-if="item.closeMatch">
            <p class="lable"><a target="_blank" :href="item.closeMatch.closeMatchLink" >{{item.closeMatch.closeMatchShort}}</a></p>
            <p class="lable">skos:colseMatch</p>
            <p class="msg"><span class="item-color" v-for="(value,key) in item.closeMatch.value" :key="key"><a target="_blank" v-for="(i, t) in value" :key="i" :href="t">{{i}}</a></span></p>
          </div>
          <div class="item-bottom">
            <p><img src="../assets/org-icon.png" alt=""><a :href="item.website" target="_blank" rel="noopener noreferrer">{{item.unitName}}</a></p>
            <p><img src="../assets/link-icon.png" alt=""><a @click="goBack(item.identifier)" rel="noopener noreferrer">{{item.datasetName}}</a></p>
          </div>
        </div>
        <el-empty v-if="listData.length == 0" description="No data was found" />
      </div>
      <el-pagination
          v-model:currentPage="currentPage"
          v-model:page-size="pageSize"
          layout="prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
    </div>
  </section>
  <the-footer/>
</template>

<script>
export default {
  name: 'resource',
  data() {
    return {
      flag: this.$route.query.id ? true : false,
      searchText: this.$route.query.text ? this.$route.query.text : "",
      filterValue: 'filter by domain',
      options: ['filter by domain', 'filter by institution'],
      checkList: [],
      checkOption: [],
      listData:  [],
      loading: false ,
      total: 0,
      pageSize: 5,
      pageNum: 1,
      currentPage: 1,
      DSChecked: this.$route.query.id ? true : false,
      DSName: this.$route.query.title
    }
  },
  mounted() {
    // this.getList()
    this.filterChange()
  },
  methods: {
    // 获取列表
    async getList() {
      this.loading = true
      const data = {
        condition: this.searchText,
        domain: this.filterValue == 'filter by domain' ? this.checkList: [],
        institution: this.filterValue == 'filter by institution' ? this.checkList: [],
        datasetId: this.DSChecked ? this.$route.query.id : '',
        pageNum: this.pageNum,
        pageSize: this.pageSize
      }
      await this.$http.post(`/resource/list`, data).then(res => {
        if (res.code == 200) {
          this.listData = res.data ? res.data.pageList : []
          this.total = res.data ? res.data.countNum : 0
          this.loading = false
        } else {
          this.$message.error(res.message)
          this.loading = false
        }
      })
    },
    // 点击底部连接
    goBack(item) {
      this.$router.push({ path: '/DS', query: {identifier: item} })
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
    goDetail(item) {
      this.$router.push({ path: '/resourceDetail', query: {subject: item.subject} })
    },
    // 页码改变
    handleCurrentChange(val) {
      this.pageNum = val
       this.getList()
    },
    handleSizeChange() {
      this.getList()
    }
  }
}
</script>

<style lang="scss" scoped>
  section{
    margin: 3rem 0 10rem 0;
    display: flex;
    .filter{
      width: 17rem;
      flex-shrink: 0;
      margin-right: 3rem;
      .filter-select{
        display: flex;
        align-items: center;
        margin-bottom: 1rem;
        margin-top: .5rem;
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
    .content{
      flex: 1;
      min-height: 50rem;
      .search{
        display: flex;
        margin-bottom: 2rem;
        .el-input{
          height: 3rem;
        }
        .el-button{
          height: 3rem;
          font-size: 1.2rem;
          margin-left: 2rem;
        }
      }
      .list-item{
        user-select: none;
        padding: 1rem 0;
        border-bottom: 1px dashed #d0d0d0;
        .item-title{
          cursor: pointer;
          color: #3897F1;
          font-size: 1.3rem;
          line-height: 2rem;
          .data-title{
            margin-right: 5rem;
            font-weight: 600;
          }
          span:hover{
            color: rgb(2,125,180);
          }
        }
        .item-wrap{
          display: flex;
          line-height: 2.3rem;
          .lable{
            cursor: pointer;
            font-size: 1.2rem;
            width: 10rem;
            a{
              color: #3897F1;
              text-decoration: none;
            }
            a:hover{
              font-weight: bold;
              text-decoration: underline;
            }
          }
          .lable:hover{
            color: #3897F1;
          }
          .msg{
            font-size: 1.2rem;
            color: #d0d0d0;
            flex: 1;
            span{
              margin-right: 1rem;
              padding: .1rem .6rem;
              background-color: #169BD5;
              border-radius: .2rem;
              color: #fff;
              a{
                color: #fff;
                text-decoration: none;
              }
            }
            span:hover{
              opacity: .7;
            }
            .item-color{
              background-color: #00BFBF;
            }
          }
          .rdf-label{
            span{
              margin-right: 1rem;
              color: #858181;
            }
          }
        }
        .item-bottom{
          color: #3897F1;
          margin-top: .4rem;
          display: flex;
          align-items: center;
          
          p{
            display: flex;
            align-items: center;
            margin-right: 4rem;
            img{
              width: 1rem;
              height: 1rem;
              margin-right: .5rem;
            }
            a{
              text-decoration: none;
              color: #169BD5;
              cursor: pointer;
            }
            a:hover{
              font-weight: bold;
              text-decoration: underline;
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
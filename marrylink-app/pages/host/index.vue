<template>
  <view class="host-container">
    <!-- 搜索栏 -->
    <view class="search-bar">
      <view class="search-input">
        <text class="search-icon">🔍</text>
        <input 
          type="text" 
          v-model="searchKeyword" 
          placeholder="搜索主持人姓名"
          @confirm="handleSearch"
        />
      </view>
      <view class="filter-btn" @click="handleSearch">
        <text>筛选</text>
      </view>
    </view>
    
    <!-- 标签筛选 -->
    <scroll-view class="tag-filter" scroll-x v-if="tagDict.length > 0">
      <view class="tag-list">
        <view
          class="tag-item"
          :class="{ active: selectedTag === '' }"
          @click="selectTag('')"
        >
          全部
        </view>
        <view
          class="tag-item"
          :class="{ active: selectedTag === tag.code }"
          v-for="tag in tagDict"
          :key="tag.code"
          @click="selectTag(tag.code)"
        >
          {{ tag.name }}
        </view>
      </view>
    </scroll-view>
    
    <!-- 主持人列表 -->
    <scroll-view 
      class="host-list" 
      scroll-y 
      @scrolltolower="loadMore"
      :lower-threshold="100"
    >
      <view 
        class="host-item" 
        v-for="host in hostList" 
        :key="host.id"
        @click="goToDetail(host.id)"
      >
        <image class="host-avatar" :src="host.avatar ? BASE_URL + host.avatar : '/static/default-avatar.png'" mode="aspectFill"></image>
        <view class="host-info">
          <view class="host-header">
            <text class="host-name">{{ host.name }}</text>
            <view class="host-rating">
              <text class="rating-star">⭐</text>
              <text class="rating-score">{{ host.rating || '5.0' }}</text>
            </view>
          </view>
          
          <view class="host-tags">
            <text class="tag" v-for="tag in host.tags" :key="tag">{{ getTagNameByCode(tag) }}</text>
          </view>
          
          <view class="host-desc">{{ host.description || '专业婚礼主持人，为您打造完美婚礼' }}</view>
          
          <view class="host-footer">
            <view class="host-stats">
              <text class="stat-item">📋 {{ host.orderCount || 0 }}场婚礼</text>
            </view>
            <view class="host-price">
              <text class="price-label">起</text>
              <text class="price-value">¥{{ host.price || '3000' }}</text>
            </view>
          </view>
        </view>
      </view>
      
      <!-- 加载状态 -->
      <view class="load-status">
        <text v-if="loading">加载中...</text>
        <text v-else-if="noMore">没有更多了</text>
      </view>
    </scroll-view>
    
    <!-- 筛选弹窗 -->
    <view class="filter-popup" v-if="showFilter" @click="showFilter = false">
      <view class="filter-content" @click.stop>
        <view class="filter-header">
          <text class="filter-title">筛选条件</text>
          <text class="filter-close" @click="showFilter = false">✕</text>
        </view>
        
        <view class="filter-section">
          <text class="filter-label">价格区间</text>
          <view class="price-range">
            <input type="number" v-model="filterForm.minPrice" placeholder="最低价" />
            <text>-</text>
            <input type="number" v-model="filterForm.maxPrice" placeholder="最高价" />
          </view>
        </view>
        
        <view class="filter-section">
          <text class="filter-label">经验年限</text>
          <view class="experience-options">
            <view 
              class="option-item"
              :class="{ active: filterForm.experience === item.value }"
              v-for="item in experienceOptions"
              :key="item.value"
              @click="filterForm.experience = item.value"
            >
              {{ item.label }}
            </view>
          </view>
        </view>
        
        <view class="filter-actions">
          <button class="btn-reset" @click="resetFilter">重置</button>
          <button class="btn-confirm" @click="applyFilter">确定</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import { getHostList, searchHost, filterHostByTags, getTagList } from '@/api/host'
import { BASE_URL } from '@/utils/request'

export default {
  data() {
    return {
      BASE_URL: BASE_URL,
      searchKeyword: '',
      selectedTag: '',
      tags: [],
      tagDict: [],  // 标签字典，用于存储完整的标签信息
      hostList: [],
      loading: false,
      noMore: false,
      pageNum: 1,
      pageSize: 10,
      showFilter: false,
      filterForm: {
        minPrice: '',
        maxPrice: '',
        experience: ''
      },
      experienceOptions: [
        { label: '不限', value: '' },
        { label: '1-3年', value: '1-3' },
        { label: '3-5年', value: '3-5' },
        { label: '5-10年', value: '5-10' },
        { label: '10年以上', value: '10+' }
      ]
    }
  },
  
  onShow() {
    // 重置加载状态，确保每次进入页面都能正常加载
    this.loading = false
    this.noMore = false
    
    // 使用 Promise 链式调用，确保即使 loadTagList 失败也会执行 loadHostList
    this.loadTagList()
      .finally(() => {
        this.loadHostList(true)
      })
  },

  computed: {
    ...mapState('user', ['userInfo', 'token']),
    
    isLoggedIn() {
      return !!this.token
    },
    
    avatarUrl() {
      if (!this.userInfo||!this.userInfo.avatar) {
        return '/static/default-avatar.png'
      }
      if (this.userInfo.avatar.startsWith('http')) {
        return this.userInfo.avatar
      }
      return BASE_URL + this.userInfo.avatar
    }
  },
  
  methods: {
    // 加载标签列表
    async loadTagList() {
      try {
        const res = await getTagList("01")  // "01" 是主持人风格标签分类代码
        console.log(res)
        this.tagDict = res.data || []
        // 提取标签名称用于显示
        this.tags = this.tagDict.map(tag => tag.name)
      } catch (error) {
        console.error('加载标签列表失败:', error)
      }
    },
    
    // 根据标签code获取标签名称
    getTagNameByCode(tagCode) {
      const tag = this.tagDict.find(t => t.code === tagCode)
      return tag ? tag.name : tagCode
    },
    
    // 加载主持人列表
    async loadHostList(reset = false) {
      if (this.loading || this.noMore) return
      
      if (reset) {
        this.pageNum = 1
        this.hostList = []
        this.noMore = false
      }
      
      this.loading = true
      
      try {
        const params = {
          current: this.pageNum,  // 后端使用 current 而不是 pageNum
          size: this.pageSize,    // 后端使用 size 而不是 pageSize
          keyword: this.searchKeyword,
          tag: this.selectedTag,
          ...this.filterForm
        }
        
        const res = await getHostList(params)
        const list = res.data.records || []  // 后端返回 records 而不是 list
        console.log(list);
        
        if (reset) {
          this.hostList = list
        } else {
          this.hostList = [...this.hostList, ...list]
        }
        
        if (list.length < this.pageSize) {
          this.noMore = true
        } else {
          this.pageNum++
        }
      } catch (error) {
        console.error('加载主持人列表失败:', error)
      } finally {
        this.loading = false
      }
    },
    
    // 加载更多
    loadMore() {
      if (!this.loading && !this.noMore) {
        this.loadHostList()
      }
    },
    
    // 搜索
    handleSearch() {
      // 重置状态，确保搜索能正常执行
      this.noMore = false
      this.loading = false
      this.loadHostList(true)
    },
    
    // 选择标签
    selectTag(tag) {
      this.selectedTag = tag
      // 重置状态，确保筛选能正常执行
      this.noMore = false
      this.loading = false
      this.loadHostList(true)
    },
    
    // 重置筛选
    resetFilter() {
      this.filterForm = {
        minPrice: '',
        maxPrice: '',
        experience: ''
      }
    },
    
    // 应用筛选
    applyFilter() {
      this.showFilter = false
      this.loadHostList(true)
    },
    
    // 跳转到详情
    goToDetail(id) {
      uni.navigateTo({
        url: `/pages/host/detail?id=${id}`
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.host-container {
  min-height: 100vh;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.search-bar {
  display: flex;
  align-items: center;
  padding: 20rpx 32rpx;
  background-color: #ffffff;
  
  .search-input {
    flex: 1;
    display: flex;
    align-items: center;
    height: 64rpx;
    background-color: #f5f7fa;
    border-radius: 32rpx;
    padding: 0 24rpx;
    margin-right: 16rpx;
    
    .search-icon {
      margin-right: 12rpx;
      font-size: 28rpx;
    }
    
    input {
      flex: 1;
      font-size: 28rpx;
    }
  }
  
  .filter-btn {
    padding: 12rpx 24rpx;
    background-color: #1d4ed8;
    color: #ffffff;
    border-radius: 32rpx;
    font-size: 28rpx;
  }
}

.tag-filter {
  background-color: #ffffff;
  padding: 20rpx 32rpx;
  white-space: nowrap;
  border-bottom: 1rpx solid #e5e7eb;
  
  .tag-list {
    display: inline-flex;
    
    .tag-item {
      padding: 12rpx 24rpx;
      background-color: #f5f7fa;
      border-radius: 32rpx;
      font-size: 24rpx;
      color: #666666;
      margin-right: 16rpx;
      
      &.active {
        background-color: #1d4ed8;
        color: #ffffff;
      }
    }
  }
}

.host-list {
  flex: 1;
  padding: 20rpx 32rpx;
  
  .host-item {
    display: flex;
    background-color: #ffffff;
    border-radius: 16rpx;
    padding: 24rpx;
    margin-bottom: 20rpx;
    
    .host-avatar {
      width: 160rpx;
      height: 160rpx;
      border-radius: 16rpx;
      margin-right: 24rpx;
    }
    
    .host-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      
      .host-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12rpx;
        
        .host-name {
          font-size: 32rpx;
          font-weight: bold;
          color: #333333;
        }
        
        .host-rating {
          display: flex;
          margin-right: 20rpx;
          align-items: center;
          
          .rating-star {
            font-size: 24rpx;
            margin-right: 2rpx;
          }
          
          .rating-score {
            font-size: 24rpx;
            color: #f59e0b;
            font-weight: bold;
          }
        }
      }
      
      .host-tags {
        display: flex;
        flex-wrap: wrap;
        margin-bottom: 12rpx;
        
        .tag {
          font-size: 20rpx;
          color: #1d4ed8;
          background-color: rgba(29, 78, 216, 0.1);
          padding: 4rpx 12rpx;
          border-radius: 8rpx;
          margin-right: 8rpx;
          margin-bottom: 8rpx;
        }
      }
      
      .host-desc {
        font-size: 24rpx;
        color: #666666;
        margin-bottom: 12rpx;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      
      .host-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        .host-stats {
          display: flex;
          
          .stat-item {
            font-size: 22rpx;
            color: #999999;
            margin-right: 16rpx;
          }
        }
        
        .host-price {
          display: flex;
          margin-right: 20rpx;
          align-items: baseline;
          
          .price-label {
            font-size: 20rpx;
            color: #999999;
            margin-right: 4rpx;
          }
          
          .price-value {
            font-size: 32rpx;
            color: #ef4444;
            font-weight: bold;
          }
        }
      }
    }
  }
  
  .load-status {
    text-align: center;
    padding: 32rpx 0;
    font-size: 24rpx;
    color: #999999;
  }
}

.filter-popup {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 999;
  
  .filter-content {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    background-color: #ffffff;
    border-radius: 32rpx 32rpx 0 0;
    padding: 32rpx;
    
    .filter-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 32rpx;
      
      .filter-title {
        font-size: 32rpx;
        font-weight: bold;
        color: #333333;
      }
      
      .filter-close {
        font-size: 40rpx;
        color: #999999;
      }
    }
    
    .filter-section {
      margin-bottom: 32rpx;
      
      .filter-label {
        display: block;
        font-size: 28rpx;
        color: #333333;
        margin-bottom: 16rpx;
      }
      
      .price-range {
        display: flex;
        align-items: center;
        
        input {
          flex: 1;
          height: 64rpx;
          padding: 0 16rpx;
          background-color: #f5f7fa;
          border-radius: 8rpx;
          font-size: 28rpx;
        }
        
        text {
          margin: 0 16rpx;
          color: #999999;
        }
      }
      
      .experience-options {
        display: flex;
        flex-wrap: wrap;
        gap: 16rpx;
        
        .option-item {
          padding: 12rpx 24rpx;
          background-color: #f5f7fa;
          border-radius: 8rpx;
          font-size: 24rpx;
          color: #666666;
          
          &.active {
            background-color: #1d4ed8;
            color: #ffffff;
          }
        }
      }
    }
    
    .filter-actions {
      display: flex;
      gap: 16rpx;
      
      button {
        flex: 1;
        height: 80rpx;
        border: none;
        border-radius: 16rpx;
        font-size: 28rpx;
        line-height: 80rpx;
      }
      
      .btn-reset {
        background-color: #f5f7fa;
        color: #666666;
      }
      
      .btn-confirm {
        background-color: #1d4ed8;
        color: #ffffff;
      }
    }
  }
}
</style>
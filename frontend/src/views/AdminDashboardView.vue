<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <RouterLink class="admin-brand" to="/admin">
        <span class="brand-mark">T</span>
        <strong>星票后台</strong>
      </RouterLink>
      <nav>
        <RouterLink
          v-for="item in visibleMenus"
          :key="item.key"
          :to="item.path"
          :class="{ active: activeSection === item.key }"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </RouterLink>
      </nav>
    </aside>

    <section class="admin-main">
      <header class="admin-header">
        <div>
          <p class="eyebrow">TicketMarket 管理后台</p>
          <h1>{{ activeMenu?.title || '运营概览' }}</h1>
        </div>
        <div class="admin-userbar">
          <span>{{ user.profile?.nickname || user.profile?.username }}</span>
          <el-tag effect="plain">{{ roleText }}</el-tag>
          <RouterLink to="/">返回前台</RouterLink>
          <el-button size="small" @click="logout">退出登录</el-button>
        </div>
      </header>

      <template v-if="activeSection === 'overview'">
        <div class="metric-grid">
          <div class="metric"><span>演出档案</span><strong>{{ metrics.performanceCount }}</strong></div>
          <div class="metric"><span>场馆</span><strong>{{ venues.length }}</strong></div>
          <div class="metric"><span>场次</span><strong>{{ sessions.length }}</strong></div>
          <div class="metric"><span>售票批次</span><strong>{{ saleBatches.length }}</strong></div>
          <div class="metric"><span>订单</span><strong>{{ metrics.orderCount }}</strong></div>
          <div class="metric"><span>待审退票</span><strong>{{ metrics.refundPending }}</strong></div>
        </div>
        <section class="admin-card">
          <div class="table-head">
            <h2>发布流程</h2>
            <el-button type="primary" plain @click="loadAll">刷新</el-button>
          </div>
          <div class="admin-flow">
            <span>1. 建场馆和区域</span>
            <span>2. 生成座位图</span>
            <span>3. 发布演出档案</span>
            <span>4. 绑定场次</span>
            <span>5. 设置票价和库存</span>
            <span>6. 配置开售批次</span>
          </div>
        </section>
      </template>

      <section v-else-if="activeSection === 'performance'" class="admin-card">
        <div class="table-head">
          <h2>演出发布</h2>
          <el-button type="primary" @click="openPerformance()">新建演出</el-button>
        </div>
        <el-table :data="performances" border empty-text="暂无演出">
          <el-table-column label="海报" width="92">
            <template #default="{ row }">
              <img v-if="assetUrl(row.poster)" :src="assetUrl(row.poster)" :alt="row.title" class="admin-thumb" />
              <span v-else class="thumb-placeholder">待导入</span>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="演出名称" min-width="210" />
          <el-table-column prop="categoryName" label="分类" width="110" />
          <el-table-column label="城市/场馆" min-width="200">
            <template #default="{ row }">{{ row.city }} / {{ row.venue }}</template>
          </el-table-column>
          <el-table-column label="价格" width="130">
            <template #default="{ row }">￥{{ row.priceMin }} - ￥{{ row.priceMax }}</template>
          </el-table-column>
          <el-table-column label="售卖状态" width="120">
            <template #default="{ row }">{{ statusText(row.saleStatus) }}</template>
          </el-table-column>
          <el-table-column label="发布" width="100">
            <template #default="{ row }">
              <el-tag :type="row.publishStatus === 'PUBLISHED' ? 'success' : 'info'" effect="plain">
                {{ row.publishStatus === 'PUBLISHED' ? '已发布' : '草稿' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPerformance(row)">编辑全部细节</el-button>
              <RouterLink class="table-link" :to="`/performances/${row.id}`">预览</RouterLink>
              <el-button link type="danger" @click="unpublishPerformance(row)">下架</el-button>
              <el-button link type="danger" @click="deletePerformance(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeSection === 'movie'" class="admin-card">
        <div class="table-head">
          <h2>电影管理</h2>
          <el-button type="primary" @click="openMovie()">新增电影</el-button>
        </div>
        <el-table :data="movies" border empty-text="暂无电影">
          <el-table-column label="海报" width="92">
            <template #default="{ row }">
              <img v-if="assetUrl(row.poster)" :src="assetUrl(row.poster)" :alt="row.title" class="admin-thumb" />
              <span v-else class="thumb-placeholder">待导入</span>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="电影" min-width="180" />
          <el-table-column prop="genre" label="类型" width="140" />
          <el-table-column prop="releaseDate" label="上映" width="120" />
          <el-table-column label="排片" width="90">
            <template #default="{ row }">{{ row.sessions?.length || 0 }} 场</template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openMovie(row)">编辑</el-button>
              <el-button link type="danger" @click="deleteMovie(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeSection === 'home-recommendation'" class="admin-card">
        <div class="table-head">
          <div>
            <h2>首页推荐</h2>
            <p class="section-note">点击左侧板块，再从右侧候选内容中选择，单个板块最多 4 个。顶部轮播图自动使用“热门推荐”的内容。</p>
          </div>
          <el-button type="primary" :disabled="!activeHomeSection" @click="saveHomeRecommendation">保存当前板块</el-button>
        </div>
        <div class="recommendation-admin">
          <aside class="recommendation-sections">
            <button
              v-for="section in homepageRecommendations"
              :key="section.code"
              :class="{ active: activeHomeSectionCode === section.code }"
              @click="selectHomeSection(section)"
            >
              <strong>{{ section.name }}</strong>
              <span>{{ section.selected?.length || 0 }}/{{ section.maxItems || 4 }}</span>
            </button>
          </aside>
          <div class="recommendation-picker">
            <h3>{{ activeHomeSection?.name || '请选择板块' }}</h3>
            <div class="recommendation-grid">
              <button
                v-for="item in activeHomeCandidates"
                :key="`${item.targetType}-${item.targetId}`"
                :class="{ selected: isHomeItemSelected(item) }"
                @click="toggleHomeItem(item)"
              >
                <img :src="assetUrl(item.poster)" :alt="item.title" />
                <strong>{{ item.title }}</strong>
                <span>{{ item.categoryName }} · {{ item.city || '待排片' }}</span>
              </button>
            </div>
          </div>
        </div>
      </section>

      <section v-else-if="activeSection === 'cinema'" class="admin-card">
        <div class="table-head">
          <div>
            <h2>电影院管理</h2>
            <p class="section-note">电影院独立管理，创建时自动生成 4 排 6 座的默认座位图，影厅按 1号厅、2号厅 顺序生成。</p>
          </div>
          <el-button type="primary" @click="openCinema()">新增电影院</el-button>
        </div>
        <el-table :data="cinemas" border empty-text="暂无电影院">
          <el-table-column prop="name" label="电影院" min-width="180" />
          <el-table-column prop="cityName" label="城市" width="100" />
          <el-table-column prop="address" label="地址" min-width="240" />
          <el-table-column label="影厅" width="100">
            <template #default="{ row }">{{ row.halls?.length || 0 }} 个</template>
          </el-table-column>
          <el-table-column prop="capacity" label="座位" width="90" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openCinema(row)">编辑</el-button>
              <el-button link type="primary" @click="openCinemaSchedules(row)">排片</el-button>
              <RouterLink class="table-link" :to="`/admin/venue/${row.id}/seats`">座位图</RouterLink>
              <el-button link type="danger" @click="deleteCinema(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeSection === 'venue'" class="admin-card">
        <div class="table-head">
          <h2>场馆管理</h2>
          <el-button type="primary" @click="openVenue()">新增场馆</el-button>
        </div>
        <el-table :data="venues" border empty-text="暂无场馆">
          <el-table-column prop="name" label="场馆" min-width="150" />
          <el-table-column prop="cityName" label="城市" width="100" />
          <el-table-column label="类型" width="130">
            <template #default="{ row }">{{ venueTypeText(row.venueType) }}</template>
          </el-table-column>
          <el-table-column prop="address" label="地址" min-width="220" />
          <el-table-column prop="capacity" label="容量" width="90" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">{{ statusText(row.status) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="340" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openVenue(row)">编辑资料</el-button>
              <RouterLink class="table-link" :to="`/admin/venue/${row.id}/areas`">区域</RouterLink>
              <RouterLink class="table-link" :to="`/admin/venue/${row.id}/seats`">座位图</RouterLink>
              <el-button link type="danger" @click="disableVenue(row)">禁用</el-button>
              <el-button link type="danger" @click="deleteVenue(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeSection === 'venue-areas'" class="admin-card">
        <div class="table-head">
          <h2>{{ selectedVenue?.name || '场馆' }}区域</h2>
          <el-button type="primary" @click="openArea()">新增区域</el-button>
        </div>
        <el-table :data="areas" border empty-text="暂无区域">
          <el-table-column prop="areaName" label="区域" />
          <el-table-column label="类型" width="120">
            <template #default="{ row }">{{ areaTypeText(row.areaType) }}</template>
          </el-table-column>
          <el-table-column prop="defaultTicketLevel" label="默认票档" width="140" />
          <el-table-column prop="sortOrder" label="排序" width="90" />
          <el-table-column label="颜色" width="100">
            <template #default="{ row }"><span class="color-swatch" :style="{ background: row.color }" /></template>
          </el-table-column>
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button link type="primary" @click="openArea(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeSection === 'seat-template'" class="admin-card">
        <div class="table-head">
          <h2>座位图生成器</h2>
          <div class="head-actions">
            <el-button type="danger" plain :disabled="!seatForm.venueId" @click="clearSeatTemplate(seatForm.venueId)">清空座位图</el-button>
            <el-button type="primary" @click="generateSeatTemplate">生成座位</el-button>
          </div>
        </div>
        <div class="resource-form">
          <el-select v-model="seatForm.venueId" placeholder="选择场馆或电影院" filterable @change="loadAreasForSeatForm">
            <el-option v-for="venue in seatVenueOptions" :key="venue.id" :label="`${venue.name}（${venueTypeText(venue.venueType)}）`" :value="venue.id" />
          </el-select>
          <el-select v-model="seatForm.layoutType" placeholder="座位图模板">
            <el-option label="标准排座" value="STANDARD" />
            <el-option label="电影院 4 排 6 座" value="CINEMA" />
            <el-option label="体育场馆 / 舞台环形看台" value="STADIUM" />
          </el-select>
          <el-select v-model="seatForm.areaId" placeholder="选择区域">
            <el-option v-for="area in seatFormAreas" :key="area.id" :label="area.areaName" :value="area.id" />
          </el-select>
          <el-input-number v-model="seatForm.rowStart" :min="1" placeholder="起始排" />
          <el-input-number v-model="seatForm.rowEnd" :min="1" placeholder="结束排" />
          <el-input-number v-model="seatForm.seatsPerRow" :min="1" placeholder="每排座位" />
          <el-input v-model="seatForm.aisleAfterSeats" placeholder="过道位置，如 8,16" />
        </div>
        <SeatSvg :seats="previewSeats" :venue="seatVenueById(seatForm.venueId)" selectable @seat-click="openSeatEditor" />
      </section>

      <section v-else-if="activeSection === 'venue-seats'" class="admin-card">
        <div class="table-head">
          <h2>{{ selectedVenue?.name || '场馆' }}座位图</h2>
          <div class="head-actions">
            <el-tag>点击座位可编辑</el-tag>
            <el-button type="danger" plain :disabled="!selectedVenue" @click="clearSeatTemplate(selectedVenue.id)">清空座位图</el-button>
          </div>
        </div>
        <SeatSvg :seats="venueSeats" :venue="selectedVenue" selectable @seat-click="openSeatEditor" />
      </section>

      <section v-else-if="activeSection === 'session'" class="admin-card">
        <div class="table-head">
          <h2>场次管理</h2>
          <el-button type="primary" @click="openSession()">新增场次</el-button>
        </div>
        <el-table :data="sessions" border empty-text="暂无场次">
          <el-table-column label="项目" min-width="180">
            <template #default="{ row }">{{ sessionItemTitle(row) }}</template>
          </el-table-column>
          <el-table-column prop="sessionName" label="场次" min-width="180" />
          <el-table-column label="场馆" min-width="150">
            <template #default="{ row }">{{ venueName(row.venueId) }}</template>
          </el-table-column>
          <el-table-column prop="saleStartTime" label="开售时间" width="170" />
          <el-table-column prop="startTime" label="演出时间" width="170" />
          <el-table-column label="模式" width="130">
            <template #default="{ row }">{{ purchaseModeText(row.purchaseMode) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openSession(row)">编辑</el-button>
              <el-button link type="success" @click="selectSessionForLevels(row)">票价</el-button>
              <el-button link type="warning" @click="initSessionSeats(row)">初始化座位</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeSection === 'ticket-level'" class="admin-card">
        <div class="table-head">
          <div>
            <h2>票档与票价</h2>
            <p class="section-note">演出发布会自动创建首批票档；这里用于发布后补改票价、区域和开放库存。</p>
          </div>
          <div class="head-actions">
            <el-select v-model="selectedSessionId" placeholder="选择场次" @change="loadTicketLevels">
              <el-option v-for="session in sessions" :key="session.id" :label="sessionLabel(session)" :value="session.id" />
            </el-select>
            <el-button type="primary" :disabled="!selectedSessionId" @click="openTicketLevel()">新增票档</el-button>
          </div>
        </div>
        <el-table :data="ticketLevels" border empty-text="请先选择场次">
          <el-table-column prop="name" label="票档" />
          <el-table-column label="区域" width="140">
            <template #default="{ row }">{{ areaName(row.areaId) }}</template>
          </el-table-column>
          <el-table-column prop="price" label="价格" width="120" />
          <el-table-column prop="releasedStock" label="库存" width="100" />
          <el-table-column prop="soldStock" label="已售" width="90" />
          <el-table-column label="操作" width="130">
            <template #default="{ row }">
              <el-button link type="primary" @click="openTicketLevel(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeSection === 'sale-batch'" class="admin-card">
        <div class="table-head">
          <div>
            <h2>开售批次</h2>
            <p class="section-note">演出发布会自动创建第一批开售；这里用于二次开售、分批释放和临时锁票。</p>
          </div>
          <div class="head-actions">
            <el-select v-model="selectedSessionId" placeholder="选择场次" @change="loadTicketLevels">
              <el-option v-for="session in sessions" :key="session.id" :label="sessionLabel(session)" :value="session.id" />
            </el-select>
            <el-button type="primary" :disabled="!selectedSessionId" @click="openBatch()">创建批次</el-button>
          </div>
        </div>
        <el-table :data="saleBatches" border empty-text="暂无批次">
          <el-table-column label="场次" min-width="180">
            <template #default="{ row }">{{ sessionLabel(sessionById(row.sessionId)) }}</template>
          </el-table-column>
          <el-table-column prop="batchName" label="批次" min-width="160" />
          <el-table-column prop="saleStartTime" label="开售时间" width="170" />
          <el-table-column prop="lockTime" label="锁票时间" width="170" />
          <el-table-column label="开放方式" width="110">
            <template #default="{ row }">{{ releaseTypeText(row.releaseType) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">{{ statusText(row.status) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openBatch(row)">编辑</el-button>
              <el-button link type="success" @click="startBatch(row)">开售</el-button>
              <el-button link type="warning" @click="lockBatch(row)">锁票</el-button>
              <el-button link type="info" @click="initRedis(row)">初始化库存</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeSection === 'stock-pool'" class="admin-card">
        <div class="table-head">
          <div>
            <h2>库存查询</h2>
            <p class="section-note">查看每个场次、每个票档当前库存。回流记录只表示退票或锁票后的待释放库存，不作为日常发布入口。</p>
          </div>
          <div class="head-actions stock-filter-actions">
            <el-select v-model="stockPerformanceId" clearable filterable placeholder="选择演出" @change="handleStockPerformanceChange">
              <el-option v-for="performance in stockPerformanceOptions" :key="performance.id" :label="performance.title" :value="performance.id" />
            </el-select>
            <el-select v-model="stockSessionId" clearable filterable placeholder="选择场次" @change="handleStockSessionChange">
              <el-option v-for="session in stockSessionOptions" :key="session.id" :label="stockOptionSessionLabel(session)" :value="session.id" />
            </el-select>
            <el-select v-model="stockTicketLevelId" clearable filterable placeholder="选择票档/票价" @change="loadInventory">
              <el-option v-for="level in stockTicketLevelOptions" :key="level.ticketLevelId" :label="stockTicketLabel(level)" :value="level.ticketLevelId" />
            </el-select>
            <el-button type="primary" plain @click="loadInventory">刷新库存</el-button>
          </div>
        </div>
        <el-table :data="inventoryRows" border empty-text="请选择场次或暂无库存">
          <el-table-column label="场次" min-width="240">
            <template #default="{ row }">
              <div>{{ row.itemTitle }}</div>
              <small class="muted-text">{{ row.startTime }} {{ row.venueName ? `/ ${row.venueName}` : '' }}</small>
            </template>
          </el-table-column>
          <el-table-column prop="ticketLevelName" label="票档" min-width="140" />
          <el-table-column prop="price" label="票价" width="90">
            <template #default="{ row }">￥{{ row.price }}</template>
          </el-table-column>
          <el-table-column prop="totalStock" label="总库存" width="90" />
          <el-table-column prop="releasedStock" label="已开放" width="90" />
          <el-table-column prop="availableStock" label="当前可售" width="100" />
          <el-table-column prop="lockedStock" label="锁定中" width="90" />
          <el-table-column prop="soldStock" label="已售" width="90" />
          <el-table-column prop="unreleasedStock" label="未开放" width="90" />
          <el-table-column prop="refundedStock" label="已退回" width="90" />
          <el-table-column prop="waitingPoolStock" label="待释放回流" width="120" />
        </el-table>
        <div class="stock-pool-subtable">
          <div class="table-head compact-head">
            <div>
              <h3>回流记录</h3>
              <p class="section-note">这里仅展示退票、锁票回收等待再次释放的库存记录。</p>
            </div>
          </div>
          <el-table :data="filteredStockPool" border empty-text="暂无回流记录">
            <el-table-column label="场次" min-width="180">
              <template #default="{ row }">{{ stockSessionLabel(row.sessionId) }}</template>
            </el-table-column>
            <el-table-column label="票档" min-width="120">
              <template #default="{ row }">{{ stockLevelLabel(row.ticketLevelId) }}</template>
            </el-table-column>
            <el-table-column label="来源" width="140">
              <template #default="{ row }">{{ sourceTypeText(row.sourceType) }}</template>
            </el-table-column>
            <el-table-column label="库存状态" width="120">
              <template #default="{ row }">{{ statusText(row.stockStatus) }}</template>
            </el-table-column>
            <el-table-column label="可用于下轮" width="120">
              <template #default="{ row }">{{ row.availableForNextBatch ? '可用' : '不可用' }}</template>
            </el-table-column>
          </el-table>
        </div>
      </section>

      <section v-else-if="activeSection === 'refunds'" class="admin-card">
        <div class="table-head">
          <h2>退票审核</h2>
          <el-button type="primary" plain @click="loadOperations">刷新</el-button>
        </div>
        <el-table :data="refunds" border empty-text="暂无退票">
          <el-table-column label="订单" min-width="320">
            <template #default="{ row }">
              <div class="admin-order-cell">
                <img v-if="assetUrl(row.order?.poster)" :src="assetUrl(row.order.poster)" :alt="row.order?.itemTitle" />
                <div>
                  <strong>{{ row.order?.itemTitle || `订单 ${row.orderId}` }}</strong>
                  <small>{{ row.order?.orderNo || row.orderId }}</small>
                  <small>{{ row.order?.sessionTime }} {{ row.order?.ticketLevelName }} × {{ row.order?.quantity }}</small>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="120">
            <template #default="{ row }">￥{{ row.amount }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">{{ refundStatusText(row.status) }}</template>
          </el-table-column>
          <el-table-column prop="message" label="说明" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button v-if="row.status === 'APPLYING'" link type="primary" @click="approve(row)">通过</el-button>
              <el-button v-if="row.status === 'APPLYING'" link type="danger" @click="reject(row)">驳回</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeSection === 'staff-register'" class="admin-card">
        <div class="table-head">
          <div>
            <h2>非用户注册</h2>
            <p class="section-note">仅管理员在这里创建后台管理员或检票员账号，普通用户仍从登录页注册。</p>
          </div>
        </div>
        <el-form label-position="top" class="admin-editor-grid staff-register-form">
          <el-form-item label="账号"><el-input v-model="staffForm.username" /></el-form-item>
          <el-form-item label="昵称"><el-input v-model="staffForm.nickname" /></el-form-item>
          <el-form-item label="密码"><el-input v-model="staffForm.password" type="password" show-password /></el-form-item>
          <el-form-item label="身份">
            <el-select v-model="staffForm.roleCode">
              <el-option label="管理员" value="ADMIN" />
              <el-option label="检票员" value="CHECKER" />
            </el-select>
          </el-form-item>
        </el-form>
        <el-button type="primary" @click="createStaffUser">创建账号</el-button>
      </section>

      <section v-else-if="activeSection === 'reports'" class="admin-card">
        <div class="table-head">
          <h2>统计报表</h2>
          <el-button type="primary" plain @click="loadOperations">刷新</el-button>
        </div>
        <div class="metric-grid">
          <div class="metric"><span>订单数</span><strong>{{ statistics.orderCount }}</strong></div>
          <div class="metric"><span>销售额</span><strong>￥{{ statistics.salesAmount }}</strong></div>
          <div class="metric"><span>电子票</span><strong>{{ statistics.ticketCount }}</strong></div>
          <div class="metric"><span>退票</span><strong>{{ statistics.refundCount }}</strong></div>
        </div>
      </section>

      <template v-else-if="activeSection === 'checkin'">
        <div class="metric-grid">
          <div class="metric"><span>今日核验</span><strong>{{ checkerMetrics.checkinToday }}</strong></div>
          <div class="metric"><span>通过率</span><strong>{{ checkerMetrics.successRate }}</strong></div>
          <div class="metric wide"><span>通道状态</span><strong>{{ checkerMetrics.latestResult }}</strong></div>
        </div>
        <section class="admin-card">
          <h2>检票管理</h2>
          <div class="resource-form compact">
            <el-input v-model="ticketCode" placeholder="输入票号或入场码" />
            <el-button type="primary" @click="verify">核验</el-button>
          </div>
          <el-table :data="checkins" border empty-text="暂无核验">
            <el-table-column prop="ticketNo" label="票号" />
            <el-table-column prop="message" label="核验结果" />
            <el-table-column prop="createdAt" label="时间" width="180" />
          </el-table>
        </section>
      </template>

      <section v-else-if="activeSection === 'risk-logs'" class="admin-card">
        <div class="table-head">
          <h2>风控日志</h2>
          <el-button type="primary" plain @click="loadOperations">刷新</el-button>
        </div>
        <el-table :data="riskLogs" border empty-text="暂无日志">
          <el-table-column prop="action" label="类型" width="140" />
          <el-table-column prop="detail" label="内容" />
          <el-table-column prop="createdAt" label="时间" width="180" />
        </el-table>
      </section>

      <section v-else class="admin-card empty-admin">
        <el-icon><FolderOpened /></el-icon>
        <h2>{{ activeMenu?.title }}</h2>
        <p>该模块暂未接入数据</p>
      </section>
    </section>

    <el-dialog v-model="performanceDialog" :title="performanceForm.id ? '编辑演出' : '新建演出'" width="980px" destroy-on-close>
      <el-form label-position="top" class="admin-editor-grid">
        <el-form-item label="演出名称"><el-input v-model="performanceForm.title" /></el-form-item>
        <el-form-item label="短标题/副标题"><el-input v-model="performanceForm.subtitle" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="performanceForm.categoryCode" @change="syncCategoryName">
            <el-option v-for="category in categories" :key="category.code" :label="category.name" :value="category.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布状态">
          <el-select v-model="performanceForm.publishStatus">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="发布到前台" value="PUBLISHED" />
          </el-select>
        </el-form-item>
        <el-form-item label="前台售卖状态">
          <el-alert
            title="由开售时间、锁票时间和实时库存自动判断"
            description="未到开售时间显示“即将开售/预约抢票”；开售后且有库存显示“热卖中/立即购票”；无库存显示“已售罄”。"
            type="info"
            :closable="false"
            show-icon
          />
        </el-form-item>
        <el-form-item label="购票模式">
          <el-select v-model="performanceForm.saleMode">
            <el-option label="自主选座" value="SELECTABLE" />
            <el-option label="自动分配" value="AUTO_ALLOCATE" />
            <el-option label="只选区域" value="AREA_ONLY" />
            <el-option label="站席" value="STANDING" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联场馆">
          <el-select v-model="performanceForm.venueId" placeholder="选择场馆" clearable @change="syncPerformanceVenue">
            <el-option v-for="venue in venues" :key="venue.id" :label="venue.name" :value="venue.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="城市"><el-input v-model="performanceForm.city" /></el-form-item>
        <el-form-item label="场馆名称"><el-input v-model="performanceForm.venue" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="performanceForm.address" /></el-form-item>
        <el-form-item label="演出时间">
          <el-input :model-value="performanceTimePreview" disabled placeholder="由下方场次自动生成" />
        </el-form-item>
        <el-form-item label="票价区间"><el-input :model-value="performancePricePreview" disabled placeholder="由下方票档自动生成" /></el-form-item>
        <el-form-item label="多场演出时间" class="span-2">
          <div class="session-date-editor">
            <div v-for="(date, index) in performanceForm.sessionDates" :key="index" class="session-date-row">
              <el-date-picker v-model="performanceForm.sessionDates[index]" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" placeholder="选择场次时间" />
              <el-button :disabled="performanceForm.sessionDates.length <= 1" @click="removeSessionDate(index)">删除</el-button>
            </div>
            <el-button @click="addSessionDate">新增场次时间</el-button>
          </div>
        </el-form-item>
        <el-form-item label="统一开售时间">
          <el-date-picker v-model="performanceForm.quickSaleStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" placeholder="选择统一开售时间" />
        </el-form-item>
        <el-form-item label="统一锁票时间">
          <el-date-picker v-model="performanceForm.quickLockTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" placeholder="留空则为演出前 1 小时" clearable />
        </el-form-item>
        <el-form-item label="票档明细" class="span-2">
          <div class="quick-ticket-editor">
            <p class="form-tip">票档名称由区域和价格自动生成，例如“看台517”。库存按每个票档单独设置，不再设置总批次数量。</p>
            <div v-for="(level, index) in performanceForm.quickTicketLevels" :key="index" class="quick-ticket-row">
              <label class="quick-field">
                <span>自动票档</span>
                <el-input :model-value="quickTicketName(level)" disabled />
              </label>
              <label class="quick-field">
                <span>区域</span>
                <el-select v-model="level.areaType" placeholder="选择区域">
                  <el-option v-for="option in quickAreaOptions" :key="option.value" :label="option.label" :value="option.value" />
                </el-select>
              </label>
              <label class="quick-field small">
                <span>价格</span>
                <el-input-number v-model="level.price" :min="0" />
              </label>
              <label class="quick-field small">
                <span>库存</span>
                <el-input-number v-model="level.stock" :min="0" />
              </label>
              <el-button type="danger" @click="removeQuickTicketLevel(index)">删除</el-button>
            </div>
            <el-button @click="addQuickTicketLevel">新增票档</el-button>
          </div>
        </el-form-item>
        <el-form-item label="标签，逗号分隔" class="span-2"><el-input v-model="performanceForm.tagsText" /></el-form-item>
        <el-form-item label="海报" class="span-2">
          <div class="upload-row">
            <img v-if="assetUrl(performanceForm.poster)" :src="assetUrl(performanceForm.poster)" alt="海报预览" class="poster-preview" />
            <input type="file" accept="image/*" :disabled="uploadingPoster" @change="setPerformancePoster" />
            <el-input v-model="performanceForm.poster" placeholder="也可以直接填写图片地址或 D:\desktop\poster.png" @change="importPerformancePosterIfLocal" />
            <el-button @click="importPerformancePoster">导入本机路径</el-button>
          </div>
        </el-form-item>
        <el-form-item label="列表摘要" class="span-2"><el-input v-model="performanceForm.summary" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="演出介绍"><el-input v-model="performanceForm.intro" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="演职人员"><el-input v-model="performanceForm.artistInfo" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="场馆介绍"><el-input v-model="performanceForm.venueIntro" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="购票须知"><el-input v-model="performanceForm.purchaseNotice" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="退票规则" class="span-2">
          <div class="refund-editor">
            <label>
              <span>免费退票截止</span>
              <el-date-picker v-model="performanceForm.refundFreeUntil" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" />
            </label>
            <label>
              <span>20%手续费开始</span>
              <el-date-picker v-model="performanceForm.refundFeeUntil" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" />
            </label>
            <label>
              <span>停止退票时间</span>
              <el-date-picker v-model="performanceForm.refundStopTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" />
            </label>
          </div>
        </el-form-item>
        <el-form-item label="观演须知"><el-input v-model="performanceForm.entryRule" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <div class="detail-builder">
        <div class="table-head">
          <h3>详情页排版</h3>
          <div class="head-actions">
            <el-button @click="insertRichHeading">标题</el-button>
            <el-button @click="insertRichParagraph">文字</el-button>
            <el-select v-model="richFontSize" class="rich-control" placeholder="字号" @change="applyRichFontSize">
              <el-option label="小号 14px" :value="14" />
              <el-option label="正文 16px" :value="16" />
              <el-option label="中号 18px" :value="18" />
              <el-option label="大号 22px" :value="22" />
              <el-option label="标题 28px" :value="28" />
            </el-select>
            <div class="rich-font-buttons" aria-label="常用字号">
              <button type="button" @mousedown.prevent @click="setRichFontSize(14)">14</button>
              <button type="button" @mousedown.prevent @click="setRichFontSize(16)">16</button>
              <button type="button" @mousedown.prevent @click="setRichFontSize(18)">18</button>
              <button type="button" @mousedown.prevent @click="setRichFontSize(22)">22</button>
              <button type="button" @mousedown.prevent @click="setRichFontSize(28)">28</button>
            </div>
            <div class="rich-image-size-control">
              <span>图片宽度</span>
              <el-slider v-model="richImageWidth" :min="20" :max="100" :step="5" :disabled="!selectedRichImage" @change="applyRichImageWidth" />
            </div>
            <input ref="richImageInputRef" type="file" accept="image/*" multiple :disabled="uploadingRichImages" @change="insertRichImageFile" />
            <el-input v-model="richImagePath" placeholder="图片地址或本机路径" class="rich-image-path" />
            <el-button @click="insertRichImagePath">插入图片地址</el-button>
          </div>
        </div>
        <div
          ref="detailEditorRef"
          class="rich-editor"
          contenteditable="true"
          @input="syncRichEditor"
          @keydown="handleRichEditorKeydown"
          @keyup="rememberRichSelection"
          @mouseup="rememberRichSelection"
          @click="handleRichEditorClick"
          @blur="syncRichEditor"
        />
      </div>
      <template #footer>
        <el-button @click="performanceDialog = false">取消</el-button>
        <el-button type="primary" @click="savePerformance">保存演出</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="movieDialog" :title="movieForm.id ? '编辑电影' : '新增电影'" width="920px">
      <el-form label-position="top" class="admin-editor-grid">
        <el-form-item label="电影名称"><el-input v-model="movieForm.title" /></el-form-item>
        <el-form-item label="类型"><el-input v-model="movieForm.genre" /></el-form-item>
        <el-form-item label="上映日期"><el-date-picker v-model="movieForm.releaseDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="片长"><el-input-number v-model="movieForm.durationMinutes" :min="1" /></el-form-item>
        <el-form-item label="导演"><el-input v-model="movieForm.director" /></el-form-item>
        <el-form-item label="主演"><el-input v-model="movieForm.actors" /></el-form-item>
        <el-form-item label="评分"><el-input v-model="movieForm.rating" /></el-form-item>
        <el-form-item label="海报" class="span-2">
          <div class="upload-row">
            <img v-if="assetUrl(movieForm.poster)" :src="assetUrl(movieForm.poster)" alt="海报预览" class="poster-preview" />
            <input type="file" accept="image/*" :disabled="uploadingMoviePoster" @change="setMoviePoster" />
            <el-input v-model="movieForm.poster" placeholder="也可以直接填写图片地址或 D:\desktop\movie.png" @change="importMoviePosterIfLocal" />
            <el-button @click="importMoviePoster">导入本机路径</el-button>
          </div>
        </el-form-item>
        <el-form-item label="简介" class="span-2"><el-input v-model="movieForm.summary" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="排片" class="span-2">
          <div class="quick-ticket-editor">
            <div v-for="(session, index) in movieForm.sessions" :key="index" class="quick-ticket-row">
              <label class="quick-field"><span>城市</span>
                <el-select v-model="session.city" filterable placeholder="选择城市" @change="syncMovieSessionCinema(index)">
                  <el-option v-for="cityName in cinemaCityOptions" :key="cityName" :label="cityName" :value="cityName" />
                </el-select>
              </label>
              <label class="quick-field"><span>电影院</span>
                <el-select v-model="session.cinemaId" filterable placeholder="选择电影院" @change="syncMovieSessionCinema(index)">
                  <el-option v-for="cinema in cinemaOptionsByCity(session.city)" :key="cinema.id" :label="cinema.name" :value="cinema.id" />
                </el-select>
              </label>
              <label class="quick-field"><span>影厅</span>
                <el-select v-model="session.hallName" placeholder="选择影厅">
                  <el-option v-for="hall in hallOptions(session)" :key="hall.name" :label="hall.name" :value="hall.name" />
                </el-select>
              </label>
              <label class="quick-field"><span>放映时间</span><el-date-picker v-model="session.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" /></label>
              <label class="quick-field small"><span>票价</span><el-input-number v-model="session.price" :min="0" /></label>
              <el-button type="danger" @click="removeMovieSession(index)">删除</el-button>
            </div>
            <el-button @click="addMovieSession">新增排片</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="movieDialog = false">取消</el-button>
        <el-button type="primary" @click="saveMovie">保存电影</el-button>
      </template>
      </el-dialog>

    <el-dialog v-model="cinemaDialog" :title="cinemaForm.id ? '编辑电影院' : '新增电影院'" width="620px">
      <el-form label-position="top" class="dialog-form">
        <el-form-item label="电影院名称"><el-input v-model="cinemaForm.name" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="cinemaForm.cityName" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="cinemaForm.address" /></el-form-item>
        <el-form-item label="影厅数量"><el-input-number v-model="cinemaForm.hallCount" :min="1" :max="30" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="cinemaForm.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cinemaDialog = false">取消</el-button>
        <el-button type="primary" @click="saveCinema">保存电影院</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="cinemaScheduleDialog" :title="`${activeCinema?.name || '电影院'}排片`" width="980px">
      <el-table :data="cinemaSchedules" border empty-text="暂无排片">
        <el-table-column label="电影" min-width="220">
          <template #default="{ row }">
            <div class="admin-order-cell">
              <img v-if="assetUrl(row.poster)" :src="assetUrl(row.poster)" :alt="row.movieTitle" />
              <div>
                <strong>{{ row.movieTitle }}</strong>
                <small>{{ row.startTime }} · {{ row.hallName }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="票价" width="100" />
        <el-table-column prop="stock" label="座位" width="90" />
        <el-table-column prop="soldStock" label="已售" width="90" />
        <el-table-column label="座位图" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewCinemaScheduleSeats(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="cinema-schedule-form">
        <el-select v-model="cinemaScheduleForm.movieId" filterable placeholder="选择电影">
          <el-option v-for="movie in movies" :key="movie.id" :label="movie.title" :value="movie.id" />
        </el-select>
        <el-select v-model="cinemaScheduleForm.hallName" filterable placeholder="选择影厅">
          <el-option v-for="hall in activeCinema?.halls || []" :key="hall.id" :label="hall.name" :value="hall.name" />
        </el-select>
        <el-date-picker v-model="cinemaScheduleForm.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" placeholder="放映时间" />
        <el-input-number v-model="cinemaScheduleForm.price" :min="1" />
        <el-button type="primary" @click="saveCinemaSchedule">新增排片</el-button>
      </div>
      <div v-if="cinemaScheduleSeats.length" class="cinema-seat-preview">
        <SeatSvg :seats="cinemaScheduleSeats" :venue="activeCinema || {}" />
      </div>
    </el-dialog>

    <el-dialog v-model="seatEditDialog" title="编辑座位" width="520px">
      <el-form label-position="top" class="dialog-form">
        <el-form-item label="座位">
          <el-input v-model="seatEditForm.seatLabel" disabled />
        </el-form-item>
        <el-form-item label="横向位置">
          <el-input-number v-model="seatEditForm.x" :min="0" :max="760" />
        </el-form-item>
        <el-form-item label="纵向位置">
          <el-input-number v-model="seatEditForm.y" :min="0" :max="560" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="seatEditForm.status">
            <el-option label="可售" value="AVAILABLE" />
            <el-option label="不可售" value="DISABLED" />
            <el-option label="未开放" value="UNRELEASED" />
          </el-select>
        </el-form-item>
        <el-form-item label="设为不可售座位">
          <el-switch v-model="seatEditForm.isDisabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="seatEditDialog = false">取消</el-button>
        <el-button type="primary" @click="saveSeatEdit">保存座位</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="venueDialog" :title="venueForm.id ? '编辑场馆' : '新增场馆'" width="680px">
      <el-form label-position="top" class="dialog-form">
        <el-form-item label="场馆名称"><el-input v-model="venueForm.name" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="venueForm.cityName" /></el-form-item>
        <el-form-item label="详细地址"><el-input v-model="venueForm.address" /></el-form-item>
        <el-form-item label="场馆类型">
          <el-select v-model="venueForm.venueType">
            <el-option label="剧场 / 演出厅" value="THEATER" />
            <el-option label="体育场馆 / 演唱会" value="STADIUM" />
          </el-select>
        </el-form-item>
        <el-form-item label="舞台/银幕标签"><el-input v-model="venueForm.stageLabel" placeholder="舞台、主舞台、银幕" /></el-form-item>
        <el-form-item label="容量"><el-input-number v-model="venueForm.capacity" :min="0" /></el-form-item>
        <el-form-item label="场馆介绍"><el-input v-model="venueForm.description" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="venueDialog = false">取消</el-button>
        <el-button type="primary" @click="saveVenue">保存场馆</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="areaDialog" :title="areaForm.id ? '编辑区域' : '新增区域'" width="560px">
      <el-form label-position="top" class="dialog-form">
        <el-form-item label="区域名称"><el-input v-model="areaForm.areaName" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="areaForm.areaType">
            <el-option label="有座区域" value="SEATED" />
            <el-option label="站席区域" value="STANDING" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认票档"><el-input v-model="areaForm.defaultTicketLevel" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="areaForm.sortOrder" :min="1" /></el-form-item>
        <el-form-item label="颜色"><el-color-picker v-model="areaForm.color" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="areaDialog = false">取消</el-button>
        <el-button type="primary" @click="saveArea">保存区域</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="sessionDialog" :title="sessionForm.id ? '编辑场次' : '新增场次'" width="760px">
      <el-form label-position="top" class="admin-editor-grid">
        <el-form-item label="演出">
          <el-select v-model="sessionForm.performanceId">
            <el-option v-for="item in performances" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="场馆">
          <el-select v-model="sessionForm.venueId">
            <el-option v-for="venue in venues" :key="venue.id" :label="venue.name" :value="venue.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="场次名称"><el-input v-model="sessionForm.sessionName" /></el-form-item>
        <el-form-item label="购票模式">
          <el-select v-model="sessionForm.purchaseMode">
            <el-option label="自主选座" value="SELECTABLE" />
            <el-option label="自动分配" value="AUTO_ALLOCATE" />
          </el-select>
        </el-form-item>
        <el-form-item label="开售时间"><el-date-picker v-model="sessionForm.saleStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" placeholder="选择开售时间" /></el-form-item>
        <el-form-item label="锁票时间"><el-date-picker v-model="sessionForm.lockTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" placeholder="选择锁票时间" /></el-form-item>
        <el-form-item label="入场时间"><el-date-picker v-model="sessionForm.entryTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" placeholder="选择入场时间" /></el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="sessionForm.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" placeholder="选择开始时间" /></el-form-item>
        <el-form-item label="结束时间"><el-date-picker v-model="sessionForm.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" placeholder="选择结束时间" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sessionDialog = false">取消</el-button>
        <el-button type="primary" @click="saveSession">保存场次</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="ticketLevelDialog" :title="ticketLevelForm.id ? '编辑票档' : '新增票档'" width="620px">
      <el-form label-position="top" class="dialog-form">
        <el-form-item label="自动票档名称"><el-input :model-value="ticketLevelAutoName" disabled /></el-form-item>
        <el-form-item label="关联区域">
          <el-select v-model="ticketLevelForm.areaId" placeholder="选择区域">
            <el-option v-for="area in ticketLevelAreas" :key="area.id" :label="area.areaName" :value="area.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格"><el-input-number v-model="ticketLevelForm.price" :min="0" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="ticketLevelForm.stock" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ticketLevelDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTicketLevel">保存票档</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchDialog" :title="batchForm.id ? '编辑开售批次' : '创建开售批次'" width="680px">
      <el-form label-position="top" class="dialog-form">
        <el-form-item label="场次">
          <el-select v-model="batchForm.sessionId">
            <el-option v-for="session in sessions" :key="session.id" :label="sessionLabel(session)" :value="session.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="批次名称"><el-input v-model="batchForm.batchName" /></el-form-item>
        <el-form-item label="开售时间"><el-date-picker v-model="batchForm.saleStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" placeholder="选择开售时间" /></el-form-item>
        <el-form-item label="锁票时间"><el-date-picker v-model="batchForm.lockTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm:ss" placeholder="选择锁票时间" /></el-form-item>
        <el-form-item label="开放方式">
          <el-select v-model="batchForm.releaseType">
            <el-option label="全部开放" value="FULL" />
            <el-option label="按数量开放" value="QUANTITY" />
            <el-option label="按比例开放" value="RATIO" />
          </el-select>
        </el-form-item>
        <el-form-item label="每人限购"><el-input-number v-model="batchForm.purchaseLimit" :min="1" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialog = false">取消</el-button>
        <el-button type="primary" @click="saveBatch">保存批次</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, h, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { http } from '../api/http'
import { adminApi } from '../api/adminResources'
import { getCategories } from '../api/portal'
import { approveRefund, getAdminRefunds, getCheckins, getRiskLogs, getStatisticsOverview, rejectRefund, verifyTicket } from '../api/operations'
import { useUserStore } from '../stores/user'
import { assetUrl } from '../utils/assets'

const SeatSvg = {
  props: { seats: { type: Array, default: () => [] }, selectable: Boolean, venue: { type: Object, default: () => ({}) } },
  emits: ['seat-click'],
  setup(props, { emit }) {
    const color = (status) => ({
      AVAILABLE: '#2f9e44',
      LOCKED: '#f59f00',
      SOLD: '#868e96',
      DISABLED: '#ced4da',
      UNRELEASED: '#adb5bd',
      POST_LOCK_RETURNED: '#7048e8',
      REFUND_WAITING_RELEASE: '#0b7285',
      CHECKED_IN: '#495057'
    }[status] || '#2f9e44')
    const sectionFill = (seat) => seat.areaId % 4 === 0 ? '#ff6b6b' : seat.areaId % 4 === 1 ? '#74c0fc' : seat.areaId % 4 === 2 ? '#b2f2bb' : '#d0bfff'
    return () => {
      const stadium = props.venue?.venueType === 'STADIUM' || props.seats.some((seat) => seat.rowNo === 'AREA')
      const stageLabel = props.venue?.stageLabel || '舞台'
      const seatShapes = props.seats.map((seat) => seat.rowNo === 'AREA'
        ? h('g', { class: props.selectable ? 'seat-section clickable' : 'seat-section', onClick: () => emit('seat-click', seat) }, [
          h('rect', { x: seat.x - 23, y: seat.y - 14, width: 46, height: 28, rx: 3, fill: sectionFill(seat), stroke: '#111827', 'stroke-width': 1.5 }),
          h('text', { x: seat.x, y: seat.y + 4, 'text-anchor': 'middle', 'font-size': 11, 'font-weight': 700, fill: '#111827' }, seat.seatLabel),
          h('title', seat.seatLabel)
        ])
        : h('circle', {
          cx: seat.x,
          cy: seat.y,
          r: 8,
          fill: color(seat.status),
          class: props.selectable ? 'seat-dot clickable' : 'seat-dot',
          onClick: () => emit('seat-click', seat)
        }, [h('title', seat.seatLabel || `${seat.rowNo}-${seat.seatNo}`)]))
      return h('div', { class: ['seat-map-wrap', stadium ? 'stadium-map-wrap' : ''] }, [
        h('div', { class: 'stage-line' }, stageLabel),
        h('svg', { viewBox: '0 0 760 560', class: 'seat-map' }, [
          stadium ? h('ellipse', { cx: 380, cy: 285, rx: 335, ry: 220, fill: 'none', stroke: '#d1d5db', 'stroke-width': 18 }) : null,
          stadium ? h('rect', { x: 295, y: 215, width: 170, height: 115, fill: '#e5e7eb', stroke: '#9ca3af' }) : null,
          stadium ? h('text', { x: 380, y: 280, 'text-anchor': 'middle', 'font-size': 28, 'font-weight': 800, fill: '#111827' }, stageLabel) : null,
          ...seatShapes
        ]),
        h('div', { class: 'seat-legend' }, [
          ['AVAILABLE', '可售'], ['LOCKED', '锁定'], ['SOLD', '已售'], ['DISABLED', '不可售'], ['UNRELEASED', '未开放'], ['POST_LOCK_RETURNED', '锁票回收']
        ].map(([status, label]) => h('span', [h('i', { style: { background: color(status) } }), label])))
      ])
    }
  }
}

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const metrics = reactive({ performanceCount: 0, movieCount: 0, orderCount: 0, ticketCount: 0, refundPending: 0, checkinToday: 0 })
const checkerMetrics = reactive({ checkinToday: 0, successRate: '0%', latestResult: '暂无数据' })
const performances = ref([])
const movies = ref([])
const homepageRecommendations = ref([])
const cinemas = ref([])
const categories = ref([])
const venues = ref([])
const areas = ref([])
const venueSeats = ref([])
const previewSeats = ref([])
const sessions = ref([])
const ticketLevels = ref([])
const ticketLevelAreas = ref([])
const saleBatches = ref([])
const stockPool = ref([])
const inventoryRows = ref([])
const stockInventoryOptions = ref([])
const cinemaSchedules = ref([])
const cinemaScheduleSeats = ref([])
const refunds = ref([])
const checkins = ref([])
const riskLogs = ref([])
const statistics = reactive({ orderCount: 0, salesAmount: 0, ticketCount: 0, refundCount: 0, checkinCount: 0, rushSuccessRate: '0%' })
const ticketCode = ref('')
const selectedSessionId = ref(null)
const stockPerformanceId = ref(null)
const stockSessionId = ref(null)
const stockTicketLevelId = ref(null)
const seatFormAreas = ref([])
const seatForm = reactive({ venueId: 1, areaId: 1, layoutType: 'STANDARD', rowStart: 1, rowEnd: 3, seatsPerRow: 12, startX: 60, startY: 80, gapX: 30, gapY: 30, aisleAfterSeats: '6' })
const detailEditorRef = ref(null)
const richImageInputRef = ref(null)
const richImagePath = ref('')
const selectedRichImage = ref(null)
const richImageWidth = ref(100)
const richFontSize = ref(16)
const savedRichRange = ref(null)
const uploadingPoster = ref(false)
const uploadingRichImages = ref(false)
const uploadingMoviePoster = ref(false)

const performanceDialog = ref(false)
const movieDialog = ref(false)
const cinemaDialog = ref(false)
const cinemaScheduleDialog = ref(false)
const seatEditDialog = ref(false)
const venueDialog = ref(false)
const areaDialog = ref(false)
const sessionDialog = ref(false)
const ticketLevelDialog = ref(false)
const batchDialog = ref(false)
const performanceForm = reactive(emptyPerformance())
const movieForm = reactive(emptyMovie())
const cinemaForm = reactive(emptyCinema())
const cinemaScheduleForm = reactive(emptyCinemaSchedule())
const staffForm = reactive(emptyStaffUser())
const seatEditForm = reactive(emptySeatEdit())
const venueForm = reactive(emptyVenue())
const areaForm = reactive(emptyArea())
const sessionForm = reactive(emptySession())
const ticketLevelForm = reactive(emptyTicketLevel())
const batchForm = reactive(emptyBatch())
const autoSaleRefundDefaults = reactive({ sale: '', free: '', fee: '', stop: '' })
const activeHomeSectionCode = ref('hot')
const activeCinema = ref(null)

const roleMap = { ADMIN: '系统管理员', MANAGER: '票务管理员', CHECKER: '检票员' }
const menus = [
  { key: 'overview', title: '运营概览', icon: 'DataAnalysis', path: '/admin', roles: ['ADMIN', 'MANAGER'] },
  { key: 'performance', title: '演出发布', icon: 'Management', path: '/admin/performance', roles: ['ADMIN', 'MANAGER'] },
  { key: 'movie', title: '电影管理', icon: 'VideoCamera', path: '/admin/movie', roles: ['ADMIN', 'MANAGER'] },
  { key: 'home-recommendation', title: '首页推荐', icon: 'Star', path: '/admin/home-recommendation', roles: ['ADMIN', 'MANAGER'] },
  { key: 'cinema', title: '电影院管理', icon: 'Film', path: '/admin/cinema', roles: ['ADMIN', 'MANAGER'] },
  { key: 'venue', title: '场馆管理', icon: 'Location', path: '/admin/venue', roles: ['ADMIN', 'MANAGER'] },
  { key: 'seat-template', title: '座位模板', icon: 'Grid', path: '/admin/seat-template', roles: ['ADMIN', 'MANAGER'] },
  { key: 'session', title: '场次管理', icon: 'Calendar', path: '/admin/session', roles: ['ADMIN', 'MANAGER'] },
  { key: 'ticket-level', title: '票档票价', icon: 'Tickets', path: '/admin/ticket-level', roles: ['ADMIN', 'MANAGER'] },
  { key: 'sale-batch', title: '开售批次', icon: 'Clock', path: '/admin/sale-batch', roles: ['ADMIN', 'MANAGER'] },
  { key: 'stock-pool', title: '库存查询', icon: 'Box', path: '/admin/stock-pool', roles: ['ADMIN', 'MANAGER'] },
  { key: 'refunds', title: '退票审核', icon: 'RefreshLeft', path: '/admin/refunds', roles: ['ADMIN', 'MANAGER'] },
  { key: 'staff-register', title: '非用户注册', icon: 'UserFilled', path: '/admin/staff-register', roles: ['ADMIN'] },
  { key: 'checkin', title: '检票管理', icon: 'Checked', path: '/admin/checkin', roles: ['ADMIN', 'MANAGER', 'CHECKER'] },
  { key: 'reports', title: '统计报表', icon: 'TrendCharts', path: '/admin/reports', roles: ['ADMIN', 'MANAGER'] },
  { key: 'risk-logs', title: '风控日志', icon: 'Warning', path: '/admin/risk-logs', roles: ['ADMIN'] }
]

const statusMap = {
  ENABLED: '启用',
  DISABLED: '禁用',
  SCHEDULED: '待开售',
  NOT_STARTED: '未开售',
  SELLING: '售票中',
  ON_SALE: '热卖中',
  COMING_SOON: '即将开售',
  SOLD_OUT: '已售罄',
  RETURNED: '票量紧张',
  LOCKED: '已锁票/结束',
  ENDED: '已结束',
  FINISHED: '已结束',
  AVAILABLE: '可售',
  UNRELEASED: '未开放',
  SOLD: '已售',
  POST_LOCK_RETURNED: '锁票回收',
  REFUND_WAITING_RELEASE: '退票待释放',
  WAITING_RELEASE: '待释放',
  CHECKED_IN: '已核验'
}
const purchaseModeMap = { SELECTABLE: '自主选座', AUTO_ALLOCATE: '系统配座', AREA_ONLY: '只选区域', STANDING: '站席' }
const releaseTypeMap = { FULL: '全部开放', PARTIAL: '分批开放', MANUAL: '手动开放', QUANTITY: '按数量', RATIO: '按比例' }
const areaTypeMap = { SEATED: '看台/有座', STANDING: '内场/站席' }
const venueTypeMap = { THEATER: '剧院/剧场', STADIUM: '体育场馆', CINEMA: '电影院' }
const sourceTypeMap = { POST_LOCK_RETURNED: '锁票回收', POST_LOCK_RETURN: '锁票回收', REFUND_WAITING_RELEASE: '退票待释放', REFUND_RETURN: '退票回流', UNRELEASED: '未开放库存', MANUAL_ADD: '人工调整' }

const textFromMap = (map, value) => map[value] || value || '暂无数据'
const statusText = (value) => textFromMap(statusMap, value)
const purchaseModeText = (value) => textFromMap(purchaseModeMap, value)
const releaseTypeText = (value) => textFromMap(releaseTypeMap, value)
const areaTypeText = (value) => textFromMap(areaTypeMap, value)
const venueTypeText = (value) => textFromMap(venueTypeMap, value)
const sourceTypeText = (value) => textFromMap(sourceTypeMap, value)
const refundStatusText = (value) => textFromMap({ APPLYING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }, value)

const activeSection = computed(() => {
  if (route.name === 'admin-venue-areas') return 'venue-areas'
  if (route.name === 'admin-venue-seats') return 'venue-seats'
  return route.params.section || 'overview'
})
const visibleMenus = computed(() => menus.filter((item) => item.roles.includes(user.role)))
const activeMenu = computed(() => menus.find((item) => item.key === activeSection.value))
const roleText = computed(() => roleMap[user.role] || user.role)
const seatVenueOptions = computed(() => [...venues.value, ...cinemas.value.map((item) => ({ ...item, venueType: item.venueType || 'CINEMA', stageLabel: item.stageLabel || '银幕' }))])
const seatVenueById = (id) => seatVenueOptions.value.find((item) => String(item.id) === String(id))
const selectedVenue = computed(() => seatVenueById(route.params.id || seatForm.venueId))
const selectedPerformanceVenue = computed(() => venues.value.find((item) => String(item.id) === String(performanceForm.venueId)))
const activeHomeSection = computed(() => homepageRecommendations.value.find((item) => item.code === activeHomeSectionCode.value) || homepageRecommendations.value[0])
const activeHomeCandidates = computed(() => activeHomeSection.value?.candidates || [])
const selectedHomeItems = computed(() => activeHomeSection.value?.selected || [])
const filteredStockPool = computed(() => stockPool.value.filter((row) => (
  (!stockPerformanceId.value || String(row.performanceId) === String(stockPerformanceId.value)) &&
  (!stockSessionId.value || String(row.sessionId) === String(stockSessionId.value)) &&
  (!stockTicketLevelId.value || String(row.ticketLevelId) === String(stockTicketLevelId.value))
)))
const stockPerformanceOptions = computed(() => {
  const seen = new Set()
  return stockInventoryOptions.value.filter((row) => {
    const key = String(row.performanceId)
    if (!row.performanceId || seen.has(key)) return false
    seen.add(key)
    return true
  }).map((row) => ({ id: row.performanceId, title: row.itemTitle }))
})
const stockSessionOptions = computed(() => {
  const seen = new Set()
  return stockInventoryOptions.value.filter((row) => {
    if (stockPerformanceId.value && String(row.performanceId) !== String(stockPerformanceId.value)) return false
    const key = String(row.sessionId)
    if (seen.has(key)) return false
    seen.add(key)
    return true
  }).map((row) => ({
    id: row.sessionId,
    itemTitle: row.itemTitle,
    sessionName: row.sessionName,
    startTime: row.startTime
  }))
})
const stockTicketLevelOptions = computed(() => {
  const seen = new Set()
  return stockInventoryOptions.value.filter((row) => {
    if (stockPerformanceId.value && String(row.performanceId) !== String(stockPerformanceId.value)) return false
    if (stockSessionId.value && String(row.sessionId) !== String(stockSessionId.value)) return false
    const key = String(row.ticketLevelId)
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
})
const cinemaCityOptions = computed(() => [...new Set(cinemas.value.map((item) => item.cityName).filter(Boolean))])
const ticketLevelAutoName = computed(() => `${areaName(ticketLevelForm.areaId).replace(/^区域\s*/, '票档')}${Number(ticketLevelForm.price || 0)}`)
const performanceDateRows = computed(() => (performanceForm.sessionDates || []).map(normalizeDateTime).filter(Boolean).sort())
const performanceTimePreview = computed(() => {
  if (!performanceDateRows.value.length) return ''
  const first = performanceDateRows.value[0]
  const last = performanceDateRows.value[performanceDateRows.value.length - 1]
  return first === last ? first : `${first} 至 ${last}`
})
const performancePriceRows = computed(() => performanceForm.quickTicketLevels.map((item) => Number(item.price || 0)).filter((value) => value > 0))
const performancePricePreview = computed(() => {
  if (!performancePriceRows.value.length) return ''
  const min = Math.min(...performancePriceRows.value)
  const max = Math.max(...performancePriceRows.value)
  return min === max ? `￥${min}` : `￥${min} - ￥${max}`
})
const quickAreaOptions = computed(() => {
  const venueType = selectedPerformanceVenue.value?.venueType
  if (venueType === 'STADIUM') {
    return [
      { label: '看台', value: 'SEATED' },
      { label: '内场', value: 'STANDING' }
    ]
  }
  if (venueType === 'CINEMA') return [{ label: '座位区', value: 'SEATED' }]
  if (venueType === 'THEATER') return [{ label: '座位区', value: 'SEATED' }]
  return [
    { label: '看台', value: 'SEATED' },
    { label: '内场', value: 'STANDING' }
  ]
})

const clone = (value) => JSON.parse(JSON.stringify(value || {}))
const sessionById = (id) => sessions.value.find((item) => String(item.id) === String(id))
const performanceTitle = (id) => performances.value.find((item) => String(item.id) === String(id))?.title || `演出 ${id || ''}`
const movieTitle = (id) => movies.value.find((item) => String(item.id) === String(id))?.title || `电影 ${id || ''}`
const sessionItemTitle = (session) => {
  if (!session) return '未选择项目'
  if (session.performanceTitle) return session.performanceTitle
  if (session.movieTitle) return session.movieTitle
  if (session.performanceId) return performanceTitle(session.performanceId)
  if (session.movieId) return movieTitle(session.movieId)
  return '未选择项目'
}
const venueName = (id) => venues.value.find((item) => String(item.id) === String(id))?.name || `场馆 ${id || ''}`
const areaName = (id) => [...areas.value, ...seatFormAreas.value, ...ticketLevelAreas.value].find((item) => String(item.id) === String(id))?.areaName || `区域 ${id || ''}`
const sessionLabel = (session) => session ? `${sessionItemTitle(session)} / ${session.sessionName}` : '未选择场次'
const stockSessionLabel = (id) => {
  const session = sessionById(id)
  return session ? sessionLabel(session) : `场次 ${id || ''}`
}
const stockLevelLabel = (id) => inventoryRows.value.find((item) => String(item.ticketLevelId) === String(id))?.ticketLevelName || `票档 ${id || ''}`
const stockTicketLabel = (row) => `${row.ticketLevelName || `票档 ${row.ticketLevelId}`} ￥${row.price}`
const stockOptionSessionLabel = (session) => `${session.itemTitle || '未命名项目'} / ${session.sessionName || session.startTime || '未命名场次'}`

async function loadAll() {
  if (user.canUseAdminApi) {
    Object.assign(metrics, await adminApi.dashboard())
    const [categoryRows, performanceRows, movieRows, venueRows, sessionRows, batchRows, poolRows, cinemaRows, recommendationRows] = await Promise.all([
      getCategories(),
      adminApi.performances(),
      adminApi.movies(),
      adminApi.venues(),
      adminApi.sessions(),
      adminApi.saleBatches(),
      adminApi.stockPool(),
      adminApi.cinemas(),
      adminApi.homepageRecommendations()
    ])
    categories.value = categoryRows
    performances.value = performanceRows
    movies.value = movieRows
    venues.value = venueRows.filter((item) => item.venueType !== 'CINEMA')
    cinemas.value = cinemaRows
    homepageRecommendations.value = recommendationRows.sections || []
    sessions.value = sessionRows
    saleBatches.value = batchRows
    stockPool.value = poolRows
    if (!selectedSessionId.value && sessions.value.length) selectedSessionId.value = sessions.value[0].id
    if (!seatForm.venueId && seatVenueOptions.value.length) seatForm.venueId = seatVenueOptions.value[0].id
    await Promise.all([loadTicketLevels(), loadInventory({ ensureDefault: true }), loadAreasForRoute(), loadVenueSeatsForRoute(), loadAreasForSeatForm()])
  }
  if (user.canUseChecker) Object.assign(checkerMetrics, await http.get('/api/checker/dashboard'))
  await loadOperations()
}

async function loadOperations() {
  if (user.canUseAdminApi) {
    refunds.value = await getAdminRefunds()
    Object.assign(statistics, await getStatisticsOverview())
    riskLogs.value = await getRiskLogs()
  }
  if (user.canUseChecker) checkins.value = await getCheckins()
}

async function loadAreasForRoute() {
  if (route.params.id) areas.value = await adminApi.areas(route.params.id)
}

async function loadVenueSeatsForRoute() {
  if (route.params.id) venueSeats.value = await adminApi.seats(route.params.id)
}

async function loadAreasForSeatForm() {
  if (!seatForm.venueId) return
  const venue = seatVenueById(seatForm.venueId)
  if (venue?.venueType === 'STADIUM') seatForm.layoutType = 'STADIUM'
  if (venue?.venueType === 'CINEMA') {
    seatForm.layoutType = 'CINEMA'
    seatForm.rowStart = 1
    seatForm.rowEnd = 4
    seatForm.seatsPerRow = 6
    seatForm.startX = 245
    seatForm.startY = 150
    seatForm.gapX = 54
    seatForm.gapY = 48
    seatForm.aisleAfterSeats = ''
  }
  seatFormAreas.value = await adminApi.areas(seatForm.venueId)
  if (!seatFormAreas.value.some((area) => area.id === seatForm.areaId)) {
    seatForm.areaId = seatFormAreas.value[0]?.id || null
  }
}

async function loadTicketLevels() {
  if (!selectedSessionId.value) return
  ticketLevels.value = await adminApi.ticketLevels(selectedSessionId.value)
  const session = sessionById(selectedSessionId.value)
  ticketLevelAreas.value = session?.venueId ? await adminApi.areas(session.venueId) : []
}

async function loadInventory(options = {}) {
  if (!user.canUseAdminApi) return
  stockInventoryOptions.value = await adminApi.inventory()
  const hasSelectedPerformance = !stockPerformanceId.value || stockPerformanceOptions.value.some((row) => String(row.id) === String(stockPerformanceId.value))
  if (!hasSelectedPerformance) {
    stockPerformanceId.value = null
    stockSessionId.value = null
    stockTicketLevelId.value = null
  }
  const hasSelectedInventory = stockSessionId.value && stockSessionOptions.value.some((row) => String(row.id) === String(stockSessionId.value))
  if (!hasSelectedInventory) {
    stockSessionId.value = null
    stockTicketLevelId.value = null
  }
  const params = {}
  if (stockPerformanceId.value) params.performanceId = stockPerformanceId.value
  if (stockSessionId.value) params.sessionId = stockSessionId.value
  if (stockTicketLevelId.value) params.ticketLevelId = stockTicketLevelId.value
  inventoryRows.value = await adminApi.inventory(params)
  stockPool.value = stockSessionId.value ? await adminApi.sessionStockPool(stockSessionId.value) : await adminApi.stockPool()
  if (stockTicketLevelId.value && !inventoryRows.value.some((row) => String(row.ticketLevelId) === String(stockTicketLevelId.value))) {
    stockTicketLevelId.value = null
  }
}

async function handleStockPerformanceChange() {
  stockSessionId.value = null
  stockTicketLevelId.value = null
  await loadInventory()
}

async function handleStockSessionChange() {
  stockTicketLevelId.value = null
  await loadInventory()
}

function emptyPerformance() {
  return {
    id: null,
    title: '',
    subtitle: '',
    categoryCode: 'concert',
    categoryName: '演唱会',
    venueId: null,
    city: '上海',
    venue: '',
    address: '',
    startTime: '',
    priceMin: 0,
    priceMax: 0,
    sessionDatesText: '',
    sessionDates: [],
    quickSaleStartTime: '',
    quickLockTime: '',
    quickTicketLevels: [],
    poster: '',
    banner: '',
    detailImage: '',
    saleStatus: 'COMING_SOON',
    saleMode: 'SELECTABLE',
    publishStatus: 'DRAFT',
    homeRecommended: false,
    homeSort: 0,
    tagsText: '实名制,电子票,限购2张',
    summary: '',
    intro: '',
    artistInfo: '',
    venueIntro: '',
    purchaseNotice: '',
    refundRule: '',
    refundFreeUntil: '',
    refundFeeUntil: '',
    refundStopTime: '',
    entryRule: '',
    detailContent: '<h2>项目介绍</h2><p>请在这里编辑详情页内容，可以插入多段文字和多张图片。</p>',
    detailBlocks: [
      { type: 'HEADING', content: '项目介绍' },
      { type: 'PARAGRAPH', content: '' }
    ]
  }
}

function emptyVenue() {
  return { id: null, name: '', cityName: '上海', address: '', venueType: 'THEATER', stageLabel: '舞台', capacity: 0, description: '', status: 'ENABLED' }
}

function emptyCinema() {
  return { id: null, name: '', cityName: '上海', address: '', hallCount: 1, description: '', status: 'ENABLED' }
}

function emptyArea() {
  return { id: null, areaName: '', areaType: 'SEATED', defaultTicketLevel: '标准票', sortOrder: 1, color: '#d9303e' }
}

function emptySeatEdit() {
  return { id: null, seatLabel: '', x: 0, y: 0, isAisle: false, isDisabled: false, status: 'AVAILABLE' }
}

function emptySession() {
  return {
    id: null,
    performanceId: performances.value[0]?.id || null,
    venueId: venues.value[0]?.id || null,
    sessionName: '',
    saleStartTime: '2026-07-20 10:00:00',
    lockTime: '2026-08-01 18:00:00',
    entryTime: '2026-08-18 18:00:00',
    startTime: '2026-08-18 19:30:00',
    endTime: '2026-08-18 22:00:00',
    purchaseMode: 'SELECTABLE',
    status: 'SCHEDULED'
  }
}

function emptyTicketLevel() {
  return { id: null, sessionId: selectedSessionId.value, price: 180, areaId: null, stock: 0, totalStock: 0, releasedStock: 0, unreleasedStock: 0, status: 'ENABLED' }
}

function emptyBatch() {
  return { id: null, sessionId: selectedSessionId.value, batchName: '第一批开售', saleStartTime: '2026-07-20 10:00:00', lockTime: '2026-08-01 18:00:00', releaseType: 'QUANTITY', releaseQuantity: 0, releaseRatio: 0, purchaseLimit: 2, enableQueue: true, allowReturnDuringSale: true, status: 'NOT_STARTED' }
}

function resetReactive(target, source) {
  Object.keys(target).forEach((key) => delete target[key])
  Object.assign(target, source)
}

async function openPerformance(row) {
  const fullRow = row?.id ? await adminApi.performance(row.id) : null
  const next = fullRow ? clone(fullRow) : (row ? clone(row) : emptyPerformance())
  next.tagsText = (next.tags || []).join(',') || next.tagsText || ''
  if (row?.id) {
    const existingSessions = performanceSessions(row.id)
    if (existingSessions.length) {
      const orderedSessions = [...existingSessions].sort((a, b) => normalizeDateTime(a.startTime).localeCompare(normalizeDateTime(b.startTime)))
      const firstSession = orderedSessions[0]
      next.startTime = normalizeDateTime(firstSession.startTime)
      next.sessionDates = orderedSessions.map((session) => normalizeDateTime(session.startTime)).filter(Boolean)
      next.quickSaleStartTime = normalizeDateTime(firstSession.saleStartTime)
      next.quickLockTime = normalizeDateTime(firstSession.lockTime)
      next.saleMode = firstSession.purchaseMode || next.saleMode
      const [levels, sessionAreas] = await Promise.all([
        adminApi.ticketLevels(firstSession.id),
        firstSession.venueId ? adminApi.areas(firstSession.venueId) : Promise.resolve([])
      ])
      if (levels.length) {
        next.quickTicketLevels = levels.map((level) => {
          const area = sessionAreas.find((item) => String(item.id) === String(level.areaId))
          return {
            areaType: area?.areaType || 'SEATED',
            price: Number(level.price || 0),
            stock: Number(level.releasedStock || level.totalStock || 0)
          }
        })
      }
    }
  }
  const legacyDates = String(next.sessionDatesText || '').split(/\r?\n/).map(normalizeDateTime).filter(Boolean)
  next.sessionDates = Array.isArray(next.sessionDates) && next.sessionDates.length
    ? next.sessionDates.map(normalizeDateTime).filter(Boolean)
    : (legacyDates.length ? legacyDates : (next.startTime ? [normalizeDateTime(next.startTime)].filter(Boolean) : []))
  if (!Array.isArray(next.quickTicketLevels)) next.quickTicketLevels = []
  if (!next.detailBlocks?.length) {
    next.detailBlocks = [
      { type: 'HEADING', content: '项目介绍' },
      { type: 'PARAGRAPH', content: next.intro || next.summary || '' }
    ]
  }
  Object.assign(autoSaleRefundDefaults, { sale: '', free: '', fee: '', stop: '' })
  resetReactive(performanceForm, { ...emptyPerformance(), ...next })
  applyDefaultSaleRefundTimes()
  performanceDialog.value = true
  renderRichEditor()
}

function syncCategoryName() {
  performanceForm.categoryName = categories.value.find((item) => item.code === performanceForm.categoryCode)?.name || performanceForm.categoryName
}

function syncPerformanceVenue() {
  const venue = venues.value.find((item) => item.id === performanceForm.venueId)
  if (!venue) return
  performanceForm.venue = venue.name
  performanceForm.city = venue.cityName
  performanceForm.address = venue.address
  performanceForm.venueIntro = venue.description
  if (venue.venueType !== 'STADIUM') {
    performanceForm.quickTicketLevels.forEach((level) => {
      level.areaType = 'SEATED'
    })
  }
}

function emptyMovie() {
  return {
    id: null,
    title: '',
    genre: '剧情',
    releaseDate: '2026-08-01',
    durationMinutes: 120,
    director: '',
    actors: '',
    rating: '',
    poster: '/uploads/posters/movie/movie-201.svg',
    summary: '',
    homeRecommended: false,
    homeSort: 0,
    sessions: []
  }
}

function emptyMovieSession() {
  const firstCinema = cinemas.value[0] || {}
  return {
    city: firstCinema.cityName || cinemaCityOptions.value[0] || '',
    cinemaId: firstCinema.id || null,
    cinemaName: firstCinema.name || '',
    hallName: '1号厅',
    startTime: '',
    price: 68
  }
}

function emptyCinemaSchedule() {
  return { movieId: null, hallName: '', startTime: '', price: 68 }
}

function emptyStaffUser() {
  return { username: '', nickname: '', password: '', roleCode: 'CHECKER' }
}

async function uploadSelectedImage(event, callback) {
  const file = event.target.files?.[0]
  if (!file) return
  try {
    const result = await adminApi.uploadImage(file)
    callback(result.path)
    ElMessage.success('图片已上传')
  } catch (error) {
    ElMessage.error(error.message || '图片上传失败')
  } finally {
    event.target.value = ''
  }
}

async function setPerformancePoster(event) {
  uploadingPoster.value = true
  try {
    await uploadSelectedImage(event, (value) => {
      performanceForm.poster = value
      performanceForm.banner = value
    })
  } finally {
    uploadingPoster.value = false
  }
}

async function setMoviePoster(event) {
  uploadingMoviePoster.value = true
  try {
    await uploadSelectedImage(event, (value) => {
      movieForm.poster = value
    })
  } finally {
    uploadingMoviePoster.value = false
  }
}

function setBlockImage(index, event) {
  uploadSelectedImage(event, (value) => {
    performanceForm.detailBlocks[index].content = value
    performanceForm.detailBlocks[index].imagePath = value
    if (!performanceForm.detailImage) performanceForm.detailImage = value
  })
}

async function importPerformancePoster() {
  if (!performanceForm.poster) {
    ElMessage.warning('请先填写本机图片路径')
    return
  }
  const result = await adminApi.uploadLocalImage(performanceForm.poster)
  performanceForm.poster = result.path
  performanceForm.banner = result.path
  ElMessage.success('海报已导入 uploads')
}

function looksLikeLocalImagePath(path) {
  const value = String(path || '').trim()
  return /^[a-zA-Z]:[\\/]/.test(value) || value.startsWith('\\\\')
}

async function importPerformancePosterIfLocal() {
  if (!looksLikeLocalImagePath(performanceForm.poster)) return
  await importPerformancePoster()
}

async function importMoviePoster() {
  if (!movieForm.poster) {
    ElMessage.warning('请先填写本机图片路径')
    return
  }
  const result = await adminApi.uploadLocalImage(movieForm.poster)
  movieForm.poster = result.path
  ElMessage.success('电影海报已导入 uploads')
}

async function importMoviePosterIfLocal() {
  if (!looksLikeLocalImagePath(movieForm.poster)) return
  await importMoviePoster()
}

async function importBlockImage(index) {
  const path = performanceForm.detailBlocks[index]?.content
  if (!path) {
    ElMessage.warning('请先填写本机图片路径')
    return
  }
  const result = await adminApi.uploadLocalImage(path)
  performanceForm.detailBlocks[index].content = result.path
  performanceForm.detailBlocks[index].imagePath = result.path
  if (!performanceForm.detailImage) performanceForm.detailImage = result.path
  ElMessage.success('详情图片已导入 uploads')
}

function addDetailBlock(type) {
  performanceForm.detailBlocks.push({ type, content: type === 'HEADING' ? '新的小标题' : '' })
}

function moveBlock(index, offset) {
  const target = index + offset
  if (target < 0 || target >= performanceForm.detailBlocks.length) return
  const [item] = performanceForm.detailBlocks.splice(index, 1)
  performanceForm.detailBlocks.splice(target, 0, item)
}

function removeBlock(index) {
  performanceForm.detailBlocks.splice(index, 1)
}

function syncRichEditor() {
  if (!detailEditorRef.value) return
  performanceForm.detailContent = detailEditorRef.value.innerHTML
}

function renderRichEditor() {
  nextTick(() => {
    if (!detailEditorRef.value) return
    detailEditorRef.value.innerHTML = rewriteRichImageSources(performanceForm.detailContent || '')
    selectedRichImage.value = null
  })
}

function rewriteRichImageSources(html) {
  return String(html || '').replace(/src="([^"]+)"/g, (_, src) => `src="${assetUrl(src)}"`)
}

function handleRichEditorClick(event) {
  rememberRichSelection()
  const image = event.target?.tagName === 'IMG' ? event.target : null
  selectedRichImage.value = image
  if (!image) return
  const width = image.style.width || image.getAttribute('width') || ''
  if (String(width).endsWith('%')) {
    richImageWidth.value = Number.parseInt(width, 10) || 100
  } else {
    const parentWidth = image.parentElement?.clientWidth || detailEditorRef.value?.clientWidth || image.clientWidth
    richImageWidth.value = parentWidth ? Math.round((image.clientWidth / parentWidth) * 100) : 100
  }
}

function rememberRichSelection() {
  const selection = window.getSelection()
  if (!selection?.rangeCount || !detailEditorRef.value) return
  const range = selection.getRangeAt(0)
  if (detailEditorRef.value.contains(range.commonAncestorContainer)) {
    savedRichRange.value = range.cloneRange()
  }
}

function handleDocumentSelectionChange() {
  rememberRichSelection()
}

function selectionInsideRichEditor(selection = window.getSelection()) {
  if (!selection?.rangeCount || !detailEditorRef.value) return false
  return detailEditorRef.value.contains(selection.getRangeAt(0).commonAncestorContainer)
}

function restoreRichSelection() {
  if (!savedRichRange.value || !detailEditorRef.value) return false
  if (!detailEditorRef.value.contains(savedRichRange.value.commonAncestorContainer)) return false
  detailEditorRef.value.focus()
  const selection = window.getSelection()
  selection?.removeAllRanges()
  selection?.addRange(savedRichRange.value)
  return true
}

function applyRichImageWidth() {
  const image = selectedRichImage.value
  if (!image || !detailEditorRef.value?.contains(image)) return
  image.style.width = `${richImageWidth.value}%`
  image.style.height = 'auto'
  syncRichEditor()
}

function applyRichFontSize() {
  const size = Number(richFontSize.value || 16)
  const selection = window.getSelection()
  if (!selectionInsideRichEditor(selection) || selection.isCollapsed) {
    restoreRichSelection()
  }
  const activeSelection = window.getSelection()
  if (!selectionInsideRichEditor(activeSelection)) {
    ElMessage.info('请先选中详情正文里的文字')
    return
  }
  if (!activeSelection?.rangeCount || activeSelection.isCollapsed) {
    const block = richBlockFromNode(activeSelection?.anchorNode || savedRichRange.value?.commonAncestorContainer)
    if (block && detailEditorRef.value?.contains(block)) {
      block.style.fontSize = `${size}px`
      syncRichEditor()
      return
    }
    ElMessage.info('请先选中文字，或把光标放在需要调整的段落中')
    return
  }
  document.execCommand('fontSize', false, '7')
  detailEditorRef.value?.querySelectorAll('font[size="7"]').forEach((node) => {
    const span = document.createElement('span')
    span.style.fontSize = `${size}px`
    span.innerHTML = node.innerHTML
    node.replaceWith(span)
  })
  rememberRichSelection()
  syncRichEditor()
}

function setRichFontSize(size) {
  richFontSize.value = size
  applyRichFontSize()
}

function richBlockFromNode(node) {
  let current = node?.nodeType === Node.ELEMENT_NODE ? node : node?.parentElement
  while (current && current !== detailEditorRef.value) {
    if (['P', 'DIV', 'H2', 'H3', 'LI'].includes(current.tagName)) return current
    current = current.parentElement
  }
  return null
}

function isEmptyRichBlock(block) {
  if (!block || block.querySelector('img')) return false
  const text = (block.textContent || '').replace(/\u00a0/g, '').trim()
  const html = block.innerHTML.replace(/<br\s*\/?>/gi, '').replace(/&nbsp;/gi, '').trim()
  return !text && !html
}

function placeCaretInEditor(target) {
  const editor = detailEditorRef.value
  if (!editor) return
  const range = document.createRange()
  const selection = window.getSelection()
  const node = target && editor.contains(target) ? target : editor
  range.selectNodeContents(node)
  range.collapse(false)
  selection?.removeAllRanges()
  selection?.addRange(range)
}

function ensureEditableContent() {
  const editor = detailEditorRef.value
  if (!editor) return
  if (!editor.textContent.trim() && !editor.querySelector('img')) {
    editor.innerHTML = '<p><br></p>'
  }
}

function handleRichEditorKeydown(event) {
  if (!['Backspace', 'Delete'].includes(event.key)) return
  const selection = window.getSelection()
  if (!selection?.rangeCount || !selection.getRangeAt(0).collapsed) return
  const block = richBlockFromNode(selection.anchorNode)
  if (!isEmptyRichBlock(block)) return
  event.preventDefault()
  const nextTarget = event.key === 'Delete'
    ? (block.nextElementSibling || block.previousElementSibling)
    : (block.previousElementSibling || block.nextElementSibling)
  block.remove()
  ensureEditableContent()
  placeCaretInEditor(nextTarget)
  syncRichEditor()
}

function insertRichHtml(html) {
  if (!detailEditorRef.value) return
  detailEditorRef.value.focus()
  document.execCommand('insertHTML', false, html)
  syncRichEditor()
}

function insertRichHeading() {
  insertRichHtml('<h2>新的小标题</h2>')
}

function insertRichParagraph() {
  insertRichHtml('<p>在这里输入正文内容。</p>')
}

async function insertRichImagePath() {
  let path = richImagePath.value.trim()
  if (!path) {
    ElMessage.warning('请先填写图片地址或本机路径')
    return
  }
  if (looksLikeLocalImagePath(path)) {
    const result = await adminApi.uploadLocalImage(path)
    path = result.path
  }
  insertRichHtml(`<p><img src="${assetUrl(path)}" alt="详情图片"></p>`)
  richImagePath.value = ''
  if (!performanceForm.detailImage) performanceForm.detailImage = path
}

async function insertRichImageFile(event) {
  const files = Array.from(event.target.files || [])
  if (!files.length) return
  uploadingRichImages.value = true
  try {
    for (const file of files) {
      const result = await adminApi.uploadImage(file)
      insertRichHtml(`<p><img src="${assetUrl(result.path)}" alt="详情图片"></p>`)
      if (!performanceForm.detailImage) performanceForm.detailImage = result.path
    }
    ElMessage.success(files.length > 1 ? `已插入 ${files.length} 张图片` : '图片已插入')
  } catch (error) {
    ElMessage.error(error.message || '详情图片上传失败')
  } finally {
    uploadingRichImages.value = false
    event.target.value = ''
  }
}

function addQuickTicketLevel() {
  performanceForm.quickTicketLevels.push({ areaType: quickAreaOptions.value[0]?.value || 'SEATED', price: 0, stock: 0 })
}

function removeQuickTicketLevel(index) {
  performanceForm.quickTicketLevels.splice(index, 1)
}

function openMovie(row) {
  resetReactive(movieForm, { ...emptyMovie(), ...(row ? clone(row) : {}) })
  movieForm.sessions = movieForm.sessions?.length
    ? movieForm.sessions.map((session) => ({ ...emptyMovieSession(), ...session, cinemaId: session.venueId || session.cinemaId, cinemaName: session.cinemaName || cinemaName(session.venueId) }))
    : []
  movieDialog.value = true
}

function addMovieSession() {
  const last = movieForm.sessions.at(-1)
  movieForm.sessions.push(last ? { ...emptyMovieSession(), ...last, startTime: addHours(last.startTime, 2) } : emptyMovieSession())
}

function removeMovieSession(index) {
  movieForm.sessions.splice(index, 1)
}

async function saveMovie() {
  await importMoviePosterIfLocal()
  const payload = clone(movieForm)
  payload.sessions = payload.sessions.filter((session) => session.startTime && session.cinemaId).map((session) => {
    const cinema = cinemas.value.find((item) => String(item.id) === String(session.cinemaId))
    return {
      ...session,
      city: cinema?.cityName || session.city,
      cinemaName: cinema?.name || session.cinemaName,
      venueId: session.cinemaId,
      saleStartTime: '',
      lockTime: '',
      stock: undefined
    }
  })
  if (payload.id) await adminApi.updateMovie(payload.id, payload)
  else await adminApi.createMovie(payload)
  ElMessage.success('电影已保存')
  movieDialog.value = false
  await loadAll()
}

async function deleteMovie(row) {
  await adminApi.deleteMovie(row.id)
  ElMessage.success('电影已删除')
  await loadAll()
}

function selectHomeSection(section) {
  activeHomeSectionCode.value = section.code
}

function homeItemKey(item) {
  return `${item.targetType}-${item.targetId}`
}

function isHomeItemSelected(item) {
  return selectedHomeItems.value.some((selected) => homeItemKey(selected) === homeItemKey(item))
}

function toggleHomeItem(item) {
  if (!activeHomeSection.value) return
  const selected = [...selectedHomeItems.value]
  const index = selected.findIndex((row) => homeItemKey(row) === homeItemKey(item))
  if (index >= 0) selected.splice(index, 1)
  else {
    if (selected.length >= (activeHomeSection.value.maxItems || 4)) {
      ElMessage.warning('单个板块最多选择 4 个')
      return
    }
    selected.push(item)
  }
  activeHomeSection.value.selected = selected
}

async function saveHomeRecommendation() {
  if (!activeHomeSection.value) return
  const result = await adminApi.saveHomepageRecommendation(activeHomeSection.value.code, selectedHomeItems.value)
  homepageRecommendations.value = result.sections || []
  ElMessage.success('首页推荐已保存')
}

function cinemaOptionsByCity(city) {
  const value = String(city || '').trim()
  return cinemas.value.filter((item) => !value || item.cityName === value)
}

function cinemaName(id) {
  return cinemas.value.find((item) => String(item.id) === String(id))?.name || `电影院 ${id || ''}`
}

function hallOptions(session) {
  return cinemas.value.find((item) => String(item.id) === String(session.cinemaId))?.halls || []
}

function syncMovieSessionCinema(index) {
  const session = movieForm.sessions[index]
  const options = cinemaOptionsByCity(session.city)
  if (!options.some((item) => String(item.id) === String(session.cinemaId))) {
    session.cinemaId = options[0]?.id || null
  }
  const cinema = cinemas.value.find((item) => String(item.id) === String(session.cinemaId))
  session.cinemaName = cinema?.name || ''
  session.city = cinema?.cityName || session.city
  const halls = cinema?.halls || []
  if (!halls.some((hall) => hall.name === session.hallName)) {
    session.hallName = halls[0]?.name || '1号厅'
  }
}

function openCinema(row) {
  resetReactive(cinemaForm, row ? { ...emptyCinema(), ...clone(row), hallCount: row.halls?.length || row.hallCount || 1 } : emptyCinema())
  cinemaDialog.value = true
}

async function saveCinema() {
  if (cinemaForm.id) await adminApi.updateCinema(cinemaForm.id, cinemaForm)
  else await adminApi.createCinema(cinemaForm)
  ElMessage.success('电影院已保存，默认座位图已生成')
  cinemaDialog.value = false
  await loadAll()
}

async function openCinemaSchedules(row) {
  activeCinema.value = row
  cinemaScheduleDialog.value = true
  cinemaScheduleSeats.value = []
  resetReactive(cinemaScheduleForm, emptyCinemaSchedule())
  cinemaScheduleForm.hallName = row.halls?.[0]?.name || ''
  cinemaScheduleForm.movieId = movies.value[0]?.id || null
  cinemaSchedules.value = await adminApi.cinemaSchedules(row.id)
}

async function saveCinemaSchedule() {
  if (!activeCinema.value) return
  await adminApi.saveCinemaSchedule(activeCinema.value.id, cinemaScheduleForm)
  ElMessage.success('排片已保存')
  cinemaSchedules.value = await adminApi.cinemaSchedules(activeCinema.value.id)
  resetReactive(cinemaScheduleForm, { ...emptyCinemaSchedule(), movieId: cinemaScheduleForm.movieId, hallName: cinemaScheduleForm.hallName })
  await loadAll()
}

async function viewCinemaScheduleSeats(row) {
  cinemaScheduleSeats.value = await adminApi.sessionSeats(row.sessionId)
}

async function createStaffUser() {
  await adminApi.createStaffUser(staffForm)
  ElMessage.success('账号已创建')
  resetReactive(staffForm, emptyStaffUser())
}

async function deleteCinema(row) {
  await ElMessageBox.confirm(`确定删除电影院「${row.name}」吗？`, '删除电影院', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await adminApi.deleteCinema(row.id)
  ElMessage.success('电影院已删除')
  await loadAll()
}

function addSessionDate() {
  const base = performanceForm.sessionDates.at(-1) || performanceForm.startTime || defaultSessionTime()
  performanceForm.sessionDates.push(addHours(base, 24))
  applyDefaultSaleRefundTimes()
}

function removeSessionDate(index) {
  performanceForm.sessionDates.splice(index, 1)
  applyDefaultSaleRefundTimes()
}

function normalizeDateTime(value) {
  const text = String(value || '').trim()
  if (!text) return ''
  if (/^\d{4}-\d{2}-\d{2}$/.test(text)) return `${text} 19:30:00`
  if (/^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}$/.test(text)) return `${text}:00`
  return text
}

function addHours(dateTime, hours) {
  const normalized = normalizeDateTime(dateTime).replace(' ', 'T')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return dateTime
  date.setHours(date.getHours() + hours)
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function defaultSessionTime() {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  date.setHours(19, 30, 0, 0)
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function parseDateTime(value) {
  const normalized = normalizeDateTime(value).replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

function formatDateTime(date) {
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function stableSaleMinute(seed) {
  let hash = 0
  for (const char of String(seed || '')) hash = ((hash * 31) + char.charCodeAt(0)) | 0
  const min = 9 * 60
  const max = 18 * 60
  return min + Math.abs(hash % (max - min + 1))
}

function defaultSaleRefundTimes(firstStartText) {
  const firstStart = parseDateTime(firstStartText)
  if (!firstStart) return null
  const minute = stableSaleMinute(normalizeDateTime(firstStartText))
  const sale = new Date(firstStart)
  sale.setMonth(sale.getMonth() - 1)
  sale.setHours(Math.floor(minute / 60), minute % 60, 0, 0)
  const free = new Date(sale)
  free.setDate(free.getDate() + 1)
  const fee = new Date(sale)
  fee.setDate(fee.getDate() + 14)
  const stop = new Date(firstStart)
  stop.setDate(stop.getDate() - 7)
  stop.setHours(sale.getHours(), sale.getMinutes(), 0, 0)
  return {
    sale: formatDateTime(sale),
    free: formatDateTime(free),
    fee: formatDateTime(fee),
    stop: formatDateTime(stop)
  }
}

function applyDefaultSaleRefundTimes(force = false) {
  const dates = [...new Set((performanceForm.sessionDates || []).map(normalizeDateTime).filter(Boolean))].sort()
  const defaults = defaultSaleRefundTimes(dates[0] || performanceForm.startTime)
  if (!defaults) return
  const assignIfAuto = (field, key) => {
    if (force || !performanceForm[field] || performanceForm[field] === autoSaleRefundDefaults[key]) {
      performanceForm[field] = defaults[key]
    }
  }
  assignIfAuto('quickSaleStartTime', 'sale')
  assignIfAuto('refundFreeUntil', 'free')
  assignIfAuto('refundFeeUntil', 'fee')
  assignIfAuto('refundStopTime', 'stop')
  Object.assign(autoSaleRefundDefaults, defaults)
}

function quickAreaName(level) {
  const venueType = selectedPerformanceVenue.value?.venueType
  if (venueType === 'STADIUM') return level.areaType === 'STANDING' ? '内场' : '看台'
  if (venueType === 'CINEMA') return '座位区'
  return '座位区'
}

function quickTicketName(level) {
  return `${quickAreaName(level)}${Number(level.price || 0)}`
}

function quickAreaColor(level) {
  if (level.areaType === 'STANDING') return '#ff6b6b'
  return selectedPerformanceVenue.value?.venueType === 'CINEMA' ? '#177e89' : '#74c0fc'
}

async function ensureQuickArea(venueId, level, index) {
  const currentAreas = await adminApi.areas(venueId)
  const areaName = quickAreaName(level)
  const existing = currentAreas.find((area) => area.areaName === areaName)
  if (existing) return existing
  return adminApi.createArea(venueId, {
    areaName,
    areaType: level.areaType || 'SEATED',
    defaultTicketLevel: level.name,
    sortOrder: index + 1,
    color: quickAreaColor(level)
  })
}

async function createQuickSessionsAndTickets(performance) {
  const rawDates = [
    ...(performanceForm.sessionDates || []).map(normalizeDateTime),
    ...String(performanceForm.sessionDatesText || '').split(/\r?\n/).map(normalizeDateTime)
  ].filter(Boolean)
  const dateRows = [...new Set(rawDates)]
  const levels = performanceForm.quickTicketLevels.filter((level) => Number(level.price || 0) > 0 && Number(level.stock || level.totalStock || 0) >= 0)
  const existingSessions = performanceSessions(performance.id)
  for (const session of existingSessions) {
    if (!dateRows.includes(normalizeDateTime(session.startTime))) {
      await adminApi.deleteSession(session.id)
    }
  }
  if (!performanceForm.venueId || !dateRows.length || !levels.length) return
  const areasByName = new Map()
  for (let i = 0; i < levels.length; i++) {
    const area = await ensureQuickArea(performanceForm.venueId, levels[i], i)
    areasByName.set(quickAreaName(levels[i]), area)
  }
  for (const startTime of dateRows) {
    const sessionPayload = {
      performanceId: performance.id,
      venueId: performanceForm.venueId,
      sessionName: `${performanceForm.title} ${startTime.slice(5, 16)}`,
      saleStartTime: normalizeDateTime(performanceForm.quickSaleStartTime || performanceForm.startTime),
      lockTime: normalizeDateTime(performanceForm.quickLockTime) || addHours(startTime, -1),
      entryTime: addHours(startTime, -1),
      startTime,
      endTime: addHours(startTime, 2),
      purchaseMode: performanceForm.saleMode
    }
    const existingSession = existingSessions.find((session) => normalizeDateTime(session.startTime) === startTime)
    const session = existingSession
      ? await adminApi.updateSession(existingSession.id, { ...existingSession, ...sessionPayload })
      : await adminApi.createSession(sessionPayload)
    await syncSessionTicketLevels(session, levels, areasByName)
    await syncFirstSaleBatch(session, levels)
    await adminApi.initSessionSeats(session.id)
  }
}

async function syncSessionTicketLevels(session, levels, areasByName) {
  const existingLevels = await adminApi.ticketLevels(session.id)
  const usedIds = new Set()
  for (const level of levels) {
    const areaId = areasByName.get(quickAreaName(level))?.id
    const price = Number(level.price || 0)
    const stock = Number(level.stock || level.totalStock || 0)
    const payload = {
      sessionId: session.id,
      areaId,
      price,
      stock,
      totalStock: stock,
      releasedStock: stock,
      unreleasedStock: 0
    }
    const matched = existingLevels.find((item) => !usedIds.has(item.id) && String(item.areaId || '') === String(areaId || '') && Number(item.price) === price)
      || existingLevels.find((item) => !usedIds.has(item.id) && Number(item.price) === price)
    if (matched) {
      usedIds.add(matched.id)
      await adminApi.updateTicketLevel(matched.id, { ...matched, ...payload })
    } else {
      const created = await adminApi.createTicketLevel(payload)
      usedIds.add(created.id)
    }
  }
  for (const level of existingLevels) {
    if (!usedIds.has(level.id)) await adminApi.deleteTicketLevel(level.id)
  }
}

async function syncFirstSaleBatch(session, levels) {
  const existingBatch = saleBatches.value.find((batch) => String(batch.sessionId) === String(session.id))
  const payload = {
    sessionId: session.id,
    batchName: existingBatch?.batchName || '第一批开售',
    saleStartTime: normalizeDateTime(performanceForm.quickSaleStartTime || performanceForm.startTime),
    lockTime: normalizeDateTime(performanceForm.quickLockTime) || addHours(session.startTime, -1),
    releaseType: 'QUANTITY',
    releaseQuantity: 0,
    purchaseLimit: 4,
    enableQueue: true,
    allowReturnDuringSale: true,
    status: existingBatch?.status || 'NOT_STARTED'
  }
  if (existingBatch) await adminApi.updateSaleBatch(existingBatch.id, { ...existingBatch, ...payload })
  else await adminApi.createSaleBatch(payload)
}

async function savePerformance() {
  syncRichEditor()
  syncCategoryName()
  await importPerformancePosterIfLocal()
  const prices = performanceForm.quickTicketLevels.map((item) => Number(item.price || 0)).filter((value) => value > 0)
  performanceForm.priceMin = prices.length ? Math.min(...prices) : 0
  performanceForm.priceMax = prices.length ? Math.max(...prices) : 0
  const dateRows = [...new Set((performanceForm.sessionDates || []).map(normalizeDateTime).filter(Boolean))].sort()
  performanceForm.sessionDates = dateRows
  performanceForm.startTime = dateRows[0] || ''
  applyDefaultSaleRefundTimes()
  const payload = {
    ...clone(performanceForm),
    tags: performanceForm.tagsText.split(',').map((item) => item.trim()).filter(Boolean),
    detailImage: performanceForm.detailImage
  }
  if (payload.id) await adminApi.updatePerformance(payload.id, payload)
  else await adminApi.createPerformance(payload)
  ElMessage.success('演出已保存')
  performanceDialog.value = false
  await loadAll()
}

async function unpublishPerformance(row) {
  await adminApi.updatePerformance(row.id, { ...row, publishStatus: 'DRAFT' })
  ElMessage.success('演出已下架为草稿')
  await loadAll()
}

async function deletePerformance(row) {
  await ElMessageBox.confirm(`确定删除演出「${row.title}」吗？删除后前台将不再展示。`, '删除演出', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await adminApi.deletePerformance(row.id)
  ElMessage.success('演出已删除')
  await loadAll()
}

function openVenue(row) {
  resetReactive(venueForm, row ? { ...emptyVenue(), ...clone(row) } : emptyVenue())
  venueDialog.value = true
}

async function saveVenue() {
  if (venueForm.id) await adminApi.updateVenue(venueForm.id, venueForm)
  else await adminApi.createVenue(venueForm)
  ElMessage.success('场馆已保存')
  venueDialog.value = false
  await loadAll()
}

async function disableVenue(row) {
  await adminApi.disableVenue(row.id)
  ElMessage.success('场馆已禁用')
  await loadAll()
}

async function deleteVenue(row) {
  await ElMessageBox.confirm(`确定删除场馆「${row.name}」吗？该场馆会从后台列表移除，座位模板也会清空。`, '删除场馆', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await adminApi.deleteVenue(row.id)
  ElMessage.success('场馆已删除')
  await loadAll()
}

function openArea(row) {
  resetReactive(areaForm, row ? { ...emptyArea(), ...clone(row) } : emptyArea())
  areaDialog.value = true
}

async function saveArea() {
  if (areaForm.id) await adminApi.updateArea(areaForm.id, areaForm)
  else await adminApi.createArea(route.params.id, areaForm)
  ElMessage.success('区域已保存')
  areaDialog.value = false
  await loadAreasForRoute()
  await loadAreasForSeatForm()
}

async function generateSeatTemplate() {
  previewSeats.value = await adminApi.generateSeats(seatForm.venueId, { ...seatForm, clearExisting: true })
  if (String(route.params.id || '') === String(seatForm.venueId)) {
    await loadVenueSeatsForRoute()
  }
  ElMessage.success(`已生成 ${previewSeats.value.length} 个座位/分区`)
}

async function clearSeatTemplate(venueId) {
  if (!venueId) return
  await ElMessageBox.confirm('确定清空该场馆的座位图吗？清空后可重新生成。', '清空座位图', {
    confirmButtonText: '清空',
    cancelButtonText: '取消',
    type: 'warning'
  })
  const result = await adminApi.clearSeats(venueId)
  previewSeats.value = []
  if (String(route.params.id || '') === String(venueId)) {
    await loadVenueSeatsForRoute()
  }
  ElMessage.success(`已清空 ${result.deleted || 0} 个座位/场次座位`)
}

function openSeatEditor(seat) {
  resetReactive(seatEditForm, { ...emptySeatEdit(), ...clone(seat) })
  seatEditDialog.value = true
}

async function saveSeatEdit() {
  await adminApi.updateSeat(seatEditForm.id, seatEditForm)
  seatEditDialog.value = false
  if (route.params.id) {
    await loadVenueSeatsForRoute()
  }
  if (String(seatForm.venueId || '') === String(seatEditForm.venueId || '')) {
    previewSeats.value = await adminApi.seats(seatForm.venueId)
  }
  ElMessage.success('座位已保存')
}

function performanceSessions(performanceId) {
  return sessions.value.filter((session) => String(session.performanceId) === String(performanceId))
}

function openSession(row) {
  resetReactive(sessionForm, row ? { ...emptySession(), ...clone(row) } : emptySession())
  sessionDialog.value = true
}

async function saveSession() {
  if (sessionForm.id) await adminApi.updateSession(sessionForm.id, sessionForm)
  else await adminApi.createSession(sessionForm)
  ElMessage.success('场次已保存')
  sessionDialog.value = false
  await loadAll()
}

async function selectSessionForLevels(row) {
  selectedSessionId.value = row.id
  await loadTicketLevels()
  router.push('/admin/ticket-level')
}

async function initSessionSeats(row) {
  await adminApi.initSessionSeats(row.id)
  ElMessage.success('场次座位已初始化')
}

async function loadTicketLevelAreas() {
  const session = sessionById(ticketLevelForm.sessionId || selectedSessionId.value)
  ticketLevelAreas.value = session?.venueId ? await adminApi.areas(session.venueId) : []
  if (!ticketLevelForm.areaId && ticketLevelAreas.value.length) ticketLevelForm.areaId = ticketLevelAreas.value[0].id
}

async function openTicketLevel(row) {
  resetReactive(ticketLevelForm, row ? { ...emptyTicketLevel(), ...clone(row) } : emptyTicketLevel())
  ticketLevelForm.stock = Number(ticketLevelForm.releasedStock || ticketLevelForm.totalStock || ticketLevelForm.stock || 0)
  ticketLevelForm.sessionId = ticketLevelForm.sessionId || selectedSessionId.value
  await loadTicketLevelAreas()
  ticketLevelDialog.value = true
}

async function saveTicketLevel() {
  const stock = Number(ticketLevelForm.stock || 0)
  ticketLevelForm.totalStock = stock
  ticketLevelForm.releasedStock = stock
  ticketLevelForm.unreleasedStock = 0
  if (ticketLevelForm.id) await adminApi.updateTicketLevel(ticketLevelForm.id, ticketLevelForm)
  else await adminApi.createTicketLevel(ticketLevelForm)
  ElMessage.success('票档已保存')
  ticketLevelDialog.value = false
  await loadTicketLevels()
}

function openBatch(row) {
  resetReactive(batchForm, row ? { ...emptyBatch(), ...clone(row) } : emptyBatch())
  batchForm.sessionId = batchForm.sessionId || selectedSessionId.value
  batchDialog.value = true
}

async function saveBatch() {
  if (batchForm.id) await adminApi.updateSaleBatch(batchForm.id, batchForm)
  else await adminApi.createSaleBatch(batchForm)
  ElMessage.success('开售批次已保存')
  batchDialog.value = false
  await loadAll()
}

async function startBatch(row) {
  await adminApi.startBatch(row.id)
  ElMessage.success('售票批次已开售')
  await loadAll()
}

async function lockBatch(row) {
  await adminApi.lockBatch(row.id)
  ElMessage.success('售票批次已锁票')
  await loadAll()
}

async function initRedis(row) {
  await adminApi.initRedisStock(row.id)
  ElMessage.success('实时库存已初始化')
}

async function approve(row) {
  await approveRefund(row.id)
  ElMessage.success('退票审核已通过')
  await loadOperations()
}

async function reject(row) {
  await rejectRefund(row.id)
  ElMessage.success('退票申请已驳回')
  await loadOperations()
}

async function verify() {
  if (!ticketCode.value) {
    ElMessage.warning('请输入票号或入场码')
    return
  }
  const result = await verifyTicket({ ticketNo: ticketCode.value })
  ElMessage.success(result.message)
  ticketCode.value = ''
  await loadOperations()
}

async function logout() {
  await user.logout()
  router.push('/')
}

watch(() => route.fullPath, () => loadAll())
watch(
  () => (performanceForm.sessionDates || []).map((item) => normalizeDateTime(item)).join('|'),
  () => {
    if (performanceDialog.value) applyDefaultSaleRefundTimes()
  }
)
onMounted(() => {
  document.addEventListener('selectionchange', handleDocumentSelectionChange)
  loadAll()
})
onBeforeUnmount(() => {
  document.removeEventListener('selectionchange', handleDocumentSelectionChange)
})
</script>

<style scoped>
.admin-order-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.admin-order-cell img {
  width: 44px;
  height: 58px;
  object-fit: cover;
  border-radius: 4px;
  background: #f3f4f6;
  flex: 0 0 auto;
}

.admin-order-cell div {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.admin-order-cell small {
  color: #6b7280;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cinema-schedule-form {
  display: grid;
  grid-template-columns: 1.3fr 1fr 1.5fr 120px auto;
  gap: 10px;
  align-items: center;
  margin-top: 14px;
}

.cinema-seat-preview {
  margin-top: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
}

.stock-filter-actions {
  grid-template-columns: repeat(3, minmax(180px, 1fr)) auto;
}
</style>


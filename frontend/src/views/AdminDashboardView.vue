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
              <img :src="row.poster" :alt="row.title" class="admin-thumb" />
            </template>
          </el-table-column>
          <el-table-column prop="title" label="演出名称" min-width="210" />
          <el-table-column prop="categoryName" label="分类" width="110" />
          <el-table-column label="城市/场馆" min-width="200">
            <template #default="{ row }">{{ row.city }} / {{ row.venue }}</template>
          </el-table-column>
          <el-table-column label="价格" width="130">
            <template #default="{ row }">¥{{ row.priceMin }} - ¥{{ row.priceMax }}</template>
          </el-table-column>
          <el-table-column label="前台状态" width="120">
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
          <el-table-column prop="address" label="地址" min-width="220" />
          <el-table-column prop="capacity" label="容量" width="90" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">{{ statusText(row.status) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="300" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openVenue(row)">编辑资料</el-button>
              <RouterLink class="table-link" :to="`/admin/venue/${row.id}/areas`">区域</RouterLink>
              <RouterLink class="table-link" :to="`/admin/venue/${row.id}/seats`">座位图</RouterLink>
              <el-button link type="danger" @click="disableVenue(row)">禁用</el-button>
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
          <el-button type="primary" @click="generateSeatTemplate">生成座位</el-button>
        </div>
        <div class="resource-form">
          <el-select v-model="seatForm.venueId" placeholder="选择场馆" @change="loadAreasForSeatForm">
            <el-option v-for="venue in venues" :key="venue.id" :label="venue.name" :value="venue.id" />
          </el-select>
          <el-select v-model="seatForm.areaId" placeholder="选择区域">
            <el-option v-for="area in seatFormAreas" :key="area.id" :label="area.areaName" :value="area.id" />
          </el-select>
          <el-input-number v-model="seatForm.rowStart" :min="1" placeholder="起始排" />
          <el-input-number v-model="seatForm.rowEnd" :min="1" placeholder="结束排" />
          <el-input-number v-model="seatForm.seatsPerRow" :min="1" placeholder="每排座位" />
          <el-input v-model="seatForm.aisleAfterSeats" placeholder="过道位置，如 8,16" />
        </div>
        <SeatSvg :seats="previewSeats" selectable />
      </section>

      <section v-else-if="activeSection === 'venue-seats'" class="admin-card">
        <div class="table-head">
          <h2>{{ selectedVenue?.name || '场馆' }}座位图</h2>
          <el-tag>点击座位查看状态</el-tag>
        </div>
        <SeatSvg :seats="venueSeats" selectable @seat-click="showSeat" />
      </section>

      <section v-else-if="activeSection === 'session'" class="admin-card">
        <div class="table-head">
          <h2>场次管理</h2>
          <el-button type="primary" @click="openSession()">新增场次</el-button>
        </div>
        <el-table :data="sessions" border empty-text="暂无场次">
          <el-table-column label="演出" min-width="180">
            <template #default="{ row }">{{ performanceTitle(row.performanceId) }}</template>
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
          <h2>票档与票价</h2>
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
          <el-table-column prop="totalStock" label="总库存" width="100" />
          <el-table-column prop="releasedStock" label="已开放" width="100" />
          <el-table-column prop="unreleasedStock" label="未开放" width="100" />
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
          <h2>开售批次</h2>
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
          <el-table-column prop="releaseQuantity" label="数量" width="90" />
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
          <h2>库存池</h2>
          <el-tag type="info">锁票后回收和退票待释放库存</el-tag>
        </div>
        <el-table :data="stockPool" border empty-text="暂无库存">
          <el-table-column prop="sessionId" label="场次" width="100" />
          <el-table-column prop="ticketLevelId" label="票档" width="100" />
          <el-table-column label="来源">
            <template #default="{ row }">{{ sourceTypeText(row.sourceType) }}</template>
          </el-table-column>
          <el-table-column label="库存状态">
            <template #default="{ row }">{{ statusText(row.stockStatus) }}</template>
          </el-table-column>
          <el-table-column label="可用于下轮" width="140">
            <template #default="{ row }">{{ row.availableForNextBatch ? '可用' : '不可用' }}</template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeSection === 'refunds'" class="admin-card">
        <div class="table-head">
          <h2>退票审核</h2>
          <el-button type="primary" plain @click="loadOperations">刷新</el-button>
        </div>
        <el-table :data="refunds" border empty-text="暂无退票">
          <el-table-column prop="orderId" label="订单" width="100" />
          <el-table-column label="金额" width="120">
            <template #default="{ row }">¥{{ row.amount }}</template>
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

      <section v-else-if="activeSection === 'reports'" class="admin-card">
        <div class="table-head">
          <h2>统计报表</h2>
          <el-button type="primary" plain @click="loadOperations">刷新</el-button>
        </div>
        <div class="metric-grid">
          <div class="metric"><span>订单数</span><strong>{{ statistics.orderCount }}</strong></div>
          <div class="metric"><span>销售额</span><strong>¥{{ statistics.salesAmount }}</strong></div>
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
          <el-select v-model="performanceForm.saleStatus">
            <el-option label="正在售票" value="ON_SALE" />
            <el-option label="即将开售" value="COMING_SOON" />
            <el-option label="票量紧张" value="RETURNED" />
            <el-option label="已结束" value="LOCKED" />
          </el-select>
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
        <el-form-item label="首场时间"><el-input v-model="performanceForm.startTime" placeholder="2026-08-18 19:30" /></el-form-item>
        <el-form-item label="最低价"><el-input-number v-model="performanceForm.priceMin" :min="0" /></el-form-item>
        <el-form-item label="最高价"><el-input-number v-model="performanceForm.priceMax" :min="0" /></el-form-item>
        <el-form-item label="标签，逗号分隔" class="span-2"><el-input v-model="performanceForm.tagsText" /></el-form-item>
        <el-form-item label="海报" class="span-2">
          <div class="upload-row">
            <img v-if="performanceForm.poster" :src="performanceForm.poster" alt="海报预览" class="poster-preview" />
            <input type="file" accept="image/*" @change="setPerformancePoster" />
            <el-input v-model="performanceForm.poster" placeholder="也可以直接填写图片地址" />
          </div>
        </el-form-item>
        <el-form-item label="列表摘要" class="span-2"><el-input v-model="performanceForm.summary" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="演出介绍"><el-input v-model="performanceForm.intro" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="演职人员"><el-input v-model="performanceForm.artistInfo" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="场馆介绍"><el-input v-model="performanceForm.venueIntro" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="购票须知"><el-input v-model="performanceForm.purchaseNotice" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="退票规则"><el-input v-model="performanceForm.refundRule" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="观演须知"><el-input v-model="performanceForm.entryRule" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <div class="detail-builder">
        <div class="table-head">
          <h3>详情页排版</h3>
          <div class="head-actions">
            <el-button @click="addDetailBlock('HEADING')">标题</el-button>
            <el-button @click="addDetailBlock('PARAGRAPH')">文字</el-button>
            <el-button @click="addDetailBlock('IMAGE')">图片</el-button>
          </div>
        </div>
        <div v-for="(block, index) in performanceForm.detailBlocks" :key="index" class="detail-block-editor">
          <el-select v-model="block.type">
            <el-option label="标题" value="HEADING" />
            <el-option label="段落" value="PARAGRAPH" />
            <el-option label="图片" value="IMAGE" />
          </el-select>
          <template v-if="block.type === 'IMAGE'">
            <img v-if="block.content" :src="block.content" alt="详情图预览" class="detail-preview" />
            <input type="file" accept="image/*" @change="setBlockImage(index, $event)" />
            <el-input v-model="block.content" placeholder="图片地址或上传图片" />
          </template>
          <el-input v-else v-model="block.content" type="textarea" :rows="block.type === 'HEADING' ? 1 : 3" />
          <div class="block-actions">
            <el-button :disabled="index === 0" @click="moveBlock(index, -1)">上移</el-button>
            <el-button :disabled="index === performanceForm.detailBlocks.length - 1" @click="moveBlock(index, 1)">下移</el-button>
            <el-button type="danger" @click="removeBlock(index)">删除</el-button>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="performanceDialog = false">取消</el-button>
        <el-button type="primary" @click="savePerformance">保存演出</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="venueDialog" :title="venueForm.id ? '编辑场馆' : '新增场馆'" width="680px">
      <el-form label-position="top" class="dialog-form">
        <el-form-item label="场馆名称"><el-input v-model="venueForm.name" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="venueForm.cityName" /></el-form-item>
        <el-form-item label="详细地址"><el-input v-model="venueForm.address" /></el-form-item>
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
        <el-form-item label="开售时间"><el-input v-model="sessionForm.saleStartTime" placeholder="2026-07-20 10:00:00" /></el-form-item>
        <el-form-item label="锁票时间"><el-input v-model="sessionForm.lockTime" placeholder="2026-08-01 18:00:00" /></el-form-item>
        <el-form-item label="入场时间"><el-input v-model="sessionForm.entryTime" /></el-form-item>
        <el-form-item label="开始时间"><el-input v-model="sessionForm.startTime" /></el-form-item>
        <el-form-item label="结束时间"><el-input v-model="sessionForm.endTime" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sessionDialog = false">取消</el-button>
        <el-button type="primary" @click="saveSession">保存场次</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="ticketLevelDialog" :title="ticketLevelForm.id ? '编辑票档' : '新增票档'" width="620px">
      <el-form label-position="top" class="dialog-form">
        <el-form-item label="票档名称"><el-input v-model="ticketLevelForm.name" /></el-form-item>
        <el-form-item label="关联区域">
          <el-select v-model="ticketLevelForm.areaId" placeholder="选择区域">
            <el-option v-for="area in ticketLevelAreas" :key="area.id" :label="area.areaName" :value="area.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格"><el-input-number v-model="ticketLevelForm.price" :min="0" /></el-form-item>
        <el-form-item label="总库存"><el-input-number v-model="ticketLevelForm.totalStock" :min="0" /></el-form-item>
        <el-form-item label="已开放库存"><el-input-number v-model="ticketLevelForm.releasedStock" :min="0" /></el-form-item>
        <el-form-item label="未开放库存"><el-input-number v-model="ticketLevelForm.unreleasedStock" :min="0" /></el-form-item>
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
        <el-form-item label="开售时间"><el-input v-model="batchForm.saleStartTime" /></el-form-item>
        <el-form-item label="锁票时间"><el-input v-model="batchForm.lockTime" /></el-form-item>
        <el-form-item label="开放方式">
          <el-select v-model="batchForm.releaseType">
            <el-option label="全部开放" value="FULL" />
            <el-option label="按数量开放" value="QUANTITY" />
            <el-option label="按比例开放" value="RATIO" />
          </el-select>
        </el-form-item>
        <el-form-item label="开放数量"><el-input-number v-model="batchForm.releaseQuantity" :min="0" /></el-form-item>
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
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { http } from '../api/http'
import { adminApi } from '../api/adminResources'
import { getCategories } from '../api/portal'
import { approveRefund, getAdminRefunds, getCheckins, getRiskLogs, getStatisticsOverview, rejectRefund, verifyTicket } from '../api/operations'
import { useUserStore } from '../stores/user'

const SeatSvg = {
  props: { seats: { type: Array, default: () => [] }, selectable: Boolean },
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
    return () => h('div', { class: 'seat-map-wrap' }, [
      h('div', { class: 'stage-line' }, '舞台 / 银幕'),
      h('svg', { viewBox: '0 0 760 560', class: 'seat-map' },
        props.seats.map((seat) => h('circle', {
          cx: seat.x,
          cy: seat.y,
          r: 8,
          fill: color(seat.status),
          class: props.selectable ? 'seat-dot clickable' : 'seat-dot',
          onClick: () => emit('seat-click', seat)
        }, [h('title', seat.seatLabel || `${seat.rowNo}排${seat.seatNo}座`)]))
      ),
      h('div', { class: 'seat-legend' }, [
        ['AVAILABLE', '可售'], ['LOCKED', '锁定'], ['SOLD', '已售'], ['DISABLED', '不可售'], ['UNRELEASED', '未开放'], ['POST_LOCK_RETURNED', '锁票回收']
      ].map(([status, label]) => h('span', [h('i', { style: { background: color(status) } }), label])))
    ])
  }
}

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const metrics = reactive({ performanceCount: 0, movieCount: 0, orderCount: 0, ticketCount: 0, refundPending: 0, checkinToday: 0 })
const checkerMetrics = reactive({ checkinToday: 0, successRate: '0%', latestResult: '暂无数据' })
const performances = ref([])
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
const refunds = ref([])
const checkins = ref([])
const riskLogs = ref([])
const statistics = reactive({ orderCount: 0, salesAmount: 0, ticketCount: 0, refundCount: 0, checkinCount: 0, rushSuccessRate: '0%' })
const ticketCode = ref('')
const selectedSessionId = ref(null)
const seatFormAreas = ref([])
const seatForm = reactive({ venueId: 1, areaId: 1, rowStart: 1, rowEnd: 3, seatsPerRow: 12, startX: 60, startY: 80, gapX: 30, gapY: 30, aisleAfterSeats: '6' })

const performanceDialog = ref(false)
const venueDialog = ref(false)
const areaDialog = ref(false)
const sessionDialog = ref(false)
const ticketLevelDialog = ref(false)
const batchDialog = ref(false)
const performanceForm = reactive(emptyPerformance())
const venueForm = reactive(emptyVenue())
const areaForm = reactive(emptyArea())
const sessionForm = reactive(emptySession())
const ticketLevelForm = reactive(emptyTicketLevel())
const batchForm = reactive(emptyBatch())

const roleMap = { ADMIN: '系统管理员', MANAGER: '票务管理员', CHECKER: '检票员' }
const menus = [
  { key: 'overview', title: '运营概览', icon: 'DataAnalysis', path: '/admin', roles: ['ADMIN', 'MANAGER'] },
  { key: 'performance', title: '演出发布', icon: 'Management', path: '/admin/performance', roles: ['ADMIN', 'MANAGER'] },
  { key: 'venue', title: '场馆管理', icon: 'Location', path: '/admin/venue', roles: ['ADMIN', 'MANAGER'] },
  { key: 'seat-template', title: '座位模板', icon: 'Grid', path: '/admin/seat-template', roles: ['ADMIN', 'MANAGER'] },
  { key: 'session', title: '场次管理', icon: 'Calendar', path: '/admin/session', roles: ['ADMIN', 'MANAGER'] },
  { key: 'ticket-level', title: '票档票价', icon: 'Tickets', path: '/admin/ticket-level', roles: ['ADMIN', 'MANAGER'] },
  { key: 'sale-batch', title: '开售批次', icon: 'Clock', path: '/admin/sale-batch', roles: ['ADMIN', 'MANAGER'] },
  { key: 'stock-pool', title: '库存池', icon: 'Box', path: '/admin/stock-pool', roles: ['ADMIN', 'MANAGER'] },
  { key: 'refunds', title: '退票审核', icon: 'RefreshLeft', path: '/admin/refunds', roles: ['ADMIN', 'MANAGER'] },
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
  ON_SALE: '正在售票',
  COMING_SOON: '即将开售',
  RETURNED: '票量紧张',
  LOCKED: '已锁票/结束',
  FINISHED: '已结束',
  AVAILABLE: '可售',
  UNRELEASED: '未开放',
  SOLD: '已售',
  POST_LOCK_RETURNED: '锁票回收',
  REFUND_WAITING_RELEASE: '退票待释放',
  WAITING_RELEASE: '待释放',
  CHECKED_IN: '已核验'
}
const purchaseModeMap = { SELECTABLE: '自主选座', AUTO_ALLOCATE: '系统配座' }
const releaseTypeMap = { FULL: '全部开放', PARTIAL: '分批开放', MANUAL: '手动开放', QUANTITY: '按数量', RATIO: '按比例' }
const areaTypeMap = { SEATED: '有座区域', STANDING: '站席区域' }
const sourceTypeMap = { POST_LOCK_RETURNED: '锁票回收', POST_LOCK_RETURN: '锁票回收', REFUND_WAITING_RELEASE: '退票待释放', REFUND_RETURN: '退票回流', UNRELEASED: '未开放库存', MANUAL_ADD: '人工调整' }

const textFromMap = (map, value) => map[value] || value || '暂无数据'
const statusText = (value) => textFromMap(statusMap, value)
const purchaseModeText = (value) => textFromMap(purchaseModeMap, value)
const releaseTypeText = (value) => textFromMap(releaseTypeMap, value)
const areaTypeText = (value) => textFromMap(areaTypeMap, value)
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
const selectedVenue = computed(() => venues.value.find((item) => String(item.id) === String(route.params.id || seatForm.venueId)))

const clone = (value) => JSON.parse(JSON.stringify(value || {}))
const sessionById = (id) => sessions.value.find((item) => String(item.id) === String(id))
const performanceTitle = (id) => performances.value.find((item) => String(item.id) === String(id))?.title || `演出 ${id || ''}`
const venueName = (id) => venues.value.find((item) => String(item.id) === String(id))?.name || `场馆 ${id || ''}`
const areaName = (id) => [...areas.value, ...seatFormAreas.value, ...ticketLevelAreas.value].find((item) => String(item.id) === String(id))?.areaName || `区域 ${id || ''}`
const sessionLabel = (session) => session ? `${performanceTitle(session.performanceId)} / ${session.sessionName}` : '未选择场次'

async function loadAll() {
  if (user.canUseAdminApi) {
    Object.assign(metrics, await adminApi.dashboard())
    const [categoryRows, performanceRows, venueRows, sessionRows, batchRows, poolRows] = await Promise.all([
      getCategories(),
      adminApi.performances(),
      adminApi.venues(),
      adminApi.sessions(),
      adminApi.saleBatches(),
      adminApi.stockPool()
    ])
    categories.value = categoryRows
    performances.value = performanceRows
    venues.value = venueRows
    sessions.value = sessionRows
    saleBatches.value = batchRows
    stockPool.value = poolRows
    if (!selectedSessionId.value && sessions.value.length) selectedSessionId.value = sessions.value[0].id
    if (!seatForm.venueId && venues.value.length) seatForm.venueId = venues.value[0].id
    await Promise.all([loadTicketLevels(), loadAreasForRoute(), loadVenueSeatsForRoute(), loadAreasForSeatForm()])
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
    startTime: '2026-08-18 19:30',
    priceMin: 180,
    priceMax: 680,
    poster: '/uploads/posters/performance/poster-101.svg',
    banner: '',
    detailImage: '',
    saleStatus: 'COMING_SOON',
    saleMode: 'SELECTABLE',
    publishStatus: 'DRAFT',
    tagsText: '实名制,电子票,限购2张',
    summary: '',
    intro: '',
    artistInfo: '',
    venueIntro: '',
    purchaseNotice: '',
    refundRule: '',
    entryRule: '',
    detailBlocks: [
      { type: 'HEADING', content: '项目介绍' },
      { type: 'PARAGRAPH', content: '' }
    ]
  }
}

function emptyVenue() {
  return { id: null, name: '', cityName: '上海', address: '', capacity: 0, description: '', status: 'ENABLED' }
}

function emptyArea() {
  return { id: null, areaName: '', areaType: 'SEATED', defaultTicketLevel: '标准票', sortOrder: 1, color: '#d9303e' }
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
  return { id: null, sessionId: selectedSessionId.value, name: '标准票', price: 180, areaId: null, totalStock: 0, releasedStock: 0, unreleasedStock: 0, status: 'ENABLED' }
}

function emptyBatch() {
  return { id: null, sessionId: selectedSessionId.value, batchName: '第一轮开售', saleStartTime: '2026-07-20 10:00:00', lockTime: '2026-08-01 18:00:00', releaseType: 'QUANTITY', releaseQuantity: 50, releaseRatio: 0, purchaseLimit: 2, enableQueue: true, allowReturnDuringSale: true, status: 'NOT_STARTED' }
}

function resetReactive(target, source) {
  Object.keys(target).forEach((key) => delete target[key])
  Object.assign(target, source)
}

function openPerformance(row) {
  const next = row ? clone(row) : emptyPerformance()
  next.tagsText = (next.tags || []).join(',') || next.tagsText || ''
  if (!next.detailBlocks?.length) {
    next.detailBlocks = [
      { type: 'HEADING', content: '项目介绍' },
      { type: 'PARAGRAPH', content: next.intro || next.summary || '' }
    ]
  }
  resetReactive(performanceForm, { ...emptyPerformance(), ...next })
  performanceDialog.value = true
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
}

async function uploadSelectedImage(event, callback) {
  const file = event.target.files?.[0]
  if (!file) return
  try {
    const result = await adminApi.uploadImage(file)
    callback(result.path)
  } finally {
    event.target.value = ''
  }
}

function setPerformancePoster(event) {
  uploadSelectedImage(event, (value) => {
    performanceForm.poster = value
    performanceForm.banner = value
  })
}

function setBlockImage(index, event) {
  uploadSelectedImage(event, (value) => {
    performanceForm.detailBlocks[index].content = value
    performanceForm.detailBlocks[index].imagePath = value
    if (!performanceForm.detailImage) performanceForm.detailImage = value
  })
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

async function savePerformance() {
  syncCategoryName()
  const payload = {
    ...clone(performanceForm),
    tags: performanceForm.tagsText.split(',').map((item) => item.trim()).filter(Boolean),
    detailImage: performanceForm.detailBlocks.find((item) => item.type === 'IMAGE' && item.content)?.content || performanceForm.detailImage
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
  await adminApi.deleteVenue(row.id)
  ElMessage.success('场馆已禁用')
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
  previewSeats.value = await adminApi.generateSeats(seatForm.venueId, seatForm)
  ElMessage.success(`已生成 ${previewSeats.value.length} 个座位`)
}

const showSeat = (seat) => ElMessage.info(`${seat.seatLabel}: ${statusText(seat.status)}`)

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
  ticketLevelForm.sessionId = ticketLevelForm.sessionId || selectedSessionId.value
  await loadTicketLevelAreas()
  ticketLevelDialog.value = true
}

async function saveTicketLevel() {
  const total = Number(ticketLevelForm.totalStock || 0)
  const released = Number(ticketLevelForm.releasedStock || 0)
  ticketLevelForm.unreleasedStock = Math.max(0, total - released)
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
onMounted(() => loadAll())
</script>

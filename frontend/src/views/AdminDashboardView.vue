<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <RouterLink class="admin-brand" to="/admin">
        <span class="brand-mark">T</span>
        <strong>鏄熺エ鍚庡彴</strong>
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
          <p class="eyebrow">TicketMarket 绠＄悊鍚庡彴</p>
          <h1>{{ activeMenu?.title || '杩愯惀姒傝' }}</h1>
        </div>
        <div class="admin-userbar">
          <span>{{ user.profile?.nickname || user.profile?.username }}</span>
          <el-tag effect="plain">{{ roleText }}</el-tag>
          <RouterLink to="/">杩斿洖鍓嶅彴</RouterLink>
          <el-button size="small" @click="logout">閫€鍑虹櫥褰</el-button>
        </div>
      </header>

      <template v-if="activeSection === 'overview'">
        <div class="metric-grid">
          <div class="metric"><span>婕斿嚭妗ｆ</span><strong>{{ metrics.performanceCount }}</strong></div>
          <div class="metric"><span>鍦洪</span><strong>{{ venues.length }}</strong></div>
          <div class="metric"><span>鍦烘</span><strong>{{ sessions.length }}</strong></div>
          <div class="metric"><span>鍞エ鎵规</span><strong>{{ saleBatches.length }}</strong></div>
          <div class="metric"><span>璁㈠崟</span><strong>{{ metrics.orderCount }}</strong></div>
          <div class="metric"><span>寰呭閫€绁</span><strong>{{ metrics.refundPending }}</strong></div>
        </div>
        <section class="admin-card">
          <div class="table-head">
            <h2>鍙戝竷娴佺▼</h2>
            <el-button type="primary" plain @click="loadAll">鍒锋柊</el-button>
          </div>
          <div class="admin-flow">
            <span>1. 寤哄満棣嗗拰鍖哄煙</span>
            <span>2. 鐢熸垚搴т綅鍥</span>
            <span>3. 鍙戝竷婕斿嚭妗ｆ</span>
            <span>4. 缁戝畾鍦烘</span>
            <span>5. 璁剧疆绁ㄤ环鍜屽簱瀛</span>
            <span>6. 閰嶇疆寮€鍞壒娆</span>
          </div>
        </section>
      </template>

      <section v-else-if="activeSection === 'performance'" class="admin-card">
        <div class="table-head">
          <h2>婕斿嚭鍙戝竷</h2>
          <el-button type="primary" @click="openPerformance()">鏂板缓婕斿嚭</el-button>
        </div>
        <el-table :data="performances" border empty-text="鏆傛棤婕斿嚭">
          <el-table-column label="娴锋姤" width="92">
            <template #default="{ row }">
              <img :src="row.poster" :alt="row.title" class="admin-thumb" />
            </template>
          </el-table-column>
          <el-table-column prop="title" label="婕斿嚭鍚嶇О" min-width="210" />
          <el-table-column prop="categoryName" label="鍒嗙被" width="110" />
          <el-table-column label="鍩庡競/鍦洪" min-width="200">
            <template #default="{ row }">{{ row.city }} / {{ row.venue }}</template>
          </el-table-column>
          <el-table-column label="浠锋牸" width="130">
            <template #default="{ row }">楼{{ row.priceMin }} - 楼{{ row.priceMax }}</template>
          </el-table-column>
          <el-table-column label="鍓嶅彴鐘舵€" width="120">
            <template #default="{ row }">{{ statusText(row.saleStatus) }}</template>
          </el-table-column>
          <el-table-column label="鍙戝竷" width="100">
            <template #default="{ row }">
              <el-tag :type="row.publishStatus === 'PUBLISHED' ? 'success' : 'info'" effect="plain">
                {{ row.publishStatus === 'PUBLISHED' ? '已发布' : '草稿' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="鎿嶄綔" width="280" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPerformance(row)">缂栬緫鍏ㄩ儴缁嗚妭</el-button>
              <RouterLink class="table-link" :to="`/performances/${row.id}`">棰勮</RouterLink>
              <el-button link type="danger" @click="unpublishPerformance(row)">涓嬫灦</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeSection === 'venue'" class="admin-card">
        <div class="table-head">
          <h2>鍦洪绠＄悊</h2>
          <el-button type="primary" @click="openVenue()">鏂板鍦洪</el-button>
        </div>
        <el-table :data="venues" border empty-text="鏆傛棤鍦洪">
          <el-table-column prop="name" label="鍦洪" min-width="150" />
          <el-table-column prop="cityName" label="鍩庡競" width="100" />
          <el-table-column prop="address" label="鍦板潃" min-width="220" />
          <el-table-column prop="capacity" label="瀹归噺" width="90" />
          <el-table-column label="鐘舵€" width="100">
            <template #default="{ row }">{{ statusText(row.status) }}</template>
          </el-table-column>
          <el-table-column label="鎿嶄綔" width="300" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openVenue(row)">缂栬緫璧勬枡</el-button>
              <RouterLink class="table-link" :to="`/admin/venue/${row.id}/areas`">鍖哄煙</RouterLink>
              <RouterLink class="table-link" :to="`/admin/venue/${row.id}/seats`">搴т綅鍥</RouterLink>
              <el-button link type="danger" @click="disableVenue(row)">绂佺敤</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeSection === 'venue-areas'" class="admin-card">
        <div class="table-head">
          <h2>{{ selectedVenue?.name || '鍦洪' }}鍖哄煙</h2>
          <el-button type="primary" @click="openArea()">鏂板鍖哄煙</el-button>
        </div>
        <el-table :data="areas" border empty-text="鏆傛棤鍖哄煙">
          <el-table-column prop="areaName" label="鍖哄煙" />
          <el-table-column label="绫诲瀷" width="120">
            <template #default="{ row }">{{ areaTypeText(row.areaType) }}</template>
          </el-table-column>
          <el-table-column prop="defaultTicketLevel" label="榛樿绁ㄦ。" width="140" />
          <el-table-column prop="sortOrder" label="鎺掑簭" width="90" />
          <el-table-column label="棰滆壊" width="100">
            <template #default="{ row }"><span class="color-swatch" :style="{ background: row.color }" /></template>
          </el-table-column>
          <el-table-column label="鎿嶄綔" width="140">
            <template #default="{ row }">
              <el-button link type="primary" @click="openArea(row)">缂栬緫</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeSection === 'seat-template'" class="admin-card">
        <div class="table-head">
          <h2>搴т綅鍥剧敓鎴愬櫒</h2>
          <el-button type="primary" @click="generateSeatTemplate">鐢熸垚搴т綅</el-button>
        </div>
        <div class="resource-form">
          <el-select v-model="seatForm.venueId" placeholder="閫夋嫨鍦洪" @change="loadAreasForSeatForm">
            <el-option v-for="venue in venues" :key="venue.id" :label="venue.name" :value="venue.id" />
          </el-select>
          <el-select v-model="seatForm.layoutType" placeholder="座位图模板">
            <el-option label="标准排座" value="STANDARD" />
            <el-option label="体育场馆 / 舞台环形看台" value="STADIUM" />
          </el-select>
          <el-select v-model="seatForm.areaId" placeholder="閫夋嫨鍖哄煙">
            <el-option v-for="area in seatFormAreas" :key="area.id" :label="area.areaName" :value="area.id" />
          </el-select>
          <el-input-number v-model="seatForm.rowStart" :min="1" placeholder="起始排" />
          <el-input-number v-model="seatForm.rowEnd" :min="1" placeholder="结束排" />
          <el-input-number v-model="seatForm.seatsPerRow" :min="1" placeholder="姣忔帓搴т綅" />
          <el-input v-model="seatForm.aisleAfterSeats" placeholder="杩囬亾浣嶇疆锛屽 8,16" />
        </div>
        <SeatSvg :seats="previewSeats" :venue="venues.find((venue) => venue.id === seatForm.venueId)" selectable />
      </section>

      <section v-else-if="activeSection === 'venue-seats'" class="admin-card">
        <div class="table-head">
          <h2>{{ selectedVenue?.name || '鍦洪' }}搴т綅鍥</h2>
          <el-tag>鐐瑰嚮搴т綅鏌ョ湅鐘舵€</el-tag>
        </div>
        <SeatSvg :seats="venueSeats" :venue="selectedVenue" selectable @seat-click="showSeat" />
      </section>

      <section v-else-if="activeSection === 'session'" class="admin-card">
        <div class="table-head">
          <h2>鍦烘绠＄悊</h2>
          <el-button type="primary" @click="openSession()">鏂板鍦烘</el-button>
        </div>
        <el-table :data="sessions" border empty-text="鏆傛棤鍦烘">
          <el-table-column label="婕斿嚭" min-width="180">
            <template #default="{ row }">{{ performanceTitle(row.performanceId) }}</template>
          </el-table-column>
          <el-table-column prop="sessionName" label="鍦烘" min-width="180" />
          <el-table-column label="鍦洪" min-width="150">
            <template #default="{ row }">{{ venueName(row.venueId) }}</template>
          </el-table-column>
          <el-table-column prop="saleStartTime" label="寮€鍞椂闂" width="170" />
          <el-table-column prop="startTime" label="婕斿嚭鏃堕棿" width="170" />
          <el-table-column label="妯″紡" width="130">
            <template #default="{ row }">{{ purchaseModeText(row.purchaseMode) }}</template>
          </el-table-column>
          <el-table-column label="鎿嶄綔" width="210" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openSession(row)">缂栬緫</el-button>
              <el-button link type="success" @click="selectSessionForLevels(row)">绁ㄤ环</el-button>
              <el-button link type="warning" @click="initSessionSeats(row)">鍒濆鍖栧骇浣</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeSection === 'ticket-level'" class="admin-card">
        <div class="table-head">
          <h2>绁ㄦ。涓庣エ浠</h2>
          <div class="head-actions">
            <el-select v-model="selectedSessionId" placeholder="閫夋嫨鍦烘" @change="loadTicketLevels">
              <el-option v-for="session in sessions" :key="session.id" :label="sessionLabel(session)" :value="session.id" />
            </el-select>
            <el-button type="primary" :disabled="!selectedSessionId" @click="openTicketLevel()">鏂板绁ㄦ。</el-button>
          </div>
        </div>
        <el-table :data="ticketLevels" border empty-text="璇峰厛閫夋嫨鍦烘">
          <el-table-column prop="name" label="绁ㄦ。" />
          <el-table-column label="鍖哄煙" width="140">
            <template #default="{ row }">{{ areaName(row.areaId) }}</template>
          </el-table-column>
          <el-table-column prop="price" label="浠锋牸" width="120" />
          <el-table-column prop="totalStock" label="鎬诲簱瀛" width="100" />
          <el-table-column prop="releasedStock" label="宸插紑鏀" width="100" />
          <el-table-column prop="unreleasedStock" label="鏈紑鏀" width="100" />
          <el-table-column prop="soldStock" label="宸插敭" width="90" />
          <el-table-column label="鎿嶄綔" width="130">
            <template #default="{ row }">
              <el-button link type="primary" @click="openTicketLevel(row)">缂栬緫</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeSection === 'sale-batch'" class="admin-card">
        <div class="table-head">
          <h2>寮€鍞壒娆</h2>
          <div class="head-actions">
            <el-select v-model="selectedSessionId" placeholder="閫夋嫨鍦烘" @change="loadTicketLevels">
              <el-option v-for="session in sessions" :key="session.id" :label="sessionLabel(session)" :value="session.id" />
            </el-select>
            <el-button type="primary" :disabled="!selectedSessionId" @click="openBatch()">鍒涘缓鎵规</el-button>
          </div>
        </div>
        <el-table :data="saleBatches" border empty-text="鏆傛棤鎵规">
          <el-table-column label="鍦烘" min-width="180">
            <template #default="{ row }">{{ sessionLabel(sessionById(row.sessionId)) }}</template>
          </el-table-column>
          <el-table-column prop="batchName" label="鎵规" min-width="160" />
          <el-table-column prop="saleStartTime" label="寮€鍞椂闂" width="170" />
          <el-table-column prop="lockTime" label="閿佺エ鏃堕棿" width="170" />
          <el-table-column label="寮€鏀炬柟寮" width="110">
            <template #default="{ row }">{{ releaseTypeText(row.releaseType) }}</template>
          </el-table-column>
          <el-table-column prop="releaseQuantity" label="鏁伴噺" width="90" />
          <el-table-column label="鐘舵€" width="100">
            <template #default="{ row }">{{ statusText(row.status) }}</template>
          </el-table-column>
          <el-table-column label="鎿嶄綔" width="260" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openBatch(row)">缂栬緫</el-button>
              <el-button link type="success" @click="startBatch(row)">寮€鍞</el-button>
              <el-button link type="warning" @click="lockBatch(row)">閿佺エ</el-button>
              <el-button link type="info" @click="initRedis(row)">鍒濆鍖栧簱瀛</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeSection === 'stock-pool'" class="admin-card">
        <div class="table-head">
          <h2>搴撳瓨姹</h2>
          <el-tag type="info">閿佺エ鍚庡洖鏀跺拰閫€绁ㄥ緟閲婃斁搴撳瓨</el-tag>
        </div>
        <el-table :data="stockPool" border empty-text="鏆傛棤搴撳瓨">
          <el-table-column prop="sessionId" label="鍦烘" width="100" />
          <el-table-column prop="ticketLevelId" label="绁ㄦ。" width="100" />
          <el-table-column label="鏉ユ簮">
            <template #default="{ row }">{{ sourceTypeText(row.sourceType) }}</template>
          </el-table-column>
          <el-table-column label="库存状态">
            <template #default="{ row }">{{ statusText(row.stockStatus) }}</template>
          </el-table-column>
          <el-table-column label="鍙敤浜庝笅杞" width="140">
            <template #default="{ row }">{{ row.availableForNextBatch ? '可用' : '不可用' }}</template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeSection === 'refunds'" class="admin-card">
        <div class="table-head">
          <h2>閫€绁ㄥ鏍</h2>
          <el-button type="primary" plain @click="loadOperations">鍒锋柊</el-button>
        </div>
        <el-table :data="refunds" border empty-text="暂无退票">
          <el-table-column prop="orderId" label="璁㈠崟" width="100" />
          <el-table-column label="閲戦" width="120">
            <template #default="{ row }">楼{{ row.amount }}</template>
          </el-table-column>
          <el-table-column label="鐘舵€" width="120">
            <template #default="{ row }">{{ refundStatusText(row.status) }}</template>
          </el-table-column>
          <el-table-column prop="message" label="璇存槑" />
          <el-table-column label="鎿嶄綔" width="160">
            <template #default="{ row }">
              <el-button v-if="row.status === 'APPLYING'" link type="primary" @click="approve(row)">閫氳繃</el-button>
              <el-button v-if="row.status === 'APPLYING'" link type="danger" @click="reject(row)">椹冲洖</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-else-if="activeSection === 'reports'" class="admin-card">
        <div class="table-head">
          <h2>缁熻鎶ヨ〃</h2>
          <el-button type="primary" plain @click="loadOperations">鍒锋柊</el-button>
        </div>
        <div class="metric-grid">
          <div class="metric"><span>璁㈠崟鏁</span><strong>{{ statistics.orderCount }}</strong></div>
          <div class="metric"><span>閿€鍞</span><strong>楼{{ statistics.salesAmount }}</strong></div>
          <div class="metric"><span>鐢靛瓙绁</span><strong>{{ statistics.ticketCount }}</strong></div>
          <div class="metric"><span>閫€绁</span><strong>{{ statistics.refundCount }}</strong></div>
        </div>
      </section>

      <template v-else-if="activeSection === 'checkin'">
        <div class="metric-grid">
          <div class="metric"><span>浠婃棩鏍搁獙</span><strong>{{ checkerMetrics.checkinToday }}</strong></div>
          <div class="metric"><span>閫氳繃鐜</span><strong>{{ checkerMetrics.successRate }}</strong></div>
          <div class="metric wide"><span>閫氶亾鐘舵€</span><strong>{{ checkerMetrics.latestResult }}</strong></div>
        </div>
        <section class="admin-card">
          <h2>妫€绁ㄧ鐞</h2>
          <div class="resource-form compact">
            <el-input v-model="ticketCode" placeholder="杈撳叆绁ㄥ彿鎴栧叆鍦虹爜" />
            <el-button type="primary" @click="verify">鏍搁獙</el-button>
          </div>
          <el-table :data="checkins" border empty-text="鏆傛棤鏍搁獙">
            <el-table-column prop="ticketNo" label="绁ㄥ彿" />
            <el-table-column prop="message" label="鏍搁獙缁撴灉" />
            <el-table-column prop="createdAt" label="鏃堕棿" width="180" />
          </el-table>
        </section>
      </template>

      <section v-else-if="activeSection === 'risk-logs'" class="admin-card">
        <div class="table-head">
          <h2>椋庢帶鏃ュ織</h2>
          <el-button type="primary" plain @click="loadOperations">鍒锋柊</el-button>
        </div>
        <el-table :data="riskLogs" border empty-text="鏆傛棤鏃ュ織">
          <el-table-column prop="action" label="绫诲瀷" width="140" />
          <el-table-column prop="detail" label="鍐呭" />
          <el-table-column prop="createdAt" label="鏃堕棿" width="180" />
        </el-table>
      </section>

      <section v-else class="admin-card empty-admin">
        <el-icon><FolderOpened /></el-icon>
        <h2>{{ activeMenu?.title }}</h2>
        <p>璇ユā鍧楁殏鏈帴鍏ユ暟鎹</p>
      </section>
    </section>

    <el-dialog v-model="performanceDialog" :title="performanceForm.id ? '缂栬緫婕斿嚭' : '鏂板缓婕斿嚭'" width="980px" destroy-on-close>
      <el-form label-position="top" class="admin-editor-grid">
        <el-form-item label="婕斿嚭鍚嶇О"><el-input v-model="performanceForm.title" /></el-form-item>
        <el-form-item label="短标题/副标题"><el-input v-model="performanceForm.subtitle" /></el-form-item>
        <el-form-item label="鍒嗙被">
          <el-select v-model="performanceForm.categoryCode" @change="syncCategoryName">
            <el-option v-for="category in categories" :key="category.code" :label="category.name" :value="category.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布状态">
          <el-select v-model="performanceForm.publishStatus">
            <el-option label="鑽夌" value="DRAFT" />
            <el-option label="发布到前台" value="PUBLISHED" />
          </el-select>
        </el-form-item>
        <el-form-item label="前台售卖状态">
          <el-select v-model="performanceForm.saleStatus">
            <el-option label="姝ｅ湪鍞エ" value="ON_SALE" />
            <el-option label="即将开售" value="COMING_SOON" />
            <el-option label="绁ㄩ噺绱у紶" value="RETURNED" />
            <el-option label="已结束" value="LOCKED" />
          </el-select>
        </el-form-item>
        <el-form-item label="璐エ妯″紡">
          <el-select v-model="performanceForm.saleMode">
            <el-option label="鑷富閫夊骇" value="SELECTABLE" />
            <el-option label="鑷姩鍒嗛厤" value="AUTO_ALLOCATE" />
            <el-option label="只选区域" value="AREA_ONLY" />
            <el-option label="绔欏腑" value="STANDING" />
          </el-select>
        </el-form-item>
        <el-form-item label="鍏宠仈鍦洪">
          <el-select v-model="performanceForm.venueId" placeholder="閫夋嫨鍦洪" clearable @change="syncPerformanceVenue">
            <el-option v-for="venue in venues" :key="venue.id" :label="venue.name" :value="venue.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="鍩庡競"><el-input v-model="performanceForm.city" /></el-form-item>
        <el-form-item label="鍦洪鍚嶇О"><el-input v-model="performanceForm.venue" /></el-form-item>
        <el-form-item label="鍦板潃"><el-input v-model="performanceForm.address" /></el-form-item>
        <el-form-item label="棣栧満鏃堕棿"><el-input v-model="performanceForm.startTime" placeholder="2026-08-18 19:30" /></el-form-item>
        <el-form-item label="鏈€浣庝环"><el-input-number v-model="performanceForm.priceMin" :min="0" /></el-form-item>
        <el-form-item label="鏈€楂樹环"><el-input-number v-model="performanceForm.priceMax" :min="0" /></el-form-item>
        <el-form-item label="多场演出时间" class="span-2">
          <el-input v-model="performanceForm.sessionDatesText" type="textarea" :rows="4" placeholder="每行一个场次，例如：2026-07-10 19:30:00" />
        </el-form-item>
        <el-form-item label="统一开售时间"><el-input v-model="performanceForm.quickSaleStartTime" placeholder="2026-07-01 10:00:00" /></el-form-item>
        <el-form-item label="统一锁票时间"><el-input v-model="performanceForm.quickLockTime" placeholder="留空则为演出前 1 小时" /></el-form-item>
        <el-form-item label="票档明细" class="span-2">
          <div class="quick-ticket-editor">
            <div v-for="(level, index) in performanceForm.quickTicketLevels" :key="index" class="quick-ticket-row">
              <el-input v-model="level.name" placeholder="票档名称，如 ￥1717 内场" />
              <el-select v-model="level.areaType" placeholder="区域类型">
                <el-option label="内场" value="STANDING" />
                <el-option label="看台" value="SEATED" />
              </el-select>
              <el-input v-model="level.areaName" placeholder="区域，如 内场/看台" />
              <el-input-number v-model="level.price" :min="0" placeholder="价格" />
              <el-input-number v-model="level.totalStock" :min="0" placeholder="总库存" />
              <el-input-number v-model="level.releasedStock" :min="0" placeholder="开放库存" />
              <el-color-picker v-model="level.color" />
              <el-button type="danger" @click="removeQuickTicketLevel(index)">删除</el-button>
            </div>
            <el-button @click="addQuickTicketLevel">新增票档</el-button>
          </div>
        </el-form-item>
        <el-form-item label="鏍囩锛岄€楀彿鍒嗛殧" class="span-2"><el-input v-model="performanceForm.tagsText" /></el-form-item>
        <el-form-item label="娴锋姤" class="span-2">
          <div class="upload-row">
            <img v-if="performanceForm.poster" :src="performanceForm.poster" alt="娴锋姤棰勮" class="poster-preview" />
            <input type="file" accept="image/*" @change="setPerformancePoster" />
            <el-input v-model="performanceForm.poster" placeholder="涔熷彲浠ョ洿鎺ュ～鍐欏浘鐗囧湴鍧€" />
            <el-button @click="importPerformancePoster">导入本机路径</el-button>
          </div>
        </el-form-item>
        <el-form-item label="鍒楄〃鎽樿" class="span-2"><el-input v-model="performanceForm.summary" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="婕斿嚭浠嬬粛"><el-input v-model="performanceForm.intro" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="婕旇亴浜哄憳"><el-input v-model="performanceForm.artistInfo" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="鍦洪浠嬬粛"><el-input v-model="performanceForm.venueIntro" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="璐エ椤荤煡"><el-input v-model="performanceForm.purchaseNotice" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="退票规则"><el-input v-model="performanceForm.refundRule" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="瑙傛紨椤荤煡"><el-input v-model="performanceForm.entryRule" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <div class="detail-builder">
        <div class="table-head">
          <h3>璇︽儏椤垫帓鐗</h3>
          <div class="head-actions">
            <el-button @click="addDetailBlock('HEADING')">鏍囬</el-button>
            <el-button @click="addDetailBlock('PARAGRAPH')">鏂囧瓧</el-button>
            <el-button @click="addDetailBlock('IMAGE')">鍥剧墖</el-button>
          </div>
        </div>
        <div v-for="(block, index) in performanceForm.detailBlocks" :key="index" class="detail-block-editor">
          <el-select v-model="block.type">
            <el-option label="鏍囬" value="HEADING" />
            <el-option label="娈佃惤" value="PARAGRAPH" />
            <el-option label="鍥剧墖" value="IMAGE" />
          </el-select>
          <template v-if="block.type === 'IMAGE'">
            <img v-if="block.content" :src="block.content" alt="璇︽儏鍥鹃瑙" class="detail-preview" />
            <input type="file" accept="image/*" @change="setBlockImage(index, $event)" />
            <el-input v-model="block.content" placeholder="图片地址或上传图片" />
            <el-button @click="importBlockImage(index)">导入本机路径</el-button>
          </template>
          <el-input v-else v-model="block.content" type="textarea" :rows="block.type === 'HEADING' ? 1 : 3" />
          <div class="block-actions">
            <el-button :disabled="index === 0" @click="moveBlock(index, -1)">涓婄Щ</el-button>
            <el-button :disabled="index === performanceForm.detailBlocks.length - 1" @click="moveBlock(index, 1)">涓嬬Щ</el-button>
            <el-button type="danger" @click="removeBlock(index)">鍒犻櫎</el-button>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="performanceDialog = false">鍙栨秷</el-button>
        <el-button type="primary" @click="savePerformance">淇濆瓨婕斿嚭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="venueDialog" :title="venueForm.id ? '缂栬緫鍦洪' : '鏂板鍦洪'" width="680px">
      <el-form label-position="top" class="dialog-form">
        <el-form-item label="鍦洪鍚嶇О"><el-input v-model="venueForm.name" /></el-form-item>
        <el-form-item label="鍩庡競"><el-input v-model="venueForm.cityName" /></el-form-item>
        <el-form-item label="璇︾粏鍦板潃"><el-input v-model="venueForm.address" /></el-form-item>
        <el-form-item label="场馆类型">
          <el-select v-model="venueForm.venueType">
            <el-option label="剧场 / 演出厅" value="THEATER" />
            <el-option label="体育场馆 / 演唱会" value="STADIUM" />
            <el-option label="影院 / 银幕厅" value="CINEMA" />
          </el-select>
        </el-form-item>
        <el-form-item label="舞台/银幕标签"><el-input v-model="venueForm.stageLabel" placeholder="舞台、主舞台、银幕" /></el-form-item>
        <el-form-item label="瀹归噺"><el-input-number v-model="venueForm.capacity" :min="0" /></el-form-item>
        <el-form-item label="鍦洪浠嬬粛"><el-input v-model="venueForm.description" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="venueDialog = false">鍙栨秷</el-button>
        <el-button type="primary" @click="saveVenue">淇濆瓨鍦洪</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="areaDialog" :title="areaForm.id ? '缂栬緫鍖哄煙' : '鏂板鍖哄煙'" width="560px">
      <el-form label-position="top" class="dialog-form">
        <el-form-item label="鍖哄煙鍚嶇О"><el-input v-model="areaForm.areaName" /></el-form-item>
        <el-form-item label="绫诲瀷">
          <el-select v-model="areaForm.areaType">
            <el-option label="鏈夊骇鍖哄煙" value="SEATED" />
            <el-option label="绔欏腑鍖哄煙" value="STANDING" />
          </el-select>
        </el-form-item>
        <el-form-item label="榛樿绁ㄦ。"><el-input v-model="areaForm.defaultTicketLevel" /></el-form-item>
        <el-form-item label="鎺掑簭"><el-input-number v-model="areaForm.sortOrder" :min="1" /></el-form-item>
        <el-form-item label="棰滆壊"><el-color-picker v-model="areaForm.color" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="areaDialog = false">鍙栨秷</el-button>
        <el-button type="primary" @click="saveArea">淇濆瓨鍖哄煙</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="sessionDialog" :title="sessionForm.id ? '缂栬緫鍦烘' : '鏂板鍦烘'" width="760px">
      <el-form label-position="top" class="admin-editor-grid">
        <el-form-item label="婕斿嚭">
          <el-select v-model="sessionForm.performanceId">
            <el-option v-for="item in performances" :key="item.id" :label="item.title" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="鍦洪">
          <el-select v-model="sessionForm.venueId">
            <el-option v-for="venue in venues" :key="venue.id" :label="venue.name" :value="venue.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="鍦烘鍚嶇О"><el-input v-model="sessionForm.sessionName" /></el-form-item>
        <el-form-item label="璐エ妯″紡">
          <el-select v-model="sessionForm.purchaseMode">
            <el-option label="鑷富閫夊骇" value="SELECTABLE" />
            <el-option label="鑷姩鍒嗛厤" value="AUTO_ALLOCATE" />
          </el-select>
        </el-form-item>
        <el-form-item label="开售时间"><el-input v-model="sessionForm.saleStartTime" placeholder="2026-07-20 10:00:00" /></el-form-item>
        <el-form-item label="閿佺エ鏃堕棿"><el-input v-model="sessionForm.lockTime" placeholder="2026-08-01 18:00:00" /></el-form-item>
        <el-form-item label="鍏ュ満鏃堕棿"><el-input v-model="sessionForm.entryTime" /></el-form-item>
        <el-form-item label="开始时间"><el-input v-model="sessionForm.startTime" /></el-form-item>
        <el-form-item label="缁撴潫鏃堕棿"><el-input v-model="sessionForm.endTime" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sessionDialog = false">鍙栨秷</el-button>
        <el-button type="primary" @click="saveSession">淇濆瓨鍦烘</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="ticketLevelDialog" :title="ticketLevelForm.id ? '缂栬緫绁ㄦ。' : '鏂板绁ㄦ。'" width="620px">
      <el-form label-position="top" class="dialog-form">
        <el-form-item label="绁ㄦ。鍚嶇О"><el-input v-model="ticketLevelForm.name" /></el-form-item>
        <el-form-item label="鍏宠仈鍖哄煙">
          <el-select v-model="ticketLevelForm.areaId" placeholder="閫夋嫨鍖哄煙">
            <el-option v-for="area in ticketLevelAreas" :key="area.id" :label="area.areaName" :value="area.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="浠锋牸"><el-input-number v-model="ticketLevelForm.price" :min="0" /></el-form-item>
        <el-form-item label="总库存"><el-input-number v-model="ticketLevelForm.totalStock" :min="0" /></el-form-item>
        <el-form-item label="已开放库存"><el-input-number v-model="ticketLevelForm.releasedStock" :min="0" /></el-form-item>
        <el-form-item label="未开放库存"><el-input-number v-model="ticketLevelForm.unreleasedStock" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ticketLevelDialog = false">鍙栨秷</el-button>
        <el-button type="primary" @click="saveTicketLevel">淇濆瓨绁ㄦ。</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchDialog" :title="batchForm.id ? '编辑开售批次' : '创建开售批次'" width="680px">
      <el-form label-position="top" class="dialog-form">
        <el-form-item label="鍦烘">
          <el-select v-model="batchForm.sessionId">
            <el-option v-for="session in sessions" :key="session.id" :label="sessionLabel(session)" :value="session.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="鎵规鍚嶇О"><el-input v-model="batchForm.batchName" /></el-form-item>
        <el-form-item label="开售时间"><el-input v-model="batchForm.saleStartTime" /></el-form-item>
        <el-form-item label="閿佺エ鏃堕棿"><el-input v-model="batchForm.lockTime" /></el-form-item>
        <el-form-item label="开放方式">
          <el-select v-model="batchForm.releaseType">
            <el-option label="全部开放" value="FULL" />
            <el-option label="按数量开放" value="QUANTITY" />
            <el-option label="按比例开放" value="RATIO" />
          </el-select>
        </el-form-item>
        <el-form-item label="开放数量"><el-input-number v-model="batchForm.releaseQuantity" :min="0" /></el-form-item>
        <el-form-item label="姣忎汉闄愯喘"><el-input-number v-model="batchForm.purchaseLimit" :min="1" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialog = false">鍙栨秷</el-button>
        <el-button type="primary" @click="saveBatch">淇濆瓨鎵规</el-button>
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
const seatForm = reactive({ venueId: 1, areaId: 1, layoutType: 'STANDARD', rowStart: 1, rowEnd: 3, seatsPerRow: 12, startX: 60, startY: 80, gapX: 30, gapY: 30, aisleAfterSeats: '6' })

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
const purchaseModeMap = { SELECTABLE: '自主选座', AUTO_ALLOCATE: '系统配座', AREA_ONLY: '只选区域', STANDING: '站席' }
const releaseTypeMap = { FULL: '全部开放', PARTIAL: '分批开放', MANUAL: '手动开放', QUANTITY: '按数量', RATIO: '按比例' }
const areaTypeMap = { SEATED: '看台/有座', STANDING: '内场/站席' }
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
const performanceTitle = (id) => performances.value.find((item) => String(item.id) === String(id))?.title || `婕斿嚭 ${id || ''}`
const venueName = (id) => venues.value.find((item) => String(item.id) === String(id))?.name || `鍦洪 ${id || ''}`
const areaName = (id) => [...areas.value, ...seatFormAreas.value, ...ticketLevelAreas.value].find((item) => String(item.id) === String(id))?.areaName || `鍖哄煙 ${id || ''}`
const sessionLabel = (session) => session ? `${performanceTitle(session.performanceId)} / ${session.sessionName}` : '鏈€夋嫨鍦烘'

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
  const venue = venues.value.find((item) => item.id === seatForm.venueId)
  if (venue?.venueType === 'STADIUM') seatForm.layoutType = 'STADIUM'
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
    sessionDatesText: '',
    quickSaleStartTime: '2026-07-01 10:00:00',
    quickLockTime: '',
    quickTicketLevels: [
      { name: '看台票 ￥517', areaName: '看台', areaType: 'SEATED', price: 517, totalStock: 1000, releasedStock: 1000, color: '#74c0fc' },
      { name: '内场票 ￥1717', areaName: '内场', areaType: 'STANDING', price: 1717, totalStock: 600, releasedStock: 600, color: '#ff6b6b' }
    ],
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
  return { id: null, name: '', cityName: '上海', address: '', venueType: 'THEATER', stageLabel: '舞台', capacity: 0, description: '', status: 'ENABLED' }
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
  return { id: null, sessionId: selectedSessionId.value, batchName: '第一批开售', saleStartTime: '2026-07-20 10:00:00', lockTime: '2026-08-01 18:00:00', releaseType: 'QUANTITY', releaseQuantity: 50, releaseRatio: 0, purchaseLimit: 2, enableQueue: true, allowReturnDuringSale: true, status: 'NOT_STARTED' }
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
      { type: 'HEADING', content: '椤圭洰浠嬬粛' },
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

function addQuickTicketLevel() {
  performanceForm.quickTicketLevels.push({ name: '新票档', areaName: '看台', areaType: 'SEATED', price: 380, totalStock: 0, releasedStock: 0, color: '#74c0fc' })
}

function removeQuickTicketLevel(index) {
  performanceForm.quickTicketLevels.splice(index, 1)
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

async function ensureQuickArea(venueId, level, index) {
  const currentAreas = await adminApi.areas(venueId)
  const existing = currentAreas.find((area) => area.areaName === level.areaName)
  if (existing) return existing
  return adminApi.createArea(venueId, {
    areaName: level.areaName || level.name || `票区 ${index + 1}`,
    areaType: level.areaType || 'SEATED',
    defaultTicketLevel: level.name,
    sortOrder: index + 1,
    color: level.color || '#74c0fc'
  })
}

async function createQuickSessionsAndTickets(performance) {
  const dateRows = performanceForm.sessionDatesText.split(/\r?\n/).map(normalizeDateTime).filter(Boolean)
  const levels = performanceForm.quickTicketLevels.filter((level) => Number(level.price || 0) > 0)
  if (!performanceForm.venueId || !dateRows.length || !levels.length) return
  const areasByName = new Map()
  for (let i = 0; i < levels.length; i++) {
    const area = await ensureQuickArea(performanceForm.venueId, levels[i], i)
    areasByName.set(levels[i].areaName, area)
  }
  for (const startTime of dateRows) {
    const session = await adminApi.createSession({
      performanceId: performance.id,
      venueId: performanceForm.venueId,
      sessionName: `${performanceForm.title} ${startTime.slice(5, 16)}`,
      saleStartTime: normalizeDateTime(performanceForm.quickSaleStartTime || performanceForm.startTime),
      lockTime: normalizeDateTime(performanceForm.quickLockTime) || addHours(startTime, -1),
      entryTime: addHours(startTime, -1),
      startTime,
      endTime: addHours(startTime, 2),
      purchaseMode: performanceForm.saleMode
    })
    for (const level of levels) {
      const total = Number(level.totalStock || 0)
      const released = Number(level.releasedStock || total)
      await adminApi.createTicketLevel({
        sessionId: session.id,
        name: level.name,
        areaId: areasByName.get(level.areaName)?.id,
        price: Number(level.price || 0),
        totalStock: total,
        releasedStock: released,
        unreleasedStock: Math.max(0, total - released)
      })
    }
    await adminApi.createSaleBatch({
      sessionId: session.id,
      batchName: '第一批开售',
      saleStartTime: normalizeDateTime(performanceForm.quickSaleStartTime || performanceForm.startTime),
      lockTime: normalizeDateTime(performanceForm.quickLockTime) || addHours(startTime, -1),
      releaseType: 'QUANTITY',
      releaseQuantity: levels.reduce((sum, level) => sum + Number(level.releasedStock || level.totalStock || 0), 0),
      purchaseLimit: 6,
      enableQueue: true,
      allowReturnDuringSale: true
    })
  }
}

async function savePerformance() {
  syncCategoryName()
  const prices = performanceForm.quickTicketLevels.map((item) => Number(item.price || 0)).filter((value) => value > 0)
  if (prices.length) {
    performanceForm.priceMin = Math.min(...prices)
    performanceForm.priceMax = Math.max(...prices)
  }
  const payload = {
    ...clone(performanceForm),
    tags: performanceForm.tagsText.split(',').map((item) => item.trim()).filter(Boolean),
    detailImage: performanceForm.detailBlocks.find((item) => item.type === 'IMAGE' && item.content)?.content || performanceForm.detailImage
  }
  const saved = payload.id ? await adminApi.updatePerformance(payload.id, payload) : await adminApi.createPerformance(payload)
  if (!payload.id) await createQuickSessionsAndTickets(saved)
  ElMessage.success('演出已保存')
  performanceDialog.value = false
  await loadAll()
}

async function unpublishPerformance(row) {
  await adminApi.updatePerformance(row.id, { ...row, publishStatus: 'DRAFT' })
  ElMessage.success('婕斿嚭宸蹭笅鏋朵负鑽夌')
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
  ElMessage.success(`已生成 ${previewSeats.value.length} 个座位/分区`)
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
  ElMessage.success('鍦烘搴т綅宸插垵濮嬪寲')
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
  ElMessage.success('寮€鍞壒娆″凡淇濆瓨')
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
  ElMessage.success('瀹炴椂搴撳瓨宸插垵濮嬪寲')
}

async function approve(row) {
  await approveRefund(row.id)
  ElMessage.success('閫€绁ㄥ鏍稿凡閫氳繃')
  await loadOperations()
}

async function reject(row) {
  await rejectRefund(row.id)
  ElMessage.success('閫€绁ㄧ敵璇峰凡椹冲洖')
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



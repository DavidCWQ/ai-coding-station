<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import dayjs from 'dayjs'

import {
  downloadMcWorld,
  getMcAdminConsole,
  getMcConsole,
  postMcActivate,
  postMcAllowlist,
  postMcBackup,
  postMcDeleteBackup,
  postMcDeleteWorld,
  postMcGamerules,
  postMcPermission,
  postMcSettings,
  postMcStart,
  postMcStop,
  type McConsoleVO,
  type McGameruleVO,
  type McBackupVO,
  type McSettingsVO,
  type McWorldVO,
} from '@/api/mcBedrockController'
import { getErrorMessage } from '@/utils/error'

const props = defineProps<{ admin: boolean }>()

const loading = ref(false)
const busy = ref(false)
const consoleVo = ref<McConsoleVO | null>(null)
const selected = ref<string>('')
const settingsDraft = reactive({
  serverName: '',
  gamemode: 'survival',
  difficulty: 'normal',
  maxPlayers: 2,
  viewDistance: 8,
  tickDistance: 4,
  playerIdleTimeout: 60,
  allowList: false,
})
const ruleDraft = ref<Record<string, boolean>>({})

let timer: number | undefined
let pollMs = 20_000

const armPoll = () => {
  const next = ['STARTING', 'STOPPING', 'BACKING_UP'].includes(runPhase.value) ? 3_000 : 20_000
  if (timer && next === pollMs) return
  pollMs = next
  if (timer) window.clearInterval(timer)
  timer = window.setInterval(() => void load(), pollMs)
}

const worlds = computed(() => consoleVo.value?.worlds ?? [])
const current = computed(() => worlds.value.find((w) => w.folder === selected.value) ?? worlds.value[0])
const isAdmin = computed(() => props.admin && consoleVo.value?.viewerRole === 'ADMIN')
const runPhase = computed(() => {
  if (consoleVo.value?.runPhase) return consoleVo.value.runPhase
  return consoleVo.value?.running ? 'RUNNING' : 'SLEEPING'
})
const statusLabel = computed(() => {
  switch (runPhase.value) {
    case 'STARTING':
      return '正在启动'
    case 'RUNNING':
      return '世界运行'
    case 'STOPPING':
      return '正在停止'
    case 'BACKING_UP':
      return '正在冷备'
    default:
      return '世界沉睡'
  }
})
const lampClass = computed(() => {
  switch (runPhase.value) {
    case 'RUNNING':
      return 'is-on'
    case 'STARTING':
    case 'BACKING_UP':
      return 'is-wait'
    case 'STOPPING':
      return 'is-stop'
    default:
      return 'is-off'
  }
})

const biome = (name: string): [string, string, string] => {
  const palettes: Array<[string, string, string]> = [
    ['#3f7a32', '#6b4a24', '#8ec5ff'],
    ['#245c3a', '#163024', '#4f7a48'],
    ['#c9b06a', '#8a6b32', '#f0dd9a'],
    ['#4d6673', '#2a3640', '#b7c9d4'],
    ['#1f4d6b', '#122433', '#6ec3e0'],
    ['#6b3a2a', '#3d2018', '#d98b5a'],
  ]
  let h = 0
  for (let i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) >>> 0
  return palettes[h % palettes.length] ?? ['#3f7a32', '#6b4a24', '#8ec5ff']
}

const artBackground = (folder: string) => {
  const [grass, dirt, sky] = biome(folder)
  return `linear-gradient(180deg, ${sky} 0 38%, ${grass} 38% 52%, ${dirt} 52%)`
}

const fmtSize = (n?: number) => {
  if (!n) return '—'
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / 1024 / 1024).toFixed(1)} MB`
}

const fmtTime = (ms?: number) => {
  if (ms == null || ms === 0) return '尚无记录'
  const n = Math.abs(ms) < 1_000_000_000_000 ? ms * 1000 : ms
  const d = dayjs(n)
  if (!d.isValid() || d.year() < 2015 || d.year() > 2100) return '尚无记录'
  return d.format('YYYY-MM-DD HH:mm')
}

const applyDrafts = (data: McConsoleVO) => {
  const s = data.settings
  if (s) {
    settingsDraft.serverName = s.serverName
    settingsDraft.gamemode = s.gamemode
    settingsDraft.difficulty = s.difficulty
    settingsDraft.maxPlayers = s.maxPlayers
    settingsDraft.viewDistance = s.viewDistance
    settingsDraft.tickDistance = s.tickDistance
    settingsDraft.playerIdleTimeout = s.playerIdleTimeout
    settingsDraft.allowList = s.allowList
  }
  const next: Record<string, boolean> = {}
  for (const r of data.gamerules ?? []) {
    if (r.value != null) next[r.id] = r.value
  }
  ruleDraft.value = next
  if (!selected.value && data.currentWorld) selected.value = data.currentWorld.folder
  if (selected.value && !(data.worlds ?? []).some((w) => w.folder === selected.value)) {
    selected.value = data.currentWorld?.folder ?? ''
  }
}

const load = async () => {
  loading.value = true
  try {
    const data = props.admin ? await getMcAdminConsole() : await getMcConsole()
    consoleVo.value = data
    applyDrafts(data)
  } catch (e) {
    message.error(getErrorMessage(e, '无法连接基岩服'))
  } finally {
    loading.value = false
    armPoll()
  }
}

const runOp = async (label: string, fn: () => Promise<{ message?: string }>) => {
  if (busy.value) return
  busy.value = true
  try {
    const r = await fn()
    message.success(r.message || `${label}已提交`)
    await load()
  } catch (e) {
    message.error(getErrorMessage(e, `${label}失败`))
  } finally {
    busy.value = false
  }
}

const confirm = (title: string, content: string, fn: () => void) => {
  Modal.confirm({
    title,
    content,
    okText: '确定',
    cancelText: '取消',
    onOk: fn,
  })
}

const onBackup = () =>
  confirm('冷备此刻', '会短暂停服 10–30 秒，在线玩家会被踢出。完成后会在存档里多一张卡片，右侧也会留一份压缩备份。', () =>
    runOp('冷备', postMcBackup),
  )

const onStop = () => confirm('关闭世界', '玩家会立即掉线。', () => runOp('停服', postMcStop))

const onStart = () => runOp('开服', postMcStart)

const onActivate = (w: McWorldVO) => {
  if (w.current) return
  confirm(
    `进入「${w.displayName}」`,
    '会先保留当前世界，再切换存档并重启容器。',
    () => runOp('切世界', () => postMcActivate(w.folder)),
  )
}

const onDeleteWorld = (w: McWorldVO) => {
  if (w.current) {
    message.warning('正在运行的世界不能删，请先切换到别的存档')
    return
  }
  confirm(`删除存档「${w.displayName}」`, '会从磁盘删掉这个世界文件夹，删了不能恢复。', () =>
    runOp('删除存档', () => postMcDeleteWorld(w.folder)),
  )
}

const onDeleteBackup = (b: McBackupVO) =>
  confirm(`删除备份「${b.filename}」`, '只删这份压缩包，不影响存档卡片。', () =>
    runOp('删除备份', () => postMcDeleteBackup(b.filename)),
  )

const onDownload = async (w: McWorldVO) => {
  try {
    await downloadMcWorld(w.folder)
  } catch (e) {
    message.error(getErrorMessage(e, '下载失败'))
  }
}

const onSaveSettings = () =>
  confirm('写入服务器设置', '视距、模拟距离等需要重启后完全生效。改之前建议先备份。', () =>
    runOp('设置', () => postMcSettings(settingsDraft as Partial<McSettingsVO>)),
  )

const onSaveRules = () => {
  const changed: Record<string, boolean> = {}
  for (const r of consoleVo.value?.gamerules ?? []) {
    const now = ruleDraft.value[r.id]
    if (now != null && now !== r.value) changed[r.id] = now
  }
  if (!Object.keys(changed).length) {
    message.info('规则没有改动')
    return
  }
  runOp('游戏规则', () => postMcGamerules(changed))
}

const onPerm = (xuid: string, name: string, permission: string) =>
  runOp('权限', () => postMcPermission({ xuid, name, permission }))

const onAllow = (xuid: string, name: string, allowlisted: boolean) =>
  runOp('白名单', () => postMcAllowlist({ xuid, name, allowlisted }))

const ruleValue = (r: McGameruleVO) => ruleDraft.value[r.id] ?? r.value ?? false

onMounted(() => {
  void load()
})
onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<template>
  <div class="realm" :class="{ 'realm--visitor': !isAdmin }">
    <div class="realm__sky" />
    <div class="realm__haze" />

    <header class="realm__mast">
      <div>
        <p class="realm__kicker">{{ isAdmin ? '管理员' : '访客' }}</p>
        <h1 class="realm__title">
          {{ consoleVo?.serverName || 'My World v6.0' }}
        </h1>
        <p class="realm__lead">
          {{ consoleVo?.message || '管理存档、规则和备份。' }}
        </p>
      </div>
      <div class="realm__status">
        <span class="realm__lamp" :class="lampClass" />
        <div>
          <strong>{{ statusLabel }}</strong>
          <small>{{ consoleVo?.joinHint || 'NetherNet · 19132' }}</small>
        </div>
        <template v-if="isAdmin">
          <button class="realm__chip" :disabled="busy" @click="onStart">启动</button>
          <button class="realm__chip realm__chip--warn" :disabled="busy" @click="onStop">停止</button>
          <button class="realm__chip realm__chip--gold" :disabled="busy" @click="onBackup">冷备此刻</button>
        </template>
      </div>
    </header>

    <p v-if="!consoleVo?.configured" class="realm__empty">
      {{ consoleVo?.message || '基岩目录尚未挂载。' }}
    </p>

    <section v-else class="realm__board">
      <div class="realm__shelf">
        <h2>{{ isAdmin ? '存档' : '当前世界' }}</h2>
        <p class="realm__hint">
          {{ isAdmin ? '点一张卡片进入存档，金边是正在运行的世界。冷备会在这里多出一张卡片。' : '你是访客：只能查看管理员选中的当前世界。' }}
        </p>
        <div class="realm__cards">
          <article
            v-for="w in worlds"
            :key="w.folder"
            class="world-card"
            :class="{ 'is-current': w.current, 'is-selected': selected === w.folder }"
            @click="selected = w.folder"
          >
            <div
              class="world-card__art"
              :style="{
                background: w.hasIcon ? undefined : artBackground(w.folder),
              }"
            >
              <img v-if="w.hasIcon && w.iconUrl" :src="w.iconUrl" :alt="w.displayName" />
              <span
                v-if="w.current"
                class="world-card__now"
                :class="{ 'is-asleep': runPhase !== 'RUNNING' }"
              >{{ runPhase === 'RUNNING' ? '运行中' : '已沉睡' }}</span>
            </div>
            <div class="world-card__soil">
              <h3>{{ w.displayName }}</h3>
              <p>{{ fmtSize(w.sizeBytes) }} · {{ fmtTime(w.lastModifiedEpochMs) }}</p>
              <div v-if="isAdmin" class="world-card__ops">
                <button type="button" @click.stop="onDownload(w)">下载</button>
                <button v-if="!w.current" type="button" @click.stop="onActivate(w)">设为当前</button>
                <button v-if="!w.current" type="button" class="is-danger" @click.stop="onDeleteWorld(w)">删除</button>
              </div>
            </div>
          </article>
        </div>
      </div>

      <aside class="realm__lectern" v-if="current">
        <h2>{{ current.displayName }}</h2>
        <dl class="realm__meta">
          <div>
            <dt>文件夹</dt>
            <dd>{{ current.folder }}</dd>
          </div>
          <div>
            <dt>体积</dt>
            <dd>{{ fmtSize(current.sizeBytes) }}</dd>
          </div>
          <div>
            <dt>上次改动</dt>
            <dd>{{ fmtTime(current.lastModifiedEpochMs) }}</dd>
          </div>
          <div v-if="isAdmin">
            <dt>上次冷备</dt>
            <dd>{{ fmtTime(consoleVo?.lastBackupEpochMs) }}</dd>
          </div>
        </dl>

        <template v-if="isAdmin && consoleVo?.settings">
          <h3>服务器设置</h3>
          <p class="realm__hint">这些会写入 server.properties，重启后生效。</p>
          <label>服务器名称<input v-model="settingsDraft.serverName" /></label>
          <div class="realm__row">
            <label>模式
              <select v-model="settingsDraft.gamemode">
                <option value="survival">生存</option>
                <option value="creative">创造</option>
                <option value="adventure">冒险</option>
              </select>
            </label>
            <label>难度
              <select v-model="settingsDraft.difficulty">
                <option value="peaceful">和平</option>
                <option value="easy">简单</option>
                <option value="normal">普通</option>
                <option value="hard">困难</option>
              </select>
            </label>
          </div>
          <div class="realm__row">
            <label>视距 <input v-model.number="settingsDraft.viewDistance" type="number" min="5" max="16" /></label>
            <label>模拟距离 <input v-model.number="settingsDraft.tickDistance" type="number" min="4" max="12" /></label>
          </div>
          <div class="realm__row">
            <label>人数 <input v-model.number="settingsDraft.maxPlayers" type="number" min="1" max="8" /></label>
            <label>挂机踢出(分) <input v-model.number="settingsDraft.playerIdleTimeout" type="number" min="0" /></label>
          </div>
          <label class="realm__check">
            <input v-model="settingsDraft.allowList" type="checkbox" /> 启用白名单
          </label>
          <button class="realm__cta" :disabled="busy" @click="onSaveSettings">保存设置并排队重启</button>

          <h3>游戏规则</h3>
          <p class="realm__hint">非作弊规则，多数可热改。没改过的会沿用服务端当前值。</p>
          <ul class="realm__rules">
            <li v-for="r in consoleVo.gamerules" :key="r.id">
              <label>
                <input
                  type="checkbox"
                  :checked="ruleValue(r)"
                  @change="ruleDraft[r.id] = ($event.target as HTMLInputElement).checked"
                />
                <span>
                  <strong>{{ r.label }}</strong>
                  <small>{{ r.hint }}</small>
                </span>
              </label>
            </li>
          </ul>
          <button class="realm__cta realm__cta--ghost" :disabled="busy" @click="onSaveRules">保存规则</button>

          <h3>玩家权限</h3>
          <p class="realm__hint">会写入 permissions.json。开着服时由 ops-worker 执行 permission reload，下一次进服或重登后生效。</p>
          <ul class="realm__people">
            <li v-for="p in consoleVo.players" :key="p.xuid">
              <div>
                <strong>{{ p.name || '未命名玩家' }}</strong>
                <small>{{ p.xuid }}</small>
              </div>
              <select :value="p.permission" @change="onPerm(p.xuid, p.name, ($event.target as HTMLSelectElement).value)">
                <option value="visitor">访客</option>
                <option value="member">成员</option>
                <option value="operator">管理员</option>
              </select>
              <button type="button" @click="onAllow(p.xuid, p.name, !p.allowlisted)">
                {{ p.allowlisted ? '移出白名单' : '加入白名单' }}
              </button>
            </li>
            <li v-if="!consoleVo.players.length" class="is-empty">还没有写入权限或白名单的玩家。</li>
          </ul>

          <h3>备份</h3>
          <p class="realm__hint">夜里 4 点会自动备份一份。点「冷备此刻」会在存档里加一张可切换的卡片，这里再留一份压缩包。</p>
          <div class="realm__backup-cards">
            <article v-for="b in consoleVo.backups" :key="b.filename" class="backup-card">
              <div class="backup-card__art" />
              <div class="backup-card__soil">
                <h3>世界备份</h3>
                <p>{{ fmtTime(b.createdEpochMs) }} · {{ fmtSize(b.sizeBytes) }}</p>
                <small>{{ b.filename }}</small>
                <button type="button" class="is-danger" :disabled="busy" @click="onDeleteBackup(b)">删除</button>
              </div>
            </article>
            <p v-if="!consoleVo.backups.length" class="is-empty">还没有备份。点上方「冷备此刻」会生成第一张。</p>
          </div>
        </template>

        <p v-else class="realm__visitor-note">
          当前是访客视角：可以查看正在运行的世界，不能改设置，也不能下载存档。
        </p>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.realm {
  --ink: #f4e7c8;
  --muted: #c9b48a;
  --gold: #e2b84c;
  --moss: #3f7a32;
  --soil: #5a3b22;
  --cave: #120e0b;
  position: relative;
  min-height: calc(100vh - 88px);
  padding: 28px 28px 48px;
  color: var(--ink);
  overflow: hidden;
  font-family: "Iowan Old Style", "Palatino Linotype", Palatino, "Songti SC", serif;
}

.realm__sky {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(1200px 480px at 20% -10%, #3a6d9a 0%, transparent 55%),
    radial-gradient(900px 420px at 90% 0%, #1c3d2a 0%, transparent 50%),
    linear-gradient(180deg, #1b2838 0%, #24180f 42%, #120e0b 100%);
}

.realm__haze {
  position: absolute;
  inset: 0;
  background-image: repeating-linear-gradient(
    90deg,
    transparent 0 2px,
    rgba(255, 255, 255, 0.015) 2px 3px
  );
  pointer-events: none;
}

.realm__mast,
.realm__board,
.realm__empty {
  position: relative;
  z-index: 1;
}

.realm__mast {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-end;
  margin-bottom: 28px;
}

.realm__kicker {
  margin: 0 0 6px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  font-size: 11px;
  color: var(--gold);
}

.realm__title {
  margin: 0;
  font-size: clamp(32px, 5vw, 54px);
  line-height: 1.05;
  text-shadow: 0 2px 0 #2a1a0a, 0 18px 40px rgba(0, 0, 0, 0.45);
}

.realm__lead {
  margin: 10px 0 0;
  max-width: 42em;
  color: var(--muted);
}

.realm__status {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: rgba(18, 14, 11, 0.72);
  border: 1px solid rgba(226, 184, 76, 0.35);
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.4);
}

.realm__status strong {
  display: block;
}

.realm__status small {
  color: var(--muted);
}

.realm__lamp {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  box-shadow: 0 0 12px currentColor;
}
.realm__lamp.is-on {
  background: #7dff7a;
  color: #7dff7a;
}
.realm__lamp.is-wait {
  background: #e2b84c;
  color: #e2b84c;
  animation: realm-pulse 1s ease-in-out infinite;
}
.realm__lamp.is-stop {
  background: #f0a890;
  color: #c45b3a;
  animation: realm-pulse 1s ease-in-out infinite;
}
@keyframes realm-pulse {
  50% { opacity: 0.35; }
}
.realm__lamp.is-off {
  background: #7a4a3a;
  color: #7a4a3a;
}

.realm__chip,
.realm__cta,
.world-card__ops button,
.realm__people button {
  border: 1px solid rgba(244, 231, 200, 0.25);
  background: #2b2118;
  color: var(--ink);
  padding: 6px 10px;
  cursor: pointer;
  font: inherit;
}
.realm__chip--gold {
  border-color: var(--gold);
  color: var(--gold);
}
.realm__chip--warn {
  border-color: #c45b3a;
  color: #f0a890;
}
.realm__chip:disabled,
.realm__cta:disabled {
  opacity: 0.5;
  cursor: wait;
}

.realm__empty {
  padding: 40px;
  border: 1px dashed rgba(244, 231, 200, 0.3);
}

.realm__board {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.9fr);
  gap: 22px;
}

.realm__shelf h2,
.realm__lectern h2,
.realm__lectern h3 {
  margin: 0 0 8px;
  font-size: 22px;
}

.realm__lectern h3 {
  margin-top: 22px;
  color: var(--gold);
  font-size: 16px;
}

.realm__hint,
.realm__visitor-note {
  color: var(--muted);
  margin: 0 0 16px;
}

.realm__cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.world-card {
  cursor: pointer;
  background: #3a2616;
  overflow: hidden;
  border: 2px solid #2a180e;
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.35);
  transform: translateY(0);
  transition: transform 160ms ease, box-shadow 160ms ease, border-color 160ms ease;
}
.world-card:hover {
  transform: translateY(-4px);
}
.world-card.is-current {
  border-color: var(--gold);
  box-shadow: 0 0 24px rgba(226, 184, 76, 0.35);
}
.world-card.is-selected:not(.is-current) {
  border-color: rgba(244, 231, 200, 0.55);
}

.world-card__art {
  height: 128px;
  position: relative;
  overflow: hidden;
}
.world-card__art img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.world-card__now {
  position: absolute;
  left: 10px;
  bottom: 10px;
  background: rgba(18, 14, 11, 0.82);
  color: var(--gold);
  padding: 2px 8px;
  letter-spacing: 0.08em;
  font-size: 12px;
}
.world-card__now.is-asleep {
  color: #c4b8a4;
}

.world-card__soil {
  padding: 12px 12px 14px;
  background:
    repeating-linear-gradient(90deg, #6b4a28 0 8px, #5a3b22 8px 16px),
    #5a3b22;
}
.world-card__soil h3 {
  margin: 0 0 4px;
  font-size: 18px;
}
.world-card__soil p {
  margin: 0;
  font-size: 12px;
  color: #ecd7a8;
}
.world-card__ops {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}
.is-danger {
  border-color: #c45b3a !important;
  color: #f0a890 !important;
}

.realm__lectern {
  padding: 18px 18px 24px;
  background: linear-gradient(180deg, #2a2118 0%, #1a140f 100%);
  border: 1px solid rgba(226, 184, 76, 0.22);
  box-shadow: inset 0 1px 0 rgba(255, 230, 160, 0.08);
}

.realm__meta {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 16px;
  margin: 0 0 8px;
}
.realm__meta dt {
  color: var(--muted);
  font-size: 12px;
}
.realm__meta dd {
  margin: 2px 0 0;
}

.realm__lectern label {
  display: block;
  margin: 8px 0;
  color: var(--muted);
  font-size: 13px;
}
.realm__lectern input,
.realm__lectern select {
  display: block;
  width: 100%;
  margin-top: 4px;
  padding: 6px 8px;
  background: #120e0b;
  color: var(--ink);
  border: 1px solid rgba(244, 231, 200, 0.2);
  font: inherit;
}
.realm__row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.realm__check {
  display: flex !important;
  align-items: center;
  gap: 8px;
}
.realm__check input {
  width: auto;
  margin: 0;
}

.realm__cta {
  width: 100%;
  margin-top: 8px;
  padding: 10px;
  background: #6b4a16;
  border-color: var(--gold);
}
.realm__cta--ghost {
  background: transparent;
}

.realm__rules,
.realm__people {
  list-style: none;
  padding: 0;
  margin: 0;
}
.realm__backup-cards {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}
.backup-card {
  overflow: hidden;
  background: #3a2616;
  border: 2px solid #2a180e;
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.3);
}
.backup-card__art {
  height: 56px;
  background:
    linear-gradient(180deg, #8a6b32 0 42%, #5a3b22 42%);
  box-shadow: inset 0 -8px 0 #2a180e;
}
.backup-card__soil {
  padding: 10px 12px 12px;
  background: #5a3b22;
}
.backup-card__soil h3 {
  margin: 0 0 4px;
  font-size: 16px;
}
.backup-card__soil p,
.backup-card__soil small {
  margin: 0;
  color: #ecd7a8;
}
.backup-card__soil small {
  display: block;
  margin-top: 4px;
  font-size: 11px;
  opacity: 0.8;
  word-break: break-all;
}
.backup-card__soil button {
  margin-top: 8px;
}
.realm__rules li label {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.realm__rules input {
  width: auto;
  margin-top: 4px;
}
.realm__rules small,
.realm__people small {
  display: block;
  color: var(--muted);
}

.realm__people li {
  display: grid;
  grid-template-columns: 1fr 110px auto;
  gap: 8px;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid rgba(244, 231, 200, 0.08);
}
.is-empty {
  color: var(--muted);
}

@media (max-width: 960px) {
  .realm__board,
  .realm__mast,
  .realm__row,
  .realm__people li {
    display: block;
  }
  .realm__status {
    flex-wrap: wrap;
    margin-top: 16px;
  }
}
</style>

<script>
  import { onMount } from 'svelte'
  import { processFiles, fetchSummary } from './lib/api.js'
  import { formatBRL, formatDate, formatCount } from './lib/formatters.js'
  import { categoryColor } from './lib/categoryColors.js'
  import { categoryIcon } from './lib/categoryIcons.js'
  import { computeMonthWithMostTransactions } from './lib/computeStats.js'
  import { auth } from './lib/auth.svelte.js'

  import UploadZone from './components/UploadZone.svelte'
  import FileList from './components/FileList.svelte'
  import LoadingSpinner from './components/LoadingSpinner.svelte'
  import ErrorBanner from './components/ErrorBanner.svelte'
  import DonutChart from './components/DonutChart.svelte'
  import StatCard from './components/StatCard.svelte'
  import SmartGroupTable from './components/SmartGroupTable.svelte'
  import AllExpensesTable from './components/AllExpensesTable.svelte'
  import AuthModal from './components/AuthModal.svelte'
  import PaywallModal from './components/PaywallModal.svelte'

  const MAX_FILES = 6
  const MAX_SIZE = 512 * 1024

  let files = $state([])
  let sessionId = $state('')
  let result = $state(null)
  let loading = $state(false)
  let error = $state(null)

  // Derived stats
  let monthStat = $derived(computeMonthWithMostTransactions(result?.AllExpenses))
  let mostRecurring = $derived(
    result?.SmartGroupExpenselist?.slice().sort((a, b) => b.instances - a.instances)[0] ?? null
  )
  let mostExpensiveDay = $derived(
    result?.NotableDays?.slice().sort((a, b) => Number(b.total) - Number(a.total))[0] ?? null
  )

  let hasOversized = $derived(files.some(f => f.size > MAX_SIZE))
  let canSubmitFiles = $derived(files.length > 0 && files.length <= MAX_FILES && !hasOversized && !loading)
  let canSubmitSession = $derived(sessionId.trim().length > 0 && !loading)

  onMount(() => {
    auth.init()
  })

  function handleFilesChange(newFiles) {
    const merged = [...files, ...newFiles]
    const seen = new Set()
    const deduped = merged.filter(f => {
      const key = f.name + f.size
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })
    files = deduped.slice(-MAX_FILES)
  }

  function removeFile(index) {
    files = files.filter((_, i) => i !== index)
  }

  async function handleSubmitFiles() {
    if (!canSubmitFiles) return
    loading = true
    error = null
    try {
      result = await processFiles(files)
    } catch (err) {
      error = { errorCode: err.errorCode, message: err.message, details: err.details }
    } finally {
      loading = false
    }
  }

  async function handleSubmitSession() {
    if (!canSubmitSession) return
    loading = true
    error = null
    try {
      result = await fetchSummary(sessionId.trim())
    } catch (err) {
      error = { errorCode: err.errorCode, message: err.message, details: err.details }
    } finally {
      loading = false
    }
  }

  function reset() {
    result = null
    files = []
    sessionId = ''
    error = null
  }
</script>

{#if !auth.session}
  <AuthModal />
{:else if auth.isPremium === false}
  <PaywallModal />
{/if}

<div class="app" class:blurred={!auth.session || auth.isPremium === false}>
  <header class="app-header">
    <div class="header-inner">
      <div class="logo">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="2" y="3" width="20" height="14" rx="2"/>
          <path d="M8 21h8M12 17v4"/>
          <path d="M6 9l3 3 3-3 3 3 3-3" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <span>Extract</span>
      </div>

      <div class="header-right">
        {#if result}
          <button class="reset-btn" onclick={reset}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="1 4 1 10 7 10"/>
              <path d="M3.51 15a9 9 0 102.13-9.36L1 10"/>
            </svg>
            Novo relatório
          </button>
        {/if}

        {#if auth.session}
          <div class="user-info">
            <span class="user-email">{auth.session.user.email}</span>
            <button class="signout-btn" onclick={() => auth.signOut()} title="Sair">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                <polyline points="16 17 21 12 16 7"/>
                <line x1="21" y1="12" x2="9" y2="12"/>
              </svg>
            </button>
          </div>
        {/if}
      </div>
    </div>
  </header>

  <main class="app-main">
    {#if auth.isPremium === null && auth.session}
      <div class="status-checking">
        <LoadingSpinner />
      </div>
    {:else if auth.isPremium}
      {#if !result}
        <!-- Upload Phase -->
        <section class="upload-section">
          <div class="upload-header">
            <h1>Relatório de Gastos</h1>
            <p>Envie seus extratos em PDF para gerar uma análise detalhada das despesas.</p>
          </div>

          {#if error}
            <ErrorBanner
              errorCode={error.errorCode}
              message={error.message}
              details={error.details}
              onDismiss={() => (error = null)}
            />
          {/if}

          {#if loading}
            <LoadingSpinner />
          {:else}
            <UploadZone onFilesChange={handleFilesChange} />

            {#if files.length}
              <FileList {files} onRemove={removeFile} />

              <div class="submit-row">
                <span class="file-counter" class:warn={files.length === MAX_FILES}>
                  {files.length} / {MAX_FILES} arquivos
                </span>
                <button
                  class="submit-btn"
                  disabled={!canSubmitFiles}
                  onclick={handleSubmitFiles}
                >
                  Processar extratos
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <line x1="5" y1="12" x2="19" y2="12"/>
                    <polyline points="12 5 19 12 12 19"/>
                  </svg>
                </button>
              </div>
            {/if}

            <div class="divider"><span>ou buscar sessão existente</span></div>

            <div class="session-row">
              <input
                class="session-input"
                type="text"
                placeholder="Session ID"
                bind:value={sessionId}
                onkeydown={(e) => e.key === 'Enter' && handleSubmitSession()}
              />
              <button
                class="submit-btn"
                disabled={!canSubmitSession}
                onclick={handleSubmitSession}
              >
                Buscar
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <circle cx="11" cy="11" r="8"/>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
              </button>
            </div>
          {/if}
        </section>
      {:else}
        <!-- Report Phase -->
        <section class="report-section">
          {#if error}
            <ErrorBanner
              errorCode={error.errorCode}
              message={error.message}
              details={error.details}
              onDismiss={() => (error = null)}
            />
          {/if}

          <!-- Top row: chart + stat cards -->
          <div class="top-row">
            <div class="chart-panel panel">
              <h2 class="panel-title">Gastos por Categoria</h2>
              {#if result.ExpensesByCategory?.length}
                <DonutChart data={result.ExpensesByCategory} />
              {:else}
                <p class="empty">Sem dados de categoria</p>
              {/if}
            </div>

            <div class="stats-grid">
              {#if result.BiggestSingularExpense}
                <StatCard
                  label="Maior gasto único"
                  value={formatBRL(Number(result.BiggestSingularExpense.value))}
                  sub="{result.BiggestSingularExpense.expenseName} · {formatDate(result.BiggestSingularExpense.date)}"
                  color={categoryColor(result.BiggestSingularExpense.category)}
                  icon={categoryIcon(result.BiggestSingularExpense.category)}
                />
              {/if}

              {#if monthStat}
                <StatCard
                  label="Mês com mais transações"
                  value={monthStat.label}
                  sub={formatCount(monthStat.count)}
                  color="#10b981"
                  icon={'<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V8h14v11zM9 10H7v2h2v-2zm4 0h-2v2h2v-2zm4 0h-2v2h2v-2z"/></svg>'}
                />
              {/if}

              {#if mostRecurring}
                <StatCard
                  label="Compra mais recorrente"
                  value={mostRecurring.expenseName}
                  sub="{mostRecurring.instances}× · {formatBRL(Number(mostRecurring.total))}"
                  color={categoryColor(mostRecurring.category)}
                  icon={categoryIcon(mostRecurring.category)}
                />
              {/if}

              {#if mostExpensiveDay}
                <StatCard
                  label="Dia mais caro"
                  value={formatDate(mostExpensiveDay.date)}
                  sub="{formatBRL(Number(mostExpensiveDay.total))} · {mostExpensiveDay.transactions} transações"
                  color="#FF3B30"
                  icon={'<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M13.5 0.7c-0.2-0.4-0.7-0.6-1.1-0.4-0.4 0.2-0.6 0.7-0.4 1.1 0 0 0 0 0 0L12 2c-3 2-5 5-5 9 0 0.5 0 1 0.1 1.5C5.8 11.1 5 9.1 5 7c0-0.4-0.3-0.8-0.8-0.8S3.5 6.6 3.5 7c0 4.1 2.7 7.6 6.5 8.7V22h4v-6.3c3.8-1.1 6.5-4.6 6.5-8.7 0-3.2-2.7-6.3-7-7.3z"/></svg>'}
                />
              {/if}
            </div>
          </div>

          <!-- Smart group table -->
          {#if result.SmartGroupExpenselist?.length}
            <div class="panel">
              <h2 class="panel-title">Top Gastos por Nome</h2>
              <SmartGroupTable items={result.SmartGroupExpenselist} />
            </div>
          {/if}

          <!-- All expenses table -->
          {#if result.AllExpenses?.length}
            <div class="panel">
              <h2 class="panel-title">Todas as Despesas</h2>
              <AllExpensesTable items={result.AllExpenses} />
            </div>
          {/if}
        </section>
      {/if}
    {/if}
  </main>
</div>

<style>
  :global(*, *::before, *::after) { box-sizing: border-box; }

  :global(:root) {
    --bg-page: #0f1115;
    --panel-bg: #161920;
    --panel-border: rgba(16, 185, 129, 0.15);
    --primary: #10b981;
    --primary-glow: rgba(16, 185, 129, 0.08);
    --text-main: #e2e8f0;
    --text-muted: #94a3b8;
    --text-dim: #64748b;
    --surface-1: #1e2330;
    --surface-2: #252d3a;
    --surface-input: #0d1017;
    --danger: #ef4444;
    --danger-bg: rgba(239, 68, 68, 0.1);
    --warning: #f59e0b;
    --radius-sm: 8px;
    --radius-md: 12px;
    --radius-lg: 16px;
    --transition: 150ms ease;
  }

  :global(body) {
    margin: 0;
    background: var(--bg-page);
    color: var(--text-main);
    font-family: 'Inter', -apple-system, system-ui, sans-serif;
    min-height: 100vh;
    -webkit-font-smoothing: antialiased;
  }

  .app {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    transition: filter 200ms ease;
  }

  .app.blurred {
    filter: blur(4px);
    pointer-events: none;
    user-select: none;
  }

  /* Header */
  .app-header {
    border-bottom: 1px solid var(--panel-border);
    background: var(--panel-bg);
    position: sticky;
    top: 0;
    z-index: 10;
  }

  .header-inner {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 1.5rem;
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .logo {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-weight: 700;
    font-size: 1.125rem;
    color: var(--primary);
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 0.75rem;
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  .user-email {
    font-size: 0.8125rem;
    color: var(--text-dim);
    max-width: 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .signout-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    background: none;
    border: 1px solid var(--panel-border);
    border-radius: var(--radius-sm);
    color: var(--text-muted);
    cursor: pointer;
    padding: 0.375rem;
    transition: color var(--transition), border-color var(--transition);
  }

  .signout-btn:hover {
    color: var(--danger);
    border-color: var(--danger);
  }

  .reset-btn {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    background: none;
    border: 1px solid var(--panel-border);
    border-radius: var(--radius-sm);
    color: var(--text-muted);
    font-family: inherit;
    font-size: 0.875rem;
    padding: 0.375rem 0.875rem;
    cursor: pointer;
    transition: color var(--transition), border-color var(--transition);
  }

  .reset-btn:hover {
    color: var(--primary);
    border-color: var(--primary);
  }

  /* Main */
  .app-main {
    flex: 1;
    max-width: 1200px;
    width: 100%;
    margin: 0 auto;
    padding: 2rem 1.5rem;
  }

  .status-checking {
    display: flex;
    justify-content: center;
    padding-top: 4rem;
  }

  /* Upload section */
  .upload-section {
    max-width: 640px;
    margin: 0 auto;
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
  }

  .upload-header h1 {
    font-size: 2rem;
    font-weight: 700;
    margin: 0 0 0.5rem;
    background: linear-gradient(135deg, var(--text-main) 60%, var(--primary));
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .upload-header p {
    color: var(--text-muted);
    margin: 0;
    font-size: 0.9375rem;
    line-height: 1.6;
  }

  .submit-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
  }

  .file-counter {
    font-size: 0.875rem;
    color: var(--text-muted);
  }

  .file-counter.warn { color: var(--warning); }

  .divider {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    color: var(--text-dim);
    font-size: 0.8125rem;
  }

  .divider::before,
  .divider::after {
    content: '';
    flex: 1;
    height: 1px;
    background: var(--panel-border);
  }

  .session-row {
    display: flex;
    gap: 0.75rem;
  }

  .session-input {
    flex: 1;
    background: var(--surface-input);
    border: 1px solid var(--panel-border);
    border-radius: var(--radius-sm);
    padding: 0.625rem 0.875rem;
    color: var(--text-main);
    font-size: 0.9375rem;
    font-family: monospace;
    outline: none;
    transition: border-color var(--transition);
  }

  .session-input::placeholder { color: var(--text-dim); font-family: inherit; }
  .session-input:focus { border-color: var(--primary); }

  .submit-btn {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    background: var(--primary);
    border: none;
    border-radius: var(--radius-sm);
    color: #0f1115;
    font-family: inherit;
    font-size: 0.9375rem;
    font-weight: 600;
    padding: 0.625rem 1.25rem;
    cursor: pointer;
    transition: opacity var(--transition), transform var(--transition);
  }

  .submit-btn:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  .submit-btn:not(:disabled):hover {
    opacity: 0.9;
    transform: translateY(-1px);
  }

  /* Report section */
  .report-section {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
  }

  .top-row {
    display: grid;
    grid-template-columns: 320px 1fr;
    gap: 1.5rem;
    align-items: stretch;
  }

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 1rem;
  }

  .panel {
    background: var(--panel-bg);
    border: 1px solid var(--panel-border);
    border-radius: var(--radius-lg);
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
  }

  .chart-panel {
    min-width: 0;
  }

  .panel-title {
    font-size: 1rem;
    font-weight: 600;
    margin: 0;
    color: var(--text-main);
  }

  .empty {
    color: var(--text-dim);
    font-size: 0.875rem;
    margin: 0;
  }

  /* Responsive */
  @media (max-width: 900px) {
    .top-row {
      grid-template-columns: 1fr;
    }
    .chart-panel {
      max-width: 480px;
    }
  }

  @media (max-width: 600px) {
    .app-main { padding: 1.25rem 1rem; }
    .stats-grid { grid-template-columns: 1fr; }
    .upload-header h1 { font-size: 1.5rem; }
    .user-email { display: none; }
  }
</style>

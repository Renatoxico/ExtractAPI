<script>
  import { onMount } from 'svelte'
  import { onAuthStateChanged } from 'firebase/auth'
  import { auth } from './lib/firebase.js'
  import { processFiles, fetchReportHistory, fetchSummary } from './lib/api.js'
  import { formatBRL, formatCreatedAt, formatCount } from './lib/formatters.js'
  import { computeMonthWithMostTransactions } from './lib/computeStats.js'

  import UploadZone from './components/UploadZone.svelte'
  import FileList from './components/FileList.svelte'
  import LoadingSpinner from './components/LoadingSpinner.svelte'
  import ErrorBanner from './components/ErrorBanner.svelte'
  import DonutChart from './components/DonutChart.svelte'
  import EditorialHighlights from './components/EditorialHighlights.svelte'
  import GroupedExpensesTable from './components/GroupedExpensesTable.svelte'
  import ReportHistory from './components/ReportHistory.svelte'
  import AuthControls from './components/AuthControls.svelte'

  const MAX_FILES = 6
  const MAX_SIZE = 512 * 1024

  let files = $state([])
  let reportId = $state('')
  let result = $state(null)
  let loading = $state(false)
  let error = $state(null)

  let user = $state(null)
  let authReady = $state(false)
  let history = $state([])
  let historyLoading = $state(false)
  let historyError = $state('')
  let historyOpen = $state(false)
  let loadingReportId = $state('')

  let monthStat = $derived(computeMonthWithMostTransactions(result?.expenses))
  let mostRecurring = $derived(
    result?.expenseGroups?.slice().sort((a, b) => b.occurrenceCount - a.occurrenceCount)[0] ?? null
  )
  let mostExpensiveDay = $derived(result?.highlights?.highestSpendingDay ?? null)
  let reportTotal = $derived(
    result?.expenses?.reduce((sum, expense) => sum + Number(expense.amount), 0) ?? 0
  )
  let reportCount = $derived(result?.expenses?.length ?? 0)

  let hasOversized = $derived(files.some(f => f.size > MAX_SIZE))
  let canSubmitFiles = $derived(
    Boolean(user) && files.length > 0 && files.length <= MAX_FILES && !hasOversized && !loading
  )
  let canSubmitReport = $derived(Boolean(user) && reportId.trim().length > 0 && !loading)

  onMount(() => {
    const unsubscribe = onAuthStateChanged(auth, (firebaseUser) => {
      user = firebaseUser
      authReady = true
      historyOpen = false

      if (firebaseUser) {
        refreshHistory(firebaseUser.uid)
      } else {
        clearPrivateState()
      }
    })

    return unsubscribe
  })

  function clearPrivateState() {
    history = []
    historyLoading = false
    historyError = ''
    loadingReportId = ''
    result = null
    reportId = ''
    files = []
    error = null
  }

  function toUiError(err) {
    return { errorCode: err.errorCode, message: err.message, details: err.details }
  }

  async function refreshHistory(expectedUid = auth.currentUser?.uid) {
    if (!expectedUid) return
    historyLoading = true
    historyError = ''
    try {
      const reports = await fetchReportHistory()
      if (auth.currentUser?.uid === expectedUid) history = reports
    } catch (err) {
      if (auth.currentUser?.uid === expectedUid) {
        historyError = err.message ?? 'Não foi possível carregar o histórico.'
      }
    } finally {
      if (auth.currentUser?.uid === expectedUid) historyLoading = false
    }
  }

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
      reportId = result.reportId
      files = []
      await refreshHistory()
    } catch (err) {
      error = toUiError(err)
    } finally {
      loading = false
    }
  }

  async function handleSubmitReport() {
    if (!canSubmitReport) return
    loading = true
    error = null
    try {
      result = await fetchSummary(reportId.trim())
      reportId = result.reportId
    } catch (err) {
      error = toUiError(err)
    } finally {
      loading = false
    }
  }

  async function openHistoryReport(selectedReportId) {
    if (loadingReportId || selectedReportId === result?.reportId) {
      historyOpen = false
      return
    }

    loadingReportId = selectedReportId
    error = null
    try {
      const selected = await fetchSummary(selectedReportId)
      result = selected
      reportId = selected.reportId
      historyOpen = false
    } catch (err) {
      error = toUiError(err)
    } finally {
      loadingReportId = ''
    }
  }

  function reset() {
    result = null
    files = []
    reportId = ''
    error = null
    historyOpen = false
  }
</script>

<svelte:window onkeydown={(event) => event.key === 'Escape' && (historyOpen = false)} />

<div class="app">
  <button
    class="sidebar-toggle"
    type="button"
    aria-label="Abrir menu"
    aria-expanded={historyOpen}
    aria-controls="app-sidebar"
    onclick={() => (historyOpen = true)}
  >
    <span></span><span></span><span></span>
  </button>

  <aside id="app-sidebar" class="app-sidebar" class:open={historyOpen} aria-label="Navegação principal">
    <div class="sidebar-brand">
      <a class="logo" href="/" aria-label="Somai — novo relatório" onclick={(event) => { event.preventDefault(); reset() }}>
        <img src="/somai-logo.png" alt="Somai" />
      </a>
    </div>

    <div class="sidebar-content">
      {#if user}
        <ReportHistory
          items={history}
          loading={historyLoading}
          error={historyError}
          activeReportId={result?.reportId ?? ''}
          {loadingReportId}
          onSelect={openHistoryReport}
          onRetry={() => refreshHistory()}
        />
      {:else}
        <div class="sidebar-intro">
          <p class="section-kicker">Seu espaço financeiro</p>
          <p>Entre para manter seus relatórios organizados e acessíveis em um só lugar.</p>
        </div>
      {/if}
    </div>

    <div class="sidebar-footer">
      <AuthControls {user} {authReady} />
    </div>
  </aside>

  {#if historyOpen}
    <button class="drawer-backdrop" type="button" aria-label="Fechar menu" onclick={() => (historyOpen = false)}></button>
  {/if}

  <div class="workspace">
    <main class="app-main" class:report-main={Boolean(result)}>
      {#if !result}
        <section class="upload-section" class:with-files={files.length > 0}>
          <div class="upload-header">
            <p class="section-kicker">Leitura financeira</p>
            <h1>Relatório de Gastos</h1>
            <p>Envie seus extratos em PDF para transformar lançamentos dispersos em uma leitura clara das suas despesas.</p>
          </div>

          {#if !user && authReady}
            <div class="signin-note">Entre com o Google para processar extratos e acessar seu histórico.</div>
          {/if}

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
            <UploadZone fileCount={files.length} onFilesChange={handleFilesChange} />

            {#if files.length}
              <FileList {files} onRemove={removeFile} />

              <div class="submit-row">
                <span class="file-counter" class:warn={files.length === MAX_FILES}>
                  {files.length} / {MAX_FILES} arquivos
                </span>
                <button class="submit-btn" disabled={!canSubmitFiles} onclick={handleSubmitFiles}>
                  Processar extratos <span aria-hidden="true">→</span>
                </button>
              </div>
            {/if}

            <div class="divider"><span>ou buscar relatório existente</span></div>

            <div class="report-row">
              <input
                class="report-input"
                type="text"
                aria-label="Report ID"
                placeholder="Report ID"
                bind:value={reportId}
                onkeydown={(event) => event.key === 'Enter' && handleSubmitReport()}
              />
              <button class="submit-btn" disabled={!canSubmitReport} onclick={handleSubmitReport}>Buscar</button>
            </div>
          {/if}
        </section>
      {:else}
        <section class="report-section">
          {#if error}
            <ErrorBanner
              errorCode={error.errorCode}
              message={error.message}
              details={error.details}
              onDismiss={() => (error = null)}
            />
          {/if}

          <header class="report-heading">
            <div>
              <p class="section-kicker">Visão consolidada</p>
              <h1>Seu relatório de gastos</h1>
            </div>
            <dl class="report-meta">
              <div class="meta-id">
                <dt>Report ID</dt>
                <dd><code title={result.reportId}>{result.reportId}</code></dd>
              </div>
              <div>
                <dt>Criado em</dt>
                <dd>{formatCreatedAt(result.createdAt)}</dd>
              </div>
              <div>
                <dt>Total</dt>
                <dd>{formatBRL(reportTotal)}</dd>
              </div>
              <div>
                <dt>Despesas</dt>
                <dd>{formatCount(reportCount)}</dd>
              </div>
            </dl>
          </header>

          <div class="top-row">
            <div class="chart-panel panel">
              <div class="panel-heading">
                <p class="panel-index">Distribuição</p>
                <h2>Gastos por categoria</h2>
              </div>
              {#if result.categorySummaries?.length}
                <DonutChart data={result.categorySummaries} />
              {:else}
                <p class="empty">Sem dados de categoria</p>
              {/if}
            </div>

            <EditorialHighlights
              largestExpense={result.highlights?.largestExpense}
              highestSpendingDay={mostExpensiveDay}
              {mostRecurring}
              {monthStat}
            />
          </div>

          {#if result.expenseGroups?.length}
            <div class="panel expenses-panel">
              <div class="panel-heading expenses-heading">
                <div>
                  <p class="panel-index">Inventário</p>
                  <h2>Despesas agrupadas</h2>
                </div>
              </div>
              <GroupedExpensesTable groups={result.expenseGroups} expenses={result.expenses} />
            </div>
          {/if}
        </section>
      {/if}
    </main>
  </div>
</div>

<style>
  :global(*, *::before, *::after) { box-sizing: border-box; }

  :global(:root) {
    --bg-page: #0b0d11;
    --panel-bg: #15181e;
    --panel-border: rgba(16, 185, 129, 0.16);
    --primary: #10b981;
    --primary-glow: rgba(16, 185, 129, 0.09);
    --text-main: #e8edf3;
    --text-muted: #98a4b3;
    --text-dim: #626e7d;
    --surface-1: #1d222b;
    --surface-2: #222936;
    --surface-input: #0c0f14;
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
    min-width: 320px;
    min-height: 100vh;
    background: radial-gradient(circle at 78% -10%, rgba(16,185,129,0.07), transparent 28rem), var(--bg-page);
    color: var(--text-main);
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
    -webkit-font-smoothing: antialiased;
  }

  :global(button), :global(input) { font-family: inherit; }
  .app { min-height: 100vh; display: grid; grid-template-columns: 400px minmax(0, 1fr); }
  .app-sidebar { position: sticky; top: 0; z-index: 30; min-width: 0; height: 100vh; display: flex; flex-direction: column; overflow: hidden; border-right: 1px solid var(--panel-border); background: rgba(15, 17, 22, 0.97); backdrop-filter: blur(18px); }
  .sidebar-brand { position: relative; min-height: 210px; padding: 0.5rem 0.75rem; display: flex; align-items: center; justify-content: center; }
  .logo { width: 100%; display: flex; align-items: center; justify-content: center; }
  .logo img { display: block; width: min(360px, calc(100% - 1.5rem)); height: auto; }
  .sidebar-toggle { display: none; }
  .sidebar-content { flex: 1; min-height: 0; overflow: hidden; }
  .sidebar-intro { margin: 0 1rem; padding: 1.15rem 0.2rem; border-top: 1px solid rgba(255,255,255,0.06); }
  .sidebar-intro > p:last-child { margin: 0; color: var(--text-muted); font-size: 0.8rem; line-height: 1.6; }
  .sidebar-footer { padding: 1rem; border-top: 1px solid rgba(255,255,255,0.06); }

  .workspace { min-width: 0; min-height: 100vh; }
  .app-main { width: 100%; max-width: 1240px; margin: 0 auto; padding: 2rem 1.5rem 3rem; min-width: 0; }
  .app-main.report-main { max-width: 1340px; padding-right: 0.75rem; padding-left: 0.75rem; }

  .upload-section { max-width: 820px; margin: 5vh auto 0; display: flex; flex-direction: column; gap: 1.2rem; }
  .upload-section.with-files { margin-top: clamp(0.75rem, 2vh, 1.5rem); gap: 0.8rem; }
  .section-kicker, .panel-index { margin: 0 0 0.45rem; color: var(--primary); font-size: 0.68rem; font-weight: 700; letter-spacing: 0.14em; text-transform: uppercase; }
  .upload-header h1, .report-heading h1 { margin: 0; font-size: clamp(1.65rem, 3vw, 2.25rem); letter-spacing: -0.04em; }
  .upload-header > p:last-child { max-width: 580px; margin: 0.65rem 0 0; color: var(--text-muted); font-size: 0.92rem; line-height: 1.65; }
  .signin-note { padding: 0.8rem 1rem; border-left: 2px solid var(--primary); background: var(--primary-glow); color: var(--text-muted); font-size: 0.82rem; }
  .submit-row, .report-row { display: flex; align-items: center; gap: 0.75rem; }
  .submit-row { justify-content: space-between; }
  .file-counter { color: var(--text-muted); font-size: 0.8rem; }
  .file-counter.warn { color: var(--warning); }
  .divider { display: flex; align-items: center; gap: 0.75rem; color: var(--text-dim); font-size: 0.75rem; }
  .divider::before, .divider::after { content: ''; flex: 1; height: 1px; background: var(--panel-border); }
  .report-input { flex: 1; min-width: 0; padding: 0.65rem 0.85rem; border: 1px solid var(--panel-border); border-radius: var(--radius-sm); background: var(--surface-input); color: var(--text-main); font: 0.86rem monospace; outline: none; }
  .report-input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-glow); }
  .report-input::placeholder { color: var(--text-dim); font-family: inherit; }
  .submit-btn { display: flex; align-items: center; gap: 0.5rem; padding: 0.65rem 1.1rem; border: 0; border-radius: var(--radius-sm); background: var(--primary); color: #07110d; font-size: 0.85rem; font-weight: 700; cursor: pointer; }
  .submit-btn:disabled { opacity: 0.38; cursor: not-allowed; }
  .submit-btn:not(:disabled):hover { filter: brightness(1.08); }

  .report-section { display: flex; flex-direction: column; gap: 1.35rem; }
  .report-heading { padding: 0.5rem 0 0.25rem; display: flex; align-items: flex-end; justify-content: space-between; gap: 2rem; }
  .report-meta { margin: 0; display: grid; grid-template-columns: minmax(170px, 1.5fr) repeat(3, auto); gap: 0; border-top: 1px solid var(--panel-border); border-bottom: 1px solid var(--panel-border); }
  .report-meta div { min-width: 0; padding: 0.65rem 0.9rem; border-left: 1px solid rgba(255,255,255,0.06); }
  .report-meta div:first-child { border-left: 0; }
  .report-meta dt { margin-bottom: 0.25rem; color: var(--text-dim); font-size: 0.62rem; letter-spacing: 0.09em; text-transform: uppercase; }
  .report-meta dd { margin: 0; color: var(--text-main); font-size: 0.78rem; font-weight: 600; white-space: nowrap; }
  .report-meta code { display: block; max-width: 180px; overflow: hidden; color: var(--text-muted); font-size: 0.69rem; text-overflow: ellipsis; }

  .top-row { display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(330px, 0.65fr); gap: 1.25rem; align-items: stretch; }
  .panel { padding: 1.3rem; display: flex; flex-direction: column; gap: 1.15rem; border: 1px solid var(--panel-border); border-radius: var(--radius-lg); background: var(--panel-bg); }
  .chart-panel { min-width: 0; }
  .panel-heading h2 { margin: 0; font-size: 1rem; font-weight: 650; }
  .expenses-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 1rem; }
  .empty { color: var(--text-dim); font-size: 0.82rem; }

  .drawer-backdrop { display: none; }

  @media (max-width: 1280px) {
    .top-row { grid-template-columns: 1fr; }
  }

  @media (max-width: 1120px) {
    .report-heading { align-items: stretch; flex-direction: column; gap: 1rem; }
    .report-meta { width: 100%; }
  }

  @media (max-width: 900px) {
    .app { display: block; }
    .app-sidebar { position: fixed; inset: 0 auto 0 0; width: min(320px, 88vw); height: 100vh; transform: translateX(-102%); transition: transform 180ms ease-out; box-shadow: 18px 0 50px rgba(0,0,0,0.4); }
    .app-sidebar.open { transform: translateX(0); }
    .sidebar-brand { min-height: 138px; padding: 0.5rem 0.75rem; }
    .logo img { width: min(280px, calc(100% - 1rem)); }
    .sidebar-toggle { position: fixed; top: 1rem; left: 1rem; z-index: 28; width: 42px; height: 42px; padding: 10px; display: flex; flex-direction: column; justify-content: center; gap: 5px; border: 1px solid var(--panel-border); border-radius: var(--radius-sm); background: rgba(21,24,30,0.92); box-shadow: 0 8px 24px rgba(0,0,0,0.25); backdrop-filter: blur(12px); cursor: pointer; }
    .sidebar-toggle span { display: block; width: 100%; height: 1px; background: var(--text-muted); }
    .app-main { padding-top: 5.25rem; }
    .chart-panel { max-width: none; }
    .drawer-backdrop { position: fixed; inset: 0; z-index: 29; display: block; width: 100%; border: 0; background: rgba(0,0,0,0.58); backdrop-filter: blur(2px); }
  }

  @media (max-width: 680px) {
    .app-main { padding: 5.25rem 0.9rem 2.5rem; }
    .upload-section { margin-top: 0; }
    .report-meta { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    .report-meta div:nth-child(3) { border-left: 0; border-top: 1px solid rgba(255,255,255,0.06); }
    .report-meta div:nth-child(4) { border-top: 1px solid rgba(255,255,255,0.06); }
    .report-meta code { max-width: 135px; }
    .expenses-heading { align-items: flex-start; flex-direction: column; }
    .panel { padding: 1rem; }
  }
</style>

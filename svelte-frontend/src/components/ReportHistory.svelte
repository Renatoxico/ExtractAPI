<script>
  import { formatBRL, formatCount, formatCreatedAt } from '../lib/formatters.js';
  import { isActiveReport, shortReportId } from '../lib/reportHistory.js';

  let {
    items = [],
    loading = false,
    error = '',
    activeReportId = '',
    loadingReportId = '',
    onSelect,
    onRetry,
    onClose = null
  } = $props();
</script>

<aside class="history" class:drawer={Boolean(onClose)} aria-label="Histórico de relatórios">
  <div class="history-header">
    <div>
      <p class="eyebrow">Sua biblioteca</p>
      <h2>Relatórios</h2>
    </div>
    {#if onClose}
      <button class="close-button" type="button" aria-label="Fechar histórico" onclick={onClose}>×</button>
    {/if}
  </div>

  {#if loading}
    <div class="history-state" aria-live="polite">
      <span class="pulse"></span>
      Carregando histórico…
    </div>
  {:else if error}
    <div class="history-state error-state">
      <p>{error}</p>
      <button type="button" onclick={onRetry}>Tentar novamente</button>
    </div>
  {:else if !items.length}
    <div class="history-state empty-state">
      <span class="empty-mark" aria-hidden="true">01</span>
      <p>Seus relatórios processados aparecerão aqui.</p>
    </div>
  {:else}
    <ul class="history-list">
      {#each items as item (item.reportId)}
        <li>
          <button
            type="button"
            class="history-item"
            class:active={isActiveReport(item.reportId, activeReportId)}
            aria-current={isActiveReport(item.reportId, activeReportId) ? 'page' : undefined}
            onclick={() => onSelect(item.reportId)}
            disabled={Boolean(loadingReportId)}
          >
            <time class="item-date" datetime={item.createdAt}>{formatCreatedAt(item.createdAt)}</time>
            <span class="item-summary">
              <strong>{formatBRL(Number(item.total))}</strong>
              <span>{formatCount(Number(item.countExpenses))}</span>
            </span>
            <code title={item.reportId}>{shortReportId(item.reportId)}</code>
            {#if loadingReportId === item.reportId}
              <span class="loading-line" aria-label="Abrindo relatório"></span>
            {/if}
          </button>
        </li>
      {/each}
    </ul>
  {/if}
</aside>

<style>
  .history {
    height: 100%;
    display: flex;
    flex-direction: column;
    background: #11141a;
    border-right: 1px solid var(--panel-border);
  }

  .history-header {
    min-height: 86px;
    padding: 1.25rem 1.15rem 1rem;
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    border-bottom: 1px solid rgba(255,255,255,0.05);
  }

  .eyebrow {
    margin: 0 0 0.2rem;
    color: var(--primary);
    font-size: 0.66rem;
    font-weight: 700;
    letter-spacing: 0.14em;
    text-transform: uppercase;
  }

  h2 { margin: 0; font-size: 1.05rem; }

  .close-button {
    border: 0;
    background: transparent;
    color: var(--text-muted);
    font: inherit;
    font-size: 1.5rem;
    line-height: 1;
    cursor: pointer;
  }

  .history-list {
    flex: 1;
    min-height: 0;
    list-style: none;
    margin: 0;
    padding: 0.75rem;
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
    overflow-y: auto;
    scrollbar-width: thin;
    scrollbar-color: rgba(16, 185, 129, 0.42) transparent;
  }

  .history-list::-webkit-scrollbar {
    width: 6px;
  }

  .history-list::-webkit-scrollbar-track {
    background: transparent;
  }

  .history-list::-webkit-scrollbar-thumb {
    border-radius: 999px;
    background: rgba(16, 185, 129, 0.32);
  }

  .history-list::-webkit-scrollbar-thumb:hover {
    background: rgba(16, 185, 129, 0.58);
  }

  .history-item {
    position: relative;
    width: 100%;
    padding: 0.85rem 0.8rem;
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    grid-template-areas:
      'total date'
      'id count';
    gap: 0.45rem 0.5rem;
    border: 1px solid transparent;
    border-radius: 10px;
    background: transparent;
    color: var(--text-main);
    text-align: left;
    cursor: pointer;
    transition: background var(--transition), border-color var(--transition);
  }

  .history-item:hover { background: var(--surface-1); }
  .history-item:focus-visible { outline: 2px solid var(--primary); outline-offset: 1px; }
  .history-item.active {
    background: rgba(16, 185, 129, 0.08);
    border-color: rgba(16, 185, 129, 0.28);
  }
  .history-item:disabled { cursor: wait; opacity: 0.68; }

  .item-summary {
    display: contents;
  }

  .item-date {
    grid-area: date;
    align-self: center;
    color: var(--text-muted);
    font-size: 0.68rem;
    line-height: 1.3;
    text-align: right;
  }

  .item-summary strong {
    grid-area: total;
    font-size: 0.94rem;
    letter-spacing: -0.02em;
  }

  .item-summary span {
    grid-area: count;
    align-self: center;
    color: var(--text-muted);
    font-size: 0.68rem;
    text-align: right;
  }

  .item-summary span::before {
    display: none;
  }

  code {
    grid-area: id;
    min-width: 0;
    color: var(--text-dim);
    font-size: 0.68rem;
  }

  .loading-line {
    position: absolute;
    left: 0.8rem;
    right: 0.8rem;
    bottom: 0.25rem;
    height: 2px;
    border-radius: 999px;
    background: linear-gradient(90deg, transparent, var(--primary), transparent);
    animation: shimmer 1s linear infinite;
  }

  .history-state {
    margin: 1rem;
    padding: 1.2rem;
    color: var(--text-muted);
    font-size: 0.8rem;
    line-height: 1.5;
    border: 1px dashed rgba(255,255,255,0.1);
    border-radius: var(--radius-md);
  }

  .history-state p { margin: 0; }
  .history-state button {
    margin-top: 0.75rem;
    border: 0;
    background: none;
    color: var(--primary);
    font: inherit;
    font-weight: 600;
    cursor: pointer;
    padding: 0;
  }
  .error-state { color: #fca5a5; }
  .empty-mark { display: block; color: var(--primary); font: 700 1.6rem/1 monospace; margin-bottom: 0.75rem; }
  .pulse { display: inline-block; width: 7px; height: 7px; margin-right: 0.4rem; border-radius: 50%; background: var(--primary); animation: pulse 1s ease-in-out infinite; }

  @keyframes shimmer { from { transform: translateX(-35%); } to { transform: translateX(35%); } }
  @keyframes pulse { 50% { opacity: 0.35; } }
</style>

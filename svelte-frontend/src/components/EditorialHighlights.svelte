<script>
  import { formatBRL, formatCount, formatDate } from '../lib/formatters.js';

  let { largestExpense = null, highestSpendingDay = null, mostRecurring = null, monthStat = null } = $props();
</script>

<section class="highlights" aria-labelledby="highlights-title">
  <header class="highlights-header">
    <span class="header-icon" aria-hidden="true">
      <svg viewBox="0 0 24 24">
        <path d="M13 2 5.5 13h5L10 22l8.5-12h-5L13 2Z" />
      </svg>
    </span>
    <div>
      <h2 id="highlights-title">Insights rápidos</h2>
      <p>Os principais destaques deste relatório.</p>
    </div>
  </header>

  <div class="insight-grid">
    <article class="insight-card largest-card">
      <header class="card-header">
        <span class="card-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path d="M8 4h8v3a4 4 0 0 1-8 0V4Z" />
            <path d="M8 6H4v1a4 4 0 0 0 4 4M16 6h4v1a4 4 0 0 1-4 4M12 11v4M8 19h8M10 15h4v4h-4z" />
          </svg>
        </span>
        <span>Maior gasto</span>
      </header>
      {#if largestExpense}
        <strong class="primary-value">{formatBRL(Number(largestExpense.amount))}</strong>
        <div class="card-details">
          <p class="truncate" title={largestExpense.expenseName}>{largestExpense.expenseName}</p>
          <p>{formatDate(largestExpense.date)}</p>
        </div>
      {:else}
        <strong class="primary-value">—</strong>
      {/if}
    </article>

    <article class="insight-card day-card">
      <header class="card-header">
        <span class="card-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path d="M5 5h14a1 1 0 0 1 1 1v14H4V6a1 1 0 0 1 1-1Z" />
            <path d="M8 2v5M16 2v5M4 10h16" />
          </svg>
        </span>
        <span>Dia mais caro</span>
      </header>
      {#if highestSpendingDay}
        <strong class="primary-value">{formatDate(highestSpendingDay.date)}</strong>
        <div class="card-details">
          <p>{formatBRL(Number(highestSpendingDay.totalAmount))} em {formatCount(highestSpendingDay.transactionCount)}</p>
        </div>
      {:else}
        <strong class="primary-value">—</strong>
      {/if}
    </article>

    <article class="insight-card recurring-card">
      <header class="card-header">
        <span class="card-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path d="M20 7v5h-5M4 17v-5h5" />
            <path d="M6.1 8a7 7 0 0 1 11.5-1L20 12M4 12l2.4 5a7 7 0 0 0 11.5-1" />
          </svg>
        </span>
        <span>Compra recorrente</span>
      </header>
      {#if mostRecurring}
        <strong class="primary-value text-value" title={mostRecurring.expenseName}>{mostRecurring.expenseName}</strong>
        <div class="card-details">
          <p>{mostRecurring.occurrenceCount}× <span>·</span> {formatBRL(Number(mostRecurring.totalAmount))}</p>
        </div>
      {:else}
        <strong class="primary-value">—</strong>
      {/if}
    </article>

    <article class="insight-card month-card">
      <header class="card-header">
        <span class="card-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path d="M5 20v-6h3v6H5ZM11 20V9h3v11h-3ZM17 20V4h3v16h-3Z" />
          </svg>
        </span>
        <span>Mês mais movimentado</span>
      </header>
      {#if monthStat}
        <strong class="primary-value text-value">{monthStat.label}</strong>
        <div class="card-details">
          <p>{formatCount(monthStat.count)}</p>
        </div>
      {:else}
        <strong class="primary-value">—</strong>
      {/if}
    </article>
  </div>
</section>

<style>
  .highlights {
    min-width: 0;
    padding: 1rem;
    display: flex;
    flex-direction: column;
    gap: 0.9rem;
    border: 1px solid rgba(16, 185, 129, 0.22);
    border-radius: var(--radius-lg);
    background:
      radial-gradient(circle at 8% 0%, rgba(16, 185, 129, 0.08), transparent 32%),
      #10151b;
  }

  .highlights-header {
    display: flex;
    align-items: flex-start;
    gap: 0.7rem;
    padding: 0 0.15rem;
  }

  .header-icon {
    width: 20px;
    height: 20px;
    flex: 0 0 20px;
    color: #20e5ae;
  }

  .header-icon svg,
  .card-icon svg {
    display: block;
    width: 100%;
    height: 100%;
  }

  .header-icon svg { fill: currentColor; }

  .highlights-header h2 {
    margin: 0 0 0.25rem;
    color: #20e5ae;
    font-size: 0.68rem;
    font-weight: 750;
    letter-spacing: 0.14em;
    text-transform: uppercase;
  }

  .highlights-header p {
    margin: 0;
    color: var(--text-muted);
    font-size: 0.72rem;
    line-height: 1.4;
  }

  .insight-grid {
    flex: 1;
    min-height: 0;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-auto-rows: minmax(112px, 1fr);
    gap: 0.65rem;
  }

  .insight-card {
    --card-accent: var(--primary);
    min-width: 0;
    padding: 0.85rem;
    display: flex;
    flex-direction: column;
    border: 1px solid rgba(148, 163, 184, 0.12);
    border-radius: 11px;
    background: linear-gradient(145deg, rgba(255,255,255,0.025), transparent 65%), #151b22;
    box-shadow: inset 0 1px rgba(255,255,255,0.018);
  }

  .largest-card { --card-accent: #facc15; }
  .day-card { --card-accent: #3b82f6; }
  .recurring-card { --card-accent: #19d3a2; }
  .month-card { --card-accent: #2dd4bf; }

  .card-header {
    min-width: 0;
    display: flex;
    align-items: center;
    gap: 0.55rem;
    color: #b5c0ce;
    font-size: 0.7rem;
  }

  .card-icon {
    width: 18px;
    height: 18px;
    flex: 0 0 18px;
    color: var(--card-accent);
  }

  .card-icon svg {
    fill: none;
    stroke: currentColor;
    stroke-width: 1.8;
    stroke-linecap: round;
    stroke-linejoin: round;
  }

  .largest-card .card-icon svg,
  .month-card .card-icon svg {
    fill: currentColor;
    stroke: none;
  }

  .primary-value {
    min-width: 0;
    margin-top: 0.85rem;
    display: block;
    overflow: hidden;
    color: var(--text-main);
    font-size: clamp(1rem, 1.45vw, 1.3rem);
    line-height: 1.15;
    letter-spacing: -0.025em;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .text-value {
    font-size: clamp(0.82rem, 1vw, 1rem);
    line-height: 1.3;
    text-transform: capitalize;
    white-space: normal;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 3;
    line-clamp: 3;
  }

  .card-details {
    min-width: 0;
    margin-top: auto;
    padding-top: 0.55rem;
  }

  .card-details p {
    min-width: 0;
    margin: 0.2rem 0 0;
    overflow: hidden;
    color: var(--text-muted);
    font-size: 0.68rem;
    line-height: 1.35;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .card-details .truncate {
    display: -webkit-box;
    white-space: normal;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    line-clamp: 2;
  }

  .card-details span { color: var(--card-accent); }

  @media (max-width: 520px) {
    .insight-grid { grid-template-columns: 1fr; }
    .insight-card { min-height: 112px; }
  }
</style>

<script>
  import { categoryColor } from '../lib/categoryColors.js';
  import { formatBRL, formatCount, formatDate } from '../lib/formatters.js';

  let { largestExpense = null, highestSpendingDay = null, mostRecurring = null, monthStat = null } = $props();
</script>

<div class="highlights" aria-label="Destaques do relatório">
  <article class="story story-peaks" style={largestExpense ? `--story-accent: ${categoryColor(largestExpense.category)}` : ''}>
    <header>
      <span class="story-number">01</span>
      <div>
        <p class="story-kicker">Picos</p>
        <h2>Onde o gasto pesou</h2>
      </div>
    </header>

    <div class="lead-insight">
      <span class="insight-label">Maior gasto individual</span>
      {#if largestExpense}
        <strong>{formatBRL(Number(largestExpense.amount))}</strong>
        <p>{largestExpense.expenseName} <span>·</span> {formatDate(largestExpense.date)}</p>
      {:else}
        <strong>—</strong>
      {/if}
    </div>

    <div class="secondary-insight">
      <span class="insight-label">Dia mais caro</span>
      {#if highestSpendingDay}
        <strong>{formatDate(highestSpendingDay.date)}</strong>
        <p>{formatBRL(Number(highestSpendingDay.totalAmount))} em {formatCount(highestSpendingDay.transactionCount)}</p>
      {:else}
        <strong>—</strong>
      {/if}
    </div>
  </article>

  <article class="story story-patterns">
    <header>
      <span class="story-number">02</span>
      <div>
        <p class="story-kicker">Padrões</p>
        <h2>O que se repetiu</h2>
      </div>
    </header>

    <div class="lead-insight">
      <span class="insight-label">Compra mais recorrente</span>
      {#if mostRecurring}
        <strong class="text-value" title={mostRecurring.expenseName}>{mostRecurring.expenseName}</strong>
        <p>{mostRecurring.occurrenceCount}× <span>·</span> {formatBRL(Number(mostRecurring.totalAmount))}</p>
      {:else}
        <strong>—</strong>
      {/if}
    </div>

    <div class="secondary-insight">
      <span class="insight-label">Mês mais movimentado</span>
      {#if monthStat}
        <strong>{monthStat.label}</strong>
        <p>{formatCount(monthStat.count)}</p>
      {:else}
        <strong>—</strong>
      {/if}
    </div>
  </article>
</div>

<style>
  .highlights {
    min-width: 0;
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    grid-template-rows: repeat(2, minmax(0, 1fr));
    gap: 1px;
    overflow: hidden;
    border: 1px solid var(--panel-border);
    border-radius: var(--radius-lg);
    background: var(--panel-border);
  }

  .story {
    --story-accent: var(--primary);
    min-width: 0;
    padding: 1rem 1.15rem;
    display: grid;
    grid-template-rows: auto 1fr auto;
    gap: 0.85rem;
    background: linear-gradient(145deg, rgba(255,255,255,0.025), transparent 55%), var(--panel-bg);
  }

  .story-patterns { --story-accent: #38bdf8; }

  header {
    display: flex;
    align-items: flex-start;
    gap: 0.75rem;
  }

  .story-number {
    padding-top: 0.1rem;
    color: var(--story-accent);
    font: 700 0.68rem/1 monospace;
    letter-spacing: 0.06em;
  }

  .story-kicker {
    margin: 0 0 0.2rem;
    color: var(--story-accent);
    font-size: 0.67rem;
    font-weight: 700;
    letter-spacing: 0.13em;
    text-transform: uppercase;
  }

  h2 { margin: 0; font-size: 0.98rem; font-weight: 600; }
  .lead-insight, .secondary-insight { min-width: 0; }
  .insight-label { display: block; margin-bottom: 0.4rem; color: var(--text-muted); font-size: 0.72rem; }
  .lead-insight strong { display: block; color: var(--text-main); font-size: clamp(1.25rem, 2.2vw, 1.8rem); line-height: 1.1; }
  .lead-insight .text-value { white-space: normal; overflow-wrap: anywhere; }
  .secondary-insight { padding-top: 0.9rem; border-top: 1px solid rgba(255,255,255,0.07); }
  .secondary-insight strong { display: block; font-size: 0.92rem; text-transform: capitalize; }
  p { margin: 0.4rem 0 0; color: var(--text-muted); font-size: 0.76rem; }
  p span { color: var(--story-accent); }

  @media (max-width: 1280px) and (min-width: 721px) {
    .highlights {
      grid-template-columns: repeat(2, minmax(0, 1fr));
      grid-template-rows: minmax(0, 1fr);
    }
  }

  @media (max-width: 720px) {
    .highlights {
      grid-template-columns: 1fr;
      grid-template-rows: repeat(2, minmax(0, 1fr));
    }
  }
</style>

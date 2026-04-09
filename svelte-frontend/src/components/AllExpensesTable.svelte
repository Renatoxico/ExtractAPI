<script>
  import { categoryColor } from '../lib/categoryColors.js';
  import { categoryIcon } from '../lib/categoryIcons.js';
  import { formatBRL, formatDate } from '../lib/formatters.js';

  let { items } = $props();

  const PAGE_SIZE = 50;

  let filter = $state('');
  let page = $state(1);

  let filtered = $derived(
    items.filter(e => {
      if (!filter) return true;
      const q = filter.toLowerCase();
      return (
        e.expenseName?.toLowerCase().includes(q) ||
        e.category?.toLowerCase().includes(q)
      );
    })
  );

  let paginated = $derived(filtered.slice(0, page * PAGE_SIZE));
  let hasMore = $derived(paginated.length < filtered.length);

  // Reset page when filter changes
  $effect(() => {
    filter;
    page = 1;
  });
</script>

<div class="all-expenses">
  <div class="toolbar">
    <input
      class="search"
      type="text"
      placeholder="Filtrar por nome ou categoria..."
      bind:value={filter}
    />
    <span class="count">{filtered.length} despesas</span>
  </div>

  <div class="table-wrap">
    <table>
      <thead>
        <tr>
          <th class="th-name">Nome</th>
          <th>Categoria</th>
          <th class="th-num">Data</th>
          <th class="th-num">Valor</th>
        </tr>
      </thead>
      <tbody>
        {#each paginated as expense, i (expense.expenseName + expense.date + i)}
          <tr>
            <td class="td-name">{expense.expenseName}</td>
            <td>
              <span class="cat-tag" style="--c: {categoryColor(expense.category)}">
                <span class="cat-icon" style="color: var(--c)">{@html categoryIcon(expense.category)}</span>
                {expense.category}
              </span>
            </td>
            <td class="td-num td-date">{formatDate(expense.date)}</td>
            <td class="td-num td-value">{formatBRL(Number(expense.value))}</td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>

  {#if hasMore}
    <button class="load-more" onclick={() => (page += 1)}>
      Carregar mais ({filtered.length - paginated.length} restantes)
    </button>
  {/if}
</div>

<style>
  .all-expenses {
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }

  .toolbar {
    display: flex;
    align-items: center;
    gap: 1rem;
  }

  .search {
    flex: 1;
    background: var(--surface-input);
    border: 1px solid var(--panel-border);
    border-radius: var(--radius-sm);
    padding: 0.5rem 0.875rem;
    color: var(--text-main);
    font-size: 0.875rem;
    font-family: inherit;
    outline: none;
    transition: border-color var(--transition);
  }

  .search::placeholder { color: var(--text-dim); }
  .search:focus { border-color: var(--primary); }

  .count {
    font-size: 0.8125rem;
    color: var(--text-muted);
    white-space: nowrap;
  }

  .table-wrap {
    overflow-x: auto;
    border-radius: var(--radius-md);
    border: 1px solid var(--panel-border);
  }

  table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.875rem;
  }

  thead { background: var(--surface-1); }

  th {
    padding: 0.75rem 1rem;
    text-align: left;
    font-weight: 600;
    color: var(--text-muted);
    white-space: nowrap;
    border-bottom: 1px solid var(--panel-border);
  }

  td {
    padding: 0.625rem 1rem;
    color: var(--text-main);
    border-bottom: 1px solid rgba(255,255,255,0.04);
  }

  tbody tr:last-child td { border-bottom: none; }
  tbody tr:hover td { background: var(--surface-2); }

  .th-num { text-align: right; }
  .td-num { text-align: right; }

  .td-name {
    font-weight: 500;
    max-width: 240px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .td-date { color: var(--text-muted); font-size: 0.8125rem; }
  .td-value { font-weight: 600; color: var(--primary); }

  .cat-tag {
    display: inline-flex;
    align-items: center;
    gap: 0.375rem;
    font-size: 0.75rem;
    color: var(--text-muted);
    white-space: nowrap;
  }

  .cat-icon {
    display: flex;
    align-items: center;
    flex-shrink: 0;
  }

  .load-more {
    background: var(--surface-1);
    border: 1px solid var(--panel-border);
    border-radius: var(--radius-sm);
    color: var(--text-muted);
    font-family: inherit;
    font-size: 0.875rem;
    padding: 0.625rem 1.25rem;
    cursor: pointer;
    align-self: center;
    transition: color var(--transition), border-color var(--transition);
  }

  .load-more:hover {
    color: var(--primary);
    border-color: var(--primary);
  }
</style>

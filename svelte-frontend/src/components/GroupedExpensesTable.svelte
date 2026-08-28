<script>
  import { categoryColor } from '../lib/categoryColors.js';
  import { categoryIcon } from '../lib/categoryIcons.js';
  import { buildExpenseGroups, filterExpenseGroups } from '../lib/expenseGroups.js';
  import { formatBRL, formatCategory, formatDate } from '../lib/formatters.js';

  let { groups = [], expenses = [] } = $props();

  let filter = $state('');
  let sortKey = $state('totalAmount');
  let sortDir = $state(-1);
  let expanded = $state(new Set());

  let grouped = $derived(buildExpenseGroups(groups, expenses));
  let filtered = $derived(filterExpenseGroups(grouped, filter));
  let sorted = $derived(
    [...filtered].sort((a, b) => {
      const av = sortValue(a, sortKey);
      const bv = sortValue(b, sortKey);
      if (av < bv) return -1 * sortDir;
      if (av > bv) return 1 * sortDir;
      return a.expenseName.localeCompare(b.expenseName, 'pt-BR');
    })
  );

  function sortValue(item, key) {
    if (key === 'category') return formatCategory(item.category).toLocaleLowerCase('pt-BR');
    const value = item[key];
    return typeof value === 'string' ? value.toLocaleLowerCase('pt-BR') : Number(value);
  }

  function toggleSort(key) {
    if (sortKey === key) sortDir *= -1;
    else {
      sortKey = key;
      sortDir = key === 'expenseName' || key === 'category' ? 1 : -1;
    }
  }

  function toggleGroup(key) {
    const next = new Set(expanded);
    if (next.has(key)) next.delete(key);
    else next.add(key);
    expanded = next;
  }

  function sortIcon(key) {
    if (sortKey !== key) return '↕';
    return sortDir === -1 ? '↓' : '↑';
  }
</script>

<div class="expenses-list">
  <div class="toolbar">
    <label class="search-field">
      <span class="sr-only">Buscar despesas</span>
      <span class="search-mark" aria-hidden="true"></span>
      <input type="search" placeholder="Buscar por nome ou categoria…" bind:value={filter} />
    </label>
    <span class="count">{sorted.length} {sorted.length === 1 ? 'grupo' : 'grupos'} · {expenses.length} despesas</span>
  </div>

  <div class="table-wrap">
    <table>
      <thead>
        <tr>
          <th class="th-name"><button onclick={() => toggleSort('expenseName')}>Nome <span>{sortIcon('expenseName')}</span></button></th>
          <th><button onclick={() => toggleSort('category')}>Categoria <span>{sortIcon('category')}</span></button></th>
          <th class="th-num"><button onclick={() => toggleSort('occurrenceCount')}>Ocorrências <span>{sortIcon('occurrenceCount')}</span></button></th>
          <th class="th-num"><button onclick={() => toggleSort('totalAmount')}>Total <span>{sortIcon('totalAmount')}</span></button></th>
        </tr>
      </thead>
      <tbody>
        {#each sorted as group (group.key)}
          <tr class="group-row" class:open={expanded.has(group.key)}>
            <td class="td-name">
              <button
                class="expand-button"
                type="button"
                aria-expanded={expanded.has(group.key)}
                aria-controls={`instances-${encodeURIComponent(group.key)}`}
                onclick={() => toggleGroup(group.key)}
              >
                <span class="chevron" aria-hidden="true">›</span>
                <span title={group.expenseName}>{group.expenseName}</span>
              </button>
            </td>
            <td>
              <span class="cat-tag" style={`--c: ${categoryColor(group.category)}`}>
                <span class="cat-icon" style="color: var(--c)">{@html categoryIcon(group.category)}</span>
                {formatCategory(group.category)}
              </span>
            </td>
            <td class="td-num">{group.occurrenceCount}×</td>
            <td class="td-num td-value">{formatBRL(Number(group.totalAmount))}</td>
          </tr>
          {#if expanded.has(group.key)}
            <tr class="instances-row">
              <td colspan="4">
                <div class="instances" id={`instances-${encodeURIComponent(group.key)}`}>
                  {#if group.instances.length}
                    {#each group.instances as expense, index (expense.expenseId)}
                      <div class="instance">
                        <span class="instance-index">{String(index + 1).padStart(2, '0')}</span>
                        <span class="instance-name">Lançamento <code>#{expense.expenseId}</code></span>
                        <time datetime={expense.date}>{formatDate(expense.date)}</time>
                        <strong>{formatBRL(Number(expense.amount))}</strong>
                      </div>
                    {/each}
                  {:else}
                    <p class="no-instances">Nenhum lançamento correspondente encontrado.</p>
                  {/if}
                </div>
              </td>
            </tr>
          {/if}
        {:else}
          <tr><td class="empty" colspan="4">Nenhuma despesa corresponde à busca.</td></tr>
        {/each}
      </tbody>
    </table>
  </div>
</div>

<style>
  .expenses-list { display: flex; flex-direction: column; gap: 1rem; }
  .toolbar { display: flex; align-items: center; gap: 1rem; }
  .search-field { position: relative; flex: 1; }
  .search-field input {
    width: 100%;
    padding: 0.65rem 0.9rem 0.65rem 2.3rem;
    border: 1px solid var(--panel-border);
    border-radius: var(--radius-sm);
    background: var(--surface-input);
    color: var(--text-main);
    font: inherit;
    font-size: 0.84rem;
    outline: none;
  }
  .search-field input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-glow); }
  .search-field input::placeholder { color: var(--text-dim); }
  .search-mark { position: absolute; top: 50%; left: 0.85rem; width: 10px; height: 10px; border: 1.5px solid var(--text-dim); border-radius: 50%; transform: translateY(-58%); pointer-events: none; }
  .search-mark::after { content: ''; position: absolute; width: 5px; height: 1.5px; right: -4px; bottom: -2px; background: var(--text-dim); transform: rotate(45deg); }
  .count { color: var(--text-muted); font-size: 0.75rem; white-space: nowrap; }

  .table-wrap { overflow-x: auto; border: 1px solid var(--panel-border); border-radius: var(--radius-md); }
  table { width: 100%; border-collapse: collapse; font-size: 0.84rem; }
  thead { background: var(--surface-1); }
  th { padding: 0; color: var(--text-muted); text-align: left; white-space: nowrap; border-bottom: 1px solid var(--panel-border); }
  th button { width: 100%; padding: 0.75rem 1rem; border: 0; background: none; color: inherit; font: inherit; font-weight: 600; text-align: left; cursor: pointer; }
  th button:hover { color: var(--primary); }
  th button span { margin-left: 0.2rem; color: var(--text-dim); }
  .th-num button, .td-num { text-align: right; }
  td { padding: 0.68rem 1rem; border-bottom: 1px solid rgba(255,255,255,0.04); }
  .group-row:hover td, .group-row.open td { background: var(--surface-2); }
  .td-name { min-width: 220px; font-weight: 500; }
  .expand-button { width: 100%; min-width: 0; display: flex; align-items: center; gap: 0.65rem; padding: 0; border: 0; background: none; color: var(--text-main); font: inherit; font-weight: 500; text-align: left; cursor: pointer; }
  .expand-button > span:last-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .expand-button:focus-visible { outline: 2px solid var(--primary); outline-offset: 3px; border-radius: 2px; }
  .chevron { color: var(--primary); font-size: 1.15rem; transition: transform var(--transition); }
  .group-row.open .chevron { transform: rotate(90deg); }
  .cat-tag { display: inline-flex; align-items: center; gap: 0.4rem; color: var(--text-muted); font-size: 0.74rem; white-space: nowrap; }
  .cat-icon { display: flex; color: var(--c); }
  .td-value { color: var(--primary); font-weight: 700; }

  .instances-row td { padding: 0; background: #101319; }
  .instances { padding: 0.45rem 1rem 0.55rem 2.75rem; box-shadow: inset 3px 0 0 var(--primary); }
  .instance { min-height: 38px; display: grid; grid-template-columns: 28px minmax(140px, 1fr) 110px 110px; align-items: center; gap: 0.75rem; color: var(--text-muted); border-bottom: 1px solid rgba(255,255,255,0.04); font-size: 0.77rem; }
  .instance:last-child { border-bottom: 0; }
  .instance-index { color: var(--text-dim); font: 0.66rem monospace; }
  .instance-name code { color: var(--text-dim); }
  .instance time { text-align: right; }
  .instance strong { color: var(--text-main); text-align: right; }
  .no-instances, .empty { color: var(--text-dim); text-align: center; }
  .empty { padding: 2rem; }
  .sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0,0,0,0); white-space: nowrap; border: 0; }

  @media (max-width: 680px) {
    .toolbar { align-items: stretch; flex-direction: column; gap: 0.55rem; }
    .count { align-self: flex-end; }
    th:nth-child(2), td:nth-child(2) { display: none; }
    .instances { padding-left: 1rem; }
    .instance { grid-template-columns: 24px minmax(90px, 1fr) auto; }
    .instance time { display: none; }
  }
</style>

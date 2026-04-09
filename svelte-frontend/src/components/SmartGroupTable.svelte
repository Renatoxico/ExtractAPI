<script>
  import { categoryColor } from '../lib/categoryColors.js';
  import { categoryIcon } from '../lib/categoryIcons.js';
  import { formatBRL } from '../lib/formatters.js';

  let { items } = $props();

  let sortKey = $state('total');
  let sortDir = $state(-1); // -1 = desc, 1 = asc

  function toggleSort(key) {
    if (sortKey === key) {
      sortDir = sortDir * -1;
    } else {
      sortKey = key;
      sortDir = -1;
    }
  }

  let sorted = $derived(
    [...items].sort((a, b) => {
      const av = typeof a[sortKey] === 'string' ? a[sortKey].toLowerCase() : Number(a[sortKey]);
      const bv = typeof b[sortKey] === 'string' ? b[sortKey].toLowerCase() : Number(b[sortKey]);
      if (av < bv) return -1 * sortDir;
      if (av > bv) return 1 * sortDir;
      return 0;
    })
  );

  function sortIcon(key) {
    if (sortKey !== key) return '↕';
    return sortDir === -1 ? '↓' : '↑';
  }
</script>

<div class="table-wrap">
  <table>
    <thead>
      <tr>
        <th class="th-name">
          <button onclick={() => toggleSort('expenseName')}>
            Nome {sortIcon('expenseName')}
          </button>
        </th>
        <th>
          <button onclick={() => toggleSort('category')}>
            Categoria {sortIcon('category')}
          </button>
        </th>
        <th class="th-num">
          <button onclick={() => toggleSort('instances')}>
            Ocorrências {sortIcon('instances')}
          </button>
        </th>
        <th class="th-num">
          <button onclick={() => toggleSort('total')}>
            Total {sortIcon('total')}
          </button>
        </th>
      </tr>
    </thead>
    <tbody>
      {#each sorted as item (item.expenseName)}
        <tr>
          <td class="td-name">{item.expenseName}</td>
          <td>
            <span class="cat-tag" style="--c: {categoryColor(item.category)}">
              <span class="cat-icon" style="color: var(--c)">{@html categoryIcon(item.category)}</span>
              {item.category}
            </span>
          </td>
          <td class="td-num">{item.instances}×</td>
          <td class="td-num td-value">{formatBRL(Number(item.total))}</td>
        </tr>
      {/each}
    </tbody>
  </table>
</div>

<style>
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
    padding: 0;
    text-align: left;
    font-weight: 600;
    color: var(--text-muted);
    white-space: nowrap;
    border-bottom: 1px solid var(--panel-border);
  }

  th button {
    background: none;
    border: none;
    cursor: pointer;
    color: inherit;
    font: inherit;
    font-weight: 600;
    padding: 0.75rem 1rem;
    width: 100%;
    text-align: left;
    transition: color var(--transition);
  }

  th button:hover { color: var(--primary); }

  td {
    padding: 0.625rem 1rem;
    color: var(--text-main);
    border-bottom: 1px solid rgba(255,255,255,0.04);
  }

  tbody tr:last-child td { border-bottom: none; }

  tbody tr:hover td { background: var(--surface-2); }

  .th-num button,
  .td-num { text-align: right; }

  .td-name {
    font-weight: 500;
    max-width: 220px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

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
</style>

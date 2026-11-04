<script>
  import { tick } from 'svelte';
  import { addManualExpense, changeExpenseCategory, deleteExpense } from '../lib/api.js';
  import { countExpensesByName, parseExpenseAmount, isExpenseDate } from '../lib/expenseEditing.js';
  import { formatBRL, formatDate } from '../lib/formatters.js';
  import GroupedExpensesTable from './GroupedExpensesTable.svelte';
  import CategoryPicker from './CategoryPicker.svelte';

  let { report, onUpdate } = $props();
  let dialog;
  let mode = $state('add');
  let selected = $state(null);
  let name = $state('');
  let amount = $state('');
  let date = $state('');
  let category = $state('');
  let pending = $state(false);
  let error = $state('');
  let notice = $state('');
  let affected = $derived(countExpensesByName(report.expenses ?? [], selected?.expenseName));

  async function open(nextMode, expense = null) {
    mode = nextMode;
    selected = expense;
    name = '';
    amount = '';
    date = '';
    category = nextMode === 'category' ? expense.category ?? '' : '';
    error = '';
    notice = '';
    await tick();
    if (dialog?.isConnected && !dialog.open) dialog.showModal();
  }

  function formatAmount() {
    const parsed = parseExpenseAmount(amount);
    if (parsed !== null) amount = parsed.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  async function save(event) {
    event.preventDefault();
    if (pending) return;
    error = '';
    const parsed = parseExpenseAmount(amount);
    if (mode === 'add' && (!name.trim() || parsed === null || !isExpenseDate(date))) {
      error = 'Preencha a descrição, um valor maior que zero (ex.: 1.234,56) e uma data válida.';
      return;
    }
    if (mode !== 'delete' && !category) {
      error = 'Selecione uma categoria.';
      return;
    }
    pending = true;
    try {
      let updated;
      if (mode === 'add') updated = await addManualExpense(report.reportId, { name: name.trim(), amount: parsed, date, category });
      else if (mode === 'category') updated = await changeExpenseCategory(report.reportId, selected.expenseName, category);
      else updated = await deleteExpense(report.reportId, selected.expenseId);
      onUpdate(updated);
      notice = mode === 'add' ? 'Despesa adicionada.' : mode === 'delete' ? 'Despesa excluída.' : 'Categoria atualizada para todas as despesas com esse nome.';
      dialog.close();
    } catch (failure) {
      error = failure.message || 'Não foi possível salvar a alteração. Tente novamente.';
    } finally {
      pending = false;
    }
  }
</script>

<div class="editing-toolbar">
  <p>Abra um grupo para alterar a categoria ou excluir um lançamento.</p>
  <button type="button" class="primary" disabled={pending} onclick={() => open('add')}>+ Adicionar despesa</button>
</div>
<p class="notice" role="status">{notice}</p>
<GroupedExpensesTable groups={report.expenseGroups} expenses={report.expenses}
  disabled={pending} onCategory={(expense) => open('category', expense)} onDelete={(expense) => open('delete', expense)} />

<dialog bind:this={dialog} aria-labelledby="expense-edit-title" oncancel={(event) => { if (pending) event.preventDefault(); }}>
  <form onsubmit={save} aria-busy={pending}>
    <h2 id="expense-edit-title">{mode === 'add' ? 'Adicionar despesa' : mode === 'category' ? 'Alterar categoria' : 'Excluir despesa?'}</h2>
    {#if mode === 'add'}
      <label class="field">Descrição
        <input type="text" bind:value={name} required disabled={pending} placeholder="Ex.: Almoço" />
      </label>
      <div class="form-row">
        <label class="field">Valor (R$)
          <input type="text" inputmode="decimal" bind:value={amount} onblur={formatAmount} required disabled={pending} placeholder="0,00" aria-describedby="amount-help" />
          <small id="amount-help">Ex.: 1.234,56</small>
        </label>
        <label class="field">Data
          <input type="date" bind:value={date} min="0001-01-01" max="9999-12-31" required disabled={pending} />
        </label>
      </div>
    {:else if mode === 'category'}
      <p class="scope">Aplicar a todas as despesas “{selected?.expenseName}” ({affected}) neste relatório.</p>
      <p class="help">A alteração inclui lançamentos com esse mesmo nome em outras categorias.</p>
    {:else}
      <p class="scope">{selected?.expenseName}</p>
      <p>{formatDate(selected?.date)} · {formatBRL(Number(selected?.amount))}</p>
      <p class="help">Somente este lançamento será excluído. Esta ação não pode ser desfeita.</p>
    {/if}
    {#if mode !== 'delete'}<CategoryPicker bind:value={category} disabled={pending} />{/if}
    {#if error}<p class="error" role="alert">{error}</p>{/if}
    <div class="actions">
      <button type="button" disabled={pending} onclick={() => dialog.close()}>Cancelar</button>
      <button type="submit" class:danger={mode === 'delete'} class:primary={mode !== 'delete'} disabled={pending}>
        {pending ? 'Salvando…' : mode === 'delete' ? 'Excluir despesa' : 'Salvar'}
      </button>
    </div>
  </form>
</dialog>

<style>
  .editing-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 1rem; }
  .editing-toolbar p, .help { color: var(--text-muted); font-size: 0.8rem; line-height: 1.5; }
  button { min-height: 44px; padding: 0.65rem 1rem; border: 1px solid var(--panel-border); border-radius: var(--radius-sm); background: var(--surface-1); color: var(--text-main); font: inherit; font-size: 0.85rem; cursor: pointer; }
  button.primary { background: var(--primary); color: #07110d; font-weight: 700; }
  button.danger { background: var(--danger); color: white; font-weight: 700; }
  button:disabled { opacity: 0.5; cursor: wait; }
  button:focus-visible, input:focus-visible { outline: 2px solid var(--primary); outline-offset: 3px; }
  .notice { color: var(--primary); margin: 0; font-size: 0.85rem; }
  .notice:empty { display: none; }
  dialog { width: min(600px, calc(100% - 1.5rem)); max-height: calc(100dvh - 2rem); padding: 1.4rem; border: 1px solid var(--panel-border); border-radius: var(--radius-lg); background: var(--panel-bg); color: var(--text-main); }
  dialog::backdrop { background: rgba(0,0,0,0.7); backdrop-filter: blur(3px); }
  form { display: flex; flex-direction: column; gap: 1rem; }
  h2 { font-size: 1.15rem; margin: 0; }
  p { margin: 0; }
  .scope { overflow-wrap: anywhere; line-height: 1.5; }
  .field { min-width: 0; display: flex; flex-direction: column; gap: 0.5rem; font-size: 0.85rem; }
  input { width: 100%; min-width: 0; min-height: 44px; padding: 0.7rem; background: var(--surface-input); border: 1px solid var(--panel-border); border-radius: var(--radius-sm); color: var(--text-main); font: inherit; color-scheme: dark; }
  small { color: var(--text-muted); }
  .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 0.8rem; }
  .actions { display: flex; justify-content: flex-end; gap: 0.6rem; }
  .error { padding: 0.8rem; background: var(--danger-bg); color: #fca5a5; border-radius: var(--radius-sm); font-size: 0.85rem; line-height: 1.5; }
  @media (max-width: 480px) {
    .editing-toolbar { flex-direction: column; align-items: stretch; }
    .form-row { grid-template-columns: 1fr; }
    dialog { padding: 1rem; }
  }
</style>

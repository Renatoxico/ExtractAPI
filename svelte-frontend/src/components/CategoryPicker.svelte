<script>
  import { EXPENSE_CATEGORIES } from '../lib/expenseEditing.js';
  import { categoryColor } from '../lib/categoryColors.js';
  import { categoryIcon } from '../lib/categoryIcons.js';

  let { value = $bindable(''), disabled = false } = $props();
</script>

<fieldset {disabled}>
  <legend>Categoria</legend>
  <div class="categories">
    {#each EXPENSE_CATEGORIES as category}
      <label class:selected={value === category} style={`--category-color: ${categoryColor(category)}`}>
        <input type="radio" name="expense-category" bind:group={value} value={category} required />
        <span class="icon" aria-hidden="true">{@html categoryIcon(category)}</span>
        <span>{category}</span>
      </label>
    {/each}
  </div>
</fieldset>

<style>
  fieldset { min-width: 0; margin: 0; padding: 0; border: 0; }
  legend { margin-bottom: 0.6rem; font-size: 0.85rem; font-weight: 600; }
  .categories { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0.4rem; }
  label { display: flex; align-items: center; gap: 0.5rem; min-height: 48px; padding: 0.6rem; border: 1px solid var(--panel-border); border-radius: var(--radius-sm); font-size: 0.75rem; line-height: 1.4; cursor: pointer; }
  label.selected { background: var(--surface-2); border-color: var(--category-color); }
  label:focus-within { outline: 2px solid var(--primary); outline-offset: 2px; }
  input { accent-color: var(--category-color); margin: 0; }
  .icon { display: flex; flex-shrink: 0; color: var(--category-color); }
  fieldset:disabled { opacity: 0.5; }
  @media (max-width: 480px) { .categories { grid-template-columns: 1fr; } }
</style>

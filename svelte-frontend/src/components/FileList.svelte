<script>
  let { files, onRemove } = $props();

  const MAX_SIZE = 512 * 1024;

  function formatSize(bytes) {
    if (bytes < 1024) return `${bytes} B`;
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
</script>

{#if files.length}
  <ul class="file-list">
    {#each files as file, i (file.name + i)}
      {@const oversized = file.size > MAX_SIZE}
      <li class="file-pill" class:error={oversized}>
        <span class="file-icon">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
          </svg>
        </span>
        <span class="file-name">{file.name}</span>
        <span class="file-size" class:oversized>{formatSize(file.size)}</span>
        {#if oversized}
          <span class="file-warning" title="Arquivo maior que 512 KB">⚠</span>
        {/if}
        <button class="remove-btn" onclick={() => onRemove(i)} aria-label="Remover {file.name}">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </li>
    {/each}
  </ul>
{/if}

<style>
  .file-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  .file-pill {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    background: var(--surface-1);
    border: 1px solid var(--panel-border);
    border-radius: var(--radius-sm);
    padding: 0.5rem 0.75rem;
    font-size: 0.875rem;
  }

  .file-pill.error {
    border-color: rgba(239, 68, 68, 0.4);
    background: rgba(239, 68, 68, 0.05);
  }

  .file-icon { color: var(--primary); flex-shrink: 0; }

  .file-name {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--text-main);
  }

  .file-size {
    color: var(--text-muted);
    white-space: nowrap;
    font-size: 0.8125rem;
  }

  .file-size.oversized { color: var(--danger); }

  .file-warning { color: var(--warning); font-size: 0.875rem; }

  .remove-btn {
    background: none;
    border: none;
    cursor: pointer;
    color: var(--text-dim);
    display: flex;
    align-items: center;
    padding: 2px;
    border-radius: 4px;
    transition: color var(--transition), background var(--transition);
    flex-shrink: 0;
  }

  .remove-btn:hover {
    color: var(--danger);
    background: var(--danger-bg);
  }
</style>

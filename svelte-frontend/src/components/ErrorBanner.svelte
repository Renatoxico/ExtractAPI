<script>
  let { errorCode, message, details = null, onDismiss } = $props();
</script>

<div class="error-banner" role="alert">
  <div class="error-icon">
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <circle cx="12" cy="12" r="10"/>
      <line x1="12" y1="8" x2="12" y2="12"/>
      <line x1="12" y1="16" x2="12.01" y2="16"/>
    </svg>
  </div>
  <div class="error-body">
    <p class="error-message">{message}</p>
    {#if details}
      <p class="error-details">{details}</p>
    {/if}
    <code class="error-code">{errorCode}</code>
  </div>
  <button class="dismiss-btn" onclick={onDismiss} aria-label="Fechar erro">
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
      <line x1="18" y1="6" x2="6" y2="18"/>
      <line x1="6" y1="6" x2="18" y2="18"/>
    </svg>
  </button>
</div>

<style>
  .error-banner {
    display: flex;
    align-items: flex-start;
    gap: 0.75rem;
    background: var(--danger-bg);
    border: 1px solid rgba(239, 68, 68, 0.3);
    border-radius: var(--radius-md);
    padding: 1rem 1rem 1rem 1.25rem;
  }

  .error-icon { color: var(--danger); flex-shrink: 0; margin-top: 1px; }

  .error-body { flex: 1; }

  .error-message {
    color: var(--text-main);
    font-weight: 500;
    margin: 0 0 0.25rem;
    font-size: 0.9375rem;
  }

  .error-details {
    color: var(--text-muted);
    font-size: 0.875rem;
    margin: 0 0 0.5rem;
  }

  .error-code {
    font-family: monospace;
    font-size: 0.75rem;
    color: var(--danger);
    background: rgba(239, 68, 68, 0.1);
    padding: 2px 6px;
    border-radius: 4px;
  }

  .dismiss-btn {
    background: none;
    border: none;
    cursor: pointer;
    color: var(--text-dim);
    display: flex;
    align-items: center;
    padding: 2px;
    border-radius: 4px;
    flex-shrink: 0;
    transition: color var(--transition);
  }

  .dismiss-btn:hover { color: var(--danger); }
</style>

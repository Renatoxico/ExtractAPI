<script>
  let { label, value, sub = null, color = null, icon = null } = $props();
</script>

<div class="stat-card" style={color ? `--accent: ${color}` : ''}>
  <div class="stat-header">
    {#if icon}
      <span class="stat-icon" style="color: var(--accent)">{@html icon}</span>
    {/if}
    <p class="stat-label">{label}</p>
  </div>
  <p class="stat-value">{value}</p>
  {#if sub}
    <p class="stat-sub">{sub}</p>
  {/if}
</div>

<style>
  .stat-card {
    --accent: var(--primary);
    background: var(--surface-1);
    border: 1px solid var(--panel-border);
    border-left: 3px solid var(--accent);
    border-radius: var(--radius-md);
    padding: 1.5rem 1.25rem 1.5rem 1.125rem;
    display: flex;
    flex-direction: column;
    gap: 0.35rem;
    justify-content: center;
  }

  .stat-header {
    display: flex;
    align-items: center;
    gap: 0.4rem;
  }

  .stat-icon {
    display: flex;
    align-items: center;
    flex-shrink: 0;
    opacity: 0.9;
  }

  .stat-label {
    font-size: 0.8rem;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    color: var(--text-muted);
    margin: 0;
    font-weight: 500;
  }

  .stat-value {
    font-size: 1.35rem;
    font-weight: 700;
    color: var(--text-main);
    margin: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .stat-sub {
    font-size: 0.8125rem;
    color: var(--text-muted);
    margin: 0;
  }

</style>

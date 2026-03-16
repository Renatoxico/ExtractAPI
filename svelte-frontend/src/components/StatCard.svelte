<script>
  let { label, value, sub = null, color = null, icon = null } = $props();
</script>

<div class="stat-card" style={color ? `--accent: ${color}` : ''}>
  {#if icon}
    <div class="stat-icon">{@html icon}</div>
  {/if}
  <p class="stat-label">{label}</p>
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
    padding: 1.25rem 1.25rem 1.25rem 1.125rem;
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }

  .stat-label {
    font-size: 0.75rem;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    color: var(--text-muted);
    margin: 0;
    font-weight: 500;
  }

  .stat-value {
    font-size: 1.25rem;
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

  .stat-icon {
    color: var(--accent);
    margin-bottom: 0.25rem;
  }
</style>

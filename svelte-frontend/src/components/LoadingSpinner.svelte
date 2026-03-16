<script>
  let { message = 'Processando seus extratos...' } = $props();
</script>

<div class="spinner-wrap">
  <div class="spinner"></div>
  <p class="spinner-msg">{message}</p>
</div>

<style>
  .spinner-wrap {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 1rem;
    padding: 3rem;
  }

  .spinner {
    width: 48px;
    height: 48px;
    border: 3px solid rgba(16, 185, 129, 0.2);
    border-top-color: var(--primary);
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  .spinner-msg {
    color: var(--text-muted);
    font-size: 0.9375rem;
    margin: 0;
  }
</style>

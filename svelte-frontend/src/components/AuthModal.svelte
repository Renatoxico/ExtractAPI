<script>
  import { signInWithGoogle } from '../lib/auth.js'

  let loading = $state(false)
  let error = $state(null)

  async function handleGoogle() {
    loading = true
    error = null
    try {
      await signInWithGoogle()
      // Page will redirect for OAuth; loading state stays until redirect
    } catch (e) {
      error = 'Não foi possível iniciar o login. Tente novamente.'
      loading = false
    }
  }
</script>

<div class="backdrop">
  <div class="modal">
    <div class="modal-logo">
      <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <rect x="2" y="3" width="20" height="14" rx="2"/>
        <path d="M8 21h8M12 17v4"/>
        <path d="M6 9l3 3 3-3 3 3 3-3" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <span class="modal-title">Extract</span>
    </div>

    <h2>Acesse sua conta</h2>
    <p class="subtitle">Faça login para analisar seus extratos bancários</p>

    {#if error}
      <div class="error-msg">{error}</div>
    {/if}

    <div class="providers">
      <button class="provider-btn" onclick={handleGoogle} disabled={loading}>
        {#if loading}
          <span class="spinner"></span>
          Redirecionando...
        {:else}
          <svg width="18" height="18" viewBox="0 0 24 24">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
          </svg>
          Entrar com Google
        {/if}
      </button>

      <button class="provider-btn coming-soon" disabled>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
          <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.8-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z"/>
        </svg>
        Entrar com Apple
        <span class="badge">Em breve</span>
      </button>

      <button class="provider-btn coming-soon" disabled>
        <svg width="18" height="18" viewBox="0 0 24 24">
          <path fill="#f25022" d="M1 1h10v10H1z"/>
          <path fill="#00a4ef" d="M13 1h10v10H13z"/>
          <path fill="#7fba00" d="M1 13h10v10H1z"/>
          <path fill="#ffb900" d="M13 13h10v10H13z"/>
        </svg>
        Entrar com Microsoft
        <span class="badge">Em breve</span>
      </button>
    </div>
  </div>
</div>

<style>
  .backdrop {
    position: fixed;
    inset: 0;
    background: rgba(15, 17, 21, 0.85);
    backdrop-filter: blur(8px);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 100;
  }

  .modal {
    background: var(--panel-bg);
    border: 1px solid var(--panel-border);
    border-radius: var(--radius-lg);
    padding: 2.5rem 2rem;
    width: 100%;
    max-width: 400px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 1rem;
  }

  .modal-logo {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    color: var(--primary);
  }

  .modal-title {
    font-size: 1.25rem;
    font-weight: 700;
  }

  h2 {
    margin: 0.25rem 0 0;
    font-size: 1.25rem;
    font-weight: 700;
    color: var(--text-main);
  }

  .subtitle {
    margin: 0;
    font-size: 0.875rem;
    color: var(--text-muted);
    text-align: center;
  }

  .error-msg {
    width: 100%;
    background: var(--danger-bg);
    color: var(--danger);
    border: 1px solid var(--danger);
    border-radius: var(--radius-sm);
    padding: 0.625rem 0.875rem;
    font-size: 0.875rem;
    text-align: center;
  }

  .providers {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    width: 100%;
    margin-top: 0.5rem;
  }

  .provider-btn {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    width: 100%;
    padding: 0.75rem 1rem;
    background: var(--surface-1);
    border: 1px solid var(--panel-border);
    border-radius: var(--radius-sm);
    color: var(--text-main);
    font-family: inherit;
    font-size: 0.9375rem;
    font-weight: 500;
    cursor: pointer;
    transition: border-color var(--transition), background var(--transition);
  }

  .provider-btn:not(:disabled):hover {
    border-color: var(--primary);
    background: var(--surface-2);
  }

  .provider-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .provider-btn.coming-soon {
    opacity: 0.45;
  }

  .badge {
    margin-left: auto;
    font-size: 0.7rem;
    font-weight: 600;
    color: var(--text-dim);
    background: var(--surface-2);
    border-radius: 4px;
    padding: 0.1rem 0.4rem;
    letter-spacing: 0.03em;
    text-transform: uppercase;
  }

  .spinner {
    display: inline-block;
    width: 16px;
    height: 16px;
    border: 2px solid var(--panel-border);
    border-top-color: var(--primary);
    border-radius: 50%;
    animation: spin 0.7s linear infinite;
  }

  @keyframes spin { to { transform: rotate(360deg); } }
</style>

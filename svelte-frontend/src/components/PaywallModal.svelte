<script>
  import { auth } from '../lib/auth.svelte.js'

  const REVENUECAT_CHECKOUT_URL = import.meta.env.VITE_REVENUECAT_CHECKOUT_URL

  let checking = $state(false)

  function openCheckout() {
    const url = `${REVENUECAT_CHECKOUT_URL}?app_user_id=${auth.session.user.id}`
    window.open(url, '_blank')
  }

  async function handleCheckAgain() {
    checking = true
    await auth.checkPremiumStatus()
    checking = false
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

    <div class="lock-icon">
      <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
        <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
      </svg>
    </div>

    <h2>Acesso Premium</h2>
    <p class="subtitle">
      Para analisar seus extratos você precisa de uma assinatura ativa.<br/>
      Acesso liberado para web e iOS com um único plano.
    </p>

    {#if auth.session}
      <p class="user-email">{auth.session.user.email}</p>
    {/if}

    <div class="pricing-card">
      <span class="price-label">Assinatura mensal</span>
      <span class="price">R$ 9,90<span class="per">/mês</span></span>
      <ul class="features">
        <li>Análise de até 6 extratos por sessão</li>
        <li>Categorização automática com IA</li>
        <li>Acesso no app iOS e na web</li>
      </ul>
    </div>

    <button class="cta-btn" onclick={openCheckout}>
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
        <path d="M20 12V22H4V12"/>
        <path d="M22 7H2v5h20V7z"/>
        <path d="M12 22V7"/>
        <path d="M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z"/>
        <path d="M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z"/>
      </svg>
      Assinar agora
    </button>

    <button class="check-btn" onclick={handleCheckAgain} disabled={checking}>
      {#if checking}
        <span class="spinner"></span>
        Verificando...
      {:else}
        Já assinei — verificar acesso
      {/if}
    </button>

    <button class="signout-link" onclick={() => auth.signOut()}>Sair da conta</button>
  </div>
</div>

<style>
  .backdrop {
    position: fixed;
    inset: 0;
    background: rgba(15, 17, 21, 0.88);
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
    max-width: 420px;
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

  .lock-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 56px;
    height: 56px;
    background: var(--primary-glow);
    border: 1px solid rgba(16, 185, 129, 0.25);
    border-radius: 50%;
    color: var(--primary);
    margin: 0.25rem 0;
  }

  h2 {
    margin: 0;
    font-size: 1.375rem;
    font-weight: 700;
    color: var(--text-main);
  }

  .subtitle {
    margin: 0;
    font-size: 0.875rem;
    color: var(--text-muted);
    text-align: center;
    line-height: 1.6;
  }

  .user-email {
    margin: 0;
    font-size: 0.8125rem;
    color: var(--text-dim);
    background: var(--surface-1);
    padding: 0.25rem 0.75rem;
    border-radius: var(--radius-sm);
  }

  .pricing-card {
    width: 100%;
    background: var(--surface-1);
    border: 1px solid var(--panel-border);
    border-radius: var(--radius-md);
    padding: 1.25rem;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  .price-label {
    font-size: 0.8125rem;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.05em;
    font-weight: 600;
  }

  .price {
    font-size: 2rem;
    font-weight: 800;
    color: var(--primary);
  }

  .per {
    font-size: 1rem;
    font-weight: 400;
    color: var(--text-muted);
  }

  .features {
    margin: 0.25rem 0 0;
    padding: 0 0 0 1.125rem;
    display: flex;
    flex-direction: column;
    gap: 0.3rem;
  }

  .features li {
    font-size: 0.875rem;
    color: var(--text-muted);
  }

  .cta-btn {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    width: 100%;
    padding: 0.875rem 1rem;
    background: var(--primary);
    border: none;
    border-radius: var(--radius-sm);
    color: #0f1115;
    font-family: inherit;
    font-size: 1rem;
    font-weight: 700;
    cursor: pointer;
    justify-content: center;
    transition: opacity var(--transition), transform var(--transition);
  }

  .cta-btn:hover {
    opacity: 0.9;
    transform: translateY(-1px);
  }

  .check-btn {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    width: 100%;
    padding: 0.75rem 1rem;
    background: none;
    border: 1px solid var(--panel-border);
    border-radius: var(--radius-sm);
    color: var(--text-muted);
    font-family: inherit;
    font-size: 0.9375rem;
    cursor: pointer;
    justify-content: center;
    transition: border-color var(--transition), color var(--transition);
  }

  .check-btn:not(:disabled):hover {
    border-color: var(--primary);
    color: var(--primary);
  }

  .check-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .signout-link {
    background: none;
    border: none;
    color: var(--text-dim);
    font-family: inherit;
    font-size: 0.8125rem;
    cursor: pointer;
    padding: 0;
    text-decoration: underline;
    margin-top: -0.25rem;
  }

  .signout-link:hover {
    color: var(--text-muted);
  }

  .spinner {
    display: inline-block;
    width: 14px;
    height: 14px;
    border: 2px solid var(--panel-border);
    border-top-color: var(--primary);
    border-radius: 50%;
    animation: spin 0.7s linear infinite;
  }

  @keyframes spin { to { transform: rotate(360deg); } }
</style>

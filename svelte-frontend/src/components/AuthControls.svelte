<script>
  import { GoogleAuthProvider, signInWithPopup, signOut } from 'firebase/auth'
  import { auth } from '../lib/firebase.js'

  const googleProvider = new GoogleAuthProvider()

  let { user, authReady } = $props()
  let loading = $state(false)
  let error = $state('')

  async function loginWithGoogle() {
    loading = true
    error = ''

    try {
      await signInWithPopup(auth, googleProvider)
    } catch (err) {
      if (err.code !== 'auth/popup-closed-by-user') {
        error = 'Não foi possível entrar com o Google.'
      }
    } finally {
      loading = false
    }
  }

  async function logout() {
    loading = true
    error = ''

    try {
      await signOut(auth)
    } catch {
      error = 'Não foi possível sair.'
    } finally {
      loading = false
    }
  }
</script>

<div class="auth-controls">
  {#if !authReady}
    <span class="auth-loading">Verificando sessão...</span>
  {:else if user}
    <div class="user-row">
      <div class="user-info">
        {#if user.photoURL}
          <img src={user.photoURL} alt="" referrerpolicy="no-referrer" />
        {/if}
        <div class="user-text">
          <strong>{user.displayName ?? 'Usuário'}</strong>
          <span>{user.email}</span>
        </div>
      </div>
      <button class="logout-button" type="button" aria-label="Sair" title="Sair" onclick={logout} disabled={loading}>
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M10 5H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h4M14 8l4 4-4 4M9 12h9" />
        </svg>
      </button>
    </div>
  {:else}
    <button type="button" class="google-button" onclick={loginWithGoogle} disabled={loading}>
      <span class="google-mark" aria-hidden="true">G</span>
      {loading ? 'Entrando...' : 'Entrar com Google'}
    </button>
  {/if}

  {#if error}
    <span class="auth-error" role="alert">{error}</span>
  {/if}
</div>

<style>
  .auth-controls,
  .user-info {
    display: flex;
    gap: 0.75rem;
  }

  .auth-controls {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
  }

  .user-info {
    min-width: 0;
    align-items: center;
  }

  .user-row {
    display: flex;
    align-items: center;
    gap: 0.6rem;
  }

  .user-row .user-info {
    flex: 1;
  }

  .user-info img {
    width: 32px;
    height: 32px;
    border-radius: 50%;
  }

  .user-text {
    display: flex;
    flex-direction: column;
    max-width: 180px;
  }

  .user-text strong,
  .user-text span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .user-text strong {
    font-size: 0.8125rem;
  }

  .user-text span {
    color: var(--text-muted);
    font-size: 0.75rem;
  }

  button {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    border: 1px solid var(--panel-border);
    border-radius: var(--radius-sm);
    background: var(--surface-1);
    color: var(--text-main);
    padding: 0.5rem 0.75rem;
    font: inherit;
    font-size: 0.8125rem;
    cursor: pointer;
  }

  .google-button { width: 100%; }

  .logout-button {
    width: 34px;
    height: 34px;
    flex: 0 0 34px;
    padding: 0;
    color: var(--text-muted);
    background: transparent;
  }

  .logout-button:hover {
    color: var(--primary);
    border-color: rgba(16, 185, 129, 0.55);
    background: var(--primary-glow);
  }

  .logout-button svg {
    width: 17px;
    height: 17px;
    fill: none;
    stroke: currentColor;
    stroke-width: 1.8;
    stroke-linecap: round;
    stroke-linejoin: round;
  }

  button:disabled {
    cursor: wait;
    opacity: 0.6;
  }

  .google-mark {
    color: #4285f4;
    font-weight: 700;
  }

  .auth-error {
    color: var(--danger);
    font-size: 0.75rem;
  }

  .auth-loading {
    color: var(--text-muted);
    font-size: 0.75rem;
  }

</style>

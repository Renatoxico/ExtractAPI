<script>
  import { onMount } from 'svelte'
  import { GoogleAuthProvider, onAuthStateChanged, signInWithPopup, signOut } from 'firebase/auth'
  import { auth } from '../lib/firebase.js'
  import { probeAuthenticatedRequest } from '../lib/api.js'

  const googleProvider = new GoogleAuthProvider()

  let user = $state(null)
  let authReady = $state(false)
  let loading = $state(false)
  let apiLoading = $state(false)
  let apiResult = $state('')
  let error = $state('')

  onMount(() => {
    const unsubscribe = onAuthStateChanged(auth, (firebaseUser) => {
      user = firebaseUser
      authReady = true
      apiResult = ''
    })

    return unsubscribe
  })

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

  async function testApiRequest() {
    apiLoading = true
    apiResult = ''
    error = ''

    try {
      const status = await probeAuthenticatedRequest()
      apiResult = `API respondeu HTTP ${status}`
    } catch (err) {
      error = err.message
    } finally {
      apiLoading = false
    }
  }
</script>

<div class="auth-controls">
  {#if !authReady}
    <span class="auth-loading">Verificando sessão...</span>
  {:else if user}
    <div class="user-info">
      {#if user.photoURL}
        <img src={user.photoURL} alt="" referrerpolicy="no-referrer" />
      {/if}
      <div class="user-text">
        <strong>{user.displayName ?? 'Usuário'}</strong>
        <span>{user.email}</span>
      </div>
    </div>
    <button type="button" onclick={testApiRequest} disabled={apiLoading}>
      {apiLoading ? 'Testando...' : 'Testar API'}
    </button>
    <button type="button" onclick={logout} disabled={loading}>
      {loading ? 'Saindo...' : 'Sair'}
    </button>
  {:else}
    <button type="button" class="google-button" onclick={loginWithGoogle} disabled={loading}>
      <span class="google-mark" aria-hidden="true">G</span>
      {loading ? 'Entrando...' : 'Entrar com Google'}
    </button>
  {/if}

  {#if apiResult}
    <span class="api-result">{apiResult}</span>
  {/if}

  {#if error}
    <span class="auth-error" role="alert">{error}</span>
  {/if}
</div>

<style>
  .auth-controls,
  .user-info {
    display: flex;
    align-items: center;
    gap: 0.75rem;
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

  .auth-loading,
  .api-result {
    color: var(--text-muted);
    font-size: 0.75rem;
  }

  @media (max-width: 600px) {
    .user-text {
      display: none;
    }
  }
</style>

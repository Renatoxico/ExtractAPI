export function resolveView({ authReady, user, path }) {
  if (!authReady) return 'pending'
  if (user || path === '/entrar') return 'app'
  return 'landing'
}

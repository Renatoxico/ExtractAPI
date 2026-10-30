// Only a confirmed 204 means the account is gone. Any other status, including
// other 2xx responses, is a failure and must keep the signed-in screen.
export function describeAccountDeletionFailure(status, body) {
  if (status === 204) return null

  const errorCode = body?.errorCode || `HTTP_${status}`
  const suffix = body?.errorCode ? ` (${body.errorCode})` : ` (HTTP ${status})`
  const retryHint = errorCode === 'ACCOUNT_DELETION_FAILED' || errorCode === 'AUTH_TOKEN_INVALID'
    ? ' Se não conseguir repetir, saia e entre de novo.'
    : ''
  return {
    errorCode,
    message: `Não foi possível excluir a conta${suffix}. Sua conta continua ativa.${retryHint}`
  }
}

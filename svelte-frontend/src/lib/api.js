import { auth } from './firebase.js'
import { describeAccountDeletionFailure } from './accountDeletion.js'

export const host = import.meta.env.VITE_API_URL

async function buildAuthHeaders() {
  const user = auth.currentUser

  if (!user) return {}

  const idToken = await user.getIdToken()
  return { Authorization: `Bearer ${idToken}` }
}

export async function fetchSummary(reportId) {
  const headers = await buildAuthHeaders()
  let res
  try {
    res = await fetch(`${host}/v2/extract/summary/${encodeURIComponent(reportId)}`, { headers })
  } catch {
    const err = new Error('Não foi possível conectar ao servidor. Verifique se a API está rodando.')
    err.errorCode = 'NETWORK_ERROR'
    err.details = null
    throw err
  }

  const json = await res.json()

  if (!res.ok) {
    const err = new Error(json.message ?? 'Erro desconhecido')
    err.errorCode = json.errorCode ?? `HTTP_${res.status}`
    err.details = json.details ?? null
    throw err
  }

  return json
}

export async function fetchReportHistory() {
  const headers = await buildAuthHeaders()
  let res
  try {
    res = await fetch(`${host}/v2/extract/reports`, { headers })
  } catch {
    const err = new Error('Não foi possível carregar o histórico de relatórios.')
    err.errorCode = 'NETWORK_ERROR'
    err.details = null
    throw err
  }

  const json = await res.json()

  if (!res.ok) {
    const err = new Error(json.message ?? 'Erro ao carregar o histórico')
    err.errorCode = json.errorCode ?? `HTTP_${res.status}`
    err.details = json.details ?? null
    throw err
  }

  return json
}

export async function processFiles(fileArray) {
  const form = new FormData()
  for (const f of fileArray) form.append('file', f)

  const headers = await buildAuthHeaders()
  let res
  try {
    res = await fetch(`${host}/v2/extract`, {
      method: 'POST',
      headers,
      body: form
    })
  } catch {
    const err = new Error('Não foi possível conectar ao servidor. Verifique se a API está rodando.')
    err.errorCode = 'NETWORK_ERROR'
    err.details = null
    throw err
  }

  const json = await res.json()

  if (!res.ok) {
    const err = new Error(json.message ?? 'Erro desconhecido')
    err.errorCode = json.errorCode ?? `HTTP_${res.status}`
    err.details = json.details ?? null
    throw err
  }

  return json
}

export async function deleteAccount() {
  const headers = await buildAuthHeaders()
  if (!headers.Authorization) {
    throw accountDeletionError(
      'AUTH_TOKEN_MISSING',
      'É preciso estar autenticado para excluir a conta. Sua conta continua ativa.'
    )
  }

  let response
  try {
    response = await fetch(`${host}/v2/account`, {
      method: 'DELETE',
      headers,
      redirect: 'error',
      cache: 'no-store'
    })
  } catch {
    throw accountDeletionError(
      'NETWORK_ERROR',
      'Não foi possível conectar ao servidor. Sua conta não foi excluída e continua ativa.'
    )
  }

  if (response.status === 204) return

  let body = null
  try {
    body = await response.json()
  } catch {
    body = null
  }
  const failure = describeAccountDeletionFailure(response.status, body)
  throw accountDeletionError(failure.errorCode, failure.message)
}

function accountDeletionError(errorCode, message) {
  const error = new Error(message)
  error.errorCode = errorCode
  return error
}

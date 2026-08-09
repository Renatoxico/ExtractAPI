import { auth } from './firebase.js'

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

export async function fetchAuthenticatedUser() {
  const headers = await buildAuthHeaders()

  if (!headers.Authorization) {
    const err = new Error('Entre com o Google antes de testar a API.')
    err.errorCode = 'AUTH_REQUIRED'
    throw err
  }

  const res = await fetch(`${host}/api/auth/me`, { headers })

  const json = await res.json()

  if (!res.ok) {
    const err = new Error(json.message ?? `A API respondeu com HTTP ${res.status}.`)
    err.errorCode = json.errorCode ?? `HTTP_${res.status}`
    throw err
  }

  return json
}

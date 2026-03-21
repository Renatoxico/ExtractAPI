import { supabase } from './supabase.js'

export const host = import.meta.env.VITE_API_URL

async function authHeaders() {
  const { data: { session } } = await supabase.auth.getSession()
  return session ? { Authorization: `Bearer ${session.access_token}` } : {}
}

export async function fetchSummary(sessionId) {
  let res
  try {
    res = await fetch(`${host}/extract/summary/${encodeURIComponent(sessionId)}`, {
      headers: await authHeaders()
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

export async function processFiles(fileArray) {
  const form = new FormData()
  for (const f of fileArray) form.append('file', f)

  let res
  try {
    res = await fetch(`${host}/extract/`, {
      method: 'POST',
      body: form,
      headers: await authHeaders()
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

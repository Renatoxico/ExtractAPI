import { supabase } from './supabase.js'
import { host } from './api.js'

// Reactive auth state using Svelte 5 runes.
// Import these from components: import { session, isPremium, ... } from '$lib/auth.js'

export let session = $state(null)
export let isPremium = $state(null) // null = unchecked, false = free, true = premium

export async function init() {
  const { data } = await supabase.auth.getSession()
  session = data.session

  supabase.auth.onAuthStateChange((_event, newSession) => {
    session = newSession
    if (!newSession) {
      isPremium = null
    }
  })

  if (session) {
    await checkPremiumStatus()
  }
}

export async function checkPremiumStatus() {
  if (!session) return
  try {
    const res = await fetch(`${host}/api/user/status`, {
      headers: { Authorization: `Bearer ${session.access_token}` }
    })
    if (res.ok) {
      const data = await res.json()
      isPremium = data.isPremium
    } else {
      isPremium = false
    }
  } catch {
    isPremium = false
  }
}

export async function signInWithGoogle() {
  await supabase.auth.signInWithOAuth({
    provider: 'google',
    options: { redirectTo: window.location.origin }
  })
}

export async function signOut() {
  await supabase.auth.signOut()
  session = null
  isPremium = null
}

import { supabase } from './supabase.js'
import { host } from './api.js'

class Auth {
  session = $state(null)
  isPremium = $state(null) // null = unchecked, false = free, true = premium

  async init() {
    const { data } = await supabase.auth.getSession()
    this.session = data.session

    supabase.auth.onAuthStateChange((_event, newSession) => {
      this.session = newSession
      if (!newSession) {
        this.isPremium = null
      }
    })

    if (this.session) {
      await this.checkPremiumStatus()
    }
  }

  async checkPremiumStatus() {
    if (!this.session) return
    try {
      const res = await fetch(`${host}/api/user/status`, {
        headers: { Authorization: `Bearer ${this.session.access_token}` }
      })
      if (res.ok) {
        const data = await res.json()
        this.isPremium = data.isPremium
      } else {
        this.isPremium = false
      }
    } catch {
      this.isPremium = false
    }
  }

  async signInWithGoogle() {
    await supabase.auth.signInWithOAuth({
      provider: 'google',
      options: { redirectTo: window.location.origin }
    })
  }

  async signUp(email, password) {
    const { data, error } = await supabase.auth.signUp({ email, password })
    if (error) throw error
    return data
  }

  async signInWithEmail(email, password) {
    const { data, error } = await supabase.auth.signInWithPassword({ email, password })
    if (error) throw error
    return data
  }

  async resetPassword(email) {
    const { error } = await supabase.auth.resetPasswordForEmail(email, {
      redirectTo: window.location.origin
    })
    if (error) throw error
  }

  async signOut() {
    await supabase.auth.signOut()
    this.session = null
    this.isPremium = null
  }
}

export const auth = new Auth()

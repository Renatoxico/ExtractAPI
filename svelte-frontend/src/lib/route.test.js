import test from 'node:test'
import assert from 'node:assert/strict'
import { resolveView } from './route.js'

test('auth initialization always renders the neutral screen', () => {
  for (const path of ['/', '/entrar', '/unknown']) {
    for (const user of [null, { uid: 'example' }]) {
      assert.equal(resolveView({ authReady: false, user, path }), 'pending')
    }
  }
})

test('authenticated users see the app at every path', () => {
  for (const path of ['/', '/entrar', '/unknown']) {
    assert.equal(resolveView({ authReady: true, user: { uid: 'example' }, path }), 'app')
  }
})

test('signed-out users see sign-in only at /entrar', () => {
  assert.equal(resolveView({ authReady: true, user: null, path: '/entrar' }), 'app')
  for (const path of ['/', '/unknown', '/entrar/']) {
    assert.equal(resolveView({ authReady: true, user: null, path }), 'landing')
  }
})

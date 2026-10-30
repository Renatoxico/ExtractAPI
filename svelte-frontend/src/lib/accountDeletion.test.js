import test from 'node:test'
import assert from 'node:assert/strict'

import { describeAccountDeletionFailure } from './accountDeletion.js'

test('only HTTP 204 is a successful account deletion', () => {
  assert.equal(describeAccountDeletionFailure(204, null), null)
  assert.equal(describeAccountDeletionFailure(204, { errorCode: 'IGNORED' }), null)
})

test('error responses stay failures and say the account is still active', () => {
  const failure = describeAccountDeletionFailure(502, { errorCode: 'ACCOUNT_DELETION_FAILED' })

  assert.equal(failure.errorCode, 'ACCOUNT_DELETION_FAILED')
  assert.match(failure.message, /continua ativa/)
  assert.match(failure.message, /ACCOUNT_DELETION_FAILED/)
  assert.match(failure.message, /saia e entre de novo/)
})

test('a non-204 success status is not treated as deletion', () => {
  for (const status of [200, 201, 202, 401, 500]) {
    const failure = describeAccountDeletionFailure(status, null)
    assert.equal(failure.errorCode, `HTTP_${status}`)
    assert.match(failure.message, /continua ativa/)
    assert.match(failure.message, new RegExp(`HTTP ${status}`))
  }
})

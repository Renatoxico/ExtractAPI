import test from 'node:test'
import assert from 'node:assert/strict'
import { landingCopy } from './landingCopy.js'

function keyPaths(value, prefix = '') {
  return Object.entries(value).flatMap(([key, entry]) => {
    const path = prefix ? `${prefix}.${key}` : key
    return typeof entry === 'object' ? [path, ...keyPaths(entry, path)] : [path]
  }).sort()
}

function strings(value) {
  return Object.values(value).flatMap(entry => typeof entry === 'object' ? strings(entry) : [entry])
}

test('PT and EN copy have identical keys, including steps and feature blocks', () => {
  assert.deepEqual(keyPaths(landingCopy.pt), keyPaths(landingCopy.en))
  for (const copy of Object.values(landingCopy)) {
    assert.equal(copy.features.length, 4)
    assert.equal(copy.steps.length, 3)
  }
})

test('landing copy contains only supported claims', () => {
  for (const copy of strings(landingCopy)) {
    assert.equal(typeof copy, 'string')
    assert.doesNotMatch(copy, /CSV|compara[çc][aã]o|comparison|Apple|\d+\s*(?:usuários|users)/iu)
  }
})

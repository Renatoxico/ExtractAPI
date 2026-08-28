import test from 'node:test';
import assert from 'node:assert/strict';

import { isActiveReport, shortReportId } from './reportHistory.js';

test('marks only the selected history report as active', () => {
  assert.equal(isActiveReport('report-a', 'report-a'), true);
  assert.equal(isActiveReport('report-b', 'report-a'), false);
  assert.equal(isActiveReport('', ''), false);
});

test('abbreviates long report ids while retaining both ends', () => {
  assert.equal(shortReportId('abcdefghijklmnop'), 'abcdefgh…mnop');
  assert.equal(shortReportId('short-id'), 'short-id');
});

import test from 'node:test';
import assert from 'node:assert/strict';
import {
  EXPENSE_CATEGORIES, parseExpenseAmount, isExpenseDate,
  countExpensesByName, expenseEditingError
} from './expenseEditing.js';
import { CATEGORY_COLORS } from './categoryColors.js';
import { categoryIcon } from './categoryIcons.js';

test('parses Brazilian amounts without changing their scale', () => {
  for (const [input, expected] of [
    ['1.234,56', 1234.56], ['R$ 1.234,56', 1234.56],
    [' 12,5 ', 12.5], ['1234', 1234], ['0,01', 0.01], ['1.234', 1234]
  ]) assert.equal(parseExpenseAmount(input), expected, input);
});

test('rejects zero, negatives, foreign notation, malformed grouping and excess precision', () => {
  for (const input of ['', null, '0', '0,00', '-12,34', '1.23', '1,234.56',
    '1,234', '1e3', 'NaN', 'Infinity', '12 reais', '1.23.456', '9007199254740992']) {
    assert.equal(parseExpenseAmount(input), null, String(input));
  }
});

test('validates real ISO dates including leap days and positive four digit years', () => {
  for (const date of ['2024-02-29', '2026-11-04', '0001-01-01', '9999-12-31']) {
    assert.equal(isExpenseDate(date), true, date);
  }
  for (const date of ['', null, '0000-01-01', '2025-02-29', '2026-04-31',
    '2026-13-01', '2026-00-01', '2026-01-00', '04/11/2026', '2026-1-01', '10000-01-01']) {
    assert.equal(isExpenseDate(date), false, String(date));
  }
});

test('category scope counts exact names across categories, never similar names', () => {
  const expenses = [
    { expenseName: 'UBER *TRIP', category: 'Transporte / Auto' },
    { expenseName: 'UBER *TRIP', category: 'Outros / Transferências' },
    { expenseName: 'uber *trip' }, { expenseName: 'UBER *TRIP ' }
  ];
  assert.equal(countExpensesByName(expenses, 'UBER *TRIP'), 2);
  assert.equal(countExpensesByName(expenses, 'Outro'), 0);
  assert.equal(countExpensesByName([], 'UBER *TRIP'), 0);
});

test('picker reuses all existing categories, colors and icons', () => {
  assert.deepEqual(EXPENSE_CATEGORIES, Object.keys(CATEGORY_COLORS));
  assert.equal(EXPENSE_CATEGORIES.length, 10);
  for (const category of EXPENSE_CATEGORIES) assert.match(categoryIcon(category), /<svg/);
});

test('editing errors give actionable Portuguese copy without assuming a failed write rolled back', () => {
  assert.match(expenseEditingError(401), /sessão expirou/);
  assert.match(expenseEditingError(403), /permissão/);
  assert.match(expenseEditingError(404), /Reabra o relatório/);
  assert.match(expenseEditingError(400), /Confira/);
  for (const status of [undefined, 500, 502]) {
    assert.match(expenseEditingError(status), /Seus dados continuam na tela/);
    assert.match(expenseEditingError(status), /reabra o relatório/);
  }
});

import test from 'node:test';
import assert from 'node:assert/strict';

import { buildExpenseGroups, filterExpenseGroups } from './expenseGroups.js';

const groups = [
  { expenseName: 'CAFÉ CENTRAL', category: 'Restaurante / Lanches', totalAmount: 30, occurrenceCount: 2 },
  { expenseName: 'PIX JOAO', category: null, totalAmount: 50, occurrenceCount: 1 }
];

const expenses = [
  { expenseId: 1, expenseName: 'CAFÉ CENTRAL', category: 'Restaurante / Lanches', amount: 20, date: '2026-08-20' },
  { expenseId: 2, expenseName: 'CAFÉ CENTRAL', category: 'Restaurante / Lanches', amount: 10, date: '2026-08-03' },
  { expenseId: 3, expenseName: 'PIX JOAO', category: null, amount: 50, date: '2026-08-10' },
  { expenseId: 4, expenseName: 'PIX JOAO', category: 'Outros / Transferências', amount: 5, date: '2026-08-01' }
];

test('associates instances using the backend group name and category', () => {
  const result = buildExpenseGroups(groups, expenses);

  assert.deepEqual(result[0].instances.map((expense) => expense.expenseId), [2, 1]);
  assert.deepEqual(result[1].instances.map((expense) => expense.expenseId), [3]);
});

test('orders expanded instances from oldest to newest regardless of amount', () => {
  const result = buildExpenseGroups(groups, expenses);

  assert.deepEqual(result[0].instances.map((expense) => expense.date), ['2026-08-03', '2026-08-20']);
  assert.deepEqual(result[0].instances.map((expense) => expense.amount), [10, 20]);
});

test('searches names and displayed categories without case or accent sensitivity', () => {
  const result = buildExpenseGroups(groups, expenses);

  assert.deepEqual(filterExpenseGroups(result, 'cafe').map((group) => group.expenseName), ['CAFÉ CENTRAL']);
  assert.deepEqual(filterExpenseGroups(result, 'transferencias').map((group) => group.expenseName), ['PIX JOAO']);
});

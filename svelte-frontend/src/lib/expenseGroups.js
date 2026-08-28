import { formatCategory } from './formatters.js';

export function expenseGroupKey(expenseName, category) {
  return JSON.stringify([expenseName ?? '', category ?? null]);
}

export function buildExpenseGroups(groups = [], expenses = []) {
  const instancesByGroup = new Map();

  for (const expense of expenses) {
    const key = expenseGroupKey(expense.expenseName, expense.category);
    const instances = instancesByGroup.get(key) ?? [];
    instances.push(expense);
    instancesByGroup.set(key, instances);
  }

  return groups.map((group) => ({
    ...group,
    key: expenseGroupKey(group.expenseName, group.category),
    instances: [...(instancesByGroup.get(expenseGroupKey(group.expenseName, group.category)) ?? [])]
      .sort(compareExpenseDates)
  }));
}

function compareExpenseDates(a, b) {
  const dateComparison = sortableDate(a.date).localeCompare(sortableDate(b.date));
  if (dateComparison !== 0) return dateComparison;
  return Number(a.expenseId ?? 0) - Number(b.expenseId ?? 0);
}

function sortableDate(date) {
  return /^\d{4}-\d{2}-\d{2}$/.test(date ?? '') ? date : '9999-12-31';
}

export function normalizeSearch(value) {
  return String(value ?? '')
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLocaleLowerCase('pt-BR')
    .trim();
}

export function filterExpenseGroups(groups, query) {
  const normalizedQuery = normalizeSearch(query);
  if (!normalizedQuery) return groups;

  return groups.filter((group) =>
    normalizeSearch(group.expenseName).includes(normalizedQuery) ||
    normalizeSearch(formatCategory(group.category)).includes(normalizedQuery)
  );
}

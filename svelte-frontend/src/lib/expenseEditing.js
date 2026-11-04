import { CATEGORY_COLORS } from './categoryColors.js';

export const EXPENSE_CATEGORIES = Object.keys(CATEGORY_COLORS);

// Accept Brazilian decimal notation, including optional thousands separators.
export function parseExpenseAmount(value) {
  const text = String(value ?? '').trim().replace(/^R\$\s*/, '');
  if (!/^(?:\d+|\d{1,3}(?:\.\d{3})+)(?:,\d{1,2})?$/.test(text)) return null;
  const amount = Number(text.replaceAll('.', '').replace(',', '.'));
  const cents = Math.round(amount * 100);
  return Number.isSafeInteger(cents) && cents > 0 ? cents / 100 : null;
}

export function isExpenseDate(value) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value ?? '') || value.startsWith('0000')) return false;
  const date = new Date(`${value}T00:00:00Z`);
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value;
}

export function countExpensesByName(expenses, name) {
  return expenses.filter(expense => expense.expenseName === name).length;
}

export function expenseEditingError(status) {
  if (status === 401) return 'Sua sessão expirou. Entre novamente para editar o relatório.';
  if (status === 403) return 'Você não tem permissão para editar este relatório.';
  if (status === 404) return 'Relatório ou despesa não encontrado. Reabra o relatório e tente novamente.';
  if (status === 400) return 'Confira a descrição, o valor, a data e a categoria e tente novamente.';
  return 'Não foi possível confirmar a alteração. Seus dados continuam na tela. Verifique sua conexão e reabra o relatório antes de tentar novamente.';
}

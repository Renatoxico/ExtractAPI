export const formatBRL = (value) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);

export const formatDate = (ddMMYYYY) => ddMMYYYY ?? '—';

export const formatCount = (n) => `${n} ${n === 1 ? 'transação' : 'transações'}`;

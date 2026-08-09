export const formatBRL = (value) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);

export const formatDate = (isoDate) => {
  if (!isoDate) return '—';
  const [year, month, day] = isoDate.split('-').map(Number);
  if (!year || !month || !day) return isoDate;
  return new Intl.DateTimeFormat('pt-BR').format(new Date(year, month - 1, day));
};

export const formatCategory = (category) => category || 'Outros / Transferências';

export const formatCount = (n) => `${n} ${n === 1 ? 'transação' : 'transações'}`;

export const formatBRL = (value) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);

export const FALLBACK_CATEGORY = 'Outros / Transferências';

export const formatDate = (isoDate) => {
  if (!isoDate) return '—';
  const [year, month, day] = isoDate.split('-').map(Number);
  if (!year || !month || !day) return isoDate;
  return new Intl.DateTimeFormat('pt-BR').format(new Date(year, month - 1, day));
};

export const formatCreatedAt = (instant) => {
  if (!instant) return '—';
  const date = new Date(instant);
  if (Number.isNaN(date.getTime())) return instant;
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric'
  }).format(date);
};

export const formatCategory = (category) => category || FALLBACK_CATEGORY;

export const formatCount = (n) => `${n} ${n === 1 ? 'transação' : 'transações'}`;

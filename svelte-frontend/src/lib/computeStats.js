export function computeMonthWithMostTransactions(allExpenses) {
  if (!allExpenses?.length) return null;

  const counts = {};
  for (const e of allExpenses) {
    const parts = e.date?.split('-');
    if (!parts || parts.length < 3) continue;
    const [yyyy, mm] = parts;
    const key = `${mm}/${yyyy}`;
    counts[key] = (counts[key] ?? 0) + 1;
  }

  const entries = Object.entries(counts);
  if (!entries.length) return null;

  const [topKey, count] = entries.sort((a, b) => b[1] - a[1])[0];
  const [mm, yyyy] = topKey.split('/');

  const label = new Date(Number(yyyy), Number(mm) - 1, 1)
    .toLocaleString('pt-BR', { month: 'long', year: 'numeric' });

  return { label, count, key: topKey };
}

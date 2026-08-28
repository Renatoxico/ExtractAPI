export function shortReportId(reportId) {
  if (!reportId || reportId.length <= 12) return reportId ?? '';
  return `${reportId.slice(0, 8)}…${reportId.slice(-4)}`;
}

export function isActiveReport(reportId, activeReportId) {
  return Boolean(reportId && reportId === activeReportId);
}

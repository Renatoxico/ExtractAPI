export async function fetchSummary(sessionId) {
  let res;
  const API = import.meta.env.VITE_API_URL;
  try {
    res = await fetch( API + `/extract/summary/${encodeURIComponent(sessionId)}`);
  } catch {
    const err = new Error('Não foi possível conectar ao servidor. Verifique se a API está rodando.');
    err.errorCode = 'NETWORK_ERROR';
    err.details = null;
    throw err;
  }

  const json = await res.json();

  if (!res.ok) {
    const err = new Error(json.message ?? 'Erro desconhecido');
    err.errorCode = json.errorCode ?? `HTTP_${res.status}`;
    err.details = json.details ?? null;
    throw err;
  }

  return json;
}

export async function processFiles(fileArray) {
  const form = new FormData();
  for (const f of fileArray) form.append('file', f);

  let res;
  try {
    res = await fetch( API + '/extract/', { method: 'POST', body: form });
  } catch {
    const err = new Error('Não foi possível conectar ao servidor. Verifique se a API está rodando.');
    err.errorCode = 'NETWORK_ERROR';
    err.details = null;
    throw err;
  }
  console.log(res)
  const json = await res.json();

  if (!res.ok) {
    const err = new Error(json.message ?? 'Erro desconhecido');
    err.errorCode = json.errorCode ?? `HTTP_${res.status}`;
    err.details = json.details ?? null;
    throw err;
  }

  return json;
}

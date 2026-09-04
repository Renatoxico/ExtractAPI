<script>
  let { fileCount = 0, onFilesChange } = $props();

  let dragging = $state(false);
  let inputEl;

  function handleDrop(e) {
    e.preventDefault();
    dragging = false;
    const dropped = Array.from(e.dataTransfer.files).filter(f => f.type === 'application/pdf');
    if (dropped.length) onFilesChange(dropped);
  }

  function handleChange(e) {
    const selected = Array.from(e.target.files);
    if (selected.length) onFilesChange(selected);
    e.target.value = '';
  }
</script>

<div
  class="upload-zone"
  class:dragging
  class:compact={fileCount > 0}
  style={`--dropzone-shrink: ${Math.min(fileCount, 6) * 29}px`}
  role="button"
  tabindex="0"
  aria-label="Clique ou arraste PDFs aqui"
  ondragover={(e) => { e.preventDefault(); dragging = true; }}
  ondragleave={() => (dragging = false)}
  ondrop={handleDrop}
  onclick={() => inputEl.click()}
  onkeydown={(e) => e.key === 'Enter' && inputEl.click()}
>
  <input
    bind:this={inputEl}
    type="file"
    accept=".pdf,application/pdf"
    multiple
    style="display:none"
    onchange={handleChange}
  />
  <div class="upload-icon">
    <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
      <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/>
      <polyline points="17 8 12 3 7 8"/>
      <line x1="12" y1="3" x2="12" y2="15"/>
    </svg>
  </div>
  <p class="upload-title">Arraste seus PDFs aqui</p>
  <p class="upload-sub">ou clique para selecionar · máx. 6 arquivos · 512 KB cada</p>
</div>

<style>
  .upload-zone {
    min-height: clamp(135px, calc(31.5vh - var(--dropzone-shrink)), 300px);
    border: 2px dashed var(--panel-border);
    border-radius: var(--radius-lg);
    padding: 3rem 2rem;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
    cursor: pointer;
    transition: min-height 180ms ease, padding 180ms ease, border-color var(--transition), background var(--transition);
    background: var(--panel-bg);
    color: var(--text-muted);
    user-select: none;
  }

  .upload-zone.compact {
    padding-block: 1rem;
  }

  .upload-zone:hover,
  .upload-zone.dragging {
    border-color: var(--primary);
    background: var(--primary-glow);
    color: var(--primary);
  }

  .upload-icon {
    margin-bottom: 1rem;
    opacity: 0.6;
  }

  .upload-zone.compact .upload-icon {
    margin-bottom: 0.5rem;
  }

  .upload-zone:hover .upload-icon,
  .upload-zone.dragging .upload-icon {
    opacity: 1;
  }

  .upload-title {
    font-size: 1.125rem;
    font-weight: 600;
    color: var(--text-main);
    margin: 0 0 0.5rem;
  }

  .upload-sub {
    font-size: 0.875rem;
    margin: 0;
  }
</style>

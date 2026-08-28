<script>
  import { onMount, onDestroy } from 'svelte';
  import { Chart, ArcElement, DoughnutController, Tooltip } from 'chart.js';
  import { categoryColor } from '../lib/categoryColors.js';
  import { categoryIcon } from '../lib/categoryIcons.js';
  import { formatBRL, formatCategory } from '../lib/formatters.js';

  Chart.register(ArcElement, DoughnutController, Tooltip);

  let { data } = $props();

  let canvas;
  let chartInstance = null;

  function buildChart() {
    if (chartInstance) {
      chartInstance.destroy();
      chartInstance = null;
    }
    if (!canvas || !data?.length) return;

    chartInstance = new Chart(canvas, {
      type: 'doughnut',
      data: {
        labels: data.map(d => formatCategory(d.category)),
        datasets: [{
          data: data.map(d => Number(d.totalAmount)),
          backgroundColor: data.map(d => categoryColor(d.category)),
          borderWidth: 0,
          hoverOffset: 10
        }]
      },
      options: {
        cutout: '68%',
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (ctx) => {
                const count = data[ctx.dataIndex]?.occurrenceCount ?? 0;
                return ` ${formatBRL(ctx.parsed)} · ${count} ocorrência${count === 1 ? '' : 's'}`;
              }
            }
          }
        },
        animation: { animateRotate: true, duration: 600 }
      }
    });
  }

  onMount(() => buildChart());
  onDestroy(() => chartInstance?.destroy());

  $effect(() => {
    // Rebuild when data changes (new upload)
    data;
    buildChart();
  });

  let total = $derived(data?.reduce((sum, d) => sum + Number(d.totalAmount), 0) ?? 0);
</script>

<div class="donut-wrap">
  <div class="canvas-wrap">
    <canvas bind:this={canvas}></canvas>
    <div class="donut-center">
      <span class="donut-total-label">Total</span>
      <span class="donut-total">{formatBRL(total)}</span>
    </div>
  </div>

  <ul class="legend">
    {#each data as item}
      <li class="legend-item">
        <span class="legend-icon" style="color: {categoryColor(item.category)}">{@html categoryIcon(item.category)}</span>
        <span class="legend-name">{formatCategory(item.category)} · {item.occurrenceCount}×</span>
        <span class="legend-value">{formatBRL(Number(item.totalAmount))}</span>
      </li>
    {/each}
  </ul>
</div>

<style>
  .donut-wrap {
    display: grid;
    grid-template-columns: minmax(300px, 360px) minmax(0, 1fr);
    gap: 1.75rem;
    align-items: center;
  }

  .canvas-wrap {
    position: relative;
    width: min(360px, 100%);
    justify-self: center;
    flex-shrink: 0;
  }

  .canvas-wrap canvas {
    width: 100% !important;
    height: auto !important;
  }

  .donut-center {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    text-align: center;
    pointer-events: none;
  }

  .donut-total-label {
    display: block;
    font-size: 0.75rem;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.08em;
  }

  .donut-total {
    display: block;
    font-size: 1.05rem;
    font-weight: 700;
    color: var(--text-main);
    white-space: nowrap;
  }

  .legend {
    list-style: none;
    margin: 0;
    padding: 0;
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 0;
  }

  .legend-item {
    display: grid;
    grid-template-columns: 16px minmax(0, 1fr) auto;
    align-items: start;
    gap: 0.5rem;
    font-size: 0.8125rem;
    padding: 0.48rem 0;
    border-bottom: 1px solid rgba(255,255,255,0.05);
  }

  .legend-item:last-child { border-bottom: 0; }

  .legend-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 16px;
    height: 16px;
    flex-shrink: 0;
  }

  .legend-name {
    color: var(--text-muted);
    line-height: 1.35;
    white-space: normal;
    overflow-wrap: anywhere;
  }

  .legend-value {
    color: var(--text-main);
    font-weight: 500;
    white-space: nowrap;
    text-align: right;
  }

  @media (max-width: 720px) {
    .donut-wrap {
      grid-template-columns: 1fr;
      gap: 1.25rem;
    }

    .canvas-wrap {
      width: min(340px, 100%);
    }
  }
</style>

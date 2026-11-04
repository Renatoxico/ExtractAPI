<script>
  import { landingCopy } from '../lib/landingCopy.js'
  import { CONTACT_EMAIL, SHOW_AUTHOR_CREDIT } from '../lib/landingConfig.js'

  let { onNavigate } = $props()

  function initialLanguage() {
    const queryLanguage = new URLSearchParams(window.location.search).get('lang')
    if (queryLanguage === 'en' || queryLanguage === 'pt') return queryLanguage
    try {
      return localStorage.getItem('somai.lang') === 'en' ? 'en' : 'pt'
    } catch {
      return 'pt'
    }
  }

  let language = $state(initialLanguage())
  let copy = $derived(landingCopy[language])
  let categories = $derived([
    { label: copy.food, amount: 420 },
    { label: copy.transport, amount: 180 },
    { label: copy.groceries, amount: 480 },
    { label: copy.subscriptions, amount: 120 },
  ])

  $effect(() => {
    document.documentElement.lang = language === 'en' ? 'en' : 'pt-BR'
    try {
      localStorage.setItem('somai.lang', language)
    } catch {
      // Language switching also works when browser storage is unavailable.
    }
  })

  function setLanguage(nextLanguage) {
    language = nextLanguage
    const url = new URL(window.location.href)
    url.searchParams.set('lang', language)
    window.history.replaceState(null, '', url)
  }

  function money(amount) {
    return new Intl.NumberFormat(language === 'en' ? 'en-US' : 'pt-BR', {
      style: 'currency', currency: 'BRL',
    }).format(amount)
  }

  function showExample(event) {
    if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return
    event.preventDefault()
    document.getElementById('exemplo')?.scrollIntoView({
      behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth',
      block: 'center',
    })
    document.getElementById('exemplo')?.focus({ preventScroll: true })
  }
</script>

<div class="landing">
  <header class="site-header container">
    <a class="brand" href="/" aria-label="Somaí"><img src="/somai-logo.png" alt="Somaí" /></a>
    <nav class="header-actions" aria-label={copy.signIn}>
      <div class="languages">
        <button type="button" lang="pt-BR" aria-pressed={language === 'pt'} onclick={() => setLanguage('pt')}>PT</button>
        <span aria-hidden="true">|</span>
        <button type="button" lang="en" aria-pressed={language === 'en'} onclick={() => setLanguage('en')}>EN</button>
      </div>
      <a class="button outline" href="/entrar" onclick={onNavigate}>{copy.signIn}</a>
    </nav>
  </header>

  <main class="container">
    <section class="hero" aria-labelledby="hero-title">
      <div class="hero-copy">
        <h1 id="hero-title">{copy.title}</h1>
        <p class="subtitle">{copy.subtitle}</p>
        <div class="hero-actions">
          <a class="button primary" href="/entrar" onclick={onNavigate}>{copy.start}<span aria-hidden="true">↗</span></a>
          <a class="example-link" href="#exemplo" onclick={showExample}>{copy.example}<span aria-hidden="true">↓</span></a>
        </div>
        <p class="trust"><span aria-hidden="true">✓</span>{copy.trust}</p>
      </div>

      <div class="phone-stage">
        <section class="phone" id="exemplo" tabindex="-1" aria-labelledby="example-title">
          <div class="phone-speaker" aria-hidden="true"></div>
          <div class="report-top"><span>Somaí</span><span class="example-badge">{copy.exampleBadge}</span></div>
          <h2 id="example-title">{copy.report}</h2>
          <p class="total-label">{copy.total}</p>
          <p class="total">{money(1200)}</p>
          <div class="chart">
            <svg viewBox="0 0 120 120" aria-hidden="true">
              <circle class="ring-base" cx="60" cy="60" r="46" />
              <g transform="rotate(-90 60 60)" fill="none" stroke-width="13">
                <circle class="segment segment-0" cx="60" cy="60" r="46" pathLength="100" stroke-dasharray="33 67" />
                <circle class="segment segment-1" cx="60" cy="60" r="46" pathLength="100" stroke-dasharray="13 87" stroke-dashoffset="-35" />
                <circle class="segment segment-2" cx="60" cy="60" r="46" pathLength="100" stroke-dasharray="38 62" stroke-dashoffset="-50" />
                <circle class="segment segment-3" cx="60" cy="60" r="46" pathLength="100" stroke-dasharray="8 92" stroke-dashoffset="-90" />
              </g>
            </svg>
            <dl class="legend">
              {#each categories as category, index}
                <div><dt><span class="dot segment-{index}"></span>{category.label}</dt><dd>{money(category.amount)}</dd></div>
              {/each}
            </dl>
          </div>
          <div class="largest">
            <span class="card-icon" aria-hidden="true">↗</span>
            <div><h3>{copy.largest}</h3><p>{copy.exampleMerchant}</p></div>
            <strong>{money(240)}</strong>
          </div>
          <div class="grouped"><span>{copy.grouped}</span><strong>{money(180)}</strong></div>
          <div class="phone-home" aria-hidden="true"></div>
        </section>
      </div>
    </section>

    <section class="steps" aria-label={copy.steps[2]}>
      <ol>
        {#each copy.steps as step, index}
          <li><span class="step-number" aria-hidden="true">0{index + 1}</span><h2>{step}</h2></li>
        {/each}
      </ol>
    </section>

    <section class="features" aria-label={copy.report}>
      {#each copy.features as feature, index}
        <article>
          <span class="feature-icon" aria-hidden="true">{['≋', '◔', '↗', '↶'][index]}</span>
          <h2>{feature.title}</h2>
          <p>{feature.description}</p>
        </article>
      {/each}
    </section>

    <section class="privacy" aria-labelledby="privacy-title">
      <div class="privacy-mark" aria-hidden="true">
        <svg viewBox="0 0 32 32" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M16 3 27 7v8c0 7-11 14-11 14S5 22 5 15V7Z"/><path d="m11 15 4 4 7-8"/></svg>
      </div>
      <div><h2 id="privacy-title">{copy.providers}</h2><p>{copy.deletion}</p><p>{copy.bank}</p></div>
      <nav class="privacy-links" aria-label={copy.privacy}><a href="/privacy">{copy.privacy}</a><a href="/terms">{copy.terms}</a></nav>
    </section>

    <section class="final-cta" aria-labelledby="final-title">
      <h2 id="final-title">{copy.finalTitle}</h2>
      <a class="button primary" href="/entrar" onclick={onNavigate}>{copy.start}<span aria-hidden="true">↗</span></a>
    </section>
  </main>

  <footer class="site-footer container">
    <span class="copyright">© Somaí</span>
    <nav aria-label={copy.privacy}>
      <a href="/privacy">{copy.privacy}</a>
      <a href="/terms">{copy.terms}</a>
      {#if CONTACT_EMAIL}<a href={`mailto:${CONTACT_EMAIL}`}>{copy.contact}</a>{/if}
    </nav>
    {#if SHOW_AUTHOR_CREDIT}<a class="author" href="https://github.com/Renatoxico" target="_blank" rel="noopener">{copy.credit}</a>{/if}
  </footer>
</div>

<style>
  .landing { background: radial-gradient(ellipse 650px 560px at 75% 19%, rgba(16,185,129,.09), transparent 80%), #0b0f0e; min-height: 100vh; line-height: 1.6; }
  .container { width: min(1120px, calc(100% - 40px)); margin-inline: auto; }
  a { color: inherit; text-decoration: none; }
  a, button { -webkit-tap-highlight-color: transparent; }
  a:focus-visible, button:focus-visible, .phone:focus-visible { outline: 2px solid var(--primary); outline-offset: 5px; border-radius: var(--radius-sm); }
  a:hover { color: var(--primary); }
  .site-header { min-height: 100px; display: flex; align-items: center; justify-content: space-between; gap: 16px; }
  .brand { display: flex; width: 136px; }
  .brand img { width: 100%; height: auto; }
  .header-actions, .languages { display: flex; align-items: center; gap: 16px; }
  .languages { gap: 6px; font-size: .75rem; color: var(--text-muted); }
  .languages button { padding: 8px 3px; background: transparent; border: 0; color: var(--text-muted); cursor: pointer; font-size: inherit; }
  .languages button[aria-pressed='true'] { color: var(--text-main); font-weight: 700; text-decoration: underline; text-underline-offset: 5px; text-decoration-color: var(--primary); }
  .button { display: inline-flex; align-items: center; justify-content: center; gap: 24px; min-height: 48px; padding: 12px 22px; border-radius: var(--radius-sm); font-size: .875rem; font-weight: 600; transition: background 150ms ease; }
  .outline { min-height: 42px; padding: 8px 18px; border: 1px solid #36443e; }
  .primary { background: var(--primary); color: #071b13; }
  .primary:hover { background: #34d399; color: #071b13; }
  .hero { display: grid; gap: 48px; padding: 52px 0 64px; align-items: center; }
  h1 { margin: 0; max-width: 650px; font-size: clamp(2.6rem, 4.7vw, 4.1rem); line-height: 1.08; letter-spacing: -.055em; font-weight: 700; text-wrap: balance; }
  .subtitle { max-width: 510px; margin: 24px 0 28px; font-size: 1.02rem; color: var(--text-muted); line-height: 1.8; }
  .hero-actions { display: flex; align-items: center; flex-wrap: wrap; gap: 24px; }
  .example-link { display: inline-flex; align-items: center; gap: 12px; padding: 12px 0; font-size: .875rem; }
  .trust { max-width: 400px; display: flex; align-items: flex-start; gap: 10px; color: var(--text-muted); font-size: .75rem; margin: 25px 0 0; }
  .trust > span { color: var(--primary); }
  .phone-stage { display: flex; justify-content: center; min-width: 0; }
  .phone { width: 340px; max-width: 100%; padding: 14px 20px 12px; border: 5px solid #28332f; border-radius: 40px; background: linear-gradient(150deg, #19231e, #101714 55%); box-shadow: 0 30px 80px #0006, 0 0 60px #10b98108; font-variant-numeric: tabular-nums; scroll-margin-block: 30px; }
  .phone-speaker { width: 64px; height: 5px; margin: 0 auto 25px; background: #070d0a; border-radius: 10px; }
  .report-top { display: flex; align-items: center; justify-content: space-between; gap: 5px; }
  .report-top > span:first-child { font-weight: 700; font-size: 1rem; letter-spacing: -.05em; }
  .example-badge { color: #a4e7cb; background: #10b98112; border: 1px solid #10b98130; border-radius: 20px; padding: 3px 8px; font-size: .6rem; }
  .phone h2 { margin: 24px 0 18px; font-size: .85rem; font-weight: 500; }
  .total-label { margin: 0; color: var(--text-muted); font-size: .7rem; }
  .total { margin: 0; font-size: 2.25rem; line-height: 1.5; font-weight: 600; letter-spacing: -.055em; }
  .chart { margin: 18px 0 24px; }
  .chart svg { display: block; width: 142px; margin: 0 auto 22px; }
  .ring-base { fill: none; stroke: #1b2923; stroke-width: 13; }
  .segment-0 { stroke: #10b981; background: #10b981; }
  .segment-1 { stroke: #a7f3d0; background: #a7f3d0; }
  .segment-2 { stroke: #43896c; background: #43896c; }
  .segment-3 { stroke: #d1e4da; background: #d1e4da; }
  .legend { display: grid; gap: 9px; margin: 0; font-size: .68rem; }
  .legend > div { display: flex; align-items: center; justify-content: space-between; gap: 6px; }
  .legend dt { display: flex; align-items: center; gap: 8px; color: #b8c5bd; }
  .legend dd { margin: 0; }
  .dot { width: 6px; height: 6px; border-radius: 50%; }
  .largest { display: flex; align-items: center; gap: 10px; padding: 13px 10px; background: #10b98109; border: 1px solid #10b98124; border-radius: 12px; }
  .card-icon { color: var(--primary); font-size: 1.25rem; }
  .largest h3 { margin: 0; color: #b8c5bd; font-size: .62rem; font-weight: 400; }
  .largest p { margin: 3px 0 0; font-size: .62rem; }
  .largest strong { margin-left: auto; font-size: .7rem; white-space: nowrap; }
  .grouped { display: flex; justify-content: space-between; gap: 8px; padding: 18px 0; font-size: .7rem; color: #b8c5bd; }
  .grouped strong { color: var(--text-main); }
  .phone-home { width: 90px; height: 4px; background: #45594e; margin: 7px auto 0; border-radius: 10px; }
  .steps { border-block: 1px solid #ffffff0f; padding: 32px 0; }
  .steps ol { display: grid; gap: 28px; list-style: none; padding: 0; margin: 0; }
  .steps li { display: flex; align-items: center; gap: 16px; }
  .step-number { color: var(--primary); font-size: .9rem; font-variant-numeric: tabular-nums; }
  .steps h2 { margin: 0; max-width: 225px; font-size: .9rem; font-weight: 500; line-height: 1.6; }
  .features { display: grid; gap: 38px 36px; padding: 64px 0; }
  .feature-icon { width: 42px; height: 42px; display: grid; place-items: center; color: var(--primary); background: #10b9810c; border: 1px solid #10b98120; border-radius: 12px; font-size: 1.5rem; }
  .features h2 { margin: 18px 0 9px; font-size: 1rem; font-weight: 600; letter-spacing: -.02em; }
  .features p { margin: 0; color: var(--text-muted); font-size: .875rem; line-height: 1.8; max-width: 390px; }
  .privacy { display: flex; flex-wrap: wrap; align-items: center; gap: 22px; padding: 28px; border: 1px solid #10b98129; background: #10b98106; border-radius: var(--radius-lg); }
  .privacy-mark { color: var(--primary); width: 40px; flex-shrink: 0; }
  .privacy-mark svg { display: block; width: 100%; }
  .privacy h2 { margin: 0 0 8px; font-size: 1rem; font-weight: 600; }
  .privacy p { margin: 3px 0; color: var(--text-muted); font-size: .8rem; }
  .privacy-links { display: flex; gap: 20px; font-size: .75rem; text-decoration: underline; text-underline-offset: 4px; }
  .final-cta { text-align: center; padding: 72px 0; }
  .final-cta h2 { font-size: clamp(1.8rem, 3.5vw, 2.5rem); line-height: 1.25; letter-spacing: -.045em; max-width: 560px; margin: 0 auto 28px; text-wrap: balance; }
  .site-footer { display: flex; align-items: center; flex-wrap: wrap; gap: 20px 28px; padding-block: 28px; border-top: 1px solid #ffffff0f; color: var(--text-muted); font-size: .75rem; }
  .copyright { color: var(--text-main); }
  .site-footer nav { display: flex; flex-wrap: wrap; gap: 20px; }
  .author { font-size: .68rem; }
  @media (min-width: 600px) {
    .container { width: min(1120px, calc(100% - 80px)); }
    .features { grid-template-columns: 1fr 1fr; gap: 44px 64px; }
    .steps ol { grid-template-columns: repeat(3, 1fr); }
    .steps li { align-items: flex-start; }
    .privacy-links { margin-left: auto; }
    .author { margin-left: auto; }
  }
  @media (min-width: 1000px) {
    .site-header { min-height: 110px; }
    .hero { grid-template-columns: 1.3fr 1fr; gap: 64px; padding: 62px 0 78px; }
    .phone-stage { justify-content: flex-end; padding-right: 24px; }
    .features { padding-block: 80px; }
    .privacy { padding: 32px; }
    .final-cta { padding-block: 90px; }
  }
  @media (prefers-reduced-motion: reduce) {
    .button { transition: none; }
  }
</style>

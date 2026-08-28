import { FALLBACK_CATEGORY } from './formatters.js';

// Inline SVG icons mapped to category names (matching iOS SF Symbols)
// All icons are 16×16, using currentColor for easy color inheritance.

const ICONS = {
  'Roupas / Acessórios': `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M6.2 2L1 7.2l3.7 3.7L6 9.6V21c0 .6.4 1 1 1h10c.6 0 1-.4 1-1V9.6l1.3 1.3L23 7.2 17.8 2H15l-1.5 3h-3L9 2H6.2zM8 9v11h8V9l2.6 2.6.8-.8L16.6 8V4h.6l3.6 3.6-2.3 2.3L17 8.3V20H7V8.3L5.5 9.9 3.2 7.6 6.8 4h.6v4l-1.8 2.8.8.8L8 9z"/></svg>`,
  'E-commerce / Compras online': `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M18 6h-2c0-2.2-1.8-4-4-4S8 3.8 8 6H6c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-6-2c1.1 0 2 .9 2 2h-4c0-1.1.9-2 2-2zm6 16H6V8h2v2c0 .6.4 1 1 1s1-.4 1-1V8h4v2c0 .6.4 1 1 1s1-.4 1-1V8h2v12z"/></svg>`,
  'Restaurante / Lanches': `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M16 6v8h3v8h2V2c-2.8 0-5 2.2-5 4zM11 9H9V2H7v7H5V2H3v7c0 2.2 1.8 4 4 4v9h2v-9c2.2 0 4-1.8 4-4V2h-2v7z"/></svg>`,
  'Investimentos / Assinaturas profissionais': `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M20 7h-4V5l-2-2h-4L8 5v2H4c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V9c0-1.1-.9-2-2-2zM10 5h4v2h-4V5zm10 14H4V9h16v10z"/></svg>`,
  'Saúde / Farmácia / Bem-estar': `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M10 3H8v5H3v2h5v5h2v-5h5V8h-5V3zm4.5 12.5c-1.4 0-2.5 1.1-2.5 2.5s1.1 2.5 2.5 2.5 2.5-1.1 2.5-2.5-1.1-2.5-2.5-2.5z"/></svg>`,
  'Transporte / Auto': `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M18.9 6c-.2-.6-.8-1-1.4-1h-11c-.7 0-1.2.4-1.4 1L3 12v8c0 .6.4 1 1 1h1c.6 0 1-.4 1-1v-1h12v1c0 .6.4 1 1 1h1c.6 0 1-.4 1-1v-8l-2.1-6zM6.5 16c-.8 0-1.5-.7-1.5-1.5S5.7 13 6.5 13s1.5.7 1.5 1.5S7.3 16 6.5 16zm11 0c-.8 0-1.5-.7-1.5-1.5s.7-1.5 1.5-1.5 1.5.7 1.5 1.5-.7 1.5-1.5 1.5zM5 11l1.5-4.5h11L19 11H5z"/></svg>`,
  'Lazer / Entretenimento / Pets': `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M21.6 9.2l-1.4-1.7c-.2-.2-.4-.3-.7-.3H17V5c0-1.1-.9-2-2-2H9c-1.1 0-2 .9-2 2v2.2H4.5c-.3 0-.5.1-.7.3L2.4 9.2c-.3.3-.4.8-.1 1.2l4 5.2c.2.3.5.4.8.4h1.8l-.4 4c0 .6.4 1 1 1h5c.6 0 1-.4 1-1l-.4-4h1.8c.3 0 .6-.2.8-.4l4-5.2c.3-.4.2-.9-.1-1.2zM9 5h6v2.2H9V5z"/></svg>`,
  'Supermercado': `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M7 18c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm10 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zM7.2 14.8l.1-.2L8.1 13h7.4c.8 0 1.4-.4 1.7-1l3.9-7-1.7-1-3.9 7H8.5L4.3 2H1v2h2l3.6 7.6L5.2 14c-.1.3-.2.6-.2 1 0 1.1.9 2 2 2h12v-2H7.4c-.1 0-.2-.1-.2-.2z"/></svg>`,
  'Outros / Transferências': `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M6.99 11L3 15l3.99 4v-3H14v-2H6.99v-3zM21 9l-3.99-4v3H10v2h7.01v3L21 9z"/></svg>`,
  'Moradia / Contas': `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/></svg>`,
};

const DEFAULT_ICON = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2zm1 17h-2v-2h2v2zm2.1-7.7l-.9.9C13.5 12.9 13 13.5 13 15h-2v-.5c0-1.1.5-2.1 1.2-2.8l1.2-1.3c.4-.4.6-.9.6-1.4 0-1.1-.9-2-2-2s-2 .9-2 2H8c0-2.2 1.8-4 4-4s4 1.8 4 4c0 .9-.4 1.7-.9 2.3z"/></svg>`;

export function categoryIcon(category) {
  return ICONS[category || FALLBACK_CATEGORY] ?? DEFAULT_ICON;
}

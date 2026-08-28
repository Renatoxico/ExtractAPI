import { FALLBACK_CATEGORY } from './formatters.js';

export const CATEGORY_COLORS = {
  'Roupas / Acessórios':                          '#AF52DE',
  'E-commerce / Compras online':                  '#5856D6',
  'Restaurante / Lanches':                        '#FF9500',
  'Investimentos / Assinaturas profissionais':    '#007AFF',
  'Saúde / Farmácia / Bem-estar':                 '#FF3B30',
  'Transporte / Auto':                            '#8E8E93',
  'Lazer / Entretenimento / Pets':                '#FFCC00',
  'Supermercado':                                 '#34C759',
  'Outros / Transferências':                      '#A2845E',
  'Moradia / Contas':                             '#32ADE6',
};

export const DEFAULT_COLOR = '#4B5563';

export function categoryColor(category) {
  return CATEGORY_COLORS[category || FALLBACK_CATEGORY] ?? DEFAULT_COLOR;
}

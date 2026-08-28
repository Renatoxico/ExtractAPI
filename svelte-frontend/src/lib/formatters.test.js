import test from 'node:test';
import assert from 'node:assert/strict';

import { categoryColor, CATEGORY_COLORS } from './categoryColors.js';
import { categoryIcon } from './categoryIcons.js';
import { FALLBACK_CATEGORY, formatCategory } from './formatters.js';

test('uses Outros / Transferências consistently for null categories', () => {
  assert.equal(formatCategory(null), FALLBACK_CATEGORY);
  assert.equal(categoryColor(null), CATEGORY_COLORS[FALLBACK_CATEGORY]);
  assert.match(categoryIcon(null), /<svg/);
  assert.equal(categoryIcon(null), categoryIcon(FALLBACK_CATEGORY));
});

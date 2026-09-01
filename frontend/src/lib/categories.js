/** Canonical categories — must match the backend CategoryMapper and the DB. */
export const CATEGORIES = [
  { slug: 'technology', label: 'Technology' },
  { slug: 'business', label: 'Business' },
  { slug: 'world', label: 'World' },
  { slug: 'india', label: 'India' },
  { slug: 'science', label: 'Science' },
  { slug: 'sports', label: 'Sports' },
  { slug: 'health', label: 'Health' },
  { slug: 'entertainment', label: 'Entertainment' },
  { slug: 'politics', label: 'Politics' },
];

export const CATEGORY_LABELS = Object.fromEntries(
  CATEGORIES.map((category) => [category.slug, category.label]),
);

export function labelForCategory(slug) {
  return CATEGORY_LABELS[slug] || (slug ? slug[0].toUpperCase() + slug.slice(1) : 'News');
}

export const LANGUAGES = [
  { code: 'en', label: 'English' },
  { code: 'hi', label: 'Hindi' },
  { code: 'es', label: 'Spanish' },
  { code: 'fr', label: 'French' },
  { code: 'de', label: 'German' },
];

export const COUNTRIES = [
  { code: 'us', label: 'United States' },
  { code: 'in', label: 'India' },
  { code: 'gb', label: 'United Kingdom' },
  { code: 'ca', label: 'Canada' },
  { code: 'au', label: 'Australia' },
];

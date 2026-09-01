import { NavLink } from 'react-router-dom';
import { CATEGORIES } from '../lib/categories';

/** Horizontally scrollable category chips shown under the top bar. */
export function CategoryNav() {
  return (
    <nav aria-label="Categories" className="border-b border-hair bg-surface/80 backdrop-blur">
      <div className="mx-auto flex max-w-6xl gap-2 overflow-x-auto px-4 py-2.5
        [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        {CATEGORIES.map((category) => (
          <NavLink
            key={category.slug}
            to={`/category/${category.slug}`}
            className={({ isActive }) =>
              `whitespace-nowrap rounded-full px-3.5 py-1.5 text-sm font-medium transition-colors
              ${isActive ? 'bg-ink text-white' : 'bg-surface-2 text-muted hover:text-ink'}`
            }
          >
            {category.label}
          </NavLink>
        ))}
      </div>
    </nav>
  );
}

export default CategoryNav;

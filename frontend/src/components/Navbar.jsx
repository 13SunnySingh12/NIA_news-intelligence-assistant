import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { Bookmark, LogOut, Menu, MessageSquareText, Newspaper, Search, Settings, TrendingUp, User, X } from 'lucide-react';
import { useAuth } from '../auth/AuthProvider';
import { Brand } from './ui/Brand';

const NAV_LINKS = [
  { to: '/', label: 'Home', icon: Newspaper, end: true },
  { to: '/trending', label: 'Trending', icon: TrendingUp },
  { to: '/assistant', label: 'Assistant', icon: MessageSquareText },
  { to: '/bookmarks', label: 'Bookmarks', icon: Bookmark },
];

function desktopLinkClass({ isActive }) {
  return `inline-flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm font-medium transition-colors
    ${isActive ? 'bg-brand-soft text-brand-strong' : 'text-muted hover:bg-surface-2 hover:text-ink'}`;
}

export function Navbar() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef(null);

  useEffect(() => {
    function onClickOutside(event) {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setMenuOpen(false);
      }
    }
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  const handleSignOut = async () => {
    await signOut();
    navigate('/login');
  };

  const initial = (user?.email?.[0] || 'U').toUpperCase();

  return (
    <header className="sticky top-0 z-40 border-b border-hair bg-surface/85 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-6xl items-center gap-3 px-4">
        <Link to="/" className="flex items-center pr-2" aria-label="NIA home">
          <Brand size={32} />
        </Link>

        <nav className="hidden items-center gap-1 md:flex" aria-label="Primary">
          {NAV_LINKS.map((link) => (
            <NavLink key={link.to} to={link.to} end={link.end} className={desktopLinkClass}>
              <link.icon className="h-4 w-4" aria-hidden="true" />
              {link.label}
            </NavLink>
          ))}
        </nav>

        <div className="ml-auto flex items-center gap-1">
          <NavLink
            to="/search"
            aria-label="Search"
            className="rounded-lg p-2.5 text-muted transition-colors hover:bg-surface-2 hover:text-ink"
          >
            <Search className="h-5 w-5" aria-hidden="true" />
          </NavLink>

          {/* User menu (desktop) */}
          <div className="relative hidden md:block" ref={menuRef}>
            <button
              type="button"
              onClick={() => setMenuOpen((open) => !open)}
              aria-haspopup="menu"
              aria-expanded={menuOpen}
              className="grid h-9 w-9 place-items-center rounded-full bg-brand-soft text-sm
                font-semibold text-brand-strong transition-colors hover:bg-brand hover:text-white"
            >
              {initial}
            </button>
            {menuOpen && (
              <div
                role="menu"
                className="absolute right-0 mt-2 w-56 overflow-hidden rounded-xl border border-hair
                  bg-surface shadow-pop"
              >
                <p className="truncate border-b border-hair px-4 py-3 text-xs text-muted">
                  {user?.email}
                </p>
                <Link
                  to="/profile"
                  role="menuitem"
                  onClick={() => setMenuOpen(false)}
                  className="flex items-center gap-2 px-4 py-2.5 text-sm text-ink hover:bg-surface-2"
                >
                  <Settings className="h-4 w-4" aria-hidden="true" /> Preferences
                </Link>
                <button
                  type="button"
                  role="menuitem"
                  onClick={handleSignOut}
                  className="flex w-full items-center gap-2 px-4 py-2.5 text-left text-sm text-ink hover:bg-surface-2"
                >
                  <LogOut className="h-4 w-4" aria-hidden="true" /> Sign out
                </button>
              </div>
            )}
          </div>

          {/* Mobile menu toggle */}
          <button
            type="button"
            aria-label={mobileOpen ? 'Close menu' : 'Open menu'}
            aria-expanded={mobileOpen}
            onClick={() => setMobileOpen((open) => !open)}
            className="rounded-lg p-2.5 text-muted transition-colors hover:bg-surface-2 hover:text-ink md:hidden"
          >
            {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>
      </div>

      {mobileOpen && (
        <div className="border-t border-hair bg-surface md:hidden">
          <nav className="mx-auto flex max-w-6xl flex-col gap-1 px-4 py-3" aria-label="Mobile">
            {NAV_LINKS.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                end={link.end}
                onClick={() => setMobileOpen(false)}
                className={desktopLinkClass}
              >
                <link.icon className="h-4 w-4" aria-hidden="true" />
                {link.label}
              </NavLink>
            ))}
            <NavLink to="/profile" onClick={() => setMobileOpen(false)} className={desktopLinkClass}>
              <User className="h-4 w-4" aria-hidden="true" /> Preferences
            </NavLink>
            <button
              type="button"
              onClick={handleSignOut}
              className="mt-1 inline-flex items-center gap-1.5 rounded-lg px-3 py-2 text-left text-sm
                font-medium text-muted hover:bg-surface-2 hover:text-ink"
            >
              <LogOut className="h-4 w-4" aria-hidden="true" /> Sign out
            </button>
          </nav>
        </div>
      )}
    </header>
  );
}

export default Navbar;

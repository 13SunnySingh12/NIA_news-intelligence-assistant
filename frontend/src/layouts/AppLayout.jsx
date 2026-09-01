import { Outlet } from 'react-router-dom';
import { Navbar } from '../components/Navbar';
import { CategoryNav } from '../components/CategoryNav';

/** Shell for all authenticated pages: top bar, category strip, and page content. */
export function AppLayout() {
  return (
    <div className="min-h-screen bg-page">
      <Navbar />
      <CategoryNav />
      <main className="mx-auto max-w-6xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  );
}

export default AppLayout;

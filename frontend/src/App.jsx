import { lazy, Suspense } from 'react';
import { Route, Routes } from 'react-router-dom';
import { AppLayout } from './layouts/AppLayout';
import { ProtectedRoute } from './routes/ProtectedRoute';
import { LoadingState } from './components/ui/states';

// Eager: the auth screens and home are the most common entry points.
import Login from './pages/Login';
import Signup from './pages/Signup';
import ResetPassword from './pages/ResetPassword';
import Home from './pages/Home';

// Lazy: split the rest so the initial bundle stays small.
const Category = lazy(() => import('./pages/Category'));
const Article = lazy(() => import('./pages/Article'));
const Search = lazy(() => import('./pages/Search'));
const Assistant = lazy(() => import('./pages/Assistant'));
const Bookmarks = lazy(() => import('./pages/Bookmarks'));
const Trending = lazy(() => import('./pages/Trending'));
const Profile = lazy(() => import('./pages/Profile'));
const NotFound = lazy(() => import('./pages/NotFound'));

export default function App() {
  return (
    <Suspense fallback={<div className="grid min-h-screen place-items-center"><LoadingState /></div>}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />
        <Route path="/reset-password" element={<ResetPassword />} />

        <Route
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route path="/" element={<Home />} />
          <Route path="/category/:name" element={<Category />} />
          <Route path="/article/:id" element={<Article />} />
          <Route path="/search" element={<Search />} />
          <Route path="/assistant" element={<Assistant />} />
          <Route path="/bookmarks" element={<Bookmarks />} />
          <Route path="/trending" element={<Trending />} />
          <Route path="/profile" element={<Profile />} />
        </Route>

        <Route path="*" element={<NotFound />} />
      </Routes>
    </Suspense>
  );
}

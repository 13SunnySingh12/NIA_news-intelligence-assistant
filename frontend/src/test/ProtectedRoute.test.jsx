import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { ProtectedRoute } from '../routes/ProtectedRoute';

// Control what useAuth() returns for each case.
const mockAuth = vi.fn();
vi.mock('../auth/AuthProvider', () => ({ useAuth: () => mockAuth() }));

function renderAt(authState) {
  mockAuth.mockReturnValue(authState);
  return render(
    <MemoryRouter initialEntries={['/private']}>
      <Routes>
        <Route path="/login" element={<p>login page</p>} />
        <Route
          path="/private"
          element={
            <ProtectedRoute>
              <p>secret content</p>
            </ProtectedRoute>
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe('ProtectedRoute', () => {
  it('redirects an unauthenticated visitor to /login', () => {
    renderAt({ user: null, loading: false });
    expect(screen.getByText('login page')).toBeInTheDocument();
    expect(screen.queryByText('secret content')).not.toBeInTheDocument();
  });

  it('renders the page for a signed-in user', () => {
    renderAt({ user: { id: 'user-1' }, loading: false });
    expect(screen.getByText('secret content')).toBeInTheDocument();
  });

  it('shows a loader instead of redirecting while the session resolves', () => {
    renderAt({ user: null, loading: true });
    // Must NOT bounce to /login before we know whether a session exists.
    expect(screen.queryByText('login page')).not.toBeInTheDocument();
    expect(screen.queryByText('secret content')).not.toBeInTheDocument();
  });
});

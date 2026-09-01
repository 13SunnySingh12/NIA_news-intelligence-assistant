import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Signup from '../pages/Signup';

const signUp = vi.fn();
vi.mock('../auth/AuthProvider', () => ({
  useAuth: () => ({ signUp, signInWithOAuth: vi.fn(), isConfigured: true }),
}));

const renderSignup = () => render(<MemoryRouter><Signup /></MemoryRouter>);

describe('Signup validation', () => {
  beforeEach(() => signUp.mockReset());

  it('blocks submission and reports every invalid field', async () => {
    const user = userEvent.setup();
    renderSignup();

    await user.type(screen.getByLabelText(/^email$/i), 'not-an-email');
    await user.type(screen.getByLabelText(/^password$/i), 'abc');
    await user.type(screen.getByLabelText(/confirm password/i), 'xyz');
    await user.click(screen.getByRole('button', { name: /create account/i }));

    expect(await screen.findByText(/enter a valid email address/i)).toBeInTheDocument();
    expect(screen.getByText(/at least 8 characters/i)).toBeInTheDocument();
    expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument();
    expect(signUp).not.toHaveBeenCalled(); // never hits Supabase with invalid input
  });

  it('submits valid credentials to Supabase auth', async () => {
    const user = userEvent.setup();
    // A brand-new account: Supabase returns a user with one identity and no session.
    signUp.mockResolvedValue({
      data: { user: { identities: [{ provider: 'email' }] }, session: null },
      error: null,
    });
    renderSignup();

    await user.type(screen.getByLabelText(/^email$/i), 'reader@example.com');
    await user.type(screen.getByLabelText(/^password$/i), 'A-strong-password1');
    await user.type(screen.getByLabelText(/confirm password/i), 'A-strong-password1');
    await user.click(screen.getByRole('button', { name: /create account/i }));

    expect(signUp).toHaveBeenCalledWith('reader@example.com', 'A-strong-password1');
    expect(await screen.findByText(/check your email to confirm/i)).toBeInTheDocument();
  });

  it('rejects a password that misses the provider complexity rule', async () => {
    const user = userEvent.setup();
    renderSignup();

    await user.type(screen.getByLabelText(/^email$/i), 'reader@example.com');
    await user.type(screen.getByLabelText(/^password$/i), 'alllowercase');
    await user.type(screen.getByLabelText(/confirm password/i), 'alllowercase');
    await user.click(screen.getByRole('button', { name: /create account/i }));

    expect(await screen.findByText(/uppercase letter/i)).toBeInTheDocument();
    expect(signUp).not.toHaveBeenCalled();
  });

  it('tells the user when the email is already registered', async () => {
    const user = userEvent.setup();
    // Supabase signals "already registered" with an empty identities array.
    signUp.mockResolvedValue({ data: { user: { identities: [] }, session: null }, error: null });
    renderSignup();

    await user.type(screen.getByLabelText(/^email$/i), 'taken@example.com');
    await user.type(screen.getByLabelText(/^password$/i), 'A-strong-password1');
    await user.type(screen.getByLabelText(/confirm password/i), 'A-strong-password1');
    await user.click(screen.getByRole('button', { name: /create account/i }));

    expect(await screen.findByText(/already registered/i)).toBeInTheDocument();
    expect(screen.queryByText(/check your email to confirm/i)).not.toBeInTheDocument();
  });

  it('offers only Google and GitHub sign-in (never Apple)', () => {
    renderSignup();
    expect(screen.getByRole('button', { name: /google/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /github/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /apple/i })).not.toBeInTheDocument();
  });
});

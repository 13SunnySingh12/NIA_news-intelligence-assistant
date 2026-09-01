import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { AuthShell } from '../components/auth/AuthShell';
import { SocialButtons } from '../components/auth/SocialButtons';
import { Field } from '../components/ui/Field';
import { Input } from '../components/ui/Input';
import { PasswordInput } from '../components/ui/PasswordInput';
import { Button } from '../components/ui/Button';
import { useAuth } from '../auth/AuthProvider';

export default function Login() {
  const { signInWithPassword, signInWithOAuth, resetPassword, isConfigured } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from?.pathname || '/';

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setNotice('');
    setLoading(true);
    const { error: signInError } = await signInWithPassword(email.trim(), password);
    setLoading(false);
    if (signInError) {
      setError(signInError.message);
    } else {
      navigate(from, { replace: true });
    }
  };

  const handleProvider = async (provider) => {
    setError('');
    const { error: oauthError } = await signInWithOAuth(provider);
    if (oauthError) setError(oauthError.message);
  };

  const handleForgot = async () => {
    setError('');
    setNotice('');
    if (!email.trim()) {
      setError('Enter your email above, then choose “Forgot password”.');
      return;
    }
    const { error: resetError } = await resetPassword(email.trim());
    if (resetError) setError(resetError.message);
    else setNotice('Check your email for a password reset link.');
  };

  return (
    <AuthShell
      title="Welcome back"
      subtitle="Sign in to your personalized news feed."
      footer={
        <>
          New to NIA?{' '}
          <Link to="/signup" className="font-medium text-brand-strong hover:underline">
            Create an account
          </Link>
        </>
      }
    >
      {!isConfigured && (
        <p className="mb-4 rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-700">
          Sign-in becomes available once Supabase keys are configured.
        </p>
      )}

      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <Field label="Email">
          {({ id, describedBy, invalid }) => (
            <Input
              id={id}
              describedBy={describedBy}
              invalid={invalid}
              type="email"
              autoComplete="email"
              placeholder="you@example.com"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          )}
        </Field>

        <Field label="Password">
          {({ id, describedBy, invalid }) => (
            <PasswordInput
              id={id}
              describedBy={describedBy}
              invalid={invalid}
              autoComplete="current-password"
              placeholder="Your password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
          )}
        </Field>

        <div className="flex justify-end">
          <button
            type="button"
            onClick={handleForgot}
            className="text-sm font-medium text-brand-strong hover:underline"
          >
            Forgot password?
          </button>
        </div>

        {error && (
          <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600" role="alert">
            {error}
          </p>
        )}
        {notice && (
          <p className="rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700">{notice}</p>
        )}

        <Button type="submit" size="lg" loading={loading} className="w-full">
          Sign in
        </Button>
      </form>

      <div className="my-5 flex items-center gap-3 text-xs text-muted">
        <span className="h-px flex-1 bg-hair" />
        or continue with
        <span className="h-px flex-1 bg-hair" />
      </div>

      <SocialButtons onProvider={handleProvider} disabled={loading} />
    </AuthShell>
  );
}

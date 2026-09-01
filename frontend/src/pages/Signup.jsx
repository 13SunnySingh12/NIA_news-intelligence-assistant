import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthShell } from '../components/auth/AuthShell';
import { SocialButtons } from '../components/auth/SocialButtons';
import { Field } from '../components/ui/Field';
import { Input } from '../components/ui/Input';
import { PasswordInput } from '../components/ui/PasswordInput';
import { Button } from '../components/ui/Button';
import { useAuth } from '../auth/AuthProvider';

export default function Signup() {
  const { signUp, signInWithOAuth, resendVerification, isConfigured } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(false);
  const [pendingEmail, setPendingEmail] = useState('');
  const [resendIn, setResendIn] = useState(0);

  const validate = () => {
    const errors = {};
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      errors.email = 'Enter a valid email address.';
    }
    if (password.length < 8) {
      errors.password = 'Use at least 8 characters.';
    } else if (!/[a-z]/.test(password) || !/[A-Z]/.test(password) || !/[0-9]/.test(password)) {
      // Supabase itself rejects weaker passwords, so check here for a clear message.
      errors.password = 'Include an uppercase letter, a lowercase letter, and a number.';
    }
    if (confirm !== password) {
      errors.confirm = 'Passwords do not match.';
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setNotice('');
    if (!validate()) return;

    setLoading(true);
    const { data, error: signUpError } = await signUp(email.trim(), password);
    setLoading(false);

    if (signUpError) {
      setError(signUpError.message);
      return;
    }
    // Supabase returns a user with an empty `identities` array when the address is
    // already registered (it will not send another email). Say so plainly instead
    // of telling the user to check an inbox that will stay empty.
    if (data?.user && Array.isArray(data.user.identities) && data.user.identities.length === 0) {
      setError('That email is already registered. Try signing in, or reset your password.');
      return;
    }
    if (data?.session) {
      navigate('/', { replace: true });
      return;
    }
    setPendingEmail(email.trim());
    startResendCooldown();
    setNotice('Account created. Check your email to confirm, then sign in.');
  };

  // Simple client-side cooldown so the resend button can't be hammered.
  const startResendCooldown = () => {
    setResendIn(60);
    const timer = setInterval(() => {
      setResendIn((seconds) => {
        if (seconds <= 1) {
          clearInterval(timer);
          return 0;
        }
        return seconds - 1;
      });
    }, 1000);
  };

  const handleResend = async () => {
    setError('');
    const { error: resendError } = await resendVerification(pendingEmail);
    if (resendError) {
      setError(resendError.message);
      return;
    }
    startResendCooldown();
    setNotice('Verification email sent again. Check your inbox and spam folder.');
  };

  const handleProvider = async (provider) => {
    setError('');
    const { error: oauthError } = await signInWithOAuth(provider);
    if (oauthError) setError(oauthError.message);
  };

  return (
    <AuthShell
      title="Create your account"
      subtitle="Get a news feed tailored to what you care about."
      footer={
        <>
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-brand-strong hover:underline">
            Sign in
          </Link>
        </>
      }
    >
      {!isConfigured && (
        <p className="mb-4 rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-700">
          Sign-up becomes available once Supabase keys are configured.
        </p>
      )}

      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <Field label="Email" error={fieldErrors.email}>
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

        <Field label="Password" error={fieldErrors.password} hint="At least 8 characters.">
          {({ id, describedBy, invalid }) => (
            <PasswordInput
              id={id}
              describedBy={describedBy}
              invalid={invalid}
              autoComplete="new-password"
              placeholder="Create a password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
          )}
        </Field>

        <Field label="Confirm password" error={fieldErrors.confirm}>
          {({ id, describedBy, invalid }) => (
            <PasswordInput
              id={id}
              describedBy={describedBy}
              invalid={invalid}
              autoComplete="new-password"
              placeholder="Re-enter your password"
              value={confirm}
              onChange={(event) => setConfirm(event.target.value)}
              required
            />
          )}
        </Field>

        {error && (
          <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600" role="alert">
            {error}
          </p>
        )}
        {notice && (
          <div className="space-y-2 rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700">
            <p>{notice}</p>
            {pendingEmail && (
              <button
                type="button"
                onClick={handleResend}
                disabled={resendIn > 0}
                className="font-medium underline disabled:no-underline disabled:opacity-60"
              >
                {resendIn > 0 ? `Resend email in ${resendIn}s` : "Didn't get it? Resend email"}
              </button>
            )}
          </div>
        )}

        <Button type="submit" size="lg" loading={loading} className="w-full">
          Create account
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

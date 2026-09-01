import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthShell } from '../components/auth/AuthShell';
import { Field } from '../components/ui/Field';
import { PasswordInput } from '../components/ui/PasswordInput';
import { Button } from '../components/ui/Button';
import { useAuth } from '../auth/AuthProvider';

/**
 * Completes the "forgot password" flow. Supabase's reset email links here with a
 * recovery token in the URL; the Supabase client (detectSessionInUrl) turns that
 * into a temporary session, which lets the user set a new password via updateUser.
 */
export default function ResetPassword() {
  const { session, loading: authLoading, updatePassword } = useAuth();
  const navigate = useNavigate();

  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(false);

  const validate = () => {
    const errors = {};
    if (password.length < 8) {
      errors.password = 'Use at least 8 characters.';
    } else if (!/[a-z]/.test(password) || !/[A-Z]/.test(password) || !/[0-9]/.test(password)) {
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
    const { error: updateError } = await updatePassword(password);
    setLoading(false);

    if (updateError) {
      setError(updateError.message);
    } else {
      setNotice('Password updated. Taking you to your feed…');
      setTimeout(() => navigate('/', { replace: true }), 1200);
    }
  };

  // Once auth has loaded, a recovery link should have produced a session.
  const missingRecovery = !authLoading && !session;

  return (
    <AuthShell
      title="Set a new password"
      subtitle="Choose a new password for your account."
      footer={
        <>
          Remembered it?{' '}
          <Link to="/login" className="font-medium text-brand-strong hover:underline">
            Sign in
          </Link>
        </>
      }
    >
      {missingRecovery && (
        <p className="mb-4 rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-700">
          Open the password reset link from your email to set a new password.
        </p>
      )}

      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <Field label="New password" error={fieldErrors.password} hint="At least 8 characters.">
          {({ id, describedBy, invalid }) => (
            <PasswordInput
              id={id}
              describedBy={describedBy}
              invalid={invalid}
              autoComplete="new-password"
              placeholder="Create a new password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
          )}
        </Field>

        <Field label="Confirm new password" error={fieldErrors.confirm}>
          {({ id, describedBy, invalid }) => (
            <PasswordInput
              id={id}
              describedBy={describedBy}
              invalid={invalid}
              autoComplete="new-password"
              placeholder="Re-enter your new password"
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
          <p className="rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700">{notice}</p>
        )}

        <Button type="submit" size="lg" loading={loading} disabled={missingRecovery} className="w-full">
          Update password
        </Button>
      </form>
    </AuthShell>
  );
}

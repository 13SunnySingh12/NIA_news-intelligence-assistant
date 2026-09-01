import { useCallback, useEffect, useState } from 'react';
import { getPreferences, updatePreferences } from '../api/preferences';
import { CATEGORIES, COUNTRIES, LANGUAGES } from '../lib/categories';
import { Button } from '../components/ui/Button';
import { LoadingState, ErrorState } from '../components/ui/states';
import { useAuth } from '../auth/AuthProvider';

function normalize(data) {
  return {
    favoriteCategories: data?.favoriteCategories ?? [],
    languages: data?.languages?.length ? data.languages : ['en'],
    countries: data?.countries?.length ? data.countries : ['us'],
  };
}

function TogglePill({ active, onClick, children }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={`rounded-full border px-3.5 py-1.5 text-sm font-medium transition-colors
        ${active ? 'border-brand bg-brand text-white' : 'border-hair bg-surface text-ink hover:bg-surface-2'}`}
    >
      {children}
    </button>
  );
}

function Section({ title, description, children }) {
  return (
    <div className="border-t border-hair py-6 first:border-t-0 first:pt-0">
      <h2 className="text-sm font-semibold text-ink">{title}</h2>
      {description && <p className="mt-0.5 text-sm text-muted">{description}</p>}
      <div className="mt-3 flex flex-wrap gap-2">{children}</div>
    </div>
  );
}

export default function Profile() {
  const { user } = useAuth();
  const [prefs, setPrefs] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState('');
  const [saveError, setSaveError] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    getPreferences()
      .then((data) => {
        setPrefs(normalize(data));
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message || 'Could not load your preferences.');
        setLoading(false);
      });
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const toggle = (field, value) =>
    setPrefs((current) => {
      const set = new Set(current[field]);
      if (set.has(value)) set.delete(value);
      else set.add(value);
      return { ...current, [field]: [...set] };
    });

  const save = async () => {
    setSaving(true);
    setNotice('');
    setSaveError('');
    try {
      const updated = await updatePreferences(prefs);
      setPrefs(normalize(updated));
      setNotice('Preferences saved. Your feed will reflect these on your next visit home.');
    } catch (err) {
      setSaveError(err.message || 'Could not save your preferences.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <LoadingState message="Loading preferences…" />;
  if (error) return <ErrorState message={error} onRetry={load} />;

  return (
    <section className="mx-auto max-w-2xl">
      <div className="mb-6">
        <h1 className="text-2xl font-bold tracking-tight text-ink">Preferences</h1>
        <p className="mt-1 text-sm text-muted">Tune what shows up in your personalized feed.</p>
      </div>

      <div className="rounded-2xl border border-hair bg-surface p-6">
        <Section title="Account">
          <span className="rounded-lg bg-surface-2 px-3 py-1.5 text-sm text-muted">{user?.email}</span>
        </Section>

        <Section title="Favorite categories" description="Prioritize these in your feed.">
          {CATEGORIES.map((category) => (
            <TogglePill
              key={category.slug}
              active={prefs.favoriteCategories.includes(category.slug)}
              onClick={() => toggle('favoriteCategories', category.slug)}
            >
              {category.label}
            </TogglePill>
          ))}
        </Section>

        <Section title="Languages" description="Show articles in these languages.">
          {LANGUAGES.map((language) => (
            <TogglePill
              key={language.code}
              active={prefs.languages.includes(language.code)}
              onClick={() => toggle('languages', language.code)}
            >
              {language.label}
            </TogglePill>
          ))}
        </Section>

        <Section title="Countries" description="Focus coverage on these countries.">
          {COUNTRIES.map((country) => (
            <TogglePill
              key={country.code}
              active={prefs.countries.includes(country.code)}
              onClick={() => toggle('countries', country.code)}
            >
              {country.label}
            </TogglePill>
          ))}
        </Section>

        <div className="mt-6 flex flex-wrap items-center gap-3 border-t border-hair pt-6">
          <Button onClick={save} loading={saving}>
            Save preferences
          </Button>
          {notice && <span className="text-sm text-green-700">{notice}</span>}
          {saveError && <span className="text-sm text-red-600">{saveError}</span>}
        </div>
      </div>
    </section>
  );
}

import { useEffect, useRef, useState } from 'react';
import { Send, Sparkles } from 'lucide-react';
import { askAssistant } from '../api/assistant';
import { SourceList } from '../components/SourceList';
import { Spinner } from '../components/ui/Spinner';

const SUGGESTIONS = [
  'What are the latest developments in AI?',
  'Summarize this week in world news.',
  "What's happening in business today?",
];

function MessageBubble({ message }) {
  if (message.role === 'user') {
    return (
      <div className="flex justify-end">
        <div className="max-w-[85%] rounded-2xl rounded-br-md bg-brand px-4 py-2.5 text-[15px] text-white">
          {message.content}
        </div>
      </div>
    );
  }
  return (
    <div className="flex justify-start">
      <div
        className={`max-w-[90%] rounded-2xl rounded-bl-md border px-4 py-3 text-[15px] leading-relaxed
          ${message.error ? 'border-red-200 bg-red-50 text-red-700' : 'border-hair bg-surface text-ink/90'}`}
      >
        <p className="whitespace-pre-wrap">{message.content}</p>
        {!message.error && <SourceList sources={message.sources} />}
      </div>
    </div>
  );
}

export default function Assistant() {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const endRef = useRef(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, sending]);

  const send = async (text) => {
    const question = text.trim();
    if (!question || sending) return;

    const history = messages.slice(-6).map((m) => ({ role: m.role, content: m.content }));
    setMessages((prev) => [...prev, { role: 'user', content: question }]);
    setInput('');
    setSending(true);
    try {
      const response = await askAssistant(question, history);
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: response.answer, sources: response.sources },
      ]);
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: err.message || 'The assistant is unavailable right now — please try again in a few minutes.',
          error: true,
        },
      ]);
    } finally {
      setSending(false);
    }
  };

  const handleKeyDown = (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      send(input);
    }
  };

  return (
    <section className="mx-auto flex min-h-[calc(100vh-11rem)] max-w-3xl flex-col">
      <div className="mb-4">
        <h1 className="text-2xl font-bold tracking-tight text-ink">News assistant</h1>
        <p className="mt-1 text-sm text-muted">
          Ask about the news. Answers are grounded in real articles and cite their sources.
        </p>
      </div>

      <div className="flex-1 space-y-4">
        {messages.length === 0 ? (
          <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed
            border-hair bg-surface/60 px-6 py-14 text-center">
            <div className="mb-4 rounded-2xl bg-brand-soft p-3 text-brand">
              <Sparkles className="h-6 w-6" aria-hidden="true" />
            </div>
            <h2 className="text-base font-semibold text-ink">Ask about today’s news</h2>
            <p className="mt-1 max-w-sm text-sm text-muted">
              Try one of these to get started:
            </p>
            <div className="mt-4 flex flex-wrap justify-center gap-2">
              {SUGGESTIONS.map((suggestion) => (
                <button
                  key={suggestion}
                  onClick={() => send(suggestion)}
                  className="rounded-full border border-hair bg-surface px-3.5 py-1.5 text-sm
                    text-ink transition-colors hover:bg-surface-2"
                >
                  {suggestion}
                </button>
              ))}
            </div>
          </div>
        ) : (
          messages.map((message, index) => <MessageBubble key={index} message={message} />)
        )}

        {sending && (
          <div className="flex items-center gap-2 text-sm text-muted">
            <Spinner className="h-4 w-4" /> Thinking…
          </div>
        )}
        <div ref={endRef} />
      </div>

      <div className="sticky bottom-0 mt-4 border-t border-hair bg-page/90 py-3 backdrop-blur">
        <div className="flex items-end gap-2">
          <textarea
            value={input}
            onChange={(event) => setInput(event.target.value)}
            onKeyDown={handleKeyDown}
            rows={1}
            placeholder="Ask a question about the news…"
            aria-label="Ask the news assistant"
            className="max-h-40 min-h-[2.75rem] flex-1 resize-none rounded-xl border border-hair
              bg-surface px-3.5 py-3 text-sm text-ink transition-colors focus:border-brand"
          />
          <button
            type="button"
            onClick={() => send(input)}
            disabled={sending || !input.trim()}
            aria-label="Send"
            className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-brand text-white
              transition-colors hover:bg-brand-strong disabled:opacity-50"
          >
            <Send className="h-4 w-4" aria-hidden="true" />
          </button>
        </div>
      </div>
    </section>
  );
}

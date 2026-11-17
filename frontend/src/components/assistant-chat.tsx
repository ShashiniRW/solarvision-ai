"use client";

import { FormEvent, KeyboardEvent, useEffect, useId, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import Markdown, { type Components } from "react-markdown";
import remarkGfm from "remark-gfm";
import { Bot, FileText, RotateCcw, Send } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { brandButtonClass } from "@/lib/styles";
import type { ChatReply, ChatRole, ChatSource } from "@/lib/types";

type Turn = { role: ChatRole; text: string; sources?: ChatSource[] };

// Same limits as the backend, so the user gets a clear message before sending
const MAX_QUESTION = 2000;
const MAX_HISTORY_TURNS = 20;
const MAX_HISTORY_TEXT = 4000;

const SUGGESTIONS = [
  "What does SUSTAINED UNDERPERFORMANCE mean?",
  "My inverter output drops on hot afternoons. Why?",
  "When is the best time to clean solar panels?",
  "What must I do before working on the DC side?",
];

// How each part of an answer looks. Raw HTML is never rendered:
// react-markdown is safe by default, so AI text cannot inject scripts.
const markdownComponents: Components = {
  p: ({ children }) => <p className="mb-2 last:mb-0">{children}</p>,
  ul: ({ children }) => <ul className="mb-2 list-disc space-y-1 pl-5 last:mb-0">{children}</ul>,
  ol: ({ children }) => <ol className="mb-2 list-decimal space-y-1 pl-5 last:mb-0">{children}</ol>,
  strong: ({ children }) => <strong className="font-semibold">{children}</strong>,
  code: ({ children }) => (
    <code className="rounded bg-background/60 px-1 py-0.5 font-mono text-[0.85em]">{children}</code>
  ),
  a: ({ href, children }) => (
    <a href={href} target="_blank" rel="noopener noreferrer" className="underline underline-offset-2">
      {children}
    </a>
  ),
};

export function AssistantChat() {
  const router = useRouter();
  const inputId = useId();
  const [turns, setTurns] = useState<Turn[]>([]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const endRef = useRef<HTMLDivElement>(null);

  // keep the newest message in view (no smooth scrolling for users who prefer reduced motion)
  useEffect(() => {
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    endRef.current?.scrollIntoView({ block: "end", behavior: reduceMotion ? "auto" : "smooth" });
  }, [turns, busy]);

  async function ask(question: string) {
    const text = question.trim();
    if (!text || busy) {
      return;
    }
    if (text.length > MAX_QUESTION) {
      setError(`Please keep your question under ${MAX_QUESTION} characters.`);
      return;
    }

    // the conversation so far, so follow-up questions make sense
    const history = turns
      .slice(-MAX_HISTORY_TURNS)
      .map((turn) => ({ role: turn.role, text: turn.text.slice(0, MAX_HISTORY_TEXT) }));

    setTurns((current) => [...current, { role: "user", text }]);
    setInput("");
    setError(null);
    setBusy(true);

    try {
      const response = await fetch("/api/backend/api/assistant/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ message: text, history }),
      });

      // the login is no longer valid: clear the cookie, then show the login page
      if (response.status === 401) {
        await fetch("/api/auth/logout", { method: "POST" });
        router.push("/login?expired=1");
        return;
      }

      if (!response.ok) {
        let message = "The assistant could not answer. Please try again.";
        try {
          const data = await response.json();
          if (data?.fieldErrors) {
            message = Object.values(data.fieldErrors as Record<string, string>).join(" ");
          } else if (data?.message) {
            message = data.message;
          }
        } catch {
          // the error body was not JSON: keep the default message
        }
        undoQuestion(text, message);
        return;
      }

      const data = (await response.json()) as ChatReply;
      setTurns((current) => [...current, { role: "model", text: data.reply, sources: data.sources }]);
    } catch {
      undoQuestion(text, "Cannot reach the server. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  // The question was not answered: move it back into the input box, so it can be sent again
  function undoQuestion(question: string, message: string) {
    setTurns((current) => current.slice(0, -1));
    setInput(question);
    setError(message);
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void ask(input);
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    // Enter sends, Shift+Enter adds a new line (but not while an input method is composing text)
    if (event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing) {
      event.preventDefault();
      void ask(input);
    }
  }

  function startNewChat() {
    setTurns([]);
    setError(null);
  }

  return (
    <div className="flex h-[calc(100dvh-13rem)] min-h-104 flex-col rounded-xl border bg-card">
      <div className="flex items-center justify-between gap-3 border-b px-4 py-3">
        <p className="text-sm text-muted-foreground">Answers are based on the SolarVision guides.</p>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={startNewChat}
          disabled={busy || turns.length === 0}
        >
          <RotateCcw aria-hidden="true" />
          New chat
        </Button>
      </div>

      <div role="log" aria-live="polite" aria-label="Conversation" className="flex-1 space-y-4 overflow-y-auto p-4">
        {turns.length === 0 && !busy && (
          <div className="mx-auto flex max-w-xl flex-col items-center gap-4 py-8 text-center">
            <Bot className="size-10 text-amber-500" aria-hidden="true" />
            <div className="space-y-1">
              <h2 className="text-lg font-semibold">Ask about solar maintenance or SolarVision</h2>
              <p className="text-sm text-muted-foreground">Try one of these questions:</p>
            </div>
            <div className="grid w-full gap-2 sm:grid-cols-2">
              {SUGGESTIONS.map((suggestion) => (
                <button
                  key={suggestion}
                  type="button"
                  onClick={() => void ask(suggestion)}
                  className="rounded-lg border bg-muted/40 px-3 py-2 text-left text-sm transition-colors hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring motion-reduce:transition-none"
                >
                  {suggestion}
                </button>
              ))}
            </div>
          </div>
        )}

        {turns.map((turn, index) => (
          <div key={index} className={turn.role === "user" ? "flex justify-end" : "flex justify-start"}>
            <div
              className={
                turn.role === "user"
                  ? "max-w-[85%] whitespace-pre-wrap rounded-2xl rounded-br-sm bg-amber-500 px-4 py-2 text-sm text-white dark:bg-amber-600"
                  : "max-w-[85%] rounded-2xl rounded-bl-sm bg-muted px-4 py-3 text-sm"
              }
            >
              <span className="sr-only">{turn.role === "user" ? "You:" : "Assistant:"}</span>
              {turn.role === "user" ? (
                turn.text
              ) : (
                <Markdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
                  {turn.text}
                </Markdown>
              )}

              {turn.sources && turn.sources.length > 0 && (
                <div className="mt-3 flex flex-wrap items-center gap-1.5 border-t pt-2">
                  <span className="sr-only">Sources:</span>
                  {turn.sources.map((source) => (
                    <span
                      key={`${source.document}-${source.title}`}
                      title={source.document}
                      className="inline-flex items-center gap-1 rounded-full border bg-background px-2 py-0.5 text-xs text-muted-foreground"
                    >
                      <FileText className="size-3" aria-hidden="true" />
                      {source.title}
                    </span>
                  ))}
                </div>
              )}
            </div>
          </div>
        ))}

        {busy && (
          <div role="status" className="flex justify-start">
            <div className="flex items-center gap-2 rounded-2xl rounded-bl-sm bg-muted px-4 py-3 text-sm text-muted-foreground">
              <span className="flex gap-1" aria-hidden="true">
                <span className="size-1.5 animate-bounce rounded-full bg-current motion-reduce:animate-none" />
                <span className="size-1.5 animate-bounce rounded-full bg-current [animation-delay:150ms] motion-reduce:animate-none" />
                <span className="size-1.5 animate-bounce rounded-full bg-current [animation-delay:300ms] motion-reduce:animate-none" />
              </span>
              Thinking...
            </div>
          </div>
        )}

        <div ref={endRef} />
      </div>

      <form onSubmit={handleSubmit} className="border-t p-3">
        {error && (
          <p role="alert" className="mb-2 text-sm text-red-600 dark:text-red-400">
            {error}
          </p>
        )}
        <div className="flex items-end gap-2">
          <label htmlFor={inputId} className="sr-only">
            Your question
          </label>
          <Textarea
            id={inputId}
            value={input}
            onChange={(event) => setInput(event.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask a question (Enter to send, Shift+Enter for a new line)"
            rows={2}
            maxLength={MAX_QUESTION}
            className="max-h-40 min-h-11 resize-none"
          />
          <Button
            type="submit"
            disabled={busy || input.trim() === ""}
            className={brandButtonClass}
            aria-label="Send question"
          >
            <Send aria-hidden="true" />
          </Button>
        </div>
        <p className="mt-2 text-xs text-muted-foreground">
          AI answers can be wrong. For electrical work, always follow the safety rules and call a qualified
          technician.
        </p>
      </form>
    </div>
  );
}

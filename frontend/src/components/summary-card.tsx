import Link from "next/link";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { cn } from "@/lib/utils";

type Tone = "normal" | "warning" | "danger";

// Status colours carry meaning, so they stay explicit (with dark-mode versions).
const TONES: Record<Tone, string> = {
  normal: "",
  warning: "border-amber-300 bg-amber-50 dark:border-amber-900 dark:bg-amber-950/40",
  danger: "border-red-300 bg-red-50 dark:border-red-900 dark:bg-red-950/40",
};

type Props = {
  label: string;
  value: string;
  hint?: string;
  href?: string;
  tone?: Tone;
};

// A big number with a label, optionally a link to the page behind it.
export function SummaryCard({ label, value, hint, href, tone = "normal" }: Props) {
  const card = (
    <Card className={cn("h-full", TONES[tone])}>
      <CardHeader>
        <CardDescription className="text-xs uppercase tracking-wide">{label}</CardDescription>
        <CardTitle className="text-3xl font-semibold tabular-nums">{value}</CardTitle>
      </CardHeader>
      {hint && (
        <CardContent>
          <p className="text-sm text-muted-foreground">{hint}</p>
        </CardContent>
      )}
    </Card>
  );

  if (!href) {
    return card;
  }

  return (
    <Link
      href={href}
      className="block rounded-xl transition hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
    >
      {card}
    </Link>
  );
}

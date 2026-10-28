import Link from "next/link";

type Tone = "normal" | "warning" | "danger";

const TONES: Record<Tone, string> = {
  normal: "border-gray-200 bg-white",
  warning: "border-amber-300 bg-amber-50",
  danger: "border-red-300 bg-red-50",
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
    <div className={`h-full rounded-lg border p-5 ${TONES[tone]}`}>
      <p className="text-xs uppercase text-gray-500">{label}</p>
      <p className="mt-2 text-3xl font-semibold">{value}</p>
      {hint && <p className="mt-1 text-sm text-gray-600">{hint}</p>}
    </div>
  );

  if (!href) {
    return card;
  }

  return (
    <Link href={href} className="block rounded-lg transition hover:shadow-md">
      {card}
    </Link>
  );
}
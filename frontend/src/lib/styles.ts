// Shared class names, so the same look is defined in one place.

// Links inside content (tables, cards): the brand amber, readable in light and dark mode
export const linkClass =
  "font-medium text-amber-700 underline-offset-4 hover:underline dark:text-amber-400";

// Native <select>: same look as the shadcn Input (height, border, focus ring, dark background)
export const selectClass =
  "h-9 w-full rounded-md border border-input bg-transparent px-3 text-sm shadow-xs outline-none transition-[color,box-shadow] focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-input/30 [&>option]:bg-popover [&>option]:text-popover-foreground";

// Brand action button (amber), used on top of the shadcn Button
export const brandButtonClass = "bg-amber-500 text-white hover:bg-amber-600";

// Positive action button (green): Resolve, Complete, Save
export const successButtonClass = "bg-green-600 text-white hover:bg-green-700";

// A layout stays mounted between pages; a template is re-created on every navigation.
// So this short fade runs each time you open a page. It is switched off for "reduce motion".
export default function AppTemplate({ children }: { children: React.ReactNode }) {
  return (
    <div className="animate-in fade-in-0 duration-300 motion-reduce:animate-none">
      {children}
    </div>
  );
}

export type BeaconModeStrict = "normal" | "emergency" | "evacuation" | "maintenance";

export function getModeColor(mode: BeaconModeStrict | null): string {
  switch (mode) {
    case "normal":
      return "bg-emerald-500/10 text-emerald-400 ring-emerald-500/30";
    case "emergency":
      return "bg-red-500/10 text-red-400 ring-red-500/30";
    case "evacuation":
      return "bg-amber-500/10 text-amber-400 ring-amber-500/30";
    case "maintenance":
      return "bg-sky-500/10 text-sky-400 ring-sky-500/30";
    default:
      return "bg-slate-500/10 text-slate-400 ring-slate-500/30";
  }
}

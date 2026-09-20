import { Check, Circle, PartyPopper } from "lucide-react"
import { useMemo, useState } from "react"
import { Link } from "react-router-dom"

import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { useAuthStore } from "@/features/auth/auth-store"
import { useClubTypes } from "@/features/clubs/club-hooks"
import { cn } from "@/lib/utils"

type ChecklistStep = {
  id: string
  title: string
  description: string
  path: string
  action: string
  permission: string
  module: string
}

const checklistSteps: ChecklistStep[] = [
  { id: "members", title: "Build your member list", description: "Invite members individually or import a CSV, then confirm everyone has the right status.", path: "/members", action: "Open members", permission: "MEMBERS_WRITE", module: "MEMBERS" },
  { id: "contributions", title: "Set contribution expectations", description: "Create a schedule so members know what is due and when it should be paid.", path: "/contributions", action: "Set up contributions", permission: "CONTRIBUTIONS_WRITE", module: "CONTRIBUTIONS" },
  { id: "meetings", title: "Plan the next meeting", description: "Schedule a meeting, add its agenda, and give members time to RSVP.", path: "/meetings", action: "Schedule a meeting", permission: "MEETINGS_WRITE", module: "MEETINGS" },
  { id: "voting", title: "Prepare club decisions", description: "Create a motion when the club needs an accountable, one-member-one-vote decision.", path: "/voting", action: "Create a motion", permission: "VOTES_CREATE", module: "VOTING" },
  { id: "documents", title: "Share important documents", description: "Upload rules, minutes, or statements and choose which roles may see them.", path: "/documents", action: "Open documents", permission: "DOCUMENTS_MANAGE", module: "DOCUMENTS" },
]

function readCompleted(storageKey: string) {
  try {
    const value = JSON.parse(window.localStorage.getItem(storageKey) ?? "[]")
    return Array.isArray(value) ? value.filter((item): item is string => typeof item === "string") : []
  } catch {
    return []
  }
}

export function OnboardingChecklist() {
  const activeClub = useAuthStore((state) => state.activeClub)
  const userId = useAuthStore((state) => state.user?.id)
  const clubTypes = useClubTypes()
  const activeTemplate = clubTypes.data?.find((type) => type.code === activeClub?.clubType)
  const enabledModules = activeTemplate?.enabledModules
  const storageKey = `kds:onboarding:${userId ?? "user"}:${activeClub?.id ?? "club"}`
  const [completed, setCompleted] = useState<string[]>(() => readCompleted(storageKey))

  const availableSteps = useMemo(() => checklistSteps.filter((step) =>
    activeClub?.permissions?.includes(step.permission)
    && (!enabledModules || enabledModules.includes(step.module))), [activeClub?.permissions, enabledModules])

  if (!activeClub?.administrator || availableSteps.length === 0) return null

  const completedCount = availableSteps.filter((step) => completed.includes(step.id)).length
  const toggle = (stepId: string) => {
    setCompleted((current) => {
      const next = current.includes(stepId) ? current.filter((id) => id !== stepId) : [...current, stepId]
      window.localStorage.setItem(storageKey, JSON.stringify(next))
      return next
    })
  }

  return (
    <Card className="border-primary/25 bg-primary/[0.03] shadow-none">
      <CardHeader className="gap-2">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="flex items-center gap-2 text-xl font-semibold"><PartyPopper className="size-5 text-primary" aria-hidden="true" />Set up your club</h2>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-muted-foreground">Work through these first actions at your own pace. Tick a step when your club is comfortable with it.</p>
          </div>
          <p className="rounded-full bg-background px-3 py-1 text-sm font-medium" aria-live="polite">{completedCount} of {availableSteps.length} complete</p>
        </div>
        <div className="h-2 overflow-hidden rounded-full bg-muted" aria-hidden="true"><div className="h-full bg-primary transition-[width]" style={{ width: `${(completedCount / availableSteps.length) * 100}%` }} /></div>
      </CardHeader>
      <CardContent>
        {completedCount === availableSteps.length && <p role="status" className="mb-4 rounded-lg border border-primary/20 bg-background p-3 text-sm font-medium">Your pilot setup checklist is complete. You can revisit any step whenever the club's process changes.</p>}
        <ol className="grid gap-3 lg:grid-cols-2">
          {availableSteps.map((step) => {
            const isComplete = completed.includes(step.id)
            return <li key={step.id} className={cn("rounded-xl border bg-background p-4", isComplete && "border-primary/25 bg-primary/[0.04]")}>
              <div className="flex items-start gap-3">
                <button type="button" className="mt-0.5 shrink-0 rounded-full text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring" aria-label={`${isComplete ? "Mark incomplete" : "Mark complete"}: ${step.title}`} aria-pressed={isComplete} onClick={() => toggle(step.id)}>
                  {isComplete ? <span className="grid size-6 place-items-center rounded-full bg-primary text-primary-foreground"><Check className="size-4" aria-hidden="true" /></span> : <Circle className="size-6" aria-hidden="true" />}
                </button>
                <div className="min-w-0"><h3 className={cn("font-semibold", isComplete && "text-muted-foreground line-through")}>{step.title}</h3><p className="mt-1 text-sm leading-5 text-muted-foreground">{step.description}</p><Link className="mt-2 inline-block text-sm font-medium text-primary underline-offset-4 hover:underline" to={step.path}>{step.action}</Link></div>
              </div>
            </li>
          })}
        </ol>
      </CardContent>
    </Card>
  )
}

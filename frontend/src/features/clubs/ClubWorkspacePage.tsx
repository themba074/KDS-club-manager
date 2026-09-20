import { Link, useNavigate } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { errorMessage } from "@/features/auth/auth-api"
import { EmptyState } from "@/components/states/EmptyState"
import { ErrorState } from "@/components/states/ErrorState"
import { Building2 } from "lucide-react"
import { useClubs, useSelectClub, useClubContext, useClubTypes } from "./club-hooks"
import { CreateClubWizard } from "./CreateClubWizard"

export function ClubWorkspacePage() {
  const clubs = useClubs()
  const clubTypes = useClubTypes()
  const selection = useSelectClub()
  const activeClub = useClubContext()
  const navigate = useNavigate()
  const select = (clubId: string) => selection.mutate(clubId, { onSuccess: () => navigate("/", { replace: true }) })

  return <main className="mx-auto min-h-screen max-w-3xl space-y-6 p-6">
    <header><p className="font-bold text-primary">KDS Club Manager</p><h1 className="mt-2 text-3xl font-semibold">Your clubs</h1>
      <p className="mt-2 text-muted-foreground">Choose a workspace or create your first club.</p>
      {activeClub && <Link className="mt-3 inline-block underline" to="/">Return to {activeClub.name}</Link>}
    </header>
    {clubs.isPending && <p role="status">Loading your clubs…</p>}
    {clubs.error && <ErrorState title="We couldn't load your clubs" description={errorMessage(clubs.error, "Try loading your club list again.")} onRetry={() => void clubs.refetch()}/>}
    {clubs.data?.length === 0 && <EmptyState icon={Building2} title="You don't belong to a club yet" description="Create your first club below, or ask a club administrator to send you an invitation."/>}
    <ul className="space-y-3">{clubs.data?.map((club) => <li key={club.id} className="flex items-center justify-between gap-4 rounded-xl border bg-card p-4">
      <div><h2 className="font-semibold">{club.name}</h2><p className="text-sm text-muted-foreground">{clubTypes.data?.find((type) => type.code === club.clubType)?.name ?? club.clubType} · {club.administrator ? "Administrator" : "Member"}</p></div>
      <Button type="button" disabled={selection.isPending} onClick={() => select(club.id)}>{selection.isPending ? "Switching…" : "Open club"}</Button>
    </li>)}</ul>
    {selection.error && <p role="alert" className="text-destructive">{errorMessage(selection.error)} Your club remains in the list; choose it again to retry.</p>}
    <CreateClubWizard onCreated={(club) => select(club.id)} />
  </main>
}

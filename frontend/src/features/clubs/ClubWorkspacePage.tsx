import { Link, useNavigate } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { errorMessage } from "@/features/auth/auth-api"
import { useClubs, useSelectClub, useClubContext } from "./club-hooks"
import { CreateClubWizard } from "./CreateClubWizard"

export function ClubWorkspacePage() {
  const clubs = useClubs()
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
    {clubs.error && <div role="alert"><p>{errorMessage(clubs.error)}</p><Button type="button" onClick={() => void clubs.refetch()}>Retry</Button></div>}
    {clubs.data?.length === 0 && <p>You do not belong to any clubs yet. Create one below to get started.</p>}
    <ul className="space-y-3">{clubs.data?.map((club) => <li key={club.id} className="flex items-center justify-between gap-4 rounded-xl border bg-card p-4">
      <div><h2 className="font-semibold">{club.name}</h2><p className="text-sm text-muted-foreground">Investment Club · {club.administrator ? "Administrator" : "Member"}</p></div>
      <Button type="button" disabled={selection.isPending} onClick={() => select(club.id)}>{selection.isPending ? "Switching…" : "Open club"}</Button>
    </li>)}</ul>
    {selection.error && <p role="alert" className="text-destructive">{errorMessage(selection.error)} Your club remains in the list; choose it again to retry.</p>}
    <CreateClubWizard onCreated={(club) => select(club.id)} />
  </main>
}

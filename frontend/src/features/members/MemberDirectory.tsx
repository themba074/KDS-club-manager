import { Search, UserRoundPlus, Users } from "lucide-react"
import { useState } from "react"

import { EmptyState } from "@/components/states/EmptyState"
import { LoadingState } from "@/components/states/LoadingState"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { errorMessage } from "@/features/auth/auth-api"
import { usePermission } from "@/features/roles/use-permission"
import { InviteMemberForm } from "./InviteMemberForm"
import { useMembers, type MemberStatus } from "./member-hooks"

export function MemberDirectory() {
  const [search, setSearch] = useState("")
  const [status, setStatus] = useState<MemberStatus | "ALL">("ALL")
  const [showInvitation, setShowInvitation] = useState(false)
  const members = useMembers(search, status)
  const canInvite = usePermission("MEMBERS_WRITE")

  return <section className="space-y-6">
    <header className="flex flex-wrap items-start justify-between gap-4">
      <div><h1 className="text-2xl font-semibold">Members</h1>
        <p className="mt-2 text-muted-foreground">View active members and invitations awaiting acceptance.</p></div>
      {canInvite && <Button onClick={() => setShowInvitation((visible) => !visible)}>
        <UserRoundPlus aria-hidden="true" />{showInvitation ? "Close invite form" : "Invite member"}
      </Button>}
    </header>
    {showInvitation && canInvite && <InviteMemberForm />}
    <div className="flex flex-col gap-3 sm:flex-row">
      <label className="relative flex-1"><span className="sr-only">Search members</span>
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
        <Input className="pl-9" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search by name, email, or phone" />
      </label>
      <label><span className="sr-only">Filter by status</span>
        <select className="h-9 rounded-lg border bg-background px-3 text-sm" value={status} onChange={(event) => setStatus(event.target.value as MemberStatus | "ALL")}>
          <option value="ALL">All statuses</option><option value="ACTIVE">Active</option><option value="INVITED">Invited</option>
        </select>
      </label>
    </div>
    {members.isPending && <LoadingState label="Loading member directory" />}
    {members.error && <div role="alert" className="rounded-xl border border-destructive/30 p-4"><p>{errorMessage(members.error)}</p><Button className="mt-3" variant="outline" onClick={() => void members.refetch()}>Retry</Button></div>}
    {members.data?.length === 0 && <EmptyState icon={Users} title="No matching members" description={search || status !== "ALL" ? "Try changing your search or status filter." : "Invite someone to start building your club directory."} />}
    {members.data && members.data.length > 0 && <div className="rounded-xl border bg-card">
      <Table><TableHeader><TableRow><TableHead>Name</TableHead><TableHead>Email</TableHead><TableHead>Phone</TableHead><TableHead>Role</TableHead><TableHead>Status</TableHead></TableRow></TableHeader>
        <TableBody>{members.data.map((member) => <TableRow key={`${member.status}-${member.id}`}>
          <TableCell className="font-medium">{[member.firstName, member.lastName].filter(Boolean).join(" ") || "Not provided"}</TableCell>
          <TableCell>{member.email}</TableCell><TableCell>{member.phone || "—"}</TableCell>
          <TableCell>{member.roleCode.charAt(0) + member.roleCode.slice(1).toLowerCase()}</TableCell>
          <TableCell><span className={member.status === "ACTIVE" ? "rounded-full bg-primary/10 px-2 py-1 text-xs font-medium text-primary" : "rounded-full bg-muted px-2 py-1 text-xs font-medium text-muted-foreground"}>{member.status === "ACTIVE" ? "Active" : "Invited"}</span></TableCell>
        </TableRow>)}</TableBody></Table>
    </div>}
  </section>
}

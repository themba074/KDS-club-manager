import { useState } from "react"
import { Button } from "@/components/ui/button"
import { errorMessage } from "@/features/auth/auth-api"
import { EmptyState } from "@/components/states/EmptyState"
import { ErrorState } from "@/components/states/ErrorState"
import { ShieldCheck, UserRoundCog } from "lucide-react"
import { usePermission } from "./use-permission"
import { useAssignRole, useRoleMembers, useRoles, type Role, type RoleMember } from "./role-hooks"

function Assignment({ member, roles }: { member: RoleMember; roles: Role[] }) {
  const [roleCode, setRoleCode] = useState(member.roleCode)
  const assignment = useAssignRole()
  return <li className="space-y-3 rounded-xl border bg-card p-4">
    <p className="font-medium break-all">{member.email}</p>
    <div className="flex flex-wrap items-center gap-3">
      <label className="text-sm">Role for {member.email}
        <select className="ml-2 rounded-lg border bg-background p-2" value={roleCode} disabled={assignment.isPending}
          onChange={(event) => setRoleCode(event.target.value)}>
          {roles.map((role) => <option key={role.code} value={role.code}>{role.name}</option>)}
        </select>
      </label>
      <Button type="button" disabled={assignment.isPending || roleCode === member.roleCode}
        onClick={() => assignment.mutate({ id: member.id, roleCode })}>{assignment.isPending ? "Saving…" : "Save role"}</Button>
    </div>
    {assignment.error && <p role="alert" className="text-sm text-destructive">{errorMessage(assignment.error)}</p>}
    {assignment.isSuccess && <p role="status" className="text-sm">Role saved.</p>}
  </li>
}

export function RoleManagement() {
  const roles = useRoles()
  const members = useRoleMembers()
  const canManage = usePermission("ROLES_MANAGE")
  if (roles.isPending) return <p role="status">Loading roles…</p>
  if (roles.error) return <ErrorState title="We couldn't load roles and permissions" description={errorMessage(roles.error, "Try loading the role catalogue again.")} onRetry={() => void roles.refetch()}/>
  return <section className="space-y-6">
    <header><h1 className="text-2xl font-semibold">Roles and permissions</h1>
      <p className="mt-2 text-muted-foreground">Roles group the actions a person can perform in this club.</p></header>
    {roles.data?.length === 0 && <EmptyState icon={ShieldCheck} title="No roles are configured" description="This club type does not currently provide a role catalogue. Ask an administrator to check the club template."/>}
    <div className="grid gap-4 md:grid-cols-2">{roles.data?.map((role) => <article key={role.code} className="rounded-xl border bg-card p-4">
      <h2 className="font-semibold">{role.name}</h2>
      <ul className="mt-2 space-y-1 text-sm text-muted-foreground">{role.permissions.map((permission) => <li key={permission}>{permission.replaceAll("_", " ").toLowerCase()}</li>)}</ul>
    </article>)}</div>
    {canManage && <section className="space-y-4"><h2 className="text-xl font-semibold">Member roles</h2>
      <p className="text-sm text-muted-foreground">Each member has one role. Keep at least one administrator. Invite new members from the Members page.</p>
      {members.isPending && <p role="status">Loading members…</p>}
      {members.error && <ErrorState title="We couldn't load member roles" description={errorMessage(members.error, "Try loading the member role list again.")} onRetry={() => void members.refetch()}/>}
      {members.data?.length === 0 && <EmptyState icon={UserRoundCog} title="No members are ready for role assignment" description="Invite members from the Members page. Their role can be reviewed here after they join."/>}
      <ul className="space-y-3">{members.data?.map((member) => <Assignment key={member.id + member.roleCode} member={member} roles={roles.data ?? []} />)}</ul>
    </section>}
  </section>
}

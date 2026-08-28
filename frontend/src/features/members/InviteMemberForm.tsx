import { useForm } from "react-hook-form"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { errorMessage } from "@/features/auth/auth-api"
import { useInviteMember, type InviteMemberInput } from "./member-hooks"

export function InviteMemberForm() {
  const invitation = useInviteMember()
  const { register, handleSubmit, reset, formState: { errors } } = useForm<InviteMemberInput>()

  return <section className="rounded-xl border bg-card p-5">
    <h2 className="text-lg font-semibold">Invite a member</h2>
    <p className="mt-1 text-sm text-muted-foreground">They will join as a Member. An administrator can change their role after acceptance.</p>
    <form className="mt-4 grid gap-4 md:grid-cols-2" onSubmit={handleSubmit((values) => invitation.mutate({
      ...values,
      email: values.email.trim(),
      firstName: values.firstName.trim(),
      lastName: values.lastName.trim(),
      phone: values.phone?.trim() || undefined,
    }, { onSuccess: () => reset() }))}>
      <label className="text-sm font-medium">First name
        <Input autoComplete="given-name" maxLength={80} {...register("firstName", { required: "First name is required" })} />
        {errors.firstName && <span className="mt-1 block text-destructive">{errors.firstName.message}</span>}
      </label>
      <label className="text-sm font-medium">Last name
        <Input autoComplete="family-name" maxLength={80} {...register("lastName", { required: "Last name is required" })} />
        {errors.lastName && <span className="mt-1 block text-destructive">{errors.lastName.message}</span>}
      </label>
      <label className="text-sm font-medium">Email
        <Input type="email" autoComplete="email" maxLength={320} {...register("email", { required: "Email is required" })} />
        {errors.email && <span className="mt-1 block text-destructive">{errors.email.message}</span>}
      </label>
      <label className="text-sm font-medium">Phone <span className="font-normal text-muted-foreground">(optional)</span>
        <Input type="tel" autoComplete="tel" maxLength={30} {...register("phone")} />
      </label>
      <div className="md:col-span-2">
        {invitation.error && <p role="alert" className="mb-3 text-sm text-destructive">{errorMessage(invitation.error)}</p>}
        {invitation.isSuccess && <p role="status" className="mb-3 text-sm">Invitation created. In development, the acceptance link is printed in the backend logs.</p>}
        <Button type="submit" disabled={invitation.isPending}>{invitation.isPending ? "Sending…" : "Send invitation"}</Button>
      </div>
    </form>
  </section>
}

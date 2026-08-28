import { useForm } from "react-hook-form"
import { Link, useNavigate, useSearchParams } from "react-router-dom"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { errorMessage } from "@/features/auth/auth-api"
import { useAcceptInvitation, useInvitationPreview } from "./member-hooks"

export function AcceptInvitationPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const token = params.get("token")
  const preview = useInvitationPreview(token)
  const acceptance = useAcceptInvitation()
  const { register, handleSubmit, formState: { errors } } = useForm<{ password: string }>()

  return <main className="grid min-h-screen place-items-center bg-muted p-4"><section className="w-full max-w-md rounded-2xl border bg-card p-6 shadow-sm">
    <p className="font-bold text-primary">KDS Club Manager</p><h1 className="mt-2 text-2xl font-semibold">Accept club invitation</h1>
    {!token && <div className="mt-5"><p role="alert">This invitation link is missing its token.</p><Link className="mt-4 block text-sm text-primary" to="/login">Go to login</Link></div>}
    {preview.isPending && token && <p role="status" className="mt-5">Checking invitation…</p>}
    {preview.error && <div className="mt-5"><p role="alert" className="text-destructive">{errorMessage(preview.error)}</p><Link className="mt-4 block text-sm text-primary" to="/login">Go to login</Link></div>}
    {preview.data && token && <form className="mt-5 space-y-4" onSubmit={handleSubmit(({ password }) => acceptance.mutate({ token, password: preview.data.accountExists ? undefined : password }, { onSuccess: () => navigate("/") }))}>
      <div className="rounded-xl bg-muted p-4"><p className="font-medium">{preview.data.clubName}</p><p className="mt-1 text-sm">Invited as {preview.data.firstName} {preview.data.lastName} ({preview.data.email})</p></div>
      {preview.data.accountExists ? <p className="text-sm text-muted-foreground">Accepting will link this club to your existing account and sign you in.</p> : <label className="block text-sm font-medium">Create a password
        <Input type="password" autoComplete="new-password" {...register("password", { required: "Password is required", minLength: { value: 8, message: "Use at least 8 characters" }, maxLength: 72 })} />
        {errors.password && <span role="alert" className="mt-1 block text-destructive">{errors.password.message}</span>}
      </label>}
      {acceptance.error && <p role="alert" className="text-sm text-destructive">{errorMessage(acceptance.error)}</p>}
      <Button className="w-full" type="submit" disabled={acceptance.isPending}>{acceptance.isPending ? "Joining club…" : "Accept invitation"}</Button>
    </form>}
  </section></main>
}

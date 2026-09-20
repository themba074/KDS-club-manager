import { useForm } from "react-hook-form"
import { Link, Navigate, useNavigate, useSearchParams } from "react-router-dom"
import { Landmark, ShieldCheck, UsersRound } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { errorMessage } from "./auth-api"
import { useAuthenticate, useConfirmPasswordReset, useRequestPasswordReset } from "./auth-hooks"
import { useAuthStore } from "./auth-store"

type Credentials = { email: string; password: string }

function Frame({ title, children }: { title: string; children: React.ReactNode }) {
  return <main className="grid min-h-screen bg-background lg:grid-cols-[minmax(0,1fr)_minmax(28rem,0.95fr)]">
    <section className="relative hidden overflow-hidden bg-sidebar p-10 text-sidebar-foreground lg:flex lg:flex-col lg:justify-between">
      <div className="absolute -right-24 -top-20 size-96 rounded-full border border-sidebar-foreground/10" />
      <div className="absolute -bottom-32 -left-24 size-96 rounded-full border border-sidebar-foreground/10" />
      <div className="relative flex items-center gap-3"><span className="grid size-12 place-items-center rounded-2xl bg-[#d9ad50] text-sidebar"><Landmark className="size-6" aria-hidden="true" /></span><span><span className="block text-2xl font-semibold tracking-tight">KDS</span><span className="text-sm text-sidebar-foreground/70">Club Manager</span></span></div>
      <div className="relative max-w-md"><p className="text-sm font-semibold uppercase tracking-[0.16em] text-[#d9ad50]">Club operations, in one place</p><h2 className="mt-5 font-heading text-5xl font-semibold leading-[1.05] tracking-[-0.045em]">Manage together.<br /><span className="text-[#d9ad50]">Move forward.</span></h2><p className="mt-6 text-base leading-7 text-sidebar-foreground/75">Keep your club’s members, meetings, contributions, documents, and decisions connected.</p></div>
      <div className="relative grid gap-4 text-sm"><div className="flex items-start gap-3"><ShieldCheck className="mt-0.5 size-5 shrink-0 text-[#d9ad50]" aria-hidden="true" /><span><strong className="block text-sidebar-foreground">Role-based access</strong><span className="text-sidebar-foreground/70">Members see only what their role allows.</span></span></div><div className="flex items-start gap-3"><UsersRound className="mt-0.5 size-5 shrink-0 text-[#d9ad50]" aria-hidden="true" /><span><strong className="block text-sidebar-foreground">Built for club teams</strong><span className="text-sidebar-foreground/70">Run shared work with clear accountability.</span></span></div></div>
    </section>
    <section className="grid place-items-center p-4 sm:p-8">
      <div className="w-full max-w-md rounded-2xl border border-border/80 bg-card p-6 shadow-[0_16px_40px_oklch(0.22_0.03_160/10%)] sm:p-8"><div className="mb-8"><div className="mb-6 flex items-center gap-2 lg:hidden"><span className="grid size-9 place-items-center rounded-xl bg-primary text-primary-foreground"><Landmark className="size-5" aria-hidden="true" /></span><span className="font-semibold text-primary">KDS Club Manager</span></div><p className="text-sm font-medium text-primary">Secure club access</p><h1 className="mt-2 font-heading text-3xl font-semibold tracking-[-0.035em]">{title}</h1></div>{children}</div>
    </section>
  </main>
}

export function CredentialsPage({ mode }: { mode: "login" | "register" }) {
  const mutation = useAuthenticate(mode)
  const navigate = useNavigate()
  const { register, handleSubmit, formState: { errors } } = useForm<Credentials>()
  if (useAuthStore.getState().accessToken) return <Navigate to="/" replace />
  return <Frame title={mode === "login" ? "Welcome back" : "Create your account"}>
    <form className="space-y-4" onSubmit={handleSubmit((values) => mutation.mutate(values, { onSuccess: () => navigate("/") }))}>
      <label className="block text-sm font-medium">Email<Input type="email" autoComplete="email" {...register("email", { required: "Email is required" })} /></label>
      {errors.email && <p className="text-sm text-destructive">{errors.email.message}</p>}
      <label className="block text-sm font-medium">Password<Input type="password" autoComplete={mode === "login" ? "current-password" : "new-password"} {...register("password", { required: "Password is required", minLength: mode === "register" ? { value: 8, message: "Use at least 8 characters" } : undefined })} /></label>
      {errors.password && <p className="text-sm text-destructive">{errors.password.message}</p>}
      {mutation.error && <p role="alert" className="text-sm text-destructive">{errorMessage(mutation.error)}</p>}
      <Button type="submit" className="w-full" disabled={mutation.isPending}>{mutation.isPending ? "Please wait…" : mode === "login" ? "Log in" : "Register"}</Button>
    </form>
    <div className="mt-5 flex justify-between text-sm"><Link to={mode === "login" ? "/register" : "/login"}>{mode === "login" ? "Create account" : "Already registered?"}</Link>{mode === "login" && <Link to="/forgot-password">Forgot password?</Link>}</div>
  </Frame>
}

export function ForgotPasswordPage() {
  const mutation = useRequestPasswordReset(); const { register, handleSubmit } = useForm<{ email: string }>()
  return <Frame title="Reset your password"><form className="space-y-4" onSubmit={handleSubmit(({ email }) => mutation.mutate(email))}><label className="block text-sm font-medium">Email<Input type="email" {...register("email", { required: true })} /></label>{mutation.isSuccess ? <p>Check your inbox if an account exists for that email.</p> : <Button type="submit" className="w-full">Send reset instructions</Button>}</form><Link className="mt-4 block text-sm" to="/login">Back to login</Link></Frame>
}

export function ResetPasswordPage() {
  const [params] = useSearchParams(); const navigate = useNavigate(); const mutation = useConfirmPasswordReset(); const { register, handleSubmit, formState: { errors } } = useForm<{ password: string }>()
  const token = params.get("token")
  if (!token) return <Frame title="Invalid reset link"><Link to="/forgot-password">Request a new link</Link></Frame>
  return <Frame title="Choose a new password"><form className="space-y-4" onSubmit={handleSubmit(({ password }) => mutation.mutate({ token, newPassword: password }, { onSuccess: () => navigate("/login") }))}><label className="block text-sm font-medium">New password<Input type="password" {...register("password", { required: "Password is required", minLength: { value: 8, message: "Use at least 8 characters" } })} /></label>{errors.password && <p role="alert" className="text-sm text-destructive">{errors.password.message}</p>}{mutation.error && <p role="alert" className="text-sm text-destructive">{errorMessage(mutation.error)}</p>}<Button type="submit" className="w-full">Reset password</Button></form></Frame>
}

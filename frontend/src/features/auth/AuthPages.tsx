import { useForm } from "react-hook-form"
import { Link, Navigate, useNavigate, useSearchParams } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { errorMessage } from "./auth-api"
import { useAuthenticate, useConfirmPasswordReset, useRequestPasswordReset } from "./auth-hooks"
import { useAuthStore } from "./auth-store"

type Credentials = { email: string; password: string }

function Frame({ title, children }: { title: string; children: React.ReactNode }) {
  return <main className="grid min-h-screen place-items-center bg-muted p-4"><section className="w-full max-w-md rounded-2xl border bg-card p-6 shadow-sm"><div className="mb-6"><p className="font-bold text-primary">KDS Club Manager</p><h1 className="mt-2 text-2xl font-semibold">{title}</h1></div>{children}</section></main>
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
      <label className="block text-sm font-medium">Password<Input type="password" autoComplete={mode === "login" ? "current-password" : "new-password"} {...register("password", { required: "Password is required", minLength: mode === "register" ? { value: 12, message: "Use at least 12 characters" } : undefined })} /></label>
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
  const [params] = useSearchParams(); const navigate = useNavigate(); const mutation = useConfirmPasswordReset(); const { register, handleSubmit } = useForm<{ password: string }>()
  const token = params.get("token")
  if (!token) return <Frame title="Invalid reset link"><Link to="/forgot-password">Request a new link</Link></Frame>
  return <Frame title="Choose a new password"><form className="space-y-4" onSubmit={handleSubmit(({ password }) => mutation.mutate({ token, newPassword: password }, { onSuccess: () => navigate("/login") }))}><label className="block text-sm font-medium">New password<Input type="password" {...register("password", { required: true, minLength: 12 })} /></label>{mutation.error && <p role="alert" className="text-sm text-destructive">{errorMessage(mutation.error)}</p>}<Button type="submit" className="w-full">Reset password</Button></form></Frame>
}

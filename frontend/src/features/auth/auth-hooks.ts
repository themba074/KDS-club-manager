import { useMutation } from "@tanstack/react-query"
import { api, type AuthResponse } from "./auth-api"
import { useAuthStore } from "./auth-store"

export function useAuthenticate(path: "login" | "register") {
  const setSession = useAuthStore((state) => state.setSession)
  return useMutation({
    mutationFn: (input: { email: string; password: string }) =>
      api.post<AuthResponse>(`/auth/${path}`, input).then(({ data }) => data),
    onSuccess: (session) => setSession(session.accessToken, session.user),
  })
}

export function useRequestPasswordReset() {
  return useMutation({ mutationFn: (email: string) => api.post("/auth/password-reset/request", { email }) })
}

export function useConfirmPasswordReset() {
  return useMutation({ mutationFn: (input: { token: string; newPassword: string }) => api.post("/auth/password-reset/confirm", input) })
}

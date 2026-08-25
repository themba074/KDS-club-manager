import { create } from "zustand"

export type AuthUser = { id: string; email: string }

type AuthState = {
  accessToken: string | null
  user: AuthUser | null
  initialized: boolean
  setSession: (accessToken: string, user: AuthUser) => void
  clearSession: () => void
  setInitialized: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  user: null,
  initialized: false,
  setSession: (accessToken, user) => set({ accessToken, user, initialized: true }),
  clearSession: () => set({ accessToken: null, user: null, initialized: true }),
  setInitialized: () => set({ initialized: true }),
}))

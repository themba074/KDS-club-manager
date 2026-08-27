import { create } from "zustand"

export type AuthUser = { id: string; email: string }
export type ClubSummary = { id: string; name: string; clubType: "INVESTMENT_CLUB"; administrator: boolean }

type AuthState = {
  accessToken: string | null
  user: AuthUser | null
  initialized: boolean
  activeClub: ClubSummary | null
  switchingClub: boolean
  sessionVersion: number
  beginClubSwitch: () => number
  endClubSwitch: () => void
  setSession: (accessToken: string, user: AuthUser, activeClub?: ClubSummary | null) => void
  clearSession: () => void
  setInitialized: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  user: null,
  initialized: false,
  activeClub: null,
  switchingClub: false,
  sessionVersion: 0,
  beginClubSwitch: () => {
    let version = 0
    set((state) => { version = state.sessionVersion + 1; return { sessionVersion: version, switchingClub: true } })
    return version
  },
  endClubSwitch: () => set({ switchingClub: false }),
  setSession: (accessToken, user, activeClub = null) => set({ accessToken, user, activeClub, initialized: true }),
  clearSession: () => set((state) => ({ accessToken: null, user: null, activeClub: null, switchingClub: false, initialized: true, sessionVersion: state.sessionVersion + 1 })),
  setInitialized: () => set({ initialized: true }),
}))

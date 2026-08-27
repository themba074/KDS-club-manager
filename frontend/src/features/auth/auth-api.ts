import axios, { AxiosError, CanceledError, type InternalAxiosRequestConfig } from "axios"
import { useAuthStore, type AuthUser, type ClubSummary } from "./auth-store"

export type AuthResponse = { accessToken: string; expiresInSeconds: number; user: AuthUser; activeClub: ClubSummary | null }
export type Problem = { detail?: string; errors?: { field: string; message: string }[] }
type ScopedRequest = InternalAxiosRequestConfig & { retried?: boolean; sessionVersion?: number; clubId?: string | null }

export const api = axios.create({ baseURL: "/api/v1", withCredentials: true })
let refreshRequest: Promise<AuthResponse> | null = null
let sessionQueue: Promise<unknown> = Promise.resolve()

function serialized<T>(operation: () => Promise<T>): Promise<T> {
  const result = sessionQueue.then(operation, operation)
  sessionQueue = result.catch(() => undefined)
  return result
}

function applySession(session: AuthResponse, version: number) {
  if (useAuthStore.getState().sessionVersion !== version) throw new CanceledError("Session changed")
  useAuthStore.getState().setSession(session.accessToken, session.user, session.activeClub)
  return session
}

export function refreshSession() {
  if (!refreshRequest) {
    const version = useAuthStore.getState().sessionVersion
    const operation = serialized(async () => {
      if (useAuthStore.getState().sessionVersion !== version) throw new CanceledError("Session changed")
      return applySession((await api.post<AuthResponse>("/auth/refresh")).data, version)
    })
    refreshRequest = operation.finally(() => { refreshRequest = null })
  }
  return refreshRequest
}

// Serialize club switching with refresh so their Set-Cookie responses cannot race within a tab.
export function selectClubSession(clubId: string, version: number) {
  return serialized(async () => {
    if (useAuthStore.getState().sessionVersion !== version) throw new CanceledError("Session changed")
    applySession((await api.post<AuthResponse>("/auth/refresh")).data, version)
    return applySession((await api.post<AuthResponse>("/auth/select-club", { clubId })).data, version)
  })
}

api.interceptors.request.use((config: ScopedRequest) => {
  const state = useAuthStore.getState()
  const authenticationRequest = config.url?.startsWith("/auth/")
  if (state.switchingClub && !authenticationRequest) throw new CanceledError("Club is changing")
  config.sessionVersion ??= state.sessionVersion
  if (config.sessionVersion !== state.sessionVersion) throw new CanceledError("Session changed")
  config.clubId = state.activeClub?.id ?? null
  if (state.accessToken && (!authenticationRequest || config.url === "/auth/select-club")) {
    config.headers.Authorization = `Bearer ${state.accessToken}`
  }
  return config
})

api.interceptors.response.use((response) => {
  const request = response.config as ScopedRequest
  if (request.sessionVersion !== useAuthStore.getState().sessionVersion) throw new CanceledError("Session changed")
  return response
}, async (error: AxiosError) => {
  const request = error.config as ScopedRequest | undefined
  if (request && request.sessionVersion !== useAuthStore.getState().sessionVersion) throw new CanceledError("Session changed")
  if (!request || request.retried || error.response?.status !== 401 || request.url?.startsWith("/auth/")) {
    throw error
  }
  request.retried = true
  try {
    const session = await refreshSession()
    if (request.clubId !== (session.activeClub?.id ?? null)) throw new CanceledError("Active club changed")
    request.headers.Authorization = `Bearer ${session.accessToken}`
    return api(request)
  } catch (refreshError) {
    if (!axios.isCancel(refreshError)) useAuthStore.getState().clearSession()
    throw refreshError
  }
})

export function errorMessage(error: unknown) {
  return axios.isAxiosError<Problem>(error) ? error.response?.data.detail ?? "Something went wrong." : "Something went wrong."
}

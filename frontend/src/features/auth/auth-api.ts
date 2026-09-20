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

const statusMessages: Record<number, string> = {
  400: "Check the information you entered and try again.",
  401: "Your session has expired. Sign in again to continue.",
  403: "You do not have permission to perform this action.",
  404: "The requested information could not be found.",
  409: "This information changed while you were working. Refresh and try again.",
  413: "That file is too large. Choose a smaller file and try again.",
  429: "There have been too many attempts. Wait a moment and try again.",
}

function safeProblemDetail(detail: unknown) {
  if (typeof detail !== "string") return null
  const value = detail.trim()
  if (!value || value.length > 240 || /[\r\n]|exception|stack trace|\bat\s+[\w.$]+\(/i.test(value)) return null
  return value
}

export function errorMessage(error: unknown, fallback = "We couldn't complete that request. Please try again.") {
  if (!axios.isAxiosError<Problem>(error)) return fallback
  const status = error.response?.status
  const detail = status && status < 500 ? safeProblemDetail(error.response?.data.detail) : null
  if (detail) return detail
  if (status && statusMessages[status]) return statusMessages[status]
  if (status && status >= 500) return "The service is temporarily unavailable. Please try again shortly."
  if (!error.response) return "We couldn't reach the service. Check your connection and try again."
  return fallback
}

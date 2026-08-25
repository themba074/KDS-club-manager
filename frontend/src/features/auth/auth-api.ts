import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios"
import { useAuthStore, type AuthUser } from "./auth-store"

export type AuthResponse = { accessToken: string; expiresInSeconds: number; user: AuthUser }
export type Problem = { detail?: string; errors?: { field: string; message: string }[] }

export const api = axios.create({ baseURL: "/api/v1", withCredentials: true })
let refreshRequest: Promise<AuthResponse> | null = null

export function refreshSession() {
  refreshRequest ??= api.post<AuthResponse>("/auth/refresh").then(({ data }) => data)
  return refreshRequest.finally(() => { refreshRequest = null })
}

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(undefined, async (error: AxiosError) => {
  const request = error.config as (InternalAxiosRequestConfig & { retried?: boolean }) | undefined
  if (!request || request.retried || error.response?.status !== 401 || request.url?.startsWith("/auth/")) {
    throw error
  }
  request.retried = true
  try {
    const session = await refreshSession()
    useAuthStore.getState().setSession(session.accessToken, session.user)
    request.headers.Authorization = `Bearer ${session.accessToken}`
    return api(request)
  } catch (refreshError) {
    useAuthStore.getState().clearSession()
    throw refreshError
  }
})

export function errorMessage(error: unknown) {
  return axios.isAxiosError<Problem>(error) ? error.response?.data.detail ?? "Something went wrong." : "Something went wrong."
}

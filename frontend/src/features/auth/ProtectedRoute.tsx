import { useEffect } from "react"
import { Navigate, Outlet } from "react-router-dom"
import { LoadingState } from "@/components/states/LoadingState"
import { refreshSession } from "./auth-api"
import { useAuthStore } from "./auth-store"

export function ProtectedRoute() {
  const { accessToken, initialized, setInitialized } = useAuthStore()
  useEffect(() => {
    if (initialized) return
    refreshSession().catch(setInitialized)
  }, [initialized, setInitialized])
  if (!initialized) return <LoadingState label="Restoring your session" />
  return accessToken ? <Outlet /> : <Navigate to="/login" replace />
}

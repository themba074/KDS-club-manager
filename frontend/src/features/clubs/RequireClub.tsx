import { Navigate, Outlet } from "react-router-dom"
import { useAuthStore } from "@/features/auth/auth-store"
import { LoadingState } from "@/components/states/LoadingState"

export function RequireClub() {
  const activeClub = useAuthStore((state) => state.activeClub)
  const switching = useAuthStore((state) => state.switchingClub)
  if (switching) return <LoadingState label="Switching club…" />
  return activeClub ? <Outlet key={activeClub.id} /> : <Navigate to="/clubs" replace />
}

import { useAuthStore } from "@/features/auth/auth-store"
export function usePermission(permission: string) {
  return useAuthStore((state) => state.activeClub?.permissions?.includes(permission) ?? false)
}

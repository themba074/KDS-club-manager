import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { api, refreshSession } from "@/features/auth/auth-api"
import { useAuthStore } from "@/features/auth/auth-store"
import { usePermission } from "./use-permission"

export type Role = { code: string; name: string; permissions: string[] }
export type RoleMember = { id: string; email: string; roleCode: string }

export function useRoles() {
  const clubId = useAuthStore((state) => state.activeClub?.id)
  const allowed = usePermission("ROLES_READ")
  return useQuery({ queryKey: ["roles", clubId], enabled: Boolean(clubId) && allowed,
    queryFn: ({ signal }) => api.get<Role[]>("/roles", { signal }).then(({ data }) => data) })
}
export function useRoleMembers() {
  const clubId = useAuthStore((state) => state.activeClub?.id)
  const allowed = usePermission("ROLES_MANAGE")
  return useQuery({ queryKey: ["role-members", clubId], enabled: Boolean(clubId) && allowed,
    queryFn: ({ signal }) => api.get<RoleMember[]>("/role-members", { signal }).then(({ data }) => data) })
}
export function useAssignRole() {
  const client = useQueryClient()
  const clubId = useAuthStore((state) => state.activeClub?.id)
  return useMutation({
    mutationFn: ({ id, roleCode }: { id: string; roleCode: string }) => api.put("/role-members/" + id, { roleCode }),
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: ["role-members", clubId] })
      // A self-demotion updates navigation and controls as well as the server checks.
      await refreshSession()
    },
  })
}

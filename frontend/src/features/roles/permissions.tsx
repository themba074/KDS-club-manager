import type { ReactNode } from "react"
import { usePermission } from "./use-permission"
export function PermissionGate({ permission, children }: { permission: string; children: ReactNode }) {
  const allowed = usePermission(permission)
  return allowed ? children : <p role="alert">You do not have permission to view this page.</p>
}

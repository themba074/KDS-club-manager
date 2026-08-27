import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { beforeEach, expect, it, vi } from "vitest"
import { useAuthStore } from "@/features/auth/auth-store"
import { RoleManagement } from "./RoleManagement"
import { PermissionGate } from "./permissions"

const { get, put, refresh } = vi.hoisted(() => ({ get: vi.fn(), put: vi.fn(), refresh: vi.fn() }))
vi.mock("@/features/auth/auth-api", () => ({
  api: { get, put }, refreshSession: refresh, errorMessage: () => "Assignment denied",
}))
const roles = [
  { code: "ADMINISTRATOR", name: "Administrator", permissions: ["ROLES_READ", "ROLES_MANAGE"] },
  { code: "MEMBER", name: "Member", permissions: ["MEMBERS_READ"] },
]
beforeEach(() => {
  vi.clearAllMocks()
  useAuthStore.getState().setSession("token", { id: "user", email: "owner@example.com" },
    { id: "club", name: "Club", clubType: "INVESTMENT_CLUB", administrator: true, permissions: ["ROLES_READ", "ROLES_MANAGE"] })
  get.mockImplementation((path: string) => Promise.resolve({ data: path === "/roles" ? roles :
    [{ id: "membership", email: "owner@example.com", roleCode: "ADMINISTRATOR" }] }))
  put.mockResolvedValue({})
  refresh.mockResolvedValue({})
})
function page() {
  render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })}>
    <PermissionGate permission="ROLES_READ"><RoleManagement /></PermissionGate>
  </QueryClientProvider>)
}
it("submits a selected role and refreshes session permissions", async () => {
  page()
  fireEvent.change(await screen.findByRole("combobox"), { target: { value: "MEMBER" } })
  fireEvent.click(screen.getByRole("button", { name: "Save role" }))
  await waitFor(() => expect(put).toHaveBeenCalledWith("/role-members/membership", { roleCode: "MEMBER" }))
  await waitFor(() => expect(refresh).toHaveBeenCalled())
})
it("shows read-only roles without requesting the member list", async () => {
  useAuthStore.setState((state) => ({ activeClub: { ...state.activeClub!, permissions: ["ROLES_READ"] } }))
  page()
  expect(await screen.findByRole("heading", { name: "Administrator" })).toBeInTheDocument()
  expect(screen.queryByRole("combobox")).not.toBeInTheDocument()
  expect(get.mock.calls.some(([path]) => path === "/role-members")).toBe(false)
})
it("does not trust the administrator display flag for access", () => {
  useAuthStore.setState((state) => ({ activeClub: { ...state.activeClub!, permissions: [] } }))
  page()
  expect(screen.getByRole("alert")).toHaveTextContent("You do not have permission")
  expect(get).not.toHaveBeenCalled()
})
it("displays a denied assignment without reporting success", async () => {
  put.mockRejectedValue(new Error("denied"))
  page()
  fireEvent.change(await screen.findByRole("combobox"), { target: { value: "MEMBER" } })
  fireEvent.click(screen.getByRole("button", { name: "Save role" }))
  expect(await screen.findByRole("alert")).toHaveTextContent("Assignment denied")
  expect(refresh).not.toHaveBeenCalled()
})

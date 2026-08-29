import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { beforeEach, expect, it, vi } from "vitest"

import { useAuthStore } from "@/features/auth/auth-store"
import { MemberDirectory } from "./MemberDirectory"

const { get, post, patch } = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn(), patch: vi.fn() }))
vi.mock("@/features/auth/auth-api", () => ({ api: { get, post, patch }, errorMessage: () => "Request failed" }))

beforeEach(() => {
  vi.clearAllMocks()
  useAuthStore.getState().setSession("token", { id: "owner", email: "owner@example.test" }, {
    id: "club", name: "Club", clubType: "INVESTMENT_CLUB", administrator: true,
    permissions: ["MEMBERS_READ", "MEMBERS_WRITE"],
  })
  get.mockResolvedValue({ data: [{ id: "member", email: "member@example.test", firstName: "Thandi", lastName: "Ndlovu", phone: null, roleCode: "MEMBER", status: "ACTIVE", joinedOrInvitedAt: "2026-08-28T00:00:00Z" }] })
  post.mockResolvedValue({ data: { id: "invite", status: "INVITED" } })
  patch.mockResolvedValue({ data: undefined })
})

function page() {
  render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })}>
    <MemberDirectory />
  </QueryClientProvider>)
}

it("lists members and submits a normalized invitation", async () => {
  page()
  expect(await screen.findByText("Thandi Ndlovu")).toBeInTheDocument()
  fireEvent.click(screen.getByRole("button", { name: "Invite member" }))
  fireEvent.change(screen.getByLabelText("First name"), { target: { value: " Sipho " } })
  fireEvent.change(screen.getByLabelText("Last name"), { target: { value: " Dlamini " } })
  fireEvent.change(screen.getByLabelText("Email"), { target: { value: " sipho@example.test " } })
  fireEvent.click(screen.getByRole("button", { name: "Send invitation" }))
  await waitFor(() => expect(post).toHaveBeenCalledWith("/member-invitations", {
    email: "sipho@example.test", firstName: "Sipho", lastName: "Dlamini", phone: undefined,
  }))
  expect(await screen.findByRole("status")).toHaveTextContent("Invitation created")
})

it("keeps invitation controls hidden without write permission", async () => {
  useAuthStore.setState((state) => ({ activeClub: { ...state.activeClub!, permissions: ["MEMBERS_READ"] } }))
  page()
  expect(await screen.findByText("member@example.test")).toBeInTheDocument()
  expect(screen.queryByRole("button", { name: "Invite member" })).not.toBeInTheDocument()
})

it("changes an active membership status", async () => {
  page()
  await screen.findByText("Thandi Ndlovu")
  fireEvent.change(screen.getByLabelText("Change status for member@example.test"), { target: { value: "SUSPENDED" } })
  await waitFor(() => expect(patch).toHaveBeenCalledWith("/members/member/status", { status: "SUSPENDED" }))
})

it("inspects previews and confirms a bulk import", async () => {
  post.mockImplementation((url: string) => {
    if (url === "/member-imports/inspect") return Promise.resolve({ data: { headers: ["Email", "First", "Last"], rowCount: 1, sampleRows: [] } })
    if (url === "/member-imports/preview") return Promise.resolve({ data: { totalRows: 1, readyRows: 1, invalidRows: 0, rows: [
      { rowNumber: 2, email: "bulk@example.test", firstName: "Bulk", lastName: "Member", phone: null, status: "READY", errors: [] },
    ] } })
    if (url === "/member-imports/confirm") return Promise.resolve({ data: { totalRows: 1, invitedRows: 1, failedRows: 0, rows: [
      { rowNumber: 2, email: "bulk@example.test", firstName: "Bulk", lastName: "Member", phone: null, status: "INVITED", errors: [] },
    ] } })
    return Promise.resolve({ data: { id: "invite", status: "INVITED" } })
  })
  page()
  await screen.findByText("Thandi Ndlovu")
  fireEvent.click(screen.getByRole("button", { name: "Bulk import" }))
  fireEvent.change(screen.getByLabelText("CSV file"), { target: { files: [new File(["Email,First,Last\nbulk@example.test,Bulk,Member"], "members.csv", { type: "text/csv" })] } })
  fireEvent.click(screen.getByRole("button", { name: "Read columns" }))
  expect(await screen.findByText("Found 1 data row. Match the required fields below.")).toBeInTheDocument()
  fireEvent.click(screen.getByRole("button", { name: "Preview import" }))
  expect(await screen.findByText("1 ready · 0 invalid")).toBeInTheDocument()
  fireEvent.click(screen.getByRole("button", { name: "Import 1 valid row" }))
  expect(await screen.findByRole("status")).toHaveTextContent("Created 1 invitation")
  expect(post).toHaveBeenCalledWith("/member-imports/confirm", expect.any(FormData))
})

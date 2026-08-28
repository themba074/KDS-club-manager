import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { beforeEach, expect, it, vi } from "vitest"

import { useAuthStore } from "@/features/auth/auth-store"
import { MemberDirectory } from "./MemberDirectory"

const { get, post } = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn() }))
vi.mock("@/features/auth/auth-api", () => ({ api: { get, post }, errorMessage: () => "Request failed" }))

beforeEach(() => {
  vi.clearAllMocks()
  useAuthStore.getState().setSession("token", { id: "owner", email: "owner@example.test" }, {
    id: "club", name: "Club", clubType: "INVESTMENT_CLUB", administrator: true,
    permissions: ["MEMBERS_READ", "MEMBERS_WRITE"],
  })
  get.mockResolvedValue({ data: [{ id: "member", email: "member@example.test", firstName: "Thandi", lastName: "Ndlovu", phone: null, roleCode: "MEMBER", status: "ACTIVE", joinedOrInvitedAt: "2026-08-28T00:00:00Z" }] })
  post.mockResolvedValue({ data: { id: "invite", status: "INVITED" } })
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

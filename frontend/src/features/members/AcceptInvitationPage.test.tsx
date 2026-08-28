import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { MemoryRouter, Route, Routes } from "react-router-dom"
import { beforeEach, expect, it, vi } from "vitest"

import { useAuthStore } from "@/features/auth/auth-store"
import { AcceptInvitationPage } from "./AcceptInvitationPage"

const { get, post } = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn() }))
vi.mock("@/features/auth/auth-api", () => ({ api: { get, post }, errorMessage: () => "Invitation unavailable" }))

beforeEach(() => {
  vi.clearAllMocks()
  useAuthStore.getState().clearSession()
  get.mockResolvedValue({ data: { clubName: "Ubuntu Investors", email: "new@example.test", firstName: "New", lastName: "Member", roleCode: "MEMBER", accountExists: false, expiresAt: "2026-09-04T00:00:00Z" } })
  post.mockResolvedValue({ data: {
    accessToken: "accepted-token", expiresInSeconds: 900, user: { id: "new-user", email: "new@example.test" },
    activeClub: { id: "club", name: "Ubuntu Investors", clubType: "INVESTMENT_CLUB", administrator: false, permissions: ["MEMBERS_READ"] },
  } })
})

it("creates an invited account, stores its selected-club session, and enters the app", async () => {
  render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })}>
    <MemoryRouter initialEntries={["/accept-invitation?token=raw-token"]}><Routes>
      <Route path="accept-invitation" element={<AcceptInvitationPage />} />
      <Route path="/" element={<p>Club workspace</p>} />
    </Routes></MemoryRouter>
  </QueryClientProvider>)
  expect(await screen.findByText("Ubuntu Investors")).toBeInTheDocument()
  fireEvent.change(screen.getByLabelText("Create a password"), { target: { value: "secure-password" } })
  fireEvent.click(screen.getByRole("button", { name: "Accept invitation" }))
  await waitFor(() => expect(post).toHaveBeenCalledWith("/member-invitations/accept", { token: "raw-token", password: "secure-password" }))
  expect(await screen.findByText("Club workspace")).toBeInTheDocument()
  expect(useAuthStore.getState().activeClub?.id).toBe("club")
})

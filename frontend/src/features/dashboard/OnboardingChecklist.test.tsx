import { fireEvent, render, screen } from "@testing-library/react"
import { MemoryRouter } from "react-router-dom"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { useAuthStore } from "@/features/auth/auth-store"
import { OnboardingChecklist } from "./OnboardingChecklist"

vi.mock("@/features/clubs/club-hooks", () => ({
  useClubTypes: () => ({ data: [{ code: "INVESTMENT_CLUB", enabledModules: ["MEMBERS", "CONTRIBUTIONS", "MEETINGS"] }] }),
}))

describe("OnboardingChecklist", () => {
  beforeEach(() => {
    window.localStorage.clear()
    useAuthStore.setState({ user: { id: "user-1", email: "admin@example.test" }, activeClub: { id: "club-1", name: "Pilot Club", clubType: "INVESTMENT_CLUB", administrator: true, permissions: ["MEMBERS_WRITE", "CONTRIBUTIONS_WRITE", "MEETINGS_WRITE", "VOTES_CREATE"] } })
  })

  it("shows only actions enabled for the active template and administrator", () => {
    render(<MemoryRouter><OnboardingChecklist /></MemoryRouter>)
    expect(screen.getByRole("heading", { name: "Set up your club" })).toBeInTheDocument()
    expect(screen.getByText("Build your member list")).toBeInTheDocument()
    expect(screen.getByText("Set contribution expectations")).toBeInTheDocument()
    expect(screen.getByText("Plan the next meeting")).toBeInTheDocument()
    expect(screen.queryByText("Prepare club decisions")).not.toBeInTheDocument()
  })

  it("persists progress separately for the active user and club", () => {
    const { unmount } = render(<MemoryRouter><OnboardingChecklist /></MemoryRouter>)
    fireEvent.click(screen.getByRole("button", { name: "Mark complete: Build your member list" }))
    expect(screen.getByText("1 of 3 complete")).toBeInTheDocument()
    expect(window.localStorage.getItem("kds:onboarding:user-1:club-1")).toContain("members")
    unmount()
    render(<MemoryRouter><OnboardingChecklist /></MemoryRouter>)
    expect(screen.getByRole("button", { name: "Mark incomplete: Build your member list" })).toBeInTheDocument()
  })

  it("stays hidden for non-administrators", () => {
    useAuthStore.setState((state) => ({ activeClub: state.activeClub ? { ...state.activeClub, administrator: false } : null }))
    render(<MemoryRouter><OnboardingChecklist /></MemoryRouter>)
    expect(screen.queryByRole("heading", { name: "Set up your club" })).not.toBeInTheDocument()
  })
})

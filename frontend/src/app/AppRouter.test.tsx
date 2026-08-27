import { fireEvent, render, screen } from "@testing-library/react"
import { beforeEach, describe, expect, it } from "vitest"

import { AppRouter } from "@/app/AppRouter"
import { useAuthStore } from "@/features/auth/auth-store"

describe("AppRouter", () => {
  beforeEach(() => {
    window.history.replaceState({}, "", "/")
    useAuthStore.setState({ accessToken: "test-token", user: { id: "user-1", email: "member@example.com" }, initialized: true,
      activeClub: { id: "club-1", name: "Ubuntu Investment Club", clubType: "INVESTMENT_CLUB", administrator: true, permissions: ["MEMBERS_READ", "CONTRIBUTIONS_READ"] }, switchingClub: false })
  })

  it("renders the application shell and dashboard route", () => {
    render(<AppRouter />)

    expect(screen.getByRole("navigation", { name: "Main navigation" })).toBeInTheDocument()
    expect(screen.getByRole("heading", { name: "Dashboard" })).toBeInTheDocument()
    expect(screen.getByText("Ubuntu Investment Club")).toBeInTheDocument()
  })

  it("navigates between feature routes without a page reload", () => {
    render(<AppRouter />)

    fireEvent.click(screen.getByRole("link", { name: "Members" }))
    expect(screen.getByRole("heading", { name: "Members" })).toBeInTheDocument()

    fireEvent.click(screen.getByRole("link", { name: "Contributions" }))
    expect(screen.getByRole("heading", { name: "Contributions" })).toBeInTheDocument()
  })

  it("opens and closes the mobile navigation", () => {
    render(<AppRouter />)

    fireEvent.click(screen.getByRole("button", { name: "Open navigation" }))
    expect(screen.getAllByRole("navigation", { name: "Main navigation" })).toHaveLength(2)

    fireEvent.click(screen.getByRole("button", { name: "Close navigation" }))
    expect(screen.getAllByRole("navigation", { name: "Main navigation" })).toHaveLength(1)
  })

  it("renders the not-found state for an unknown route", () => {
    window.history.replaceState({}, "", "/does-not-exist")

    render(<AppRouter />)

    expect(screen.getByText("Page not found")).toBeInTheDocument()
    expect(screen.getByRole("link", { name: "Return to dashboard" })).toBeInTheDocument()
  })
})

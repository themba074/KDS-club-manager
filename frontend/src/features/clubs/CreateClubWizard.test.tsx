import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { api } from "@/features/auth/auth-api"
import { useAuthStore } from "@/features/auth/auth-store"
import { CreateClubWizard } from "./CreateClubWizard"

afterEach(() => vi.restoreAllMocks())
beforeEach(() => useAuthStore.getState().setSession("token", { id: "owner", email: "owner@example.test" }))

describe("club creation wizard", () => {
  it("reviews the name before creating and reports success", async () => {
    const club = { id: "club-1", name: "Savings", clubType: "INVESTMENT_CLUB", administrator: true }
    const post = vi.spyOn(api, "post").mockResolvedValue({ data: club })
    vi.spyOn(api, "get").mockResolvedValue({ data: [{ code: "INVESTMENT_CLUB", name: "Investment Club", enabledModules: [] }] })
    const onCreated = vi.fn()
    render(<QueryClientProvider client={new QueryClient()}><CreateClubWizard onCreated={onCreated} /></QueryClientProvider>)
    await screen.findByRole("option", { name: "Investment Club" })
    fireEvent.change(screen.getByLabelText("Club name"), { target: { value: "Savings" } })
    fireEvent.click(screen.getByRole("button", { name: "Review club" }))
    expect(await screen.findByText("Step 2 of 2")).toBeInTheDocument()
    expect(post).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole("button", { name: "Create club" }))
    await waitFor(() => expect(post).toHaveBeenCalledWith("/clubs", { name: "Savings", clubType: "INVESTMENT_CLUB" }))
    await waitFor(() => expect(onCreated).toHaveBeenCalledWith(club))
  })

  it("does not advance with a blank name", async () => {
    vi.spyOn(api, "get").mockResolvedValue({ data: [{ code: "INVESTMENT_CLUB", name: "Investment Club", enabledModules: [] }] })
    render(<QueryClientProvider client={new QueryClient()}><CreateClubWizard onCreated={vi.fn()} /></QueryClientProvider>)
    await screen.findByRole("option", { name: "Investment Club" })
    fireEvent.click(screen.getByRole("button", { name: "Review club" }))
    expect(await screen.findByRole("alert")).toHaveTextContent("Enter a club name")
  })

  it("creates the selected second club type", async () => {
    const club = { id: "club-2", name: "Riverside FC", clubType: "SPORTS_CLUB", administrator: true }
    const post = vi.spyOn(api, "post").mockResolvedValue({ data: club })
    vi.spyOn(api, "get").mockResolvedValue({ data: [
      { code: "INVESTMENT_CLUB", name: "Investment Club", enabledModules: [] },
      { code: "SPORTS_CLUB", name: "Sports Club", enabledModules: ["MEMBERS", "MEETINGS"] },
    ] })
    render(<QueryClientProvider client={new QueryClient()}><CreateClubWizard onCreated={vi.fn()} /></QueryClientProvider>)
    await screen.findByRole("option", { name: "Sports Club" })
    fireEvent.change(screen.getByLabelText("Club name"), { target: { value: "Riverside FC" } })
    fireEvent.change(screen.getByLabelText("Club type"), { target: { value: "SPORTS_CLUB" } })
    fireEvent.click(screen.getByRole("button", { name: "Review club" }))
    expect(await screen.findByText("Step 2 of 2")).toBeInTheDocument()
    expect(screen.getByText("Sports Club")).toBeInTheDocument()
    fireEvent.click(screen.getByRole("button", { name: "Create club" }))
    await waitFor(() => expect(post).toHaveBeenCalledWith("/clubs", { name: "Riverside FC", clubType: "SPORTS_CLUB" }))
  })
})

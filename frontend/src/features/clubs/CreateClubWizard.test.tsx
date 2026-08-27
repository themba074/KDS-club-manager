import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { afterEach, describe, expect, it, vi } from "vitest"
import { api } from "@/features/auth/auth-api"
import { CreateClubWizard } from "./CreateClubWizard"

afterEach(() => vi.restoreAllMocks())

describe("club creation wizard", () => {
  it("reviews the name before creating and reports success", async () => {
    const club = { id: "club-1", name: "Savings", clubType: "INVESTMENT_CLUB", administrator: true }
    const post = vi.spyOn(api, "post").mockResolvedValue({ data: club })
    const onCreated = vi.fn()
    render(<QueryClientProvider client={new QueryClient()}><CreateClubWizard onCreated={onCreated} /></QueryClientProvider>)
    fireEvent.change(screen.getByLabelText("Club name"), { target: { value: "Savings" } })
    fireEvent.click(screen.getByRole("button", { name: "Review club" }))
    expect(await screen.findByText("Step 2 of 2")).toBeInTheDocument()
    expect(post).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole("button", { name: "Create club" }))
    await waitFor(() => expect(post).toHaveBeenCalledWith("/clubs", { name: "Savings", clubType: "INVESTMENT_CLUB" }))
    await waitFor(() => expect(onCreated).toHaveBeenCalledWith(club))
  })

  it("does not advance with a blank name", async () => {
    render(<QueryClientProvider client={new QueryClient()}><CreateClubWizard onCreated={vi.fn()} /></QueryClientProvider>)
    fireEvent.click(screen.getByRole("button", { name: "Review club" }))
    expect(await screen.findByRole("alert")).toHaveTextContent("Enter a club name")
  })
})

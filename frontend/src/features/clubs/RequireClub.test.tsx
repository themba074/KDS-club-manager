import { render, screen } from "@testing-library/react"
import { MemoryRouter, Route, Routes } from "react-router-dom"
import { beforeEach, expect, it } from "vitest"
import { useAuthStore } from "@/features/auth/auth-store"
import { RequireClub } from "./RequireClub"

beforeEach(() => useAuthStore.getState().clearSession())

function renderGuard() {
  render(<MemoryRouter><Routes>
    <Route element={<RequireClub />}><Route path="/" element={<h1>Club dashboard</h1>} /></Route>
    <Route path="/clubs" element={<h1>Choose a club</h1>} />
  </Routes></MemoryRouter>)
}

it("requires club selection before displaying tenant pages", () => {
  renderGuard()
  expect(screen.getByText("Choose a club")).toBeInTheDocument()
  expect(screen.queryByText("Club dashboard")).not.toBeInTheDocument()
})

it("hides tenant pages while switching", () => {
  useAuthStore.getState().beginClubSwitch()
  renderGuard()
  expect(screen.getByText("Switching club…")).toBeInTheDocument()
  expect(screen.queryByText("Club dashboard")).not.toBeInTheDocument()
})

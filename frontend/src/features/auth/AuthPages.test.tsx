import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { render, screen } from "@testing-library/react"
import { MemoryRouter } from "react-router-dom"
import { describe, expect, it } from "vitest"

import { CredentialsPage } from "./AuthPages"
import { useAuthStore } from "./auth-store"

describe("authentication forms", () => {
  it("uses a real submit button for registration", () => {
    useAuthStore.setState({ accessToken: null, user: null, initialized: true })
    render(
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter><CredentialsPage mode="register" /></MemoryRouter>
      </QueryClientProvider>,
    )

    expect(screen.getByRole("button", { name: "Register" })).toHaveAttribute("type", "submit")
  })
})

import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { MemoryRouter } from "react-router-dom"
import { beforeEach, expect, it, vi } from "vitest"
import { ForgotPasswordPage } from "./AuthPages"

const { post } = vi.hoisted(() => ({ post: vi.fn() }))
vi.mock("./auth-api", () => ({ api: { post }, errorMessage: () => "Network unavailable" }))
beforeEach(() => vi.clearAllMocks())
function page() {
  render(<QueryClientProvider client={new QueryClient({ defaultOptions: { mutations: { retry: false } } })}>
    <MemoryRouter><ForgotPasswordPage /></MemoryRouter>
  </QueryClientProvider>)
  fireEvent.change(screen.getByLabelText("Email"), { target: { value: "member@example.com" } })
}
it("shows pending state, blocks repeated submits, and confirms success", async () => {
  let finish!: (value: unknown) => void
  post.mockImplementation(() => new Promise((resolve) => { finish = resolve }))
  page()
  fireEvent.click(screen.getByRole("button", { name: "Send reset instructions" }))
  fireEvent.click(screen.getByRole("button", { name: "Send reset instructions" }))
  const pending = await screen.findByRole("button", { name: "Sending…" })
  expect(pending).toBeDisabled()
  fireEvent.click(pending)
  fireEvent.submit(pending.closest("form")!)
  expect(post).toHaveBeenCalledTimes(1)
  finish({})
  expect(await screen.findByRole("status")).toHaveTextContent("Check your inbox")
})
it("shows request failures and lets the user retry", async () => {
  post.mockRejectedValueOnce(new Error("offline")).mockResolvedValueOnce({})
  page()
  fireEvent.click(screen.getByRole("button", { name: "Send reset instructions" }))
  expect(await screen.findByRole("alert")).toHaveTextContent("Network unavailable")
  fireEvent.click(screen.getByRole("button", { name: "Send reset instructions" }))
  await waitFor(() => expect(post).toHaveBeenCalledTimes(2))
  expect(await screen.findByRole("status")).toHaveTextContent("Check your inbox")
})

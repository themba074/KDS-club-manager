import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { MemoryRouter } from "react-router-dom"
import { beforeEach, expect, it, vi } from "vitest"
import { CredentialsPage, ResetPasswordPage } from "./AuthPages"
import { useAuthStore } from "./auth-store"

const { mutate } = vi.hoisted(() => ({ mutate: vi.fn() }))
vi.mock("./auth-hooks", () => ({
  useAuthenticate: () => ({ mutate }),
  useConfirmPasswordReset: () => ({ mutate }),
  useRequestPasswordReset: () => ({ mutate }),
}))
beforeEach(() => { mutate.mockClear(); useAuthStore.getState().clearSession() })

it.each(["register", "reset"])("%s rejects seven characters and accepts eight", async (mode) => {
  render(<MemoryRouter initialEntries={["/?token=test-token"]}>
    {mode === "register" ? <CredentialsPage mode="register" /> : <ResetPasswordPage />}
  </MemoryRouter>)
  if (mode === "register") fireEvent.change(screen.getByLabelText("Email"), { target: { value: "test@example.com" } })
  const password = screen.getByLabelText(mode === "register" ? "Password" : "New password")
  const submit = screen.getByRole("button", { name: mode === "register" ? "Register" : "Reset password" })
  fireEvent.change(password, { target: { value: "abcdefg" } })
  fireEvent.click(submit)
  expect(await screen.findByText("Use at least 8 characters")).toBeInTheDocument()
  expect(mutate).not.toHaveBeenCalled()
  fireEvent.change(password, { target: { value: "abcdefgh" } })
  fireEvent.click(submit)
  await waitFor(() => expect(mutate).toHaveBeenCalledTimes(1))
})

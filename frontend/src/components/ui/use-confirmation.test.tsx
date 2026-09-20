import { act, fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import { useAuthStore } from "@/features/auth/auth-store";
import { useConfirmation } from "./use-confirmation";

function ReviewAction({ execute }: { execute: () => void }) {
  const { confirm, confirmation } = useConfirmation();
  return (
    <>
      {confirmation}
      <button
        onClick={() =>
          confirm("Review change", "This changes club access.", execute)
        }
      >
        Change
      </button>
    </>
  );
}

beforeEach(() => {
  useAuthStore.getState().setSession(
    "token",
    { id: "owner", email: "owner@example.test" },
    {
      id: "club-1",
      name: "Club",
      clubType: "INVESTMENT_CLUB",
      administrator: true,
      permissions: [],
    },
  );
});

it("executes only after confirmation and permits cancellation", () => {
  const execute = vi.fn();
  render(<ReviewAction execute={execute} />);
  fireEvent.click(screen.getByRole("button", { name: "Change" }));
  expect(screen.getByRole("dialog")).toHaveAccessibleName("Review change");
  expect(execute).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole("button", { name: "Go back" }));
  expect(execute).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole("button", { name: "Change" }));
  const confirm = screen.getByRole("button", { name: "Confirm" });
  fireEvent.click(confirm);
  fireEvent.click(confirm);
  expect(execute).toHaveBeenCalledTimes(1);
});

it("does not confirm an action after changing clubs", () => {
  const execute = vi.fn();
  render(<ReviewAction execute={execute} />);
  fireEvent.click(screen.getByRole("button", { name: "Change" }));
  const confirm = screen.getByRole("button", { name: "Confirm" });
  act(() =>
    useAuthStore.setState((state) => ({
      activeClub: { ...state.activeClub!, id: "club-2" },
    })),
  );
  fireEvent.click(confirm);
  expect(execute).not.toHaveBeenCalled();
});

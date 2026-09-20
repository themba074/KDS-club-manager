import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, expect, it, vi } from "vitest";
import { useAuthStore } from "@/features/auth/auth-store";
import { DocumentsPage } from "./index";
import type { DocumentLibrary } from "./document-hooks";

const { get, post, put } = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}));
vi.mock("@/features/auth/auth-api", () => ({
  api: { get, post, put },
  errorMessage: () => "Document request failed",
}));
let library: DocumentLibrary;
function setup() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <DocumentsPage />
    </QueryClientProvider>,
  );
}
beforeEach(() => {
  vi.resetAllMocks();
  useAuthStore
    .getState()
    .setSession(
      "token",
      { id: "owner", email: "owner@example.test" },
      {
        id: "club-1",
        name: "Club",
        clubType: "INVESTMENT_CLUB",
        administrator: true,
        permissions: ["DOCUMENTS_READ", "DOCUMENTS_MANAGE"],
      },
    );
  library = {
    canManage: true,
    visibilityRoles: [
      { code: "MEMBER", name: "Member" },
      { code: "TREASURER", name: "Treasurer" },
    ],
    documents: [
      {
        id: "doc-1",
        title: "Club rules",
        category: "Policy",
        visibleRoleCodes: ["MEMBER"],
        createdBy: "owner",
        createdAt: "2026-09-12T10:00:00Z",
        updatedAt: "2026-09-12T10:00:00Z",
        version: 0,
        versions: [
          {
            id: "version-1",
            versionNumber: 1,
            fileName: "rules.pdf",
            contentType: "application/pdf",
            fileSize: 1200,
            uploadedBy: "owner",
            uploadedAt: "2026-09-12T10:00:00Z",
          },
        ],
      },
    ],
  };
  get.mockResolvedValue({ data: library });
  post.mockResolvedValue({ data: library.documents[0] });
  put.mockResolvedValue({
    data: { ...library.documents[0], title: "Updated" },
  });
  Object.defineProperty(URL, "createObjectURL", {
    value: vi.fn(() => "blob:test"),
    configurable: true,
  });
  Object.defineProperty(URL, "revokeObjectURL", {
    value: vi.fn(),
    configurable: true,
  });
});

it("shows version history and manager controls and sends uploads as multipart", async () => {
  setup();
  expect(await screen.findByText("Club rules")).toBeInTheDocument();
  expect(screen.getByText(/v1 · rules.pdf/)).toBeInTheDocument();
  fireEvent.change(screen.getByLabelText("Title", { selector: "input" }), {
    target: { value: "Minutes" },
  });
  fireEvent.change(screen.getByLabelText("Category", { selector: "input" }), {
    target: { value: "Governance" },
  });
  fireEvent.change(screen.getByLabelText("File"), {
    target: {
      files: [new File(["hello"], "minutes.txt", { type: "text/plain" })],
    },
  });
  fireEvent.submit(
    screen.getByRole("button", { name: "Upload document" }).closest("form")!,
  );
  await waitFor(() =>
    expect(post).toHaveBeenCalledWith("/documents", expect.any(FormData)),
  );
  fireEvent.click(screen.getByRole("button", { name: "Edit access" }));
  const form = screen.getByRole("button", { name: "Save" }).closest("form")!;
  fireEvent.change(within(form).getByLabelText("Title"), {
    target: { value: "Updated" },
  });
  fireEvent.click(within(form).getByRole("button", { name: "Save" }));
  expect(put).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole("button", { name: "Confirm" }));
  await waitFor(() =>
    expect(put).toHaveBeenCalledWith(
      "/documents/doc-1",
      expect.objectContaining({ version: 0, title: "Updated" }),
    ),
  );
});

it("hides management controls for ordinary members", async () => {
  library = { ...library, canManage: false, visibilityRoles: [] };
  get.mockResolvedValue({ data: library });
  setup();
  expect(await screen.findByText("Club rules")).toBeInTheDocument();
  expect(screen.queryByText("Upload a document")).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Edit access" }),
  ).not.toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: "Download v1" }),
  ).toBeInTheDocument();
});

it("validates the five megabyte browser limit", async () => {
  setup();
  await screen.findByText("Club rules");
  fireEvent.change(screen.getByLabelText("Title", { selector: "input" }), {
    target: { value: "Large" },
  });
  fireEvent.change(screen.getByLabelText("Category", { selector: "input" }), {
    target: { value: "Policy" },
  });
  const large = new File([new Uint8Array(5 * 1024 * 1024 + 1)], "large.pdf", {
    type: "application/pdf",
  });
  fireEvent.change(screen.getByLabelText("File"), {
    target: { files: [large] },
  });
  fireEvent.submit(
    screen.getByRole("button", { name: "Upload document" }).closest("form")!,
  );
  expect(await screen.findByRole("alert")).toHaveTextContent("5 MB or smaller");
  expect(post).not.toHaveBeenCalled();
});

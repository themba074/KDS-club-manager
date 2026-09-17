import { useMutation } from "@tanstack/react-query"
import { api } from "@/features/auth/auth-api"

export type ReportKind = "members" | "meetings" | "voting"
export type ReportFormat = "CSV" | "PDF"

export function useReportDownload() {
  return useMutation({
    mutationFn: async ({ kind, from, to, format }: { kind: ReportKind; from: string; to: string; format: ReportFormat }) => {
      const response = await api.get<Blob>(`/reports/${kind}/export`, { params: { from, to, format }, responseType: "blob" })
      const url = URL.createObjectURL(response.data)
      try {
        const link = document.createElement("a")
        link.href = url
        link.download = `${kind}-${from}-to-${to}.${format.toLowerCase()}`
        link.click()
      } finally {
        URL.revokeObjectURL(url)
      }
    },
  })
}

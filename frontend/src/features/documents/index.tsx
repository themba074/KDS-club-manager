import { FileText } from "lucide-react"

import { ModulePlaceholder } from "@/components/layout/ModulePlaceholder"

export function DocumentsPage() {
  return (
    <ModulePlaceholder
      title="Documents"
      description="Keep constitutions, policies, minutes, and financial records organised."
      emptyTitle="No documents uploaded"
      emptyDescription="Secure club files and their version history will appear here."
      icon={FileText}
    />
  )
}

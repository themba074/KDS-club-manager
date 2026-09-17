import {
  Bell,
  ChartNoAxesCombined,
  ClipboardCheck,
  FileText,
  HandCoins,
  LayoutDashboard,
  Scale,
  Users,
  Vote,
  type LucideIcon,
} from "lucide-react"

export type NavigationItem = {
  label: string
  path: string
  icon: LucideIcon
  section: "Club" | "Governance"
  permission?: string
  module?: string
}

export const navigationItems: NavigationItem[] = [
  { label: "Roles", path: "/roles", icon: Users, section: "Governance", permission: "ROLES_READ", module: "ROLES" },
  { label: "Dashboard", path: "/", icon: LayoutDashboard, section: "Club" },
  { label: "Members", path: "/members", icon: Users, section: "Club", permission: "MEMBERS_READ", module: "MEMBERS" },
  {
    label: "Contributions",
    path: "/contributions",
    icon: HandCoins,
    permission: "CONTRIBUTIONS_READ",
    module: "CONTRIBUTIONS",
    section: "Club",
  },
  { label: "Meetings", path: "/meetings", icon: ClipboardCheck, section: "Club", permission: "MEETINGS_READ", module: "MEETINGS" },
  { label: "Voting", path: "/voting", icon: Vote, section: "Governance", permission: "VOTES_READ", module: "VOTING" },
  { label: "Documents", path: "/documents", icon: FileText, section: "Governance", permission: "DOCUMENTS_READ", module: "DOCUMENTS" },
  { label: "Notifications", path: "/notifications", icon: Bell, section: "Governance", module: "NOTIFICATIONS" },
  { label: "Audit", path: "/audit", icon: Scale, section: "Governance", permission: "AUDIT_READ", module: "AUDIT" },
  {
    label: "Reports",
    path: "/reports",
    icon: ChartNoAxesCombined,
    permission: "REPORTS_READ",
    module: "REPORTS",
    section: "Governance",
  },
]

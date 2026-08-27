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
}

export const navigationItems: NavigationItem[] = [
  { label: "Roles", path: "/roles", icon: Users, section: "Governance", permission: "ROLES_READ" },
  { label: "Dashboard", path: "/", icon: LayoutDashboard, section: "Club" },
  { label: "Members", path: "/members", icon: Users, section: "Club", permission: "MEMBERS_READ" },
  {
    label: "Contributions",
    path: "/contributions",
    icon: HandCoins,
    permission: "CONTRIBUTIONS_READ",
    section: "Club",
  },
  { label: "Meetings", path: "/meetings", icon: ClipboardCheck, section: "Club", permission: "MEETINGS_READ" },
  { label: "Voting", path: "/voting", icon: Vote, section: "Governance", permission: "VOTES_READ" },
  { label: "Documents", path: "/documents", icon: FileText, section: "Governance", permission: "DOCUMENTS_READ" },
  { label: "Notifications", path: "/notifications", icon: Bell, section: "Governance" },
  { label: "Audit", path: "/audit", icon: Scale, section: "Governance", permission: "AUDIT_READ" },
  {
    label: "Reports",
    path: "/reports",
    icon: ChartNoAxesCombined,
    permission: "REPORTS_READ",
    section: "Governance",
  },
]

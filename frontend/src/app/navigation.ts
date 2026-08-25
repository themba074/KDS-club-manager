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
}

export const navigationItems: NavigationItem[] = [
  { label: "Dashboard", path: "/", icon: LayoutDashboard, section: "Club" },
  { label: "Members", path: "/members", icon: Users, section: "Club" },
  {
    label: "Contributions",
    path: "/contributions",
    icon: HandCoins,
    section: "Club",
  },
  { label: "Meetings", path: "/meetings", icon: ClipboardCheck, section: "Club" },
  { label: "Voting", path: "/voting", icon: Vote, section: "Governance" },
  { label: "Documents", path: "/documents", icon: FileText, section: "Governance" },
  { label: "Notifications", path: "/notifications", icon: Bell, section: "Governance" },
  { label: "Audit", path: "/audit", icon: Scale, section: "Governance" },
  {
    label: "Reports",
    path: "/reports",
    icon: ChartNoAxesCombined,
    section: "Governance",
  },
]

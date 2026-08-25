import { Bell, ChevronDown, Menu, X } from "lucide-react"
import { useState } from "react"
import { NavLink, Outlet } from "react-router-dom"

import { navigationItems } from "@/app/navigation"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import { useAuthStore } from "@/features/auth/auth-store"
import { api } from "@/features/auth/auth-api"

const navigationSections = ["Club", "Governance"] as const

function Brand() {
  return (
    <NavLink
      to="/"
      className="flex items-center gap-3 rounded-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sidebar-ring"
      aria-label="KDS Club Manager home"
    >
      <span className="grid size-9 place-items-center rounded-xl bg-sidebar-primary text-sm font-bold tracking-tight text-sidebar-primary-foreground shadow-sm">
        KDS
      </span>
      <span className="min-w-0">
        <span className="block truncate text-sm font-semibold text-sidebar-foreground">
          Club Manager
        </span>
        <span className="block truncate text-xs text-muted-foreground">Investment club</span>
      </span>
    </NavLink>
  )
}

function SidebarNavigation({ onNavigate }: { onNavigate?: () => void }) {
  return (
    <nav className="flex-1 space-y-6 overflow-y-auto px-3 py-5" aria-label="Main navigation">
      {navigationSections.map((section) => (
        <div key={section}>
          <p className="mb-2 px-3 text-[0.6875rem] font-semibold uppercase tracking-[0.14em] text-muted-foreground">
            {section}
          </p>
          <ul className="space-y-1">
            {navigationItems
              .filter((item) => item.section === section)
              .map((item) => {
                const Icon = item.icon

                return (
                  <li key={item.path}>
                    <NavLink
                      to={item.path}
                      end={item.path === "/"}
                      onClick={onNavigate}
                      className={({ isActive }) =>
                        cn(
                          "flex min-h-10 items-center gap-3 rounded-lg px-3 text-sm font-medium text-muted-foreground transition-colors hover:bg-sidebar-accent hover:text-sidebar-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sidebar-ring",
                          isActive &&
                            "bg-sidebar-primary text-sidebar-primary-foreground shadow-sm hover:bg-sidebar-primary hover:text-sidebar-primary-foreground",
                        )
                      }
                    >
                      <Icon className="size-[1.125rem]" aria-hidden="true" />
                      <span>{item.label}</span>
                    </NavLink>
                  </li>
                )
              })}
          </ul>
        </div>
      ))}
    </nav>
  )
}

function Sidebar({ onNavigate }: { onNavigate?: () => void }) {
  const user = useAuthStore((state) => state.user)
  const clearSession = useAuthStore((state) => state.clearSession)
  return (
    <>
      <div className="border-b border-sidebar-border p-4">
        <Brand />
      </div>
      <SidebarNavigation onNavigate={onNavigate} />
      <div className="border-t border-sidebar-border p-3">
        <button onClick={() => void api.post("/auth/logout").finally(clearSession)}
          type="button"
          className="flex w-full items-center gap-3 rounded-lg p-2 text-left transition-colors hover:bg-sidebar-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sidebar-ring"
        >
          <span className="grid size-9 shrink-0 place-items-center rounded-full bg-accent text-xs font-semibold text-accent-foreground">
            {user?.email.slice(0, 2).toUpperCase() ?? "?"}
          </span>
          <span className="min-w-0 flex-1">
            <span className="block truncate text-sm font-medium">{user?.email}</span>
            <span className="block truncate text-xs text-muted-foreground">Log out</span>
          </span>
          <ChevronDown className="size-4 text-muted-foreground" aria-hidden="true" />
        </button>
      </div>
    </>
  )
}

export function AppLayout() {
  const [mobileNavigationOpen, setMobileNavigationOpen] = useState(false)

  return (
    <div className="min-h-screen bg-background text-foreground">
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 flex-col border-r border-sidebar-border bg-sidebar lg:flex">
        <Sidebar />
      </aside>

      {mobileNavigationOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button
            type="button"
            className="absolute inset-0 bg-foreground/35 backdrop-blur-[2px]"
            aria-label="Dismiss navigation"
            onClick={() => setMobileNavigationOpen(false)}
          />
          <aside className="relative flex h-full w-[min(20rem,88vw)] flex-col border-r border-sidebar-border bg-sidebar shadow-2xl">
            <div className="flex items-center justify-between border-b border-sidebar-border p-4">
              <Brand />
              <Button
                variant="ghost"
                size="icon"
                aria-label="Close navigation"
                onClick={() => setMobileNavigationOpen(false)}
              >
                <X aria-hidden="true" />
              </Button>
            </div>
            <SidebarNavigation onNavigate={() => setMobileNavigationOpen(false)} />
          </aside>
        </div>
      )}

      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 flex h-16 items-center gap-3 border-b border-border/80 bg-background/90 px-4 backdrop-blur-xl sm:px-6 lg:px-8">
          <Button
            variant="ghost"
            size="icon"
            className="lg:hidden"
            aria-label="Open navigation"
            onClick={() => setMobileNavigationOpen(true)}
          >
            <Menu aria-hidden="true" />
          </Button>
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-medium">Ubuntu Investment Club</p>
            <p className="truncate text-xs text-muted-foreground">Active workspace</p>
          </div>
          <Button variant="ghost" size="icon" aria-label="View notifications">
            <Bell aria-hidden="true" />
          </Button>
          <span className="grid size-8 place-items-center rounded-full bg-primary text-xs font-semibold text-primary-foreground lg:hidden">
            TS
          </span>
        </header>

        <main className="mx-auto w-full max-w-[100rem] p-4 sm:p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

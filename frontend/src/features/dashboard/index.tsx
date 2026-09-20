import { Link } from "react-router-dom";
import { ArrowUpRight, ShieldCheck } from "lucide-react";
import { navigationItems } from "@/app/navigation";
import { PageLayout } from "@/components/layout/PageLayout";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuthStore } from "@/features/auth/auth-store";
import { useClubTypes } from "@/features/clubs/club-hooks";

const descriptions: Record<string, string> = {
  "/members": "Find members and view their membership status.",
  "/contributions": "View contribution schedules and your payment ledger.",
  "/meetings": "Check meeting dates, agendas, RSVPs, and published minutes.",
  "/voting":
    "Review motions, cast eligible ballots, and read published results.",
  "/documents": "Read the club documents available to your role.",
  "/notifications": "Read your club updates and reminders.",
  "/roles": "Review roles and manage member permissions.",
  "/audit": "Review the club's recorded activity.",
  "/reports": "View club reports and download exports.",
};

export function DashboardPage() {
  const club = useAuthStore((state) => state.activeClub);
  const templates = useClubTypes();
  const modules = templates.data?.find(
    (template) => template.code === club?.clubType,
  )?.enabledModules;
  const shortcuts = navigationItems.filter(
    (item) =>
      item.path !== "/" &&
      (!item.permission || club?.permissions?.includes(item.permission)) &&
      (!item.module || !modules || modules.includes(item.module)),
  );
  return (
    <PageLayout
      title="Dashboard"
      description="Choose a workspace to manage your club’s records, governance, and member activity."
    >
      <Card className="relative overflow-hidden border-primary/15 bg-[linear-gradient(115deg,oklch(0.985_0.012_155),oklch(0.945_0.04_155))] shadow-none">
        <CardContent className="relative grid gap-6 py-6 sm:grid-cols-[1fr_auto] sm:items-center sm:py-7">
          <div>
            <p className="text-sm font-medium text-primary">Club workspace</p>
            <h2 className="mt-2 max-w-2xl font-heading text-2xl font-semibold tracking-[-0.03em]">
              Ready to manage your club?
            </h2>
            <p className="mt-2 max-w-xl text-sm leading-6 text-muted-foreground">
              Your available workspaces are shown below based on your club’s configuration and your current role.
            </p>
          </div>
          <div className="flex items-center gap-3 rounded-xl border border-primary/15 bg-card/75 px-4 py-3 text-sm shadow-sm">
            <ShieldCheck className="size-5 text-primary" aria-hidden="true" />
            <span className="text-muted-foreground">Access is role-based</span>
          </div>
        </CardContent>
      </Card>

      <div>
        <div className="mb-4 flex items-center justify-between gap-4">
          <h2 className="font-heading text-lg font-semibold tracking-tight">Your workspaces</h2>
          <span className="text-sm text-muted-foreground">{shortcuts.length} available</span>
        </div>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {shortcuts.map((item) => {
          const Icon = item.icon;
          return (
            <Card key={item.path} className="group min-h-52 transition duration-200 hover:-translate-y-0.5 hover:border-primary/30 hover:shadow-md">
              <CardHeader className="grid grid-cols-[1fr_auto] items-center gap-3">
                <CardTitle>{item.label}</CardTitle>
                <span className="grid size-10 place-items-center rounded-xl bg-primary/10 text-primary">
                  <Icon className="size-5" aria-hidden="true" />
                </span>
              </CardHeader>
              <CardContent className="flex flex-1 flex-col justify-between gap-5">
                <p className="text-sm text-muted-foreground">
                  {descriptions[item.path]}
                </p>
                <Link
                  className="inline-flex w-fit items-center gap-1.5 rounded text-sm font-semibold text-primary focus-visible:outline-2 focus-visible:outline-offset-4"
                  to={item.path}
                >
                  Open {item.label.toLowerCase()}
                  <ArrowUpRight className="size-4 transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5" aria-hidden="true" />
                </Link>
              </CardContent>
            </Card>
          );
        })}
        </div>
      </div>
      {shortcuts.length === 0 && (
        <Card><CardContent className="py-6 text-sm text-muted-foreground">No workspaces are available for your current role. Contact your club administrator.</CardContent></Card>
      )}
    </PageLayout>
  );
}

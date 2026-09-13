import { Bell,CalendarDays,CircleDollarSign,FileCheck2,Vote } from "lucide-react"
import { Link } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { errorMessage } from "@/features/auth/auth-api"
import { useMarkAllNotificationsRead,useMarkNotificationRead,useNotifications,type NotificationType } from "./notification-hooks"

const icons:Record<NotificationType,typeof Bell>={PAYMENT_REMINDER:CircleDollarSign,MEETING_SCHEDULED:CalendarDays,MEETING_UPDATED:CalendarDays,VOTE_OPEN:Vote,VOTE_CLOSING:Vote,MINUTES_PUBLISHED:FileCheck2}
export function NotificationCenter(){const feed=useNotifications(),read=useMarkNotificationRead(),readAll=useMarkAllNotificationsRead();const unread=feed.data?.filter(item=>!item.readAt).length??0
  return <section className="space-y-4">
    <div className="flex flex-wrap items-start justify-between gap-3"><div><h1 className="text-3xl font-semibold tracking-tight">Notifications</h1><p className="text-muted-foreground">Payments, meetings, votes, and published records for this club.</p></div>{unread>0&&<Button variant="outline" onClick={()=>readAll.mutate()} disabled={readAll.isPending}>Mark all as read</Button>}</div>
    {feed.isPending&&<p role="status">Loading notifications…</p>}{feed.error&&<p role="alert" className="text-destructive">{errorMessage(feed.error)}</p>}
    {!feed.isPending&&feed.data?.length===0&&<div className="rounded-xl border bg-card p-8 text-center"><Bell className="mx-auto mb-3 size-8 text-muted-foreground"/><h2 className="font-semibold">You're all caught up</h2><p className="text-sm text-muted-foreground">New club notifications will appear here.</p></div>}
    <div className="space-y-3">{feed.data?.map(item=>{const Icon=icons[item.type];return <article key={item.id} className={`flex gap-3 rounded-xl border p-4 ${item.readAt?"bg-card":"border-primary/40 bg-primary/5"}`}><Icon className="mt-1 size-5 shrink-0 text-primary" aria-hidden="true"/><div className="min-w-0 flex-1"><div className="flex flex-wrap items-start justify-between gap-2"><div><h2 className="font-semibold">{item.title}</h2><p className="text-sm text-muted-foreground">{item.message}</p><p className="mt-1 text-xs text-muted-foreground">{new Date(item.availableAt).toLocaleString()}</p></div>{!item.readAt&&<Button size="sm" variant="ghost" onClick={()=>read.mutate(item.id)}>Mark read</Button>}</div>{item.targetPath&&<Link className="mt-2 inline-block text-sm font-medium text-primary underline" to={item.targetPath} onClick={()=>!item.readAt&&read.mutate(item.id)}>View details</Link>}</div></article>})}</div>
  </section>}

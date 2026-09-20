# KDS Club Manager frontend

The React application for KDS Club Manager. The planned feature sequence through
#21 is complete; current work focuses on quality improvements and pilot feedback.
See the [project README](../README.md), [architecture](../docs/TECH_SPEC.md), and
[working agreement](../AGENTS.md) for the full context.

## Local development

From this directory, install dependencies with `npm ci`, then run `npm run dev`.
Vite serves the app at http://127.0.0.1:5175 and proxies `/api` to the backend at
http://127.0.0.1:8080. If Docker already occupies port 5175, use
`npm run dev -- --port 5176` for a separate development preview.

## Verification

```powershell
npm run test:run -- --maxWorkers=1
npm run lint
npm run build
```

The single-worker setting avoids worker startup timeouts seen on Windows.

## Implementation conventions

- Put module UI and TanStack Query hooks under `src/features/<module>`.
- Keep server requests in hooks/services and session state in Zustand.
- Respect both permissions and club-type modules in navigation. The dashboard
  provides shortcuts to available workspaces, not placeholder live statistics.
- Use `src/components/ui/use-confirmation.tsx` for consequential actions. Show
  the affected record and consequence before executing the captured action.
  Dismissing a review must not send a mutation request.
- Validate inputs before opening a review; the backend remains authoritative
  for authorization, tenant isolation, concurrency, and business rules.
- Keep failed form values available for correction. Disable pending actions and
  reset both file state and the file input after successful uploads.

Regression coverage includes confirmations, cancellation without mutations,
club changes during review, unsaved minutes, attachment limits, schedule dates,
and permission-aware workspace shortcuts.

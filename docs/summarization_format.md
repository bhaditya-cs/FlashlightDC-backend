# Bill Summarization API — Response Format

## Endpoint

```
GET /api/v1/bills/{congress}/{type}/{number}/summary
```

Example: `GET /api/v1/bills/119/hr/5499/summary`

## Outer envelope

The endpoint returns a JSON object with these top-level fields:

```jsonc
{
  "billId": "119-hr-5499",          // string, "{congress}-{type}-{number}"
  "summary": "<see below>",         // string — raw JSON when status=SUCCESS, plain text otherwise
  "modelUsed": "gemini-2.5-flash",  // string
  "status": "SUCCESS",              // "SUCCESS" | "NO_CONTENT" | "ERROR"
  "format": "json"                  // "json" | "plaintext"
}
```

| `status` | `summary` value | `format` |
|----------|----------------|----------|
| `SUCCESS` | Raw JSON string (the structured summary — parse it with `JSON.parse`) | `json` |
| `NO_CONTENT` | Human-readable message (e.g. "No bill text or CRS summary available…") | `plaintext` |
| `ERROR` | Human-readable error message | `plaintext` |

**Frontend must `JSON.parse(response.summary)` only when `response.status === "SUCCESS"` and `response.format === "json"`.**

## Inner JSON (the parsed `summary` string)

When `status` is `SUCCESS`, the `summary` field contains a JSON string that parses to:

```jsonc
{
  "bill_number": "HR 5499",                             // string
  "title": "Short bill title",                          // string
  "hook": "One-sentence takeaway under 25 words…",      // string
                                                        //   if bill text absent, may be prefixed
                                                        //   with "[Based on official summary,
                                                        //    not full bill text]"
  "sections": [
    {
      "id": "tldr",
      "title": "TL;DR",
      "collapsed_summary": {
        "items": [
          { "label": "What",   "text": "Max 12 words" },
          { "label": "Who",    "text": "Max 15 words (sponsor, party, state)" },
          { "label": "Status", "text": "Plain English status" }
        ]
      },
      "expanded_detail": "3-5 sentences explaining the bill, sponsor background, and what the status means."
    },
    {
      "id": "status",
      "title": "Where Is This Bill?",
      "collapsed_summary": {
        "stages": ["Introduced", "Committee", "House Vote", "Senate", "Law"],
        "current": "Committee"      // string — one of the values in stages
      },
      "expanded_detail": "2-4 sentences describing the current stage, known dates, and what needs to happen next."
    },
    {
      "id": "impacts",
      "title": "If This Passes, You Might Notice",
      "collapsed_summary": {
        "bullets": [
          "Concrete impact 1 (e.g., 'Your credit card rate could change')",
          "Concrete impact 2",
          "Concrete impact 3"
        ]
      },
      "expanded_detail": "3-4 sentences adding context to each bullet point, sticking to facts."
    },
    {
      "id": "debate",
      "title": "The Debate",
      "collapsed_summary": {
        "supporters": "One sentence summarizing the main argument in favor.",
        "critics": "One sentence summarizing the main argument against."
      },
      "expanded_detail": "Optional 2-3 sentences with additional viewpoints or notable endorsements/criticisms. May be empty string."
    },
    {
      "id": "bottom_line",
      "title": "Bottom Line",
      "collapsed_summary": "One sentence takeaway a reader might share with a friend.",
      "expanded_detail": "Optional 1-2 sentences with crucial caveats. May be empty string."
    }
  ]
}
```

## Section-by-section guide for progressive disclosure

Each section has the same shape:

```ts
interface Section {
  id: string;                  // stable key: "tldr" | "status" | "impacts" | "debate" | "bottom_line"
  title: string;               // display heading
  collapsed_summary: object;   // compact view (rendered when section is collapsed)
  expanded_detail: string;     // longer text (rendered when section is expanded)
}
```

The `collapsed_summary` shape varies per section:

| Section `id` | `collapsed_summary` shape |
|-------------|---------------------------|
| `tldr` | `{ items: [{ label, text }, …] }` — 3 fixed items (What, Who, Status) |
| `status` | `{ stages: string[], current: string }` — `current` is always one of `stages` |
| `impacts` | `{ bullets: string[] }` — typically 3 bullets |
| `debate` | `{ supporters: string, critics: string }` |
| `bottom_line` | `string` directly — not an object |

## Status values in context

The `status` section uses these stage names (always exactly these strings):

| Stage | Meaning |
|-------|---------|
| `Introduced` | Bill has been introduced but not yet in committee |
| `Committee` | Under review by one or more committees |
| `House Vote` | Passed committee, awaiting or completed House floor vote |
| `Senate` | Under consideration by the Senate |
| `Law` | Signed into law |

The `current` field will be one of the above strings. The `stages` array always contains all five in order — you can use it to render a progress indicator.

## Error shape (when both sources unavailable)

If both bill text and CRS summary are absent, the AI returns:

```json
{ "error": "No bill text available" }
```

This would arrive inside the envelope as `summary`, but in practice the backend returns `status: "NO_CONTENT"` with a plain-text message before calling the AI in this case. Treat this as a fallback shape to handle defensively.

## Notes

- **No Markdown, no emojis** in any text field. All strings are plain text.
- **Anchor in bill text** — claims are based on the actual legislative language, not the CRS summary alone.
- **Hooked prefix** — when bill text is unavailable and only the CRS summary is used, the `hook` field will start with `[Based on official summary, not full bill text]`.
- Fields marked "optional" or "may be empty" can be empty strings (`""`), especially `debate.expanded_detail` and `bottom_line.expanded_detail`.
- Section order (`tldr`, `status`, `impacts`, `debate`, `bottom_line`) should be treated as stable but defensive code should reference sections by `id` rather than array index.

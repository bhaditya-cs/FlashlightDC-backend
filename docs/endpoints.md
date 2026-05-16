# API Endpoints

Developer reference for all REST endpoints. The application runs on `http://localhost:8080` by default.

Base paths:
- `/api/bills` — Bill listing, detail, fetch, and search
- `/api/members` — Member listing, detail, and fetch
- `/api/v1/bills` — AI bill summarization
- `/api/stats` — Aggregate statistics

---

## Bills — `/api/bills`

### GET /api/bills

List bills from the local database, paginated.

| Param | Type | Default | Description |
|---|---|---|---|
| `congress` | int | `119` | Congress number |
| `page` | int | `0` | Zero-based page index |
| `size` | int | `20` | Page size |

```bash
curl "http://localhost:8080/api/bills?congress=119&page=0&size=10"
```

Returns a Spring Data `Page<Bill>` containing bill entities with their sponsors and cosponsors.

Bill entity fields: `id`, `congress`, `billNumber`, `billType`, `title`, `originChamber`, `introducedDate`, `latestActionDate`, `latestActionText`, `policyArea`, `url`, `summary`, `summaryUpdatedAt`, `updatedAt`, `sponsors[]`, `cosponsors[]`.

---

### GET /api/bills/{congress}/{type}/{number}

Get a single bill from the DB by congress, type, and bill number.

```bash
curl "http://localhost:8080/api/bills/119/hr/1234"
```

| Param | Type | Example | Description |
|---|---|---|---|
| `congress` | int | `119` | Congress number |
| `type` | String | `hr`, `s`, `hjres` | Bill type code |
| `number` | String | `1234` | Bill number (String to support prefixes like `conres`) |

Returns `200` with a `Bill` entity, or `404` if not in the database.

---

### GET /api/bills/policy-area/{policyArea}

List bills by policy area, paginated.

```bash
curl "http://localhost:8080/api/bills/policy-area/Health?congress=119&page=0&size=10"
```

| Param | Type | Default | Description |
|---|---|---|---|
| `policyArea` | path | (required) | Policy area name (e.g., `Health`, `Armed Forces and National Security`) |
| `congress` | int | `119` | Congress number |
| `page` | int | `0` | Zero-based page index |
| `size` | int | `20` | Page size |

Returns a `Page<Bill>`. The policy area must match exactly (case-sensitive).

---

### GET /api/bills/raw

Proxy to the Congress.gov API. Returns raw bill listing data directly from Congress.gov.

```bash
curl "http://localhost:8080/api/bills/raw?congress=119&limit=20&offset=0"
```

| Param | Type | Default | Description |
|---|---|---|---|
| `congress` | int | `119` | Congress number |
| `limit` | int | `20` | Results per page (max 250) |
| `offset` | int | `0` | Pagination offset |

Returns the deserialized `BillListResponse` from Congress.gov.

---

### GET /api/bills/raw/{congress}/{type}/{number}

Proxy to the Congress.gov API for a single bill's detail.

```bash
curl "http://localhost:8080/api/bills/raw/119/hr/1234"
```

Returns the deserialized `BillDetailResponse` from Congress.gov.

---

### POST /api/bills/fetch

Fetch bills from the Congress.gov API and persist them to the database. Bypasses any pagination defaults of `/raw` — batches a single `limit`/`offset` page and saves all results via upsert.

```bash
curl -X POST "http://localhost:8080/api/bills/fetch?congress=119&limit=20&offset=0"
```

| Param | Type | Default | Description |
|---|---|---|---|
| `congress` | int | `119` | Congress number |
| `limit` | int | `20` | Bills to fetch |
| `offset` | int | `0` | Starting offset |

Returns a text response like `"Persisted 20 bills"`.

---

### POST /api/bills/fetch/{congress}/{type}/{number}

Fetch and persist a single bill by congress, type, and number.

```bash
curl -X POST "http://localhost:8080/api/bills/fetch/119/hr/1234"
```

Returns the persisted `Bill` entity with `200`.

---

## Members — `/api/members`

### GET /api/members

List members from the local database, optionally filtered by party and/or state.

```bash
curl "http://localhost:8080/api/members"
curl "http://localhost:8080/api/members?party=Democratic"
curl "http://localhost:8080/api/members?state=CA"
curl "http://localhost:8080/api/members?party=Republican&state=TX"
```

| Param | Type | Required | Description |
|---|---|---|---|
| `party` | String | no | Party name (e.g., `Democratic`, `Republican`, `Independent`) |
| `state` | String | no | Two-letter state abbreviation (e.g., `CA`, `NY`) |

Returns a `List<Member>`. Member entities include: `bioguideId`, `name`, `partyName`, `state`, `district`, `imageUrl`, `attribution`, `url`, `updatedAt`, `terms[]`, `sponsorships[]`, `cosponsorships[]`.

---

### GET /api/members/{bioguideId}

Get a single member by bioguide ID.

```bash
curl "http://localhost:8080/api/members/A000374"
```

Returns `200` with the `Member` entity, or `404` if not found.

---

### GET /api/members/raw

Proxy to the Congress.gov API. Returns raw member listing data.

```bash
curl "http://localhost:8080/api/members/raw?congress=119&limit=20&offset=0"
```

| Param | Type | Default | Description |
|---|---|---|---|
| `congress` | int | `119` | Congress number |
| `limit` | int | `20` | Results per page |
| `offset` | int | `0` | Pagination offset |

Returns the deserialized `MemberListResponse` from Congress.gov.

---

### POST /api/members/fetch

Fetch members from the Congress.gov API and persist them to the database.

```bash
curl -X POST "http://localhost:8080/api/members/fetch?congress=119&limit=20&offset=0"
```

| Param | Type | Default | Description |
|---|---|---|---|
| `congress` | int | `119` | Congress number |
| `limit` | int | `20` | Members to fetch |
| `offset` | int | `0` | Starting offset |

Returns a text response like `"Persisted 20 members"`. Note: existing members are deleted and re-inserted (terms are replaced entirely).

---

## Summarization — `/api/v1/bills`

### GET /api/v1/bills/{congress}/{type}/{number}/summary

Get or generate an AI summary for a bill. Checks the database first — if the bill already has a summary, returns it immediately. Otherwise, fetches the bill text from Congress.gov, sends it to Vertex AI (Gemini), persists the result, and returns it.

```bash
curl "http://localhost:8080/api/v1/bills/119/hr/1234/summary"
```

| Param | Type | Description |
|---|---|---|
| `congress` | int | Congress number |
| `type` | String | Bill type code |
| `number` | int | Bill number |

**Response (cached):**

```json
{
  "billId": "119-hr-1234",
  "summary": "This bill establishes a grant program for...",
  "modelUsed": null,
  "status": "SUCCESS",
  "format": "plaintext"
}
```

**Response (on error):**

```json
{
  "billId": "119-hr-9999",
  "summary": null,
  "modelUsed": "gemini-2.5-flash",
  "status": "ERROR",
  "format": "plaintext"
}
```

| Field | Type | Description |
|---|---|---|
| `billId` | String | Composite identifier: `{congress}-{type}-{number}` |
| `summary` | String | The plaintext summary, or null on error |
| `modelUsed` | String | The Vertex AI model used (null when served from cache) |
| `status` | String | `"SUCCESS"` or `"ERROR"` |
| `format` | String | Always `"plaintext"` |

This endpoint uses a blocking Vertex AI call wrapped in a reactive `Mono`. Summarization speed depends on the bill's text length and the Vertex AI API response time.

---

## Statistics — `/api/stats`

See [`stats_endpoints.md`](./stats_endpoints.md) for full details.

| Endpoint | Description |
|---|---|
| `GET /api/stats/bills` | Total bill count, summarized bill count, and 24h delta |
| `GET /api/stats/party-breakdown` | Bill count by sponsor party |
| `GET /api/stats/state-legislation` | Bill count by sponsor state |
| `GET /api/stats/cosponsor-averages` | Cosponsorship statistics |

---

## Common Notes

- **CORS**: The application allows GET requests from the configured `FRONTEND_URL` (default `http://localhost:3000`). The `/api/v1/bills` endpoint also has its own `@CrossOrigin` annotation. POST endpoints are not covered by CORS and will be blocked by browsers, though they work from `curl` and server-side clients.
- **Content type**: All responses are `application/json`.
- **Error handling**: Proxy endpoints return a reactive error signal for non-2xx Congress.gov responses. DB read endpoints return `404` when an entity is not found.
- **Pagination**: DB-backed endpoints use Spring Data's `Page` with zero-based page numbers. Raw proxy endpoints use Congress.gov's `offset`/`limit` model.
- **API key**: All Congress.gov proxy requests include the key from the `CONGRESS_API_KEY` environment variable via a `WebClient` filter.

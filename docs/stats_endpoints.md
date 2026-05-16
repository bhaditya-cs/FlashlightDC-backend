# Statistics Endpoints

Base path: `/api/stats`

All endpoints support an optional `congress` query parameter (defaults to `119`).

---

## GET /api/stats/bills

Returns aggregate bill counts for a given congress, including how many have been summarized.

**Example request:**

```bash
curl "http://localhost:8080/api/stats/bills?congress=119"
```

**Response:**

```json
{
  "totalBills": 14023,
  "summarizedBills": 8200,
  "deltaLast24h": 34
}
```

| Field | Type | Description |
|---|---|---|
| `totalBills` | `long` | Total number of bills in the DB for this congress |
| `summarizedBills` | `long` | Number of bills that have a non-null summary |
| `deltaLast24h` | `long` | Difference in total bill count compared to 24 hours ago (snapshot-based; 0 if no snapshot exists) |

---

## GET /api/stats/party-breakdown

Returns the number of bills sponsored by members of each party.

**Example request:**

```bash
curl "http://localhost:8080/api/stats/party-breakdown?congress=119"
```

**Response:**

```json
[
  { "party": "Republican", "billCount": 6200 },
  { "party": "Democratic", "billCount": 7100 },
  { "party": "Independent", "billCount": 723 }
]
```

| Field | Type | Description |
|---|---|---|
| `party` | `String` | Party name from the member's most recent term |
| `billCount` | `long` | Number of bills whose sponsors or cosponsors belong to this party |

---

## GET /api/stats/state-legislation

Returns the number of bills sponsored by members from each state.

**Example request:**

```bash
curl "http://localhost:8080/api/stats/state-legislation?congress=119"
```

**Response:**

```json
[
  { "state": "CA", "billCount": 1450 },
  { "state": "TX", "billCount": 980 },
  ...
]
```

| Field | Type | Description |
|---|---|---|
| `state` | `String` | Two-letter state abbreviation |
| `billCount` | `long` | Number of bills whose primary sponsor represents this state |

---

## GET /api/stats/cosponsor-averages

Returns cosponsorship statistics for the given congress.

**Example request:**

```bash
curl "http://localhost:8080/api/stats/cosponsor-averages?congress=119"
```

**Response:**

```json
{
  "avgCosponsorsPerBill": 12.45,
  "maxCosponsors": 420,
  "billsWithCosponsors": 11900,
  "totalBills": 14023,
  "bipartisanBills": 3400
}
```

| Field | Type | Description |
|---|---|---|
| `avgCosponsorsPerBill` | `BigDecimal` | Average number of cosponsors across all bills |
| `maxCosponsors` | `long` | Most cosponsors on any single bill |
| `billsWithCosponsors` | `long` | Count of bills with at least one cosponsor |
| `totalBills` | `long` | Total bill count used in these calculations |
| `bipartisanBills` | `long` | Count of bills where at least one cosponsor is from a different party than the primary sponsor |

---

## Usage notes

- All stats are **read-only**. Responses are served from the database only — no calls to the Congress.gov API.
- The default congress is `119` (the current congress as of 2025–2026). Pass `?congress=118` etc. to query historical data.
- Stats are computed from the DB via native queries. No caching layer exists, so large congresses may be slow.
- The `deltaLast24h` in `/bills` depends on entries in the `bill_count_snapshots` table, which are populated by a cron job.

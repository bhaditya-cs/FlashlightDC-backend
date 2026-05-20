#!/usr/bin/env bash
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

ENV_INPUT="${1:-}"
if [ -z "$ENV_INPUT" ]; then
    echo ""
    echo -e "${CYAN}  Select target environment:${NC}"
    echo -e "    ${GREEN}1${NC}) local   (http://localhost:8080)"
    echo -e "    ${GREEN}2${NC}) hosted  (https://api.flashlight-dc.org)"
    echo -e "    ${GREEN}3${NC}) custom  (enter a URL)"
    echo ""
    read -rp "  Choice [1-3]: " choice
    case "$choice" in
        1) BASE_URL="http://localhost:8080" ;;
        2) BASE_URL="https://api.flashlight-dc.org" ;;
        3) read -rp "  Enter URL: " BASE_URL ;;
        *) echo -e "${RED}Invalid choice${NC}"; exit 1 ;;
    esac
elif [ "$ENV_INPUT" = "local" ]; then
    BASE_URL="http://localhost:8080"
elif [ "$ENV_INPUT" = "hosted" ]; then
    BASE_URL="https://api.flashlight-dc.org"
else
    BASE_URL="$ENV_INPUT"
fi

TIMEOUT=30
PASS=0
FAIL=0
TOTAL=0

divider() {
    echo -e "${CYAN}============================================================${NC}"
}

header() {
    echo ""
    divider
    echo -e "${CYAN}  $1${NC}"
    divider
}

test_get() {
    local label="$1"
    local url="$2"
    local expected_code="${3:-200}"
    TOTAL=$((TOTAL + 1))
    echo -ne "${YELLOW}[TEST $TOTAL]${NC} ${label} ... "
    local http_code
    http_code=$(curl -s -o /tmp/flashlight_test_resp.txt -w "%{http_code}" --max-time "$TIMEOUT" "$url" 2>/dev/null || echo "000")
    if [ "$http_code" = "$expected_code" ]; then
        echo -e "${GREEN}PASS${NC} (HTTP $http_code)"
        PASS=$((PASS + 1))
    else
        echo -e "${RED}FAIL${NC} (expected $expected_code, got $http_code)"
        FAIL=$((FAIL + 1))
        echo -e "       URL: $url"
        local body
        body=$(head -c 200 /tmp/flashlight_test_resp.txt 2>/dev/null || true)
        echo -e "       Body: ${body}"
    fi
}

test_post() {
    local label="$1"
    local url="$2"
    local expected_code="${3:-200}"
    TOTAL=$((TOTAL + 1))
    echo -ne "${YELLOW}[TEST $TOTAL]${NC} ${label} ... "
    local http_code
    http_code=$(curl -s -o /tmp/flashlight_test_resp.txt -w "%{http_code}" -X POST --max-time "$TIMEOUT" "$url" 2>/dev/null || echo "000")
    if [ "$http_code" = "$expected_code" ]; then
        echo -e "${GREEN}PASS${NC} (HTTP $http_code)"
        PASS=$((PASS + 1))
    else
        echo -e "${RED}FAIL${NC} (expected $expected_code, got $http_code)"
        FAIL=$((FAIL + 1))
        echo -e "       URL: $url"
        local body
        body=$(head -c 200 /tmp/flashlight_test_resp.txt 2>/dev/null || true)
        echo -e "       Body: ${body}"
    fi
}

test_summarization() {
    local label="$1"
    local url="$2"
    local bill_id="$3"
    TOTAL=$((TOTAL + 1))
    echo -ne "${YELLOW}[TEST $TOTAL]${NC} ${label} ... "
    local http_code
    http_code=$(curl -s -o /tmp/flashlight_test_resp.txt -w "%{http_code}" --max-time 120 "$url" 2>/dev/null || echo "000")

    if [ "$http_code" != "200" ]; then
        echo -e "${RED}FAIL${NC} (expected 200, got $http_code)"
        FAIL=$((FAIL + 1))
        echo -e "       URL: $url"
        head -c 200 /tmp/flashlight_test_resp.txt
        return
    fi

    # Validate JSON
    if ! python3 -c "import json,sys; json.load(sys.stdin)" < /tmp/flashlight_test_resp.txt 2>/dev/null; then
        echo -e "${RED}FAIL${NC} (invalid JSON)"
        FAIL=$((FAIL + 1))
        head -c 300 /tmp/flashlight_test_resp.txt
        return
    fi

    # Extract fields from JSON response
    local status summary model
    status=$(python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('status',''))" < /tmp/flashlight_test_resp.txt)
    summary=$(python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('summary',''))" < /tmp/flashlight_test_resp.txt)
    model=$(python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('modelUsed','unknown'))" < /tmp/flashlight_test_resp.txt)
    local summary_len=${#summary}

    # Check 1: status field
    if [ "$status" != "SUCCESS" ]; then
        echo -e "${RED}FAIL${NC} (status='$status', expected 'SUCCESS')"
        FAIL=$((FAIL + 1))
        echo -e "       AI call failed or returned an error response"
        head -c 300 /tmp/flashlight_test_resp.txt
        return
    fi

    # Check 2: summary must be non-empty
    if [ -z "$summary" ] || [ "$summary" = "null" ]; then
        echo -e "${RED}FAIL${NC} (summary is empty/null — AI did not generate content)"
        FAIL=$((FAIL + 1))
        return
    fi

    # Check 3: summary must be substantive (not just a few chars of error/placeholder)
    if [ "$summary_len" -lt 200 ]; then
        echo -e "${YELLOW}WARN${NC} (HTTP 200, status=SUCCESS, but summary is only ${summary_len} chars — may be incomplete)"
        PASS=$((PASS + 1))
        echo -e "       modelUsed: $model"
        echo -e "       first 200 chars: $(echo "$summary" | head -c 200)"
        return
    fi

    # All clear
    echo -e "${GREEN}PASS${NC} (HTTP 200, model=$model, summary=${summary_len} chars)"
    PASS=$((PASS + 1))
}

test_get_json() {
    local label="$1"
    local url="$2"
    local expected_code="${3:-200}"
    TOTAL=$((TOTAL + 1))
    echo -ne "${YELLOW}[TEST $TOTAL]${NC} ${label} ... "
    local http_code
    http_code=$(curl -s -o /tmp/flashlight_test_resp.txt -w "%{http_code}" --max-time "$TIMEOUT" "$url" 2>/dev/null || echo "000")
    if [ "$http_code" = "$expected_code" ]; then
        # validate JSON
        if python3 -c "import json,sys; json.load(sys.stdin)" < /tmp/flashlight_test_resp.txt 2>/dev/null; then
            echo -e "${GREEN}PASS${NC} (HTTP $http_code, valid JSON)"
            PASS=$((PASS + 1))
        else
            echo -e "${RED}FAIL${NC} (invalid JSON)"
            FAIL=$((FAIL + 1))
            local body
            body=$(head -c 200 /tmp/flashlight_test_resp.txt 2>/dev/null || true)
            echo -e "       Body: ${body}"
        fi
    else
        echo -e "${RED}FAIL${NC} (expected $expected_code, got $http_code)"
        FAIL=$((FAIL + 1))
        echo -e "       URL: $url"
        local body
        body=$(head -c 200 /tmp/flashlight_test_resp.txt 2>/dev/null || true)
        echo -e "       Body: ${body}"
    fi
}

echo ""
echo -e "${CYAN}  FlashlightDC Backend — Endpoint Test Suite${NC}"
echo -e "${CYAN}  Server: ${BASE_URL}${NC}"
echo ""

# ── Root / Health ──────────────────────────────────────────
header "Root & Health"
test_get_json "GET /api/bills" "$BASE_URL/api/bills?size=5"

# ── Bill Controller: /api/bills ────────────────────────────
header "BillController — Raw API (debug)"
test_get_json "GET /api/bills/raw"                 "$BASE_URL/api/bills/raw?limit=5"
test_get_json "GET /api/bills/raw/119/hr/123"      "$BASE_URL/api/bills/raw/119/hr/123"

header "BillController — Fetch & Persist (POST)"
test_post     "POST /api/bills/fetch"               "$BASE_URL/api/bills/fetch?limit=3"
test_post     "POST /api/bills/fetch/119/hr/123"    "$BASE_URL/api/bills/fetch/119/hr/123"

header "BillController — Read from DB"
test_get_json "GET /api/bills"                      "$BASE_URL/api/bills?size=5"
test_get_json "GET /api/bills/policy-area/Health"   "$BASE_URL/api/bills/policy-area/Health?size=5"
test_get_json "GET /api/bills/stats"                "$BASE_URL/api/bills/stats"

# ── Member Controller: /api/members ────────────────────────
header "MemberController — Raw API (debug)"
test_get_json "GET /api/members/raw"               "$BASE_URL/api/members/raw?limit=5"

header "MemberController — Fetch & Persist (POST)"
test_post     "POST /api/members/fetch"             "$BASE_URL/api/members/fetch?limit=3"

header "MemberController — Read from DB"
test_get_json "GET /api/members"                    "$BASE_URL/api/members"
test_get_json "GET /api/members?party=D"            "$BASE_URL/api/members?party=D"
test_get_json "GET /api/members?state=CA"           "$BASE_URL/api/members?state=CA"

# ── Summarization Controller: /api/v1/bills ────────────────
header "SummarizationController — /api/v1/bills"
test_summarization "GET /api/v1/bills/119/hr/123/summary" \
    "$BASE_URL/api/v1/bills/119/hr/123/summary" \
    "119-hr-123"
test_summarization "GET /api/v1/bills/119/s/1/summary" \
    "$BASE_URL/api/v1/bills/119/s/1/summary" \
    "119-s-1"

# ── Summary ────────────────────────────────────────────────
header "Results"
echo -e "  Passed: ${GREEN}${PASS}${NC} / ${TOTAL}"
if [ "$FAIL" -gt 0 ]; then
    echo -e "  Failed: ${RED}${FAIL}${NC} / ${TOTAL}"
else
    echo -e "  Failed: ${FAIL} / ${TOTAL}"
fi
echo ""

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi

#!/usr/bin/env bash
#
# CAMS — Phase 10 smoke test (Company entity + employer profile linkage)
#
# Credentials come from the environment so that nothing secret lives in this
# file — it is safe to commit. Add to the gitignored .env:
#
#   CAMS_ADMIN_EMAIL=admin@cams.local
#   CAMS_ADMIN_PASSWORD=your-admin-password
#
# An ADMIN account must already exist. To create one: register normally via
# POST /api/auth/register, then promote it in SQL:
#   UPDATE users SET role = 'ADMIN' WHERE email = '...';
#
# Usage:
#   1. Start the backend in another terminal:
#        set -a; source .env; set +a
#        ./mvnw spring-boot:run
#   2. In this terminal, load the same env, then run:
#        set -a; source .env; set +a
#        bash scripts/smoke-test-phase10.sh
#
# Every run creates fresh timestamp-suffixed accounts, so it is safe to re-run
# repeatedly without cleaning the database.

set -u

API=${API:-http://localhost:8080}
ADMIN_EMAIL=${CAMS_ADMIN_EMAIL:-}
ADMIN_PASSWORD=${CAMS_ADMIN_PASSWORD:-}

if [ -z "$ADMIN_EMAIL" ] || [ -z "$ADMIN_PASSWORD" ]; then
    echo "ERROR: CAMS_ADMIN_EMAIL and CAMS_ADMIN_PASSWORD are not set."
    echo "       Add them to .env, then:  set -a; source .env; set +a"
    exit 1
fi

RUN=$(date +%s)
PASS=0
FAIL=0

pass() { echo "  PASS  $1"; PASS=$((PASS + 1)); }
fail() { echo "  FAIL  $1"; FAIL=$((FAIL + 1)); }

check() { # expected_code actual_code label
    if [ "$1" = "$2" ]; then
        pass "$3 (HTTP $2)"
    else
        fail "$3 — expected HTTP $1, got $2"
        echo "        body: $BODY"
    fi
}

BODY=""
CODE=""
req() { # method path token [json_body]
    local method=$1 path=$2 token=$3 data=${4:-}
    local args=(-s -w '\n%{http_code}' -X "$method" "$API$path")
    [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
    [ -n "$data" ] && args+=(-H "Content-Type: application/json" -d "$data")
    local raw
    raw=$(curl "${args[@]}")
    CODE=$(printf '%s' "$raw" | tail -n1)
    BODY=$(printf '%s' "$raw" | sed '$d')
}

jwt_of() { printf '%s' "$BODY" | sed -E 's/.*"token":"([^"]+)".*/\1/'; }
id_of() { printf '%s' "$BODY" | sed -E 's/.*"id":([0-9]+).*/\1/'; }
name_of() { printf '%s' "$BODY" | sed -E 's/.*"name":"([^"]*)".*/\1/'; }
desc_of() { printf '%s' "$BODY" | sed -E 's/.*"description":"([^"]*)".*/\1/'; }

require_jwt() { # token label
    case "$1" in
    eyJ*) pass "$2 returned a JWT" ;;
    *)
        fail "$2 did NOT return a JWT — check credentials"
        echo "        body: $BODY"
        echo
        echo "Aborting: every later step depends on this token."
        exit 1
        ;;
    esac
}

require_num() { # value label
    case "$1" in
    '' | *[!0-9]*)
        fail "$2 — could not parse an id from the response"
        echo "        body: $BODY"
        exit 1
        ;;
    esac
}

COMPANY="Acme Ltd $RUN"
COMPANY_DUP="acme  ltd $RUN" # lowercase + double space: must still collide
EMP_EMAIL="emp1-$RUN@test.com"
EMP2_EMAIL="emp2-$RUN@test.com"
CAND_EMAIL="cand1-$RUN@test.com"

echo "CAMS Phase 10 smoke test  ($API, run $RUN)"
echo

echo "Setup"

req POST /api/auth/register "" "{
  \"email\":\"$EMP_EMAIL\",\"password\":\"password123\",\"fullName\":\"Emp One\",
  \"companyName\":\"$COMPANY\",\"companyDescription\":\"We build widgets\",
  \"companyWebsite\":\"https://acme.example.com\",\"companyLocation\":\"Sofia, Bulgaria\"}"
check 201 "$CODE" "register employer applicant"
EMP=$(jwt_of)
require_jwt "$EMP" "employer registration"

req POST /api/auth/register "" "{
  \"email\":\"$CAND_EMAIL\",\"password\":\"password123\",\"fullName\":\"Cand One\"}"
check 201 "$CODE" "register plain candidate"
CAND=$(jwt_of)
require_jwt "$CAND" "candidate registration"

req POST /api/auth/login "" "{
  \"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}"
check 200 "$CODE" "admin login"
ADMIN=$(jwt_of)
require_jwt "$ADMIN" "admin login"

echo
echo "Approval flow"

req GET /api/employer-requests/me "$EMP"
check 200 "$CODE" "employer sees their own pending request"
REQ_ID=$(id_of)
require_num "$REQ_ID" "employer request id"

req GET /api/companies/me "$EMP"
check 403 "$CODE" "pre-approval: applicant is still a CANDIDATE, blocked from /companies/me"

req POST "/api/admin/employer-requests/$REQ_ID/approve" "$ADMIN"
check 200 "$CODE" "admin approves the request"

echo
echo "Company endpoints"

req GET /api/companies/me "$EMP"
check 200 "$CODE" "employer reads own company  <-- proves @Transactional(readOnly=true) and live role refresh"
COMPANY_ID=$(id_of)
require_num "$COMPANY_ID" "company id"

if [ "$(name_of)" = "$COMPANY" ]; then
    pass "company name copied from the request verbatim"
else
    fail "company name mismatch — expected '$COMPANY', got '$(name_of)'"
fi

req PUT /api/companies/me "$EMP" '{"description":"Updated description"}'
check 200 "$CODE" "employer updates own company"
if [ "$(desc_of)" = "Updated description" ]; then
    pass "description updated"
else
    fail "description not updated — got '$(desc_of)'"
fi
if [ "$(name_of)" = "$COMPANY" ]; then
    pass "name unchanged by update (name is not editable)"
else
    fail "name changed during update — got '$(name_of)'"
fi

req GET "/api/companies/$COMPANY_ID" "$CAND"
check 200 "$CODE" "candidate reads the public company profile"

req GET /api/companies/me "$CAND"
check 403 "$CODE" "candidate blocked from the employer-only route"

echo
echo "Duplicate company name"

req POST /api/auth/register "" "{
  \"email\":\"$EMP2_EMAIL\",\"password\":\"password123\",\"fullName\":\"Emp Two\",
  \"companyName\":\"$COMPANY_DUP\"}"
check 201 "$CODE" "register second applicant with a colliding company name"
EMP2=$(jwt_of)
require_jwt "$EMP2" "second employer registration"

req GET /api/employer-requests/me "$EMP2"
check 200 "$CODE" "second applicant sees their pending request"
REQ2_ID=$(id_of)
require_num "$REQ2_ID" "second employer request id"

req POST "/api/admin/employer-requests/$REQ2_ID/approve" "$ADMIN"
check 409 "$CODE" "duplicate name rejected with a clean 409, not a 500"
case "$BODY" in
*already*) pass "409 carries a readable message" ;;
*) fail "409 message missing or generic — body: $BODY" ;;
esac

echo
echo "-----------------------------------------"
echo "  passed: $PASS    failed: $FAIL"
echo "-----------------------------------------"
[ "$FAIL" -eq 0 ] || exit 1

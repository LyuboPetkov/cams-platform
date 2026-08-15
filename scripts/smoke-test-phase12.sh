#!/usr/bin/env bash
#
# CAMS — Phase 12 smoke test
# (Company/employer linkage from Phase 10, CandidateProfile + the ESCO-seeded
#  Skill vocabulary from Phase 11, plus employer-side JobListing CRUD)
#
# Credentials come from the environment so that nothing secret lives in this
# file — it is safe to commit. Add to the gitignored .env:
#
#   CAMS_ADMIN_EMAIL=admin@cams.local
#   CAMS_ADMIN_PASSWORD=your-admin-password
#   DB_PASSWORD=your-cams_user-password
#
# DB_PASSWORD is the same variable the backend already needs. It is used by one
# check only — the "second employer at the same company" fixture, which no
# endpoint can create yet (adding a colleague to an existing company is still
# an open item from Phase 10). That single check is skipped, not failed, when
# psql or DB_PASSWORD is unavailable.
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
#        bash scripts/smoke-test-phase12.sh
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
SKIP=0

pass() { echo "  PASS  $1"; PASS=$((PASS + 1)); }
fail() { echo "  FAIL  $1"; FAIL=$((FAIL + 1)); }
skip() { echo "  SKIP  $1"; SKIP=$((SKIP + 1)); }

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

# First occurrence of a string field. Profile fields all precede the nested
# "skills" array, so this is not confused by a skill's own "name".
field_of() { printf '%s' "$BODY" | grep -o "\"$1\":\"[^\"]*\"" | head -1 | cut -d: -f2- | tr -d '"'; }

# Just the nested "skills":[...] array of a candidate profile response.
skills_block() { printf '%s' "$BODY" | sed -E 's/.*"skills":\[//; s/\].*//'; }
skills_count() { skills_block | grep -o '"id":[0-9]*' | wc -l | tr -d ' '; }
skills_ids() { skills_block | grep -o '"id":[0-9]*' | grep -o '[0-9]*' | sort -n | tr '\n' ' '; }

# The first N skill ids from a GET /api/skills response.
first_skill_ids() { printf '%s' "$BODY" | grep -o '"id":[0-9]*' | grep -o '[0-9]*' | head -"$1" | tr '\n' ' '; }

# id_of() is greedy and would return a nested company/skill id. A job listing
# response serialises its own id first, so take the first "id" instead.
first_id_of() { printf '%s' "$BODY" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*'; }

# One "postedByUserId" per listing — nested company and skill objects have none.
listings_count() { printf '%s' "$BODY" | grep -o '"postedByUserId":' | wc -l | tr -d ' '; }

# Substring assertions. Preferred over field_of for job listings: the nested
# company object carries its own "location" and "name", so a first-match field
# lookup would read the company's values instead of the listing's.
body_has() { case "$BODY" in *"$1"*) return 0 ;; *) return 1 ;; esac; }

assert_has() { # substring label
    if body_has "$1"; then
        pass "$2"
    else
        fail "$2 — expected to find: $1"
        echo "        body: $BODY"
    fi
}

assert_not_has() { # substring label
    if body_has "$1"; then
        fail "$2 — unexpectedly found: $1"
        echo "        body: $BODY"
    else
        pass "$2"
    fi
}

# psql is only needed for the same-company colleague fixture (see the header).
PSQL=${PSQL:-}
if [ -z "$PSQL" ]; then
    if command -v psql > /dev/null 2>&1; then
        PSQL=psql
    else
        for candidate in "/c/Program Files/PostgreSQL"/*/bin/psql.exe; do
            [ -x "$candidate" ] && PSQL=$candidate
        done
    fi
fi

sql() { # statement
    PGPASSWORD="${DB_PASSWORD:-}" "$PSQL" \
        -h "${DB_HOST:-localhost}" -p "${DB_PORT:-5432}" \
        -U "${DB_USER:-cams_user}" -d "${DB_NAME:-cams_db}" \
        -v ON_ERROR_STOP=1 -tAc "$1"
}

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
COMPANY_B="Globex Ltd $RUN"  # a genuinely different company, for the 403 checks
EMP_EMAIL="emp1-$RUN@test.com"
EMP2_EMAIL="emp2-$RUN@test.com"
EMP3_EMAIL="emp3-$RUN@test.com"       # employer at COMPANY_B
COLLEAGUE_EMAIL="emp1b-$RUN@test.com" # second employer at COMPANY
CAND_EMAIL="cand1-$RUN@test.com"

echo "CAMS Phase 12 smoke test  ($API, run $RUN)"
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
echo "Candidate profile — created eagerly at registration"

req GET /api/candidate-profiles/me "$CAND"
check 200 "$CODE" "fresh registration already has a profile (no lazy creation)"
PROFILE_ID=$(id_of)
require_num "$PROFILE_ID" "candidate profile id"

if [ "$(skills_count)" = "0" ]; then
    pass "new profile starts with no skills"
else
    fail "new profile should have 0 skills, got $(skills_count)"
fi

req GET /api/candidate-profiles/me "$EMP"
check 200 "$CODE" "an approved EMPLOYER still reaches their own profile  <-- proves no CANDIDATE role gate"

echo
echo "Candidate profile — partial update"

req PUT /api/candidate-profiles/me "$CAND" '{
  "headline":"Backend developer","location":"Sofia, Bulgaria",
  "openToRemote":true,"desiredWorkingHours":"FULL_TIME"}'
check 200 "$CODE" "profile update accepts all fields"

req PUT /api/candidate-profiles/me "$CAND" '{"headline":"Senior backend developer"}'
check 200 "$CODE" "profile update with a single field"
if [ "$(field_of headline)" = "Senior backend developer" ]; then
    pass "headline updated"
else
    fail "headline not updated — got '$(field_of headline)'"
fi
if [ "$(field_of location)" = "Sofia, Bulgaria" ]; then
    pass "location left unchanged by partial update"
else
    fail "location clobbered by partial update — got '$(field_of location)'"
fi
case "$BODY" in
*'"openToRemote":true'*) pass "openToRemote survived the partial update" ;;
*) fail "openToRemote lost during partial update — body: $BODY" ;;
esac
case "$BODY" in
*'"desiredWorkingHours":"FULL_TIME"'*) pass "desiredWorkingHours survived the partial update" ;;
*) fail "desiredWorkingHours lost during partial update" ;;
esac

echo
echo "Skill vocabulary"

req GET "/api/skills?search=java" "$CAND"
check 200 "$CODE" "skill search returns matches"
SKILL_IDS=$(first_skill_ids 3)
S1=$(echo "$SKILL_IDS" | cut -d' ' -f1)
S2=$(echo "$SKILL_IDS" | cut -d' ' -f2)
require_num "$S1" "first skill id"
require_num "$S2" "second skill id"
case "$BODY" in
*[Jj]ava*) pass "search results actually match the query" ;;
*) fail "search returned rows that do not contain the query" ;;
esac

req GET "/api/skills?search=zzzznotaskillzzzz" "$CAND"
check 200 "$CODE" "no-match search returns 200 with an empty list"
if [ "$BODY" = "[]" ]; then
    pass "no-match search body is []"
else
    fail "expected [] for a no-match search — got: $BODY"
fi

echo
echo "Candidate skill selection"

req PUT /api/candidate-profiles/me/skills "$CAND" "{\"skillIds\":[$S1,$S2]}"
check 200 "$CODE" "set two skills"
if [ "$(skills_count)" = "2" ]; then
    pass "response reports 2 skills"
else
    fail "expected 2 skills, got $(skills_count)"
fi

req GET /api/candidate-profiles/me "$CAND"
check 200 "$CODE" "re-read profile after setting skills"
if [ "$(skills_count)" = "2" ]; then
    pass "skills persisted across requests"
else
    fail "expected 2 persisted skills, got $(skills_count)"
fi
EXPECTED=$(printf '%s\n%s\n' "$S1" "$S2" | sort -n | tr '\n' ' ')
if [ "$(skills_ids)" = "$EXPECTED" ]; then
    pass "persisted skill ids match what was submitted"
else
    fail "skill ids differ — expected '$EXPECTED', got '$(skills_ids)'"
fi

req PUT /api/candidate-profiles/me/skills "$CAND" "{\"skillIds\":[$S1,$S1,$S1]}"
check 200 "$CODE" "submitting the same skill three times is accepted"
if [ "$(skills_count)" = "1" ]; then
    pass "duplicate ids collapse to a single join row  <-- no unique-constraint 500"
else
    fail "expected 1 skill after a duplicated submission, got $(skills_count)"
fi

req PUT /api/candidate-profiles/me/skills "$CAND" '{"skillIds":[]}'
check 200 "$CODE" "empty list clears the selection"
if [ "$(skills_count)" = "0" ]; then
    pass "selection cleared"
else
    fail "expected 0 skills after clearing, got $(skills_count)"
fi

req PUT /api/candidate-profiles/me/skills "$CAND" '{"skillIds":[999999999]}'
check 404 "$CODE" "unknown skill id gives a clean 404, not a 500"
case "$BODY" in
*999999999*) pass "404 names the offending id" ;;
*) fail "404 message does not identify the bad id — body: $BODY" ;;
esac

req PUT /api/candidate-profiles/me/skills "$CAND" '{}'
check 400 "$CODE" "missing skillIds is a validation error, not a 500"

echo
echo "Job listings — fixtures"

# A second, unrelated company: its employer must be refused access to COMPANY's
# listings with a 403.
req POST /api/auth/register "" "{
  \"email\":\"$EMP3_EMAIL\",\"password\":\"password123\",\"fullName\":\"Emp Three\",
  \"companyName\":\"$COMPANY_B\",\"companyLocation\":\"Varna, Bulgaria\"}"
check 201 "$CODE" "register an applicant for a second company"
EMP3=$(jwt_of)
require_jwt "$EMP3" "second-company registration"

req GET /api/employer-requests/me "$EMP3"
check 200 "$CODE" "second-company applicant sees their pending request"
REQ3_ID=$(id_of)
require_num "$REQ3_ID" "second-company employer request id"

req POST "/api/admin/employer-requests/$REQ3_ID/approve" "$ADMIN"
check 200 "$CODE" "admin approves the second company"

# A colleague at COMPANY. No endpoint can create this yet, so promote a plain
# registration directly in SQL. Roles and company come from the database on
# every request (JwtAuthFilter reloads the UserDetails), so the token issued at
# registration starts acting as an employer immediately.
req POST /api/auth/register "" "{
  \"email\":\"$COLLEAGUE_EMAIL\",\"password\":\"password123\",\"fullName\":\"Emp One Colleague\"}"
check 201 "$CODE" "register the colleague account"
COLLEAGUE=$(jwt_of)
require_jwt "$COLLEAGUE" "colleague registration"

COLLEAGUE_READY=no
if [ -z "$PSQL" ]; then
    skip "promote the colleague to EMPLOYER at the same company — no psql found (set PSQL=/path/to/psql)"
elif [ -z "${DB_PASSWORD:-}" ]; then
    skip "promote the colleague to EMPLOYER at the same company — DB_PASSWORD is not set"
elif sql "UPDATE users SET role = 'EMPLOYER', company_id = $COMPANY_ID WHERE email = '$COLLEAGUE_EMAIL';" > /dev/null 2>&1; then
    COLLEAGUE_READY=yes
    pass "colleague promoted to EMPLOYER at company $COMPANY_ID"
else
    fail "could not promote the colleague via psql — check DB_HOST/DB_PORT/DB_USER/DB_NAME/DB_PASSWORD"
fi

echo
echo "Job listings — creation"

TITLE1="Backend developer $RUN"
TITLE2="Summer intern $RUN"
LISTING_LOCATION="Plovdiv, Bulgaria" # deliberately not the company's own location

req POST /api/job-listings "$EMP" "{
  \"title\":\"$TITLE1\",\"description\":\"Build and maintain our Spring services.\",
  \"location\":\"$LISTING_LOCATION\",\"remote\":true,\"level\":\"MID\",
  \"skillIds\":[$S1,$S2]}"
check 201 "$CODE" "employer creates a listing with skills"
LISTING_ID=$(first_id_of)
require_num "$LISTING_ID" "job listing id"
assert_has "\"name\":\"$COMPANY\"" "response nests the employer's own company"
assert_has '"status":"OPEN"' "new listing is created OPEN"
assert_has "\"location\":\"$LISTING_LOCATION\"" "listing location is its own, not the company's"
if [ "$(skills_count)" = "2" ]; then
    pass "response lists both required skills"
else
    fail "expected 2 skills on the new listing, got $(skills_count)"
fi

req POST /api/job-listings "$EMP" "{
  \"title\":\"$TITLE2\",\"description\":\"No structured skills required.\",
  \"remote\":false,\"level\":\"INTERN\"}"
check 201 "$CODE" "a listing with no skills at all is allowed"
LISTING2_ID=$(first_id_of)
require_num "$LISTING2_ID" "second job listing id"
if [ "$(skills_count)" = "0" ]; then
    pass "zero-skill listing really has zero skills"
else
    fail "expected 0 skills, got $(skills_count)"
fi

req POST /api/job-listings "$EMP" '{"title":"Missing the required fields"}'
check 400 "$CODE" "description, remote and level are required"

req POST /api/job-listings "$CAND" "{
  \"title\":\"Candidate should not post\",\"description\":\"x\",
  \"remote\":false,\"level\":\"MID\"}"
check 403 "$CODE" "candidate blocked from creating listings (role gate)"

echo
echo "Job listings — reads scoped to the owning company"

req GET /api/job-listings/mine "$EMP"
check 200 "$CODE" "employer lists their company's listings"
MINE_COUNT=$(listings_count)
if [ "$MINE_COUNT" = "2" ]; then
    pass "both listings returned"
else
    fail "expected 2 listings in /mine, got $MINE_COUNT"
fi
assert_has "$TITLE1" "/mine includes the first listing"
assert_has "$TITLE2" "/mine includes the zero-skill listing"

req GET "/api/job-listings/$LISTING_ID" "$EMP"
check 200 "$CODE" "poster reads their own listing"

if [ "$COLLEAGUE_READY" = "yes" ]; then
    req GET "/api/job-listings/$LISTING_ID" "$COLLEAGUE"
    check 200 "$CODE" "colleague at the SAME company reads the listing  <-- proves company-scoped, not poster-scoped"

    req GET /api/job-listings/mine "$COLLEAGUE"
    check 200 "$CODE" "colleague sees the company's listings in /mine"
    if [ "$(listings_count)" = "2" ]; then
        pass "colleague sees both company listings"
    else
        fail "expected 2 listings for the colleague, got $(listings_count)"
    fi

    req PUT "/api/job-listings/$LISTING_ID" "$COLLEAGUE" '{"description":"Edited by a colleague."}'
    check 200 "$CODE" "colleague can edit a listing they did not post"
else
    skip "colleague read/edit of a same-company listing (fixture unavailable)"
fi

req GET "/api/job-listings/$LISTING_ID" "$EMP3"
check 403 "$CODE" "employer at a DIFFERENT company is refused (403 from the company check)"

req GET "/api/job-listings/$LISTING_ID" "$CAND"
check 403 "$CODE" "candidate is refused (403 from the SecurityConfig role gate, not the company check)"

req GET /api/job-listings/mine "$EMP3"
check 200 "$CODE" "the other company's /mine works"
if [ "$(listings_count)" = "0" ]; then
    pass "the other company's /mine is empty — no cross-company leakage"
else
    fail "expected 0 listings for the other company, got $(listings_count)"
fi

req GET "/api/job-listings/999999999" "$EMP"
check 404 "$CODE" "unknown listing id is a 404, not a 403"

echo
echo "Job listings — partial update"

req PUT "/api/job-listings/$LISTING_ID" "$EMP" "{\"title\":\"Senior backend developer $RUN\"}"
check 200 "$CODE" "update with a single field"
assert_has "\"title\":\"Senior backend developer $RUN\"" "title updated"
assert_has "\"location\":\"$LISTING_LOCATION\"" "location left unchanged by the partial update"
assert_has '"remote":true' "remote survived the partial update"
assert_has '"level":"MID"' "level survived the partial update"
if [ "$(skills_count)" = "2" ]; then
    pass "skills survived the partial update"
else
    fail "expected 2 skills after the update, got $(skills_count)"
fi

req PUT "/api/job-listings/$LISTING_ID" "$EMP" '{"status":"ARCHIVED","remote":false}'
check 200 "$CODE" "a status field in the edit body is ignored, not honoured"
assert_has '"status":"OPEN"' "status is NOT settable through the general edit endpoint"
assert_has '"remote":false' "the legitimate field in that same body still applied"

req PUT "/api/job-listings/$LISTING_ID" "$EMP3" '{"title":"Hijacked"}'
check 403 "$CODE" "employer at another company cannot edit the listing"

echo
echo "Job listings — required skills"

req PUT "/api/job-listings/$LISTING_ID/skills" "$EMP" "{\"skillIds\":[$S1]}"
check 200 "$CODE" "replace the skill set with a single skill"
if [ "$(skills_count)" = "1" ]; then
    pass "replaced, not appended"
else
    fail "expected 1 skill after the replace, got $(skills_count)"
fi

req PUT "/api/job-listings/$LISTING_ID/skills" "$EMP" "{\"skillIds\":[$S1,$S1,$S2]}"
check 200 "$CODE" "duplicate ids in one request are accepted"
if [ "$(skills_count)" = "2" ]; then
    pass "duplicate ids collapse to one join row each  <-- no unique-constraint 500"
else
    fail "expected 2 skills, got $(skills_count)"
fi

req PUT "/api/job-listings/$LISTING_ID/skills" "$EMP" '{"skillIds":[]}'
check 200 "$CODE" "empty list clears the required skills"
if [ "$(skills_count)" = "0" ]; then
    pass "skills cleared"
else
    fail "expected 0 skills after clearing, got $(skills_count)"
fi

req PUT "/api/job-listings/$LISTING_ID/skills" "$EMP" '{"skillIds":[999999999]}'
check 404 "$CODE" "unknown skill id gives a clean 404, not a 500"
assert_has "999999999" "404 names the offending id"

req PUT "/api/job-listings/$LISTING_ID/skills" "$EMP" '{}'
check 400 "$CODE" "missing skillIds is a validation error"

req PUT "/api/job-listings/$LISTING_ID/skills" "$EMP3" "{\"skillIds\":[$S1]}"
check 403 "$CODE" "employer at another company cannot set the required skills"

req POST /api/job-listings "$EMP" "{
  \"title\":\"Rollback probe $RUN\",\"description\":\"Should never be persisted.\",
  \"remote\":false,\"level\":\"JUNIOR\",\"skillIds\":[999999999]}"
check 404 "$CODE" "creating with a bogus skill id is rejected"
req GET /api/job-listings/mine "$EMP"
check 200 "$CODE" "re-read /mine after the rejected creation"
if [ "$(listings_count)" = "2" ]; then
    pass "the rejected creation rolled back — no orphan listing"
else
    fail "expected 2 listings after the rollback, got $(listings_count)"
fi
assert_not_has "Rollback probe $RUN" "the rolled-back listing is absent from /mine"

echo
echo "Job listings — archive"

req POST "/api/job-listings/$LISTING2_ID/archive" "$EMP3"
check 403 "$CODE" "employer at another company cannot archive the listing"

req POST "/api/job-listings/$LISTING2_ID/archive" "$EMP"
check 200 "$CODE" "employer archives their own listing"
assert_has '"status":"ARCHIVED"' "status flipped to ARCHIVED"

req GET "/api/job-listings/$LISTING2_ID" "$EMP"
check 200 "$CODE" "archived listing is still readable"
assert_has '"status":"ARCHIVED"' "archived status persisted"

req POST "/api/job-listings/$LISTING2_ID/archive" "$EMP"
check 409 "$CODE" "archiving an already-archived listing is a 409"
assert_has "already" "409 carries a readable message"

req GET /api/job-listings/mine "$EMP"
check 200 "$CODE" "/mine after archiving"
if [ "$(listings_count)" = "2" ]; then
    pass "/mine still returns archived listings (employer view is unfiltered)"
else
    fail "expected 2 listings in /mine after archiving, got $(listings_count)"
fi

req POST "/api/job-listings/999999999/archive" "$EMP"
check 404 "$CODE" "archiving an unknown listing is a 404"

echo
echo "-----------------------------------------"
echo "  passed: $PASS    failed: $FAIL    skipped: $SKIP"
echo "-----------------------------------------"
[ "$FAIL" -eq 0 ] || exit 1

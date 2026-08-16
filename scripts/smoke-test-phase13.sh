#!/usr/bin/env bash
#
# CAMS — Phase 13 smoke test
# (Company/employer linkage from Phase 10, CandidateProfile + the ESCO-seeded
#  Skill vocabulary from Phase 11, employer-side JobListing CRUD from Phase 12,
#  plus the candidate-facing browse + filters added in Phase 13)
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
#        bash scripts/smoke-test-phase13.sh
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

# --- Phase 13: paged browse helpers ---------------------------------------
#
# A browse response is a PageResponse envelope, so listings_count() cannot be
# reused: JobListingBrowseResponse has no "postedByUserId" (that omission is
# the whole point of the DTO, and is asserted separately below).
#
# A listing serialises "id" immediately followed by "title", while the nested
# company and skill objects serialise "id" followed by "name" — so this pattern
# picks out listing ids only, without needing a JSON parser.
browse_ids() { printf '%s' "$BODY" | grep -o '"id":[0-9]*,"title"' | grep -o '[0-9]*' | sort -n | tr '\n' ' '; }
browse_count() { printf '%s' "$BODY" | grep -o '"id":[0-9]*,"title"' | wc -l | tr -d ' '; }

# A numeric field of the page envelope: page, size, totalElements, totalPages.
page_field() { printf '%s' "$BODY" | grep -o "\"$1\":[0-9]*" | head -1 | grep -o '[0-9]*'; }

assert_page_field() { # field expected label
    local actual
    actual=$(page_field "$1")
    if [ "$actual" = "$2" ]; then
        pass "$3 ($1=$actual)"
    else
        fail "$3 — expected $1=$2, got '$actual'"
        echo "        body: $BODY"
    fi
}

assert_count() { # expected label
    if [ "$(browse_count)" = "$1" ]; then
        pass "$2 ($1 listing(s))"
    else
        fail "$2 — expected $1 listing(s), got $(browse_count)"
        echo "        body: $BODY"
    fi
}

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

echo "CAMS Phase 13 smoke test  ($API, run $RUN)"
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
echo "Browse — access tier"

# This pair is what proves the SecurityConfig matcher ordering is right. Browse
# and create share the exact path /api/job-listings and are separated only by
# the HTTP method, so a reordering of the two matcher lines would either 403 the
# candidate here or let them post a listing.
req GET /api/job-listings "$CAND"
check 200 "$CODE" "candidate browses listings  <-- GET carve-out sits above the EMPLOYER rule"

req POST /api/job-listings "$CAND" "{
  \"title\":\"Candidate still may not post\",\"description\":\"x\",
  \"remote\":false,\"level\":\"MID\"}"
check 403 "$CODE" "candidate still blocked from POST on the SAME path  <-- the GET carve-out is method-scoped"

req GET /api/job-listings/mine "$CAND"
check 403 "$CODE" "candidate still blocked from /mine"

req GET /api/job-listings "$EMP"
check 200 "$CODE" "employer browses too — browse is not candidate-only"

# The point of this check is that browse is NOT permitAll. It asserts 403
# rather than 401 because that is what this application actually returns for a
# missing token, on every route, and has since Phase 1: no AuthenticationEntry-
# Point is configured, and Spring Security falls back to
# Http403ForbiddenEntryPoint when neither httpBasic nor formLogin is present.
# Browse is no different from any other protected endpoint here — deliberately
# left alone, since changing it is an app-wide security change, not this phase's.
req GET /api/job-listings ""
check 403 "$CODE" "browse rejects an anonymous caller — it is authenticated, not public"

req GET /api/job-listings "not-a-real-jwt"
check 403 "$CODE" "browse rejects a garbage token too"

echo
echo "Browse — fixtures"

# Every browse assertion has to survive a shared database that already holds
# listings from previous runs. Two devices make that possible: a run-unique
# location tag, so a location filter isolates exactly this run's fixtures, and
# presence/absence assertions on run-unique titles rather than global counts.
BTAG="bt$RUN"
PTAG="pg$RUN"

B1_TITLE="Browse Sofia Junior $RUN"
B2_TITLE="Browse Plovdiv Senior $RUN"
B3_TITLE="Browse Remote Nowhere $RUN"
B4_TITLE="Browse Varna CrossCompany $RUN"
B5_TITLE="Browse Archived $RUN"

req POST /api/job-listings "$EMP" "{
  \"title\":\"$B1_TITLE\",\"description\":\"Onsite junior role.\",
  \"location\":\"Sofia-$BTAG\",\"remote\":false,\"level\":\"JUNIOR\",
  \"skillIds\":[$S1]}"
check 201 "$CODE" "fixture B1 — Sofia, onsite, JUNIOR, skill S1"
B1_ID=$(first_id_of)
require_num "$B1_ID" "B1 id"

req POST /api/job-listings "$EMP" "{
  \"title\":\"$B2_TITLE\",\"description\":\"Remote senior role.\",
  \"location\":\"Plovdiv-$BTAG\",\"remote\":true,\"level\":\"SENIOR\",
  \"skillIds\":[$S2]}"
check 201 "$CODE" "fixture B2 — Plovdiv, remote, SENIOR, skill S2"
B2_ID=$(first_id_of)
require_num "$B2_ID" "B2 id"

# No location at all: a remote-only posting. It must never match a location
# filter, and must still be reachable via ?remote=true.
req POST /api/job-listings "$EMP" "{
  \"title\":\"$B3_TITLE\",\"description\":\"Fully remote, no office.\",
  \"remote\":true,\"level\":\"MID\"}"
check 201 "$CODE" "fixture B3 — no location, remote, MID"
B3_ID=$(first_id_of)
require_num "$B3_ID" "B3 id"

# Posted by the OTHER company: browse is platform-wide, so this must be visible
# to everyone, including company A's employer.
req POST /api/job-listings "$EMP3" "{
  \"title\":\"$B4_TITLE\",\"description\":\"Posted by the second company.\",
  \"location\":\"Varna-$BTAG\",\"remote\":false,\"level\":\"MID\"}"
check 201 "$CODE" "fixture B4 — posted by the OTHER company"
B4_ID=$(first_id_of)
require_num "$B4_ID" "B4 id"

req POST /api/job-listings "$EMP" "{
  \"title\":\"$B5_TITLE\",\"description\":\"Will be archived immediately.\",
  \"location\":\"Sofia-$BTAG\",\"remote\":false,\"level\":\"JUNIOR\"}"
check 201 "$CODE" "fixture B5 — to be archived"
B5_ID=$(first_id_of)
require_num "$B5_ID" "B5 id"

req POST "/api/job-listings/$B5_ID/archive" "$EMP"
check 200 "$CODE" "archive fixture B5"

echo
echo "Browse — OPEN listings only"

req GET "/api/job-listings?location=$BTAG&size=50" "$CAND"
check 200 "$CODE" "browse this run's tagged listings"
assert_has "$B1_TITLE" "the open Sofia listing is returned"
assert_has "$B2_TITLE" "the open Plovdiv listing is returned"
assert_not_has "$B5_TITLE" "the ARCHIVED listing is absent  <-- status=OPEN is baked into the query"
assert_count 3 "exactly B1, B2 and B4 match the run tag"

req GET /api/job-listings/mine "$EMP"
check 200 "$CODE" "employer /mine after the browse fixtures"
assert_has "$B5_TITLE" "/mine still shows the archived listing  <-- the asymmetry with browse is deliberate"

echo
echo "Browse — cross-company visibility"

req GET "/api/job-listings?location=$BTAG&size=50" "$EMP"
check 200 "$CODE" "company A's employer browses"
assert_has "$B4_TITLE" "company A sees company B's listing  <-- /mine scoping does not leak into browse"

req GET "/api/job-listings?location=$BTAG&size=50" "$EMP3"
check 200 "$CODE" "company B's employer browses"
assert_has "$B1_TITLE" "company B sees company A's listing"

echo
echo "Browse — DTO shape"

req GET "/api/job-listings?location=$BTAG&size=50" "$CAND"
check 200 "$CODE" "browse for the DTO comparison"
assert_not_has '"postedByUserId"' "postedByUserId is absent from browse results  <-- the only field separating the two DTOs"
assert_has '"description"' "description IS present — browse is the only place a candidate can read it"
assert_has '"skills"' "skills are present"
assert_has '"company"' "the nested company is present"

req GET "/api/job-listings/$B1_ID" "$EMP"
check 200 "$CODE" "employer detail endpoint for the same listing"
assert_has '"postedByUserId"' "postedByUserId IS still present in GET /{id}"

echo
echo "Browse — filters in isolation"

# Each filter on its own. Assertions are presence/absence of this run's titles
# rather than counts, because an unscoped browse also returns older listings.
req GET "/api/job-listings?location=Plovdiv-$BTAG" "$CAND"
check 200 "$CODE" "location filter alone"
assert_has "$B2_TITLE" "location matches the Plovdiv listing"
assert_not_has "$B1_TITLE" "location excludes the Sofia listing"

req GET "/api/job-listings?location=SOFIA-$BTAG" "$CAND"
check 200 "$CODE" "location filter is case-insensitive"
assert_has "$B1_TITLE" "uppercase SOFIA still matches 'Sofia-$BTAG'"

req GET "/api/job-listings?remote=true&size=50" "$CAND"
check 200 "$CODE" "remote filter alone"
assert_has "$B2_TITLE" "remote=true includes the remote listing"
assert_has "$B3_TITLE" "remote=true includes the location-less remote listing"
assert_not_has "$B1_TITLE" "remote=true excludes the onsite listing"

req GET "/api/job-listings?level=SENIOR&size=50" "$CAND"
check 200 "$CODE" "level filter alone"
assert_has "$B2_TITLE" "level=SENIOR includes the senior listing"
assert_not_has "$B1_TITLE" "level=SENIOR excludes the junior listing"

req GET "/api/job-listings?skillIds=$S2&size=50" "$CAND"
check 200 "$CODE" "skill filter alone"
assert_has "$B2_TITLE" "skillIds=S2 includes the listing requiring S2"
assert_not_has "$B1_TITLE" "skillIds=S2 excludes the listing requiring only S1"

echo
echo "Browse — AND across dimensions"

req GET "/api/job-listings?location=$BTAG&remote=true&level=SENIOR" "$CAND"
check 200 "$CODE" "three filters combined"
assert_has "$B2_TITLE" "the listing matching all three is returned"
assert_not_has "$B1_TITLE" "a listing matching only location is not returned"
assert_not_has "$B4_TITLE" "a listing matching location but not remote is not returned"
assert_count 1 "exactly one listing satisfies all three filters"

req GET "/api/job-listings?location=$BTAG&level=INTERN" "$CAND"
check 200 "$CODE" "a combination nothing satisfies"
assert_has '"content":[]' "empty content, not an error"
assert_page_field totalElements 0 "totalElements is zero"

echo
echo "Browse — skill semantics"

# B1 requires S1 only and B2 requires S2 only, so a query naming both must
# return BOTH. If this ever returns neither, the filter has regressed from
# ANY-of to ALL-of, which at a 10,805-concept vocabulary means "almost always
# zero results" rather than a visible error.
req GET "/api/job-listings?skillIds=$S1&skillIds=$S2&location=$BTAG" "$CAND"
check 200 "$CODE" "two skill ids, neither listing having both"
assert_has "$B1_TITLE" "the S1-only listing is returned  <-- ANY-of, not ALL-of"
assert_has "$B2_TITLE" "the S2-only listing is returned  <-- ANY-of, not ALL-of"
assert_count 2 "both listings match, so the filter is an OR"

# Deliberately different from PUT /{id}/skills, where an unknown id is a 404:
# a filter is a query, so an id that matches nothing simply matches nothing.
req GET "/api/job-listings?skillIds=999999999" "$CAND"
check 200 "$CODE" "unknown skill id in a filter is NOT a 404"
assert_page_field totalElements 0 "unknown skill id matches nothing"

echo
echo "Browse — remote tristate"

# The first of these three is what catches a regression from Boolean to
# primitive boolean: with a primitive, an absent parameter binds to false and
# every remote listing silently disappears from an unfiltered browse.
req GET "/api/job-listings?location=$BTAG&size=50" "$CAND"
check 200 "$CODE" "remote omitted entirely"
assert_has "$B1_TITLE" "an omitted remote returns onsite listings"
assert_has "$B2_TITLE" "an omitted remote ALSO returns remote listings  <-- Boolean, not boolean"

req GET "/api/job-listings?location=$BTAG&remote=true" "$CAND"
check 200 "$CODE" "remote=true"
assert_has "$B2_TITLE" "remote=true returns the remote listing"
assert_not_has "$B1_TITLE" "remote=true excludes onsite listings"
assert_count 1 "only the remote listing matches"

req GET "/api/job-listings?location=$BTAG&remote=false" "$CAND"
check 200 "$CODE" "remote=false"
assert_has "$B1_TITLE" "remote=false returns the onsite listing"
assert_not_has "$B2_TITLE" "remote=false excludes remote listings"
assert_count 2 "both onsite listings match"

echo
echo "Browse — listings with no location"

# Intended behaviour, not a bug to fix: a listing with location IS NULL can
# never match a location substring, and is reached through ?remote=true.
req GET "/api/job-listings?location=$BTAG&size=50" "$CAND"
check 200 "$CODE" "location filter with a location-less listing on the platform"
assert_not_has "$B3_TITLE" "the location-less listing never matches a location filter"

req GET "/api/job-listings?remote=true&size=50" "$CAND"
check 200 "$CODE" "the same listing via remote=true"
assert_has "$B3_TITLE" "the location-less listing IS reachable through remote=true"

echo
echo "Browse — pagination"

# Created in a tight loop on purpose: created_at defaults to NOW(), so these
# rows tie on the sort column, which is exactly the case that makes the id
# tiebreaker load-bearing. Without it, OFFSET paging duplicates and skips rows.
PAGE_FIXTURES=5
i=1
while [ "$i" -le "$PAGE_FIXTURES" ]; do
    req POST /api/job-listings "$EMP" "{
      \"title\":\"Paging $i $RUN\",\"description\":\"Pagination fixture.\",
      \"location\":\"Page-$PTAG\",\"remote\":false,\"level\":\"MID\"}"
    if [ "$CODE" != "201" ]; then
        fail "could not create pagination fixture $i (HTTP $CODE)"
        echo "        body: $BODY"
        break
    fi
    i=$((i + 1))
done
pass "created $PAGE_FIXTURES pagination fixtures in a tight loop"

req GET "/api/job-listings?location=$PTAG&size=2&page=0" "$CAND"
check 200 "$CODE" "page 0 of 5 results at size 2"
assert_page_field totalElements 5 "totalElements counts every match, not just this page"
assert_page_field totalPages 3 "totalPages is ceil(5/2)"
assert_page_field page 0 "page echoes back as 0"
assert_page_field size 2 "size echoes back as 2"
assert_count 2 "page 0 holds two listings"
assert_has '"first":true' "page 0 is flagged first"
assert_has '"last":false' "page 0 is not flagged last"
PAGE0_IDS=$(browse_ids)

req GET "/api/job-listings?location=$PTAG&size=2&page=1" "$CAND"
check 200 "$CODE" "page 1"
assert_count 2 "page 1 holds two listings"
assert_has '"first":false' "page 1 is not flagged first"
assert_has '"last":false' "page 1 is not flagged last"
PAGE1_IDS=$(browse_ids)

# The tiebreaker assertion: with only created_at in the ORDER BY, tied rows can
# appear on both pages or on neither.
OVERLAP=""
for id in $PAGE0_IDS; do
    case " $PAGE1_IDS " in *" $id "*) OVERLAP="$OVERLAP $id" ;; esac
done
if [ -z "$OVERLAP" ]; then
    pass "pages 0 and 1 share no ids  <-- the createdAt+id tiebreaker gives a total ordering"
else
    fail "pages 0 and 1 both contain:$OVERLAP — the sort is not a total ordering"
    echo "        page 0: $PAGE0_IDS"
    echo "        page 1: $PAGE1_IDS"
fi

req GET "/api/job-listings?location=$PTAG&size=2&page=2" "$CAND"
check 200 "$CODE" "page 2, the last page"
assert_count 1 "page 2 holds the remaining listing"
assert_has '"last":true' "page 2 is flagged last"

req GET "/api/job-listings?location=$PTAG&page=9" "$CAND"
check 200 "$CODE" "a page past the end is empty, not an error"
assert_has '"content":[]' "content is empty on an out-of-range page"
assert_page_field totalElements 5 "totalElements is still correct past the end"

echo
echo "Browse — page/size clamping"

# Clamped rather than rejected: PageRequest.of(-1, ...) throws outright, and an
# unbounded size makes ?size=100000 a full-table dump.
req GET "/api/job-listings?location=$PTAG&size=1000" "$CAND"
check 200 "$CODE" "an oversized size is accepted"
assert_page_field size 50 "size is capped at 50, not honoured as 1000"

req GET "/api/job-listings?location=$PTAG&page=-1" "$CAND"
check 200 "$CODE" "a negative page is a 200, not a 500"
assert_page_field page 0 "a negative page clamps to 0"

req GET "/api/job-listings?location=$PTAG&size=0" "$CAND"
check 200 "$CODE" "size=0 is a 200, not a 500"
assert_page_field size 1 "size=0 clamps up to 1"
assert_count 1 "one listing returned at the clamped size"

echo
echo "Browse — unparseable parameters"

# All three used to be 500s: @ControllerAdvice resolvers run ahead of Spring's
# DefaultHandlerExceptionResolver, so the catch-all swallowed them.
req GET "/api/job-listings?level=NOTALEVEL" "$CAND"
check 400 "$CODE" "an unknown enum value is a 400, not a 500"
assert_has "level" "the 400 names the offending parameter"

req GET "/api/job-listings?page=abc" "$CAND"
check 400 "$CODE" "a non-numeric page is a 400, not a 500"

req GET "/api/job-listings?skillIds=x" "$CAND"
check 400 "$CODE" "a non-numeric skill id is a 400, not a 500"

# Latent since Phase 1, fixed by the same handler.
req GET "/api/job-listings/abc" "$EMP"
check 400 "$CODE" "a non-numeric path variable is a 400, not a 500"

echo
echo "-----------------------------------------"
echo "  passed: $PASS    failed: $FAIL    skipped: $SKIP"
echo "-----------------------------------------"
[ "$FAIL" -eq 0 ] || exit 1

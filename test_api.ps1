
$baseUrl = "http://127.0.0.1:8080/api"

function Test-Endpoint {
    param(
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers = @{},
        [string]$Body = $null,
        [string]$Description
    )

    Write-Host "Testing: $Description ($Method $Url)" -ForegroundColor Cyan
    try {
        $params = @{
            Method = $Method
            Uri = $Url
            Headers = $Headers
            ContentType = "application/json"
        }
        if ($Body) {
            $params.Body = $Body
        }

        $response = Invoke-RestMethod @params
        Write-Host "Success!" -ForegroundColor Green
        return $response
    } catch {
        Write-Host "Failed!" -ForegroundColor Red
        Write-Host $_.Exception.Message
        if ($_.Exception.Response) {
             $reader = New-Object System.IO.StreamReader $_.Exception.Response.GetResponseStream()
             $responseBody = $reader.ReadToEnd()
             Write-Host "Response Body: $responseBody"
        }
        return $null
    }
}

# 1. Login
$studentBody = '{ "username": "student", "password": "123456" }'
$studentLogin = Test-Endpoint -Method POST -Url "$baseUrl/auth/login" -Body $studentBody -Description "Login Student"
if (-not $studentLogin) { exit }
$studentToken = $studentLogin.data.token
$studentId = $studentLogin.data.user.id
$studentHeaders = @{ "Authorization" = "Bearer $studentToken" }

$adminBody = '{ "username": "admin", "password": "123456" }'
$adminLogin = Test-Endpoint -Method POST -Url "$baseUrl/auth/login" -Body $adminBody -Description "Login Admin"
if (-not $adminLogin) { exit }
$adminToken = $adminLogin.data.token
$adminHeaders = @{ "Authorization" = "Bearer $adminToken" }

# 2. Club Lifecycle
# Create
$clubBody = '{
    "name": "AI Club",
    "description": "Artificial Intelligence",
    "category": "TECH",
    "foundedYear": 2024
}'
$club = Test-Endpoint -Method POST -Url "$baseUrl/clubs" -Headers $studentHeaders -Body $clubBody -Description "Create Club"
if (-not $club) { exit }
$clubId = $club.data.id
Write-Host "Created Club ID: $clubId" -ForegroundColor Yellow

# Approve
Test-Endpoint -Method POST -Url "$baseUrl/admin/clubs/$clubId/approve" -Headers $adminHeaders -Description "Approve Club"

# Search
Test-Endpoint -Method GET -Url "$baseUrl/clubs?keyword=AI" -Description "Search Club 'AI'"

# Update
$updateBody = '{
    "name": "Super AI Club",
    "description": "Advanced AI",
    "category": "TECH",
    "logoUrl": "http://example.com/logo.png"
}'
Test-Endpoint -Method PUT -Url "$baseUrl/clubs/$clubId" -Headers $studentHeaders -Body $updateBody -Description "Update Club"

# 3. Recruitment Flow
# Create Batch
$batchBody = @"
{
    "club": { "id": $clubId },
    "title": "Spring 2026 Recruitment",
    "description": "Join us!",
    "startTime": "2026-01-01T00:00:00",
    "endTime": "2026-12-31T00:00:00"
}
"@
$batch = Test-Endpoint -Method POST -Url "$baseUrl/recruit/batches" -Headers $studentHeaders -Body $batchBody -Description "Create Recruit Batch"
if (-not $batch) { exit }
$batchId = $batch.data.id
Write-Host "Created Batch ID: $batchId" -ForegroundColor Yellow

# Submit Application
$appBody = @"
{
    "batch": { "id": $batchId },
    "applyData": "{\"reason\": \"I want to join!\"}"
}
"@
$app = Test-Endpoint -Method POST -Url "$baseUrl/recruit/applications" -Headers $studentHeaders -Body $appBody -Description "Submit Application"

# List Applications to get ID
$apps = Test-Endpoint -Method GET -Url "$baseUrl/recruit/applications?batchId=$batchId" -Headers $studentHeaders -Description "List Applications"
if (-not $apps -or -not $apps.data) { exit }
$appId = $apps.data[0].id
Write-Host "Application ID: $appId" -ForegroundColor Yellow

# Final Review (Pass) -> Auto Member
Test-Endpoint -Method POST -Url "$baseUrl/recruit/applications/$appId/final-review?pass=true&comment=Welcome" -Headers $studentHeaders -Description "Final Review (Pass)"

# Verify Member
$members = Test-Endpoint -Method GET -Url "$baseUrl/clubs/$clubId/members" -Headers $studentHeaders -Description "List Members"
if ($members.data.Count -ge 1) {
    Write-Host "Member verification passed: " $members.data.Count -ForegroundColor Green
} else {
    Write-Host "Member verification FAILED" -ForegroundColor Red
}

# 4. Activity Flow
# Create Activity
$actBody = @"
{
    "club": { "id": $clubId },
    "title": "AI Workshop",
    "description": "Learn LLMs",
    "type": "WORKSHOP",
    "location": "Room 101",
    "startTime": "2026-02-01T10:00:00",
    "endTime": "2026-02-01T12:00:00"
}
"@
$act = Test-Endpoint -Method POST -Url "$baseUrl/activities" -Headers $studentHeaders -Body $actBody -Description "Create Activity"
if (-not $act) { exit }
$actId = $act.data.id
Write-Host "Activity ID: $actId" -ForegroundColor Yellow

# Signup
Test-Endpoint -Method POST -Url "$baseUrl/activities/$actId/signup" -Headers $studentHeaders -Description "Signup Activity"

# Signin
Test-Endpoint -Method POST -Url "$baseUrl/activities/$actId/signin" -Headers $studentHeaders -Description "Signin Activity"


# 5. Notice
# Create Notice
$noticeBody = @"
{
    "clubId": $clubId,
    "title": "Meeting Tomorrow",
    "content": "Do not be late",
    "scope": "CLUB"
}
"@
Test-Endpoint -Method POST -Url "$baseUrl/notices" -Headers $studentHeaders -Body $noticeBody -Description "Create Notice"

# List
Test-Endpoint -Method GET -Url "$baseUrl/notices?clubId=$clubId" -Headers $studentHeaders -Description "List Club Notices"

# 6. New Features Verification

# 6.1 Stats
Test-Endpoint -Method GET -Url "$baseUrl/stats/system" -Headers $adminHeaders -Description "System Stats (Admin)"
Test-Endpoint -Method GET -Url "$baseUrl/stats/club/$clubId" -Headers $studentHeaders -Description "Club Stats"

# 6.2 User Profile
$profileBody = '{ "realName": "SuperStudent" }'
Test-Endpoint -Method PUT -Url "$baseUrl/users/me" -Headers $studentHeaders -Body $profileBody -Description "Update Profile"

# 6.3 My Activities
Test-Endpoint -Method GET -Url "$baseUrl/users/me/activities" -Headers $studentHeaders -Description "Get My Activities"

# 6.4 Member Role Management
# Update self role to VICE_PRESIDENT.
Test-Endpoint -Method PUT -Url "$baseUrl/clubs/$clubId/members/$studentId/role?role=VICE_PRESIDENT" -Headers $studentHeaders -Description "Update Member Role"

# 6.5 Notice Delete
# Fetch notices first
$notices = Test-Endpoint -Method GET -Url "$baseUrl/notices?clubId=$clubId" -Headers $studentHeaders -Description "Get Notices for Delete"
if ($notices.data.Count -gt 0) {
    $noticeId = $notices.data[0].id
    Test-Endpoint -Method DELETE -Url "$baseUrl/notices/$noticeId" -Headers $studentHeaders -Description "Delete Notice"
}

# 6.6 Remove Member (Last step as it removes permission)
Test-Endpoint -Method DELETE -Url "$baseUrl/clubs/$clubId/members/$studentId" -Headers $studentHeaders -Description "Remove Member (Self)"

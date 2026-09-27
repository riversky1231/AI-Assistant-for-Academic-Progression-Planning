param([string]$BaseUrl = 'http://127.0.0.1:18080')

$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
$health = Invoke-RestMethod -Uri "$BaseUrl/health" -TimeoutSec 15
if ($health.code -ne 0) { throw 'Health endpoint failed' }

$testUsername = 'docker_test_' + [Guid]::NewGuid().ToString('N').Substring(0, 12)
$testPassword = [Guid]::NewGuid().ToString('N')
$registrationJson = @{ username = $testUsername; password = $testPassword } | ConvertTo-Json -Compress
$registration = Invoke-RestMethod -Uri "$BaseUrl/auth/register" -Method Post -ContentType 'application/json' -Body $registrationJson -TimeoutSec 30
if ($registration.code -ne 0) { throw 'Registration failed' }
$actualPermissions = @($registration.data.permissions | Sort-Object)
if (($actualPermissions -join ',') -ne 'recommend:use,school:read') { throw 'Unexpected default account permissions' }
$headers = @{}
$headers[$registration.data.token_name] = $registration.data.token_value

try {
    $me = Invoke-RestMethod -Uri "$BaseUrl/auth/me" -Headers $headers -TimeoutSec 15
    if ((($me.data.permissions | Sort-Object) -join ',') -ne 'recommend:use,school:read') { throw 'Persisted permissions do not match registration' }
    $schools = Invoke-RestMethod -Uri "$BaseUrl/schools?limit=3" -Headers $headers -TimeoutSec 15
    if ($schools.code -ne 0 -or @($schools.data).Count -eq 0) { throw 'Seeded school query failed' }
    $schoolName = -join [char[]](0x53a6, 0x95e8, 0x5927, 0x5b66)
    $schoolSearch = Invoke-RestMethod -Uri ("$BaseUrl/schools?keyword=" + [Uri]::EscapeDataString($schoolName)) -Headers $headers -TimeoutSec 15
    if ($schoolSearch.code -ne 0 -or @($schoolSearch.data | Where-Object { $_.id -eq 1 }).Count -ne 1) { throw 'Chinese school search failed; check seed import encoding' }
    $province = -join [char[]](0x798f, 0x5efa)
    $subjectType = -join [char[]](0x7269, 0x7406, 0x7c7b)
    $recommendationJson = @{ province = $province; subject_type = $subjectType; score = 580; rank = 15000 } | ConvertTo-Json -Compress
    $recommendation = Invoke-RestMethod -Uri "$BaseUrl/recommend" -Method Post -Headers $headers -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($recommendationJson)) -TimeoutSec 30
    if ($recommendation.code -ne 0 -or $null -eq $recommendation.data.recommendations) { throw 'Recommendation failed' }
    $recommendationCount = 0
    foreach ($group in $recommendation.data.recommendations.PSObject.Properties) { $recommendationCount += @($group.Value).Count }
    if ($recommendationCount -eq 0) { throw 'Seeded recommendation unexpectedly empty; check Chinese province and subject encoding' }
    $denied = $false
    try { $null = Invoke-RestMethod -Uri "$BaseUrl/auth/users" -Headers $headers -TimeoutSec 15 }
    catch {
        if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 403) { $denied = $true }
        else { throw }
    }
    if (!$denied) { throw 'Ordinary account unexpectedly accessed account administration' }
    Write-Output 'PASS: health, registration, stored permissions, school query, recommendation and admin denial.'
    Write-Output "Created smoke-test account: $testUsername"
} finally {
    $null = Invoke-RestMethod -Uri "$BaseUrl/auth/logout" -Method Post -Headers $headers -TimeoutSec 15
}

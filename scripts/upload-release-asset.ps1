param(
    [Parameter(Mandatory = $true)]
    [string] $Repo,

    [Parameter(Mandatory = $true)]
    [string] $Tag,

    [Parameter(Mandatory = $true)]
    [string] $AssetPath,

    [string] $AssetName = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($AssetName)) {
    $AssetName = Split-Path -Leaf $AssetPath
}

if (-not (Test-Path -LiteralPath $AssetPath)) {
    throw "Asset file was not found: $AssetPath"
}

$credentialText = "protocol=https`nhost=github.com`n`n" | git credential fill 2>$null
$username = (($credentialText | Select-String "^username=").Line -replace "^username=", "")
$password = (($credentialText | Select-String "^password=").Line -replace "^password=", "")

if ([string]::IsNullOrWhiteSpace($username) -or [string]::IsNullOrWhiteSpace($password)) {
    throw "GitHub credential was not available. Sign in with Git for Windows, then run this again."
}

$basicAuth = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($username + ":" + $password))
$headers = @{
    "Accept" = "application/vnd.github+json"
    "Authorization" = "Basic " + $basicAuth
    "User-Agent" = "CreateWorkOrderProms-release-helper"
    "X-GitHub-Api-Version" = "2022-11-28"
}

$release = $null
try {
    $release = Invoke-RestMethod -Uri "https://api.github.com/repos/$Repo/releases/tags/$Tag" -Headers $headers
} catch {
    if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 404) {
        $body = @{
            tag_name = $Tag
            name = $Tag
            draft = $false
            prerelease = $false
        } | ConvertTo-Json
        $release = Invoke-RestMethod -Method Post -Uri "https://api.github.com/repos/$Repo/releases" -Headers $headers -Body $body -ContentType "application/json"
    } else {
        throw
    }
}

$assets = Invoke-RestMethod -Uri $release.assets_url -Headers $headers
$existing = $assets | Where-Object { $_.name -eq $AssetName } | Select-Object -First 1
if ($existing) {
    Invoke-RestMethod -Method Delete -Uri $existing.url -Headers $headers | Out-Null
}

$uploadUrl = ($release.upload_url -replace "\{\?name,label\}$", "") + "?name=" + [uri]::EscapeDataString($AssetName)
$result = Invoke-RestMethod -Method Post -Uri $uploadUrl -Headers $headers -ContentType "application/zip" -InFile $AssetPath

Write-Host ("Uploaded {0} ({1} bytes) to {2}" -f $result.name, $result.size, $Tag)

# Check WXML structure without third-party dependencies; not a WeChat compiler.
$ErrorActionPreference = 'Stop'
$markupRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../miniprogram'))
$markupFiles = @(Get-ChildItem -LiteralPath $markupRoot -Filter '*.wxml' -Recurse -File)
if ($markupFiles.Count -eq 0) { throw 'No WXML templates found' }

foreach ($markupFile in $markupFiles) {
    $markupText = Get-Content -LiteralPath $markupFile.FullName -Raw -Encoding UTF8
    # Expressions may contain XML operators; validate the surrounding markup.
    $markupText = [regex]::Replace($markupText, '(?s)\{\{.*?\}\}', 'expression')
    # WXML permits boolean attributes such as loading and wx:else.
    $markupText = [regex]::Replace($markupText, '\s(wx:else|loading)(?=\s|/?>)(?!\s*=)', ' $1="true"')
    try {
        $null = [xml]('<root xmlns:wx="urn:wx">' + $markupText + '</root>')
    } catch {
        throw ('Invalid WXML in ' + $markupFile.FullName + ': ' + $_.Exception.Message)
    }
}
Write-Output ('WXML structure passed: ' + $markupFiles.Count + ' templates')

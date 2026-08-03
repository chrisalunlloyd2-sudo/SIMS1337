# SIMS1337 Memory-Mapped File Reader — zero-copy shared RAM read
# Usage: powershell -File scripts/mmf_reader.ps1

using namespace System.IO.MemoryMappedFiles

$mmfName = "SIMS1337_SharedMem"

try {
    $mmf = [MemoryMappedFile]::OpenExisting($mmfName)
    $stream = $mmf.CreateViewStream()
    $reader = [System.IO.StreamReader]::new($stream)
    $data = $reader.ReadLine()
    Write-Host "[SIMS1337] MMF read: $data"

    # Forward to dashboard
    if ($data) {
        try {
            $body = $data  # already JSON
            Invoke-RestMethod -Uri "http://localhost:8899/api/pipe" -Method POST -Body $body -ContentType "application/json" -TimeoutSec 5 | Out-Null
            Write-Host "[SIMS1337] Forwarded to dashboard"
        } catch {
            Write-Host "[SIMS1337] Dashboard forward failed: $_"
        }
    }

    $reader.Dispose()
    $stream.Dispose()
    $mmf.Dispose()
} catch {
    Write-Host "[SIMS1337] MMF not found or read error: $_"
}

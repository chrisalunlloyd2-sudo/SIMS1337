# SIMS1337 Memory-Mapped File (MMF) — zero-copy shared RAM
# Writer: creates a 1MB shared memory region and writes payload
# Reader: opens existing region and reads instantly
# Usage:
#   powershell -File scripts/mmf_writer.ps1 "your payload here"
#   powershell -File scripts/mmf_reader.ps1

param([string]$Payload = "SIMS1337 shared state")

using namespace System.IO.MemoryMappedFiles

$mmfName = "SIMS1337_SharedMem"
$mmfSize = 1024 * 1024  # 1MB

# WRITER MODE
$mmf = [MemoryMappedFile]::CreateOrOpen($mmfName, $mmfSize)
$stream = $mmf.CreateViewStream()
$writer = [System.IO.StreamWriter]::new($stream)
$writer.BaseStream.SetLength(0)  # clear previous
$json = [System.Text.Json.JsonSerializer]::Serialize(@{
    payload = $Payload
    timestamp = (Get-Date -Format "o")
    pid = $PID
    source = "SIMS1337_MMF"
})
$writer.WriteLine($json)
$writer.Flush()
$writer.Dispose()
$stream.Dispose()
$mmf.Dispose()
Write-Host "[SIMS1337] MMF written: $($json.Length) chars"

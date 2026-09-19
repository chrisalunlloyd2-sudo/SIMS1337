# PowerShell named pipe listener for SIMS1337
# Listens on PSCustomPipe for incoming bytes from external processes.
# Usage: powershell -File scripts/pipe_listener.ps1

$pipeName = "PSCustomPipe"
$pipeServer = [System.IO.Pipes.NamedPipeServerStream]::new(
    $pipeName,
    [System.IO.Pipes.PipeDirection]::In,
    1,
    [System.IO.Pipes.PipeTransmissionMode]::Byte,
    [System.IO.Pipes.PipeOptions]::Asynchronous
)

Write-Host "[SIMS1337] Pipe listener starting on: $pipeName"
Write-Host "[SIMS1337] Waiting for connection..."

$pipeServer.WaitForConnection()
Write-Host "[SIMS1337] Client connected."

$reader = [System.IO.StreamReader]::new($pipeServer)
$buffer = New-Object byte[] 4096

while ($pipeServer.IsConnected) {
    try {
        $bytesRead = $pipeServer.Read($buffer, 0, $buffer.Length)
        if ($bytesRead -gt 0) {
            $message = [System.Text.Encoding]::UTF8.GetString($buffer, 0, $bytesRead)
            Write-Host "[SIMS1337] Received: $message"

            # Forward to dashboard
            try {
                $body = @{message=$message; timestamp=(Get-Date -Format "o")} | ConvertTo-Json
                Invoke-RestMethod -Uri "http://localhost:8899/api/pipe" -Method POST -Body $body -ContentType "application/json" -TimeoutSec 5 | Out-Null
            } catch {
                Write-Host "[SIMS1337] Dashboard forward failed: $_"
            }
        }
    } catch {
        if ($pipeServer.IsConnected) {
            Write-Host "[SIMS1337] Read error: $_"
        }
        break
    }
    Start-Sleep -Milliseconds 100
}

$reader.Close()
$pipeServer.Close()
Write-Host "[SIMS1337] Pipe listener stopped."

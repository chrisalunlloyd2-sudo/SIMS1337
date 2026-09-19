@echo off
REM SIMS1337 Terminal CLI — Batch wrapper for Windows
REM Usage: sims <command> [args]
REM Calls bash version under the hood, or falls back to direct curl

set SCRIPT_DIR=%~dp0
bash "%SCRIPT_DIR%sims" %*

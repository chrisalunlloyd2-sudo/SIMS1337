#!/usr/bin/env python3
"""TTS readout — speaks text via Windows SAPI (no API key needed)."""
import sys, subprocess

def speak(text):
    # Use PowerShell's System.Speech for TTS on Windows
    ps = f'''
Add-Type -AssemblyName System.Speech
$s = New-Object System.Speech.Synthesis.SpeechSynthesizer
$s.Rate = 2
$s.Speak("{text.replace('"', '""')}")
'''
    subprocess.run(["powershell", "-NoProfile", "-Command", ps],
                   capture_output=True, timeout=30)

if __name__ == "__main__":
    text = " ".join(sys.argv[1:]) if len(sys.argv) > 1 else "No text provided"
    speak(text)

param([string]$Voice = 'Microsoft Maria Desktop')
$ErrorActionPreference = 'Stop'
$taskRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$taskVideoDir = Join-Path $taskRoot 'target/dashboard-video'
New-Item -ItemType Directory -Path $taskVideoDir -Force | Out-Null
Add-Type -AssemblyName System.Speech
$taskSynth = New-Object System.Speech.Synthesis.SpeechSynthesizer
try {
    $taskSynth.SelectVoice($Voice)
    $taskSynth.Rate = 4
    $taskScenes = Get-Content -LiteralPath (Join-Path $taskRoot 'docs/dashboard/video-roteiro.json') -Raw -Encoding UTF8 | ConvertFrom-Json
    for ($taskIndex = 0; $taskIndex -lt $taskScenes.Count; $taskIndex++) {
        $taskSynth.SetOutputToWaveFile((Join-Path $taskVideoDir ('audio-{0:d2}.wav' -f $taskIndex)))
        $taskSynth.Speak($taskScenes[$taskIndex].narracao)
        $taskSynth.SetOutputToNull()
    }
} finally { $taskSynth.Dispose() }
python (Join-Path $PSScriptRoot 'generate-demo.py')
if ($LASTEXITCODE -ne 0) { throw 'Falha ao gerar vídeo.' }

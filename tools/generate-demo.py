"""Monta a apresentação narrada a partir de capturas REAIS já verificadas.

Dependências opcionais para regerar: pip install pillow imageio-ffmpeg
No Windows: powershell -File tools/generate-demo.ps1
"""
import json
import subprocess
import wave
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
from imageio_ffmpeg import get_ffmpeg_exe

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "target/dashboard-video"
EVIDENCE = ROOT / "docs/dashboard/evidencias"
SCENES = json.loads((ROOT / "docs/dashboard/video-roteiro.json").read_text(encoding="utf-8"))
OUT.mkdir(parents=True, exist_ok=True)
FONT = Path("C:/Windows/Fonts/segoeui.ttf")
BOLD = Path("C:/Windows/Fonts/seguisb.ttf")
title_font = ImageFont.truetype(str(BOLD), 34)
caption_font = ImageFont.truetype(str(FONT), 22)
small_font = ImageFont.truetype(str(FONT), 17)
ffmpeg = get_ffmpeg_exe()
duration = 0
segments = []
for index in range(len(SCENES)):
    with wave.open(str(OUT / f"audio-{index:02}.wav")) as wav:
        duration += wav.getnframes() / wav.getframerate() + 1
if duration >= 180:
    raise SystemExit(f"Vídeo excede 3 minutos ({duration:.1f}s). Reduza o roteiro.")
for index, scene in enumerate(SCENES):
    audio = OUT / f"audio-{index:02}.wav"
    with wave.open(str(audio)) as wav:
        seconds = wav.getnframes() / wav.getframerate() + 1
    canvas = Image.new("RGB", (1600, 900), "#142035")
    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle((44, 30, 51, 73), radius=3, fill="#ed7044")
    draw.text((70, 28), scene["titulo"], font=title_font, fill="white")
    draw.text((1450, 36), f"{index+1:02} / {len(SCENES):02}", font=small_font, fill="#a9b5c9")
    screenshot = Image.open(EVIDENCE / scene["imagem"]).convert("RGB")
    screenshot.thumbnail((1504, 694), Image.Resampling.LANCZOS)
    canvas.paste(screenshot, ((1600-screenshot.width)//2, 100+(694-screenshot.height)//2))
    draw.text((70, 821), scene["legenda"], font=caption_font, fill="#e6eaf0")
    draw.text((70, 862), "Pediu Patrão · Capturas da aplicação local · Dados demonstrativos · Narração sintética", font=small_font, fill="#91a1b9")
    frame = OUT / f"frame-{index:02}.png"
    canvas.save(frame)
    segment = OUT / f"segment-{index:02}.mp4"
    subprocess.run([ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-loop", "1", "-framerate", "12",
                    "-i", str(frame), "-i", str(audio), "-t", str(seconds), "-af", "apad=pad_dur=1",
                    "-c:v", "libx264", "-preset", "fast", "-tune", "stillimage", "-pix_fmt", "yuv420p",
                    "-crf", "24", "-c:a", "aac", "-b:a", "96k", str(segment)], check=True)
    segments.append(segment)
manifest = OUT / "concat.txt"
manifest.write_text("\n".join(f"file '{segment.as_posix()}'" for segment in segments), encoding="utf-8")
output = ROOT / "docs/dashboard/demonstracao.mp4"
subprocess.run([ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-f", "concat", "-safe", "0",
                "-i", str(manifest), "-c", "copy", "-movflags", "+faststart", str(output)], check=True)
(EVIDENCE / "video-metadata.json").write_text(json.dumps({"duracaoSegundos": round(duration, 2),
    "resolucao": "1600x900", "cenas": len(SCENES), "narracao": "Microsoft Maria Desktop · pt-BR · sintética",
    "origemImagens": "Capturas reais do dashboard em execução com MongoDB no perfil demo"}, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
print(f"Vídeo: {output.name}, {duration:.1f} segundos, {output.stat().st_size/1024/1024:.1f} MB")

#!/usr/bin/env python3
"""
Transcripción local de audio con faster-whisper (Whisper optimizado para CPU).

Uso:
    python3 whisper_transcribe.py <audio> --model small --language es

Imprime la transcripción por stdout (es lo que lee WhisperTranscripcionService).
Los diagnósticos van por stderr.

Instalación (ya incluida en el Dockerfile del backend):
    pip install faster-whisper
    apt-get install ffmpeg   # decodificación de m4a/aac/ogg/etc.
"""
import argparse
import sys


def main() -> int:
    parser = argparse.ArgumentParser(description="Transcribe audio con faster-whisper")
    parser.add_argument("audio", help="Ruta del archivo de audio")
    parser.add_argument("--model", default="small",
                        help="Modelo Whisper: tiny|base|small|medium|large-v3 (default: small)")
    parser.add_argument("--language", default="es", help="Idioma del audio (default: es)")
    args = parser.parse_args()

    try:
        from faster_whisper import WhisperModel
    except ImportError:
        print("faster-whisper no está instalado. Ejecuta: pip install faster-whisper", file=sys.stderr)
        return 2

    try:
        # int8: mejor rendimiento en CPU sin GPU, calidad prácticamente igual.
        model = WhisperModel(args.model, device="cpu", compute_type="int8")
        segments, info = model.transcribe(
            args.audio,
            language=args.language,
            vad_filter=True,  # filtra silencios largos de la reunión
        )
        print(f"Idioma detectado: {info.language} (p={info.language_probability:.2f})", file=sys.stderr)

        partes = []
        for seg in segments:
            partes.append(seg.text.strip())

        print(" ".join(partes).strip())
        return 0
    except Exception as e:  # noqa: BLE001
        print(f"Error transcribiendo: {e}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())

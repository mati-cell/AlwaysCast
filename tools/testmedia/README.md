# Test Media & Cast Sender Harness

Harness developed in Phase 0 to measure the actual media compatibility envelope of the 1st Generation Chromecast (2013, firmware `CrKey/1.36.157768`) running the Default Media Receiver (`CC1AD845`).

## Purpose

1. **Synthetic Media Generation**: Generates 9 test video samples targeting different codecs, containers, audio formats, HLS packaging, and bitrate envelopes using `ffmpeg`.
2. **HTTP Range Server**: Local streaming server (`server.js`) that logs every HTTP request (headers, byte ranges, user-agent) and implements RFC 7233 byte-range serving (`206 Partial Content`) required for seeking and container demuxing.
3. **Web Cast Sender**: A browser testbench (`sender.html`) using the Google Cast API (`chrome.cast.*`) to cast each sample directly to the receiver, record playback status, and export diagnostic logs.

## Test Matrix

| ID | Description | Container / Codecs | Expected / Observed Behavior |
|----|-------------|-------------------|------------------------------|
| `01` | H.264 + AAC (faststart) | MP4 (moov at start) | Plays instantly with smooth seeking. |
| `02` | H.264 + AAC (no faststart) | MP4 (moov at end) | Plays; TV device issues byte-range requests for the end of the file to read index before streaming. |
| `03` | H.264 + AC3 audio | MP4 | Plays if TV or audio receiver supports Dolby Digital passthrough. |
| `04` | VP8 + Vorbis | WebM | Supported natively by Chromium receiver. |
| `05` | HLS plain (unencrypted) | `.m3u8` + TS segments | Plays natively via receiver HLS parser. |
| `06` | HLS with AES-128 | `.m3u8` + TS + `enc.key` | Plays natively; receiver fetches encryption key and decrypts segments in real-time. |
| `07` | H.264 + AAC in Matroska | MKV | Supported natively by receiver demuxer (no remuxing required). |
| `08` | 1080p @ 12 Mbps (High Bitrate) | MP4 | Tests bandwidth ceiling and hardware decoder limits of the SoC. |
| `09` | HEVC / H.265 | MP4 | **Unsupported**; Gen 1 hardware lacks HEVC hardware decoder (rejected by app with `UnsupportedReason.HEVC`). |

## Prerequisites

- **PowerShell** 5.1 or 7+ (Windows)
- **ffmpeg** installed and available in `PATH`
- **Node.js** (v18+)
- **Google Chrome** (Chromium browser with Cast extension / Cast capability enabled)

## Usage

### 1. Generate Media Samples

Run the PowerShell generation script:

```powershell
cd tools/testmedia
powershell -ExecutionPolicy Bypass -File .\gen-testmedia.ps1
```

Generated video files will be stored in `tools/testmedia/media/` (ignored by git).

### 2. Start the Streaming Server

```bash
node server.js
```

The server outputs LAN IP addresses and listens on port `8099` (or `PORT` environment variable).

### 3. Open Cast Sender

Open Google Chrome and navigate to:
```
http://localhost:8099/sender.html
```

Click **Castear** next to any media sample to stream it directly to your Chromecast, or copy the media URL to test casting from the AlwaysCast Android application.

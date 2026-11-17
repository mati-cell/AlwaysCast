# Genera el set de medios de la fase 0 (matriz de compatibilidad del Chromecast gen1).
# Descartable. Requiere ffmpeg en el PATH.

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$media = Join-Path $root 'media'
if (-not (Test-Path $media)) {
    New-Item -ItemType Directory -Force -Path $media | Out-Null
}

$src = @('-f', 'lavfi', '-i', 'testsrc2=size=1280x720:rate=30', '-f', 'lavfi', '-i', 'sine=frequency=440:sample_rate=48000', '-t', '20')

function New-Dir($p) { New-Item -ItemType Directory -Force -Path $p | Out-Null }

Write-Host '01 h264+aac faststart (720p)'
& ffmpeg -y -loglevel error @src -c:v libx264 -profile:v high -level:v 4.0 -preset ultrafast -b:v 3M -pix_fmt yuv420p -g 60 `
    -c:a aac -b:a 128k -movflags +faststart "$media\01-h264-aac-faststart.mp4"

Write-Host '02 h264+aac sin faststart (moov al final)'
& ffmpeg -y -loglevel error @src -c:v libx264 -profile:v high -level:v 4.0 -preset ultrafast -b:v 3M -pix_fmt yuv420p -g 60 `
    -c:a aac -b:a 128k "$media\02-h264-aac-nofaststart.mp4"

Write-Host '03 h264+ac3'
& ffmpeg -y -loglevel error -i "$media\01-h264-aac-faststart.mp4" -c:v copy -c:a ac3 -b:a 192k "$media\03-h264-ac3.mp4"

Write-Host '04 vp8+vorbis'
& ffmpeg -y -loglevel error @src -c:v libvpx -b:v 2M -deadline realtime -cpu-used 8 -c:a libvorbis -b:a 128k "$media\04-vp8-vorbis.webm"

Write-Host '05 hls sin cifrar'
New-Dir "$media\05-hls-plain"
& ffmpeg -y -loglevel error -i "$media\01-h264-aac-faststart.mp4" -c copy -f hls -hls_time 4 -hls_playlist_type vod `
    -hls_segment_filename "$media\05-hls-plain\seg%03d.ts" "$media\05-hls-plain\index.m3u8"

Write-Host '06 hls con aes-128'
New-Dir "$media\06-hls-aes"
$keyPath = "$media\06-hls-aes\enc.key"
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$key = New-Object byte[] 16
$rng.GetBytes($key)
[System.IO.File]::WriteAllBytes($keyPath, $key)
$iv = New-Object byte[] 16
$rng.GetBytes($iv)
$ivHex = ($iv | ForEach-Object { $_.ToString('x2') }) -join ''
$keyInfo = "$media\06-hls-aes\keyinfo.txt"
@('enc.key', $keyPath, $ivHex) | Set-Content -Path $keyInfo -Encoding ascii
& ffmpeg -y -loglevel error -i "$media\01-h264-aac-faststart.mp4" -c copy -f hls -hls_time 4 -hls_playlist_type vod `
    -hls_key_info_file $keyInfo -hls_segment_filename "$media\06-hls-aes\seg%03d.ts" "$media\06-hls-aes\index.m3u8"

Write-Host '07 mkv h264+aac'
& ffmpeg -y -loglevel error -i "$media\01-h264-aac-faststart.mp4" -c copy "$media\07-h264-aac.mkv"

Write-Host '08 1080p 12 Mbps (prueba de ancho de banda)'
& ffmpeg -y -loglevel error -f lavfi -i 'testsrc2=size=1920x1080:rate=30' -f lavfi -i 'sine=frequency=440:sample_rate=48000' -t 20 `
    -vf 'noise=alls=18:allf=t' -c:v libx264 -profile:v high -level:v 4.0 -preset ultrafast `
    -b:v 12M -minrate 12M -maxrate 12M -bufsize 24M -pix_fmt yuv420p -g 60 -c:a aac -b:a 128k `
    -movflags +faststart "$media\08-1080p-12mbps.mp4"

Write-Host '09 hevc (se espera que NO reproduzca en gen1)'
& ffmpeg -y -loglevel error @src -c:v libx265 -preset ultrafast -b:v 2M -pix_fmt yuv420p -c:a aac -b:a 128k "$media\09-hevc.mp4"

Write-Host ''
Write-Host 'Generado en media/:'
Get-ChildItem $media -Recurse -File | Where-Object { $_.Extension -ne '.ps1' -and $_.Extension -ne '.js' -and $_.Extension -ne '.html' -and $_.Extension -ne '.txt' } |
    Select-Object @{n = 'Archivo'; e = { $_.FullName.Replace($media, '') } }, @{n = 'MB'; e = { [math]::Round($_.Length / 1MB, 2) } } |
    Format-Table -AutoSize

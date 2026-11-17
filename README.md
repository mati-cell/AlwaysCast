# AlwaysCast

Cast web video to a first-generation Chromecast (2013) from an Android phone.

The 2013 Chromecast still works as a TV. What it lost is the software layer between
"the video I want to watch" and "the screen in front of me". AlwaysCast is that layer:
paste a video URL, the app serves it through a local proxy that injects the headers the
origin requires, and sends it to the Default Media Receiver.

## Status

Early development. See `docs/` for the design and the implementation plan.

## What it does not do

- DRM (Widevine, PlayReady)
- HEVC/H.265, AV1, 4K
- Media served from `blob:` URLs (MSE players without a source URL)
- Transcoding

## License

Apache-2.0. See `LICENSE`.

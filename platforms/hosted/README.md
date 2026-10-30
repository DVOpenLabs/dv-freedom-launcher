# VIDAA and Vizio hosted pilots

Independent DV Freedom adapters reuse our own web presentation, profile storage, explicit settings
Apply/Cancel, icon sizing/spacing, focus navigation and silent-media lifecycle. No third-party
launcher renderer, vendor binary, key or themed artwork is included. The native Tizen sources remain
unchanged; the staging tool adapts that maintained renderer behind a separate companion transport.

Start the same [Setup GUI](../../setup_app/README.md), select your platform and follow
[the all-platform guide](../../setup_app/ALL-PLATFORMS.md). Both paths are experimental and need a
computer serving the page while in use. A prepared web page is not proof of installation, automatic
Home routing or native hardware compatibility. No Home watcher or root service is included.

## Connection material

Never put real profiles, certificates, keys, tokens or addresses in this repository or shared logs.
GUI fields take absolute local paths. Example field names below are format documentation, not working
credentials. These files must come from a setup you trust and are authorized to use.

### VIDAA: existing paired profile

```json
{
  "clientId": "YOUR_EXISTING_CLIENT_ID",
  "topicId": "YOUR_EXISTING_PAIRED_TOPIC_ID",
  "username": "YOUR_CURRENT_MQTT_USERNAME",
  "password": "YOUR_CURRENT_MQTT_PASSWORD",
  "certificate": "/absolute/private/client-cert.pem",
  "key": "/absolute/private/client-key.pem",
  "ca": "/absolute/private/tv-ca.pem",
  "fingerprint": "VERIFIED_TV_LEAF_CERTIFICATE_SHA256"
}
```

The existing pairing must already authorize app-list and launch operations. Client keys must be
unencrypted PEM files kept privately; the pilot does not prompt for key passwords. The trusted CA
must validate the TV's current certificate; its SHA-256 pin identifies the exact endpoint. A shared
vendor certificate/private key is deliberately not bundled. Newer firmware's rotating/dynamic
authentication is not generated here; without a working current profile this path stops before Home
creation. Do not claim compatibility from a broker connection alone.

Paho MQTT 2.1.0 checks the certificate chain and endpoint pin before the login packet. It subscribes
only to this paired client's needed app-list/state topics, never wildcard remote traffic. Its scoped
keepalive thread stops on disconnect, and there is no automatic reconnect, background Home polling,
key injection, power, source, volume or system-setting mutation API. An app is launched only from its
captured payload behind an opaque server ID. A send acknowledgement does not prove it opened.

### Vizio: trusted certificate and TV PIN

Choose a trusted public TV leaf certificate in the GUI to prepare this trust profile, or use an
existing equivalent file:

```json
{
  "ca": "/absolute/private/trusted-tv-cert.pem",
  "fingerprint": "VERIFIED_TV_LEAF_CERTIFICATE_SHA256"
}
```

For a certificate signed by a private CA, an advanced existing profile can point `ca` to the trusted
CA bundle while pinning the TV leaf. The GUI's single-certificate helper is for an already-trusted
public certificate, not certificate discovery or trust on first contact. It does not ignore expired
certificates or provide an insecure fallback.

Setup starts the normal paired SmartCast handshake on port 7345, shows the four-digit PIN field and
keeps its auth token in memory only. Open a regular TV app and capture its actual `/app/current`
identifier/namespace, then label it. The page can launch only the captured selection. **Open hosted
Home** separately requests the normal Conjure route (`17`, namespace `4`). Internal/privileged app
`3001`, root services, auth-token bypasses and firmware modifications are excluded. Conjure availability
and current-app response shape vary by firmware and need real-device testing.

## Build and test

`platforms.hosted.prepare_launcher.prepare(output, 'vidaa' or 'vizio', media=optional_folder)` stages
a web page using the existing source packager and notices. Staging does not serve, pair or install it.
The Setup GUI owns runtime serving, single-use review, session revocation and separate launch consent.

```sh
python -m unittest discover -s setup_app/tests -v
node platforms/tizen/test-launcher.cjs
node platforms/webos/test-launcher.cjs
```

No hardware is contacted by these tests. Hardware acceptance remains separate. Protocol references and source pins are in
[the source register](../../product/sources.json); these projects are references, not bundled dependencies.

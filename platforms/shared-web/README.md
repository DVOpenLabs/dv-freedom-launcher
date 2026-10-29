# Portable web policies

`core.js` holds DV Freedom's existing ES5 validation, local profiles, draft and revision-guarded store.
`create()` defaults to the original LG settings/schema/storage key. The LG wrapper retains its public
PalmServiceBridge boundary and portable-v1 webOS mapping importer. Samsung opts into its own key,
eight-favorite limit and icon/spacing/media settings; no Android or portable-v1 contract is extended.

`media.js` provides the Samsung pilot's single silent local-video controller, with bounded local paths,
foreground-time playlist accounting and stale-promise protection. LG's existing renderer/media path is
unchanged. These are independent project sources; neither file calls a platform or network service.

Staging tools copy shared files into each package so a deployed app never depends on repository-relative
paths. Run the existing LG checks and the Samsung policy/lifecycle tests after modifying this layer.

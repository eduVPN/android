# Changelog

## 1.2.4 (...)
- drop 2FA support in Android app until new server release in Q1 where 2FA 
  moves to the browser

## 1.2.3 (2018-06-28)
- fix Gradle build (#165)
- immediately show "add provider" page when starting the app and no
  providers were added before for Let's Connect! flavor
- show "Profiles" instead of "Institute Access" for Let's Connect! flavor
- use different "certificate name" through API for eduVPN / Let's Connect! 
  flavor

## 1.2.2 (2018-05-14)
- use different OAuth client information for Let's Connect! flavor

## 1.2.1 (2018-05-04)
- update `ics-openvpn` submodule to v0.7.5 (#153)

## 1.2.0 (2018-05-03)
- update `ics-openvpn` submodule to v0.7.4 (#133)
- update `client_id` (#127)
- fetch update server VPN configuration on connect (#123)
- fetch new client certificate if the old one expired (#130)
- fix connecting to 2FA enabled profile (#106)
- trigger new authorization on profiles page when OAuth client was revoked 
  (#141)
- many text changes in the application
- initial Let's Connect! branding changes, still WiP (#147)
- fix use of "refresh token" (#149)
- update VPN connection status icon to new artwork (#148)

## 1.1.1 (2017-11-23)
- N/A

## 1.0.1 (2017-02-01)
- N/A

## 1.0.0 (2017-02-01)
- initial release

# MoreMinecrafts-Beta

Fabric 기반 모드 바이브 코딩 제작 중

## Development rules

모드 개발의 기본 구조, 이식 방식, 테스트 루프는 [docs/MOD_DEVELOPMENT_RULES.md](docs/MOD_DEVELOPMENT_RULES.md)를 기준으로 한다.

## Miner Lantern dynamic lighting

The Miner Lantern socket uses **LambDynamicLights 4.13.0+26.3** on the client.
Install the full LambDynamicLights mod alongside MoreMinecrafts to enable its moving
level-15 light; the API alone does not provide a lighting engine. Dedicated servers
do not need this client mod. The development client includes it through
`clientRuntimeOnly`; it is not bundled into the distributable MoreMinecrafts JAR.

Run the lower-equipment regressions with `./gradlew runGameTest` and
`./gradlew runClientGameTest` (the latter needs a graphical or virtual display).

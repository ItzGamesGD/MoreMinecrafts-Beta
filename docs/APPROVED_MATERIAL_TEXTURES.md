# Approved material texture integration audit

- Target: Minecraft 26.3, Fabric, namespace `moreminecrafts`.
- Source: the 23-PNG attachment supplied with this task. Every source PNG decoded successfully and measured 32×32.
- Integration: original PNG bytes copied unchanged; matching source and destination SHA-256 values confirm byte identity.
- Registered block IDs: `titanium_ore`, `tungsten_ore`, `mythril_ore`, `etherium_ore`, `adamantium_debris`, and five corresponding `*_block` IDs.
- Flat registered item IDs: `raw_titanium`, `titanium_ingot`, `raw_tungsten`, `tungsten_ingot`, `mythril`, `etherium`, `adamantium_ingot`, and `enhancement_stone`.
- Adamantium source-name mapping: approved `adamantium.png` is referenced by the existing `adamantium_ingot` model; the gameplay registration and item ID are unchanged.
- The repository registers no deepslate variants. These approved files are retained in `textures/block/` and currently unused: `titanium_ore_deepslate.png`, `tungsten_ore_deepslate.png`, `mythril_ore_deepslate.png`, `etherium_ore_deepslate.png`, `adamantium_debris_deepslate.png`. No blocks or world generation were added.
- The previous rejected `adamantium_ingot.png` image was removed. The existing runtime model now references the supplied `adamantium.png`.

| Approved source | Repository destination | Dimensions | Source SHA-256 | Destination SHA-256 | Status |
|---|---|---:|---|---|---|
| `adamantium.png` | `src/main/resources/assets/moreminecrafts/textures/item/adamantium.png` | 32×32 | `f6dd356e4e7489471e37fc016d094bd1951119cc97cdb20fa95aa6bbee287542` | `f6dd356e4e7489471e37fc016d094bd1951119cc97cdb20fa95aa6bbee287542` | IDENTICAL |
| `adamantium_block.png` | `src/main/resources/assets/moreminecrafts/textures/block/adamantium_block.png` | 32×32 | `2fff266de6e11c297fd9f2468ddb095e4f20cf22b16e1c3d395c14b477b1aada` | `2fff266de6e11c297fd9f2468ddb095e4f20cf22b16e1c3d395c14b477b1aada` | IDENTICAL |
| `adamantium_debris.png` | `src/main/resources/assets/moreminecrafts/textures/block/adamantium_debris.png` | 32×32 | `91bde229a6aba0c04478527b5acf8790cd731ca997fdf92ff75d81352b93308b` | `91bde229a6aba0c04478527b5acf8790cd731ca997fdf92ff75d81352b93308b` | IDENTICAL |
| `adamantium_debris_deepslate.png` | `src/main/resources/assets/moreminecrafts/textures/block/adamantium_debris_deepslate.png` | 32×32 | `3958725ce6b4cffe94c7633a89398501ee225192481cf425e443cc17f29f3451` | `3958725ce6b4cffe94c7633a89398501ee225192481cf425e443cc17f29f3451` | IDENTICAL |
| `enhancement_stone.png` | `src/main/resources/assets/moreminecrafts/textures/item/enhancement_stone.png` | 32×32 | `ea20812bbb139b4d7de8d295f64fef14ef7e2948da7f2ec6437f29fbcc23cf21` | `ea20812bbb139b4d7de8d295f64fef14ef7e2948da7f2ec6437f29fbcc23cf21` | IDENTICAL |
| `etherium.png` | `src/main/resources/assets/moreminecrafts/textures/item/etherium.png` | 32×32 | `bfe9a83d3290bf427504db653c47b7954b359556540a8911b1b1cea6afb63979` | `bfe9a83d3290bf427504db653c47b7954b359556540a8911b1b1cea6afb63979` | IDENTICAL |
| `etherium_block.png` | `src/main/resources/assets/moreminecrafts/textures/block/etherium_block.png` | 32×32 | `fe1cf26590cd423831d735effe7c7fcdb513e36f7f493bbdc2e506e5daccc2d2` | `fe1cf26590cd423831d735effe7c7fcdb513e36f7f493bbdc2e506e5daccc2d2` | IDENTICAL |
| `etherium_ore.png` | `src/main/resources/assets/moreminecrafts/textures/block/etherium_ore.png` | 32×32 | `7c48ea0e3006b01b2652090fe91136cdf82dcf47f98ba8096355c4901c55dd9c` | `7c48ea0e3006b01b2652090fe91136cdf82dcf47f98ba8096355c4901c55dd9c` | IDENTICAL |
| `etherium_ore_deepslate.png` | `src/main/resources/assets/moreminecrafts/textures/block/etherium_ore_deepslate.png` | 32×32 | `e87f056bde4205ab2db0cd39e1be86572c6101edcf701e30932be559656f3dc5` | `e87f056bde4205ab2db0cd39e1be86572c6101edcf701e30932be559656f3dc5` | IDENTICAL |
| `mythril.png` | `src/main/resources/assets/moreminecrafts/textures/item/mythril.png` | 32×32 | `77c16702e67171b70c77908873b6a04c83393510ae0139baa69a9597004decc8` | `77c16702e67171b70c77908873b6a04c83393510ae0139baa69a9597004decc8` | IDENTICAL |
| `mythril_block.png` | `src/main/resources/assets/moreminecrafts/textures/block/mythril_block.png` | 32×32 | `390a296912f663bfa12a8264d8e9e40b375541183cbc439c26433a1570f5f41d` | `390a296912f663bfa12a8264d8e9e40b375541183cbc439c26433a1570f5f41d` | IDENTICAL |
| `mythril_ore.png` | `src/main/resources/assets/moreminecrafts/textures/block/mythril_ore.png` | 32×32 | `79d1ec8ad63fa25944720b2b72655938c28efb52d7bffb1e4c60ac95cef75cbe` | `79d1ec8ad63fa25944720b2b72655938c28efb52d7bffb1e4c60ac95cef75cbe` | IDENTICAL |
| `mythril_ore_deepslate.png` | `src/main/resources/assets/moreminecrafts/textures/block/mythril_ore_deepslate.png` | 32×32 | `7db800c991285b62b4cc01d87a68b2c634fe4a2f07e0cf20a3060ae37f434640` | `7db800c991285b62b4cc01d87a68b2c634fe4a2f07e0cf20a3060ae37f434640` | IDENTICAL |
| `raw_titanium.png` | `src/main/resources/assets/moreminecrafts/textures/item/raw_titanium.png` | 32×32 | `b6d59587f8a9efd3e8e6197e55cc4425d474dbd33f590045b36e6bda94e63a3f` | `b6d59587f8a9efd3e8e6197e55cc4425d474dbd33f590045b36e6bda94e63a3f` | IDENTICAL |
| `raw_tungsten.png` | `src/main/resources/assets/moreminecrafts/textures/item/raw_tungsten.png` | 32×32 | `17da3318bcfdcbf94deabda4a00a273111313181a7a440114861c6365e384439` | `17da3318bcfdcbf94deabda4a00a273111313181a7a440114861c6365e384439` | IDENTICAL |
| `titanium_block.png` | `src/main/resources/assets/moreminecrafts/textures/block/titanium_block.png` | 32×32 | `1c9f28b99af6fd2e7aaff1ef1f177fa6e222528d227425fb64eb27329ddf81fa` | `1c9f28b99af6fd2e7aaff1ef1f177fa6e222528d227425fb64eb27329ddf81fa` | IDENTICAL |
| `titanium_ingot.png` | `src/main/resources/assets/moreminecrafts/textures/item/titanium_ingot.png` | 32×32 | `06c2fad8934767e7f03dd768f878c005ea656b3f099795a2b06ffdf42c83c34d` | `06c2fad8934767e7f03dd768f878c005ea656b3f099795a2b06ffdf42c83c34d` | IDENTICAL |
| `titanium_ore.png` | `src/main/resources/assets/moreminecrafts/textures/block/titanium_ore.png` | 32×32 | `c808eec1dde92884dc7b60c8a02c0cd5826ee5f95e33e088da8b501550565972` | `c808eec1dde92884dc7b60c8a02c0cd5826ee5f95e33e088da8b501550565972` | IDENTICAL |
| `titanium_ore_deepslate.png` | `src/main/resources/assets/moreminecrafts/textures/block/titanium_ore_deepslate.png` | 32×32 | `d1567fa9ab73df51fdd1f1861d90c34e84163cb057bd9b9dd475ae93bb3b432c` | `d1567fa9ab73df51fdd1f1861d90c34e84163cb057bd9b9dd475ae93bb3b432c` | IDENTICAL |
| `tungsten_block.png` | `src/main/resources/assets/moreminecrafts/textures/block/tungsten_block.png` | 32×32 | `c893f6c0477f6a9de1e667ead2916f598dea7ddd154c698d96d32feb2d79acbf` | `c893f6c0477f6a9de1e667ead2916f598dea7ddd154c698d96d32feb2d79acbf` | IDENTICAL |
| `tungsten_ingot.png` | `src/main/resources/assets/moreminecrafts/textures/item/tungsten_ingot.png` | 32×32 | `4fc9e36e743a5c3ef6d76cc467ff0a2a60f841e66356c28a34561bb2fbc0031f` | `4fc9e36e743a5c3ef6d76cc467ff0a2a60f841e66356c28a34561bb2fbc0031f` | IDENTICAL |
| `tungsten_ore.png` | `src/main/resources/assets/moreminecrafts/textures/block/tungsten_ore.png` | 32×32 | `2b750f05f6aa65145f606225433172900e5a0d36439540fcfa325c8036a43116` | `2b750f05f6aa65145f606225433172900e5a0d36439540fcfa325c8036a43116` | IDENTICAL |
| `tungsten_ore_deepslate.png` | `src/main/resources/assets/moreminecrafts/textures/block/tungsten_ore_deepslate.png` | 32×32 | `1e4a48eb2927c232d2a5b0f62eeed26e5e47a617ff10b12ac863510b9cd5900f` | `1e4a48eb2927c232d2a5b0f62eeed26e5e47a617ff10b12ac863510b9cd5900f` | IDENTICAL |

Visual appearance was not verified in a running Minecraft client. In-game visual verification remains manual.

# Stage 7 Fishing vertical slice

Stage 7 intercepts `FishingHook.retrieve(ItemStack)` only when vanilla has set the hook's `biting` state. Casting, hook simulation, water checks, waiting, Lure, and non-catch reel behavior remain vanilla. The original retrieve call reports one durability point while the custom challenge owns catch resolution; ordinary treasure/junk is replaced only for this valid custom-fish catch path.

The server records challenge start tick plus a snapshot of Reel, Crankbait, Minnow, measured depth, and Luck input. The fixed 20-tick half-cycle produces a deterministic ping-pong marker, the inclusive center window is 0.42–0.58, and 160 ticks is the timeout. A subsequent rod-use is intent only: the server derives marker position from its tick and never accepts position, outcome, species, grade, or length from the client.

Reel clamps measured water-depth tier and never changes weights. Minnow removes species whose hard requirement is not met. Only after those filters, Crankbait biases the five fish-grade weights. Species and grade selection plus centimeter length generation use server RNG. Quality is reproduced from the species factor, grade, and normalized length.

A rod's three real equipment stacks are stored in the persistent `rod_equipment` data component. Fish use item identity for species and persistent `fish_grade` / `fish_length` components. Equipment uses vanilla damage fields; the isolated wear rule is one point per started valid catch.

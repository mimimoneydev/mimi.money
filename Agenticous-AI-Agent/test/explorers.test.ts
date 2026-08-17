import assert from "node:assert/strict";
import test from "node:test";
import { EXPLORERS } from "../src/explorers.js";

test("configured explorers exactly cover the MiMi facilitator networks", () => {
  assert.deepEqual(
    EXPLORERS.map(item => item.network).sort(),
    [
      "eip155:1", "eip155:137", "eip155:31612", "eip155:42161",
      "eip155:421614", "eip155:42220", "eip155:43114", "eip155:4326", "eip155:8453",
      "eip155:84532",
    ].sort(),
  );
});

test("each explorer uses HTTPS", () => {
  for (const explorer of EXPLORERS) {
    assert.equal(new URL(explorer.explorerBase).protocol, "https:");
    if (explorer.apiBase) assert.equal(new URL(explorer.apiBase).protocol, "https:");
  }
});

test("Mezo and Avalanche use their live API hosts and keep explorer links separate", () => {
  const mezo = EXPLORERS.find(item => item.chainId === 31612)!;
  assert.equal(mezo.apiBase, "https://api.explorer.mezo.org");
  assert.equal(mezo.explorerBase, "https://explorer.mezo.org");
  const avalanche = EXPLORERS.find(item => item.chainId === 43114)!;
  assert.equal(avalanche.apiKind, "routescan");
  assert.match(avalanche.apiBase ?? "", /api\.routescan\.io/);
  assert.equal(avalanche.explorerBase, "https://subnets.avax.network/c-chain");
});

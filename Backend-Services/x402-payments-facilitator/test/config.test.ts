import assert from "node:assert/strict";
import test from "node:test";
import { NETWORK_CATALOG } from "../src/config.js";

test("all required MiMi Money EVM networks are uniquely configured", () => {
  const required = [1, 137, 143, 4326, 8453, 84532, 31612, 42161, 421614, 42220, 43114];
  const actual = Object.values(NETWORK_CATALOG).map(chain => chain.id);
  assert.deepEqual([...actual].sort((a, b) => a - b), [...required].sort((a, b) => a - b));
  assert.equal(new Set(actual).size, actual.length);
});
